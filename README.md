# SMS Relay - Working with regulations if they work will upload in playstore 

An Android app for letting users choose which SMS messages to relay to a place where they can read them. The first MVP focuses on a private inbox and email sharing, with delivery channels designed to be replaceable so Google Chat or WhatsApp can be explored later.

## Current status

Includes slate-grey email/password sign-in, registration, Google sign-in,
password reset, and sign-out using Firebase Authentication. Follow
[AUTH_SETUP.md](AUTH_SETUP.md) to connect your Firebase project. Without configuration,
the authentication screens open but cannot create accounts or sign in.

The app supports a local inbox, saved email destinations, rules, and a profile.
It imports user-shared text and opens email drafts. Live SMS reading and automatic
delivery are not yet connected.

## Current workspace

- Inbox: search sample previews and texts imported using Android's Share action.
- Accounts: add, edit, and remove named email destinations; choose one when sharing a message.
- Rules: pause manual sharing, hide messages labeled Financial, and filter exact sender names.
- Profile: save a display name and About me, view workspace counts, and sign out.

Destinations, rules, profile details, and imported messages are stored locally per
signed-in account. Email sharing opens a prefilled draft in an installed email app;
the user sends it. Live SMS collection, automatic delivery, Google Chat, and WhatsApp
are not yet connected. Sender and financial filters use the message's existing labels;
shared text is labeled User shared.

## Planned scope

- Show a simple inbox of relayed messages, labeled with sender and timestamp.
- Let the user choose all senders or allowlist specific senders.
- Make financial message forwarding an explicit, off-by-default preference.
- Provide pause and clear-queued-messages controls.
- Keep delivery behind a channel interface. Start with user-initiated email sharing; add automatic delivery only after its credential and backend design is settled.
- Keep WhatsApp as a future channel, subject to its platform requirements.

## Android permission note

Live incoming SMS collection requires SMS permissions and has platform and distribution constraints. The starter deliberately omits those permissions and does not attempt to read SMS until the app's collection model is implemented and reviewed. A user-triggered share/import flow can be prototyped without broad inbox access.

## Open in Android Studio

Open this folder as a project. Android Studio can sync the Gradle project and install the app on a connected device or emulator.

