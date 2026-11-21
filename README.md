# Nobody Missing

**A hybrid-AI shelter kit: every evacuee found, every message understood, even with no signal.**
Built for #BuildHybridAI 2026 with **Firebase AI Logic hybrid inference** (Gemini Nano on the device, Gemini in the cloud). Android (main) + Chrome web hub. Apache-2.0.

## Value proposition

After floods and earthquakes, people mostly die **after** the event, in shelters. Elderly, deaf and hard-of-hearing evacuees miss spoken announcements and medicine. Shelter lists are handwritten and full of duplicates. Phone signal is often gone for days: Aceh had 4 days with no signal in Nov 2025, and BPS found that 179,000 evacuee records were really 60,426 households.

Nobody Missing runs the shelter's core work **on the staff device with no signal**. When signal returns, the cloud joins things up across shelters. **Evacuees don't need a phone.** They get information from a Big Screen, printouts, staff, and public pages on the shelter's local Wi-Fi.

## Features, and where the AI runs

| # | Feature | On the device (why) | Cloud (why) | Mode |
|---|---|---|---|---|
| F1 | Roster from a photo of a handwritten list → people + vulnerable flags, staff confirm each one | Photo read by Nano (privacy, no signal) | Only if staff approve it for a photo the device can't read | `ONLY_ON_DEVICE` + our privacy guard; `ONLY_IN_CLOUD` only after staff approve |
| F2 | Announcement → big-text picture cards on the Big Screen, public pages and print | Keyword icons (deterministic, instant), then Nano cards | Indonesian: the cloud first, because the device is measurably weaker there | `PREFER_ON_DEVICE` / `PREFER_IN_CLOUD` (measured) |
| F8 | Reach list: who must be told in person (deaf, hard of hearing, lives alone, 75+) | Rules on roster flags | — | Local |
| F4 | Care card from a medicine-bag photo; drug names matched by deterministic code, never auto-accepted | Photo read (medical privacy) | — | `ONLY_ON_DEVICE` |
| F9 | Medicine Round: schedule, ✓ given, can't-miss alerts (insulin, epilepsy, anticoagulants), shortages | Schedule, stock, alerts (pure code) | Combined supply request **by generic name, counts only** | Local → Firestore |
| F3 | Cross-shelter family search + duplicate merge (old spelling, honorifics, word order) | This shelter only, offline | All shelters (minimal fields) | Firestore |
| F11 | Outbox queue, batch mode, Signal Scout (a real check succeeds 2× → loud alert → sync in priority order), QR courier, Shelter Wi-Fi hub | Queue + state machine | Sync when signal returns | Local |
| F12 | Hazard-aware Field Check: at-home vulnerable people ranked P1/P2/P3 with "why", plus the nearest **higher** shelter for this hazard | Scoring is pure code; HAND lookup from the region pack | Rescue map when signal returns | Local; `ONLY_ON_DEVICE` for notes |

**Our routing on top of the SDK** (`web/src/features/inferenceRouter.ts`, `android/.../features/InferenceRouter.kt`). The SDK only switches on availability, so we add four things:
1. A **policy table** from Remote Config (`shared/rules/defaults.json → routerPolicy`) picks the device, a device draft, the cloud, asking staff, queueing or blocking, per task.
2. A **privacy guard** feature-detects the on-device model *before* calling. This guards against firebase-js-sdk#10635, where `ONLY_ON_DEVICE` silently uses the cloud. It also **discards any cloud answer** to an on-device-only request.
3. **Timeouts**: a slow device falls back to the cloud for non-private tasks. For private tasks it never falls back silently, so staff are asked.
4. **Schema validation** of every AI output (`shared/schemas/*`) before it enters the domain. Small models wrap JSON in prose, and we handle that.

Every AI result shows a badge (🏮 Device / ☁️ Cloud / ⚠️ Needs review / 🔤 Keywords) and the original photo or text beside it.

## What AI cannot do here (and how we guard it)
Look-alike drug names → a deterministic matcher, look-alike warnings and human confirmation. Strengths (mg/mcg, 0.5/5) → flagged when not on the list. No dose decisions, no pill identification by appearance, no diagnosis. Hazard maps are scenarios, so the app never says "safe", only "higher". Indonesian on the device is officially unsupported, so it is measured and routed (see `routerPolicy.announcement_id`).

## Architecture
Ports & adapters, with one shared domain and the same golden fixtures on both platforms:

```
shared/      rules (= Remote Config defaults), schemas, prompt pack, demo drug list, region pack, golden fixtures (synthetic only)
web/src/     domain/ (pure) · ports/ · adapters/ (Firebase AI Logic, Firestore, App Check, Auth, Remote Config, fakes, AdapterFactory)
             features/ (InferenceRouter + factory, FeatureFactory, roster, careCard, announcements, fieldCheck, scout, queue, familySearch) · ui/
android/app/ same layering in Kotlin + Jetpack Compose; JUnit runs shared/fixtures
hub/         Shelter Wi-Fi server (no AI, no deps)        tools/  eval scorer, region-pack guide
```
Factories: `AdapterFactory` (prod / emulator / test / demo), `InferenceRouterFactory` (built from the policy table), `FeatureFactory` (wires features to ports). Features never construct adapters.
The web UI is React with **no `useEffect`**: it uses `useSyncExternalStore`, event handlers and module-level init.

## Run it

```sh
# Web (Chrome desktop ≥148 for on-device Gemini Nano)
cd web && npm ci
npm test               # Vitest: shared fixtures + router + features
npm run dev            # DEMO mode (canned AI, no Firebase) unless web/.env.local is set
# Android
cd android && ./gradlew :app:testDebugUnitTest :app:assembleDebug   # JDK 21, Android SDK platform 37.2
# Shelter Wi-Fi hub (laptop hotspot)
npm --prefix web run build && node hub/server.mjs     # phones: http://<laptop-ip>:8787/#/public/news
# Firebase Emulator Suite
npx firebase-tools emulators:start --only firestore,auth   # then VITE_USE_EMULATOR=true in web/.env.local
```

Firebase setup: copy `web/.env.example` to `web/.env.local` and fill in the values. On Android, put `google-services.json` in `android/app/`. Never commit either file. **App Check is required**: register the debug token printed in the console (web) or logcat (Android) under Firebase console → App Check → Manage debug tokens.

### Notes for judges
- Views: `#/staff/today` (staff), `#/big` (Big Screen), `#/public/news` (evacuees). There is no login: a join QR is in Settings.
- On-device needs Chrome ≥148 with Gemini Nano, or a Gemini Nano 4 phone (Pixel 10/11, Galaxy S26+). Otherwise the cloud fallback runs, and the badges show which path answered.
- All data is **synthetic** (`shared/demo/shelter.json`). The drug list is a demo and **not for clinical use**.

## Privacy
Medical data stays local by default. Only counts by generic name leave the device for supply requests. Public search returns name, age band, shelter and status only, and a person can ask staff to hide their entry. Laws to respect: Indonesia UU PDP (Law 27/2022), Japan APPI.

## License
Apache-2.0, including the Kit mascot SVGs (original work).
