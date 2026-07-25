# 👤 Dev 2 — Work Log

> **Role:** AI Chat + Receipt Scanner  
> **Branch:** `feature/ai-chat-receipt`

---

## 📋 Current Task

| Task | Status | Started | Notes |
|------|--------|---------|-------|
| —    | ⏳ Pending | — | — |

---

## ✅ Completed

| # | Task | Files | Done |
|---|------|-------|------|
| — | —    | —     | —   |

---

## ❌ Not Done / Blocked

| # | Task | Blocked By | Why |
|---|------|------------|-----|
| — | —    | —          | —   |

---

## 🚧 In Progress

| # | Task | Started | Notes |
|---|------|---------|-------|
| — | —    | —       | —     |

---

## 🔜 Up Next

- [ ] `ai/GeminiClient.kt` — Gemini API wrapper
- [ ] `ai/ChatParser.kt` — NLP → structured expense
- [ ] `ai/ReceiptScanner.kt` — Vision receipt reader
- [ ] `ui/chat/ChatScreen.kt` — Chat UI
- [ ] `ui/chat/ChatViewModel.kt` — Chat state
- [ ] `ui/camera/CameraScreen.kt` — CameraX receipt capture
- [ ] `ui/camera/CameraViewModel.kt` — Camera state

---

## 📦 Depends On Dev 1
- ✅ `ChatMessage.kt` + `ParsedExpense` (data model)
- ✅ `ChatRepository` interface (for saving chat history)

## 🔗 Interfaces You Code Against
```kotlin
interface ChatRepository {
    fun getChatHistory(userId: String): Flow<List<ChatMessage>>
    suspend fun saveMessage(userId: String, message: ChatMessage)
}
```
> **Note:** Dev 3 implements the real Firestore version later. Your code uses `ChatRepository` interface — the mock is already wired.

---

## 📝 Scratch Notes
```
```
