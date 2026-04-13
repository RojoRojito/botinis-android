package io.botinis.app.ui.screens.conversation

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.botinis.app.data.model.CEFRLevel
import io.botinis.app.data.model.ConversationTurn
import io.botinis.app.data.model.Correction
import io.botinis.app.data.model.Feedback
import io.botinis.app.data.model.Scenario
import io.botinis.app.data.remote.GroqApiService
import io.botinis.app.data.remote.GroqChatRequest
import io.botinis.app.data.remote.GroqMessage
import io.botinis.app.domain.AudioPlayer
import io.botinis.app.domain.EdgeTtsClient
import io.botinis.app.domain.ScenarioCatalog
import io.botinis.app.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: GroqApiService,
    private val audioPlayer: AudioPlayer,
    private val settingsRepository: SettingsRepository,
    private val edgeTtsClient: EdgeTtsClient
) : ViewModel() {

    private var currentApiKey: String = ""

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    private val _scenario = MutableStateFlow<Scenario?>(null)
    val scenario: StateFlow<Scenario?> = _scenario

    private var conversationHistory = mutableListOf<ConversationTurn>()
    private var mediaRecorder: MediaRecorder? = null
    private var tempAudioFile: File? = null
    private var mediaPlayer: MediaPlayer? = null
    private var objectivesCompleted = listOf(false, false, false)

    private val gson = Gson()

    fun initScenario(scenarioId: String) {
        val found = ScenarioCatalog.getScenarioById(scenarioId)
        _scenario.value = found
        conversationHistory.clear()
        objectivesCompleted = List(found?.objectives?.size ?: 0) { false }

        // Load API key from settings
        viewModelScope.launch {
            currentApiKey = settingsRepository.groqApiKey.first()
        }

        _uiState.update { it.copy(isReady = true) }
    }

    private fun getApiKey(): String {
        return if (currentApiKey.isNotBlank()) {
            "Bearer $currentApiKey"
        } else {
            // Fallback: try to get it synchronously (not ideal but works for now)
            ""
        }
    }

    fun startRecording() {
        // Check API key before recording
        if (currentApiKey.isBlank()) {
            _uiState.update {
                it.copy(error = "API key not configured. Please set it in Settings first.")
            }
            return
        }

        try {
            val file = File(context.cacheDir, "recording_${System.currentTimeMillis()}.m4a")
            tempAudioFile = file

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(16000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            _uiState.update { it.copy(isRecording = true) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Error starting recording: ${e.message}") }
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            val file = tempAudioFile
            if (file != null && file.exists()) {
                viewModelScope.launch {
                    processAudio(file)
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Error stopping recording: ${e.message}") }
            tempAudioFile?.delete()
        } finally {
            _uiState.update { it.copy(isRecording = false) }
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            tempAudioFile?.delete()
        } catch (_: Exception) { }
        finally {
            _uiState.update { it.copy(isRecording = false) }
        }
    }

    private suspend fun processAudio(audioFile: File) {
        _uiState.update { it.copy(isTranscribing = true) }

        // Step 1: Transcribe audio via Groq Whisper
        val transcriptResult = transcribeAudio(audioFile)
        audioFile.delete() // Clean up temp file

        if (transcriptResult.isFailure) {
            _uiState.update {
                it.copy(
                    isTranscribing = false,
                    error = "Transcription failed: ${transcriptResult.exceptionOrNull()?.message}"
                )
            }
            return
        }

        val userText = transcriptResult.getOrNull().orEmpty()
        if (userText.isBlank()) {
            _uiState.update {
                it.copy(
                    isTranscribing = false,
                    error = "No speech detected. Try again."
                )
            }
            return
        }

        // Step 2: Analyze grammar/feedback
        val feedbackResult = analyzeTurn(userText)
        val feedback = feedbackResult.getOrNull()

        // Step 3: Get LLM response
        _uiState.update { it.copy(isTranscribing = false, isGeneratingResponse = true) }
        val scenario = _scenario.value
        if (scenario == null) {
            _uiState.update { it.copy(isGeneratingResponse = false, error = "No scenario selected") }
            return
        }

        val responseResult = getBotResponse(scenario)
        if (responseResult.isFailure) {
            _uiState.update {
                it.copy(
                    isGeneratingResponse = false,
                    error = "Failed to get response: ${responseResult.exceptionOrNull()?.message}"
                )
            }
            return
        }

        val botResponse = responseResult.getOrNull().orEmpty()

        // Step 4: Check objectives
        val updatedObjectives = checkObjectives(scenario, userText)
        objectivesCompleted = updatedObjectives

        // Step 5: Build turn and add to history
        val isPerfect = feedback?.corrections.isNullOrEmpty()
        val turn = ConversationTurn(
            userTranscript = userText,
            feedback = feedback,
            botResponse = botResponse,
            isPerfect = isPerfect
        )
        conversationHistory.add(turn)

        // Step 6: Generate TTS for bot response
        _uiState.update { it.copy(isGeneratingResponse = false, isPlayingAudio = true) }
        playBotVoice(botResponse, scenario.voice)

        // Step 7: Update UI state
        _uiState.update {
            it.copy(
                turns = conversationHistory.toList(),
                isPlayingAudio = false,
                objectivesCompleted = objectivesCompleted,
                allObjectivesComplete = objectivesCompleted.all { it }
            )
        }
    }

    private suspend fun transcribeAudio(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestFile = audioFile.asRequestBody("audio/m4a".toMediaType())
            val audioPart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
            val model = "whisper-large-v3-turbo".toRequestBody("text/plain".toMediaType())

            val response = apiService.transcribeAudio(
                apiKey = getApiKey(),
                audio = audioPart,
                model = model
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.text)
            } else {
                val errorBody = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception("Transcription failed: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun analyzeTurn(userText: String): Result<Feedback> = withContext(Dispatchers.IO) {
        try {
            val systemMsg = GroqMessage(
                "system",
                """You are an English grammar coach for a Spanish speaker. Analyze the user's English utterance.
Return a JSON object with:
- corrections: array of {original, corrected, explanation}
- strengths: array of things they did well
- suggestions: array of improvement tips
Keep explanations in Spanish. Be encouraging but precise.
If no errors, return empty corrections array."""
            )
            val userMsg = GroqMessage("user", "Analyze: $userText")

            val request = GroqChatRequest(
                model = "llama-3.1-8b-instant",
                messages = listOf(systemMsg, userMsg),
                temperature = 0.3f,
                max_tokens = 400
            )

            val response = apiService.analyzeFeedback(
                apiKey = getApiKey(),
                request = request
            )

            if (response.isSuccessful && response.body()?.choices?.isNotEmpty() == true) {
                val content = response.body()!!.choices[0].message.content
                Result.success(parseFeedback(content))
            } else {
                Result.success(Feedback(corrections = emptyList(), strengths = listOf("Good attempt!"), suggestions = emptyList()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getBotResponse(scenario: Scenario): Result<String> = withContext(Dispatchers.IO) {
        try {
            val messages = mutableListOf<GroqMessage>()
            messages.add(GroqMessage("system", scenario.systemPrompt))

            conversationHistory.forEach { turn ->
                messages.add(GroqMessage("user", turn.userTranscript))
                messages.add(GroqMessage("assistant", turn.botResponse))
            }

            val request = GroqChatRequest(
                model = "llama-3.1-8b-instant",
                messages = messages,
                temperature = 0.7f,
                max_tokens = 300
            )

            val response = apiService.chatCompletion(
                apiKey = getApiKey(),
                request = request
            )

            if (response.isSuccessful && response.body()?.choices?.isNotEmpty() == true) {
                Result.success(response.body()!!.choices[0].message.content)
            } else {
                val errorBody = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception("Chat failed: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun checkObjectives(scenario: Scenario, userText: String): List<Boolean> {
        val current = objectivesCompleted.toMutableList()
        val objectives = scenario.objectives
        val textLower = userText.lowercase()

        // Simple keyword-based objective checking
        objectives.forEachIndexed { index, objective ->
            if (current[index]) return@forEachIndexed
            val keywords = objective.lowercase()
                .replace("and", "")
                .replace("or", "")
                .split("\\s+".toRegex())
                .filter { it.length > 3 }

            val matchCount = keywords.count { keyword -> textLower.contains(keyword) }
            if (matchCount >= keywords.size * 0.5) {
                current[index] = true
            }
        }

        return current.toList()
    }

    private fun playBotVoice(text: String, voice: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ttsFile = File(context.cacheDir, "tts_${System.currentTimeMillis()}.mp3")
                val ttsResult = edgeTtsClient.synthesize(text, ttsFile, voice = voice)

                if (ttsResult.isSuccess && ttsFile.exists() && ttsFile.length() > 0) {
                    withContext(Dispatchers.Main) {
                        try {
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                        } catch (_: Exception) { }

                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(ttsFile.absolutePath)
                            setOnPreparedListener { start() }
                            prepareAsync()
                            setOnCompletionListener {
                                _uiState.update { it.copy(isPlayingAudio = false) }
                            }
                            setOnErrorListener { _, _, _ ->
                                _uiState.update { it.copy(isPlayingAudio = false) }
                                true
                            }
                        }
                    }
                } else {
                    _uiState.update { it.copy(isPlayingAudio = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isPlayingAudio = false) }
            }
        }
    }

    fun toggleTranscriptVisibility() {
        _uiState.update { it.copy(showTranscript = !it.showTranscript) }
    }

    private fun parseFeedback(content: String): Feedback {
        return try {
            // Try to extract JSON from response
            val jsonStart = content.indexOf('{')
            val jsonEnd = content.lastIndexOf('}')
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val jsonStr = content.substring(jsonStart, jsonEnd + 1)
                val feedbackDto = gson.fromJson(jsonStr, FeedbackDto::class.java)
                Feedback(
                    corrections = feedbackDto.corrections?.map {
                        Correction(
                            original = it.original ?: "",
                            corrected = it.corrected ?: "",
                            explanation = it.explanation ?: ""
                        )
                    } ?: emptyList(),
                    strengths = feedbackDto.strengths ?: emptyList(),
                    suggestions = feedbackDto.suggestions ?: emptyList()
                )
            } else {
                Feedback(
                    corrections = emptyList(),
                    strengths = listOf("Good attempt!"),
                    suggestions = emptyList()
                )
            }
        } catch (e: Exception) {
            Feedback(
                corrections = emptyList(),
                strengths = listOf("Good attempt!"),
                suggestions = emptyList()
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun endSession() {
        val scenario = _scenario.value ?: return
        val xpEarned = calculateXp()
        viewModelScope.launch {
            // TODO: Save session to DataStore
            _uiState.update {
                it.copy(
                    isSessionComplete = true,
                    xpEarned = xpEarned,
                    totalTurns = conversationHistory.size,
                    perfectTurns = conversationHistory.count { it.isPerfect }
                )
            }
        }
    }

    private fun calculateXp(): Int {
        val scenario = _scenario.value ?: return 0
        val baseXp = 50
        val levelMultiplier = when (scenario.level) {
            CEFRLevel.A2 -> 1.0f
            CEFRLevel.B1 -> 1.5f
            CEFRLevel.B2 -> 2.0f
        }
        val perfectBonus = conversationHistory.count { it.isPerfect } * 10
        return ((baseXp + perfectBonus) * levelMultiplier).toInt()
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaRecorder?.release()
        mediaRecorder = null
    }
}

data class ConversationUiState(
    val isReady: Boolean = false,
    val isRecording: Boolean = false,
    val isTranscribing: Boolean = false,
    val isGeneratingResponse: Boolean = false,
    val isPlayingAudio: Boolean = false,
    val showTranscript: Boolean = true,
    val turns: List<ConversationTurn> = emptyList(),
    val objectivesCompleted: List<Boolean> = emptyList(),
    val allObjectivesComplete: Boolean = false,
    val error: String? = null,
    val isSessionComplete: Boolean = false,
    val xpEarned: Int = 0,
    val totalTurns: Int = 0,
    val perfectTurns: Int = 0
)

// DTO for parsing LLM feedback
data class FeedbackDto(
    val corrections: List<CorrectionDto>? = null,
    val strengths: List<String>? = null,
    val suggestions: List<String>? = null
)

data class CorrectionDto(
    val original: String? = null,
    val corrected: String? = null,
    val explanation: String? = null
)
