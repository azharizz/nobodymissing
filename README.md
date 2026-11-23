<p align="center">
  <img src="submission/assets/nobodymissing-banner.png" alt="Nobody Missing: a shelter kit with AI that keeps working when the network doesn't. Gemini Nano on the phone, Gemini in the cloud, Firebase AI Logic." width="100%">
</p>

<div align="center">
  <h1>Nobody Missing</h1>
  <p><strong>A hybrid-AI shelter kit: every evacuee found, every message understood, even with no signal.</strong></p>
  <p><em>Because after a disaster, nobody should be.</em></p>
  <p>
    <a href="submission/submission-description.md"><img src="https://img.shields.io/badge/SUBMISSION%20DESCRIPTION-FF7A2F?style=for-the-badge&logoColor=white" alt="Read the submission description"></a>
    <img src="https://img.shields.io/badge/%23BUILDHYBRIDAI%202026-1C2F4A?style=for-the-badge" alt="#BuildHybridAI 2026">
    <img src="https://img.shields.io/badge/WEB%20%2B%20ANDROID-FFB000?style=for-the-badge&logoColor=1C2F4A" alt="Web and Android">
  </p>
  <p>
    <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache--2.0-2F7A52.svg" alt="Apache 2.0 license"></a>
    <img src="https://img.shields.io/badge/Firebase%20AI%20Logic-hybrid%20inference-FFCA28.svg?logo=firebase&logoColor=black" alt="Firebase AI Logic hybrid inference">
    <img src="https://img.shields.io/badge/on%20device-Gemini%20Nano-FFB000.svg" alt="Gemini Nano on device">
    <img src="https://img.shields.io/badge/cloud-Gemini%203.5%20Flash--Lite%20%C2%B7%203.8%20Flash-2D6CDF.svg" alt="Gemini in the cloud">
    <img src="https://img.shields.io/badge/grounding-Google%20Maps%20%C2%B7%20Google%20Search-4285F4.svg" alt="Maps and Search grounding">
  </p>
  <p>
    <img src="https://img.shields.io/badge/web-React%2019%20%C2%B7%20Vite%208%20%C2%B7%20firebase%2013.4-61DAFB.svg" alt="Web stack">
    <img src="https://img.shields.io/badge/android-Kotlin%20%C2%B7%20Compose%20%C2%B7%20firebase--ai%2018.2-3DDC84.svg" alt="Android stack">
    <img src="https://img.shields.io/badge/App%20Check-enforced-2F7A52.svg" alt="App Check enforced">
    <img src="https://img.shields.io/badge/also-Firestore%20%C2%B7%20Remote%20Config%20%C2%B7%20Auth-F57C00.svg" alt="Firestore, Remote Config, Auth">
    <img src="https://img.shields.io/badge/tests-web%20%2B%20Android%20share%20one%20fixture%20set-5A6170.svg" alt="Shared golden fixtures">
  </p>
  <p>
    <a href="submission/submission-description.md">Submission</a> ·
    <a href="#-how-kit-decides">How Kit decides</a> ·
    <a href="#-see-it-work">See it work</a> ·
    <a href="#-measured-not-claimed">Measured</a> ·
    <a href="#-run-it">Run it</a> ·
    <a href="#-honest-limits">Honest limits</a>
  </p>
  <p><sub>No signal → Gemini Nano does the shelter's work on the phone → the outbox waits → signal returns → only what needs the cloud leaves, and the app shows exactly what left</sub></p>
</div>

> [!IMPORTANT]
> Every AI call goes through **Firebase AI Logic** (`firebase/ai` on web, `firebase-ai` + `firebase-ai-ondevice` on Android), with **App Check enforced**. Each feature has its own inference mode, chosen per task by a policy table in Remote Config. All demo data is **synthetic**; the demo drug list is **not for clinical use**.

<details>
<summary><kbd>Contents</kbd></summary>

- [At a glance](#-at-a-glance)
- [The problem](#the-problem)
- [How Kit decides](#-how-kit-decides)
- [See it work](#-see-it-work)
- [Every feature, and where its AI runs](#every-feature-and-where-its-ai-runs)
- [Measured, not claimed](#-measured-not-claimed)
- [Firebase AI Logic in the code](#firebase-ai-logic-in-the-code)
- [Architecture](#architecture)
- [Run it](#-run-it)
- [Notes for judges](#notes-for-judges)
- [Privacy](#privacy)
- [Honest limits](#-honest-limits)
- [Repository map](#repository-map)
- [License](#license)

</details>

## 🏮 At a glance

<table>
  <tr>
    <td width="20%" align="center"><strong>01 / WARN</strong><br><br>An official flood warning becomes plain words and the way to higher ground. On the phone, no signal.</td>
    <td width="20%" align="center"><strong>02 / COUNT</strong><br><br>A photo of a handwritten list becomes a roster. Names and health never leave the phone.</td>
    <td width="20%" align="center"><strong>03 / TELL</strong><br><br>Announcements become picture cards for the hall screen, and a list of who must be told in person.</td>
    <td width="20%" align="center"><strong>04 / WAIT</strong><br><br>Work that needs the internet waits safely in the outbox, then goes the moment signal returns.</td>
    <td width="20%" align="center"><strong>05 / FIND</strong><br><br>A daughter searches with a misspelled name. Her words stay on her phone; only the name goes out.</td>
  </tr>
</table>

<p align="center">
  <img src="submission/assets/film-hero.gif" alt="Kit, the lantern cat mascot, assembles and the Nobody Missing title appears" width="100%">
</p>

## The problem

After floods and earthquakes, many people are lost **after** the event, in the shelters. Elderly, deaf and hard-of-hearing evacuees miss spoken announcements and medicine. Shelter lists are handwritten and full of duplicates. And the signal is often gone for days:

| | |
| --- | --- |
| **975,079** | people displaced by the Sumatra floods, Dec 2025 (BNPB) |
| **4 days** | Aceh went without a single bar of signal, Nov 2025 |
| **179,000 → 60,426** | evacuee records that BPS found were really 60,426 households: the same people written down again and again |

A cloud-only AI app is useless in that shelter. A device-only one can't check medicines against a big model, find which pharmacy is open, or join up families across shelters. **Nobody Missing does both, and decides per task.** Evacuees don't need a phone at all: they get information from the hall's Big Screen, printouts, staff, and public pages on the shelter's local Wi-Fi.

## 🔀 How Kit decides

<p align="center">
  <img src="submission/assets/hybrid-router.gif" alt="Animated router: seven shelter tasks fall into Firebase AI Logic and are routed to the phone, the cloud, or the outbox; when signal returns the queued medicine check goes to the cloud" width="100%">
</p>

Every AI task goes through **one router** (`web/src/features/inferenceRouter.ts`, `android/.../features/InferenceRouter.kt`) before it reaches Firebase AI Logic. The SDK switches on *availability*; Kit adds the *reason*:

```mermaid
flowchart LR
  T[Shelter task] --> P{Policy table<br/>Remote Config}
  P -->|private: names, health, photos| D[ONLY_ON_DEVICE<br/>Gemini Nano]
  P -->|measured: device as good| PD[PREFER_ON_DEVICE<br/>Nano, cloud if no model]
  P -->|needs live data or a bigger model| C{Signal?}
  C -->|yes| CL[ONLY_IN_CLOUD<br/>Gemini + Maps / Search grounding]
  C -->|no| Q[Outbox<br/>waits, then sends by priority]
  P -->|private mode on| B[Cloud blocked]
  D --> G[Privacy guard<br/>discards any cloud answer]
  D & PD & CL --> V[Schema validation<br/>then staff confirm]
```

1. **Policy table** (`shared/rules/defaults.json → routerPolicy`, served by Remote Config) decides per task: the device, a device draft, the cloud, ask staff, queue, or block.
2. **Privacy guard** feature-detects the on-device model *before* calling, and **discards any cloud answer** to an on-device-only request (guards against firebase-js-sdk#10635, where `ONLY_ON_DEVICE` could silently use the cloud).
3. **Timeouts**: a slow device falls back to the cloud for non-private tasks. Private tasks never fall back silently; staff are asked.
4. **Schema validation** of every answer (`shared/schemas/*`) before it touches the domain. Small models wrap JSON in prose; we handle that.
5. **Where Kit ran**: every answer is logged with its real `inferenceSource`, the reason, the time, and **exactly what left the shelter**. Staff see it on the *Kit today* card.

## 🎬 See it work

Every GIF is cut from our 3-minute film, built from screen recordings of the real app (web in Chrome with Gemini Nano, Android in the emulator with real airplane mode). The status bar on top of each shot shows the signal, the inference mode, the engine and what was sent.

<table>
  <tr>
    <td width="50%"><img src="submission/assets/film-warning.gif" alt="An official flood warning becomes plain words on the phone in 3.8 seconds, offline"><br><sub><strong>1 · Warning in, no signal.</strong> Gemini Nano turns the official BPBD warning into plain words and a place to go, in 3.8 s, offline. <code>PREFER_ON_DEVICE</code></sub></td>
    <td width="50%"><img src="submission/assets/film-roster.gif" alt="A photo of a handwritten list read on the phone; Maybe already here catches a duplicate"><br><sub><strong>2 · Names. Health. Faces.</strong> A paper list is read on the phone, and “Maybe already here” catches the duplicate. Even with signal, it never leaves. <code>ONLY_ON_DEVICE</code></sub></td>
  </tr>
  <tr>
    <td width="50%"><img src="submission/assets/film-measured.gif" alt="Announcement fork: phone 83 percent vs cloud 67 percent, the phone wins"><br><sub><strong>3 · Measured → the phone wins.</strong> On 24 test announcements Nano scored 83% vs the cloud's 67%, so cards are made on the phone, and Kit flags who must be told in person.</sub></td>
    <td width="50%"><img src="submission/assets/film-medicine.gif" alt="A medicine label photo is read on the phone into the care card"><br><sub><strong>4 · One photo, read on the phone.</strong> The medicine label becomes the care card. Medical data stays local. <code>ONLY_ON_DEVICE</code></sub></td>
  </tr>
  <tr>
    <td width="50%"><img src="submission/assets/film-outbox.gif" alt="Medicine check waits in the outbox in real Android airplane mode"><br><sub><strong>5 · It waits.</strong> The insulin check needs the cloud, so it waits in the outbox, shown here in real Android airplane mode. Nothing is lost.</sub></td>
    <td width="50%"><img src="submission/assets/film-signal.gif" alt="Signal is back; the outbox sends; the cloud checks only the drug names"><br><sub><strong>6 · Signal is back.</strong> Scout confirms a real connection twice, then the outbox sends, most urgent first. The cloud sees <em>only the drug names</em>. <code>ONLY_IN_CLOUD</code></sub></td>
  </tr>
  <tr>
    <td width="50%"><img src="submission/assets/film-mapoff.gif" alt="Show the way: offline flood map, walk 794 m to 5 m higher ground"><br><sub><strong>7 · Higher ground, offline.</strong> Real flood data (Global HAND 30 m + OpenStreetMap) lives on the phone: walk 794 m, 5 m higher, no signal at all.</sub></td>
    <td width="50%"><img src="submission/assets/film-findopen.gif" alt="Find it open: which pharmacy is open now, through Grounding with Google Maps, only a rough location sent"><br><sub><strong>8 · Which pharmacy is open now?</strong> Live places need Grounding with Google Maps, so this one goes to the cloud, with a location rounded to about 1 km. <code>ONLY_IN_CLOUD</code></sub></td>
  </tr>
  <tr>
    <td width="50%"><img src="submission/assets/film-family.gif" alt="Family search: her words stay on her phone, only the name goes to the cloud; Pak Hasan Basri found, safe"><br><sub><strong>9 · Spelled wrong. Found anyway.</strong> “My father Hasan Basry, about 80” is understood on her phone; only the name goes out for other spellings. Pak Hasan Basri: safe.</sub></td>
    <td width="50%"><img src="submission/assets/film-everything.gif" alt="Pickup, urgent help, field report and printed lists, each routed the same way"><br><sub><strong>10 · And everything else.</strong> Pickup requests, urgent help, field reports and paper lists, all routed the same way.</sub></td>
  </tr>
  <tr>
    <td width="50%"><img src="submission/assets/film-proof.gif" alt="Where Kit ran: every answer with where, why and what left"><br><sub><strong>11 · Where. Why. What left.</strong> Every answer is logged with where it ran, why, and exactly what left the shelter.</sub></td>
    <td width="50%"><img src="submission/assets/film-private.gif" alt="Private mode: cloud blocked, nothing leaves the device"><br><sub><strong>12 · Private mode.</strong> One switch blocks the cloud for the whole shelter. On-device work keeps going.</sub></td>
  </tr>
</table>

### Real screens

Frames from the screen recordings of the running app (synthetic Aceh shelter).

| Warning in plain words | Show the way, offline | Ask Kit, from the flood map | “I need help”, understood on the phone |
| --- | --- | --- | --- |
| ![Warning made plain on this phone](submission/screenshots/warning-plain.jpg) | ![Walk 794 m to Masjid Jami' al Chairitah, 5 m higher](submission/screenshots/show-the-way.jpg) | ![Ask Kit answers from the flood map on this phone](submission/screenshots/ask-kit-map.jpg) | ![Kit understood: urgent, nothing sent to the cloud](submission/screenshots/help-urgent.jpg) |

| Check-in: maybe already here | Paper list read on the phone | Announcement → picture cards | Medicine check waits for signal |
| --- | --- | --- | --- |
| ![Maybe already here: Pak Hasan Basri, 82](submission/screenshots/checkin-maybe-here.jpg) | ![Original photo, read on this phone, nothing sent](submission/screenshots/roster-photo.jpg) | ![Lunch, Water, Announcement cards made on this phone](submission/screenshots/announce-cards.jpg) | ![Medicine check: waiting for signal](submission/screenshots/medcheck-waits.jpg) |

| Cloud medicine check | Family found | Where Kit ran | Measured: how well Kit does |
| --- | --- | --- | --- |
| ![Medicine check made in the cloud](submission/screenshots/medcheck-cloud.jpg) | ![Pak Hasan Basri, safe, possible match with a different spelling](submission/screenshots/family-found.jpg) | ![Where Kit ran: on this phone vs cloud, and what was sent](submission/screenshots/where-kit-ran.jpg) | ![Evaluation card: device vs cloud vs keywords](submission/screenshots/measured-card.jpg) |

| Android: no signal, working on this phone | Android: signal is back | Android: Where Kit ran | Private mode |
| --- | --- | --- | --- |
| ![Android home in airplane mode](submission/screenshots/android-offline.jpg) | ![Android: Signal is back here](submission/screenshots/android-signal-back.jpg) | ![Android: Where Kit ran with measured numbers](submission/screenshots/android-where-kit-ran.jpg) | ![Private mode on web](submission/screenshots/private-mode.jpg) |

## Every feature, and where its AI runs

Seventeen AI tasks, each with its own mode. Sensitive input never leaves the phone; the cloud gets work that needs live data or a bigger model, and only the minimum it needs.

| Task | Who uses it | Mode | Why | What can leave the phone |
| --- | --- | --- | --- | --- |
| `roster_read` | Staff: paper list photo → people + needs | `ONLY_ON_DEVICE` | names, ages, health | nothing (cloud only if staff approve) |
| `care_card_read` | Staff: medicine-bag / label photo | `ONLY_ON_DEVICE` | medical data | nothing |
| `checkin_parse` | Volunteer at the door, by voice | `ONLY_ON_DEVICE` | name + health | nothing |
| `help_triage` | Evacuee: “I need help” in own words | `ONLY_ON_DEVICE` | health words; no model → keyword rules | nothing |
| `pickup_request` | Evacuee: someone can't walk | `ONLY_ON_DEVICE` | health + mobility | only when they tap Send |
| `family_query` | Family: “my father Hasan Basry, about 80” | `ONLY_ON_DEVICE` | words about a missing relative | nothing |
| `field_report` | Volunteer notes from a house visit | `ONLY_ON_DEVICE` | private notes | nothing |
| `warning_plain` | Evacuee: official warning → plain words | `PREFER_ON_DEVICE` | no signal during evacuation | nothing |
| `announcement` / `_id` | Staff → Big Screen cards (EN / ID) | `PREFER_ON_DEVICE` | **measured**: device 83% vs cloud 67% / 83% | nothing |
| `ask_kit` | Evacuee questions, from today's notices | `PREFER_ON_DEVICE` | answers only from shown notices | nothing |
| `translate` | Notices for foreign-language evacuees | `PREFER_ON_DEVICE` | free, offline | nothing |
| `med_check` | Staff: drug names vs interactions | `ONLY_IN_CLOUD` | bigger model | **drug names + strengths only** |
| `find_open` | Staff: open pharmacy / dialysis / health post | `ONLY_IN_CLOUD` + **Maps grounding** | live places | **location rounded to ~1 km** |
| `warning_latest` | Evacuee: latest official warning | `ONLY_IN_CLOUD` + **Search grounding** | live news | rounded area |
| `name_variants` | Family search across shelters | `ONLY_IN_CLOUD` | other spellings | **the name only** (already public) |
| `supply_request` | Medicine shortages → district | `ONLY_IN_CLOUD` | combined request | **counts by generic name** |
| Private mode | Whole shelter | blocks every cloud task | the shelter decides | nothing |

Plus code with no AI where AI would be the wrong tool: drug-name matching, the Medicine Round schedule and can't-miss alerts (insulin, epilepsy, anticoagulants), the reach list (deaf, hard of hearing, lives alone, 75+), hazard-aware field priorities P1/P2/P3, Signal Scout and the QR courier.

## 📊 Measured, not claimed

<p align="center">
  <img src="submission/assets/film-data.gif" alt="We measured it: names 97 vs 100, ages 76 vs 97, needs 56 vs 98; each task runs where it's strongest" width="90%">
</p>

The routing numbers come from our own eval runs (`shared/eval/results.json`, shown in the app under Settings and *Where Kit ran*), not from guesses.

**Set B: 24 announcements → picture cards** (Chrome 157 + Gemini Nano on a laptop; Gemini 3.5 Flash-Lite)

| Path | English all-correct | Indonesian all-correct | Median time |
| --- | --- | --- | --- |
| 🏮 On the phone (Gemini Nano) | **83%** | **83%** | 7.4 s |
| ☁️ Cloud (Gemini 3.5 Flash-Lite) | 67% | 83% | 1.9 s |
| 🔤 Keywords only (no AI) | 50% | 75% | 0 s |

→ The phone is as good as the cloud here, so announcements stay on the phone: free, private, no signal needed.

**Set P: 12 handwritten evacuee sheets, 63 people** (`tools/eval/make-sheets.mjs`, 4 handwriting styles)

| Path | Names | Ages | Needs | Median time |
| --- | --- | --- | --- | --- |
| 🏮 On the phone (Gemini Nano) | **97%** | 76% | 56% | 9.3 s |
| ☁️ Cloud (Gemini 3.5 Flash-Lite) | 100% | 97% | 98% | 3.2 s |

→ Photos of people stay on the phone by default: names are read almost perfectly. Ages and needs are weaker there, so every row is checked by staff, who can approve a cloud read when signal is back.

## Firebase AI Logic in the code

One call site per platform, one model per request mode. Web (`web/src/adapters/firebaseAiLogic.ts`):

```ts
const model = getGenerativeModel(this.ai, {
  mode: r.mode,                       // ONLY_ON_DEVICE | PREFER_ON_DEVICE | ONLY_IN_CLOUD, chosen by the router
  inCloudParams: maps
    ? { model: r.model, tools: [{ googleMaps: {} }],
        toolConfig: { retrievalConfig: { latLng: rounded } } }        // Grounding with Google Maps
    : search ? { model: r.model, tools: [{ googleSearch: {} }] }       // Grounding with Google Search
    : { model: r.model, generationConfig: { responseMimeType: 'application/json', responseJsonSchema } },
  onDeviceParams: {
    createOptions: { expectedInputs: [{ type: 'text' }, { type: 'image' }] },   // photos read by Nano
    promptOptions: { responseConstraint: schema },
  },
});
const { response } = await model.generateContent(parts);
const source = response.inferenceSource === InferenceSource.ON_DEVICE ? 'device' : 'cloud';  // logged in Where Kit ran
```

Android (`android/app/src/main/java/org/nobodymissing/adapters/FirebaseAiLogicInference.kt`) does the same with `firebase-ai` 18.2.0 + `firebase-ai-ondevice` 16.0.0-beta08, `Tool.googleMaps()` and `RetrievalConfig`. Staff can download the on-device model ahead of time from Settings → *Prepare on-device AI*.

| Firebase product | What it does here |
| --- | --- |
| **AI Logic** | Every AI call, hybrid, on the Gemini Developer API (no-cost tier) |
| **App Check** | Enforced. reCAPTCHA Enterprise on web, Play Integrity on Android, debug tokens for judging |
| **Remote Config** | `rules` (router policy, measured accuracy, thresholds, can't-miss list) and `promptPack`, with bundled defaults for offline |
| **Firestore** | Cross-shelter family index, supply requests, alerts (counts only), with offline cache; rules unit-tested in the emulator |
| **Authentication** | Silent anonymous sign-in, shelter membership |

## Architecture

<p align="center">
  <img src="submission/assets/film-router.gif" alt="Every task goes through Firebase AI Logic and Kit decides where it runs: private data and no signal to the phone, measured better and needs the internet to the cloud" width="80%">
</p>

Ports & adapters, one shared domain, and the **same golden fixtures on both platforms**:

```mermaid
flowchart TB
  subgraph shared["shared/ (one source of truth)"]
    R[rules = Remote Config defaults<br/>routerPolicy · thresholds]
    S[JSON schemas]
    PP[prompt pack]
    F[golden fixtures]
    I[i18n EN / ID]
  end
  subgraph web["web/ · React 19 · Vite 8"]
    WD[domain: pure TS] --> WR[InferenceRouter] --> WA[Firebase AI Logic adapter<br/>firebase/ai 13.4]
  end
  subgraph android["android/ · Kotlin · Compose"]
    AD[domain: pure Kotlin] --> AR[InferenceRouter] --> AA[Firebase AI Logic adapter<br/>firebase-ai 18.2 + ondevice]
  end
  shared --> web
  shared --> android
  WA & AA --> FB[(Firebase<br/>AI Logic · App Check · Remote Config<br/>Firestore · Auth)]
  HUB[hub/ · Shelter Wi-Fi server<br/>no AI, no deps] --> EV[Evacuee phones<br/>public pages]
  web --> BIG[Big Screen in the hall]
```

```
shared/      rules (= Remote Config defaults), schemas, prompt pack, i18n (EN/ID), golden fixtures, eval results
shared/demo/ DEMO ONLY: synthetic Aceh shelter, region pack, demo drug list (not for clinical use)
web/src/     domain/ (pure) · ports/ · adapters/ (AI Logic, Firestore, App Check, Auth, Remote Config, fakes) · features/ · ui/
android/app/ same layering in Kotlin + Jetpack Compose; JUnit runs shared/fixtures
hub/         Shelter Wi-Fi server: the app on a laptop hotspot, news to phones, help requests in
tools/       eval sheet generator + scorer, region-pack builder (Global HAND 30 m + OSM)
```

Factories: `AdapterFactory` (prod / emulator / test / demo), `InferenceRouterFactory` (built from the policy table), `FeatureFactory` (wires features to ports). Features never construct adapters. The web UI uses **no `useEffect`**: `useSyncExternalStore`, event handlers and module-level init.

## 🚀 Run it

```sh
# Web (Chrome desktop ≥148 for on-device Gemini Nano)
cd web && npm ci
npm test               # Vitest: shared fixtures + router + features
npm run dev            # DEMO mode (canned AI, no Firebase) unless web/.env.local is set
npm run test:e2e       # Playwright flows incl. offline → signal, axe accessibility
npm run test:rules     # Firestore rules in the emulator

# Android (JDK 21, Android SDK platform 37.2)
cd android && ./gradlew :app:testDebugUnitTest :app:assembleDebug

# Shelter Wi-Fi hub (laptop hotspot)
npm --prefix web run build && node hub/server.mjs     # phones: http://<laptop-ip>:8787/#/public/news

# Firebase Emulator Suite
npx firebase-tools emulators:start --only firestore,auth   # then VITE_USE_EMULATOR=true in web/.env.local
```

**Firebase setup:** copy `web/.env.example` to `web/.env.local` and fill in your project's values. On Android, put `google-services.json` in `android/app/`. Never commit either file. **App Check is required**: register the debug token printed in the browser console (web) or logcat (Android) under Firebase console → App Check → Manage debug tokens. Publish `shared/rules/defaults.json` as the Remote Config parameter `rules` and `shared/prompts/pack.json` as `promptPack`.

## Notes for judges

- **Views:** `#/staff/now` (staff), `#/big` (Big Screen), `#/public/news` (evacuees). There is no login; a join QR is in Settings.
- **On-device** needs Chrome ≥148 with Gemini Nano (enable “Gemma 4 for Built-in AI” in `chrome://flags` if needed), or a Gemini Nano 4 phone (Pixel 10/11, Galaxy S26+). Otherwise the cloud fallback runs, and the badges say which path answered (🏮 Device / ☁️ Cloud / ⚠️ Needs review / 🔤 Keywords).
- **Try offline:** in Chrome DevTools → Network → Offline, then announce, read a photo, or ask Kit. Tap the signal button on staff Now to watch Scout, the outbox and *Where Kit ran*.
- A real shelter starts **empty**. Demo mode and Settings → “Load demo shelter” load the **synthetic** Aceh shelter (`shared/demo/`).

## Privacy

Medical data stays local by default. Only counts by generic name leave the device for supply requests; drug names alone for the medicine check; a rounded location for live places; the name alone for spelling variants. Public search returns name, age band, shelter and status only, and a person can ask staff to hide their entry. Laws we design for: Indonesia UU PDP (Law 27/2022), Japan APPI.

## ⚖️ Honest limits

- **Not medical advice.** No dose decisions, no pill identification by appearance, no diagnosis. Look-alike drug names go through a deterministic matcher and human confirmation; strengths not on the list are flagged.
- **Hazard maps are scenarios**, so the app never says “safe”, only “higher”.
- **Android on-device** is wired (`firebase-ai-ondevice`) but our recordings use the emulator, which has no Gemini Nano; there it falls back to keyword rules or queues for the cloud. The airplane mode in the film is real.
- **Grounding quota.** Maps and Search grounding are implemented end to end; the free-tier grounding quota was exhausted while we recorded, so the film shows the request and what leaves the phone, not a live list of places.
- **Indonesian on the device** is officially unsupported by the model, so we measured it (83%) instead of assuming.
- All people, shelters and the drug list in the demo are **synthetic**.

## Repository map

| Path | What's there |
| --- | --- |
| `shared/rules/defaults.json` | Router policy per task, measured accuracy, thresholds (= Remote Config `rules`) |
| `shared/eval/results.json` | Measured device vs cloud vs keywords numbers shown in the app |
| `web/src/features/inferenceRouter.ts` | Decide → privacy guard → AI Logic → timeout fallback → schema validation |
| `web/src/adapters/firebaseAiLogic.ts` | The Firebase AI Logic call (hybrid modes, Maps / Search grounding) |
| `android/app/src/main/java/org/nobodymissing/` | Same domain, router and features in Kotlin + Compose |
| `hub/server.mjs` | Shelter Wi-Fi hub |
| `tools/` | Eval sheet generator + scorer, region-pack builder |
| `submission/` | Submission description, film GIFs, screenshots |
| `PRODUCT.md` · `PROGRESS.md` | Product decisions and build log |

## License

Apache-2.0, including the Kit mascot SVGs (original work). Map data © OpenStreetMap contributors (ODbL); flood height from Global HAND 30 m.
