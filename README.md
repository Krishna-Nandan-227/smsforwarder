# SMS Relay - Working with regulations if they work will upload in playstore 

An Android app for letting users choose which SMS messages to relay to a place where they can read them. The first MVP focuses on a private inbox and email sharing, with delivery channels designed to be replaceable so Google Chat or WhatsApp can be explored later.

## Current status

This is the starter product and Android project skeleton. It does not yet read live SMS or send email. Those features need device testing and a deliberate privacy and permission design.

## MVP scope

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

