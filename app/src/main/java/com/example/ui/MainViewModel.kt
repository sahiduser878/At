package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.actions.ActionResult
import com.example.data.actions.NativeActionManager
import com.example.data.api.AiProviderManager
import com.example.data.audio.VoiceManager
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import com.example.ui.components.OrbState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val settingsDao = database.userSettingsDao()
    private val chatDao = database.chatDao()
    private val memoryDao = database.memoryDao()
    private val projectDao = database.projectDao()
    private val taskDao = database.taskDao()

    val actionManager = NativeActionManager(application)
    val voiceManager = VoiceManager(application)
    val aiProviderManager = AiProviderManager(application)

    // Current user settings flow
    val settingsFlow: StateFlow<UserSettingsEntity> = settingsDao.getSettingsFlow()
        .map { it ?: UserSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettingsEntity())

    // Chat
    val conversationsFlow = chatDao.getConversationsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeConversationId = MutableStateFlow<Long?>(null)
    val activeConversationId = _activeConversationId.asStateFlow()

    val currentMessagesFlow: StateFlow<List<MessageEntity>> = _activeConversationId
        .flatMapLatest { id ->
            if (id != null) chatDao.getMessagesFlow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI generation states
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _liveVoiceActive = MutableStateFlow(false)
    val liveVoiceActive = _liveVoiceActive.asStateFlow()

    private val _liveVoiceTranscript = MutableStateFlow("")
    val liveVoiceTranscript = _liveVoiceTranscript.asStateFlow()

    private val _liveVoiceAiResponse = MutableStateFlow("")
    val liveVoiceAiResponse = _liveVoiceAiResponse.asStateFlow()

    // Confirmation dialog state for sensitive actions
    private val _pendingActionConfirmation = MutableStateFlow<Pair<String, () -> Unit>?>(null)
    val pendingActionConfirmation = _pendingActionConfirmation.asStateFlow()

    // Status snackbar / toast
    private val _statusNotice = MutableStateFlow<String?>(null)
    val statusNotice = _statusNotice.asStateFlow()

    // Memories
    val memoriesFlow = memoryDao.getAllMemoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Projects
    val projectsFlow = projectDao.getAllProjectsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId = _selectedProjectId.asStateFlow()

    val projectFilesFlow = _selectedProjectId
        .flatMapLatest { id ->
            if (id != null) projectDao.getFilesForProjectFlow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tasks
    val tasksFlow = taskDao.getAllTasksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initialize default settings and sample conversation if empty
        viewModelScope.launch {
            val existing = settingsDao.getSettings()
            if (existing == null) {
                settingsDao.saveSettings(UserSettingsEntity())
            }
            initSampleDataIfNeeded()
        }
    }

    private suspend fun initSampleDataIfNeeded() {
        val convos = chatDao.getConversationsFlow().first()
        if (convos.isEmpty()) {
            val currentSettings = settingsDao.getSettings() ?: UserSettingsEntity()
            val convId = chatDao.insertConversation(
                ConversationEntity(
                    title = "Welcome to ${currentSettings.assistantName} AI",
                    isPinned = true
                )
            )
            _activeConversationId.value = convId
            chatDao.insertMessage(
                MessageEntity(
                    conversationId = convId,
                    role = "assistant",
                    content = "Hi ${currentSettings.userNickname}! I'm ${currentSettings.assistantName}, your personal AI companion and Jarvis assistant. I'm right here with you! What shall we do today?"
                )
            )

            // Seed initial sample memory
            memoryDao.insertMemory(
                MemoryEntity(
                    key = "Nickname",
                    content = "Calls user '${currentSettings.userNickname}' with deep warmth and care",
                    category = "Personal"
                )
            )

            // Seed initial sample project
            val projId = projectDao.insertProject(
                ProjectEntity(
                    title = "Android Jarvis Assistant",
                    type = "Android App",
                    description = "Personal multi-agent companion app with Jetpack Compose & Gemini"
                )
            )
            projectDao.insertFile(
                ProjectFileEntity(
                    projectId = projId,
                    fileName = "MainScreen.kt",
                    filePath = "app/src/main/java/MainScreen.kt",
                    content = """
                        @Composable
                        fun MainScreen() {
                            Text("Welcome to Yesmin AI Studio!")
                        }
                    """.trimIndent(),
                    language = "kotlin"
                )
            )

            // Seed initial sample task
            taskDao.insertTask(
                TaskEntity(
                    title = "Explore Live Voice Mode",
                    notes = "Test real-time conversation and voice interruption",
                    priority = "High",
                    isReminder = true
                )
            )
        } else {
            _activeConversationId.value = convos.first().id
        }
    }

    fun selectConversation(id: Long) {
        _activeConversationId.value = id
    }

    fun createNewConversation(title: String = "New Chat") {
        viewModelScope.launch {
            val id = chatDao.insertConversation(ConversationEntity(title = title))
            _activeConversationId.value = id
            val currentSettings = settingsFlow.value
            chatDao.insertMessage(
                MessageEntity(
                    conversationId = id,
                    role = "assistant",
                    content = "I'm listening, ${currentSettings.userNickname}. How can I assist you right now?"
                )
            )
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            chatDao.deleteMessagesForConversation(id)
            chatDao.deleteConversation(id)
            val remaining = chatDao.getConversationsFlow().first()
            _activeConversationId.value = remaining.firstOrNull()?.id
        }
    }

    fun sendMessage(
        text: String,
        imageUriString: String? = null
    ) {
        if (text.isBlank() && imageUriString == null) return
        val convId = _activeConversationId.value ?: return

        viewModelScope.launch {
            // 1. Insert user message
            chatDao.insertMessage(
                MessageEntity(
                    conversationId = convId,
                    role = "user",
                    content = text,
                    attachedImageUri = imageUriString
                )
            )

            // Update conversation title if default
            val currentConvo = chatDao.getConversationById(convId)
            if (currentConvo != null && currentConvo.title.startsWith("New Conversation")) {
                val newTitle = text.take(30).ifBlank { "Image Analysis" }
                chatDao.updateConversation(currentConvo.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
            }

            _isGenerating.value = true

            // Gather context
            val currentSettings = settingsFlow.value
            val memories = if (currentSettings.memoryEnabled) memoryDao.getApprovedMemories() else emptyList()
            val history = chatDao.getMessagesFlow(convId).first().map { it.role to it.content }

            // 2. Call AI Provider
            val responseText = aiProviderManager.generateResponse(
                prompt = text,
                imageUriString = imageUriString,
                chatHistory = history,
                settings = currentSettings,
                memories = memories
            )

            _isGenerating.value = false

            // 3. Process potential action tags
            val actionResult = parseAndExecuteActions(responseText)

            // 4. Insert assistant message
            chatDao.insertMessage(
                MessageEntity(
                    conversationId = convId,
                    role = "assistant",
                    content = responseText,
                    actionType = actionResult?.first,
                    actionPayload = actionResult?.second
                )
            )

            // Auto speak if enabled
            if (currentSettings.autoSpeak) {
                voiceManager.speak(
                    text = responseText,
                    provider = currentSettings.voiceProvider,
                    elevenLabsKey = currentSettings.elevenLabsApiKey,
                    voiceId = currentSettings.elevenLabsVoiceId,
                    speed = currentSettings.speechSpeed,
                    pitch = currentSettings.speechPitch,
                    scope = viewModelScope
                )
            }
        }
    }

    private fun parseAndExecuteActions(response: String): Pair<String, String>? {
        val actionRegex = Regex("\\[ACTION:([A-Z_]+):([^\\]]+)\\]")
        val match = actionRegex.find(response) ?: return null
        val actionType = match.groupValues[1]
        val payload = match.groupValues[2]

        when (actionType) {
            "OPEN_APP" -> actionManager.openApp(payload)
            "OPEN_URL" -> actionManager.openUrl(payload)
            "SEARCH_WEB" -> actionManager.searchWeb(payload)
            "CALCULATE" -> actionManager.calculateMath(payload)
            "REMINDER" -> {
                viewModelScope.launch {
                    taskDao.insertTask(TaskEntity(title = payload, isReminder = true))
                    actionManager.sendNotification(
                        title = "${settingsFlow.value.assistantName} Reminder",
                        message = payload
                    )
                }
            }
            "SAVE_MEMORY" -> {
                viewModelScope.launch {
                    val parts = payload.split(":", limit = 2)
                    val key = parts.getOrNull(0) ?: "Memory"
                    val value = parts.getOrNull(1) ?: payload
                    memoryDao.insertMemory(MemoryEntity(key = key, content = value, category = "General"))
                }
            }
        }

        return actionType to payload
    }

    // Live Voice Mode
    fun startLiveVoiceSession() {
        voiceManager.stopSpeaking()
        _liveVoiceActive.value = true
        _liveVoiceTranscript.value = "Listening to you..."

        voiceManager.startListening(
            onResult = { recognizedText ->
                _liveVoiceTranscript.value = recognizedText
                handleLiveVoiceInput(recognizedText)
            },
            onError = { error ->
                _liveVoiceTranscript.value = "Say something..."
            }
        )
    }

    private fun handleLiveVoiceInput(input: String) {
        if (input.isBlank()) return
        viewModelScope.launch {
            val currentSettings = settingsFlow.value
            val memories = if (currentSettings.memoryEnabled) memoryDao.getApprovedMemories() else emptyList()

            _liveVoiceAiResponse.value = "Thinking..."
            val reply = aiProviderManager.generateResponse(
                prompt = input,
                imageUriString = null,
                chatHistory = listOf("user" to input),
                settings = currentSettings,
                memories = memories
            )
            _liveVoiceAiResponse.value = reply

            // Speak reply
            voiceManager.speak(
                text = reply,
                provider = currentSettings.voiceProvider,
                elevenLabsKey = currentSettings.elevenLabsApiKey,
                voiceId = currentSettings.elevenLabsVoiceId,
                speed = currentSettings.speechSpeed,
                pitch = currentSettings.speechPitch,
                scope = viewModelScope
            )

            // Also record in active conversation
            val convId = _activeConversationId.value
            if (convId != null) {
                chatDao.insertMessage(MessageEntity(conversationId = convId, role = "user", content = "[Live Voice] $input"))
                chatDao.insertMessage(MessageEntity(conversationId = convId, role = "assistant", content = reply))
            }
        }
    }

    fun interruptLiveVoice() {
        voiceManager.stopSpeaking()
        // Automatically restart listening
        startLiveVoiceSession()
    }

    fun endLiveVoiceSession() {
        _liveVoiceActive.value = false
        voiceManager.stopSpeaking()
        voiceManager.stopListening()
    }

    fun getOrbState(): OrbState {
        return when {
            voiceManager.isSpeaking.value -> OrbState.SPEAKING
            _isGenerating.value -> OrbState.THINKING
            voiceManager.isListening.value -> OrbState.LISTENING
            else -> OrbState.IDLE
        }
    }

    // Settings Updates
    fun updateAssistantName(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val updated = settingsFlow.value.copy(assistantName = newName.trim())
            settingsDao.saveSettings(updated)
            _statusNotice.value = "Assistant name updated to ${newName.trim()}"
        }
    }

    fun resetAssistantNameToDefault() {
        viewModelScope.launch {
            val updated = settingsFlow.value.copy(assistantName = "Yesmin")
            settingsDao.saveSettings(updated)
            _statusNotice.value = "Assistant name reset to Yesmin"
        }
    }

    fun updateSettings(updated: UserSettingsEntity) {
        viewModelScope.launch {
            settingsDao.saveSettings(updated)
            _statusNotice.value = "Settings saved successfully"
        }
    }

    fun clearStatusNotice() {
        _statusNotice.value = null
    }

    fun dismissActionConfirmation() {
        _pendingActionConfirmation.value = null
    }

    // Memory operations
    fun addMemory(key: String, content: String, category: String = "General") {
        viewModelScope.launch {
            memoryDao.insertMemory(MemoryEntity(key = key, content = content, category = category))
            _statusNotice.value = "Memory saved"
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            memoryDao.deleteMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            memoryDao.clearAllMemories()
            _statusNotice.value = "All memories cleared"
        }
    }

    // Task operations
    fun addTask(title: String, notes: String = "", priority: String = "Medium") {
        viewModelScope.launch {
            taskDao.insertTask(TaskEntity(title = title, notes = notes, priority = priority))
        }
    }

    fun toggleTask(id: Long, completed: Boolean) {
        viewModelScope.launch {
            taskDao.setTaskCompleted(id, completed)
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            taskDao.deleteTask(id)
        }
    }

    // Projects & Coding operations
    fun selectProject(id: Long) {
        _selectedProjectId.value = id
    }

    fun createProject(title: String, type: String, description: String) {
        viewModelScope.launch {
            val id = projectDao.insertProject(ProjectEntity(title = title, type = type, description = description))
            _selectedProjectId.value = id
            // Add initial template file
            projectDao.insertFile(
                ProjectFileEntity(
                    projectId = id,
                    fileName = if (type == "Android App") "MainActivity.kt" else "index.html",
                    filePath = "src/main",
                    content = if (type == "Android App") "// Android App template\nclass App : Application() {}" else "<!DOCTYPE html>\n<html><body><h1>Project: $title</h1></body></html>",
                    language = if (type == "Android App") "kotlin" else "html"
                )
            )
        }
    }

    fun saveProjectFile(file: ProjectFileEntity) {
        viewModelScope.launch {
            projectDao.updateFile(file.copy(updatedAt = System.currentTimeMillis()))
            _statusNotice.value = "File saved"
        }
    }

    fun createProjectFile(projectId: Long, name: String, content: String, language: String = "kotlin") {
        viewModelScope.launch {
            projectDao.insertFile(
                ProjectFileEntity(
                    projectId = projectId,
                    fileName = name,
                    filePath = "src/",
                    content = content,
                    language = language
                )
            )
        }
    }

    fun deleteProjectFile(fileId: Long) {
        viewModelScope.launch {
            projectDao.deleteFile(fileId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
