# YESMIN AI - AI Girlfriend + Personal Jarvis Assistant

An intelligent, futuristic Android application combining an affectionate AI Companion / Girlfriend mode with a sophisticated Jarvis executive assistant, Gemini Live Voice, full AI Coding Agent, and native Android action automation.

---

## 🌟 Key Features

1. **Custom Assistant Identity & Name**
   - Default: **YESMIN AI** (display name: "Yesmin").
   - Fully customizable in **Settings → Profile & Assistant → Assistant Name** (e.g. "Muskan", "Jarvis").
   - The custom name updates dynamically across Home, Greetings ("Hi, Jaan"), Chat headers, Voice playback, Live Voice, and System instructions.
   - Persistent storage via Room database across app restarts and reboots.
   - Live avatar preview and one-tap "Reset to Default" button.

2. **AI Girlfriend & Companion Persona**
   - Affectionate, caring, playful, deeply supportive, and conversational companion.
   - Supports natural conversations in **Hindi, Hinglish, and English**.
   - Multiple personality modes:
     - **Girlfriend / Companion**: Emotional warmth, playful teasing, care for well-being.
     - **Friendly Assistant**: Upbeat, conversational, loyal companion.
     - **Jarvis Executive**: Sharp, concise, structured intelligence.
     - **Coding Assistant**: Principal engineer, syntax-aware debugging.
     - **Study Mentor**: Patient instructional tutor.

3. **Cyberpunk & Glassmorphism UI**
   - Cyber black/navy theme (`#070814`) with glassmorphism cards and glowing borders.
   - Reactive animated AI Orb (`FuturisticOrb`) with state-dependent canvas pulsations (Idle, Listening, Thinking, Speaking).
   - Dynamic neon accent customization (Neon Magenta, Cyber Purple, Electric Cyan, Matrix Green, Amber Gold).

4. **Gemini Live Voice Mode**
   - Real-time continuous voice conversation with large animated central orb.
   - Real-time waveform audio visualizer.
   - Continuous speech recognition with Hindi/English support.
   - Live user interruption handling (tap or speak to halt TTS and resume listening).
   - Mute mic toggle and End Call control.

5. **ElevenLabs & Voice Engine**
   - High-fidelity ElevenLabs neural voice synthesis via official API.
   - Native Android TextToSpeech engine fallback.
   - Pitch, speed control, auto-speak toggle, and "Audition Voice" test button.

6. **Native Android Apps & Actions Bridge**
   - Real device launches: **YouTube, WhatsApp, Telegram, Gmail, Google Maps, Chrome, Camera, Calculator, Calendar**.
   - Direct web URL navigation and Google Web Search.
   - Real system push notifications via `NotificationManager`.
   - Math expression evaluator with recursive descent parser.
   - Safe confirmation dialog for sensitive actions.

7. **AI Coding Agent & Sandbox**
   - Project and multi-file manager.
   - Syntax-highlighted code editor with monospace typography and copy actions.
   - Natural language prompt-to-code generation (e.g., "Create Firebase Login UI", "Add M3 Card").
   - Real-time terminal and build diagnostics panel.

8. **Persistent Memory System**
   - Automatic memory context injection into the AI prompt.
   - View, search, add, and clear memories (nicknames, habits, preferences).

9. **AI Providers & API Manager**
   - Gemini (`gemini-3.5-flash`, `gemini-3.1-pro-preview`).
   - OpenAI (`gpt-4o`, `gpt-4o-mini`).
   - Anthropic (`claude-3-5-sonnet`).
   - Custom OpenAI-compatible endpoints with custom base URLs.
   - Real "Test Connection" button with ping validation.

---

## 🛠️ Environment & Setup

### 1. API Keys Configuration
Create a `.env` file in the root directory or configure secrets in the AI Studio Secrets panel:
```bash
GEMINI_API_KEY=your_actual_gemini_api_key
ELEVENLABS_API_KEY=your_actual_elevenlabs_api_key
```

### 2. Building & Running the App
Run the standard Gradle debug build task:
```bash
gradle assembleDebug
```

Run local Robolectric and JVM tests:
```bash
gradle :app:testDebugUnitTest
```

---

## 🔒 Security
- API keys are injected at build time via `BuildConfig` using the Secrets Gradle Plugin.
- User-entered custom keys are stored locally on the device in Room database.
- Sensitive device actions require user confirmation.
