package com.example.ui.screens

import android.Manifest
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.FuturisticOrb
import com.example.ui.components.GlassCard
import com.example.ui.components.OrbState
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LiveVoiceScreen(
    viewModel: MainViewModel,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settingsFlow.collectAsState()
    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsState()
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val transcript by viewModel.liveVoiceTranscript.collectAsState()
    val aiResponse by viewModel.liveVoiceAiResponse.collectAsState()

    val accentColor = remember(settings.accentColorHex) {
        parseAccentColor(settings.accentColorHex)
    }

    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val orbState = when {
        isSpeaking -> OrbState.SPEAKING
        isListening -> OrbState.LISTENING
        transcript.isNotBlank() && !isSpeaking -> OrbState.THINKING
        else -> OrbState.IDLE
    }

    // Auto-start live voice session when screen opens if permission is granted
    LaunchedEffect(micPermissionState.status.isGranted) {
        if (micPermissionState.status.isGranted) {
            viewModel.startLiveVoiceSession()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.endLiveVoiceSession()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Bar: Connection & Assistant Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = CyberSurfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (orbState != OrbState.IDLE) EmeraldMatrix else accentColor)
                        )
                        Text(
                            text = "GEMINI LIVE • ONLINE",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                IconButton(
                    onClick = {
                        viewModel.endLiveVoiceSession()
                        onEndCall()
                    }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Live", tint = TextSecondary)
                }
            }

            // 2. Center: Assistant Identity & Big Interactive Orb
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = settings.assistantName,
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = when (orbState) {
                        OrbState.SPEAKING -> "Speaking with ${settings.userNickname}..."
                        OrbState.LISTENING -> "Listening to you... (Speak Hindi, English, Hinglish)"
                        OrbState.THINKING -> "Thinking..."
                        OrbState.IDLE -> "Tap orb or mic to speak"
                    },
                    color = if (orbState == OrbState.SPEAKING) accentColor else TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                FuturisticOrb(
                    state = orbState,
                    size = 230.dp,
                    primaryAccent = accentColor,
                    onClick = {
                        if (isSpeaking) {
                            // User interruption: stop speaking and listen
                            viewModel.interruptLiveVoice()
                        } else {
                            viewModel.startLiveVoiceSession()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Animated audio soundwaves
                LiveAudioWaveform(
                    isActive = isSpeaking || isListening,
                    accent = if (isSpeaking) accentColor else ElectricCyan,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(38.dp)
                )
            }

            // 3. Dynamic Transcription Subtitle Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = accentColor.copy(alpha = 0.4f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSpeaking && aiResponse.isNotBlank()) aiResponse else transcript.ifBlank { "Say 'Hi ${settings.assistantName}' to start talking..." },
                        color = TextPrimary,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        lineHeight = 22.sp
                    )

                    if (isSpeaking) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap to interrupt",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 4. Bottom Controls: Mute, Interrupt, End Call
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute / Unmute Mic
                IconButton(
                    onClick = {
                        if (isListening) viewModel.voiceManager.stopListening()
                        else viewModel.startLiveVoiceSession()
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceVariant)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Mute",
                        tint = if (isListening) ElectricCyan else TextTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Interruption Button
                IconButton(
                    onClick = { viewModel.interruptLiveVoice() },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = "Interrupt & Speak",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // End Call Button
                IconButton(
                    onClick = {
                        viewModel.endLiveVoiceSession()
                        onEndCall()
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE11D48))
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Live Call",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Permission request overlay if microphone is not granted
        if (!micPermissionState.status.isGranted) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = CyberBgDark.copy(alpha = 0.95f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = accentColor, modifier = Modifier.size(60.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Microphone Permission Required",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${settings.assistantName} needs microphone access to listen and talk with you in real-time.",
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { micPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Permission", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveAudioWaveform(
    isActive: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    Canvas(modifier = modifier) {
        val barCount = 24
        val totalWidth = size.width
        val barWidth = totalWidth / (barCount * 1.6f)
        val space = barWidth * 0.6f
        val maxHeight = size.height

        for (i in 0 until barCount) {
            val progress = (i.toFloat() / barCount + waveOffset) % 1f
            val amplitude = if (isActive) {
                kotlin.math.sin(progress * Math.PI.toFloat()).coerceAtLeast(0.15f)
            } else {
                0.1f
            }

            val barHeight = maxHeight * amplitude
            val x = i * (barWidth + space)
            val y = (maxHeight - barHeight) / 2f

            drawRoundRect(
                color = accent.copy(alpha = if (isActive) 0.85f else 0.25f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}
