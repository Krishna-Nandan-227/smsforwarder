# Enable account sign-in

The slate-grey sign-in and registration screens use Firebase Authentication.
They open without configuration, but only a verified Firebase session opens the inbox.
Firebase manages the saved session; this app never stores passwords itself.

1. Create a project at https://console.firebase.google.com/ and add an Android app
   with package name `com.smsrelay.app`.
2. In Authentication > Sign-in method, enable Email/Password and Google.
   Choose a support email for Google.
3. Run `./gradlew signingReport` (Windows: `.\gradlew.bat signingReport`) and add
   the debug SHA-1 and SHA-256 fingerprints to the Android app in Firebase project settings.
   Before distributing an APK, also add the fingerprints of its release signing key.
4. Download the updated `google-services.json` after enabling Google and adding
   fingerprints. Place it at `app/google-services.json`.
5. Sync Gradle and run the app. The Google Services plugin is applied automatically
   when that file exists; it supplies Firebase configuration and the Web OAuth client ID.

Use a device or emulator with Google Play services and a Google account for Google sign-in.
Email sign-in, account creation, password reset, session restoration, and sign-out
are connected to Firebase. Creating an email account signs the user in immediately.
No SMS content is uploaded to Firebase by this integration.

Manual checks after configuration:

- Register with matching passwords; the inbox should open.
- Sign out in Settings, then sign back in with email/password.
- Try invalid email, mismatched passwords, wrong password, and no network.
- Request a password reset and follow the email link.
- Choose Google sign-in; also cancel the picker and try again.
- Restart the app while signed in; the saved session should open the inbox.
- Share SMS text into the signed-out app, sign in, and confirm the shared text is retained.

References:

- https://firebase.google.com/docs/auth/android/password-auth
- https://firebase.google.com/docs/auth/android/google-signin
