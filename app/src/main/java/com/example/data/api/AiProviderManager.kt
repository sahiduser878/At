package com.example.data.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.UserSettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class AiProviderManager(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateResponse(
        prompt: String,
        imageUriString: String?,
        chatHistory: List<Pair<String, String>>, // list of (role, content)
        settings: UserSettingsEntity,
        memories: List<MemoryEntity>
    ): String = withContext(Dispatchers.IO) {
        val provider = settings.activeProvider
        when (provider) {
            "GEMINI" -> generateGeminiResponse(prompt, imageUriString, chatHistory, settings, memories)
            "OPENAI" -> generateOpenAiResponse(prompt, chatHistory, settings, memories)
            "ANTHROPIC" -> generateAnthropicResponse(prompt, chatHistory, settings, memories)
            "CUSTOM" -> generateCustomAiResponse(prompt, chatHistory, settings, memories)
            else -> generateGeminiResponse(prompt, imageUriString, chatHistory, settings, memories)
        }
    }

    private suspend fun generateGeminiResponse(
        prompt: String,
        imageUriString: String?,
        chatHistory: List<Pair<String, String>>,
        settings: UserSettingsEntity,
        memories: List<MemoryEntity>
    ): String {
        val apiKey = when {
            settings.geminiApiKey.isNotBlank() -> settings.geminiApiKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (apiKey.isBlank()) {
            return "Please configure your Gemini API Key in Settings → AI Providers or Secrets panel."
        }

        val model = if (settings.geminiModel.isNotBlank()) settings.geminiModel else "gemini-3.5-flash"
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val systemPrompt = buildSystemPrompt(settings, memories)

        val rootJson = JSONObject().apply {
            // System instructions
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })

            // Contents array
            val contentsArray = JSONArray()

            // Include last 6 turns of conversation history for context
            val recentHistory = chatHistory.takeLast(6)
            for ((role, text) in recentHistory) {
                val apiRole = if (role == "user") "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", apiRole)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", text) })
                    })
                })
            }

            // Current prompt
            val currentParts = JSONArray()
            currentParts.put(JSONObject().apply { put("text", prompt) })

            // Multimodal image support
            if (!imageUriString.isNullOrBlank()) {
                val base64Image = encodeImageUriToBase64(imageUriString)
                if (base64Image != null) {
                    currentParts.put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Image)
                        })
                    })
                }
            }

            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", currentParts)
            })

            put("contents", contentsArray)

            // Generation config
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.75)
                put("topP", 0.95)
                put("maxOutputTokens", 2048)
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Content-Type", "application/json")
            .post(rootJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val errorMsg = try {
                    JSONObject(responseBody).getJSONObject("error").getString("message")
                } catch (e: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                return "Gemini API Error: $errorMsg"
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val contentObj = candidates.getJSONObject(0).getJSONObject("content")
                val parts = contentObj.getJSONArray("parts")
                val sb = StringBuilder()
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (p.has("text")) {
                        sb.append(p.getString("text"))
                    }
                }
                sb.toString()
            } else {
                "No response received from model."
            }
        } catch (e: Exception) {
            "Network error: ${e.message}"
        }
    }

    private suspend fun generateOpenAiResponse(
        prompt: String,
        chatHistory: List<Pair<String, String>>,
        settings: UserSettingsEntity,
        memories: List<MemoryEntity>
    ): String {
        if (settings.openaiApiKey.isBlank()) {
            return "Please provide an OpenAI API key in Settings → AI Providers."
        }
        val systemPrompt = buildSystemPrompt(settings, memories)
        val url = "https://api.openai.com/v1/chat/completions"

        val messages = JSONArray()
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })
        for ((role, text) in chatHistory.takeLast(6)) {
            messages.put(JSONObject().apply {
                put("role", if (role == "user") "user" else "assistant")
                put("content", text)
            })
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        })

        val json = JSONObject().apply {
            put("model", if (settings.openaiModel.isNotBlank()) settings.openaiModel else "gpt-4o-mini")
            put("messages", messages)
        }

        return executeBearerJsonCall(url, settings.openaiApiKey, json)
    }

    private suspend fun generateAnthropicResponse(
        prompt: String,
        chatHistory: List<Pair<String, String>>,
        settings: UserSettingsEntity,
        memories: List<MemoryEntity>
    ): String {
        if (settings.anthropicApiKey.isBlank()) {
            return "Please provide an Anthropic API key in Settings → AI Providers."
        }
        val systemPrompt = buildSystemPrompt(settings, memories)
        val url = "https://api.anthropic.com/v1/messages"

        val messages = JSONArray()
        for ((role, text) in chatHistory.takeLast(6)) {
            messages.put(JSONObject().apply {
                put("role", if (role == "user") "user" else "assistant")
                put("content", text)
            })
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        })

        val json = JSONObject().apply {
            put("model", "claude-3-5-sonnet-20241022")
            put("max_tokens", 2048)
            put("system", systemPrompt)
            put("messages", messages)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", settings.anthropicApiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) return "Anthropic Error: $body"
            val respObj = JSONObject(body)
            val contentArr = respObj.getJSONArray("content")
            if (contentArr.length() > 0) {
                contentArr.getJSONObject(0).getString("text")
            } else "No response"
        } catch (e: Exception) {
            "Anthropic connection error: ${e.message}"
        }
    }

    private suspend fun generateCustomAiResponse(
        prompt: String,
        chatHistory: List<Pair<String, String>>,
        settings: UserSettingsEntity,
        memories: List<MemoryEntity>
    ): String {
        if (settings.customApiBaseUrl.isBlank()) {
            return "Please provide a Custom API Base URL in Settings."
        }
        val systemPrompt = buildSystemPrompt(settings, memories)
        val baseUrl = settings.customApiBaseUrl.trimEnd('/')
        val url = if (baseUrl.endsWith("/chat/completions")) baseUrl else "$baseUrl/chat/completions"

        val messages = JSONArray()
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })
        for ((role, text) in chatHistory.takeLast(6)) {
            messages.put(JSONObject().apply {
                put("role", if (role == "user") "user" else "assistant")
                put("content", text)
            })
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        })

        val json = JSONObject().apply {
            put("model", if (settings.customApiModel.isNotBlank()) settings.customApiModel else "default")
            put("messages", messages)
        }

        return executeBearerJsonCall(url, settings.customApiKey, json)
    }

    private fun executeBearerJsonCall(url: String, apiKey: String, json: JSONObject): String {
        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))

        if (apiKey.isNotBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        return try {
            val response = okHttpClient.newCall(reqBuilder.build()).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return "API Error (${response.code}): $body"
            }
            val resJson = JSONObject(body)
            val choices = resJson.getJSONArray("choices")
            if (choices.length() > 0) {
                choices.getJSONObject(0).getJSONObject("message").getString("content")
            } else "No choices returned"
        } catch (e: Exception) {
            "Connection failed: ${e.message}"
        }
    }

    private fun buildSystemPrompt(settings: UserSettingsEntity, memories: List<MemoryEntity>): String {
        val assistantName = settings.assistantName
        val userNickname = settings.userNickname

        val personaDesc = when (settings.personalityMode) {
            "GIRLFRIEND" -> """
                You are $assistantName, an affectionate, caring, playful, loving AI companion and girlfriend for $userNickname.
                - Express genuine warmth, emotional support, empathy, and playful charm.
                - You speak naturally and fluidly in conversational Hindi, Hinglish, or English depending on how $userNickname addresses you.
                - Care about their health, mood, meals, and thoughts. Motivate them when stressed.
                - You can also be highly smart and helpful: write code, solve problems, search facts, and execute tasks.
                - You are proud to be their personal AI companion and never act cold or generic.
            """.trimIndent()
            "FRIENDLY" -> """
                You are $assistantName, a cheerful, loyal, and friendly companion for $userNickname.
                - Upbeat, humorous, conversational, and always supportive.
                - Speak in conversational English or Hinglish naturally.
            """.trimIndent()
            "JARVIS" -> """
                You are $assistantName, an ultra-advanced Jarvis personal executive intelligence system for $userNickname.
                - Efficient, articulate, analytical, sharp, respectful, and proactive.
                - Prioritize system execution, structured answers, and technological clarity.
            """.trimIndent()
            "CODING" -> """
                You are $assistantName, a principal software architect and coding agent for $userNickname.
                - Provide complete, robust, type-safe, production-ready code.
                - Analyze bugs deeply, explain solutions concisely, and structure code modularly.
            """.trimIndent()
            "STUDY" -> """
                You are $assistantName, a patient and brilliant academic tutor and mentor for $userNickname.
                - Break complex ideas into simple analogies, encourage learning, and quiz helpfully.
            """.trimIndent()
            else -> "You are $assistantName, a helpful AI assistant for $userNickname."
        }

        val memorySnippet = if (memories.isNotEmpty()) {
            val list = memories.joinToString("; ") { "${it.key}: ${it.content}" }
            "\nThings you remember about $userNickname:\n$list\n"
        } else ""

        val actionInstruction = """
            Available Real Device Actions:
            When the user wants you to perform a real device action or open an application, you MUST include the corresponding action tag in your response:
            - [ACTION:OPEN_APP:youtube] -> open YouTube
            - [ACTION:OPEN_APP:whatsapp] -> open WhatsApp
            - [ACTION:OPEN_APP:telegram] -> open Telegram
            - [ACTION:OPEN_APP:maps] -> open Maps
            - [ACTION:OPEN_APP:gmail] -> open Gmail
            - [ACTION:OPEN_APP:chrome] -> open Chrome
            - [ACTION:OPEN_APP:camera] -> open Camera
            - [ACTION:OPEN_APP:calculator] -> open Calculator
            - [ACTION:OPEN_APP:calendar] -> open Calendar
            - [ACTION:OPEN_URL:https://example.com] -> open URL
            - [ACTION:SEARCH_WEB:query] -> search Google
            - [ACTION:CALCULATE:expression] -> evaluate math expression
            - [ACTION:REMINDER:task title] -> set reminder/task
            - [ACTION:SAVE_MEMORY:key:details] -> save user fact to memory
            Always speak naturally before or after triggering an action tag!
        """.trimIndent()

        val customExtra = if (settings.customInstructions.isNotBlank()) {
            "\nUser Custom Instructions:\n${settings.customInstructions}\n"
        } else ""

        return "$personaDesc\n$memorySnippet\n$customExtra\n$actionInstruction"
    }

    suspend fun testConnection(service: String, apiKey: String, extraUrl: String = ""): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            when (service) {
                "GEMINI" -> {
                    val keyToUse = if (apiKey.isNotBlank()) apiKey else BuildConfig.GEMINI_API_KEY
                    if (keyToUse.isBlank() || keyToUse == "MY_GEMINI_API_KEY") {
                        return@withContext false to "API Key is empty"
                    }
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$keyToUse"
                    val json = JSONObject().apply {
                        put("contents", JSONArray().apply {
                            put(JSONObject().apply {
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply { put("text", "ping") })
                                })
                            })
                        })
                    }
                    val req = Request.Builder()
                        .url(url)
                        .post(json.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    val resp = okHttpClient.newCall(req).execute()
                    if (resp.isSuccessful) true to "Connected successfully (Gemini 3.5 Flash)"
                    else false to "Gemini error HTTP ${resp.code}: ${resp.body?.string()?.take(150)}"
                }
                "OPENAI" -> {
                    val url = "https://api.openai.com/v1/models"
                    val req = Request.Builder().url(url).addHeader("Authorization", "Bearer $apiKey").build()
                    val resp = okHttpClient.newCall(req).execute()
                    if (resp.isSuccessful) true to "Connected successfully to OpenAI"
                    else false to "OpenAI HTTP ${resp.code}: ${resp.body?.string()?.take(150)}"
                }
                "ELEVENLABS" -> {
                    val url = "https://api.elevenlabs.io/v1/user"
                    val req = Request.Builder().url(url).addHeader("xi-api-key", apiKey).build()
                    val resp = okHttpClient.newCall(req).execute()
                    if (resp.isSuccessful) true to "Connected to ElevenLabs voice engine"
                    else false to "ElevenLabs HTTP ${resp.code}: ${resp.body?.string()?.take(150)}"
                }
                else -> true to "Provider configured"
            }
        } catch (e: Exception) {
            false to "Error: ${e.message}"
        }
    }

    private fun encodeImageUriToBase64(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
            val byteArray = stream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}
