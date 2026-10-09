# HairGrowth OS — Clinical Protocol (Native Android)

চুলের বৃদ্ধির ক্লিনিক্যাল প্রোটোকল ট্র্যাকার — **100% নেটিভ অ্যান্ড্রয়েড অ্যাপ্লিকেশন**
(Kotlin + Android SDK, Canvas-এ আঁকা UI, Material Components theme)।

> A fully native Android hair-growth protocol tracker: 7-day / 17-routine clinical
> schedule with a **pinned day selector**, exact-alarm reminders (10 min & 5 min
> before + on time), an insights dashboard, **day / night themes** that follow the
> system, selectable reminder tones, multi-language UI (English / हिन्दी / বাংলা)
> and a completely editable protocol.

---

## 📥 Download & Install

| | |
|---|---|
| **APK** | [`HairGrowthOS-v1.0.1.apk`](../../releases/latest) — universal (~5 MB) |
| **Package** | `com.hairgrowth.os` |
| **Version** | 1.0.1 (versionCode 2) |
| **Min SDK** | 23 — Android 6.0 Marshmallow |
| **Target SDK** | 34 — runs on Android 6 → Android 16 |
| **ABI** | Universal — **32-bit + 64-bit** (no native `.so` files) |

ইনস্টল: APK খুলুন → "Install from unknown sources" অনুমতি দিন।

---

## 🎯 Features

### Home — pinned day selector + daily protocol
- The **7-day selector bar is pinned to the very top** and never scrolls out of
  view. Opening the app always lands on **today**; the user switches days from the
  fixed bar and only the selected day's routines scroll underneath — no hunting,
  no auto-scroll jumping.
- Each chip shows its own done/total counter, today carries an accent ring.
- Today's focus card, per-day progress chip, animated section underline.
- Tap a routine to complete it (haptic + premium click cue + animated
  strike-through tick). Completing the whole week fires a confetti celebration.
- **Long-press a routine** to edit / delete · **"+ Add routine"** on every day.

### Dashboard — insights
- Weekly progress **orb** (animated gradient ring with orbiting satellite).
- Bento stats: streak, completed routines, weekly sparkline.
- 7-day **bar chart** (today highlighted), streak panel + **derma countdown**,
  weekly effectiveness **mini rings**.

### Reminders — native alarm engine
- One switch per day; when a day is on, **every routine reminds you three times**:
  - **10 minutes before** → “Minoxidil 5% — in 10 min”
  - **5 minutes before** → “Minoxidil 5% — in 5 min”
  - **on time** → “Time now — Minoxidil 5%”
- Fires from a `BroadcastReceiver` + `AlarmManager` exact alarms, so reminders
  **arrive even if the app is closed or in the background**, and are re-scheduled
  after reboot / update (`BootReceiver`).
- Already-completed routines are skipped. Vibration pattern `[140,70,140,70,240]`.
- **Notification icon is the app's own mark** (monochrome silhouette of the
  launcher glyph, with the launcher icon as the large icon).

### Sound — real audio design, not beeps
- **Click cue:** a short, warm, low-amplitude tap (filtered sine + soft noise
  transient, fast attack, ~120 ms decay) used for every button, chip and
  check-off — subtle rather than cheap/clicky.
- **Six reminder tones** to choose from in Settings, each auditioned when picked:
  `Default chime · Soft chime · Bell · Marimba · Pulse · Digital`.
- Everything is **generated procedurally at build time** by
  `tools/gen_sounds.py` (additive/FM synthesis) — no downloaded or copyrighted
  audio, no licence questions, tiny files (mono 22.05 kHz 16-bit WAV).
- The chosen tone is used by the Test button, in-app previews **and** the
  notification channels (Android ties notification sound to the channel, so one
  channel per tone is prepared).

### Day / Night theme
- **Follows the system by default** — light while the device is in day mode, dark
  at night — and can be forced from Settings → **Appearance**
  (Follow system · Light · Dark).
- Two complete palettes (`values/` + `values-night/`), status/navigation bar icon
  appearance switched to match, so typography stays readable in both modes.

### Settings — customization
- Appearance (system / light / dark), reminder sound picker with preview.
- Haptic feedback and sound-effect toggles.
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

Every task, description and time above is user-editable from the Home screen;
the table is the shipped default template.

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
│       │   ├── App.kt                    # day/night mode + language + tone channels
│       │   ├── MainActivity.kt           # header, floating dock, splash, toast, confetti
│       │   ├── data/
│       │   │   ├── Schedule.kt           # template + ScheduleStore (editable) + i18n resolver
│       │   │   └── Prefs.kt              # completion, prefs, reminders, theme, tone, language
│       │   ├── reminder/
│       │   │   ├── NotificationHelper.kt # per-tone channels, app-icon notification
│       │   │   ├── ReminderScheduler.kt  # 10-min / 5-min / on-time exact alarms
│       │   │   ├── ReminderReceiver.kt
│       │   │   └── BootReceiver.kt
│       │   ├── ui/
│       │   │   ├── Ui.kt                 # day/night palette, typefaces, drawable helpers
│       │   │   ├── EditDialogs.kt        # edit / add / delete routine
│       │   │   ├── views/                # OrbView, Charts, Confetti, Splash, Blob, Switch
│       │   │   └── fragments/            # Home, Dashboard, Reminders, Settings, Host
│       │   └── util/
│       │       ├── Fx.kt                 # haptics
│       │       └── SoundEngine.kt        # SoundPool cues + MediaPlayer tone previews
│       └── res/
│           ├── raw/                      # generated UI cues + 6 reminder tones (WAV)
│           ├── values/strings.xml        # English (default)
│           ├── values-night/             # night palette + theme
│           ├── values-bn/strings.xml     # বাংলা
│           ├── values-hi/strings.xml     # हिन्दी
│           ├── font/                     # Bodoni Moda, Plus Jakarta Sans, Hind Siliguri
│           └── drawable/, mipmap-*/
├── keystore/hgos-release.jks             # release signing key
├── tools/gen_sounds.py                   # procedural sound generator (build-time tool)
└── RELEASE_NOTES.md
```

---

## 🛠️ Build From Source

**Requirements:** JDK 17, Android SDK (platform 34 + build-tools 34.0.0), Gradle 8.7+.

```bash
cd HairGrowthAndroid
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk

# regenerate the bundled sounds (optional — the WAVs are committed)
python3 tools/gen_sounds.py app/src/main/res/raw

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

| Token | Day | Night |
|---|---|---|
| Surface | `#F7F4EE` | `#07080A` |
| Panel | `#FFFFFF` | `#14161C` |
| Text / Muted | `#15171D` / `#6E6A62` | `#F5F1E8` / `#8F8A80` |
| Accents | gold `#A9791F` · mint `#1B8A69` · rose `#B85275` · ice `#3C6BAE` | gold `#D4AF7A` · mint `#7EE0C3` · rose `#E8A0B4` · ice `#9EC5FF` |

Type: Bodoni Moda (display) · Plus Jakarta Sans · Hind Siliguri (বাংলা).

Animated glow blobs, gradient glass panels with elevation, a floating dock that
never hides, animated section underlines, splash strand animation, confetti
celebration and spring-animated ticks — all drawn natively on Canvas.

---

## 🔒 Privacy

No internet permission, no analytics, no accounts. Everything (schedule edits,
completion state, preferences, theme, language, streak) is stored locally in
`SharedPreferences` and can be reset from Settings.

---

## ⚠️ Known Limitations / Verification Notes

- Built & verified with JDK 17 + Android SDK 34 + Gradle 8.9.
  Verified: Kotlin compilation, resource merge, `aapt` badging
  (`minSdk 23`, `targetSdk 34`, locales `bn`/`hi`), `apksigner` (v1 + v2), no native libs.
- **Not yet exercised on a physical device or emulator** in this environment
  (no KVM available), so runtime UI, theme-switch and notification behaviour have
  been verified statically only. Please report anything that needs tuning after
  the first run.
- Exact alarms need the *Alarms & reminders* permission on Android 12+; the app
  shows an in-app hint and falls back to inexact alarms if it is not granted.
- Battery-optimization settings on some OEM ROMs may delay alarms — the app
  requests nothing beyond `WAKE_LOCK` and asks the user from the Reminders screen.

---

## 📄 License

Personal project. All rights reserved by the repository owner.
