# Saving Coach - Calendar Refactor Notes

This document summarizes all the recent updates, bug fixes, and logic changes made to the Calendar and History screens.

## 1. Calendar Layout & Navigation
* **25-Month Range:** The calendar now renders a rolling 25-month range (12 months in the past, the current month, and 12 months in the future) instead of just the current month.
* **Auto-Scroll to Current Date:** Added logic to automatically scroll the calendar to the current month/year when the data first loads.
* **Year-to-Month Navigation Fix:** Fixed a bug where clicking a specific month from the Year view would ignore your tap and force you back to the current month. The auto-scroll logic was tweaked to only run *once* on initial load so it doesn't fight your manual navigation.
* **UI Spacing:** Fixed the "weird" layout in the Year view by adding proper null-padding and spacing between the mini-months, making the grid align perfectly.

## 2. Top Bar & UI Polish
* **Filter Relocation:** Moved the Month/Year toggle button and the Calendar Filter dropdown into the main `TopAppBar` to save vertical space and clean up the screen.
* **Text Updates:** Changed the loading state text from "Loading history..." to a more accurate "Loading calendar...".

## 3. New Rating System & Filter Logic
We completely overhauled the color rating system to unify the UI and decouple savings from the spending budget. The old "green dot" UI was completely removed in favor of full background colors.

* **"All" Filter (Peak Days):**
  * 🔴 **Red:** Highlights the Most Spent Day (the day with the highest total expense of the month).
  * 🟢 **Green:** Highlights the Most Saved Day (the day with the highest total savings deposit of the month).
  * ⚪ **No Color:** All other days remain clean.
* **"Expenses" Filter (Budget %):**
  * Divides your total monthly budget by the number of days in the month to get a "daily budget limit".
  * 🔴 **Red:** Spending was over 100% of the daily limit.
  * 🟡 **Yellow:** Spending was between 80% - 100% of the daily limit.
  * ⚪ **No Color:** Spending was under 80% of the daily limit.
  * *(Days with zero expenses are faded out).*
* **"Savings" Filter:**
  * 🟢 **Green:** Highlights the Most Saved Day.
  * ⚪ **No Color:** Other days with savings.
  * *(Days with zero savings are faded out).*
* **Dynamic Legend:** The legend at the bottom of the calendar now automatically updates its labels and colors to match whichever filter you are currently using.

## 4. Syntax & Bug Fixes
* **`CalendarHistoryScreen.kt`:** Fixed an `Expecting a top level declaration` error caused by an extra trailing closing brace after restructuring the scroll layout.
* **`CalendarHeatmap.kt`:** Fixed a structural duplication issue in the grid layout and restored missing variables (`isSelected`, `isToday`) that broke the compilation when the Green dot was removed.
