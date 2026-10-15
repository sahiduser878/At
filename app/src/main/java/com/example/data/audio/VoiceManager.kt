package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.TimeUnit

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var mediaPlayer: MediaPlayer? = null
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    private val _speechPartialText = MutableStateFlow("")
    val speechPartialText = _speechPartialText.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }
                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    fun speak(
        text: String,
        provider: String,
        elevenLabsKey: String = "",
        voiceId: String = "21m00Tcm4TlvDq8ikWAM",
        speed: Float = 1.0f,
        pitch: Float = 1.05f,
        scope: CoroutineScope
    ) {
        stopSpeaking()
        val cleanText = stripMarkdownAndActions(text)
        if (cleanText.isBlank()) return

        if (provider == "ELEVENLABS" && elevenLabsKey.isNotBlank()) {
            scope.launch {
                speakWithElevenLabs(cleanText, elevenLabsKey, voiceId, speed)
            }
        } else {
            speakWithAndroidTts(cleanText, speed, pitch)
        }
    }

    private fun speakWithAndroidTts(text: String, speed: Float, pitch: Float) {
        if (!isTtsInitialized || tts == null) return
        tts?.setSpeechRate(speed)
        tts?.setPitch(pitch)
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "YesminAI_${System.currentTimeMillis()}")
        }
        _isSpeaking.value = true
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "YesminAI_Utterance")
    }

    private suspend fun speakWithElevenLabs(
        text: String,
        apiKey: String,
        voiceId: String,
        speed: Float
    ) = withContext(Dispatchers.IO) {
        try {
            _isSpeaking.value = true
            val url = "https://api.elevenlabs.io/v1/text-to-speech/$voiceId"
            val jsonBody = JSONObject().apply {
                put("text", text)
                put("model_id", "eleven_multilingual_v2")
                val voiceSettings = JSONObject().apply {
                    put("stability", 0.5)
                    put("similarity_boost", 0.75)
                }
                put("voice_settings", voiceSettings)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("xi-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
                val tempAudioFile = File(context.cacheDir, "elevenlabs_${System.currentTimeMillis()}.mp3")
                response.body!!.byteStream().use { input ->
                    FileOutputStream(tempAudioFile).use { output ->
                        input.copyTo(output)
                    }
                }
                withContext(Dispatchers.Main) {
                    playAudioFile(tempAudioFile)
                }
            } else {
                // Fallback to Android TTS if ElevenLabs request fails
                withContext(Dispatchers.Main) {
                    speakWithAndroidTts(text, speed, 1.05f)
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceManager", "ElevenLabs error: ${e.message}")
            withContext(Dispatchers.Main) {
                speakWithAndroidTts(text, speed, 1.05f)
            }
        }
    }

    private fun playAudioFile(file: File) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    _isSpeaking.value = false
                    file.delete()
                }
                setOnErrorListener { _, _, _ ->
                    _isSpeaking.value = false
                    file.delete()
                    false
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            _isSpeaking.value = false
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            // ignore
        } finally {
            _isSpeaking.value = false
        }
    }

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        stopSpeaking()
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _speechPartialText.value = ""
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }
                    override fun onError(error: Int) {
                        _isListening.value = false
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error in voice"
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            else -> "Voice recognition code $error"
                        }
                        onError(errorMsg)
                    }
                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognized = matches[0]
                            _speechPartialText.value = recognized
                            onResult(recognized)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _speechPartialText.value = matches[0]
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
        } else {
            onError("Speech recognition not available on this device")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // ignore
        } finally {
            _isListening.value = false
        }
    }

    private fun stripMarkdownAndActions(text: String): String {
        return text
            .replace(Regex("\\[ACTION:[^\\]]+\\]"), "")
            .replace(Regex("```[\\s\\S]*?```"), "Code block omitted.")
            .replace(Regex("[*#`_~>]"), "")
            .trim()
    }

    fun release() {
        stopSpeaking()
        stopListening()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
