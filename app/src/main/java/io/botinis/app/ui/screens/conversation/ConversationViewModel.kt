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
import io.botinis.app.BuildConfig
import io.botinis.app.data.model.CEFRLevel
import io.botinis.app.data.model.ConversationTurn
import io.botinis.app.data.model.Correction
import io.botinis.app.data.model.Feedback
import io.botinis.app.data.model.Scenario
import io.botinis.app.data.remote.GroqApiService
import io.botinis.app.data.remote.GroqChatRequest
import io.botinis.app.data.remote.GroqMessage
import io.botinis.app.domain.AudioPlayer
import io.botinis.app.domain.ScenarioCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: GroqApiService,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val apiKey = "Bearer ${BuildConfig.GROQ_API_KEY}"

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
        _uiState.update { it.copy(isReady = true) }
    }

    fun startRecording() {
        try {
            val file = File(context.cacheDir, "recording_${System.currentTimeMillis()}.aac")
            tempAudioFile = file

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
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
            val requestFile = audioFile.asRequestBody("audio/aac".toMediaType())
            val audioPart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
            val model = "whisper-large-v3-turbo".toRequestBody("text/plain".toMediaType())

            val response = apiService.transcribeAudio(
                apiKey = apiKey,
                audio = audioPart,
                model = model
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.text)
            } else {
                Result.failure(Exception("Transcription failed: ${response.message()} (${response.code()})"))
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
                apiKey = apiKey,
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
                apiKey = apiKey,
                request = request
            )

            if (response.isSuccessful && response.body()?.choices?.isNotEmpty() == true) {
                Result.success(response.body()!!.choices[0].message.content)
            } else {
                Result.failure(Exception("Chat failed: ${response.message()} (${response.code()})"))
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
                // Use edge-tts via subprocess or a simple text-to-speech fallback
                // For now, using Android's built-in TTS as placeholder
                // TODO: Replace with Edge TTS API call
                val ttsFile = File(context.cacheDir, "tts_${System.currentTimeMillis()}.ogg")
                val ttsResult = generateTtsAudio(text, ttsFile)
                if (ttsResult.isSuccess && ttsFile.exists()) {
                    withContext(Dispatchers.Main) {
                        try {
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                        } catch (_: Exception) { }

                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(ttsFile.absolutePath)
                            prepare()
                            start()
                            setOnCompletionListener {
                                _uiState.update { it.copy(isPlayingAudio = false) }
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

    private fun generateTtsAudio(text: String, outputFile: File): Result<Unit> {
        return try {
            // Simple placeholder: save text as file
            // TODO: Replace with actual Edge TTS API call
            FileOutputStream(outputFile).use { fos ->
                fos.write(text.toByteArray())
            }
            // For now, just return success so the flow works
            // The actual TTS will be implemented separately
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
