package com.example.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

class AudioQueryRecorder(private val context: Context) {

    companion object {
        const val SAMPLE_RATE_HZ = 16_000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val MAX_RECORDING_SECONDS = 45
    }

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val pcmBuffer = ByteArrayOutputStream()

    @Volatile
    private var recordingActive = false

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun startRecording(
        scope: CoroutineScope,
        onAutoStopped: (ByteArray) -> Unit = {},
        onError: (String) -> Unit = {}
    ): Boolean {
        if (!hasMicPermission()) {
            onError("Izin mikrofon belum diberikan.")
            return false
        }
        if (recordingActive) return true

        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBuf <= 0) {
            onError("Perangkat audio tidak mendukung perekaman 16kHz mono.")
            return false
        }

        val bufferSize = maxOf(minBuf * 2, 4096)
        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE_HZ,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
        } catch (e: SecurityException) {
            onError("Akses mikrofon ditolak sistem: ${e.localizedMessage}")
            return false
        } catch (e: Exception) {
            onError("Gagal menginisialisasi AudioRecord: ${e.localizedMessage}")
            return false
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            onError("Mikrofon tidak siap atau sedang digunakan aplikasi lain.")
            return false
        }

        synchronized(pcmBuffer) { pcmBuffer.reset() }
        _amplitude.value = 0f
        _elapsedSeconds.value = 0
        audioRecord = recorder
        recordingActive = true

        try {
            recorder.startRecording()
        } catch (e: Exception) {
            recordingActive = false
            recorder.release()
            audioRecord = null
            onError("Tidak dapat memulai perekaman audio: ${e.localizedMessage}")
            return false
        }

        recordingJob = scope.launch(Dispatchers.IO) {
            val readBuf = ShortArray(bufferSize / 2)
            val byteBuf = ByteArray(readBuf.size * 2)
            val startedAt = System.currentTimeMillis()
            var hitMaxDuration = false

            try {
                while (isActive && recordingActive) {
                    val readCount = recorder.read(readBuf, 0, readBuf.size)
                    if (readCount > 0) {
                        var sumSquares = 0.0
                        for (i in 0 until readCount) {
                            val sample = readBuf[i].toInt()
                            sumSquares += sample.toDouble() * sample.toDouble()
                            byteBuf[i * 2] = (sample and 0xFF).toByte()
                            byteBuf[i * 2 + 1] = ((sample ushr 8) and 0xFF).toByte()
                        }
                        synchronized(pcmBuffer) {
                            pcmBuffer.write(byteBuf, 0, readCount * 2)
                        }
                        val rms = sqrt(sumSquares / readCount)
                        val normalized = (abs(rms) / 8000.0).toFloat().coerceIn(0.04f, 1f)
                        _amplitude.value = normalized
                    }

                    val sec = ((System.currentTimeMillis() - startedAt) / 1000L).toInt()
                    _elapsedSeconds.value = sec
                    if (sec >= MAX_RECORDING_SECONDS) {
                        hitMaxDuration = true
                        recordingActive = false
                        break
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError("Kesalahan saat membaca stream AudioRecord: ${e.localizedMessage}")
            } finally {
                if (hitMaxDuration) {
                    val wav = finalizeRecorderAndBuildWav()
                    if (wav.isNotEmpty()) {
                        onAutoStopped(wav)
                    }
                }
            }
        }

        return true
    }

    fun stopAndGetWav(): ByteArray {
        recordingActive = false
        recordingJob?.cancel()
        recordingJob = null
        return finalizeRecorderAndBuildWav()
    }

    fun cancelRecording() {
        recordingActive = false
        recordingJob?.cancel()
        recordingJob = null
        releaseRecorderOnly()
        synchronized(pcmBuffer) { pcmBuffer.reset() }
        _amplitude.value = 0f
        _elapsedSeconds.value = 0
    }

    private fun finalizeRecorderAndBuildWav(): ByteArray {
        releaseRecorderOnly()
        _amplitude.value = 0f
        val pcmBytes = synchronized(pcmBuffer) {
            val copy = pcmBuffer.toByteArray()
            pcmBuffer.reset()
            copy
        }
        if (pcmBytes.size < 1600) return ByteArray(0) // < ~0.05s of audio
        return pcmToWav(pcmBytes, SAMPLE_RATE_HZ, channels = 1, bitsPerSample = 16)
    }

    private fun releaseRecorderOnly() {
        val rec = audioRecord ?: return
        audioRecord = null
        try {
            if (rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                rec.stop()
            }
        } catch (_: Exception) {
        }
        try {
            rec.release()
        } catch (_: Exception) {
        }
    }

    private fun pcmToWav(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val totalDataLen = pcmData.size + 36

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalDataLen)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // Subchunk1Size (PCM)
            putShort(1.toShort()) // AudioFormat (1 = PCM)
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign)
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(pcmData.size)
        }.array()

        return ByteArray(header.size + pcmData.size).also { out ->
            System.arraycopy(header, 0, out, 0, header.size)
            System.arraycopy(pcmData, 0, out, header.size, pcmData.size)
        }
    }
}
