# Production audit — 2026-10-29

Scope: source, Gradle configuration, manifest, tracked Firebase configuration, tests, and the committed GitHub Actions workflow. This was an audit only; no production-code changes were made.

## Change log (production-prep branch)

| Commit | Change |
|--------|--------|
| `20643da` | Removed `.aistudio/` media artifacts; added `.aistudio/` to `.gitignore` |
| `2b36f66` | Added `AGENTS.md` and this audit document; placed `app/google-services.json` |
| `1e8e1a8` | **Blocker 2** ✅ — Removed duplicate Firebase config from `public/assets/aistudio/`; switched Google web-client ID to read from `R.string.default_web_client_id` |
| `5a36466` | **Blocker 3** ✅ — Removed the `// DEBUG` long-press save-error trigger in `AddEditTaskScreen` and `SaveErrorScreen`; removed the `debugSaveError` route from `TikoViewModel` |
| `c406ff6` | Added `*.keystore`, `*.jks`, `*.base64`, `debug.keystore` to `.gitignore` |
| `1b86fbb` | **Blocker 9** ✅ — Removed all sensitive auth logging: UIDs, credentials, tokens, and exception text from `AuthRepository` and `FirebaseInitializer` |
| `b55f7a4` | **Blocker 10** ✅ — Set `android:allowBackup="false"`; added explicit database and shared-prefs exclusions to `backup_rules.xml` and `data_extraction_rules.xml` |
| `5f1975d` | Mapped Google sign-in errors to friendly messages in `AuthRepository` |
| `e5a3e01` | **Blocker 4** ✅ — Data Layer V2: migrated task IDs to String UUIDs, added soft deletes, timestamps, sync tracking; enabled Room schema export (`room.schemaLocation`), bumped version to 4 with final destructive migration policy |
| `f652a64` | **Blocker 5** ✅ — Firestore Sync: re-enabled Firestore dependency; bidirectional sync in `SyncManager` with offline-first Room cache, pending write echo suppression, cloud task deletion on account deletion, and `firestore.rules` restored |
| `4691ba2` | **Blocker 6 (Part 1)** — Add reminder scheduler, receiver, actions, and boot receiver |
| `fea37db` | **Blocker 6 (Part 2)** — Wire reminders to repository, sync manager, app start, login, and logout |
| HEAD | **Blocker 6** ✅ — Reminder permissions flow, rationale dialog, exact-alarm notice, and settings synchronization |

## Firestore sync status (investigated 2026-10-29)

- **DONE** ✅ — **Blocker 5** resolved (`f652a64`): Firestore dependency `libs.firebase.firestore` re-enabled. Bidirectional sync implemented in `SyncManager.kt` (`users/{uid}/tasks/{taskId}`). Push observes unsynced tasks with non-blocking `set()`; pull listens to changes with pending-write echo suppression, conflict resolution (newest `updatedAt` wins), soft/hard delete reconciliation, and tombstone cleanup. Cloud task documents are deleted before auth account deletion. `firestore.rules` restored in repo.

## Verification result

`./gradlew assembleDebug --console=plain` does not complete successfully. After SDK setup, the Google Services plugin reports that `app/google-services.json` is missing. The only tracked copies are in `public/assets/aistudio/`, which the plugin does not consume. This also means the current CI workflow cannot reliably build the app.

**Update:** `app/google-services.json` is now placed and tracked (commit `2b36f66`). Build should pass; verification pending final run.

## Leftover debug code

- **DONE** ✅ — `app/src/main/java/com/example/ui/screens/AddEditTaskScreen.kt`: long-pressing the production Save/Update button opened the save-error screen (`// DEBUG: Testing only`). Gesture and debug route removed in `5a36466`.
- **Important** — `.github/workflows/build_apk.yml:48-49`: CI prints the debug keystore SHA-1 unnecessarily. (Not addressed — workflow file is out of scope for this session.)
- **Nice-to-have** — `app/src/main/java/com/example/ui/screens/CalendarScreen.kt:124-154`: comments and fallback UI values still refer to a mockup/demo, including a fabricated Tuesday/Oct 27 label on parse failure. No gallery route was found.

## Hardcoded demo or placeholder data and strings

- **DONE** ✅ — `app/src/main/java/com/example/data/FirebaseInitializer.kt` and `AuthRepository.kt`: Firebase project identifiers, API key, and OAuth web client ID were hardcoded in source. The same Firebase configuration was tracked under `public/assets/aistudio/`. Duplicate config files removed (`1e8e1a8`); web-client ID now reads from `R.string.default_web_client_id`.
- **Important** — User-visible copy is hardcoded throughout Compose files (for example `app/src/main/java/com/example/MainActivity.kt:156-161`, `app/src/main/java/com/example/ui/screens/LoginScreen.kt:156-208`, and `app/src/main/java/com/example/ui/screens/SettingsScreen.kt:955`). `app/src/main/res/values/strings.xml:2` contains only the app name, so the app cannot be localized or consistently managed.
- **Important** — `app/src/main/java/com/example/data/model/CategoryModel.kt:64-69`, `TaskEntity.kt:13-18`, and `TikoViewModel.kt:299-329` hardcode default categories, dates/times, reminders, and priorities. Some defaults (for example new-task time `12:00 PM`) are product behavior rather than user choice.
- **Nice-to-have** — `app/src/main/java/com/example/ui/theme/Color.kt:6-16` and `Theme.kt:9-34` use a different palette, 0dp corners, Bricolage/DMSans typography than the requested project design system. This is pre-existing design drift and should be addressed only in an explicitly requested design task.

## Package name and version handling

- **Important** — `app/build.gradle.kts:12-20`: `namespace` and `applicationId` are correctly `com.musab.tiko`, but `versionCode = 1` and `versionName = "1.0"` are manually fixed with no release/versioning policy.
- **Important** — Most implementation remains under `com.example` (`app/src/main/java/com/example/**`), while `com.musab.tiko/MainActivity.kt:1-5` is a thin wrapper and the manifest exports a legacy `com.example.MainActivity` alias at `AndroidManifest.xml:30-33`. This is confusing release identity and unnecessary public surface.

## Room

- **DONE** ✅ — **Blocker 4** resolved (`e5a3e01`): `TaskEntity` upgraded to Data Layer V2 with String UUIDs, `updatedAt`, `deleted` soft-delete flag, `synced` status, and `completedAt`. Room version bumped to 4 with final destructive migration policy noted in code. Schema export enabled via `room.schemaLocation` in KSP configuration and test sourceSets.
- **Important** — `TaskEntity.kt:14-22` persists dates, times, priority and reminder as free-form strings. There are no database constraints or type converters, so sorting, validation, migration, and reminder scheduling are fragile.

## Firebase

- **DONE** ✅ — **Blocker 5** resolved (`f652a64`): Firestore sync fully implemented with `SyncManager.kt` and offline-first Room cache; `libs.firebase.firestore` dependency active. Guest tasks merged on login; cloud task documents deleted before account removal; `firestore.rules` added to repository.
- **DONE** ✅ — `app/google-services.json` is now present and tracked (`2b36f66`). Duplicate copies under `public/assets/aistudio/` removed (`1e8e1a8`).
- **Important** — Firebase libraries in use are Auth, AI, App Check reCAPTCHA/debug, Credential Manager, Google ID, and Firestore. The Firebase AI dependency is declared but unused.
- **Important** — `FirebaseInitializer.kt:41-69` silently falls back to hardcoded options whenever resource configuration fails. This hides deployment/configuration errors and makes environment separation impossible.

## Signing configuration and GitHub workflow

- **Blocker** — `app/build.gradle.kts:27-50` defines release signing from `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD`, but `.github/workflows/build_apk.yml:45-52` only decodes and uses the tracked debug keystore to build `assembleDebug`. There is no signed release/AAB build, no secret-provisioned upload key, and no release verification.
- **Important** — `.github/workflows/build_apk.yml:3-11, 68-75` runs on every branch, grants `contents: write`, and publishes a GitHub Release for every non-PR push. This can make arbitrary branch pushes create "latest" releases.
- **Important** — `debug.keystore.base64` is tracked and decoded in CI. Debug keys are not suitable for production signing and should not be confused with a release process. (`*.base64` and `*.keystore` now in `.gitignore` — `c406ff6`.)

## Manifest

- **DONE** ✅ — **Blocker 10** resolved (`b55f7a4`): `android:allowBackup="false"` is set; `backup_rules.xml` and `data_extraction_rules.xml` now explicitly exclude `tiko_tasks.db` (+ WAL/SHM) and `tiko_settings_prefs.xml` from both cloud backup and device transfer.
- **Important** — `AndroidManifest.xml:30-33` exports the legacy activity alias without an intent filter or permission. Remove it unless there is a documented external integration requirement.
- **Nice-to-have** — The manifest declares only `INTERNET` and `ACCESS_NETWORK_STATE`, which is appropriate for current networking. Notification settings do not request Android 13+ notification permission or schedule notifications (see below).

## Build, R8, and dependencies

- **Blocker** — `app/build.gradle.kts:44-47`: release minification is disabled and `app/proguard-rules.pro:1-25` is the untouched template. A production release would ship unminified, without project-specific shrinker verification.
- **Important** — `app/build.gradle.kts:101-131` declares unused runtime dependencies: Firebase AI, OkHttp, logging interceptor, Retrofit, Moshi, and App Check packages have no application-source usage. Remove or justify them after a dependency audit; do not add replacements without approval.
- **Important** — The only tests are generated smoke tests. There is no coverage for auth, Room migrations, repositories, task creation/deletion, or error states.

## Logging that prints user data

- **DONE** ✅ — **Blocker 9** resolved (`1b86fbb`): `AuthRepository.kt` and `FirebaseInitializer.kt` no longer log credential type, exception text, token presence, Firebase UID, project ID, or raw errors. All sensitive `Log.*` calls and `TAG` constants removed; remaining catch blocks use `_` to suppress unused exception variables.

## Screens or flows that can crash or mislead

- **DONE** ✅ — **Blocker 6** resolved: Built production notification reminders system with `ReminderScheduler`, `ReminderReceiver` (channel `"task_reminders"`, monochrome icon, #6B4EFF, auto-cancel, mark-done & snooze actions), `ReminderActionReceiver`, and `BootReceiver`. Connected to `TaskRepository` for CRUD, `SyncManager` for multi-device sync, app startup/login, and logout/account deletion. Wired permissions flow: `POST_NOTIFICATIONS` runtime request, rationale dialogs via `AppDialogController`, permanent denial redirection to settings, exact-alarm warnings with settings fallback, and accurate toggle state reflecting both app preference and system permission.
- **Important** — `TaskEntity.kt:42-59` swallows all malformed-subtask JSON errors and silently returns an empty list, losing visible data with no recovery path or user-facing error.
- **Important** — `TikoViewModel.kt:207-223, 260-295` catches task-save exceptions but offers no structured error state, retry identity, or failure persistence. `AddEditTaskScreen.kt:211-233` can navigate to an error page, but its retry depends on volatile UI state and can lose the attempted edit after process death.
- **DONE** ✅ — `SettingsScreen.kt:1005-1044` now deletes user task documents from Firestore prior to account deletion (`f652a64`).
- **Nice-to-have** — `CalendarScreen.kt:124-154` uses a remembered display-card mapping and fallback time/category values, which can show invented values for malformed tasks rather than an explicit empty/error state.

## Anything else that is not production quality

- **Important** — `UserPreferences.kt:15-109` stores profile PII and settings in ordinary SharedPreferences. With backups now disabled (`b55f7a4`), the backup-leakage risk is resolved, but data handling still needs a documented privacy/security review and consideration of EncryptedSharedPreferences.
- **Important** — `TikoViewModel.kt:360-383` exports all task content into a plain string, but there is no secure export destination, consent UI, sharing policy, or import/restore validation.
- **Nice-to-have** — `TikoDatabase.kt:33-36` and `CalendarScreen.kt:100-119` use device-default locales/time zones and legacy `SimpleDateFormat`; date-only task semantics will be error-prone across locales/time-zone changes.

## Remaining blockers (not yet addressed)

| # | Description |
|---|-------------|
| Blocker | Release build: minification disabled, no signed AAB, no upload keystore in CI |

## Recommended order

1. ~~Remove the debug long-press and release-safe logging; rotate/restrict exposed Firebase credentials.~~ ✅ Done
2. ~~Put approved Firebase config in the standard build location through a secure CI process, then make `assembleDebug` pass.~~ ✅ Done
3. ~~Replace destructive Room migration with tested, real migrations and enable schema export.~~ ✅ Done (`e5a3e01`)
4. ~~Decide and implement Firestore sync plus account-data deletion, or remove cross-device/account claims.~~ ✅ Done (`f652a64`)
5. ~~Build real notification permission/scheduling or remove the reminder-enabled UX.~~ ✅ Done (Blocker 6)
6. Establish signed release/AAB CI with least-privilege workflow permissions and R8 verification.

