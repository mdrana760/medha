# Medha (মেধা) — transparent AI auto-reply assistant

Medha is an Android app that drafts AI replies to your **own** incoming messages
and shows them to you for approval before anything is sent. You bring your own
[Google AI Studio](https://aistudio.google.com/app/apikey) Gemini API key; Medha
never ships or proxies a key of its own.

This is the **consent-based** design: it uses the official Android
NotificationListener + RemoteInput APIs (which you explicitly enable) instead of
root, Xposed hooks, accessibility scraping, or silent permission granting. It
does not intercept other people's communications, log third parties' messages to
a server, or persist across a factory reset.

## How it works

1. Sign in with Google (Firebase Auth).
2. Enter your Gemini API key — it's validated against the live API and stored
   **AES-256 encrypted** (Android Keystore) locally, with an encrypted backup in
   Firestore.
3. Grant the permissions Medha guides you through (notification access, overlay,
   notifications, optional contacts) — all standard, revocable Android prompts.
4. Pick which messaging apps Medha watches.
5. When a chosen app shows a message **you** received, Medha drafts a reply with
   Gemini and pops it up. You **Send / Edit / Ignore**. Nothing is sent without
   your approval (unless you explicitly turn approval off in Settings).

## Project layout

```
app/src/main/java/com/medha/app/
├── MedhaApp.kt, MainActivity.kt
├── auth/        LoginActivity, SetupActivity, AuthManager
├── firebase/    FirebaseManager, UserRepository
├── ai/          GeminiClient, PersonalityEngine, ConversationMemory, ApiKeyManager
├── services/    MedhaSystemService (foreground), MedhaNotificationService, BootReceiver
├── overlay/     OverlayManager, ReplyPreviewOverlay (ReplyCoordinator/ReplyActionReceiver)
├── ui/          AppManagerActivity, ProfileActivity, ConversationLogActivity, SettingsActivity
├── data/        MedhaDatabase (Room), Prefs, ScheduleManager, SupportedApps
└── utils/       TTSManager, EncryptionUtil, NetworkMonitor, ContactHelper, PermissionHelper
```

## Building

Requirements: JDK 17, Android SDK (compileSdk 34), `minSdk 26`.

```bash
./gradlew :app:assembleDebug
```

### Firebase setup

The project **builds without Firebase** so CI and new contributors aren't blocked.
The Google Services Gradle plugin is only applied when `app/google-services.json`
exists (see `app/build.gradle.kts`).

To enable Google Sign-In + Firestore:

1. Create a Firebase project and add an Android app with package `com.medha.app`.
2. Add a SHA-1 fingerprint and enable Google as a sign-in provider (Auth).
3. Download `google-services.json` into `app/` (it's gitignored). A
   `app/google-services.json.template` shows the expected shape.

Without it, the app runs locally but Google Sign-In and Firestore sync are
disabled (the login screen shows a notice).

## Firestore layout

```
users/{uid}                       { profile{...}, config{...}, connectedApps{...} }
users/{uid}/conversations/{id}    { app, contactName, messages[], lastUpdated }
```

## What was intentionally NOT built

The original request also asked for root auto-granting of permissions, install to
`/system` to survive factory reset, Xposed/accessibility hooks into other apps,
and remote logging of intercepted conversations. Those were omitted because they
are the defining capabilities of stalkerware and have no consent-respecting
use. Medha keeps the useful auto-responder product without them.
