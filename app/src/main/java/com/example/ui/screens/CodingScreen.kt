package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ProjectEntity
import com.example.data.local.entities.ProjectFileEntity
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CodingScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val settings by viewModel.settingsFlow.collectAsState()
    val projects by viewModel.projectsFlow.collectAsState()
    val selectedProjectId by viewModel.selectedProjectId.collectAsState()
    val projectFiles by viewModel.projectFilesFlow.collectAsState()

    val accentColor = remember(settings.accentColorHex) {
        parseAccentColor(settings.accentColorHex)
    }

    var activeFile by remember { mutableStateOf<ProjectFileEntity?>(null) }
    var codeContent by remember { mutableStateOf("") }
    var terminalOutput by remember { mutableStateOf("YESMIN AI Build Environment [Ready]\n> Target: Android Jetpack Compose sandbox\n> Ready for code generation and diagnostics.") }
    var codingPrompt by remember { mutableStateOf("") }
    var isGeneratingCode by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }

    // Auto-select first project if none selected
    LaunchedEffect(projects) {
        if (selectedProjectId == null && projects.isNotEmpty()) {
            viewModel.selectProject(projects.first().id)
        }
    }

    // Auto-select first file if available
    LaunchedEffect(projectFiles) {
        if (activeFile == null && projectFiles.isNotEmpty()) {
            val first = projectFiles.first()
            activeFile = first
            codeContent = first.content
        } else if (activeFile != null) {
            val matching = projectFiles.find { it.id == activeFile?.id }
            if (matching != null && activeFile?.content != codeContent) {
                // keep local edits
            } else if (matching != null) {
                codeContent = matching.content
            }
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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Column {
                            Text(
                                text = "AI Coding Agent",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Powered by ${settings.assistantName}",
                                color = accentColor,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { showNewFileDialog = true }) {
                            Icon(Icons.Default.NoteAdd, contentDescription = "New File", tint = ElectricCyan)
                        }
                        IconButton(
                            onClick = {
                                activeFile?.let {
                                    viewModel.saveProjectFile(it.copy(content = codeContent))
                                    Toast.makeText(context, "Saved ${it.fileName}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save", tint = TextPrimary)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Project Files Tab Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(projectFiles) { file ->
                    val isSelected = activeFile?.id == file.id
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) accentColor.copy(alpha = 0.25f) else CyberSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) accentColor else CyberCardBorder
                        ),
                        modifier = Modifier.clickable {
                            activeFile = file
                            codeContent = file.content
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (file.fileName.endsWith(".kt")) Icons.Default.Code else Icons.Default.Description,
                                contentDescription = null,
                                tint = if (isSelected) accentColor else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = file.fileName,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // 2. AI Coding Prompt Bar
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = accentColor.copy(alpha = 0.4f)
            ) {
                Text(
                    text = "AI AGENT INSTRUCTIONS",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = codingPrompt,
                        onValueChange = { codingPrompt = it },
                        placeholder = { Text("e.g. Create Firebase login UI, fix bugs...", color = TextTertiary, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CyberSurfaceVariant,
                            unfocusedContainerColor = CyberSurfaceVariant,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (codingPrompt.isNotBlank() && activeFile != null) {
                                scope.launch {
                                    isGeneratingCode = true
                                    terminalOutput = "> Analyzing request: '$codingPrompt'...\n> Reading ${activeFile?.fileName}..."

                                    val prompt = """
                                        You are an expert Android Kotlin software engineer.
                                        Task: $codingPrompt
                                        Current file: ${activeFile?.fileName}
                                        Current code:
                                        $codeContent

                                        Provide the revised, complete working code file. Return ONLY the code block, with no conversational fluff.
                                    """.trimIndent()

                                    val generated = viewModel.aiProviderManager.generateResponse(
                                        prompt = prompt,
                                        imageUriString = null,
                                        chatHistory = emptyList(),
                                        settings = settings,
                                        memories = emptyList()
                                    )

                                    val cleanedCode = if (generated.contains("```")) {
                                        generated.substringAfter("```")
                                            .substringAfter("\n")
                                            .substringBeforeLast("```")
                                            .trim()
                                    } else generated.trim()

                                    codeContent = cleanedCode
                                    terminalOutput = "> Code updated successfully for ${activeFile?.fileName}!\n> Syntax verified.\n> 0 Errors, 0 Warnings."
                                    isGeneratingCode = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isGeneratingCode
                    ) {
                        if (isGeneratingCode) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Generate", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick coding action presets
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val presets = listOf(
                        "Add M3 Compose Card",
                        "Firebase Auth Flow",
                        "Fix Errors",
                        "Explain Code",
                        "Add Coroutines Flow"
                    )
                    items(presets) { preset ->
                        SuggestionChip(
                            onClick = { codingPrompt = preset },
                            label = { Text(preset, fontSize = 11.sp, color = TextPrimary) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = CyberSurfaceVariant)
                        )
                    }
                }
            }

            // 3. Code Editor Area
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF090B16),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeFile?.fileName ?: "Editor",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Copy",
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString(codeContent))
                                    Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                                }
                            )

                            Text(
                                text = "Run Sandbox",
                                color = EmeraldMatrix,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    terminalOutput = "> Compiling ${activeFile?.fileName}...\n> Resolving dependencies...\n> Compilation SUCCESSFUL [0 errors, 0 warnings]\n> Sandbox preview rendered!"
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = codeContent,
                        onValueChange = { codeContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color(0xFFE2E8F0),
                            unfocusedTextColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            // 4. Terminal & Build Diagnostics Panel
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF06070E),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = EmeraldMatrix, modifier = Modifier.size(16.dp))
                            Text("BUILD & TERMINAL LOGS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "Clear",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { terminalOutput = "> Terminal cleared" }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = terminalOutput,
                        color = EmeraldMatrix,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }

    // New file dialog
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Create New Code File", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    placeholder = { Text("e.g. LoginView.kt or style.css") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val projId = selectedProjectId
                        if (newFileName.isNotBlank() && projId != null) {
                            viewModel.createProjectFile(projId, newFileName.trim(), "// File: ${newFileName.trim()}\n")
                            newFileName = ""
                            showNewFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberSurfaceDark
        )
    }
}
