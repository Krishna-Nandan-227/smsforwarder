# Build roadmap

## Milestone 1: App shell

- Compose inbox and settings UI.
- Message model, user forwarding preferences, and delivery channel contract.
- Sample data only; no SMS access or network delivery.

## Milestone 2: User-initiated intake and email

- Accept a message shared by the user from the system SMS app.
- Display the sender, original text, and timestamp in the app inbox.
- Open the device email app with a prefilled message using an `ACTION_SENDTO` mail intent.
- Keep financial message forwarding off until explicitly enabled.

## Milestone 3: Persisted inbox and controls

- Add local database and durable preferences.
- Add sender allowlist, message deletion, queue clearing, and pause/resume controls.
- Make retention behavior visible and easy to change.

## Milestone 4: Optional automatic delivery

- Decide on a backend and account model before adding unattended email sending.
- Keep destination credentials off the APK; deliver through a server-side adapter.
- Add destination-specific consent, delivery status, retry, and revocation.

## Later channels

- Google Chat: user-configured Workspace incoming webhook or Chat app integration.
- WhatsApp: separate channel, gated on approved platform setup and messaging rules.

