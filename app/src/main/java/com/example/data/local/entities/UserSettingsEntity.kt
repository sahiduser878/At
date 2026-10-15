package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val assistantName: String = "Yesmin",
    val userNickname: String = "Jaan",
    val personalityMode: String = "GIRLFRIEND", // GIRLFRIEND, FRIENDLY, JARVIS, CODING, STUDY
    val greetingStyle: String = "WARM_CARING", // WARM_CARING, PLAYFUL, EXECUTIVE, MOTIVATIONAL
    val customInstructions: String = "",
    val avatarKey: String = "CYBER_ORB", // CYBER_ORB, HOLOGRAPHIC_MUSE, VIOLET_SYNTH, ROSE_QUARTZ, GOLDEN_JARVIS, CYAN_PULSE
    val voiceProvider: String = "ANDROID_TTS", // ANDROID_TTS, ELEVENLABS, GEMINI_VOICE
    val elevenLabsApiKey: String = "",
    val elevenLabsVoiceId: String = "21m00Tcm4TlvDq8ikWAM", // Rachel default
    val elevenLabsSpeed: Float = 1.0f,
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-3.5-flash",
    val activeProvider: String = "GEMINI", // GEMINI, OPENAI, ANTHROPIC, CUSTOM
    val openaiApiKey: String = "",
    val openaiModel: String = "gpt-4o-mini",
    val anthropicApiKey: String = "",
    val customApiBaseUrl: String = "",
    val customApiKey: String = "",
    val customApiModel: String = "",
    val accentColorHex: String = "#F43F5E", // Neon Magenta default
    val autoSpeak: Boolean = true,
    val speechSpeed: Float = 1.0f,
    val speechPitch: Float = 1.05f,
    val memoryEnabled: Boolean = true,
    val liveVoiceInterruptMode: Boolean = true
)
