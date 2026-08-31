# Saving Challenge — Duration & Auto-Skip Logic

## Overview

This document covers the investigation and fix for the saving challenge duration tracking system, specifically how the creation day is handled when the user doesn't check in.

---

## Design Rule

> Each calendar day is a check-in opportunity. Once midnight passes, that day is gone — if you didn't check in, it's **skipped**.

- The challenge duration starts counting from the **creation time**, not the first check-in.
- If a user creates a challenge at 11:55 PM and doesn't check in before midnight, the creation day is **skipped**.
- The user must check in **before 12 AM** each day to count that day.

---

## Problem Statement

### User Scenario
1. Created a challenge on **Aug 26 at 11:55 PM** (Myanmar time)
2. Did **not** check in before midnight
3. At 12:00 AM on Aug 27, the Aug 26 check-in opportunity was lost
4. When opening the challenge detail on Aug 27, the UI should show:
   - Aug 26: **Skipped** (✕)
   - Aug 27: **Available** for check-in (○)

### Bug Found
The `autoSkipMissedDays()` function in `ChallengeViewModel.kt` had two issues:

1. **Early return on empty `lastDepositDate`**: When no check-in existed, `lastDepositDate = "|0|30"` had an empty date part. The function returned early without ever skipping the creation day.

2. **Wrong condition**: `if (daysDiff > 1)` meant the creation day was never skipped when only 1 day had passed.

---

## Code Analysis

### Key Files

| File | Purpose |
|---|---|
| `app/src/main/java/com/savingcoach/app/data/model/SavingChallenge.kt` | Data model with `startDate`, `endDate`, `lastDepositDate` |
| `app/src/main/java/com/savingcoach/app/ui/challenges/ChallengeViewModel.kt` | `createChallenge()`, `addDepositMock()`, `autoSkipMissedDays()` |
| `app/src/main/java/com/savingcoach/app/ui/challenges/ChallengeDetailScreen.kt` | UI screen, calls `autoSkipMissedDays()` in `LaunchedEffect` |
| `app/src/main/java/com/savingcoach/app/ui/challenges/CreateChallengeScreen.kt` | Challenge creation UI |

### Data Model (`SavingChallenge.kt`)

```kotlin
val startDate: String = ""      // YYYY-MM-DD — set at creation
val endDate: String = ""        // YYYY-MM-DD — set at creation (startDate + duration)
val lastDepositDate: String = "" // Format: "lastDepositDate|completedDaysCount|durationDays"
```

**Computed properties:**
- `durationDays`: Parses from `lastDepositDate` part[2], or computes `endDate - startDate`
- `completedDaysCount`: Parses from `lastDepositDate` part[1]

### Challenge Creation (`ChallengeViewModel.kt` line 232)

```kotlin
val challenge = SavingChallenge(
    ...
    startDate = LocalDate.now().toString(),           // Today at creation
    endDate = LocalDate.now().plusDays(duration).toString(), // Today + duration
    lastDepositDate = "|0|$duration"                  // Empty date, 0 completed
)
```

### Check-In (`addDepositMock()` line 270)

```kotlin
fun addDepositMock(challengeId, amount, completedStepsParam, ...) {
    val parts = challenge.lastDepositDate.split("|")
    val currentCompletedSteps = parts[1].toIntOrNull() ?: 0
    val lastDepositDateOnly = parts[0]  // e.g., "2026-08-26"
    val todayStr = LocalDate.now().toString()

    // Guard: prevent duplicate check-in on same day
    if (currentCompletedSteps > 0 && lastDepositDateOnly == todayStr) {
        return@launch  // Already checked in today
    }

    // Increment steps and update
    val newCompletedSteps = currentCompletedSteps + 1
    lastDepositDate = LocalDate.now().toString() + "|" + newCompletedSteps + "|" + duration
}
```

**Key insight**: The guard `lastDepositDateOnly == todayStr` blocks check-in if `lastDepositDate` is set to today. This is why the skip logic must set `lastDepositDate` to **yesterday**, not today.

### Auto-Skip (`autoSkipMissedDays()` line 330)

This function runs when the user opens the challenge detail screen (`ChallengeDetailScreen.kt` line 90):

```kotlin
LaunchedEffect(challengeId) {
    viewModel.selectChallenge(challengeId)
    viewModel.autoSkipMissedDays(challengeId)  // Runs here
}
```

**Important**: The skip is **lazy** — it only calculates missed days when the user views the challenge. There is no background service or midnight trigger.

---

## Original Code (Before Fix)

```kotlin
fun autoSkipMissedDays(challengeId: String) {
    ...
    if (lastDateStr.isEmpty()) return@launch // ❌ BUG: bails when no check-in exists

    val lastDate = LocalDate.parse(lastDateStr)
    val daysDiff = ChronoUnit.DAYS.between(lastDate, today).toInt()

    if (daysDiff > 1 && completedSteps < duration) {  // ❌ BUG: > 1 misses 1-day gap
        val daysToSkip = minOf(daysDiff - 1, duration - completedSteps)
        ...
    }
}
```

**Problems:**
1. `lastDateStr.isEmpty()` → returns early → creation day never skipped
2. `daysDiff > 1` → 1-day gap (creation day → today) not caught

---

## Fixed Code

### Changes Made to `ChallengeViewModel.kt`

**1. Fall back to `startDate` when no check-in exists:**
```kotlin
val hasCheckedInBefore = lastDateStr.isNotEmpty()
val referenceDateStr = if (hasCheckedInBefore) lastDateStr else challenge.startDate
if (referenceDateStr.isEmpty()) return@launch
```

**2. Changed condition from `>` to `>=`:**
```kotlin
if (daysDiff >= 1 && completedSteps < duration) {
```

**3. Conditional `daysToSkip` calculation:**
```kotlin
val daysToSkip = if (hasCheckedInBefore) {
    minOf(daysDiff - 1, duration - completedSteps)  // Preserve today
} else {
    minOf(daysDiff, duration - completedSteps)       // Skip creation day
}
```

**4. Loop starts from `startDate` when never checked in:**
```kotlin
val loopStart = if (hasCheckedInBefore) 1 else 0
for (i in loopStart until loopStart + daysToSkip) {
    val skipDate = lastDate.plusDays(i.toLong()).toString()
    // Create skip deposit...
}
```

**5. Set `lastDepositDate` to startDate (not today) when only creation day skipped:**
```kotlin
val newLastDepositDate = if (!hasCheckedInBefore && daysToSkip == 1) {
    // Creation day only: set to startDate so today remains available
    lastDate.toString() + "|" + newCompletedSteps + "|" + duration
} else if (!hasCheckedInBefore) {
    // Multiple days: set to last skipped day (yesterday)
    lastDate.plusDays(daysToSkip.toLong() - 1).toString() + "|" + newCompletedSteps + "|" + duration
} else {
    // Previously checked in: set to last skipped day
    lastDate.plusDays(daysToSkip.toLong()).toString() + "|" + newCompletedSteps + "|" + duration
}
```

---

## Scenario Verification

### Scenario 1: Created Aug 26, no check-in, open Aug 27 (User's case)

| Step | Value |
|---|---|
| `hasCheckedInBefore` | `false` |
| `referenceDate` | `startDate = "2026-08-26"` |
| `daysDiff` | `1` (Aug 26 → Aug 27) |
| `daysToSkip` | `min(1, 30) = 1` |
| `loopStart` | `0` |
| Skip deposit created for | `Aug 26 + 0 = "2026-08-26"` |
| `lastDepositDate` | `"2026-08-26|1|30"` (startDate) |

**Check-in on Aug 27:**
```
lastDepositDateOnly = "2026-08-26"
todayStr = "2026-08-27"
guard: "2026-08-26" == "2026-08-27" → false → CHECK-IN ALLOWED ✅
```

**Final state:**
| Day | Status |
|---|---|
| Aug 26 | ✕ Skipped |
| Aug 27 | ○ Available for check-in |
| Remaining | 29 steps |

### Scenario 2: Created Aug 26, no check-in, open Aug 28

| Step | Value |
|---|---|
| `hasCheckedInBefore` | `false` |
| `daysDiff` | `2` |
| `daysToSkip` | `2` |
| Skip deposits | Aug 26, Aug 27 |
| `lastDepositDate` | `"2026-08-27|2|30"` (yesterday) |

**Check-in on Aug 28:** Allowed ✅

### Scenario 3: Created Aug 26, checked in Aug 26, open Aug 27

| Step | Value |
|---|---|
| `hasCheckedInBefore` | `true` |
| `daysDiff` | `1` |
| `daysToSkip` | `min(0, 29) = 0` |

**Result:** Nothing skipped. Aug 27 available ✅

### Scenario 4: Created Aug 26, checked in Aug 26, open Aug 28 (missed Aug 27)

| Step | Value |
|---|---|
| `hasCheckedInBefore` | `true` |
| `daysDiff` | `2` |
| `daysToSkip` | `min(1, 29) = 1` |
| `loopStart` | `1` |
| Skip deposit | Aug 27 |
| `lastDepositDate` | `"2026-08-27|1|30"` |

**Check-in on Aug 28:** Blocked (correct — Aug 27 skipped, today not yet available via skip logic)
**Check-in on Aug 29:** Allowed ✅

---

## Important Notes

### When Does the Skip Happen?

The skip is **NOT real-time at midnight**. It only runs when:
- User taps into the **challenge detail screen**
- `LaunchedEffect(challengeId)` triggers `autoSkipMissedDays()`

| Event | Skip triggers? |
|---|---|
| Midnight | ❌ No background service |
| App launch (main screen) | ❌ Only loads list |
| Challenges list screen | ❌ Doesn't open detail |
| **Open challenge detail** | ✅ `autoSkipMissedDays` runs |

### Dual-Tracking System

The app uses two parallel mechanisms:

| Mechanism | Starts From | Behavior |
|---|---|---|
| **Calendar deadline** (`startDate` → `endDate`) | Creation date | Hard wall — if today passes `endDate`, challenge auto-fails |
| **Step counter** (`completedDaysCount` / `durationDays`) | Creation date | Challenge completes when completed steps ≥ total duration |

### `lastDepositDate` Format

```
"YYYY-MM-DD|completedDaysCount|durationDays"
```

Example: `"2026-08-26|1|30"` means:
- Last activity on Aug 26
- 1 day completed
- 30-day challenge

Initial value: `"|0|30"` (empty date, 0 completed, 30 duration)

---

## Files Modified

| File | Line(s) | Change |
|---|---|---|
| `ChallengeViewModel.kt` | 330-395 | `autoSkipMissedDays()` — handle creation-day skip |

---

## Testing Checklist

- [ ] Create challenge at 11:55 PM, don't check in, open detail next day → creation day skipped
- [ ] Check in on creation day, open detail next day → nothing skipped
- [ ] Create challenge, skip 2+ days, open detail → all missed days skipped
- [ ] Check-in works after skip (not blocked by skip deposit)
- [ ] Existing challenges with deposits still work correctly
- [ ] Challenge completion still triggers when all steps done
- [ ] Challenge failure still triggers when timeline ends
