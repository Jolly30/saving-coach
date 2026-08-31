# Expense Time Display Enhancement

## Date: 2026-08-27

## Issue

The Recent Expenses section only showed the date (e.g., "27 Aug 2026") without the time. The user requested to see the time as well.

## Root Cause

- The `Expense.date` field stores only `YYYY-MM-DD` format (no time).
- The date formatter was set to `"dd MMM yyyy"` which displays date only.
- The `Expense.createdAt` field (epoch millis) already had the full timestamp with time but was not being used for display.

## Changes Made

### File: `app/src/main/java/com/savingcoach/app/ui/expenses/ExpenseScreen.kt`

1. **Updated date formatter** (line 44):
   - Before: `"dd MMM yyyy"` → After: `"dd MMM yyyy, hh:mm a"`

2. **Updated `ExpenseItemRow` display logic** (lines 555-566):
   - Now uses `expense.createdAt` (epoch millis) to get full timestamp with time
   - Falls back to parsing `expense.date` if `createdAt` is not available

3. **Renamed parameter** in `ExpenseItemRow` function signature:
   - `dateFormatter` → `dateTimeFormatter`

4. **Updated call site** to pass `dateTimeFormatter` instead of `dateFormatter`

## Result

- **Before**: `27 Aug 2026`
- **After**: `27 Aug 2026, 02:30 PM`

## Build Status

Build successful with no new errors.
