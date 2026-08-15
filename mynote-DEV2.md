# Developer 2 (Dev 2) - Personal Notes & Project Report

## Feature Pivot
- **Dropped:** Receipt Scanner & Camera features.
- **Implemented:** Voice Input (Speech-to-Text) and Natural Language Expense Parser for chat logging.

## Features & Files Changed

### 🎙️ 1. Voice Input Integration
- **`AndroidManifest.xml`**: Added `android.permission.RECORD_AUDIO` permission.
- **`app/src/main/java/com/savingcoach/app/ui/chat/ChatScreen.kt`**: 
  - Integrated native Android `SpeechRecognizer` using `rememberLauncherForActivityResult`.
  - Configured language to Burmese (`my-MM`) with automatic English fallback.
  - Linked to the Microphone button UI.
- **`app/src/main/java/com/savingcoach/app/ui/chat/ChatViewModel.kt`**: 
  - Hoisted `inputText` state as a `StateFlow`.
  - Implemented `updateInputText` and `appendVoiceText` to handle transcriptions securely without breaking Compose state.

### 🧠 2. Dual-Language Support & Prompt Injection
- **`app/src/main/java/com/savingcoach/app/ai/PromptBuilder.kt`**: 
  - Overhauled System Prompt to strictly mirror the user's language (English/Burmese).
  - Added a new JSON rule for Expense Logging formatting (`amount`, `category`, `merchant`, `isExpense`).
- **`app/src/main/java/com/savingcoach/app/ai/AiChatRepository.kt`**: 
  - Injected the System Prompt as the very first "user" message in the conversation history list to bypass the Vercel proxy dropping the system prompt parameter.
- **`app/src/main/java/com/savingcoach/app/ai/AiFinanceAssistant.kt`**:
  - Implemented logic facade to connect ChatViewModel requests to the ChatRepository.

### 🧮 3. Natural Language Expense Parser
- **`app/src/main/java/com/savingcoach/app/ai/ChatParser.kt`**: 
  - Created `ParsedExpense` data class.
  - Wrote `parseExpense()` with a Regex block (`\{.*\}`) to extract the JSON payload.
  - Wrote custom `convertBurmeseNumerals` helper to intercept AI responses and translate Myanmar digits (၀-၉) to Arabic numerals (0-9) to prevent JSON deserialization crashes.

### 🐛 4. Bug Fixes (Hilt Injection)
- **`app/src/main/java/com/savingcoach/app/di/RepositoryModule.kt`**: 
  - Updated `@Binds` target from `MockChatRepository` to `AiChatRepository` to completely disable the mocked text engine and connect the UI directly to the live Vercel proxy.

---
**Status:** All tasks tested, compiled, and finalized. Branch `feature/ai-chat-receipt` is ready for PR.
