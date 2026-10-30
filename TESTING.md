# Tiko Manual QA Checklist (On-Device)

This testing checklist validates task persistence, field integrity, and cloud synchronization on a physical device or emulator. Run these scenarios sequentially.

---

## 1. Single Task Field Lifecycle (Create, Edit, Complete, Uncomplete, Delete)

| Step | Action | Expected Result | Pass/Fail |
|---|---|---|---|
| 1.1 | Tap "+ New Task", enter Title `"Grocery run"`, Notes `"Milk, eggs, coffee"`, select Category `"Shopping"`, Priority `"High"`, Due Date (tomorrow), Due Time `"11:00 AM"`, Reminder `"15 min before"`. Add subtask `"Organic milk"`. Tap **Save**. | Task card appears in Lists under "Shopping" and on Home. Toast shows "Task saved successfully". Subtask count shows `0/1`. | [ ] |
| 1.2 | Tap on the task to open **Task Detail**. Verify all fields (Title, Category, Priority, Due Date/Time, Reminder, Notes, Subtask). | All fields exactly match input. Priority chip is "High", Category icon matches Shopping. | [ ] |
| 1.3 | Check the subtask `"Organic milk"` checkbox. | Subtask checkbox fills with checkmark. Subtask title gets strike-through or checked state. | [ ] |
| 1.4 | Tap **Mark as done** on Task Detail. | Toast "Task completed!" appears. Button label changes to "Mark as pending". Task moves to completed status. | [ ] |
| 1.5 | Navigate back to **Home**. | Task is no longer in the active cards stack. "Done" stat pill increases by 1. | [ ] |
| 1.6 | Open **Lists** screen -> tap "Shopping". | List shows `1 task • 1 completed`. Task item appears with checked circle. | [ ] |
| 1.7 | Tap the task to reopen **Task Detail** -> tap **Mark as pending**. | Toast "Task marked as pending". Button changes to "Mark as done". Task reappears in active tasks on Home. | [ ] |
| 1.8 | Tap the trash icon (or bottom "Delete task" button) -> confirm "Delete". | Confirmation dialog appears with "Cancel" and "Delete". After confirming, toast shows "Task deleted" and user returns to previous screen. Task is removed. | [ ] |

---

## 2. Screen Consistency (Counts, Dots, Filters, Stats)

| Step | Action | Expected Result | Pass/Fail |
|---|---|---|---|
| 2.1 | **Home Screen**: Observe stat pills at top. | Left pill displays total `Done` count; middle pill displays active `Pending` count. | [ ] |
| 2.2 | **Lists Screen**: Observe category cards. | Total task count and completed progress bar for each category match database tasks count. | [ ] |
| 2.3 | **Lists Filter Pills**: Tap "Active" pill, then "Completed" pill, then "All". | "Active" hides categories where all tasks are completed. "Completed" shows only categories with 100% completion. "All" shows all categories. | [ ] |
| 2.4 | **Calendar Screen**: Observe calendar month grid. | Days with scheduled tasks display colored dots corresponding to the task's category color. Tapping a day displays that day's tasks below. | [ ] |
| 2.5 | **Profile / Settings Screen**: Inspect stat cards ("Done", "Day Streak", "On time") and "This week" chart. | Stat cards show accurate counts matching completed tasks and distinct dates. The 7-bar chart shows accurate bars for Mon–Sun based on task completion timestamps. | [ ] |

---

## 3. Logout, Login, and Multi-Session Persistence (The Fixed Bug)

| Step | Action | Expected Result | Pass/Fail |
|---|---|---|---|
| 3.1 | Sign in to an account with Google or Email. | Sync status shows "Synced" or "Syncing...". User tasks load from Firestore. | [ ] |
| 3.2 | Create 2 new tasks ("Task Alpha", "Task Beta"). Mark "Task Alpha" as **Done**. | "Task Alpha" moves to Done. "Task Beta" stays active. Wait 2 seconds for cloud sync. | [ ] |
| 3.3 | Go to Settings -> tap **Log out** -> confirm in dialog. | App flushes pending writes to Firestore before signing out. Toast "Logged out successfully" appears and navigates to Welcome. | [ ] |
| 3.4 | Immediately log back into the same account. | Tasks load from Firestore. **"Task Alpha" MUST remain Done/completed**. "Task Beta" MUST remain active. None of the tasks are reset to active. | [ ] |

---

## 4. Offline / Airplane Mode Resilience

| Step | Action | Expected Result | Pass/Fail |
|---|---|---|---|
| 4.1 | Turn on **Airplane Mode** (no WiFi, no mobile data). | Sync status changes to "Offline". App remains completely usable. | [ ] |
| 4.2 | Create "Offline Task 1" and mark an existing task Done. | Changes apply instantly in local Room database. UI updates immediately. | [ ] |
| 4.3 | Go to Settings -> tap **Log out**. | Dialog warning appears: `"Some changes haven't synced yet. Logging out now will lose them."` with "Cancel" and "Log out anyway". | [ ] |
| 4.4 | Tap **Cancel** on the warning dialog. | Logout is cancelled. No local tasks are deleted. | [ ] |
| 4.5 | Turn off Airplane Mode (reconnect to internet). | Sync status transitions to "Syncing..." then "Synced". Local changes upload to Firestore. | [ ] |
| 4.6 | Log out after sync completes. | Flushes and signs out cleanly without warnings. | [ ] |

---

## 5. Guest Mode to Logged-In Account Merge

| Step | Action | Expected Result | Pass/Fail |
|---|---|---|---|
| 5.1 | Clear app storage or start from Welcome screen as Guest ("Continue as Guest"). | Lands on Home screen in Guest mode. | [ ] |
| 5.2 | Create a task: `"Guest Only Task"`. Mark it completed. | Task is stored in local Room database. | [ ] |
| 5.3 | Navigate to Settings -> tap **Log in** (or Welcome -> Sign in) and log into an existing account. | The guest task is uploaded to the account's Firestore collection without overwriting existing tasks. Both the user's remote tasks and the guest task are present. | [ ] |

---

## 6. Second Account Isolation

| Step | Action | Expected Result | Pass/Fail |
|---|---|---|---|
| 6.1 | Log in as **User A**. Create a task `"Secret Project A"`. Log out. | User A's tasks are synced to User A's Firestore collection. Local Room database is cleared on logout. | [ ] |
| 6.2 | Log in as **User B** (different email or Google account). | User B sees ONLY User B's tasks. `"Secret Project A"` is NOT visible. | [ ] |
| 6.3 | Log out of User B and log back in as User A. | `"Secret Project A"` is restored from User A's Firestore collection. | [ ] |

---

## 7. App Reinstall / Cache Clear Simulation

| Step | Action | Expected Result | Pass/Fail |
|---|---|---|---|
| 7.1 | While logged in with tasks (active and completed), go to Android App Settings -> **Clear Data / Clear Storage** (or uninstall & reinstall APK). | Local Room database and SharedPreferences are wiped. | [ ] |
| 7.2 | Open Tiko -> Log in with the same account. | All tasks are pulled from Firestore: titles, notes, categories, priorities, due dates, subtasks, and exact completion states (`done = true` and `done = false`) are restored. | [ ] |

---

## 8. Auth-Driven Sync Lifecycle & Profile Document Verification

| Step | Action | Expected Result | Pass/Fail |
|---|---|---|---|
| 8.1 | Create or log into an account with Google Sign-In or Email. | Firestore console shows `users/{uid}` profile document created with `name`, `email`, `provider`, `createdAt`, and `lastLoginAt`. | [ ] |
| 8.2 | Create tasks while online. | Firestore console shows `users/{uid}/tasks/{taskId}` subcollection documents populated with all 15 contract fields. | [ ] |
| 8.3 | Close app completely (kill task) and perform cold start. | App starts, `AuthStateListener` in `SyncCoordinator` immediately restores sync; sync status shows "Synced". Tasks remain synced. | [ ] |
| 8.4 | Trigger sync error or denial (e.g. invalid network state). | Profile screen caption displays `"Sync problem: <code>"` in `#D9534A` without exposing any user data. | [ ] |
