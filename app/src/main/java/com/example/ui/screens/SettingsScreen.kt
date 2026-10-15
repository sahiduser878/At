package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.UserSettingsEntity
import com.example.ui.MainViewModel
import com.example.ui.components.FuturisticOrb
import com.example.ui.components.GlassCard
import com.example.ui.components.OrbState
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.settingsFlow.collectAsState()

    var activeTab by remember { mutableStateOf("IDENTITY") } // IDENTITY, PROVIDERS, VOICE, MEMORY, APPEARANCE

    // Identity state
    var assistantNameInput by remember(settings.assistantName) { mutableStateOf(settings.assistantName) }
    var userNicknameInput by remember(settings.userNickname) { mutableStateOf(settings.userNickname) }
    var selectedPersonality by remember(settings.personalityMode) { mutableStateOf(settings.personalityMode) }
    var customInstructionsInput by remember(settings.customInstructions) { mutableStateOf(settings.customInstructions) }

    // Provider state
    var activeProvider by remember(settings.activeProvider) { mutableStateOf(settings.activeProvider) }
    var geminiKeyInput by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var geminiModelInput by remember(settings.geminiModel) { mutableStateOf(settings.geminiModel) }
    var openaiKeyInput by remember(settings.openaiApiKey) { mutableStateOf(settings.openaiApiKey) }
    var customBaseUrlInput by remember(settings.customApiBaseUrl) { mutableStateOf(settings.customApiBaseUrl) }
    var customApiKeyInput by remember(settings.customApiKey) { mutableStateOf(settings.customApiKey) }

    // Voice state
    var voiceProvider by remember(settings.voiceProvider) { mutableStateOf(settings.voiceProvider) }
    var elevenLabsKeyInput by remember(settings.elevenLabsApiKey) { mutableStateOf(settings.elevenLabsApiKey) }
    var elevenLabsVoiceIdInput by remember(settings.elevenLabsVoiceId) { mutableStateOf(settings.elevenLabsVoiceId) }
    var autoSpeakEnabled by remember(settings.autoSpeak) { mutableStateOf(settings.autoSpeak) }
    var speechSpeed by remember(settings.speechSpeed) { mutableFloatStateOf(settings.speechSpeed) }

    // Accent color
    var accentHex by remember(settings.accentColorHex) { mutableStateOf(settings.accentColorHex) }

    // Test results
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    val accentColor = remember(accentHex) {
        parseAccentColor(accentHex)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBgDark,
        topBar = {
            Surface(
                color = CyberSurfaceDark,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Settings & Assistant Config",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Customize ${settings.assistantName}'s Identity, Voice & Intelligence",
                            color = accentColor,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Tab bar
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tabs = listOf(
                    "IDENTITY" to "Identity & Name",
                    "PROVIDERS" to "AI Providers",
                    "VOICE" to "Voice & Speech",
                    "MEMORY" to "Memory & Context",
                    "APPEARANCE" to "Theme & Colors"
                )
                items(tabs) { (key, label) ->
                    val isSelected = activeTab == key
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) accentColor else CyberSurfaceVariant,
                        modifier = Modifier.clickable { activeTab = key }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // SECTION 1: IDENTITY & CUSTOM NAME
            if (activeTab == "IDENTITY") {
                // Live Preview Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = accentColor
                ) {
                    Text(
                        text = "LIVE ASSISTANT PREVIEW",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        FuturisticOrb(
                            state = OrbState.IDLE,
                            size = 64.dp,
                            primaryAccent = accentColor
                        )

                        Column {
                            Text(
                                text = assistantNameInput.ifBlank { "Yesmin" },
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "“Hi, ${userNicknameInput.ifBlank { "Jaan" }}! I'm here for you.”",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Assistant Name Customization
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ASSISTANT NAME",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = assistantNameInput,
                        onValueChange = { assistantNameInput = it },
                        label = { Text("Assistant Name") },
                        placeholder = { Text("e.g. Yesmin, Muskan, Jarvis...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (assistantNameInput.isNotBlank()) {
                                    viewModel.updateAssistantName(assistantNameInput)
                                    Toast.makeText(context, "Saved name: $assistantNameInput", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Name", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                assistantNameInput = "Yesmin"
                                viewModel.resetAssistantNameToDefault()
                                Toast.makeText(context, "Reset to default: Yesmin", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset Default", color = TextPrimary)
                        }
                    }
                }

                // User Nickname
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "USER NICKNAME",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = userNicknameInput,
                        onValueChange = { userNicknameInput = it },
                        label = { Text("How should $assistantNameInput call you?") },
                        placeholder = { Text("e.g. Jaan, Boss, Sweetheart, Alex...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.updateSettings(settings.copy(userNickname = userNicknameInput.ifBlank { "Jaan" }))
                            Toast.makeText(context, "Nickname updated", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Nickname", color = TextPrimary)
                    }
                }

                // Personality Mode
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "AI PERSONALITY MODE",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val modes = listOf(
                        "GIRLFRIEND" to "Girlfriend / Loving Companion (Caring, playful, Hindi/Hinglish/English)",
                        "FRIENDLY" to "Friendly Assistant (Warm, conversational, cheerful)",
                        "JARVIS" to "Jarvis Executive Assistant (Sharp, structured, high-tech)",
                        "CODING" to "Coding Principal (Architecture, debugging, syntax-aware)",
                        "STUDY" to "Study Mentor (Patient explanations, step-by-step tutoring)"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        modes.forEach { (modeKey, modeDesc) ->
                            val isSelected = selectedPersonality == modeKey
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) accentColor.copy(alpha = 0.2f) else CyberSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) accentColor else Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPersonality = modeKey
                                        viewModel.updateSettings(settings.copy(personalityMode = modeKey))
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedPersonality = modeKey
                                            viewModel.updateSettings(settings.copy(personalityMode = modeKey))
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                                    )
                                    Text(
                                        text = modeDesc,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom Instructions
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "CUSTOM PERSONALITY INSTRUCTIONS",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customInstructionsInput,
                        onValueChange = { customInstructionsInput = it },
                        placeholder = { Text("e.g. Always speak affectionately in Hinglish, remind me about my diet...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.updateSettings(settings.copy(customInstructions = customInstructionsInput))
                            Toast.makeText(context, "Instructions saved", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Custom Instructions", color = TextPrimary)
                    }
                }
            }

            // SECTION 2: AI PROVIDERS
            if (activeTab == "PROVIDERS") {
                // Active Provider Selector
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ACTIVE AI PROVIDER",
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("GEMINI", "OPENAI", "ANTHROPIC", "CUSTOM").forEach { prov ->
                            val isSelected = activeProvider == prov
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) accentColor else CyberSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        activeProvider = prov
                                        viewModel.updateSettings(settings.copy(activeProvider = prov))
                                    }
                            ) {
                                Text(
                                    text = prov,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Gemini Configuration
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (activeProvider == "GEMINI") accentColor else CyberCardBorder
                ) {
                    Text(
                        text = "GOOGLE GEMINI CONFIGURATION",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = geminiKeyInput,
                        onValueChange = { geminiKeyInput = it },
                        label = { Text("Gemini API Key (Leave empty to use Secrets)") },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = geminiModelInput,
                        onValueChange = { geminiModelInput = it },
                        label = { Text("Model ID") },
                        placeholder = { Text("gemini-3.5-flash") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateSettings(
                                    settings.copy(
                                        geminiApiKey = geminiKeyInput,
                                        geminiModel = geminiModelInput.ifBlank { "gemini-3.5-flash" }
                                    )
                                )
                                Toast.makeText(context, "Gemini settings saved", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isTestingConnection = true
                                    val (success, msg) = viewModel.aiProviderManager.testConnection("GEMINI", geminiKeyInput)
                                    testResultText = msg
                                    isTestingConnection = false
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Test Connection", color = TextPrimary)
                            }
                        }
                    }
                }

                // OpenAI & Custom APIs
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (activeProvider == "OPENAI" || activeProvider == "CUSTOM") ElectricCyan else CyberCardBorder
                ) {
                    Text(
                        text = "OPENAI & CUSTOM ENDPOINTS",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = openaiKeyInput,
                        onValueChange = { openaiKeyInput = it },
                        label = { Text("OpenAI API Key") },
                        placeholder = { Text("sk-...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = customBaseUrlInput,
                        onValueChange = { customBaseUrlInput = it },
                        label = { Text("Custom API Base URL") },
                        placeholder = { Text("https://api.together.xyz/v1") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.updateSettings(
                                settings.copy(
                                    openaiApiKey = openaiKeyInput,
                                    customApiBaseUrl = customBaseUrlInput
                                )
                            )
                            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Endpoints", color = TextPrimary)
                    }
                }

                // Connection test result toast
                testResultText?.let { res ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CyberSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = res,
                            color = if (res.contains("successfully")) EmeraldMatrix else Color(0xFFF43F5E),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // SECTION 3: VOICE & SPEECH
            if (activeTab == "VOICE") {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "VOICE ENGINE PROVIDER",
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ANDROID_TTS" to "Native TTS", "ELEVENLABS" to "ElevenLabs").forEach { (vKey, vLabel) ->
                            val isSelected = voiceProvider == vKey
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) accentColor else CyberSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        voiceProvider = vKey
                                        viewModel.updateSettings(settings.copy(voiceProvider = vKey))
                                    }
                            ) {
                                Text(
                                    text = vLabel,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto speak toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto-Speak AI Responses", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Play voice automatically when message arrives", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = autoSpeakEnabled,
                            onCheckedChange = {
                                autoSpeakEnabled = it
                                viewModel.updateSettings(settings.copy(autoSpeak = it))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                        )
                    }
                }

                // ElevenLabs credentials
                AnimatedVisibility(visible = voiceProvider == "ELEVENLABS") {
                    GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = NeonPurple) {
                        Text("ELEVENLABS INTEGRATION", color = NeonPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = elevenLabsKeyInput,
                            onValueChange = { elevenLabsKeyInput = it },
                            label = { Text("ElevenLabs API Key") },
                            placeholder = { Text("xi-api-key...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = elevenLabsVoiceIdInput,
                            onValueChange = { elevenLabsVoiceIdInput = it },
                            label = { Text("Voice ID") },
                            placeholder = { Text("21m00Tcm4TlvDq8ikWAM (Rachel)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.updateSettings(
                                        settings.copy(
                                            elevenLabsApiKey = elevenLabsKeyInput,
                                            elevenLabsVoiceId = elevenLabsVoiceIdInput
                                        )
                                    )
                                    Toast.makeText(context, "ElevenLabs saved", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save")
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.voiceManager.speak(
                                        text = "Hello ${settings.userNickname}! This is ${settings.assistantName} speaking with high fidelity ElevenLabs audio.",
                                        provider = "ELEVENLABS",
                                        elevenLabsKey = elevenLabsKeyInput,
                                        voiceId = elevenLabsVoiceIdInput,
                                        speed = 1.0f,
                                        pitch = 1.05f,
                                        scope = scope
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Test Voice", color = TextPrimary)
                            }
                        }
                    }
                }

                // Voice speed slider
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("SPEECH SPEED: ${"%.1f".format(speechSpeed)}x", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = speechSpeed,
                        onValueChange = {
                            speechSpeed = it
                            viewModel.updateSettings(settings.copy(speechSpeed = it))
                        },
                        valueRange = 0.7f..1.5f,
                        steps = 7,
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                    )

                    OutlinedButton(
                        onClick = {
                            viewModel.voiceManager.speak(
                                text = "Hi, ${settings.userNickname}! Testing speech rate and tone for ${settings.assistantName}.",
                                provider = "ANDROID_TTS",
                                speed = speechSpeed,
                                pitch = 1.05f,
                                scope = scope
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Audition Voice", color = TextPrimary)
                    }
                }
            }

            // SECTION 4: APPEARANCE & ACCENT COLOR
            if (activeTab == "APPEARANCE") {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("NEON ACCENT GLOW", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    val colorOptions = listOf(
                        "#F43F5E" to "Neon Magenta",
                        "#8B5CF6" to "Cyber Purple",
                        "#06B6D4" to "Electric Cyan",
                        "#10B981" to "Matrix Green",
                        "#F59E0B" to "Amber Gold"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorOptions.forEach { (hex, name) ->
                            val color = parseAccentColor(hex)
                            val isSelected = accentHex.equals(hex, ignoreCase = true)

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) color.copy(alpha = 0.2f) else CyberSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) color else Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        accentHex = hex
                                        viewModel.updateSettings(settings.copy(accentColorHex = hex))
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Text(text = name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("APP SPECIFICATIONS & CREDITS", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Application: YESMIN AI Studio v1.0", color = TextPrimary, fontSize = 13.sp)
                    Text("Architecture: Jetpack Compose + Room + Gemini 3.5 Flash REST + ElevenLabs", color = TextSecondary, fontSize = 12.sp)
                    Text("Status: Fully Operational Native Assistant", color = EmeraldMatrix, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
