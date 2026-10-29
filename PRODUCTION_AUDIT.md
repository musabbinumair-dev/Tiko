# Production audit — 2026-10-29

Scope: source, Gradle configuration, manifest, tracked Firebase configuration, tests, and the committed GitHub Actions workflow. This was an audit only; no production-code changes were made.

## Verification result

`./gradlew assembleDebug --console=plain` does not complete successfully. After SDK setup, the Google Services plugin reports that `app/google-services.json` is missing. The only tracked copies are in `public/assets/aistudio/`, which the plugin does not consume. This also means the current CI workflow cannot reliably build the app.

## Leftover debug code

- **Blocker** — `app/src/main/java/com/example/ui/screens/AddEditTaskScreen.kt:954-958`: long-pressing the production Save/Update button deliberately opens the save-error screen (`// DEBUG: Testing only`). Remove the gesture and verify real save-error handling instead.
- **Important** — `app/src/main/java/com/example/data/repository/AuthRepository.kt:149-196`: verbose Google sign-in diagnostics remain enabled in release code, including token-presence, credential type, Firebase failure messages, and user UID.
- **Important** — `.github/workflows/build_apk.yml:48-49`: CI prints the debug keystore SHA-1 unnecessarily.
- **Nice-to-have** — `app/src/main/java/com/example/ui/screens/CalendarScreen.kt:124-154`: comments and fallback UI values still refer to a mockup/demo, including a fabricated Tuesday/Oct 27 label on parse failure. No gallery route was found.

## Hardcoded demo or placeholder data and strings

- **Blocker** — `app/src/main/java/com/example/data/FirebaseInitializer.kt:53-57` and `app/src/main/java/com/example/data/repository/AuthRepository.kt:38`: Firebase project identifiers, API key, and OAuth web client ID are hardcoded in source. The same Firebase configuration, including API key, is tracked under `public/assets/aistudio/google-services_0f6d070f.json:3-31` and `public/assets/aistudio/google-services_a261a8d4.json:3-31`. Rotate/restrict exposed keys as appropriate and remove duplicate configuration from public assets.
- **Important** — User-visible copy is hardcoded throughout Compose files (for example `app/src/main/java/com/example/MainActivity.kt:156-161`, `app/src/main/java/com/example/ui/screens/LoginScreen.kt:156-208`, and `app/src/main/java/com/example/ui/screens/SettingsScreen.kt:955`). `app/src/main/res/values/strings.xml:2` contains only the app name, so the app cannot be localized or consistently managed.
- **Important** — `app/src/main/java/com/example/data/model/CategoryModel.kt:64-69`, `TaskEntity.kt:13-18`, and `TikoViewModel.kt:299-329` hardcode default categories, dates/times, reminders, and priorities. Some defaults (for example new-task time `12:00 PM`) are product behavior rather than user choice.
- **Nice-to-have** — `app/src/main/java/com/example/ui/theme/Color.kt:6-16` and `Theme.kt:9-34` use a different palette, 0dp corners, Bricolage/DMSans typography than the requested project design system. This is pre-existing design drift and should be addressed only in an explicitly requested design task.

## Package name and version handling

- **Important** — `app/build.gradle.kts:12-20`: `namespace` and `applicationId` are correctly `com.musab.tiko`, but `versionCode = 1` and `versionName = "1.0"` are manually fixed with no release/versioning policy.
- **Important** — Most implementation remains under `com.example` (`app/src/main/java/com/example/**`), while `com.musab.tiko/MainActivity.kt:1-5` is a thin wrapper and the manifest exports a legacy `com.example.MainActivity` alias at `AndroidManifest.xml:30-33`. This is confusing release identity and unnecessary public surface.

## Room

- **Blocker** — `app/src/main/java/com/example/data/local/TikoDatabase.kt:11-27`: database version is 3, `exportSchema = false`, no migrations are registered, and `fallbackToDestructiveMigration()` deletes user task data on schema upgrades. This directly violates the required migration rule.
- **Important** — `TaskEntity.kt:14-22` persists dates, times, priority and reminder as free-form strings. There are no database constraints or type converters, so sorting, validation, migration, and reminder scheduling are fragile.

## Firebase

- **Blocker** — Firestore sync is not implemented. `app/build.gradle.kts:113-114` comments out the Firestore dependency and no source imports or uses Firestore. Tasks and profile data are only local Room/SharedPreferences, so signing in does not sync across devices and account deletion does not delete cloud task data.
- **Blocker** — `app/google-services.json` is absent, while the Google Services plugin is applied at `app/build.gradle.kts:8` and reports the missing file during `assembleDebug`. Copies tracked under `public/assets/aistudio/` are not build inputs.
- **Important** — Firebase libraries in use are Auth, AI, App Check reCAPTCHA/debug, Credential Manager and Google ID (`app/build.gradle.kts:98-131`; versions catalog `gradle/libs.versions.toml:87-105`). The Firebase AI dependency is declared but unused; Firestore is declared only as a commented-out option.
- **Important** — `FirebaseInitializer.kt:41-69` silently falls back to hardcoded options whenever resource configuration fails. This hides deployment/configuration errors and makes environment separation impossible.

## Signing configuration and GitHub workflow

- **Blocker** — `app/build.gradle.kts:27-50` defines release signing from `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD`, but `.github/workflows/build_apk.yml:45-52` only decodes and uses the tracked debug keystore to build `assembleDebug`. There is no signed release/AAB build, no secret-provisioned upload key, and no release verification.
- **Important** — `.github/workflows/build_apk.yml:3-11, 68-75` runs on every branch, grants `contents: write`, and publishes a GitHub Release for every non-PR push. This can make arbitrary branch pushes create “latest” releases.
- **Important** — `debug.keystore.base64` is tracked and decoded in CI (`.github/workflows/build_apk.yml:45-49`). Debug keys are not suitable for production signing and should not be confused with a release process.

## Manifest

- **Important** — `app/src/main/AndroidManifest.xml:10-12` enables backups while `res/xml/backup_rules.xml:1-15` and `res/xml/data_extraction_rules.xml:1-18` are sample/empty rules. User profile and task data in SharedPreferences/Room may be included in cloud backup/device transfer without an explicit privacy decision.
- **Important** — `AndroidManifest.xml:30-33` exports the legacy activity alias without an intent filter or permission. Remove it unless there is a documented external integration requirement.
- **Nice-to-have** — The manifest declares only `INTERNET` and `ACCESS_NETWORK_STATE` (`AndroidManifest.xml:5-6`), which is appropriate for current networking. No cleartext-traffic override was found. Notification settings do not request Android 13+ notification permission or schedule notifications (see below).

## Build, R8, and dependencies

- **Blocker** — `app/build.gradle.kts:44-47`: release minification is disabled and `app/proguard-rules.pro:1-25` is the untouched template. A production release would ship unminified, without project-specific shrinker verification.
- **Important** — `app/build.gradle.kts:101-131` declares unused runtime dependencies: Firebase AI, OkHttp, logging interceptor, Retrofit, Moshi, and App Check packages have no application-source usage. Remove or justify them after a dependency audit; do not add replacements without approval.
- **Important** — The only tests are generated smoke tests (`app/src/test/java/com/example/ExampleUnitTest.kt:10-12`, `ExampleRobolectricTest.kt:14-19`, and `app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt:15-20`). There is no coverage for auth, Room migrations, repositories, task creation/deletion, or error states.

## Logging that prints user data

- **Blocker** — `app/src/main/java/com/example/data/repository/AuthRepository.kt:133-196` logs credential type, exception text, token presence and Firebase UID. Exception text can include provider/user context. Remove or redact release logging and do not log identifiers or authentication material.
- **Important** — `FirebaseInitializer.kt:38,69,72` logs Firebase initialization messages, project ID, and raw errors in all builds. Use a release-safe logging policy.

## Screens or flows that can crash or mislead

- **Blocker** — Notifications are only a preference toggle: `UserPreferences.kt:21-22,52-54`, `MainActivity.kt:141-161`, and `SettingsScreen.kt:491-504`. There is no notification permission flow, notification channel, scheduler, worker/alarm, or receiver. The UI can tell users that reminders are enabled even though no reminders can fire.
- **Important** — `TaskEntity.kt:42-59` swallows all malformed-subtask JSON errors and silently returns an empty list, losing visible data with no recovery path or user-facing error.
- **Important** — `TikoViewModel.kt:207-223, 260-295` catches task-save exceptions but offers no structured error state, retry identity, or failure persistence. `AddEditTaskScreen.kt:211-233` can navigate to an error page, but its retry depends on volatile UI state and can lose the attempted edit after process death.
- **Important** — `SettingsScreen.kt:1005-1044` promises account deletion removes “all saved tasks,” yet tasks are local-only and no Firestore deletion exists. The Google-user fallback attempts deletion after failed password reauthentication; its UX and recent-login requirements need provider-aware handling.
- **Nice-to-have** — `CalendarScreen.kt:124-154` uses a remembered display-card mapping and fallback time/category values, which can show invented values for malformed tasks rather than an explicit empty/error state.

## Anything else that is not production quality

- **Important** — `UserPreferences.kt:15-109` stores profile PII and settings in ordinary SharedPreferences. Combined with enabled backups and no encryption/privacy policy surfaced in the project, data handling needs a documented privacy/security review.
- **Important** — `TikoViewModel.kt:360-383` exports all task content into a plain string, but there is no secure export destination, consent UI, sharing policy, or import/restore validation.
- **Nice-to-have** — `TikoDatabase.kt:33-36` and `CalendarScreen.kt:100-119` use device-default locales/time zones and legacy `SimpleDateFormat`; date-only task semantics will be error-prone across locales/time-zone changes.

## Recommended order

1. Remove the debug long-press and release-safe logging; rotate/restrict exposed Firebase credentials.
2. Put approved Firebase config in the standard build location through a secure CI process, then make `assembleDebug` pass.
3. Replace destructive Room migration with tested, real migrations and enable schema export.
4. Decide and implement Firestore sync plus account-data deletion, or remove cross-device/account claims.
5. Build real notification permission/scheduling or remove the reminder-enabled UX.
6. Establish signed release/AAB CI with least-privilege workflow permissions and R8 verification.
7. Add meaningful automated tests before feature work resumes.
