# SMSForwarder

An Android personal message assistant built with Kotlin and Jetpack Compose. Collect new SMS, WhatsApp, email, and other messaging notification previews in one inbox, ask for a brief, and listen to the response.

## Current features

- Firebase email/password sign-in, registration, password reset, and sign-out. Google sign-in requires additional Firebase configuration; see [AUTH_SETUP.md](AUTH_SETUP.md).
- Assistant: typed requests, tap-to-talk using Android speech recognition, and spoken answers using text-to-speech.
- Inbox: real collected previews and user-shared text, search, sender/source labels, and email draft sharing.
- Sources: opt into SMS, WhatsApp, Email, Instagram, Discord, Teams, Telegram, Signal, Messenger, Slack, or Snapchat. Add other installed apps, grant notification access, and optionally enable assistant alerts.
- Pets: Cat, Dog, Elephant, and Rabbit; saved name, preset colours, and custom hex colour. The light orange companion can float over other apps with permission.
- Rules: pause collection, opt into financial messages, allow specific senders, and enable spoken preview counts while Assistant is open.
- Profile: saved display name and bio. Email destinations are managed from Sources.

## How it works

Android notification access supplies new previews from supported installed apps. The app stores them locally per signed-in email account. No sources are selected by default. Recognized verification codes are excluded from notification collection; financial messages require opt-in.

This first assistant uses local command matching and short text extracts. It does not use a cloud AI model or generate semantic summaries. Categories and sensitive-code detection are heuristics and can miss cases.

It cannot retrieve complete WhatsApp conversations, email bodies, old SMS inboxes, hidden previews, or notification history. Email support currently covers Gmail and Outlook notification previews. SMS support uses messaging app notifications, rather than direct SMS inbox access.

Message content is not uploaded by the assistant to Firebase or an AI backend. Firebase is used for authentication. The device's speech recognition provider may use a network service; this is separate from local brief generation. Automatic forwarding, mailbox OAuth connections, and a conversational AI backend are future work.

See [ASSISTANT_SETUP.md](ASSISTANT_SETUP.md) for device setup and examples.

## Build and run

Open this folder in Android Studio, sync Gradle, and run on an Android device (minimum Android 8). Configure Firebase using [AUTH_SETUP.md](AUTH_SETUP.md).

In PowerShell with Android Studio's JDK:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
