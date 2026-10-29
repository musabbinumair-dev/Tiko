# Tiko Project Rules

- Design system: cream `#EFEBDD`, purple `#6B4EFF`, yellow `#FFB92E`, lime `#C8DB6A`, red `#D9534A`, and black `#151515`. Use the Outfit font, 28dp card radius, pill buttons, and white round icon buttons. Never change designs unless asked.
- Do not upgrade Kotlin, Compose, KSP, Room, or the Gradle plugin versions unless a task explicitly requires it.
- Do not add new libraries without asking.
- Every change must pass `./gradlew assembleDebug`.
- Never commit keystores, passwords, or secrets.
- All user-facing messages must go through `AppToastController` or `AppDialogController`; do not use default Toasts or AlertDialogs.
- Write real Room migrations from now on. Never use `fallbackToDestructiveMigration`.
