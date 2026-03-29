# Phase A — QA Checklist

Complete sections in order. Core functionality first, polish last.
Mark with [x] when passing, [!] if needs fixing, [-] if N/A.

---

## PRIORITY 1: Core Services (Must Work)

### 1.1 Build & Install
- [ ] `./gradlew assembleDebug` builds with zero errors
- [ ] APK installs on device via `adb install`
- [ ] App launches without crash on first open
- [ ] No 16KB alignment errors on Android 16

### 1.2 Onboarding & Permissions
- [ ] First launch shows onboarding screen (not home)
- [ ] "Enable in Settings" opens Android Accessibility settings
- [ ] Can find and enable "Mindful" in Accessibility settings
- [ ] After enabling, onboarding shows green checkmark for Accessibility
- [ ] "Enable DNS Filter" triggers Android VPN consent dialog
- [ ] Accepting VPN consent starts the VPN service
- [ ] VPN key icon appears in status bar
- [ ] VPN notification appears with correct text
- [ ] "Get Started" navigates to home screen
- [ ] Second launch skips onboarding (goes straight to home)

### 1.3 App Interception — Core Flow
- [ ] Go to Apps tab → installed apps list loads
- [ ] Select an app (e.g., Instagram) via toggle
- [ ] Open Instagram → intervention overlay appears
- [ ] Overlay covers full screen with dark background
- [ ] Breathing circle animation plays smoothly
- [ ] Timer counts down correctly (10, 9, 8...)
- [ ] "Breathe in..." / "Breathe out..." text alternates
- [ ] After timer: decision buttons appear with fade-in
- [ ] "No, I'll do something else" dismisses overlay and navigates back
- [ ] "Yes, open it" dismisses overlay and app continues
- [ ] Opening a non-selected app does NOT trigger overlay
- [ ] Overlay does NOT trigger on home screen or system UI

### 1.4 App Interception — Edge Cases
- [ ] Cooldown works: re-opening same app within 60s → no overlay
- [ ] Cooldown expires: opening same app after 60s → overlay shows
- [ ] Multiple rapid app switches → only one overlay, no crash
- [ ] Overlay does NOT get stuck (can always dismiss)
- [ ] Pressing hardware Back during overlay dismisses it
- [ ] Overlay does NOT trigger for Mindful's own app

### 1.5 VPN DNS Filter — Core Flow
- [ ] Go to Filters tab → "Load Default Blocklists" button works
- [ ] Domain counts display per category after loading
- [ ] VPN toggle ON → VPN starts, notification shows
- [ ] VPN toggle OFF → VPN stops, notification disappears
- [ ] With VPN active: normal browsing works (google.com loads)
- [ ] With VPN active: blocked domain returns error (e.g., doubleclick.net)
- [ ] Test explicit domain blocking: blocked adult domain → fails to load
- [ ] Test ad blocking: visit ad-heavy site → some ads don't load

### 1.6 VPN DNS Filter — State & Reliability
- [ ] Stop VPN from notification bar → toggle in app updates to OFF
- [ ] After stopping from notification → can re-enable from app
- [ ] VPN does NOT break: HTTPS, app store, push notifications, calls, SMS
- [ ] Category toggle OFF → those domains load again
- [ ] Category toggle ON → those domains blocked again
- [ ] Add custom domain → it gets blocked
- [ ] VPN survives screen off → screen on (still running)

### 1.7 NSFW Content Detection
- [ ] NsfwClassifier initializes without crash (check logcat for "NSFW classifier initialized")
- [ ] Open a browser with safe content → no intervention triggers
- [ ] Open explicit content in browser → intervention overlay appears
- [ ] No visible screenshot flash or indication when scanning
- [ ] Scanning does NOT trigger on home screen or system apps
- [ ] Scanning stops when overlay is showing (no double-trigger)

### 1.8 Shorts/Reels Detection
- [ ] Enable shorts blocking
- [ ] Open YouTube → navigate to Shorts tab → gets blocked (Back action)
- [ ] Normal YouTube (search, subscriptions, watch page) NOT affected
- [ ] Open Instagram → navigate to Reels tab → gets blocked
- [ ] Normal Instagram (feed, DMs, profile) NOT affected
- [ ] TikTok (if installed) triggers app-level interception

### 1.9 Usage Event Logging
- [ ] After "Yes, open it" → event logged as "proceeded"
- [ ] After "No, go back" → event logged as "turned_back"
- [ ] Events appear in Stats screen
- [ ] Events survive app restart

---

## PRIORITY 2: Core UI Functionality

### 2.1 Navigation
- [ ] Bottom nav shows all 5 tabs (Home, Apps, Filters, Stats, Settings)
- [ ] Tapping each tab navigates to correct screen
- [ ] Navigation state preserved when switching tabs
- [ ] Back button doesn't exit app from non-home tab

### 2.2 Home Screen
- [ ] Loads without crash
- [ ] Shows 0/0/0 stats when no events
- [ ] Accessibility Service status accurate (Active/Inactive)
- [ ] VPN status accurate and updates in real-time
- [ ] Stats update after interception events

### 2.3 App Selection Screen
- [ ] Search filters by app name
- [ ] Search filters by package name
- [ ] Selected count updates correctly
- [ ] Mindful's own package excluded from list
- [ ] List scrolls smoothly

### 2.4 Content Filter Screen
- [ ] Category toggles work (explicit, ads, telemetry)
- [ ] Custom domain add/remove works
- [ ] Enabled domain count is accurate

### 2.5 Stats Screen
- [ ] Time range chips work (Today, This Week, This Month)
- [ ] Empty state shows "No activity yet"
- [ ] With data: counts are correct
- [ ] Success rate percentage correct
- [ ] Recent activity shows events in order

### 2.6 Settings Screen
- [ ] Permission cards show correct status
- [ ] "Enable" buttons navigate to system settings

---

## PRIORITY 3: Module Onboarding Education

### 3.1 Onboarding Sheet UI
- [ ] Sheet appears when enabling a module for first time
- [ ] Progress dots show correctly
- [ ] Next/Back navigation works
- [ ] Animation slides correctly in both directions
- [ ] "Skip" dismisses without enabling
- [ ] "Enable [Module]" on last card activates module

### 3.2 Card Content Quality
- [ ] App Interception: 4 cards render, text readable
- [ ] DNS Filter: 4 cards render, text readable
- [ ] Content Detection: 4 cards render, text readable
- [ ] Shorts Blocker: 4 cards render, text readable
- [ ] Stats bullet points display correctly
- [ ] Citation links are tappable

### 3.3 Citation Verification (open each link)
- [ ] Gollwitzer 1999 → real paper on APA/DOI
- [ ] Lyngs et al. 2020 → real paper on ACM
- [ ] Lukas & Elhai 2023 → real paper on DOI
- [ ] Wood & Neal 2007 → real paper on DOI
- [ ] Binns et al. 2018 → real paper on ACM
- [ ] Kollnig et al. 2022 → real paper on DOI
- [ ] Exodus Privacy → real website
- [ ] Pi-hole → real website
- [ ] OISD → real website
- [ ] Voon et al. 2014 → real paper on PLOS ONE
- [ ] Grubbs et al. 2019 → real paper on DOI
- [ ] Wright et al. 2017 → real paper on DOI
- [ ] Twohig & Crosby 2010 → real paper on DOI
- [ ] Hayes et al. 2006 → real paper on DOI
- [ ] Montag et al. 2021 → real paper on Frontiers
- [ ] Bai et al. 2024 → real paper on Nature
- [ ] Tran 2022 → real paper on DOI
- [ ] Zhao 2021 → real paper on DOI
- [ ] Allcott et al. 2022 → real paper on AER
- [ ] eMarketer 2023 → real source

---

## PRIORITY 4: Data & Persistence

- [ ] Intercepted apps survive app restart
- [ ] Blocked domains survive app restart
- [ ] Usage events survive app restart
- [ ] Onboarding "done" flag survives app restart
- [ ] Database not corrupted after force-close

---

## PRIORITY 5: Stability & Edge Cases

- [ ] No internet → VPN doesn't crash, app works offline
- [ ] Accessibility Service disabled externally → app doesn't crash
- [ ] Another VPN app takes over → Mindful handles gracefully
- [ ] Rapid enable/disable VPN → no crash
- [ ] Large blocklist (all defaults loaded) → no OOM or slow startup
- [ ] No ANR under normal use
- [ ] App handles screen rotation without crash
- [ ] App survives process death ("Don't keep activities" on)

---

## PRIORITY 6: Performance & Battery

- [ ] App cold start < 2 seconds
- [ ] Overlay appears within 500ms of opening target app
- [ ] DNS resolution with VPN < 200ms for allowed domains
- [ ] NSFW scan < 500ms per frame
- [ ] 1 hour idle with all features: note battery % before/after
- [ ] 1 hour active use: note battery % before/after
- [ ] RAM under 100MB during normal use

---

## PRIORITY 7: Security & Privacy

- [ ] No data transmitted externally (verify with network monitor)
- [ ] VPN only intercepts DNS (port 53)
- [ ] NSFW screenshots never saved to disk
- [ ] Room DB sandboxed (not readable by other apps)

---

## PRIORITY 8: Polish (defer to Phase B)

These will change significantly in Phase B. Test lightly now, revisit later.

- [ ] Dark mode renders correctly
- [ ] Light mode renders correctly
- [ ] System theme toggle follows device setting
- [ ] App icon appears correctly in launcher
- [ ] App name shows as "Mindful" in launcher
- [ ] Export CSV button works (can be placeholder)

---

## Sign-off

- [ ] All Priority 1-5 items pass
- [ ] Priority 6-7 reviewed (note any concerns)
- [ ] Device: _______________
- [ ] Android version: _______________
- [ ] Date: _______________
- [ ] Ready for Phase B: Intervention Design
