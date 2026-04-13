# Botinis Android

AI-powered English conversation practice app for Spanish speakers.

## Features

- 🎙️ Voice-based conversation with AI characters
- 📊 Real-time grammar feedback in Spanish
- 🎮 Gamification: XP, levels, achievements, streaks
- 🌍 8 scenarios across 3 CEFR levels (A2, B1, B2)
- 🌙 Dark mode support

## Tech Stack

- **Kotlin** + **Jetpack Compose**
- **Hilt** for DI
- **Room** for local persistence
- **Retrofit** + **OkHttp** for API calls
- **Groq API** (Whisper + Llama 3.1)
- **Edge TTS** for voice synthesis

## Setup

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Groq API key

### Build

1. Clone the repo
2. Add your Groq API key to `local.properties`:
   ```
   GROQ_API_KEY=your_api_key_here
   ```
3. Open in Android Studio and sync Gradle
4. Run on emulator or device (API 26+)

## CI/CD

APK builds automatically via GitHub Actions.

### Secrets Required
- `GROQ_API_KEY` - Your Groq API key

### Triggers
- Push to `main` or `develop`
- Pull requests
- Manual trigger (`workflow_dispatch`)

### Artifacts
- Debug APK on every push
- Release APK + GitHub Release on main branch push

## Architecture

```
app/
├── data/
│   ├── model/        # Data classes
│   ├── remote/       # Retrofit API client
│   ├── local/        # Room database
│   └── repository/   # Data repositories
├── domain/           # Business logic
└── ui/               # Compose screens
    ├── theme/
    ├── navigation/
    └── screens/
```

## Project Status

🚧 **In Development** - Core structure complete, conversation flow in progress.
