# PROGRESS: autonomous build run, Sat Nov 21 2026

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
