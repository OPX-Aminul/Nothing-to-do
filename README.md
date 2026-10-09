# HairGrowth OS — Clinical Protocol (Native Android)

চুলের বৃদ্ধির ক্লিনিক্যাল প্রোটোকল ট্র্যাকার — **100% নেটিভ অ্যান্ড্রয়েড অ্যাপ্লিকেশন** (Kotlin + Android SDK)।
WebView/HTML ব্যবহার করা হয়নি; সোর্স HTML শুধু রেফারেন্স হিসেবে `workspace.zip`-এ রাখা আছে।

> A native Android hair-growth protocol tracker: 7-day / 17-routine clinical schedule,
> exact-alarm reminders (10 min & 5 min before + on time), progress dashboard,
> multi-language UI (English / हिन्दी / বাংলা) and a fully editable protocol.

---

## 📥 Download & Install

| | |
|---|---|
| **APK** | [`HairGrowthOS-v1.0.apk`](../../releases/latest) — universal APK (~5 MB) |
| **Package** | `com.hairgrowth.os` |
| **Version** | 1.0 (versionCode 1) |
| **Min SDK** | 23 — Android 6.0 Marshmallow |
| **Target SDK** | 34 — runs on Android 6 → Android 16 |
| **ABI** | Universal — **32-bit + 64-bit** (no native `.so` files) |

ইনস্টল: APK খুলুন → "Install from unknown sources" অনুমতি দিন।

---

## 🎯 Features

### Home — daily protocol
- Today's protocol with the **7-day week strip** (per-day done/total counters).
- All **17 routines** grouped day by day; tap a routine to complete it
  (haptic + tone + animated strike-through tick + mint state).
- Completing the whole week triggers a native confetti celebration.
- **Long-press a routine** to edit / delete it · **"+ Add routine"** on each day.

### Dashboard — insights
- Weekly progress **orb** (animated gradient ring with orbiting satellite).
- Bento stats: streak, completed routines, weekly sparkline.
- 7-day **bar chart** (today highlighted), streak panel + **derma countdown**,
  weekly effectiveness **mini rings**.

### Reminders — native alarm engine
- One switch per day (off by default), exactly like the reference behaviour.
- When a day is on, **every routine reminds you three times**:
  - **10 minutes before** → “Minoxidil 5% — in 10 min”
  - **5 minutes before** → “Minoxidil 5% — in 5 min”
  - **on time** → “Time now — Minoxidil 5%”
- Notifications fire from a `BroadcastReceiver` + `AlarmManager` exact alarms, so
  **they arrive even if the app is closed or in the background**.
- Already-completed routines are skipped automatically.
- Re-scheduled automatically after reboot / app update (`BootReceiver`).
- "All on / All off / Test" quick actions. Vibration pattern `[140,70,140,70,240]`.

### Settings — customization
- Haptic feedback, sound effects toggles.
- **Language:** English (default) · हिन्दी · বাংলা — instant switch.
- 4 accent colors (gold / mint / rose / ice).
- **Customize protocol** — edit every routine's title, description and time.
- Reset week (unchecks tasks) · **Reset protocol** (restores the default template).
- 100% private: all data stays on the device (SharedPreferences).

---

## 🗓️ Default Protocol (17 routines / 7 days)

| Day | Routines | Times |
|---|---|---|
| Sunday | Minoxidil 5% (morning) · Minoxidil 5% (night) · Ketoconazole Shampoo | 09:00 · 21:00 · 21:30 |
| Monday | Minoxidil 5% × 2 | 09:00 · 21:00 |
| Tuesday | Minoxidil 5% (morning) · Minoxidil 5% (night) · Ketoconazole Shampoo | 09:00 · 21:00 · 21:30 |
| Wednesday | Minoxidil 5% × 2 | 09:00 · 21:00 |
| Thursday | Minoxidil 5% × 2 | 09:00 · 21:00 |
| Friday | Minoxidil 5% × 2 | 09:00 · 21:00 |
| **Saturday (Derma Day)** | Minoxidil 5% (morning) · Ketoconazole Shampoo · **Derma Roller (0.5 mm)** | 09:00 · 21:30 · 22:00 |

Every task, description and time above is user-editable from the Home screen; the table is the shipped default template.

---

## 🧱 Project Structure

```
HairGrowthAndroid/
├── app/
│   ├── build.gradle                     # module config (minSdk 23, targetSdk 34)
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/hairgrowth/os/
│       │   ├── App.kt                    # language bootstrap + notification channel
│       │   ├── MainActivity.kt           # header, floating dock, splash, toast, confetti
│       │   ├── data/
│       │   │   ├── Schedule.kt           # template + ScheduleStore (editable) + i18n resolver
│       │   │   └── Prefs.kt              # completion, prefs, reminders, streak, language
│       │   ├── reminder/
│       │   │   ├── NotificationHelper.kt
│       │   │   ├── ReminderScheduler.kt  # 10-min / 5-min / on-time exact alarms
│       │   │   ├── ReminderReceiver.kt
│       │   │   └── BootReceiver.kt
│       │   ├── ui/
│       │   │   ├── Ui.kt                 # palette, typefaces, drawable helpers
│       │   │   ├── EditDialogs.kt        # edit / add / delete routine
│       │   │   ├── views/                # OrbView, Charts, Confetti, Splash, Blob, Switch
│       │   │   └── fragments/            # Home, Dashboard, Reminders, Settings, Host
│       │   └── util/Fx.kt                # haptics + sine-tone player
│       └── res/
│           ├── values/strings.xml        # English (default)
│           ├── values-bn/strings.xml     # বাংলা
│           ├── values-hi/strings.xml     # हिन्दी
│           ├── font/                     # Bodoni Moda, Plus Jakarta Sans, Hind Siliguri
│           └── drawable/, mipmap-*/
├── keystore/hgos-release.jks             # release signing key
└── RELEASE_NOTES.md
```

---

## 🛠️ Build From Source

**Requirements:** JDK 17, Android SDK (platform 34 + build-tools 34.0.0), Gradle 8.7+.

```bash
cd HairGrowthAndroid
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk

# debug
gradle assembleDebug        # → app/build/outputs/apk/debug/app-debug.apk
# signed release
gradle assembleRelease      # → app/build/outputs/apk/release/app-release.apk
```

`local.properties` (not committed) must contain `sdk.dir=/path/to/android-sdk`.

Verify the built artifact:

```bash
$ANDROID_HOME/build-tools/34.0.0/aapt dump badging app/build/outputs/apk/release/app-release.apk
$ANDROID_HOME/build-tools/34.0.0/apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
```

---

## 🔐 Signing & Credentials

The only credential in this project is the **Android release signing key**
(there are no API keys, tokens or backend secrets — the app is fully offline).

| Item | Value |
|---|---|
| Keystore file | `HairGrowthAndroid/keystore/hgos-release.jks` |
| Keystore type | JKS |
| Key alias | `hgos` |
| **Store password** | `hgos2026` |
| **Key password** | `hgos2026` |
| Key algorithm | RSA 2048, `SHA256withRSA` |
| Validity | 2026-10-09 → 2056-10-01 |
| Certificate DN | `CN=HairGrowth OS, OU=Mobile, O=HairGrowth, L=Dhaka, C=BD` |
| Certificate SHA-256 | `BA:AB:F9:5C:31:FD:0A:3A:D5:66:50:CC:44:2D:AE:DC:D3:27:1E:7D:2B:3E:1E:FB:0D:7F:67:9A:17:CC:E8:43` |
| Certificate SHA-1 | `BF:BD:B0:F4:92:13:00:A9:5D:FA:1A:B6:BF:8B:A9:EC:15:98:E9:EB` |
| Signature schemes | v1 (JAR) ✔ · v2 (APK Signature Scheme v2) ✔ |

`app/build.gradle` reads these values from its `signingConfigs.release` block.

### Sign in / accounts
এই অ্যাপে **কোনো সাইন-ইন, লগইন বা ইউজার অ্যাকাউন্ট নেই** — কোনো সার্ভারও নেই।
তাই "sign-in key" বলতে কিছু নেই; উপরের সাইনিং কী-টিই এই প্রজেক্টের একমাত্র ক্রেডেনশিয়াল।

> ### ⚠️ Security warning
> এই রিপোজিটরি **পাবলিক**, এবং উপরের সাইনিং কী + পাসওয়ার্ড এখানে প্রকাশ করা হয়েছে
> (ব্যবহারকারীর স্পষ্ট অনুরোধে)। যে কেউ এই কী দিয়ে একই `applicationId`-এর APK
> সাইন করতে পারে। রিপোজিটরিটি **অবিলম্বে প্রাইভেট** করার পরামর্শ দেওয়া হচ্ছে;
> আরও নিরাপদ বিকল্প হিসেবে —
> ১) পাবলিক ডিস্ট্রিবিউশনের আগে **নতুন কী তৈরি করে রোটেট** করুন
> (`keytool -genkeypair …`), ২) `keystore/` ফোল্ডারটি `.gitignore`-এ রেখে কী আলাদা
> নিরাপদ জায়গায় রাখুন, ৩) পাসওয়ার্ড `keystore.properties`/CI secret-এ রাখুন।

---

## 🎨 Design

| Token | Value |
|---|---|
| Void / Ink | `#07080A` / `#0C0E12` |
| Accents | gold `#D4AF7A` · mint `#7EE0C3` · rose `#E8A0B4` · ice `#9EC5FF` |
| Text / Muted | `#F5F1E8` / `#8F8A80` |
| Type | Bodoni Moda (display) · Plus Jakarta Sans · Hind Siliguri (বাংলা) |

Animated glow blobs, glass panels, floating dock, splash strand animation,
confetti celebration, spring-animated ticks — all drawn natively (Canvas).

---

## 🔒 Privacy

No internet permission, no analytics, no accounts. Everything (schedule edits,
completion state, preferences, language, streak) is stored locally in
`SharedPreferences` and can be reset from Settings.

---

## ⚠️ Known Limitations / Verification Notes

- Built & verified with JDK 17 + Android SDK 34 + Gradle 8.9.
  Verified: Kotlin compilation, resource merge, `aapt` badging
  (`minSdk 23`, `targetSdk 34`, locales `bn`/`hi`), `apksigner` (v1 + v2), no native libs.
- **Not yet exercised on a physical device or emulator** in this environment
  (no KVM available), so runtime UI/notification behaviour has been verified
  statically only. Please report anything that needs tuning after the first run.
- Exact alarms need the *Alarms & reminders* permission on Android 12+; the app
  shows an in-app hint and falls back to inexact alarms if it is not granted.
- Battery-optimization settings on some OEM ROMs may delay alarms — the app
  requests nothing beyond `WAKE_LOCK` and asks the user from the Reminders screen.

---

## 📄 License

Personal project. All rights reserved by the repository owner.
