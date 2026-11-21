# PROGRESS: update, Sat Nov 21 2026 (second autonomous run)

## Status in one line
The live Firebase project `nobodymissing` is set up and working end to end (free Spark plan, **no billing**): real Gemini calls through Firebase AI Logic with App Check enforced, **on-device Gemini Nano working on this laptop**, anonymous Auth, Firestore + rules, and Remote Config. Everything is on local branch `day1-skeleton` and nothing has been pushed. Hosting has not been deployed.

## Done in this run
- **Firebase (CLI + console):**
  - web + Android apps registered; `web/.env.local` and `android/app/google-services.json` written locally and gitignored;
  - Firestore in asia-southeast2, with rules deployed;
  - reCAPTCHA Enterprise key, App Check **enforced** for AI Logic, web debug token registered;
  - Anonymous Auth on;
  - AI Logic on the **Gemini Developer API (no-cost)**, with optional AI monitoring left OFF;
  - Remote Config v3 (`rules`, `promptPack`);
  - Android Play Integrity registered with the debug SHA-1/SHA-256.
- **Verified live in Chrome:**
  - cloud cards with the ☁️ badge;
  - **on-device cards with the 🏮 badge** (Nano on Chrome 156);
  - a sync to Firestore wrote alerts (counts only), a supply request, the announcement and the membership record.
- **Bugs found by tests and fixed:**
  - Firestore rules let members add or promote other devices;
  - batch mode dropped an approved photo when offline;
  - the device timeout was too short for Nano (8 s → 15 s, measured);
  - the cloud translated Indonesian place names (prompt fixed);
  - the overloaded `gemini-3.8-flash` model now falls back to Flash-Lite.
- **New tests:**
  - Firestore rules unit tests (8, run in the emulator);
  - Playwright end-to-end tests (6, demo mode);
  - Android feature tests (announce, field report, family search, Remote Config override);
  - eval set B (24 EN/ID announcements) with a scorer and an in-browser runner.
- **Android:**
  - AI announcement cards with approve + sync;
  - field report flow;
  - family search;
  - Remote Config;
  - shelter membership on sign-in;
  - the router's backup model.
- Commits from now on carry no Claude co-author line (your request).

## Test results (this run)
```
web  vitest            Test Files 4 passed, Tests 136 passed
web  playwright e2e    6 passed
web  rules (emulator)  Tests 8 passed (twice in a row after adding clearFirestore)
android JUnit          FixturesTest 13 · FeaturesTest 5 · InferenceRouterTest 7 · RemoteRulesTest 2 -> 0 failures; assembleDebug OK
```

## Eval set B (measured, prompt pack v1)
| Path | EN | ID |
|---|---|---|
| Keywords (no AI) | 50% | 75% |
| Cloud flash-lite | 92% | 42% (places translated; prompt fixed, re-run pending) |
| Device Nano | pending (run cut by a browser disconnect) | pending |

## PENDING HUMAN now
1. **Device eval:** in Chrome, keep the tab in the **foreground**, open `http://localhost:5173` (`npm --prefix web run dev`), then run `await (await import('/src/eval/runB.ts')).runB()` in the console. Put the device-vs-cloud numbers into `routerPolicy.announcement_id` / `announcement` and republish Remote Config.
2. **Android on a real phone:** install the APK (`android/app/build/outputs/apk/debug/app-debug.apk`), copy the App Check debug token from logcat (`DebugAppCheckProvider`), and add it under Firebase console → App Check → Android app → Manage debug tokens. There was no room on this Mac for an emulator (5.6 GB free).
3. **Pixel Nano check:** run a scan and an announcement; the badge should say 🏮 Device.
4. **Optional:** delete the unused GCP project `nobody-missing-hk26`.
5. **Your choice:** deploy Hosting (`npx firebase-tools deploy --only hosting`, this is public) and push the branch.

---

# (Earlier report) PROGRESS: first autonomous run, Sat Nov 21 2026

## Status in one line
The code work for the Day 1–5 web scope and the core of Day 6–7 Android is done. It is tested on both platforms with the same fixtures, and the app runs in demo mode. **Not done, because a person must do them:** real Firebase project setup, the on-device Nano checks, evaluations on real photos, the video and the Devpost submission. Everything is on local branch `day1-skeleton`, and **nothing has been pushed**.

## What's done
| Area | State |
|---|---|
| Repo skeleton (§6.12) | `shared/ web/ android/ hub/ tools/ .github/workflows/`, Apache-2.0 LICENSE, README with the value proposition + device/cloud table + judge notes |
| `shared/` | Remote Config defaults (rules, router policy, priority weights, can't-miss list, thresholds), 6 JSON schemas, prompt pack, 40-drug demo list, keyword icons, synthetic demo shelter, synthetic Aceh region pack, **14 golden fixture sets** |
| Domain (web TS + Android Kotlin, identical) | priority (F12), drug matcher + substitution levels, Medicine Round schedule/alerts/shortages + combined requests, dedupe + family search, queue state machine + priority, router policy + privacy guard, keyword icons, reach list, schema validator, roster mapping, scout, QR courier, region-pack HAND lookup + nearest higher shelter |
| Factories (D16) | `AdapterFactory` (prod/emulator/test/demo), `InferenceRouterFactory` (from the policy table), `FeatureFactory` (web + Android) |
| Firebase | AI Logic hybrid adapter (web `firebase/ai` 13.4.0; Android `firebase-ai` 18.2.0 + `-ondevice` beta08), App Check (reCAPTCHA Enterprise + debug token / Play Integrity + debug), silent anonymous Auth, Remote Config with bundled defaults, Firestore with offline cache, `firestore.rules`, `firebase.json` + emulator config (verified: the emulators start) |
| Web features + UI | F1 scan → review → confirm; F4 care card + matcher warnings + confirm; F9 round ✓/✗/∅, can't-miss pinned, stock, supply request (counts only); F2 announce → keyword icons + AI cards → approve → Big Screen; F8 reach list ✓; F12 field list P1/P2/P3 + why + higher place + override + report; F11 Scout loop + full-screen alert + vibrate/beep + priority sync + QR courier export/import; F3 family search; Settings (Shelter-ready, language EN/ID, private mode, join QRs); Big Screen auto-rotation; Public News/Map/Help/Family; Kit SVG; PWA service worker; live hybrid counter. React with no `useEffect` |
| Hub (D15) | `hub/server.mjs`: serves the app on the hotspot, shares news to phones, collects help requests (tested: HTTP 200) |
| Android | Compose app with 5 tabs, camera → roster/meds via the router, privacy-guard prompts, schedule/alerts, keyword cards + reach list, field priority list, Scout loop + alert; **debug APK builds** |
| CI | `.github/workflows/ci.yml`: web typecheck + tests + build; Android JUnit |

## Test results (pasted)
```
web$ npx tsc --noEmit            -> typecheck OK
web$ npx vitest run
 Test Files  3 passed (3)
      Tests  129 passed (129)
     (src/domain/fixtures.test.ts: all shared fixtures; src/features/inferenceRouter.test.ts: 10; src/features/features.test.ts: 8)

android$ ./gradlew :app:testDebugUnitTest --rerun-tasks
org.nobodymissing.domain.FixturesTest        tests="13" skipped="0" failures="0" errors="0"   (13 suites = every shared fixture file)
org.nobodymissing.features.InferenceRouterTest tests="6" skipped="0" failures="0" errors="0"
android$ ./gradlew :app:assembleDebug -> BUILD SUCCESSFUL (app-debug.apk, 23 MB)

firebase emulators:exec --only firestore,auth -> "firestore emulator HTTP 200", script exited 0
```
Honest notes on test-first:
- Web: the fixtures and Vitest tests were written first and failed (modules missing) before the implementation. The same goes for the router and feature tests.
- Android: the JUnit test was written before the Kotlin domain, but the first Gradle run happened after both existed, so I did not watch it fail.

## PENDING HUMAN (step by step)
1. **Create the Firebase project.**
   - console.firebase.google.com → Add project. Enable **Firebase AI Logic** (Gemini Developer API), **Firestore**, **Authentication → Anonymous**, and **Remote Config**.
   - Add a Web app and copy its config into `web/.env.local` (template: `web/.env.example`).
   - Add an Android app with package `org.nobodymissing` and put `google-services.json` in `android/app/`. Never commit either file.
   - Update `.firebaserc` with your project ID.
2. **App Check (required).**
   - Web: create a reCAPTCHA Enterprise key and set it as `VITE_RECAPTCHA_ENTERPRISE_KEY`.
   - Run `npm run dev`, copy the debug token from the console, then go to App Check → Apps → Manage debug tokens → add it.
   - Android: run the debug build, copy the debug token from logcat (`DebugAppCheckProvider`), and add it the same way.
3. **Remote Config.** Create the parameters `rules` (the content of `shared/rules/defaults.json`) and `promptPack` (`shared/prompts/pack.json`) as JSON. Then publish.
4. **Deploy rules and hosting.** Run `npx firebase-tools login`, then `npm --prefix web run build && npx firebase-tools deploy --only firestore:rules,hosting`.
5. **Day-1 device checks.**
   - On the laptop (Chrome ≥148; enable "Gemma 4 for Built-in AI" in `chrome://flags` if needed), open Settings → "Prepare on-device AI". It should report `available`.
   - On the Pixel 10/11, install the APK. The top bar should say 🏮 Device after a scan, and the counter should increase "on device".
6. **Evaluation set E (Indonesian).**
   - Photograph 10 synthetic BNPB-style sheets and write their ground truth.
   - Run the device path and the cloud path, then `node tools/eval/score.mjs truth.json device.json` (and the same for cloud).
   - Put the measured numbers into `routerPolicy.announcement_id` / `roster_read` (the current 70/92 are **placeholders**).
7. **Region pack.** Follow `tools/region-pack/README.md` (needs GDAL) to replace the synthetic `aceh-demo.json` with real HAND/DEM data.
8. **Android `id-ID` offline speech** check on the phone (only needed if speech input comes back; it is cut-line #1).
9. Push the branch and open a PR yourself when you're happy. I did not push or publish anything.

## At risk
- **On-device behaviour is not yet proven on real hardware.** The web `LanguageModel.availability()` options and the Android on-device JSON quality are unmeasured.
- The Android cloud path puts the schema in the prompt instead of using a typed `responseJsonSchema`, so it is weaker than web. Validation still guards it.
- **The `announcement_id` routing numbers are placeholders** until set E is run.
- Android gaps vs web: no announcement AI cards, no field report (GPS + notes) flow, no QR courier UI, no family search UI, and state lives in memory (not persisted).
- Batch photos are memory-only, so they are lost on restart. IndexedDB is needed.
- No Playwright end-to-end tests yet. Evaluation sets A–D/F/G are not written.
- Firestore rules are written but have no rules unit tests. The membership "join" check is client-side (see the ponytail note in the rules).
- The web bundle is about 900 kB (Firebase), so the first load over a weak signal is slow. The service worker caches it after that.

## Exact next steps (in order)
1. Do PENDING HUMAN steps 1–5, then run the hosted app against real Firebase and fix any runtime errors in `adapters/firebaseApp.ts` and `firebaseAiLogic.ts`.
2. Android parity (Day 6–7):
   - persist state (DataStore/Room);
   - announcement AI cards via the router;
   - field report flow (GPS + notes);
   - QR courier (`chunk`/`join` already exist in the domain) and family search;
   - convert the schema to a typed `JsonSchema` for the cloud.
3. Write eval sets A–E, run them on both paths, and set the routing numbers in Remote Config (step 6).
4. Playwright end-to-end tests for: scan → confirm, announce → Big Screen, Scout → sync.
5. Firestore rules tests with the emulator (`@firebase/rules-unit-testing`).
6. Day 8: accessibility pass, more Kit poses, printable daily lists (roster, medicine round, reach list).
7. Day 9: the video (§8) and the Devpost story (§9). Upload to YouTube by Sun Nov 29.
