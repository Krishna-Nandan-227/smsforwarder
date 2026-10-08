# Set up the message assistant

## Enable collection

1. Install and launch the latest app, then sign in.
2. Open **Sources** and select the apps you want to collect: SMS, WhatsApp, or Email.
3. Tap the notification access button. In Android settings, enable notification access for SMSForwarder and return to the app.
4. In **Rules**, keep collection enabled. Enable financial messages if you want banking/payment previews. Leave specific senders blank for all senders, or enter exact sender names or phone numbers separated by commas.
5. Receive a new notification from a selected app. Its available text should appear in **Inbox**. Notifications received before setup are not imported.

WhatsApp includes WhatsApp Business. Email currently includes Gmail and Outlook. SMS collection requires your messaging app to post a notification containing text. Android and the source app may hide or shorten previews, including verification codes.

## Talk to the assistant

Open **Assistant**, type a request or tap **Tap to talk** and allow microphone access. Examples:

- Summarize today
- Show important messages
- Read WhatsApp messages
- Show email messages from Alex
- Summarize yesterday

Answers contain excerpts of matching stored previews with source references. Tap a reference to see the captured text, or open the source app. Opening an app does not navigate to the exact conversation.

Turn on **Read answers aloud** for spoken replies. **Stop voice** stops playback. A system speech recognition app and text-to-speech engine must be available; typing remains available without voice services.

## Alerts

Enable assistant notifications in **Sources** and grant Android's notification permission when prompted. Alerts contain a preview count rather than message bodies.

Enable spoken assistant alerts in **Rules** to announce new preview counts while the Assistant screen is open. This version does not listen continuously or speak in the background.

## Controls and limitations

- Clear collected messages in Sources to delete the local inbox. The temporary assistant conversation may still contain excerpts until sign-out or app restart.
- Pause collection in Rules, deselect sources, or revoke Android notification access to stop collecting.
- Saved email destinations open email drafts for you to send; adding a destination does not connect its mailbox.
- The assistant is a local command-based prototype. It extracts text rather than reasoning with a language model.
- Categories, importance, sender matching, and sensitive-code detection use simple rules; review the captured originals when accuracy matters.
- Notification collection depends on the phone, source app settings, and available previews. It cannot promise every message.
- Message content remains in the local app workspace. System speech recognition can use the provider's network service.

## Floating phone pet

1. Sign in, select your Sources, and enable notification access.
2. Open Assistant and tap **Show phone pet**. Allow **Display over other apps** in Android settings and return.
3. A slate cat appears over other apps. Drag its face to move it. Its caption shows today's collected count, or a quiet prompt after new previews arrive.
4. Tap the cat to open today's count and brief controls. Tap **Read today's brief** to display and speak a local brief. **Stop voice** stops playback.
5. Tap **Hide pet**, or use Assistant's hide control, to remove it.

The pet stays silent until you request a brief. It hides on the lockscreen and after sign-out. It depends on Android keeping the notification listener connected; revoked access, force-stop, and device restrictions can interrupt it. It does not request an always-on microphone. Counts cover collected previews permitted by your rules, not every message in your accounts. The brief uses the same local excerpts as Assistant, rather than a cloud language model.

## Pet appearance and more sources

Open the separate **Pets** tab to choose Cat, Dog, Elephant, or Rabbit. The cat defaults to light orange (#F7C18C). Set a name, choose a preset coat colour, or enter #RRGGBB and tap Apply colour. Changes are saved per signed-in account and update the floating companion.

Sources now includes Instagram, Discord, Teams, Telegram, Signal, Messenger, Slack, and Snapchat alongside SMS, WhatsApp, and Email. Each source requires explicit opt-in. **Add another messaging app** lists other installed launcher apps; choose only the ones whose notification text you want collected. Such apps may also post non-message notifications. No selection connects an account or downloads a conversation history. Hidden, suppressed, and non-text notifications cannot be reconstructed.

Examples: “Summarize Instagram”, “Read Discord messages”, and “Show Teams updates today”.
