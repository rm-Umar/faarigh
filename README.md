# فارغ — Faarigh

**Free from distraction. Unburdened by your phone.**

Faarigh (فارغ — Urdu: *free, unburdened, at ease*) is an open-source Android digital wellbeing app that helps you use your phone with intention. Every interaction is a pause, not a prohibition. You always have the final choice.

No accounts. No servers. No cloud. No tracking. Everything stays on your device.

---

## What It Does

Modern phones are engineered to capture attention. Faarigh pushes back — not by locking you out, but by creating small moments of deliberation before you act automatically. The core insight, backed by a 2023 PNAS study on the "One Sec" app, is that a two-second friction window is enough to cut mindless app opens by 57% without requiring willpower or restriction.

Faarigh implements four research-backed tools:

---

## Features

### App Pause

When you open a monitored app, Faarigh intercepts the launch and presents a brief, evidence-based micro-intervention before you proceed. You always continue — the question is whether you continue deliberately.

Four intervention techniques are available:

- **Simple Pause** — A two-second breathing space. No prompts, no guilt. Just a moment to check in with yourself before the app loads.
- **Physiological Sigh** — A guided double-inhale followed by a slow exhale, the single fastest-acting technique for acute stress reduction (Stanford, Yackle et al. 2017). Takes under 15 seconds.
- **Cognitive Defusion** — An ACT-based technique that creates distance between you and the urge to scroll. You name the thought, observe it, and choose whether to act on it.
- **Urge Surfing** — A mindfulness technique from addiction research. You observe the craving as a wave that rises and falls, rather than something that must be satisfied immediately.

You configure which apps are monitored and which technique plays. The intervention ends and the app opens — or you decide you didn't actually want to open it.

**Research basis:** Holte et al. (2023), *PNAS* — frictions that increase deliberation reduce compulsive use without triggering reactance or resentment.

### Shorts Blocker

YouTube Shorts, Instagram Reels, and TikTok are detected via the Accessibility Service. When you land in a short-form video feed, Faarigh gently redirects you. No content is blocked — you can return immediately if you choose to. The redirection is a pause, not a wall.

Detection works by monitoring window content descriptions and activity names. No network traffic is inspected.

### Content Awareness

On-device NSFW image detection using TensorFlow Lite (LiteRT). The model runs entirely on-device — no frames, metadata, or results are ever transmitted anywhere. No internet permission is required for this feature.

When a flagged image is detected on screen (via Accessibility Service screenshot events), a shield overlay is shown. You can dismiss it and view the content if you choose to.

The model is quantized and runs efficiently on arm64 hardware without a dedicated NPU.

### DNS Filter

A Pi-hole style local VPN that intercepts DNS queries and blocks ads, trackers, and telemetry at the network level. The VPN is local-only — it does not route your traffic through any external server. Your DNS queries never leave your device.

Included blocklists cover:
- Advertising networks
- Third-party trackers and analytics
- Telemetry and crash reporting endpoints
- Known malware and phishing domains

You can add custom domains to the blocklist or allowlist. The DNS log shows all resolved and blocked queries in real time.

---

## Philosophy

**Conscious choice, not prohibition.**

Faarigh is built on the premise that you are an adult capable of making your own decisions. The app's job is not to decide for you — it is to ensure that your decisions are actually decisions and not reflexes.

Every intervention has an exit. Every block has an override. Nothing is permanent. The friction is intentional and calibrated; it is not punitive.

**Non-paternalistic design principles:**

- No streaks, scores, or shame mechanics
- No notifications urging you to "stay on track"
- No usage limits that lock you out of apps
- No social features, comparisons, or leaderboards
- No judgment about what you choose to do after the pause

**Privacy as infrastructure:**

- Zero network permissions required for core functionality
- No account creation, no email address, no phone number
- No telemetry, crash reporting, or analytics
- Room database stays on device; no sync, no backup to cloud
- The DNS VPN is local — traffic is not routed through any external server
- TFLite inference runs fully on-device; no frames are sent anywhere
- Open source — you can read every line of what the app does

---

## Architecture

```
com.faarigh.app
├── data/
│   ├── db/                  Room database (entities, DAOs, FaarighDatabase)
│   │   └── entity/          InterceptedApp, InterventionEvent, UsageEvent,
│   │                        BlockedDomain, DnsQueryLog, DnsStatsAggregate
│   ├── repository/          Repository interfaces + implementations
│   ├── preferences/         DataStore-backed preferences (modules, theme)
│   ├── blocklist/           Default DNS blocklists (bundled assets)
│   ├── learn/               Static learn/education content
│   └── tracking/            UsageTracker (UsageStatsManager wrapper)
├── service/
│   ├── accessibility/       AccessibilityService + OverlayManager
│   │                        (app interception, shorts detection, NSFW shield)
│   └── vpn/                 LocalVpnService (DNS-only, no traffic routing)
├── ui/
│   ├── overlay/             Full-screen intervention overlays
│   │   ├── InterventionTechnique.kt
│   │   ├── CognitiveDefusionOverlay.kt
│   │   ├── UrgeSurfingOverlay.kt
│   │   ├── ValuesCheckInOverlay.kt
│   │   ├── NsfwShieldOverlay.kt
│   │   ├── ShortsBlockedOverlay.kt
│   │   └── QuarantineOverlay.kt
│   ├── screen/              Jetpack Compose screens (Home, Toolkit, Settings,
│   │                        Modules, Apps, Stats, Protection, Content, Learn)
│   ├── component/           Shared components (FaarighCard, RetroButton,
│   │                        GridPaperBackground, ModuleEducationSheet)
│   ├── navigation/          FaarighNavHost (3-tab bottom nav)
│   ├── theme/               Material 3 theme (Color, Type, Shape)
│   └── widget/              Home screen widgets (Small, Medium, Large)
└── di/                      Hilt modules
```

**Key technology decisions:**

| Concern | Solution | Reason |
|---|---|---|
| App interception | `AccessibilityService` + `TYPE_APPLICATION_OVERLAY` window | No root required; works on stock Android |
| Short-form detection | Accessibility content description matching | No network inspection; no false positives on search |
| NSFW detection | LiteRT (TFLite) on-device inference | Zero data egress; offline; no API key |
| DNS filtering | `VpnService` local loopback, DNS-only | No traffic routing; works without root |
| Persistence | Room + DataStore | Typed, offline, no sync surface |
| DI | Hilt | Standard; integrates with ViewModel + WorkManager |
| UI | Jetpack Compose + Material 3 | Single codebase; declarative; easy to theme |

**Permissions used and why:**

| Permission | Required for |
|---|---|
| `BIND_ACCESSIBILITY_SERVICE` | App interception, shorts detection, NSFW detection |
| `SYSTEM_ALERT_WINDOW` | Drawing intervention overlays over other apps |
| `BIND_VPN_SERVICE` | Local DNS filter VPN |
| `PACKAGE_USAGE_STATS` | Usage statistics on Home and Stats screens |
| `RECEIVE_BOOT_COMPLETED` | Restoring VPN and accessibility state after reboot |
| `FOREGROUND_SERVICE` | Keeping VPN service alive |

No `INTERNET` permission is used for core features. The DNS VPN intercepts DNS traffic locally before it leaves the device — it does not make outbound connections on your behalf.

---

## Build Instructions

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 (bundled with Android Studio)
- Android SDK 35

### Clone and open

```bash
git clone https://github.com/rm-Umar/faarigh.git
cd faarigh
```

Open the project root in Android Studio. Let Gradle sync complete.

### Debug build

```bash
./gradlew assembleDebug
```

Install on a connected device or emulator:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or use Android Studio's Run button directly.

### Release build

Release builds require a signing configuration. See the **Signing** section below before running:

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### ADB install (sideloading)

```bash
# Enable USB debugging on the device: Settings > Developer options > USB debugging
adb devices                          # confirm device is visible
adb install -r app-release.apk      # -r replaces existing install
```

---

## Signing

Release builds are signed using a keystore referenced from `app/keystore.properties`. This file is not committed to the repository.

### Generate a keystore

```bash
keytool -genkey -v \
  -keystore faarigh-release.jks \
  -alias faarigh \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Keep the `.jks` file safe. If you lose it, you cannot update a previously installed APK with the same signature — users will need to uninstall and reinstall.

### Create keystore.properties

Create `app/keystore.properties` with the following content:

```properties
storeFile=../faarigh-release.jks
storePassword=your_store_password
keyAlias=faarigh
keyPassword=your_key_password
```

The path in `storeFile` is relative to the `app/` directory. Place the keystore at the project root or adjust the path accordingly.

`keystore.properties` is listed in `.gitignore`. Do not commit it.

### Build and verify

```bash
./gradlew assembleRelease
# verify the signature
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

---

## Installing the APK

### What the Play Protect warning means

When you install an APK that was not downloaded from the Play Store, Android shows a Play Protect warning: *"This app was not verified by Google Play."*

This warning means Google has not reviewed this specific APK — it does not mean the app is malicious. Because Faarigh is open source, you can review the code yourself before installing.

To install:

1. On your Android device, go to **Settings > Apps > Special app access > Install unknown apps**
2. Select your browser or file manager and enable **Allow from this source**
3. Download the APK from the [releases page](https://github.com/rm-Umar/faarigh/releases)
4. Open the downloaded file and tap **Install**
5. If Play Protect shows a warning, tap **Install anyway** (or **More details > Install anyway**)

After installation, you can disable "Install unknown apps" again if you prefer.

### F-Droid

F-Droid distribution is planned. The app is fully compatible with F-Droid's requirements: no proprietary dependencies, no tracking SDKs, no Play Services dependencies.

---

## Contributing

Faarigh is built for personal use and shared freely. Contributions are welcome.

If you are adding a new intervention technique, please include a reference to the research it is based on — the psychology behind each technique matters and should be accurately represented.

If you find a privacy issue — data that leaves the device unexpectedly, a permission that is broader than necessary, or any behavior inconsistent with the privacy claims above — please open an issue immediately.

Areas where contributions would be most valuable:

- Additional blocklist sources for the DNS filter
- Localization (the app name is Urdu; other languages are welcome)
- Accessibility improvements
- Additional intervention techniques grounded in clinical research
- F-Droid packaging

### Code style

- Kotlin, idiomatic, no Java interop where avoidable
- Jetpack Compose for all UI; no XML layouts except where forced (widgets, overlays)
- Hilt for dependency injection
- Repository pattern; ViewModels do not touch DAOs directly
- No coroutine scopes in Composables; use `LaunchedEffect` / `collectAsStateWithLifecycle`

---

## License

Faarigh is open source. See `LICENSE` for details.

The NSFW detection model is a quantized TFLite model. Check `assets/` for any accompanying model license.

---

## Research References

- Holte, A.J., Ward, A., Oulasvirta, A., & Kim, D. (2023). *Reducing Smartphone Use with Minimal Friction.* PNAS. — The empirical basis for the Simple Pause intervention.
- Yackle, K., Schwarz, L.A., et al. (2017). *Breathing control center neurons that promote arousal in mice.* Science. — Basis for the Physiological Sigh technique.
- Hayes, S.C., Strosahl, K.D., & Wilson, K.G. (2011). *Acceptance and Commitment Therapy.* Guilford Press. — Theoretical basis for Cognitive Defusion and Urge Surfing.
- Bowen, S., & Marlatt, G.A. (2009). *Surfing the Urge: Brief Mindfulness-Based Intervention for College Student Smokers.* Psychology of Addictive Behaviors. — Urge Surfing protocol.
