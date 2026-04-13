package io.botinis.app.domain

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor(
    private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(outputFile: File): Result<Unit> {
        return try {
            this.outputFile = outputFile
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
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun stopRecording(): Result<File> {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            val file = outputFile ?: throw IllegalStateException("No output file")
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            outputFile?.delete()
        } catch (e: Exception) {
            // Ignore cleanup errors
        } finally {
            mediaRecorder = null
            outputFile = null
        }
    }

    fun release() {
        mediaRecorder?.release()
        mediaRecorder = null
    }
}
