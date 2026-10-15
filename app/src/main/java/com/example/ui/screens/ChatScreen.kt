package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entities.MessageEntity
import com.example.ui.MainViewModel
import com.example.ui.components.FuturisticOrb
import com.example.ui.components.GlassCard
import com.example.ui.components.OrbState
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val settings by viewModel.settingsFlow.collectAsState()
    val messages by viewModel.currentMessagesFlow.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsState()
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val partialSpeech by viewModel.voiceManager.speechPartialText.collectAsState()

    val accentColor = remember(settings.accentColorHex) {
        parseAccentColor(settings.accentColorHex)
    }

    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val listState = rememberLazyListState()

    // Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
    }

    // Scroll to bottom on new message
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Update input text if speech recognizer delivers partial text
    LaunchedEffect(partialSpeech) {
        if (partialSpeech.isNotBlank() && isListening) {
            inputText = partialSpeech
        }
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
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }

                        FuturisticOrb(
                            state = when {
                                isSpeaking -> OrbState.SPEAKING
                                isGenerating -> OrbState.THINKING
                                else -> OrbState.IDLE
                            },
                            size = 36.dp,
                            primaryAccent = accentColor
                        )

                        Column {
                            Text(
                                text = settings.assistantName,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSpeaking) accentColor else EmeraldMatrix)
                                )
                                Text(
                                    text = when {
                                        isSpeaking -> "Speaking..."
                                        isGenerating -> "Thinking..."
                                        else -> "Active Companion"
                                    },
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Row {
                        if (isSpeaking) {
                            IconButton(onClick = { viewModel.voiceManager.stopSpeaking() }) {
                                Icon(Icons.Default.VolumeOff, contentDescription = "Mute", tint = accentColor)
                            }
                        }
                        IconButton(onClick = { viewModel.createNewConversation() }) {
                            Icon(Icons.Default.AddComment, contentDescription = "New Chat", tint = TextPrimary)
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Chat Input Bar
            Surface(
                color = CyberSurfaceDark,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Preview attached image if any
                    AnimatedVisibility(visible = selectedImageUri != null) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .size(70.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Attached image",
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Image attachment button
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Attach photo", tint = TextSecondary)
                        }

                        // Speech-to-text mic button
                        IconButton(
                            onClick = {
                                if (isListening) {
                                    viewModel.voiceManager.stopListening()
                                } else {
                                    viewModel.voiceManager.startListening(
                                        onResult = { text ->
                                            inputText = text
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isListening) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Voice input",
                                tint = if (isListening) accentColor else TextSecondary
                            )
                        }

                        // Text input field
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Message ${settings.assistantName}...", color = TextTertiary, fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CyberSurfaceVariant,
                                unfocusedContainerColor = CyberSurfaceVariant,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = CyberCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            maxLines = 4
                        )

                        // Send button
                        IconButton(
                            onClick = {
                                val text = inputText
                                val img = selectedImageUri?.toString()
                                if (text.isNotBlank() || img != null) {
                                    viewModel.sendMessage(text, img)
                                    inputText = ""
                                    selectedImageUri = null
                                }
                            },
                            enabled = !isGenerating && (inputText.isNotBlank() || selectedImageUri != null),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (!isGenerating && (inputText.isNotBlank() || selectedImageUri != null)) accentColor else CyberSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (!isGenerating && (inputText.isNotBlank() || selectedImageUri != null)) Color.White else TextTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    assistantName = settings.assistantName,
                    accentColor = accentColor,
                    onSpeak = {
                        viewModel.voiceManager.speak(
                            text = message.content,
                            provider = settings.voiceProvider,
                            elevenLabsKey = settings.elevenLabsApiKey,
                            voiceId = settings.elevenLabsVoiceId,
                            speed = settings.speechSpeed,
                            pitch = settings.speechPitch,
                            scope = scope
                        )
                    },
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(message.content))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onExecuteAction = { actionType, payload ->
                        when (actionType) {
                            "OPEN_APP" -> viewModel.actionManager.openApp(payload)
                            "OPEN_URL" -> viewModel.actionManager.openUrl(payload)
                            "SEARCH_WEB" -> viewModel.actionManager.searchWeb(payload)
                            "CALCULATE" -> viewModel.actionManager.calculateMath(payload)
                        }
                    }
                )
            }

            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = accentColor,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "${settings.assistantName} is thinking...",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageEntity,
    assistantName: String,
    accentColor: Color,
    onSpeak: () -> Unit,
    onCopy: () -> Unit,
    onExecuteAction: (String, String) -> Unit
) {
    val isUser = message.role == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Role header
        Text(
            text = if (isUser) "You" else assistantName,
            color = if (isUser) TextTertiary else accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        // Attached image preview if user sent photo
        if (!message.attachedImageUri.isNullOrBlank()) {
            AsyncImage(
                model = message.attachedImageUri,
                contentDescription = "User attachment",
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .size(180.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(14.dp))
            )
        }

        // Bubble container
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) accentColor.copy(alpha = 0.22f) else CyberSurfaceDark,
            border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder) else androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Parse code blocks vs regular text
                val content = message.content
                if (content.contains("```")) {
                    FormattedTextWithCode(content, accentColor)
                } else {
                    Text(
                        text = content,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                // If action was executed or requested, show quick action badge
                if (!message.actionType.isNullOrBlank() && !message.actionPayload.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AssistChip(
                        onClick = { onExecuteAction(message.actionType, message.actionPayload) },
                        label = {
                            Text(
                                text = "Execute: ${message.actionType.lowercase()} (${message.actionPayload})",
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = CyberSurfaceVariant)
                    )
                }

                // Action buttons on bottom of assistant bubble (Listen, Copy)
                if (!isUser) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onSpeak, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormattedTextWithCode(fullText: String, accentColor: Color) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val parts = fullText.split("```")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Code block
                val lines = part.trim().lines()
                val lang = lines.firstOrNull()?.trim() ?: "code"
                val code = if (lines.size > 1) lines.drop(1).joinToString("\n") else part

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0A0C18),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = lang.uppercase(), color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Copy",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString(code))
                                    Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = code,
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                if (part.isNotBlank()) {
                    Text(text = part, color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}
