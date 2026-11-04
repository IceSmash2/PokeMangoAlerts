# PokéMango Alerts (Android)

A small Android app for the restock alert service. It shows the alert web app inside the app and adds
what a browser can't: Firebase push, a long alarm-volume sound with two tone choices, a Stop button, and
an alert history kept on the phone.

## How it works
- `MainActivity` loads `https://<your-service>.onrender.com/app/` in a WebView. The page detects the
  `AndroidApp` bridge (`Bridge.kt`) and registers this phone as an Android (`fcm`) device instead of using
  browser push.
- `PushService` receives the alert message, saves it to the history, and starts `AlertService`.
- `AlertService` is a foreground service. It shows the alert notification and plays the chosen tone through
  `TonePlayer` on the alarm volume until it ends, you tap Stop, swipe the notification away, press a volume
  key, or open the app.
- If the service can't start, the alert falls back to a notification channel with a built-in sound.

## Tones
`app/src/main/res/raw/alert_full.mp3` (about 2 minutes) and `alert_short.mp3` (15 seconds).

## Build (GitHub Actions)
1. Add these repository secrets (Settings, Secrets and variables, Actions): `GOOGLE_SERVICES_B64`,
   `KEYSTORE_B64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
2. Open the Actions tab, choose **Build APK**, tap **Run workflow**, and enter a version and a build number.
3. The signed APK is attached to a new release and to the run.

Keep the signing key safe. An app can only be updated by an APK signed with the same key.

## Server address
The default is set in `app/build.gradle.kts` (`serverUrl`). To point the app at another service, pass
`-PserverUrl=https://example.onrender.com/`.
