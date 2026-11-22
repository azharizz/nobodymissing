# PRODUCT.md: Nobody Missing, a shelter kit for #BuildHybridAI 2026

> **Read this first.** This file is the single source of truth for a new agent or teammate with no prior context.
> It covers the hackathon, the platform's technical limits, the competition, the real-world evidence, and the product spec.
> Written 2026-11-21. Facts marked ⚠️ are unverified or need re-checking before you rely on them.
> Product name: **Nobody Missing** (final). All decisions so far: §12 Decisions log. Possible tagline: "Every evacuee found, every message understood, even with no signal."
> Mascot: **Kit**, a calm firefighter cat (see §6.8).

---

## 0. TL;DR

- **What:** A hybrid-AI kit for evacuation shelters, global by design, demo story in Aceh. Staff photograph handwritten evacuee lists → structured roster with vulnerable flags. Announcements become big-text Kit cards for deaf and elderly evacuees, with a reach list. Medicine bag photos → care cards → a shelter-wide **Medicine Round** with can't-miss alerts and supply requests. A **hazard-aware Field Check** list finds vulnerable people still at home. Works with no signal: batch mode, airplane mode and a **Signal Scout** phone that syncs when signal returns.
- **Who:** shelter staff and volunteers, plus evacuees who miss information or care: **deaf and hard-of-hearing people, and all elderly people**. In Japan this group is officially called 要配慮者, "people requiring special consideration".
- **Why hybrid is real, not decoration:**
  - Shelters lose signal for 24–72 h or longer, so the core runs **on the device**.
  - Evacuee medical and identity data is private, so it is read **on the device**.
  - Merging many shelters, family search, drug-interaction checks and translation need the **cloud**, used when any signal returns.
- **Key design insight:** only ~53% of people in their 70s in Japan use a smartphone. **The evacuee does not need a phone.** The system works through the staff device, a big shared screen (TV or laptop) and printouts.
- **Deadline:** **Mon Nov 30 2026, 2:00 PM PT** (= Nov 30, 22:00 UTC). No extensions.

---

## 1. The hackathon: everything you must know

### 1.1 Basics
- Name: **#BuildHybridAI 2026**, sponsored by Google / Firebase, run on Devpost: https://firebasehybridaihackathon.devpost.com/
- Submission period: **Nov 11 to Nov 30 2026, 2:00 PM PT.** Judging Dec 10–16. Winners around Dec 21.
- About 3.6k participants. Individuals, teams and organizations are all allowed.
- Not eligible: residents of Brazil, Italy, Quebec, Crimea, Cuba, Iran, Syria, North Korea, Sudan, Belarus, Russia (see rules.txt for the full list).
- Once the deadline passes, the submission is locked and **commits pushed after the deadline are not evaluated.** To keep building, fork into a new repo.
- Upload the YouTube video early, because processing takes time.

### 1.2 The challenge (official wording)
> "On-device models are fast, private and free to run, but they aren't on every device. #BuildHybridAI challenges you to build apps that use both. Using the Firebase AI Logic Hybrid Inference SDK, your app runs prompts on-device when a local model is available and falls back to the cloud when it isn't."

- Build for **at least one** platform:
  - **Web (Chrome, web app or extension):** Gemini Nano 4 via Firebase AI Logic.
  - **Android:** Gemini Nano 4 via Firebase AI Logic.
  - **iOS:** Apple Foundation Models via Firebase AI Logic.
- **Multi-platform apps get "strong consideration" for the Grand Prize** ("that's where a single hybrid SDK really shines"). "An outstanding single-platform app can still win."
- Official example idea: "a notes app that summarizes on-device offline and hands long documents to the cloud." This shows the expected level: **practical, not exotic**.
- FAQ: "Firebase wants to see both on-device and cloud inference… You choose per feature: for example, keep sensitive data on-device only and use hybrid or cloud for other features."
- FAQ: "Practical apps that use AI for everyday tasks… aim for a creative, non-obvious use of hybrid inference that solves a real problem for a specific audience."
- **ALL AI inference must go through Firebase AI Logic.** No direct calls to other model APIs. Speech-to-text from the operating system is not an LLM call (see §2.6).

### 1.3 Hard requirements (Stage One is pass/fail; miss one and you're out)
1. A Gen AI app using **Firebase AI Logic** with hybrid inference, using both on-device and cloud.
2. **App Check is REQUIRED.** It was auto-enforced for AI Logic on **Nov 2 2026**, so without it calls simply fail. Debug tokens are accepted in submissions.
3. Encouraged: **Remote Config, Firestore, Authentication.** We use all three.
4. **Public GitHub or GitLab repo** with all the source code, assets and setup instructions, and it must state the value proposition.
5. The repo must show Firebase AI Logic **imported and actually called at runtime**, not just named in the README.
6. **An open-source license** that GitHub detects and shows in the repo's About section. Apache-2.0 is preferred.
7. **Demo video ≤ 3 minutes on YouTube, public.** Anything after 3:00 may be ignored. It must be in English or have English subtitles, and must show the app **running on the selected platform(s)**.
8. A text description covering how Firebase AI Logic is used, the features, and the value proposition.
9. **Select at least one platform** when submitting. This decides the "Best of" category.
10. The **app must at least support English**, and all submission materials must be in English. ⇒ The UI needs an English mode even if the demo is set in Japan.
11. The app must be free of charge.
12. A hosted URL is "strongly encouraged" but not required. Judges may judge from the description, images and video alone.
13. Never put production API keys, secrets or real personal data in the repo or the testing instructions. **Use only synthetic evacuee data.**

### 1.4 Judging
- **Stage One:** pass/fail on requirements.
- **Stage Two:** a score of 1–5 on **4 equally weighted criteria**, averaged:
  1. **Technological Implementation:** "How well is the project built, and how effectively does it use Firebase AI Logic hybrid inference?"
  2. **Design:** "A complete, coherent product experience, not just a technical proof of concept?"
  3. **Potential Impact:** "A credible, specific case for solving a real problem for a real audience, and does the solution address it based on what's demonstrated?"
  4. **Quality of the Idea:** "A creative, **non-obvious** use of Firebase AI Logic, and does the team show genuine understanding of the problem space?"
- **Ties are broken by criteria in that order, so Tech Implementation is the tiebreaker.**
- The rules say judging "may utilize expert panels, peer review, **automated AI-driven analysis**". ⇒ The write-up and README must be explicit and well-structured: state the per-feature device/cloud split, the user, and the evidence in plain words.
- **First 30 seconds of the video** (Cynthia Jiang, Firebase, at the Build Session): show **who it's for, the problem, and how Firebase AI Logic makes it work**. "Judges may evaluate from the video alone, so show the hybrid features working early."
- Judge listed: **Ankita Saxena** (no title or photo on Devpost; role unverified). More judges may be added.
- Individual scores and feedback are generally not shared.
- Agents, MCP and function calling are **NOT judging criteria**. "Agents" in the docs means coding agents for building the app (Antigravity, Claude Code, Firebase Agent Skills). MCP appears once, as one way to prove usage in code.

### 1.5 Prizes ($30k)
| Prize | Cash | Extras |
|---|---|---|
| Grand Prize | $10,000 | Interview, featured Firebase blog post, social mention |
| 1st Runner Up | $7,000 | Interview, blog post, social mention |
| 2nd Runner Up | $4,000 | Social mention |
| Best iOS App | $3,000 | Social mention |
| Best Android App | $3,000 | Social mention |
| Best Web/Chrome App | $3,000 | Social mention |
One project can win **1 grand-tier prize + 1 "Best of"**.

### 1.6 Google Account Quota (optional, for billed models such as image generation)
- Each account lasts 4 days from issue. One per entrant, per project, per sprint. Approval is at Google's discretion, and you pay anything beyond the quota.
- **Sprint 3 (final): request by Wed Nov 25, 10:00 AM PT, account issued Fri Nov 27.**
- **We don't need it.** The core features use free models. Request it only if we add billed models.

### 1.7 Free cloud models (no billing needed)
- **Gemini 3.8 Flash**, **Gemini 4 Neon**, **Gemini 3.5 Flash-Lite** (the default hybrid cloud fallback is `gemini-3.5-flash-lite`).
- Billing required: image generation (Nano Banana 2.1) and some others. Check the "billing required" label on the Firebase AI Logic models page.
- Official free-tier idea seeds: a Live API voice/camera coach with function calling, a PDF read-aloud using `gemini-3.8-flash` + `gemini-3.1-flash-tts-preview`, and a multiplayer tabletop game master.

### 1.8 Forum notes
- In Chrome, the model component may report itself as "v3Nano". That's fine as long as you access it through the AI Logic SDK. To get Nano 4 you may need `chrome://flags`, search "Gemma 4 for Built-in AI" and enable it. ⚠️
- iOS builds need a **Mac with Xcode 27 / iOS 27** (a forum user is looking for a Mac teammate).
- Unanswered: whether an AI voiceover is OK in the demo video (assume it is, but recording a real voice is safest), and how to give judges free access under the free-tier limits for EEA/UK users.

---

## 2. Platform technical facts (Firebase AI Logic hybrid)

### 2.1 Inference modes (create one model instance per mode)
| Mode | Behaviour |
|---|---|
| `PREFER_ON_DEVICE` | Uses the device; falls back to the cloud if the model is unsupported, still downloading, or given unsupported input |
| `ONLY_ON_DEVICE` | Throws instead of falling back; waits for the model download |
| `PREFER_IN_CLOUD` | Uses the cloud; falls back to the device when offline |
| `ONLY_IN_CLOUD` | Cloud only |
- **The SDK switches only on availability.** Routing by content (privacy, task type, measured accuracy) is **our own code**, and that is what scores on Tech.
- Web: the SDK response reports which backend answered. Show it in the UI with a device/cloud badge.

### 2.2 Web (Chrome desktop)
- Chrome desktop ≥139 (Prompt API is stable on the web from Chrome 148). Firebase JS SDK v12.5.0+ is the minimum; entrants report using 13.x. ⚠️ Use the latest.
- **Not on Chrome Android, Chrome iOS or Android WebView.** So Capacitor, Tauri, Electron and Dioxus wrappers do NOT get on-device inference.
- Device requirements: Windows 10/11, macOS 13+, Linux or Chromebook Plus; **22 GB free disk**; >4 GB VRAM or 16 GB RAM with 4+ CPU cores; an unmetered network for the first download.
- On-device input: text, plus **JPEG/PNG images** (may need `chrome://flags/#prompt-api-for-gemini-nano-multimodal-input`). Structured output with JSON schema (`responseConstraint`).
- Through Firebase hybrid: **single-turn only**. Context is small (about 4k tokens, from a third-party source ⚠️); exceeding it throws `QuotaExceededError`.
- **On-device languages: en, ja, es, de, fr only. Indonesian is NOT supported.** Japanese works, so a Japan-based demo is safe.
- `initializeDeviceModel()` / the model download must be triggered by a **user gesture**. Pre-download at onboarding (the FriendlyMeals sample shows the pattern).
- App Check web debug token: `self.FIREBASE_APPCHECK_DEBUG_TOKEN = true` before `initializeAppCheck`.
```js
const ai = getAI(app, { backend: new GoogleAIBackend() });
const model = getGenerativeModel(ai, { mode: InferenceMode.PREFER_ON_DEVICE });
```

### 2.3 Android (experimental)
- `com.google.firebase:firebase-ai` + `com.google.firebase:firebase-ai-ondevice` (not in the BoM). Versions seen: 17.11.0 + 16.0.0-beta01 in the docs, and 18.1.0 + 16.0.0-beta07 in one entrant's repo. ⚠️ Use the latest.
- Uses the ML Kit Prompt API / Gemini Nano through AICore. Input is text plus **one Bitmap**.
- Devices with Gemini Nano 4 (FAQ): **Pixel 10 and 11, Galaxy S26+, Z Fold8, Z Flip8**. On other devices `PREFER_ON_DEVICE` falls back to the cloud.
- Model download is a background job (`FirebaseAIOnDevice.download()`); don't block the UI. The response reports `inferenceSource`.
- The API naming differs between docs and blog posts (`OnDeviceConfig(mode=…)` vs `inferenceMode = InferenceMode.PREFER_ON_DEVICE`). Check the current docs.
- **Android on-device languages:** Google's ML Kit Prompt API page lists only **English and Korean as "validated"**. Indonesian is not listed, and Japanese is not confirmed. ⚠️ Test on a real device.

### 2.4 iOS (experimental)
- FirebaseAILogic SDK 12.13.0+ via **SPM only**. Requires a Mac with Xcode 27 and iOS 27 ⚠️ (per the forum). Runs only on Apple Intelligence devices (e.g. iPhone 15 Pro and newer). The app cannot trigger the model download.
- `HybridModel(primary:, secondary:)` with `FirebaseAI.SystemLanguageModel.default` plus `ai.geminiModel(name:)`.
- **On-device is text only**, 4096 tokens, foreground only. Supports structured output (`@Generable`) and **function calling (iOS only)**.
- Not supported in hybrid mode: Live API, server prompt templates, count tokens.
- **Apple Intelligence languages (Apple newsroom, June 2026, iOS 27):** English, Danish, Dutch, French, German, Italian, Norwegian, Portuguese, Spanish, Swedish, Turkish, Vietnamese, Simplified and Traditional Chinese, **Japanese**, Korean. **Indonesian is NOT supported**, and Apple has given no date.

### 2.4b ⚠️ Language summary (critical for an Indonesia version)
| Language | Web (Chrome Nano) | Android (ML Kit Prompt API) | iOS (Apple FM) |
|---|---|---|---|
| English | ✅ | ✅ validated | ✅ |
| Japanese | ✅ | ❓ not listed | ✅ |
| **Indonesian** | ❌ | ❌ not listed | ❌ |
⇒ An Indonesian-language app can't officially run its language tasks on the device. Language-neutral extraction (names, ages, numbers, checkboxes, medicine names) may still work. **Test it on Day 1.**

### 2.5 Cloud-only capabilities (every platform)
Image generation, cloud TTS (natural voice), Live API, video input, long documents, **Google Search / Maps grounding**, chat and function calling (web/Android), server prompt templates.

### 2.6 Things that are NOT cloud-only (don't make that mistake again)
- **TTS is on the device on every platform:** Web `speechSynthesis`, Android `TextToSpeech`, iOS `AVSpeechSynthesizer`. Cloud TTS only sounds more natural.
- **Speech-to-text on the device:** Chrome on-device Web Speech (check that `processLocally` is used), Android `SpeechRecognizer` (offline mode), iOS `SFSpeechRecognizer` with `requiresOnDeviceRecognition`.
- The Firebase hybrid SDK does **not** take audio on the device. So: OS speech-to-text gives text, then Nano processes the text. PillCheck did exactly this.

### 2.7 Known gotchas (from other entrants' write-ups)
- **`ONLY_ON_DEVICE` can silently go to the cloud on web** when Chrome's `LanguageModel` API is missing (firebase-js-sdk#10635, found by PillCheck and Chaupal). **Add our own guard**, a feature-detect check before calling, so private data never leaves the device.
- The SDK creates a **new on-device session per call**. Keep prompts short and ask for small outputs.
- The device can be **slower than the cloud** if you ask for too much. Roundabout went from ~4 s to <1 s by asking only for `{"words":[...]}`.
- Small models wrap JSON in prose, miscount and lose context. Always use a schema (`responseConstraint` on device, `responseSchema` in the cloud); the same schema gives the same output shape on both sides.
- Add your own timeouts: a slow device falls back to the cloud, and a slow cloud falls back to the device.

### 2.8 Language stack: the LLM is only one layer (researched 2026-11-21)
Language support comes from **several separate on-device components**, not only the LLM:

| Layer | Web (Chrome desktop) | Android | iOS | Indonesian offline? |
|---|---|---|---|---|
| **Speech-to-text (dictation)** | Web Speech API with `processLocally = true`; language packs of ~60 MB ("SODA"); `SpeechRecognition.available()` / `install()` check and download them. Disabled on ChromeOS (Aug 2025 report) | `SpeechRecognizer.createOnDeviceSpeechRecognizer()`; `checkRecognitionSupport()` lists installed and downloadable languages; `triggerModelDownload()` | `SFSpeechRecognizer` + `requiresOnDeviceRecognition`; check `supportsOnDeviceRecognition` per locale after the dictation model downloads | ⚠️ Probably on Android; **check on the device** (Chrome and iOS lists not found) |
| **Translation** | **Chrome Translator API** (on-device, desktop only), **Indonesian `id` listed** in Chrome's docs; Language Detector API | **ML Kit Translation** (on-device, 50–59 languages, models downloaded on demand; non-English pairs go through English) | **Apple Translation framework** (iOS 17.4+, on-device after download); **Indonesian supported** (Translate app since 2022) | ✅ Web and iOS confirmed; Android ⚠️ check the full ML Kit list |
| **Text-to-speech** | `speechSynthesis` (OS voices) | `TextToSpeech` | `AVSpeechSynthesizer` | ✅ Usually (OS voices; check the device) |
| **LLM (Gemini Nano / Apple FM)** | en, ja, es, de, fr | English, Korean validated | 16 languages, Japanese yes | ❌ |

**⇒ Pipeline idea (post-hackathon only, see D11):** Indonesian speech or text → (on-device speech-to-text) → **translate to English on the device** → Nano works in English (structuring, cards, extraction) → translate the output back to Indonesian on the device → show it, or speak it with OS TTS.
- **Never translate names, places or medicine names.** Extract them as-is first (keep them in "as written" fields) and only translate the surrounding text. Translation mangles proper nouns.
- Measure accuracy of this chain vs the cloud (Gemini supports Indonesian) and route by the measurement (evaluation §7).

**`LanguageService` design.** ⚠️ Superseded for the hackathon by D11 (below): **drop the separate translation engines** (ML Kit / Chrome Translator / Apple Translation); translation runs through Firebase AI Logic (Nano offline, Gemini cloud) + OS speech/TTS. The full table is kept as a post-hackathon option:
| Need | Online (best quality, clearly allowed) | Offline: Android (field phones, main) | Offline: Web (hub laptop) | Offline: iOS |
|---|---|---|---|---|
| Translate | **Cloud Gemini via Firebase AI Logic** (`ONLY_IN_CLOUD`), the widest language coverage | **ML Kit Translation** (most offline languages, ~59; also has an iOS SDK, so one engine for both phones) | **Chrome Translator API** | Apple Translation (better quality, fewer languages), or ML Kit iOS |
| Speech-to-text | Same OS engines | `createOnDeviceSpeechRecognizer` | Web Speech `processLocally` | `SFSpeechRecognizer` on-device |
| Text-to-speech | — | `TextToSpeech` | `speechSynthesis` | `AVSpeechSynthesizer` |
- Interface: `translate(text, from, to)`, `transcribe(audio, lang)`, `speak(text, lang)`, `status(lang)` (installed / downloadable / unavailable). The router picks cloud when online, then the on-device engine, then "show the original + icons".
- **Language downloads are on demand; English is the only default.**
  - English is the base language (the app UI must support English anyway). Nothing else is downloaded automatically.
  - Each other language (translation model, speech pack, TTS voice) is downloaded **only when the user picks it** ("Add language: Indonesian ⬇ ~X MB"), with its size shown and Wi-Fi recommended. ML Kit and Chrome download per language; Web Speech packs are ~60 MB each.
  - ⚠️ The catch: **with no signal you can't download a language in the middle of a disaster.** So Shelter-ready mode asks once, in normal times: "Which languages does your area need?" (e.g. Indonesian for Aceh, Japanese for Noto) and downloads only those.
  - If a language is missing offline: Kit says "Indonesian isn't downloaded yet. It will download when signal returns." Meanwhile the app shows the original text + icons, and the download is queued in §6.10 (low priority, Wi-Fi/hotspot only).
  - Settings show each language's status (installed / downloadable / downloading) with a delete button to free space.
- MVP languages: **Indonesian, English, Japanese** only. Add more later via Remote Config.
- ⚠️ Local languages: many elderly people in Aceh may speak **Acehnese** (or Javanese elsewhere) rather than Indonesian. Most offline engines won't support them (unverified). Staff speak Indonesian; keep "show the original" + icons as the universal fallback.

**✅ DECIDED (D11): no separate translation AI in the hackathon build.** Background, kept for later: the FAQ says "Your app's AI inference must still run through Firebase AI Logic, so don't call other model APIs directly."
- OS speech-to-text and TTS are input/output services (like a keyboard); other entrants use them (PillCheck: Chrome on-device speech). Probably OK.
- Translation APIs (Chrome Translator, ML Kit Translate, Apple Translation) are AI models outside Firebase AI Logic. Koe Relay uses the Chrome Translator API, but **that's not proof it's allowed.**
- (Not needed any more. Optional question, only if we want translation APIs after the hackathon: *"Can we use the OS/browser on-device translation and speech-to-text APIs (Chrome Translator API, ML Kit Translation, Apple Translation, Web Speech) alongside Firebase AI Logic, which does all the generative inference?"*)
- **What we do instead (D11):**
  - **All AI translation goes through Firebase AI Logic**, on both sides of the hybrid:
    - **Offline:** translation runs on **Gemini Nano through Firebase AI Logic** (`ONLY_ON_DEVICE` / `PREFER_ON_DEVICE`). Fully allowed, because it's the same SDK. It works well for Nano's supported languages (web: en, ja, es, de, fr; Android: en validated).
    - **Indonesian offline via Nano:** officially unsupported, but **we try it and measure it** (set E). If it passes the accuracy bar, the device translation is shown as a **draft** ("offline draft translation") and re-checked by cloud Gemini when signal returns. If it fails, use the keyword icons below.
    - ⚠️ Day-1 check: Chrome's Prompt API may refuse a language that isn't supported if the expected input language is declared. Check whether the Firebase SDK's on-device options pass that, and test Indonesian input both ways.
    - **Online:** cloud Gemini via Firebase AI Logic, the most accurate for Indonesian.
  - **Offline Indonesian announcements:** show the staff's own text big and clear, plus **icons and times from a deterministic keyword list** (e.g. `makan/makanan` → 🍙, `air` → 💧, `obat` → 💊, `dokter` → 🩺, `jam 15.00` → 15:00, place words → 📍). No AI needed, so it's exact. Cloud Gemini improves and translates the card when signal returns.
  - **Offline roster and medicine photos in Indonesian:** Nano does language-neutral extraction (names as written, ages, numbers, checkbox columns, medicine names), measured in set E; anything below the accuracy bar becomes a draft for review + cloud re-check.
  - **UI language** (English + Indonesian) is ordinary translated text files (i18n), not AI.
  - **OS speech-to-text and TTS** are kept as input/output tools (like a keyboard), like other entrants (PillCheck). Speech input is already first on the cut line, so the build never depends on it.

---

## 3. Competition landscape (as of 2026-11-21)

Data: **366 projects found** (220 confirmed submitted on Devpost + 146 GitHub repos whose README says #BuildHybridAI). Full ranked list: `examples/RANKED.md`.

### 3.1 Saturated categories (avoid or expect a low Idea score)
- **PII redaction before the cloud** (mask on the device, ask the cloud, restore locally): 20+ entries (NanoShield, NullTrace, Veil, Valkyrie, Blackbar…).
- **Scam checkers:** Scam Shield, Second Look, Shortstop, PasteGuard, loanshield, tipucek, did-i-order-this…
- **Document / letter / lease / bill explainers:** PlainPost, PlainTerms, Portia, Callsheet, Paperlight, BillShield…
- **Study / notes / summarizers:** StudySpark, StudyNest, many more.
- **Creator tools:** Creator Studio, ClipCraft, Smardy.

### 3.2 Top entries (hand-reviewed) and what they teach
| Rank | Project | Why it's strong |
|---|---|---|
| 1 | [Chaupal](https://devpost.com/software/chaupal) | One laptop marks a whole class with no internet (about a third of India's schools are offline; UDISE+). Phones join by QR plus an audio chirp. Notebook photos are read `ONLY_ON_DEVICE`. Web + Android |
| 3 | [Roundabout](https://devpost.com/software/roundabout-84zu1f) | Word finding for aphasia. **Measured** device vs cloud on 255 words (device 100% on places, 52–57% on actions and feelings) and **routes by that measurement**. All 4 modes, Remote Config, Playwright tests, Web + Android. Weakness: offline need is weak (users are often at home) |
| 4 | [PillCheck](https://devpost.com/software/pillcheck-wexj9t) | Voice drug-interaction check for elderly users. 30/30 safety evaluation. Found the ONLY_ON_DEVICE SDK bug |
| — | [Seen Aloud](https://devpost.com/software/seen-aloud) | Audio description for blind viewers, 71 tests |
| — | [CommunityFitness ATLAS](https://devpost.com/software/communityfitness-atlas-hybrid-ai) | The only one claiming Web + Android + iOS (Flutter + native bridges). The fitness idea is common |

**Lessons:** a specific person with a specific problem; a measured, per-feature routing rule; tests and evaluation numbers; device/cloud badges in the UI; Remote Config for prompts and routing.

### 3.3 Disaster / emergency entries (our direct competitors): ALL web-only
| Project | What | Gap vs us |
|---|---|---|
| [FloodLine](https://devpost.com/software/floodline-flood-rescue-requests-read-even-offline) | Chennai flood SOS-post board for volunteers | Web only, rescue posts, not shelters |
| [Koe Relay](https://devpost.com/software/koe-relay) | Rewrites dense Japanese notices on the device and speaks them | **Closest to our idea 1.** Web only; general notices; no staff roster, no deaf-first visual board, no care card |
| [CareSync AI](https://devpost.com/software/caresync-ai-offline-first-clinical-triage-ecosystem) | Clinical triage plus dispatch | Java server; hospital-focused |
| [HELIOX](https://devpost.com/software/heliox-hybrid-ai-space-weather-observatory) | Space weather | Different domain |
| [lifeline-hybrid-ai](https://github.com/erkambbb/lifeline-hybrid-ai) (GitHub) | Offline disaster assistant | Generic |
| [aidready](https://github.com/Wahid1sher/aidready) (GitHub) | Damage documentation for claims | Different job |
| [babala](https://github.com/pimcabanlit/babala) (GitHub) | Typhoon alerts (Philippines) | Warnings, which governments already cover |
| [resqpulse-ai](https://github.com/IrsadS/resqpulse-ai) (GitHub) | Disaster reporting and dispatch | Generic "platform" |
| [fieldcare](https://github.com/tydavid5/fieldcare) (GitHub) | Rural health worker triage | Different user |
**How we differ:** native on phones (Android, then iOS), not only web; a **shelter** focus (after the event, where people actually die); a staff + evacuee two-sided design; **no phone needed for the vulnerable person**; real evidence (Noto 2024, Kobe, Tohoku).

### 3.4 Open niches overall
iOS (only ~3 real entries), all three platforms (1), games (almost none), wildfire (0), deaf-first emergency info (~0).

---

## 4. Past winners (closest earlier contest: Google Chrome Built-in AI Challenge)

### 2025 (~1,300 submissions; judges were Chrome PM/DevRel)
- **Best Hybrid AI**
  - Web: **Trail**, an offline hiking companion PWA. The builder is a hiker who lost signal on trails. Online it uses Maps/Places data; offline, Gemini Nano answers.
  - Extension: **Mentelo**, a talk-to-it helper for dense pages and tax forms, by a non-native English speaker. **Built solo in 1 week with AI help**, as their first extension.
- Most Helpful: **Nutshell** (hands-free browsing for motor disabilities), **AAC Board AI** (communication for non-speaking people).
- Multimodal: Phonaify (pronunciation), Pet Shelter App.
- Honorable mentions: BrowseBack, EduAdapt (ADHD/dyslexia), Helix AI, Marionette (offline agent), **The Turing Werewolf** (a game).
- The judges' hybrid definition: "There is a **clear reason why the submission benefits from a hybrid AI approach**."

### 2024
- Best Hybrid: Orma, BrowseGraph. Real-world: WP on-device assistant, Mochi (dyslexia/ADHD). Most innovative: The Crooked Tankard (a text adventure game), Opale.

### Winner pattern
A concrete persona with a personal story; one clear reason for hybrid; **accessibility and underserved audiences win repeatedly**; polish beats architecture. **The offline need must come from the user's situation** (a trail with no signal, a school with no internet). "At home on Wi-Fi" makes offline sound invented.

---

## 5. Real-world evidence (put these in the write-up and video)

### 5.1 Communication after disasters
- **Networks jam for 24–72 h after a disaster.** After Kobe 1995 it lasted about 5 days. **SMS gets through more often than calls.**
- Many failures are **power** failures at towers, not lost connectivity. Sichuan 2008: 2,300+ base stations affected.
- **Maui 2023:** the county's lawsuit says **all 21 cell towers in West Maui failed**; carriers dispute this. Sirens were not activated.
- **Australia Black Summer 2019–20:** **1,406 telecom facilities** were hit, mostly by power loss. Rural firefighters refuelled tower generators.
- **Tonga 2022:** a cable cut left the country **without internet for 5 weeks**; satellite phones, HF radio and boats were used.
- **Ukraine 2025–26:** blackouts up to 12 h/day; tower batteries last 6–8 h.
- 313 government internet shutdowns in 52 countries in 2025 (Access Now).

### 5.2 Warnings already work; the gaps come after the event
- Japan: JMA Earthquake Early Warning via **cell broadcast** (reaches every phone even when the network is congested), **J-Alert** via satellite to loudspeakers, TV and radio, **Disaster Message Dial 171**, carrier message boards.
- Indonesia: BMKG warnings within about 2–3 minutes through InfoBMKG, digital TV and HT radio backup.
- Android Earthquake Alerts are in 200+ regions (not Japan) and **need data**. iPhone Emergency SOS via satellite works in Japan (iOS 17.6+); not confirmed for Indonesia.
- Mesh apps (Bridgefy, Briar) only work if installed **before** the disaster, and security audits are mixed.
- **Google Person Finder shut down in 2019.** Family-finding is fragmented.
- ⇒ **Do NOT build:** warning apps, mesh messengers, or SOS buttons.

### 5.3 Shelters (the core of our product)
- **Noto Peninsula earthquake, Jan 1 2024:**
  - Distribution was first run with analog methods: **officials photographed handwritten lists and shared them by email.**
  - A shelter board in Wajima said "food and water distribution has yet to be determined".
  - Staff at a welfare shelter confirmed home-nursing patients' safety **by phone, one by one**.
  - Electricity and water were cut.
  - **Disaster-related (indirect) deaths exceeded direct deaths:** 228 direct vs 397 indirect as of July 2025 (sources differ by date; Ishikawa reported 489 total including 261 disaster-related in Dec 2024).
  - One shelter (Ishikawa Sports Center): **80% elderly, 60% needing nursing care.**
  - About **15% of elderly evacuees** moved from care homes and hospitals to other prefectures had died by Dec 2024.
  - Squat toilets were still in use 6 months later.
- **Tohoku 2011:** **56.1% of the 13,007 deaths** in Iwate, Miyagi and Fukushima were **aged 65+** (NPA). **89.5% of 1,632 indirect deaths** were people aged 66+ (government, March 2012).
- Japan's 2021 law change made **individual evacuation plans** for people who need help a municipal duty, but **fewer than 10% of municipalities had finished them** (2024). Registers exist in 99% of municipalities.
- Similar shelter problems (cold floors, crowded gyms) were seen in 1995, 2011 and 2024. **The problem repeats.**

### 5.4 Deaf and hard-of-hearing people
- **Kobe 1995: deaf death rate was over 4x that of hearing people** (Kobe Deaf Association figure, not peer-reviewed).
- **Kobe and Tohoku:** deaf death rates were "significantly higher" than the general population (Takayama, 2017; cited secondhand ⚠️).
- Mechanism: people did not hear sirens or evacuation announcements (e.g. a 72-year-old woman in Miyagi, 2011).
- Disability overall in Tohoku: **more than twice** the death rate of people without disabilities.
- Australia (NSW Deaf communities): the biggest barrier is limited disaster information in sign language, **plain English or pictorial form**. Standard messages are audio-based or too complicated. **Shelter staff find it hard to communicate** with deaf people.
- 2024 paper: **no research publications specifically on how well early warnings work for deaf and hard-of-hearing people.**
- Shelter guidance (Washington State) recommends signage, message boards and a trained point of contact for deaf people.

### 5.5 Elderly people and phones
- Japan 2024: smartphones are used as the internet device by **78.8% of people in their 60s and only 53.0% in their 70s** (MIC White Paper 2025). The 80s are lower (no figure found).
- ⇒ **Never assume the evacuee has a phone.** Use the shared screen, printouts and staff devices.

### 5.6 Misinformation (secondary feature idea)
- Palu 2018: WhatsApp hoaxes (the mayor's "death", "free Hercules flights"). Police arrested 8 people. Corrections went out on Twitter, which rural survivors rarely read.
- Maluku 2019: tsunami hoaxes kept survivors in shelters.
- BMKG Feb 2026: fake InaEEWS Telegram accounts.

### 5.7a Indonesia: current and recent disasters (researched 2026-11-21)
**Sumatra floods and landslides, late Nov 2025 (Aceh, North Sumatra, West Sumatra). The strongest local evidence for this product.**
- Deaths: 174 (Nov 29) → 921 (Dec 7) → 1,053–1,059 (Dec 16) → **1,204 by Feb 3 2026** (BNPB). By Jan 14 2026: Aceh 550, North Sumatra 375, West Sumatra 231.
- **Displaced at the peak: 975,079 people (Dec 7 2025)**, most of them in Aceh. Still 242,174 on Jan 6 2026. BNPB said displaced people were mostly women.
- **Power and signal:** 5 PLN 150 kV transmission towers collapsed in Bireuen (Nov 26 2025), cutting power in **18 regencies/cities**. In Aceh Utara, evacuees had **4 days with no signal on any operator**, and in Langkahan **21 nights without electricity** (oil lamps and candles). In Aceh Timur, evacuees **queued at the regent's office to get internet**. The Aceh Utara regent phoned the deputy communications minister to ask for signal to be restored.
- **Evacuee data broke down (exactly what F1 + F3 fix):**
  - **BPS found many duplicates: 179,000 records sent by regional heads became 60,426 households after cleaning** (CNN Indonesia, Jan 28 2026).
  - **BNPB and local data didn't match:** BNPB counted 6,187 evacuees in tents across Aceh, but for Aceh Timur BNPB had **113 people while the regent said 2,326** (NU Online). NU warned that inaccurate data slows aid.
  - The Home Affairs Minister told local governments to form teams to clean up the data, to prevent duplicates and aid going to the wrong people.
  - Older pattern: Pandeglang 2019, aid was uneven because of "fake evacuees" and volunteer posts that hadn't been registered.
- **The official procedure is manual:** BNPB Juklak No. 9/2023 requires evacuee data **broken down by sex, age group and protection needs (vulnerable groups)**, processed **manually**, with data collectors able to work 24 hours. Kemenko PMK says data standardisation, integration and **data on vulnerable groups** are still problems.
- ⇒ The Indonesian evidence for the roster and dedupe feature is **even stronger than Japan's**. The government itself documents duplicate records, mismatched counts and manual processing.

**Aceh, November 2026 (happening now):**
- Floods and landslides from mid-November. Aceh Tamiang: 18,158 affected and **5,088 displaced** (BNPB, Nov 15). By Nov 18: **25,244 affected, 844 still displaced** across 4 regencies. 1 death in Aceh Tengah, plus a landslide death in Aceh Tenggara on Nov 10.
- Aceh is on **flood/landslide emergency alert status from Nov 11 2026 to Jan 31 2027**. The coverage notes recovery from the 2025 disaster "isn't finished" and floods are recurring.
- Counts differ between sources and dates.

**National, 2026:** BNPB recorded **2,390 disasters from Jan 1 to Nov 18 2026; 99.24% were hydrometeorological** (floods, landslides, extreme weather). On Nov 20 BNPB warned of floods and landslides, with Central Java a priority area.

**Forest and land fires (karhutla) 2026:**
- Kalimantan is the centre: **35,085 hotspots** on Kalimantan and Sumatra on Oct 8–15 (34,226 in Kalimantan), with 7,183 hotspots on Oct 14 alone (VIIRS via Katadata). Burned area (Jan–Sep 14): West Kalimantan 48,535 ha, South 19,304 ha, Central 15,976 ha, East 5,761 ha (Kompas). Riau 18,882 ha by Sep 4. Smoke reached Malaysia and Singapore.
- Health: **333,470 respiratory infection (ISPA) cases by Oct 21 2026** in 68 regencies/cities across 7 provinces, and **more than 3 million people directly exposed to smoke** (Kemenkes). Pontianak moved schools online; South Kalimantan delayed school start times. More smoke is expected for at least 3 more months.
- Fit with our product: weaker than for floods (fires don't usually create shelters with no signal). Possible later extension: a care card plus smoke-health guidance for elderly people and children.

**Existing Indonesian tools (don't duplicate them):**
- **PetaBencana.id** (BNPB-supported, from MIT Urban Risk Lab; UN Public Service Award 2019): AI chatbots reply to social posts containing "banjir" and collect flood reports for a live map via Twitter/X, Facebook and Telegram. It's "data-light" but **needs a connection**.
- **InfoBMKG:** warnings. **InaRISK (BNPB):** risk information (not researched in detail).
- **The gap none of them cover:** shelter registers and vulnerable-person data **at the shelter, offline**.

### 5.7b How to handle Indonesia in the product (DECIDED: Aceh story + measured routing, i.e. option C; see D2 and D11 in §12)
| Option | Setting | On the device | Risk |
|---|---|---|---|
| **A (safe)** | Demo in Japan (Noto story), Indonesia evidence shown as "the same failure happens everywhere", Indonesia on the roadmap | Everything (English/Japanese supported) | Less personal for an Indonesian team |
| **B (Indonesia-first)** | Demo in Aceh (Sumatra 2025 story) | Only language-neutral tasks (photo → names, ages, flags, medicine names); Indonesian announcement text goes to the cloud, or is shown as raw transcript + keyword icons offline | The offline story is weaker for announcements; officially unsupported language |
| **C (bilingual, recommended if tests pass)** | Aceh story + English UI (required anyway) | Test Day 1: if Nano reads Indonesian roster sheets acceptably, do F1 on the device in Indonesian and **state honestly** that it's "officially unsupported, measured X%"; announcements fall back to the cloud when Indonesian accuracy is low (a measured routing rule) | Needs evaluation data; makes a strong Tech story ("routing by measured language accuracy") |

### 5.7 Other world problems we researched (backup ideas)
Wildfire "prepare online, survive offline" plans (0 rivals); documenting during internet shutdowns; community health worker paper registers (PATH 2025); a private journal for domestic violence survivors; small-scale fishers (32k–100k+ deaths a year, FAO/Pew); livestock health where data is costly (Sub-Saharan Africa: 1 GB = 14% of income, 33% for the poorest 40%); a blackout store-and-forward assistant. Details: `RESEARCH_ideas_world.md`.

---

## 6. Product spec

### 6.0 Final tech stack (summary; details in §2, §6.4–6.15)
| Layer | We use | Why |
|---|---|---|
| **Web hub** (shelter laptop + big screen) | TypeScript + Vite PWA, Firebase JS SDK (`firebase/ai`), service worker | Chrome desktop is the only web runtime with on-device Nano; a PWA opens offline; one laptop = hub with a big battery |
| **Android** (volunteer, Scout, field phones) | Kotlin + Jetpack Compose, `firebase-ai` + `firebase-ai-ondevice` | The model is part of the phone system (AICore); GPS, camera, Bluetooth, offline speech and ML Kit; phones are what people carry in a disaster |
| **iOS** (not planned for the hackathon, D3; possible later with a Mac + Apple Intelligence iPhone) | SwiftUI, FirebaseAILogic (SPM), `HybridModel` | Grand Prize multi-platform bonus; least competitive "Best of" |
| **NOT used** | Flutter, React Native, Capacitor, Electron, Tauri, Dioxus | Wrappers lose on-device inference or aren't confirmed to support it |
| **On-device AI** | Gemini Nano 4 (Chrome / Android), Apple Foundation Models (iOS), all via Firebase AI Logic hybrid | Required by the hackathon; private, free, offline |
| **Cloud AI** | Gemini via Firebase AI Logic: `gemini-3.5-flash-lite` (default fallback), `gemini-3.8-flash` for heavier jobs; Search/Maps grounding | Free tier (no billing); merging shelters, interactions, translation, clinic search |
| **Routing** | Our own router per feature (mode table §6.2), privacy guard, timeouts | The SDK only switches on availability; per-feature routing is what scores on Tech |
| **Firebase services** | App Check (required), Remote Config (prompts, schemas, thresholds, flags; **with in-code defaults for offline**), Firestore (offline persistence, roster, requests), Auth (staff, medic role claim), Hosting | Required or encouraged by the rules; all free tier |
| **Language** | UI in English + Indonesian (i18n files); **AI translation only via Firebase AI Logic: Nano offline (Indonesian measured, draft), cloud Gemini online**; offline = original text + deterministic keyword icons; OS speech-to-text and TTS as input/output; English default, other UI/voice languages on demand | No rules risk (D11) + most accurate Indonesian (§2.8) |
| **Drug matching** | Deterministic fuzzy matcher + small offline drug list (demo ~100 drugs) | Accuracy and safety matter more than AI (§6.9) |
| **Sync and power** | Local job queue with priorities, batch mode, real connectivity check, Signal Scout (alert + QR courier; hotspot; BLE/Nearby stretch) | 24–72 h+ with no signal, weeks without power (§6.10) |
| **Maps and hazards** | Phone GPS (no internet needed) + MapLibre + OpenStreetMap PMTiles + **global HAND 30 m** + Copernicus DEM per region, all in region packs (§6.13); no routing engine | Offline, free, open license; "safe route" is deliberately not promised (§6.11) |
| **Mascot / art** | Hand-made SVG (Kit) | Original work, loads offline, no billing |
| **Testing** | Vitest (web), JUnit (Android), Playwright (web end-to-end); evaluation scripts for test sets A–D | Tech evidence; proves the privacy guard and routing |
| **Repo** | Public GitHub, Apache-2.0 | Required (license visible in About) |

### 6.1 Personas
Demo story is in Aceh (D2); the same personas exist in any country.
1. **Dewi, 34, BPBD staffer / volunteer** running a school-hall shelter. 120 evacuees, a paper BNPB-style list, no signal, one laptop and an Android phone.
2. **Pak Hasan, 82**, hard of hearing, no smartphone, can't walk far, takes insulin + blood-pressure medicine and can't remember the names. Misses the 3 pm food announcement.
3. **Ibu Rina, 45, deaf** (Indonesian Sign Language + text). Has a phone. Spoken announcements exclude her.
4. **Pak Hasan's daughter, in Medan:** "Is my father in any shelter? Is he OK?"
5. **Volunteer Andi (Scout + Field Check):** checks elderly people who stayed at home.

### 6.2 Features, priorities, and the device/cloud split
| # | Feature | Priority | On the device (why) | Cloud (why) | Mode |
|---|---|---|---|---|---|
| F1 | **Roster from a photo of a handwritten list** → people + flags (hard of hearing, needs medicine, wheelchair, lives alone, foreign language, infant) | MUST | Photo read by Nano (privacy; no signal) | Re-check low-confidence rows when online | Read: `ONLY_ON_DEVICE` (privacy guard); retry: `PREFER_IN_CLOUD` on staff request |
| F2 | **Speak once, everyone sees it:** staff speak → OS speech-to-text → Nano builds big-text picture cards ("🍙 Food 15:00, gym entrance") → shared big screen + phone view + print | MUST | Speech-to-text + card structuring (no signal; speed) | Translation into English and other languages; check against official bulletins | `PREFER_ON_DEVICE` |
| F3 | **Cross-shelter family search + roster merge** | MUST | — | Merge rosters in Firestore, remove duplicates (name, age, reading variants), search by name | `ONLY_IN_CLOUD` + Firestore |
| F4 | **Personal care card** from a medicine bag or prescription notebook (お薬手帳) photo: medicines, conditions, allergies, mobility, hearing, contact; printable or shown to a medic | **MUST** (it feeds the Medicine Round) | Photo read (medical privacy) | Drug-name lookup and interaction flags, only with consent | Read: `ONLY_ON_DEVICE`; check: `ONLY_IN_CLOUD` (with consent) |
| F8 | **Reach list per announcement:** "these 12 hard-of-hearing / 5 living-alone people must be told in person", with a ✓ when each one has been told | **MUST** | Build the list from roster flags | — | Local (rules) |
| F9 | **Medicine Round** (full spec in §6.9): shelter-wide medicine schedule, ✓ when given, can't-miss treatment alerts, stock and shortages, supply requests, *safe* alternatives for a pharmacist to confirm | **MUST** (core) / alternatives SHOULD | Schedule, checklist, stock counts, matching names against the offline drug list | Supply requests to the health office across shelters, generic-name lookup, interaction check, finding an open dialysis clinic | See §6.9 |
| F10 | **Outbreak counts:** daily symptom tallies per shelter (diarrhoea, fever, cough, rash) → health office alert when they rise across shelters | SHOULD | Count on the device (no names) | Combine across shelters, detect clusters | Device count → `ONLY_IN_CLOUD` aggregate |
| F11 | **Batch mode + airplane mode + Signal Scout** (full spec in §6.10): devices save battery in airplane mode; one "scout" phone watches for signal and alerts the others when it's back | **MUST** (batch + airplane tip + scout alert) / BLE beacon SHOULD | Queue, batch processing, connectivity checks | Sync the queue when signal returns | Local |
| F12 | **Field Check + offline map** (full spec in §6.11): find and check vulnerable people who stayed at home; photo + GPS + voice note → a structured report; offline map of pins and hazards | **MUST-lite: hazard-aware priority list (§6.11c)** / SHOULD: report + GPS / NICE: map view | GPS (needs no internet), photo/voice report read on the device, offline map tiles | Share the priority map with rescue teams (BPBD/Basarnas), flood reports, Maps grounding | Read: `ONLY_ON_DEVICE`; share: `ONLY_IN_CLOUD` |
| F5 | **Write-to-talk:** a deaf person types and staff speak; text simplified for elderly people | NICE | Simplification, speech-to-text, TTS | Translation | `PREFER_ON_DEVICE` |
| F6 | **"I'm safe" message sized for SMS** (≤70 Japanese characters / 160 GSM) | NICE | Compress status into SMS size | — | `PREFER_ON_DEVICE` |
| F7 | **Shelter needs summary** (counts of meds, diapers, wheelchairs, hearing support) for the city office | NICE | Count from the roster | Aggregate across shelters | Cloud |

Always:
- A **device/cloud badge** on every AI output (green = this device, blue = cloud).
- The **original transcript or photo shown beside every AI summary.** Safety: never hide the source.
- A **"Private mode" switch:** nothing leaves the device.
- An **offline queue:** cloud jobs wait and run automatically when the signal returns, with a visible queue count.

### 6.3 Routing rules (our own code on top of the SDK)
1. Anything with an evacuee's name, health or photo is processed on the device, with an **explicit guard** (feature-detect `LanguageModel` / on-device availability). If on-device isn't available, **ask the staff member** before using the cloud. Never send silently (avoid the SDK#10635 behaviour).
2. Tasks where the device is measured to be weak go to the cloud first, decided by the evaluation in §7.
3. Timeouts: if the device doesn't start answering within T_device, offer the cloud; if the cloud doesn't answer within T_cloud, use the device. Both values live in Remote Config.
4. **Every rule and threshold is in Remote Config** so it can change without a release.

### 6.4 Platforms
- **Web (shelter laptop / big screen):** a TypeScript + Vite PWA with the Firebase JS SDK (`firebase/ai`) and a service worker so it opens offline. Chrome desktop for on-device. Hub + big screen (D3).
- **Android (volunteer, Scout and field phones): the main platform and the submission category (D3).** Kotlin + Jetpack Compose with `firebase-ai` + `firebase-ai-ondevice`.
- **iOS (not planned, D3; notes kept for later):** SwiftUI with FirebaseAILogic and `HybridModel`. Text on the device; **photos are read in the cloud with consent** (on-device iOS is text only). Typed entry works on the device.
- **One shared prompt pack** (prompts + JSON schemas) in Remote Config, used by all platforms. Same schema on the device (`responseConstraint`) and in the cloud (`responseSchema`).
- Don't use Flutter, Capacitor, Electron or Tauri. They lose on-device inference or aren't confirmed to support it.

### 6.5 Firebase services
- **Firebase AI Logic** (Gemini Developer API backend, free models).
- **App Check:** reCAPTCHA Enterprise on web, Play Integrity on Android, App Attest on iOS; debug tokens for development and the submission.
- **Remote Config:** prompt pack, schemas, model names (primary and backup), routing thresholds, timeouts, feature flags.
- **Firestore:** shared roster (synthetic data only), announcements, sync queue. Offline persistence on. Security rules: staff write; public family search (D6) returns minimal fields only (name, age band, shelter, status).
- **Authentication:** silent **Anonymous Auth** (no login screen, D14); roles come from the join QR and a membership record checked by security rules. Family search is public (D6), rate-limited, minimal fields.
- **Hosting:** the web app plus a QR link for the phone view.

### 6.6 Data model (draft)
```
shelters/{shelterId}: name, location(approx), updatedAt
shelters/{shelterId}/people/{personId}:
  nameKanji, nameKana, nameLatin, age, gender?, flags[hard_of_hearing, deaf, needs_meds,
  wheelchair, lives_alone, foreign_language, infant, pregnant], familyContact?, status(safe|injured|moved),
  source(photoId, rowIndex, confidence), inferredBy(device|cloud), updatedAt
shelters/{shelterId}/announcements/{id}: transcript, cards[{icon, title, time, place, detail}], lang, inferredBy, createdAt
careCards (local only by default; synced only with consent): meds[{name, dose, timing}], conditions, allergies, mobility, hearing
syncQueue (local): pending cloud jobs
```

### 6.7 Privacy and safety
- Demo and repo: **synthetic data only**; a handwritten test list we write ourselves.
- Care cards are **local by default.**
- AI output is a **draft for staff to confirm**, never a medical decision. A drug check says "talk to the doctor/pharmacist", like PillCheck.
- Announcement cards always show the original transcript; staff approve before showing on the big screen.

### 6.8 Mascot: Kit, the calm firefighter cat
**Who:** a cat in a firefighter / rescue uniform (helmet, orange-yellow reflective vest, a small lantern). Personality: **calm, slow, warm and steady**, like a firefighter who says "I'm here, we'll do this one step at a time." Never hyper, never panicked, never jokey.

**Name: Kit** (one global name, no regional variants; chosen 2026-11-21).
- Double meaning: a *kit* is a young cat (as in kitten), and an **emergency kit** is what you grab in a disaster. Nobody Missing is a "shelter kit", and Kit is its guide.
- One syllable, easy for elderly people, children and foreigners. Japanese: キット. Indonesian: just "Kit".
- Video line: "Meet Kit, your shelter's calm helper."
- Cultural hook (Indonesia): the fire department (*Damkar*) is famous online for rescuing cats. Here **the cat is the firefighter**. Light and friendly, use sparingly.

**What Kit does (helpful, not decoration):**
1. **Guides the announcement cards:** Kit holds each card ("🍙 Food 15:00, gym entrance"). One consistent character helps elderly evacuees and children recognise "this is official shelter information" on the big screen and printouts.
2. **Shows calm status states** instead of scary error messages:
   - No signal: "Kit is holding the lantern: working on this device."
   - Syncing: "Kit is carrying your list to the other shelters."
   - Waiting for the cloud: "Kit will send this when the signal comes back (3 waiting)."
   - This doubles as the **device/cloud badge** (lantern = device, carrying = cloud).
3. **Calming prompts for staff** ("Take a breath. Let's add the next person."), plus step-by-step onboarding for first-time volunteers.
4. **For children and elderly evacuees:** a familiar friendly face on the care card and the "I'm safe" message.

**Tone and safety rules (important):**
- Kit **never replaces text.** Every card still has large plain text plus the original transcript. Accessibility comes first.
- **No mascot on serious news:** family-search "not found", injury or death, or a tsunami/evacuation order. Those screens are neutral, clear and text-first. A smiling cat on bad news is hurtful.
- No jokes, no meme speech in the app. Short, kind, plain sentences (good for elderly people and translation).
- High contrast, works in black-and-white printouts, readable from across a gym.

**Making the art (rules):**
- Submissions must be our **original work**. Draw Kit ourselves as **simple SVG** (flat shapes, 3–5 poses: holding a card, holding a lantern, carrying a bag, waving, sitting calmly). No copying existing characters.
- Image generation (Nano Banana) needs billing or the Google quota (§1.6). Not required; a hand-made SVG is enough and loads offline.
- Put the mascot SVGs in the repo under the same open-source license.

### 6.8b Where the "vulnerable" flags come from (the app never guesses a disability)
| Source | How | Example |
|---|---|---|
| Intake form / handwritten list | AI reads the columns from the photo. BNPB Juklak 9/2023 already requires data broken down by age, sex and vulnerable group | "Lansia ✓ (elderly), Tuna rungu ✓ (deaf)" |
| Age | Computed: 65+ → elderly (threshold in Remote Config) | "Sato, 81" |
| Free-text notes | AI *suggests* flags from the notes | "tidak bisa dengar, pakai kursi roda" ("can't hear, uses a wheelchair") → hard of hearing + wheelchair |
| Self or family report | Staff ask at check-in, one tap | "Do you need announcements in writing?" |
| Care card (F4) | Medicines and treatments imply needs | Insulin → can't-miss treatment |
| Government registers (future) | Japan has evacuation-support registers in 99% of municipalities | Out of scope |
**Rule:** AI suggestions are drafts. **Staff confirm every flag.** Never infer a disability from appearance.
**Not covered (state this honestly):** people who stay at home and never reach the shelter. A later "at-home check list" could cover them; Noto staff phoned home-nursing patients one by one.

### 6.9 Medicine Round (core feature): connecting the organizer, the medicine and the people
**Why (evidence):**
- Iwate 2011: 11/12 (92%) of shelter evacuees who stopped blood-pressure medicine had very high BP; keeping medicine going prevents serious hypertension in the first 10 days.
- Evacuees often lose their medicine and insulin pens while fleeing (PLOS Currents systematic review).
- Katrina: 44% of dialysis patients missed at least one session; missing 3+ doubled hospitalisation (OR 2.16). Tohoku: about half of dialysis facilities were closed for more than 10 days.
- Tohoku: about 90% of disaster-related (indirect) deaths were people aged 66+; "fatigue from shelter life" was the top cause. Noto 2024: indirect deaths exceeded direct deaths.
- Aceh, Dec 2025: 126 puskesmas (community health centres) out of operation, so medicine supply ran through shelters and health posts.

**Roles (who uses it):**
| Role | What they do in the Medicine Round |
|---|---|
| **Organizer** (shelter coordinator) | Sees the whole picture: who needs what and when, shortages, can't-miss alerts; sends supply requests |
| **Volunteer** | Runs the round with the checklist: "08:00, give or remind these 14 people", taps ✓ |
| **Medic / pharmacist** (verified role, Auth claim) | **The only role allowed to confirm** a medicine reading, a substitute or a dispensing decision |
| **Evacuee / family** | Has a care card; confirms "yes, this is my medicine"; gets reminders on the shared screen or a printout (no phone needed) |
| **Health office / puskesmas** (cloud side) | Gets combined shortage requests and can't-miss transfer lists from many shelters |

**The flow:**
1. **Intake (on the device):** photo of the medicine bag, prescription notebook (お薬手帳), blister strip or prescription → AI extracts `{name, strength, dose, times per day, timing (before/after meals), quantity left}`.
   - The **photo is always shown beside the result.** The evacuee or family confirms; a medic confirms when available.
   - Drug names are **matched against an offline drug list** (fuzzy match, deterministic code, not AI). Unknown name → "needs a pharmacist", never a guess.
2. **Schedule (on the device):** a shelter-wide timetable is built: "08:00: 14 people · 12:00: 6 · 20:00: 11", each a person + medicine + dose **exactly as written on the label/prescription**. Kit holds the "next round" card on the big screen.
3. **Round (on the device):** the volunteer checklist shows ✓ taken, ✗ refused, or "medicine missing". People not checked show up on the reach list (F8).
4. **Can't-miss alerts (on the device):** dialysis, insulin, home oxygen, epilepsy drugs, anticoagulants, etc. (list in Remote Config) → **pinned at the top of the organizer screen** with "next due" and "supply left: 2 days".
5. **Stock and shortages (on the device):** counts per medicine: "6 people out of blood-pressure medicine within 2 days".
6. **Supply request (cloud, when there's signal):** shortages from all shelters are combined into one request **by generic (active ingredient) name** for the health office or puskesmas. No evacuee names, only counts (plus names when a medic requests it).
7. **Transfer (cloud):** for dialysis or oxygen, find open facilities nearby (Maps/Search grounding) and build a transfer list with priority.
8. **Delivery and confirm:** when supplies arrive, stock is updated and the round continues.

**"Alternative medicine": what is safe and what isn't**
| Level | What it means | Who decides | What the app does |
|---|---|---|---|
| ✅ **Same medicine, different brand** (same active ingredient, same strength, same form) | E.g. a brand name in Japan vs a generic in Indonesia | Matching is mechanical, but **a pharmacist or medic confirms before giving it** | Shows "Same ingredient: amlodipine 5 mg. Ask the pharmacist to confirm", with the source of the match |
| ⚠️ **Same ingredient, different strength or form** (5 mg vs 10 mg, tablet vs liquid) | The dose has to be recalculated | **Only a medic or pharmacist** | Shows "Different strength, needs a pharmacist". **Never calculates the dose** |
| ❌ **Different medicine, same purpose** (another blood-pressure drug) | A clinical decision (allergies, kidneys, interactions, other conditions) | **Only a doctor** | **Never suggests one.** Shows "Ask a doctor" and adds the person to the medic's list |
| ❌ **Herbal / traditional** (jamu, kampo) | Not verifiable and can interact | Doctor | Records it on the care card only ("takes jamu X"); never recommends it |

**What AI cannot do accurately (write these into the README and the video, which shows understanding of the problem):**
1. **Look-alike / sound-alike drug names** (very different medicines with similar spellings). Handwriting makes this worse. → Always match against the drug list, show the top candidates and require a human to confirm. Never auto-accept.
2. **Strength and units** (mg vs mcg, 0.5 vs 5). One misread digit can be fatal. → Show the photo crop of the number; a human must confirm it.
3. **Identifying a pill by how it looks.** → **Not supported.** The app only reads printed text and labels.
4. **Complete interaction checks.** AI and even databases miss some. → Present as "possible interactions to ask about", never as "safe".
5. **Dose decisions** (kidney function, children, elderly, pregnancy). → **Never.** Dose comes only from the label or prescription, or from a medic.
6. **Diagnosing symptoms.** → Not done. Symptom tallies (F10) are counts only.
7. **Storage** (e.g. insulin needs cool storage, some drugs are damaged by heat or flood water). → Show storage notes from the label or official leaflet only, and flag "check with the pharmacist".
8. **Unsupported languages** (Indonesian on the device, §2.4b). → Measure; route to the cloud or a human if accuracy is low.

**Hybrid split (why each part runs where it does):**
| Step | Where | Mode | Why |
|---|---|---|---|
| Read the medicine photo | Device | `ONLY_ON_DEVICE` + privacy guard | Medical data, no signal |
| Match the drug name | Device (deterministic code + offline list) | — (no LLM) | Accuracy matters more than AI here |
| Schedule, round, stock, alerts | Device | Local rules | Must work offline, all day |
| Cross-shelter supply request | Cloud | `ONLY_IN_CLOUD` + Firestore | One shelter can't see the total |
| Generic-name lookup and interactions | Cloud | `ONLY_IN_CLOUD` with Search grounding, with consent | Fresh, broad drug info; shown as "ask the pharmacist" |
| Find an open dialysis clinic | Cloud | `ONLY_IN_CLOUD` + Maps grounding | Live information |

**Offline drug list (data source, ⚠️ verify licensing before use):**
- Indonesia: **DOEN** (Daftar Obat Esensial Nasional, the national essential medicines list) and **Fornas** (Formularium Nasional). Registered products: BPOM.
- Japan: the national drug price list (薬価基準) / PMDA product information.
- Global fallback: the **WHO Model List of Essential Medicines**.
- For the hackathon demo: a **small curated list** (~100 common chronic-disease drugs: blood pressure, diabetes, epilepsy, anticoagulants, asthma) with generic name, common brands and strengths. Synthetic and clearly labelled "demo list, not for clinical use".

**Data model additions:**
```
shelters/{id}/medRound/{date}/{slot}: [{personId, medId, doseAsWritten, status(taken|refused|missing|pending), by, at}]
shelters/{id}/medStock/{genericName_strength_form}: {count, daysLeft, people:[personId]}
shelters/{id}/alerts/{id}: {type(dialysis|insulin|oxygen|...), personId, nextDue, supplyDays, status}
requests/{id} (cloud): {fromShelters[], items:[{generic, strength, form, qty}], priority, status}
careCards.meds[]: {nameAsWritten, matchedGeneric?, strength, dose, timing, qtyLeft, confirmedBy(self|family|medic), photoRef}
```

**Evaluation (add to §7):** test set D = 30 synthetic medicine labels and prescription notebook pages (Japanese, English and Indonesian), including look-alike names and tricky strengths. Measure name and strength accuracy on the device vs the cloud, and how often the deterministic matcher catches an AI misread. Report "0 auto-accepted errors" as the safety target.

**Demo moment (video):** airplane mode → Mr. Sato's medicine bag photo → schedule appears → the volunteer's 08:00 round → "Sato: amlodipine runs out in 2 days" → signal returns → a combined request from 3 shelters is sent to the puskesmas → the pharmacist role confirms a same-ingredient brand swap.

### 6.10 Power and signal: batch mode, airplane mode, Signal Scout
**Why:** Aceh 2025 had 4 days with no signal on any operator and 21 nights without power. Evacuees queued at the regent's office just to get internet. With no signal, a phone keeps searching for a tower and **drains its battery fast**. On-device AI also uses power, but only in short bursts.

**A. Batch mode (MUST)**
- Staff can **photograph** roster sheets and medicine bags at any time. Photos go into a **local queue** ("12 sheets waiting"), with no AI run yet.
- The AI processes the queue:
  - **when the device is charging** (e.g. generator hours), automatically, or
  - when staff tap "Process now" (urgent items such as a can't-miss medicine skip the queue).
- Each processed item still needs staff review (photo beside the result).
- The power schedule ("generator 18:00–22:00") can be set by staff so Kit says "I'll read the 12 sheets at 18:00 when power is on."
- Low-battery mode (≤20%, threshold in Remote Config): non-urgent AI pauses, the screen dims, and Kit prompts **"Print or save today's lists now."**
- **Paper fallback:** every day the app produces printable or photographable versions of the roster, medicine schedule and reach list. The app is never the only copy.

**B. Airplane mode tip (MUST)**
- When there's no signal for N minutes (Remote Config), Kit shows: **"No signal here. Turn on airplane mode to save battery. I'll keep working, and the Scout phone will tell us when signal is back."**
- The app works fully in airplane mode. **Wi-Fi and Bluetooth can be switched back on while in airplane mode** (both Android and iOS allow this), which the Scout features below use.

**C. Signal Scout (two or more devices)**
Roles:
- **Worker devices** (hub laptop, volunteer phones): in airplane mode, doing the work, saving battery.
- **Scout phone** (one volunteer phone, role assigned in the app): **keeps its mobile signal on** and checks for real internet.
  - Rotate the Scout role between phones so no single battery dies.
  - Put the Scout where signal is most likely (high floor, window, or the "signal spot" people know about).

How the Scout detects signal:
- Don't trust the OS "connected" flag; it can say connected with no internet. Do a **real check**: a small request to Firebase with a short timeout, every N minutes (interval in Remote Config, longer when the battery is low).
- When it succeeds 2 times in a row → **"Signal is back"**.

How the Scout tells the other devices (layered: simplest first):
| Level | Method | Works on | Priority |
|---|---|---|---|
| 1 | **Loud alert on the Scout phone** (sound, vibration, full-screen Kit: "Signal is back here! Bring devices to sync.") The volunteer tells others or carries the phone over | All | **MUST** |
| 2 | **Courier sync:** worker devices hand their queue to the Scout over a local link (QR codes or a file), and the Scout uploads it while it has signal ("store and forward") | All | **MUST** (QR) |
| 3 | **Hotspot:** the Scout volunteer turns on a mobile hotspot (manual; apps can't turn it on automatically on modern Android/iOS). Worker devices join over Wi-Fi (allowed in airplane mode) and sync directly. The app detects "online" and syncs the queue automatically | All (web hub too) | **SHOULD** |
| 4 | **Bluetooth beacon:** the Scout broadcasts a small Bluetooth LE signal ("signal available, hotspot NM-Shelter-3"). Worker phones in airplane mode with Bluetooth on scan in short, low-power intervals and show a notification: **"Signal is back near the Scout phone. Join hotspot NM-Shelter-3 to sync."** | Android (BLE or Nearby Connections); iOS limited in background; **web can't scan reliably** | NICE (stretch) |
| 5 | **SMS fallback:** if only SMS works (common when data is jammed), the Scout sends the short "I'm safe" and shortage messages (F6) as SMS | Phones | NICE |

Sync rules when signal returns:
- **Priority order:** (1) can't-miss medical alerts and transfer requests, (2) medicine shortage requests, (3) family search updates, (4) roster merge, (5) everything else.
- Upload **small text first** (structured JSON); photos only if needed and allowed. Data is expensive and the signal may drop again.
- Every job is idempotent (safe to retry) and has a visible state: waiting, sending, sent, failed.
- Kit shows: "Sent 14 of 20 items. 6 waiting for the next signal."

**Platform notes:**
- Web hub (laptop): Level 1–3 (online detection via a real fetch; it can join the Scout's hotspot over Wi-Fi).
- Android: all levels (BLE / Nearby Connections for Level 4).
- iOS: Levels 1–3; Level 4 is limited in the background.

**Data model additions:**
```
local/queue/{jobId}: {type(roster_photo|med_photo|request|family_update|...), priority, payloadRef, createdAt, state(waiting|processing|sending|sent|failed), retries}
local/power: {schedule:[{from,to}], lowBatteryThreshold, lastCharging}
shelters/{id}/scout: {deviceId, lastSignalAt, hotspotName?}
```

**Evaluation (add to §7):** measure **battery % used** for (a) processing 100 roster rows and 30 medicine photos on the laptop and on the phone, (b) 1 hour of Scout checks at the chosen interval. Put the numbers in the README and show one in the video.

**Demo moment (video):** all devices in airplane mode, working → the Scout phone at the window gets signal → loud Kit alert ("Signal is back!") → a worker phone gets the hotspot/Bluetooth notification → the queue syncs in priority order → combined medicine request sent.

### 6.11 Field Check + offline map (finding vulnerable people who did NOT reach the shelter)
**Why:** many elderly and disabled people stay at home (they can't move, won't leave, or don't hear warnings). In Noto, a welfare shelter phoned home-nursing patients one by one. The shelter roster misses these people entirely. This feature covers the gap we named in §6.8b.

**What it is (and is NOT):**
- ✅ A **check list + report tool** for volunteers going door to door: "who still needs to be checked, where, and what did we find".
- ✅ An **offline map** of pins: shelters, people to check, reports, hazards marked by volunteers.
- ❌ **NOT an automatic "best/safe route" finder.** Flood depth, collapsed bridges and landslides change by the hour, and the app can't know them offline. Claiming a "safe route" could send someone into danger. The app shows **direction and distance plus recent hazard pins with their time ("reported 40 min ago")**, and the volunteer decides.

**Who is on the check list:**
- Family reports ("my mother stayed at home"), roster notes ("husband stayed home"), the village head's list of people needing help (*kepala desa / RT/RW*; Japan: evacuation-support registers), plus volunteer finds.
- Staff confirm every entry. Locations of vulnerable people are **sensitive data**, visible to staff and rescue roles only.

**Flow:**
1. **Pin:** the volunteer's phone gets its position from **GPS, which works with no internet** (satellite signal; the first fix may be slower without data).
2. **Capture:** a photo of the house/situation plus a short voice note or text ("Pak Hasan, 78, can't walk, water knee-high, needs insulin").
3. **Structure (on the device):** speech-to-text → Nano turns the note (and the photo, on web/Android) into a report: `{people, ages, mobility, medical needs (links to the Medicine Round can't-miss list), water/hazard as reported, urgency}`.
   - **Urgency comes from fixed rules** (e.g. can't walk + water rising + insulin → top priority), not an AI guess.
   - **AI does not judge water depth or injuries from a photo.** Those fields come from what the volunteer reports. The photo is attached as evidence.
4. **Queue → sync:** reports wait in the queue (§6.10) and go out first when signal returns. Priority: urgent medical first.
5. **Cloud:** combine all reports into one **priority map for rescue teams** (BPBD/Basarnas in Indonesia; fire department / SDF in Japan). Optionally add public flood reports (PetaBencana.id ⚠️ check API and terms) and Maps grounding for open facilities.

**Camera capture:**
- Web (Chrome) and Android: Nano can read the photo on the device (JPEG/PNG; Android one bitmap). Use it only to **pre-fill** fields ("house, 2 people visible, water present") for the volunteer to confirm.
- iOS: on-device is text only, so photos are read in the cloud with consent, or just attached without AI.

**Offline map, technical:**
- GPS: Android/iOS location APIs (no network needed). Laptops often lack GPS; this is a phone feature.
- Map tiles: pre-download the area in "Shelter-ready mode" (e.g. **MapLibre + an OpenStreetMap extract in PMTiles/MBTiles**; OSM data is ODbL, so show attribution). ⚠️ The Google Maps SDK doesn't officially offer offline tiles to third-party apps; verify before relying on it.
- No offline routing engine in the hackathon scope (heavy, and unsafe for floods anyway).

**Scope for 9 days:** SHOULD = check list + photo/voice report + GPS pin + queue. NICE = offline map view. Cut first if time is short.

**Demo moment:** a volunteer at a house in airplane mode → GPS pin + photo + voice note "Pak Hasan, 78, can't walk, needs insulin" → structured report, top priority (rule) → linked to the can't-miss insulin alert → signal returns → it appears on the rescue team's priority map.

### 6.11b Offline hazard and elevation intelligence (researched 2026-11-21)
**Key idea:** most "where is risky / where is safer" knowledge **already exists as open data that can be downloaded before a disaster**. Combined with phone GPS (no internet needed), the app can answer offline: *"Am I in a hazard zone? Where is the nearest place that is higher, or designated for THIS hazard?"* That's mostly **plain geographic computation, not AI**. The AI's job is to turn the result into a simple instruction for elderly people (plain words, big card, read aloud, translated).

**Data we can pre-download (Shelter-ready mode, per area):**
| Data | What it gives | Coverage | Access / licence |
|---|---|---|---|
| **GSI designated emergency evacuation sites (指定緊急避難場所)** | Every site **with the hazard types it's designated for** (flood; cliff collapse/debris flow/landslide; storm surge; earthquake…; tsunami ⚠️ check the field list) | Japan (sites municipalities registered and agreed to publish) | Open data, CSV + GeoJSON (GSI) |
| **GSI elevation tiles (標高タイル)** | Ground height | Japan | Open XYZ tiles (cyberjapandata.gsi.go.jp); check the GSI tile terms |
| **MLIT/GSI hazard map portal** (重ねるハザードマップ): flood inundation zones (洪水浸水想定区域, maximum scenario), landslide warning zones, tsunami, storm surge | Hazard zones and expected flood depth | Japan | ⚠️ Tile URL and terms for third-party apps unverified |
| **InaRISK (BNPB)** | Hazard layers: flood, landslide, tsunami, earthquake, forest fire | Indonesia | Download function on the site + ArcGIS ImageServer layers (e.g. `layer_bahaya_banjir`). ⚠️ Terms and licence unverified |
| **DEMNAS (BIG)** | National elevation model at **~8 m** (built from IFSAR/TerraSAR-X/ALOS) | Indonesia | Free download after registering on BIG's portal; check redistribution terms |
| **World Bank: Indonesia river flood hazard** (GLOFRIS) | Flood depth for return periods of 5–1000 years | Indonesia | World Bank Data Catalog |
| **Banda Aceh tsunami escape buildings** | 6 official buildings + **96–134 alternative vertical-evacuation buildings** identified in studies | Banda Aceh | Academic papers / JICA reports / BPBD (need to digitise) |
| **Copernicus DEM GLO-30** | Global 30 m elevation; more accurate than SRTM (FABDEM best in flood-prone areas) | Global | Free licence, with the attribution "produced using Copernicus WorldDEM-30 © DLR e.V." |
| **Global HAND 30 m** (Height Above Nearest Drainage, from Copernicus DEM) | **How high a spot is above the nearest river/stream.** Low HAND = likely to flood; a well-known, light flood-susceptibility proxy | Global | AWS Open Data (attribution); MERIT Hydro HAND at 90 m |
| **NASA LHASA landslide susceptibility** | Landslide susceptibility (slope, geology, faults, forest loss) | Global, ~1 km | NASA GPM (open source) |
| **OpenStreetMap** | Roads, buildings, hospitals, `emergency=assembly_point` (with `assembly_point:tsunami=yes` / height), shelters | Global (Indonesia mapped by HOT with BPBD) | ODbL (attribution) |
| **PetaBencana.id API** (online only) | Live crowdsourced flood reports (last 3 h by default, GeoJSON); flooded areas and gauges **Jakarta only** | Indonesia | Open API; set a User-Agent. ⚠️ Data licence unconfirmed; reports unverified |

**What the app computes offline (deterministic code, no AI):**
1. **"Am I in a hazard zone?"** Look up the GPS point in the hazard layers: flood zone (and expected depth where available), low HAND, landslide susceptibility, tsunami zone.
2. **Nearest place for THIS hazard:** the nearest shelter **designated for the current hazard** (Japan: by hazard type), a tsunami escape building, or **higher ground** (e.g. elevation ≥ +N m or HAND ≥ N m; thresholds in Remote Config), with direction, distance and height gain.
3. **Prioritise the Field Check list by hazard:** each at-home vulnerable person is scored by their location's hazard plus their needs. Example: "Pak Hasan: flood zone (deep) + can't walk + insulin → visit first." **This connects the map to the elderly and medicine features.**
4. **Hazard pins from volunteers** (flooded road, landslide, bridge out) with timestamps, overlaid on the static layers.
5. **(Stretch) Offline routing:** **BRouter** (Android, offline, OSM, elevation-aware) or GraphHopper, with a custom cost that penalises low-HAND roads, hazard polygons and blocked pins. Still advisory, never "safe".

**Where the AI fits:**
- **On the device:** turns the computed result into a plain card for elderly people and staff: *"This house is in a flood area. The school 600 m north is 8 m higher and is a flood shelter."* Read aloud with OS TTS, translated with the LanguageService. Also structures field reports (§6.11).
- **Cloud (when the Scout finds signal):** combines field reports with live data (PetaBencana reports, BMKG warnings, Maps grounding for open facilities) into the rescue team's priority map.

**Accuracy and safety rules:**
- Hazard maps are **scenarios, not live conditions**. Real floods can be bigger than the map (e.g. flash floods). Always show "map data from [source, date]".
- Elevation errors are metres (Copernicus LE90 < 4–6 m; SRTM overestimates in vegetation and built-up areas). Use for *relative* "higher/lower", never exact depth.
- **Never say "safe".** Say "higher", "designated for floods", "lower risk on the map".
- Live volunteer hazard pins override static layers in the display.

**Technical notes:**
- Map: MapLibre Native reads a local `pmtiles://` file on Android and iOS. **MapLibre GL JS (web) has no offline API** → the web hub caches tiles with the service worker or loads a local file. PMTiles region extracts can be cut from the planet file by bounding box or GeoJSON. One PMTiles file per map in MapLibre Native.
- Hazard and elevation rasters: clip to the demo area and convert to small tiles or a compact grid; look up values in code. Sizes: ⚠️ test (expect small for one town).
- Hillshade/contours are separate layers or computed from elevation tiles. Optional, visual only.

**Scope for 9 days:**
- MUST-lite (if F12 is kept): one demo area (one Aceh town or one Noto town) with shelters + **one hazard layer (flood or HAND)** + elevation → features 1–3.
- NICE: hillshade, landslide layer, volunteer hazard pins.
- Stretch: offline routing (BRouter).

**Demo moment:** airplane mode → GPS shows "flood area (deep)" → Kit card: "Nearest higher place: school, 600 m north, +8 m, flood shelter" → read aloud → the Field Check list re-sorts with Pak Hasan first.

### 6.11c Hazard-aware Field Check priority (decided: build this, 2026-11-21)
**Goal:** volunteers have limited time and battery. The list of at-home vulnerable people is **sorted so the people in the most danger with the most needs are visited first**, offline, with the reasons shown.

**Principles:**
- **Deterministic and explainable.** Plain rules and points, **no AI** in the scoring. Every person shows "Why first: …".
- **Weights and thresholds live in Remote Config** (with in-code defaults for offline), so experts can tune them without a release.
- **Missing data never pushes someone down.** Unknown location or unknown needs count as risky ("unknown, visit early").
- **Staff can override** (pin to the top or move down), with a short reason that is logged.
- **Event-aware:** staff set the current event (flood / earthquake / tsunami / landslide). Only the matching hazard layers count.

**Inputs per person:**
- Location: GPS pin, address match, or "unknown".
- From the roster, family reports, Field Check reports and care cards (§6.8b, §6.9): mobility, can't-miss treatment and supply left, other daily medicines, hearing, dementia, living alone, age.
- Last contact time.
- From §6.11b: hazard values at the location; volunteer hazard pins nearby.

**Default points (Remote Config `fieldPriority.*`):**
| Factor | Points |
|---|---|
| **Hazard at location** (current event only) | Flood: none 0 · shallow (<0.5 m) 1 · medium (0.5–3 m) 2 · deep (>3 m) 3. If there's no flood layer, use HAND: >10 m 0 · 5–10 m 1 · 2–5 m 2 · <2 m 3. Landslide susceptibility high +2. Tsunami zone (tsunami event) 3 |
| Location unknown | Hazard counts as **3** + label "location unknown" |
| Volunteer hazard pin within 200 m in the last 6 h | +2 |
| **Mobility** | Bedridden / can't walk 3 · wheelchair or needs help 2 · walks slowly 1 |
| **Can't-miss treatment** (dialysis, insulin, home oxygen, …) | 3, plus +1 if supply ≤ 1 day |
| Other daily medicines | 1 |
| Deaf / hard of hearing | 1 (may not have heard the warning) |
| Dementia / cognitive impairment | 2 |
| Lives alone | 2 |
| Age | 75–84: +1 · 85+: +2 |
| Last contact | Never checked +2 · more than 24 h ago +1 |

**Tiers:**
- **P1 "Visit first"** if total ≥ 9, **or** any override rule:
  - can't walk **and** hazard ≥ 2,
  - can't-miss treatment **and** supply ≤ 1 day,
  - location unknown **and** (can't walk **or** can't-miss treatment).
- **P2 "Visit today"** if total 5–8. **P3 "Check when possible"** below 5.
- **Within a tier:** sort by score, then by **distance from the volunteer's current GPS**, so one trip covers nearby people.

**What the volunteer sees:**
- `P1 · Pak Hasan, 82 · 450 m NE` → **Why first:** deep flood zone · can't walk · insulin, 1 day left · lives alone · never checked.
- Kit's card for the next visit, plus the "nearest higher place / shelter for this hazard" from §6.11b.
- After a visit (Field Check report), the score recomputes: last contact updates, new needs are added, the person is moved to "safe at home", "needs evacuation" or "moved to shelter".

**Where AI is (and isn't):**
- AI only **suggests flags from free-text notes** (e.g. "tidak bisa jalan", "can't walk" → can't walk), and staff confirm them (§6.8b). It also writes the plain-language visit card.
- The **score, tiers and order are pure code.** Easy to test, explain and trust.

**Reference implementation (TypeScript, the same logic in Kotlin):**
```ts
type Hazard = { floodClass?: 0|1|2|3; hand?: number; landslideHigh?: boolean; tsunamiZone?: boolean; unknown?: boolean; pinNearby?: boolean };
type Person = { age?: number; mobility?: 0|1|2|3; cantMiss?: boolean; supplyDays?: number; dailyMeds?: boolean;
  hearing?: boolean; dementia?: boolean; alone?: boolean; hoursSinceContact?: number | null; hazard: Hazard };
type Event = 'flood'|'earthquake'|'tsunami'|'landslide';

export function hazardPoints(h: Hazard, ev: Event): number {
  if (h.unknown) return 3; // ponytail: unknown counts as worst case; refine when address matching improves
  let p = 0;
  if (ev === 'flood') p = h.floodClass ?? (h.hand == null ? 0 : h.hand < 2 ? 3 : h.hand < 5 ? 2 : h.hand <= 10 ? 1 : 0);
  if (ev === 'tsunami' && h.tsunamiZone) p = 3;
  if ((ev === 'landslide' || ev === 'earthquake' || ev === 'flood') && h.landslideHigh) p += 2;
  return p + (h.pinNearby ? 2 : 0);
}

export function priority(p: Person, ev: Event) {
  const hz = hazardPoints(p.hazard, ev), why: string[] = [];
  let s = hz; if (hz >= 2) why.push('hazard at location');
  if (p.hazard.unknown) why.push('location unknown');
  s += p.mobility ?? 0; if ((p.mobility ?? 0) >= 2) why.push('limited mobility');
  if (p.cantMiss) { s += 3 + ((p.supplyDays ?? 0) <= 1 ? 1 : 0); why.push(`can't-miss treatment, ${p.supplyDays ?? '?'} day(s) left`); }
  else if (p.dailyMeds) s += 1;
  if (p.hearing) { s += 1; why.push('hard of hearing'); }
  if (p.dementia) { s += 2; why.push('dementia'); }
  if (p.alone) { s += 2; why.push('lives alone'); }
  if ((p.age ?? 0) >= 85) s += 2; else if ((p.age ?? 0) >= 75) s += 1;
  if (p.hoursSinceContact == null) { s += 2; why.push('never checked'); } else if (p.hoursSinceContact > 24) s += 1;
  const cantWalk = (p.mobility ?? 0) === 3, lowSupply = !!p.cantMiss && (p.supplyDays ?? 0) <= 1;
  const override = (cantWalk && hz >= 2) || lowSupply || (!!p.hazard.unknown && (cantWalk || !!p.cantMiss));
  const tier = override || s >= 9 ? 'P1' : s >= 5 ? 'P2' : 'P3';
  return { score: s, tier, why };
}

// Self-check (run with: npx tsx priority.ts)
if (import.meta.url === `file://${process.argv[1]}`) {
  const hasan = priority({ age: 82, mobility: 3, cantMiss: true, supplyDays: 1, alone: true, hoursSinceContact: null, hazard: { floodClass: 3 } }, 'flood');
  console.assert(hasan.tier === 'P1' && hasan.score === 15, 'Hasan must be P1/15', hasan);
  const unknownInsulin = priority({ cantMiss: true, supplyDays: 5, hoursSinceContact: 2, hazard: { unknown: true } }, 'flood');
  console.assert(unknownInsulin.tier === 'P1', 'unknown location + cant-miss must be P1');
  const safeHigh = priority({ age: 70, mobility: 0, hoursSinceContact: 2, hazard: { hand: 15 } }, 'flood');
  console.assert(safeHigh.tier === 'P3', 'healthy person on high ground must be P3');
  console.log('priority self-check done');
}
```
- Tests to keep: the 3 asserts above, plus "a tsunami zone counts only for a tsunami event", "a staff override wins over the score", and "same tier → nearer first".

**Data model additions:**
```
fieldCheck/{personId}: {location:{lat,lng}|null, source(gps|address|unknown), needs:{mobility, cantMiss, supplyDays, dailyMeds, hearing, dementia, alone, age},
  lastContactAt|null, status(unchecked|safe_home|needs_evacuation|moved_to_shelter), override:{tier, reason, by, at}|null}
config (Remote Config): fieldPriority.weights, fieldPriority.tiers, fieldPriority.overrides, currentEvent
```

**Evaluation:** 20 synthetic at-home cases ranked by the rules vs. ranked by an experienced volunteer or nurse (if we can interview one). Report agreement on the top 5. This also gives a quote for "genuine understanding of the problem space".

### 6.12 Repo and code architecture (one repo, clean, robust)
**Style:** ports and adapters ("hexagonal"). The **domain** (rules, schemas, state machines) is pure code with no Firebase/Android/browser imports. Platform-specific things sit behind small interfaces (**ports**) with one implementation per platform (**adapters**). This keeps the logic testable and identical on web and Android. Don't add an interface until there's a real second implementation or a test needs it (no speculative abstractions).

```
nobody-missing/
├─ LICENSE (Apache-2.0)          README.md           PRODUCT.md
├─ shared/                       # single source of truth, no code
│  ├─ schemas/                   # JSON Schemas: roster, careCard, med, announcementCard, fieldReport, request
│  ├─ prompts/                   # prompt pack (versioned); also loaded into Remote Config defaults
│  ├─ rules/                     # priority weights, can't-miss list, tiers, thresholds (JSON) = Remote Config defaults
│  ├─ drugs/                     # demo drug list (generic, brands, strengths) "not for clinical use"
│  ├─ regions/                   # region-pack manifest format + the demo Aceh pack manifest
│  └─ fixtures/                  # golden test cases + eval sets A–G (synthetic only)
├─ web/                          # TypeScript + Vite PWA (hub laptop + big screen + phone view)
│  └─ src/
│     ├─ domain/                 # pure: priority, drugMatcher, schedule, dedupe, queue state machine, router policy
│     ├─ ports/                  # Inference, Language, Storage, Sync, Location, Clock
│     ├─ adapters/               # firebaseAiLogic, webSpeech, firestore, indexedDb, serviceWorker
│     ├─ features/               # roster, announcements, careCard, medicineRound, reachList, familySearch, scout, fieldCheck
│     └─ ui/                     # components, Kit SVGs, big-screen mode, accessibility
├─ android/                      # Kotlin + Jetpack Compose, same layering
│  └─ app/src/main/java/.../{domain,ports,adapters,features,ui}
├─ hub/                          # small local web server + hotspot guide for Shelter Wi-Fi (no AI)
├─ tools/                        # region-pack builder (clip HAND/DEM/OSM/shelters to a bbox), eval runner
└─ .github/workflows/            # CI: web tests, Android unit tests, both run shared/fixtures
```

**Patterns used (and why):**
| Pattern | Where | Why |
|---|---|---|
| Ports & adapters | All platform services | Same domain on web + Android; easy tests with fakes |
| **Strategy** | `InferenceRouter` picks device / cloud / review per task from a policy table | Accuracy-first routing is data (Remote Config), not `if` chains |
| **Outbox / job queue + state machine** | Sync jobs: waiting → processing → sending → sent / failed | Offline-safe, idempotent retries, priority order |
| Repository | Roster, care cards, medicine stock, field checks | One place for local storage + Firestore sync |
| Specification / rules as data | Priority scoring, can't-miss list, flags | Tunable by experts without a release; same JSON on both platforms |
| **Contract tests (golden fixtures)** | `shared/fixtures` run by Vitest and JUnit | Proves web and Android give identical results |
| Anti-corruption layer | Mapping AI output (schema-validated JSON) into domain objects | AI output never enters the domain unvalidated |

**Code rules:** schema-validate every AI output; no AI call outside `InferenceRouter`; no network call outside `Sync`; every medical/identity field keeps `asWritten` + `confirmedBy`; synthetic data only; small files, one feature per folder.

### 6.13 Region packs and Shelter-ready mode (global by design)
A **region pack** is everything one area needs offline, downloaded **in normal times** and only for the regions you choose:
| Part | Source | Notes |
|---|---|---|
| Map tiles | OpenStreetMap extract (PMTiles) for the bbox | ODbL attribution |
| Flood layer | **Global HAND 30 m** clipped to the bbox | Main hazard layer (D8) |
| Elevation | **Copernicus DEM 30 m** clipped, **only if this region needs it** (higher-ground finder) | Attribution "Copernicus WorldDEM-30 © DLR e.V." |
| Shelters | OSM (`emergency=assembly_point`, shelters) + shelters typed in by the organizer | Each with name, capacity, hazard types if known |
| Languages | Translation/speech/voice packs for the region (§2.8) | English always; others on demand |
| Drug list | Demo list; later national lists (DOEN/Fornas, 薬価基準, WHO EML) | Licence check before real use |
| Config | Region ID, shelter IDs, hub device ID, thresholds | Lets the hub + QR courier know every shelter in advance (D7) |
- **Shelter-ready mode** (first run, in normal times, Wi-Fi): pick region(s) → download the region pack + the on-device model (~4 GB in Chrome; Android via AICore) + languages → test run → "Shelter-ready ✓" with sizes and dates shown.
- Built by `tools/region-pack` from a bounding box; the demo ships one Aceh pack. Other regions = run the same tool (global by design).

### 6.14 Accuracy-first model choices (D10: lightweight only where it doesn't cost accuracy)
| Task | Default path | Why |
|---|---|---|
| Read roster / medicine photos | Device (Nano) **+ deterministic validators + staff confirm**; low-confidence rows → cloud re-check when online | Privacy + offline, but accuracy guarded |
| Cloud tasks needing accuracy (medicine, merge/dedupe hints, translation, re-checks) | **`gemini-3.8-flash`** (free tier) | More capable than Flash-Lite; accuracy first |
| Simple cloud formatting (short cards when the device failed) | `gemini-3.5-flash-lite` as backup | Cheaper, only where measured equal |
| Gemini 4 Neon | Evaluate on sets A–E; use where it measures best | ⚠️ unknown strengths |
| Drug names, priority, dedupe keys | **Deterministic code, no LLM** | Exactness matters most |
| Flood layer | HAND **30 m** (not 90 m) | Better resolution |
- Model names per task live in Remote Config; the evaluation (§7) picks the winner per task. Never trade accuracy for size or speed on medicine or identity fields.

### 6.15 Cross-cutting requirements
- **Privacy:** consent at check-in (one screen, plain language); medical data local by default; data deleted or archived when the emergency status ends. Laws to name in the README: Indonesia **UU PDP (Law 27/2022)** (health data = specific personal data), Japan **APPI**. Synthetic data only in the repo.
- **Family search (D6):** public, scoped to the region's shelters and surrounding area, minimal fields; "hide me" on request via staff.
- **Roles (no login, D14):** organizer (creates the shelter, shows join QRs), volunteer, medic/pharmacist (joins via the medic QR), Scout (any volunteer phone). Silent Anonymous Auth + a shelter membership record let security rules check roles.
- **Accessibility:** large text (min 18 pt UI, huge on the big screen), high contrast, device/cloud badges use **icon + text, not colour alone**, screen-reader labels, plain language, works in black-and-white printouts.
- **Live hybrid counter** on the hub: "Today: N on device · N cloud · N waiting".
- **Judge testing notes** (README): hosted URL, a demo shelter with ready-made join QRs (no accounts needed), App Check debug-token steps, "on-device needs Chrome ≥148 + Nano; otherwise cloud fallback runs (badges show which)".
- **User input:** try to interview 1–2 real people (PMI/Tagana volunteer, nurse, deaf community member). Quote them in the write-up.
- **Test data realism:** roster sheets copy the BNPB Juklak 9/2023 form columns (name, NIK optional, sex, age group, vulnerable group, needs).

### 6.16 UI/UX: simple, few pages, little text, no login (decided 2026-11-21)
**One app, three views. No login screens anywhere (D14).** Evacuees mostly don't use the app (many elderly people have no phone, §5.5); they use the Big Screen, printouts, staff, or the Public pages on their own phone.

| View | Who | Device | How you get in |
|---|---|---|---|
| **Staff app** | Organizer, volunteer, medic, Scout (pages shown by role) | Android phone + web hub laptop | The organizer's device creates the shelter; others **scan a "Join as volunteer / medic" QR** |
| **Big Screen** | Everyone in the shelter, incl. deaf + elderly | Hub laptop on a TV/projector, full screen | Opened from the hub |
| **Public pages** | Evacuees with a phone (e.g. Ibu Rina), families | Any phone browser | **Scan the shelter QR** (on the wall, printouts, Big Screen) |

**No login, how it works (D14):**
- Joining = scanning a QR from the organizer's device. The QR carries the shelter ID, region pack ID and role, signed with the shelter key. The role is stored on the device.
- Behind the scenes, Firebase **Anonymous Auth** runs silently (no screen) when online, so Firestore security rules can still check "this device joined shelter X as a medic". Auth stays in use (encouraged by the rules) with zero login UX.
- The organizer can revoke a device (lost phone) from Settings.

**Offline Public pages need "Shelter Wi-Fi" (D15):**
- With no internet, a phone can't load a website. So the hub laptop **creates a local Wi-Fi hotspot** ("NobodyMissing-SD3") and runs a **small local web server** (`hub/` in the repo: plain code, no AI) that serves the Public pages, the region-pack map tiles and the latest cards.
- The shelter QR = "join this Wi-Fi" + the local page address. Phones don't need internet or an install.
- Bonus: staff phones on the same Wi-Fi **sync with the hub directly** (faster than the QR courier, which stays as the fallback).
- Limits: phone GPS doesn't work on a local (non-HTTPS) page, so the public map is **centred on the shelter** (fine for people inside it). Family search offline covers **this shelter only**; other shelters appear when the signal returns (labelled).
- Fallback if Shelter Wi-Fi isn't possible: **text QR codes**. Each card's text is put directly in a QR, so any phone camera shows it offline without a server.

**Staff app: 5 tabs + settings:**
| Tab | Purpose | Main button |
|---|---|---|
| 🏠 **Today** | What needs doing now: big tiles with counts ("💊 3 now", "⚠️ 2 can't-miss", "📢 5 to tell", "🏚 4 visits", "🙋 2 help requests") | — |
| 👥 **People** | Roster + search + flags; tap a person → care card | 📷 **Scan list** |
| 💊 **Medicine** | Next round ✓/✗, can't-miss on top; stock + requests | ✓ **Given** |
| 📢 **Announce** | Type (or speak) → preview card → show on Big Screen / Public / print; reach list ✓ | ➤ **Show** |
| 🏚 **Field** | Hazard-aware visit list (P1/P2/P3 + why); report 📷 + 🎤; map | 📷 **Report** |
| ⚙️ Settings (icon) | Region pack + Shelter-ready, Shelter Wi-Fi, join QRs, languages, Scout role, power schedule | — |

**Public pages: 4 pages, built for deaf and elderly people:**
| Page | What | Deaf / elderly details |
|---|---|---|
| 📢 **News** | The shelter's announcement cards, newest first | Huge text + icon + time + place; 🔊 read-aloud button; **a new card flashes the screen + vibrates** (deaf users notice without sound) |
| 🗺️ **Map** | Shelter-centred map: this shelter, **safer/higher places and the shelter for this hazard**, medical post, water, toilets, kitchen, flood-risk shading (HAND), all from the region pack | Big icons with 1-word labels; "nearest higher place: school, 600 m north, +8 m"; never says "safe" (§6.11b) |
| 🙋 **Help** | Ask staff for help **without speaking**: tap 💊 medicine · 🩺 feeling sick · 🚻 need help moving · 🍼 baby · 💬 talk in writing · 📍 my spot (bed/area number) | Built for deaf people (and anyone who can't speak up). Arrives on the staff **Today** tab as "🙋 help request". "💬 talk in writing" opens write-to-talk (F5) |
| 🔍 **Family** | Find a person: name → shelter + status (D6) | Big search box; offline = this shelter only (labelled) |

**Big Screen (one page, auto-rotating, no buttons):** latest announcement card (≥ 60 pt main line) → next medicine round → mini map ("higher place / medical post") → "Scan for News · Map · Help · Family" QR. Readable from 10 m. **For elderly people without phones, the Big Screen + printouts carry the same News and Map.**

**Where Kit lives (exactly):**
| Place | Kit's job |
|---|---|
| Status bar (staff app, every tab) | Small Kit icon: 🏮 holding the lantern = working on the device · 🎒 carrying = syncing · ⏳ waiting = "3 waiting for signal" |
| Big Screen | **Holds the current announcement card**: the "official shelter info" sign |
| Public pages header | Small Kit + "Shelter SD 3" so people know they're on the right page |
| Empty states | "All checked. Take a breath." / "No one waiting for medicine now." |
| Onboarding + Shelter-ready | Step-by-step guide for first-time volunteers |
| Scout alert | Full-screen Kit: "Signal is back here!" |
| Printouts | Kit at the top of printed cards and lists |
| **Never** | Bad news: family "not found", injuries/deaths, evacuation or tsunami orders (§6.8) |

**Every screen follows the same 6 rules:**
1. **One main action per screen**: a big button at the bottom, at least 48 dp tall.
2. **Icon + 1–3 words**, never icon alone, never a paragraph.
3. **Camera first.** Every AI result opens the **same review screen**: photo on top, fields below, uncertain fields highlighted, **✓ Confirm / ✎ Fix**.
4. **Status = icon + word + colour** (never colour alone): 🏮 Device · ☁️ Cloud · ⏳ Waiting (n) · ⚠️ Needs review.
5. **Max 2 levels deep** (tab → detail).
6. **Works the same offline**: no network spinners; queued items show ⏳.

**Visual style:** large type (UI ≥ 18 pt; Big Screen ≥ 60 pt), high contrast, lots of white space, one accent colour (safety orange from Kit's vest) for main buttons, red only for can't-miss/P1. Works on cheap phones and black-and-white printouts. English + Indonesian, one-tap switch.

**Wireframe: Today (staff phone)**
```
┌──────────────────────────────┐
│ 🏮Kit  Shelter SD 3 · ⏳3      │
├──────────────┬───────────────┤
│ 💊 3  now    │ ⚠️ 2 can't-miss │
├──────────────┼───────────────┤
│ 📢 5  to tell │ 🏚 4  visits   │
├──────────────┼───────────────┤
│ 🙋 2  help    │ 👥 118 people  │
└──────────────┴───────────────┘
 🏠Today 👥People 💊Medicine 📢Announce 🏚Field
```
**Wireframe: Public page (phone, News)**
```
┌──────────────────────────────┐
│ Kit · Shelter SD 3     EN|ID │
│ 🍙 FOOD · 15:00               │
│ 📍 Hall entrance        🔊    │
├──────────────────────────────┤
│ 📢News 🗺️Map 🙋Help 🔍Family  │
└──────────────────────────────┘
```

---

## 7. Evaluation plan (our strongest Tech evidence; accuracy is the #1 goal)
Every set has ground-truth JSON in `shared/fixtures/`. Each run reports device vs cloud: accuracy, latency, and the routing rule derived from the numbers.
| Set | Feature | Content | Main metric |
|---|---|---|---|
| A | F1 roster | 30–50 synthetic handwritten sheets **in the BNPB Juklak 9/2023 form layout** (Indonesian + English names; a few Japanese) | Field accuracy (name, age, sex, vulnerable flags) |
| B | F2 cards | 50 announcement texts (food, water, medicine, moves, warnings), Indonesian + English | Time/place/action correct |
| C | F4 care card | 20 medicine bag / prescription photos | Fields correct |
| D | F9 medicine | 30 labels incl. look-alike names and tricky strengths (mg/mcg, 0.5/5) | Name + strength accuracy; **0 auto-accepted errors** (safety target) |
| E | Indonesian | Sets A/B in Indonesian: Nano (language-neutral extraction) vs cloud Gemini; keyword-icon mapper vs ground truth | Accuracy per path → routing rule |
| F | F12 priority | 20 synthetic at-home cases ranked by rules vs a volunteer/nurse (if interviewed) | Top-5 agreement |
| G | Power | 100 roster rows + 30 medicine photos; 1 h of Scout checks | Battery % on laptop and phone |
- **Accuracy-first routing rule:** for every task, the path with the best measured accuracy wins. On-device is used when it is within the accuracy bar set in Remote Config (default: no worse than the cloud by more than 3 points on that task). Below the bar: on the device = draft + "needs review", and a cloud re-check is queued for when signal returns.
- Automated tests: domain rules (priority, drug matcher, queue, router, privacy guard) run the **same golden fixtures on web (Vitest) and Android (JUnit)**, so both platforms provably behave the same. Playwright for the web flows.

---

## 8. Demo video (≤ 3:00, English subtitles) — one story, deaf + elderly first
Characters (synthetic): **Pak Hasan, 82** (hard of hearing, can't walk far, insulin + blood-pressure medicine) and **Ibu Rina, 45, deaf** (reads text, has a phone). Setting: a flood shelter in Aceh; the product is global.
- **0:00–0:30:** the problem in 3 facts (Sumatra 2025: 975,079 displaced, 4 days without signal; 179,000 records became 60,426 households after removing duplicates; deaf people died at 4× the rate in Kobe) + "Nobody Missing: Firebase AI Logic on the device when there's no signal, the cloud when it returns." Meet Kit.
- **0:30–1:05:** **airplane mode.** Photo of the handwritten BNPB-style list → roster with flags (green "device" badges, photo beside it). Reach list: "Pak Hasan and Ibu Rina must be told in person."
- **1:05–1:30:** staff type an announcement → big-text Kit cards on the big screen; Ibu Rina reads it on her phone via QR; Pak Hasan gets it from the printed sheet / a volunteer ✓.
- **1:30–2:00:** Pak Hasan's medicine bag photo → care card → 08:00 Medicine Round ✓ → "insulin: 1 day left" pinned as can't-miss.
- **2:00–2:30:** Field Check: hazard-aware list puts an at-home elderly woman in a low-HAND flood zone first; Kit card "higher place: school, 600 m north, +8 m".
- **2:30–2:50:** the Scout phone at the window gets signal → loud alert → QR courier → priority sync → combined insulin request to the puskesmas; Pak Hasan's daughter finds him in public search (blue "cloud" badges).
- **2:50–3:00:** the live counter ("214 on device · 12 cloud"), evaluation numbers, Android + web shown running.
- Film on real devices (Chrome desktop + an Android phone with Gemini Nano 4). Upload by Sun Nov 29.

## 9. Write-up structure (Devpost story), mapped to the criteria
Inspiration (evidence §5, deaf + elderly) → What it does (F1–F4, F8, F9, F11, F12) → **How we use Firebase AI Logic** (mode table §6.2, accuracy-first routing §6.3/§7, privacy guard, Remote Config, App Check, Firestore, Auth) → **Evaluation** (§7 tables) → Challenges (SDK gotchas §2.7, Indonesian not supported on the device) → What AI can't do and how we guard it (§6.9) → What's next (more region packs, government register import, iOS).
Explicit and scannable: AI-assisted judging may read it.

---

## 10. 9-day plan (solo + one coding agent; Sat Nov 21 → Mon Nov 30, 2:00 PM PT)
All 8 MUST features are kept (decision 2026-11-21). Order = build order; the **cut line** below says what drops first if a day slips.
| Day | Date | Goal |
|---|---|---|
| 1 | Sat Nov 21 | Repo skeleton (§6.12), Firebase project, App Check, Remote Config defaults; **Day-1 checks (§12)**: Nano on the laptop, Nano 4 on the phone, Indonesian on Nano, Indonesian offline speech; post the forum question |
| 2 | Sun Nov 22 | `shared/`: schemas, prompt pack, rules spec, golden fixtures. Web: router + privacy guard + F1 roster (photo → review table) |
| 3 | Mon Nov 23 | Web: F4 care card + F9 Medicine Round (schedule, round ✓, can't-miss alerts, stock, drug matcher) |
| 4 | Tue Nov 24 | Web hub: F2 typed announcement → Big Screen cards; Public pages (News, Map, Help, Family) served by the `hub/` local server (Shelter Wi-Fi); join QRs (no login); F8 reach list; F3 cloud merge/dedupe + public search + supply requests |
| 5 | Wed Nov 25 | F11 queue + batch mode + Scout (real check, alert, QR courier); region pack builder (HAND + DEM clip + shelters) ; F12-lite priority list. Build eval sets A–E |
| 6 | Thu Nov 26 | Android: shell, router, F1, F4/F9 (same fixtures pass) |
| 7 | Fri Nov 27 | Android: F11 Scout + F12 Field Check (GPS, photo/voice report, priority list); run all evaluations, set routing thresholds |
| 8 | Sat Nov 28 | Kit SVGs, accessibility pass, English + Indonesian UI, live counter, README, hosting, testing notes |
| 9 | Sun Nov 29 | Video, Devpost story, screenshots; upload to YouTube |
| — | Mon Nov 30 | Buffer; submit **before 2:00 PM PT** (22:00 UTC; 05:00 Tue Dec 1 WIB) |
**Cut line (drop in this order if behind):** speech input for F2 → Shelter Wi-Fi server (fallback: text QR cards) → offline map view (keep the list) → hotspot auto-sync → BLE beacon → F10 outbreak counts → F5/F6/F7 → iOS (not planned).

---

## 11. Risks and mitigations
| Risk | Mitigation |
|---|---|
| No hardware that runs Nano | Day-1 check; minimums in §12. Without a Nano phone, Android shows cloud fallback only (weaker): borrow/rent a Pixel 10/11 or Galaxy S26+ |
| 8 MUST features, solo | Shared domain + fixtures written once; strict build order; cut line in §10 |
| On-device accuracy too low for a task | Accuracy-first rule (§7): device output becomes a draft + cloud re-check; never auto-accept medicine or names |
| Indonesian not supported by Nano | Language-neutral extraction on the device (measured, set E) + deterministic keyword icons offline + cloud Gemini when online |
| Rules risk from separate translation AI | Removed (D11): all translation via Firebase AI Logic (Nano offline, Gemini cloud) |
| ONLY_ON_DEVICE silently goes to the cloud (SDK#10635) | Our own guard + a test |
| Wrong announcement summary | Original text always shown; staff approve; never auto-broadcast |
| Medical liability | Drafts only; pharmacist/medic confirm; no dosing; "not for clinical use" demo list |
| Hazard map ≠ live conditions | Show source + date; never say "safe"; volunteer pins override |
| Similar to Koe Relay / FloodLine | Phones + hub, roster + medicine + Field Check, deaf/elderly-first, no phone needed for evacuees |
| Judges' laptops can't run Nano | Hosted demo works via cloud fallback with clear badges; README explains the on-device requirements |
| Free-tier quota / EEA access for judges | Test the hosted demo early; device-first design reduces cloud calls |

## 12. Decisions log (2026-11-21) and Day-1 checks
**Decided:**
| # | Decision |
|---|---|
| D1 | Name **Nobody Missing**, mascot **Kit** |
| D2 | **Global product**, demo story in **Aceh** (Indonesian + English; other languages via region packs) |
| D3 | **Native Android (main) + native web hub** (TypeScript PWA in Chrome). Submit under **Android**; multi-platform for the Grand Prize. No wrappers. iOS not planned |
| D4 | **Keep all 8 MUST features** (F1, F2, F3, F4, F8, F9, F11, F12-lite) with the cut line in §10 |
| D5 | Video = one deaf + elderly story (§8) |
| D6 | **Family search is public** during an emergency, scoped to the shelters and the surrounding area, minimal fields (name, age band, shelter, status). Small safeguard: a person can ask staff to hide their entry |
| D7 | In-shelter sync = **one hub laptop + QR courier**, driven by the **region pack** (§6.13): each device knows its region and shelter list in advance |
| D8 | Hazard data = **global only**: **HAND 30 m** as the main flood layer; **Copernicus DEM 30 m** downloaded **per region, only when needed** |
| D9 | **One repo**, clean architecture with clear patterns (§6.12) |
| D10 | **Accuracy is priority #1**: lightweight only where it doesn't cost accuracy (§6.14) |
| D11 | **All AI, including translation, goes through Firebase AI Logic.** No separate translation engines (ML Kit Translation / Chrome Translator / Apple Translation). Offline translation = Gemini Nano via Firebase AI Logic (Indonesian measured, shown as a draft); online = cloud Gemini; safety net = original text + deterministic keyword icons (§2.8) |
| D13 | **One app, three views:** Staff app (5 tabs), Big Screen (one auto-rotating page), Public pages via QR (**News, Map, Help, Family**, built for deaf + elderly). Evacuees don't need the app (§6.16) |
| D14 | **No login screens.** Join by scanning the organizer's QR (role inside); silent Firebase Anonymous Auth when online for security rules (§6.16) |
| D15 | **Shelter Wi-Fi:** the hub laptop runs a local hotspot + small local web server for the offline Public pages, map tiles and staff-phone sync; fallback = text QR cards (§6.16) |
| D12 | **Hardware target:** laptop with **16 GB+ RAM (32 GB better), 4+ cores, or a GPU with >4 GB VRAM; 30 GB+ free disk; Chrome ≥148**. Phone: **Pixel 10** (Google says the Prompt API performs best on the Pixel 10 series); Pixel 11 or Galaxy S26+ also OK. If no Nano phone is available, borrow or rent one: without it, Android shows cloud fallback only |

**Day-1 checks (the builder agent must run these first and record the results here):**
1. Hardware matches D12; confirm Nano downloads and runs on both devices.
2. Nano on Indonesian roster sheets (10 samples): accuracy vs cloud.
3. Android offline speech recognition for `id-ID`: installed/downloadable?
4. Keyword-icon list for Indonesian announcements: build and test on set B.
5. (Removed: no forum question needed, D11.)
6. HAND 30 m + Copernicus DEM clip for the demo town: sizes and value sanity check.
7. All ⚠️ items in this file that the build touches: verify or replace.

**D16 (from the builder brief, 2026-11-21):** tests first with shared golden fixtures (Vitest + JUnit); ports & adapters with AdapterFactory / InferenceRouterFactory / FeatureFactory; all AI only via Firebase AI Logic through the InferenceRouter; every AI output schema-validated; React without `useEffect`.

**Agent decisions (builder agent, autonomous run on 2026-11-21).** Format: what · why · alternatives.
- Agent decision (2026-11-21): moved PRODUCT.md into the git repo root (`nobodymissing/`) · it was outside the git checkout, so nothing tracked it · alternatives: keep it outside and copy it in (two copies drift).
- Agent decision (2026-11-21): work on branch `day1-skeleton` with local commits only · don't commit to the default branch, and never push · alternatives: commit on main.
- Agent decision (2026-11-21): a hand-written JSON Schema subset validator (type, properties, required, items, enum, min/max, maxLength, maxItems) on both platforms, instead of ajv · the same `schemaValidation.json` fixtures must give identical results on web and Android, and no ajv equivalent exists in Kotlin · alternatives: ajv on web + networknt on Android (behaviour may drift).
- Agent decision (2026-11-21): all Remote Config defaults in ONE file, `shared/rules/defaults.json` (plus `keywords.json`), bundled on web and copied into Android assets from `shared/` · one source of truth, works offline · alternatives: one file per rule (more files, same data).
- Agent decision (2026-11-21): HAND buckets are `<2 → 3`, `<5 → 2`, `≤10 → 1`, else 0 (the §6.11c reference code); the thresholds are in `fieldPriority.handDeep/Medium/Shallow` · the §6.11c table boundaries were ambiguous, so I followed the code · alternatives: inclusive lower bounds.
- Agent decision (2026-11-21): the Field Check sorts by tier, then score (high first), then distance (unknown last) · matches §6.11c "within a tier: by score, then distance" · alternatives: distance before score.
- Agent decision (2026-11-21): the dedupe rule (F3) is: names equal after normalising (lowercase, no accents, honorifics removed: pak/bapak/bu/ibu/mr/mrs/ms/san/sdr; old spelling oe→u, dj→j, tj→c, sj→sy, nj→ny; word order ignored), OR edit distance ≤1 when both names have ≥5 letters; ages within ±2 years or unknown · deterministic and explainable (§6.14) · alternatives: phonetic codes (Soundex does not fit Indonesian), LLM matching (not exact).
- Agent decision (2026-11-21): family search matches whole name tokens ("hasan" does not match "Hasanah") · fewer false hits on a public page · alternatives: prefix match.
- Agent decision (2026-11-21): the public search index stores the age as its decade (age band), not the exact age · minimal fields (D6) · alternatives: exact age.
- Agent decision (2026-11-21): the demo drug list has 40 drugs, not ~100 · enough for every matcher case (look-alikes, mg/mcg, brands); clearly labelled "not for clinical use" · alternatives: spend the time on a 100-drug list.
- Agent decision (2026-11-21): the drug matcher returns "matched" only for an exact generic or brand name; fuzzy hits (distance ≤ max(1, len/4)) are "candidates"; look-alike warnings for other generics within distance 3; `needsConfirm` is always true · §6.9 (never auto-accept) · alternatives: a similarity threshold that auto-accepts.
- Agent decision (2026-11-21): only medicines confirmed by a person (self, family, staff or medic) enter the Medicine Round; unconfirmed AI readings show as review cards · "staff confirmation on medicine and names" must never be cut · alternatives: show unconfirmed ones in grey.
- Agent decision (2026-11-21): supply requests use qty = number of people short (a count), not tablets · a dose calculation is forbidden (§6.9) · alternatives: compute tablet counts (not allowed).
- Agent decision (2026-11-21): router policy: a sensitive task with no device asks staff (never silent); private mode blocks any cloud use; a task where the device is measured more than `accuracyBarPoints` worse goes to the cloud first, or becomes a device draft when offline. `announcement_id` uses PLACEHOLDER accuracies 70 vs 92 until set E is measured (PENDING HUMAN) · §6.3 + §7 · alternatives: hard-coded if-chains.
- Agent decision (2026-11-21): on web, the privacy guard checks `LanguageModel.availability()` before every call, AND a post-check discards any cloud answer to an `ONLY_ON_DEVICE` request · the post-check alone can't stop data leaving, so the pre-check is the real guard · alternatives: trust the SDK mode.
- Agent decision (2026-11-21): web cloud calls use `responseJsonSchema` (plain JSON Schema), and on-device calls use `responseConstraint` with the same schema · same output shape (§2.7) · alternatives: convert to the Firebase `Schema` builder.
- Agent decision (2026-11-21): on Android, the cloud call uses `responseMimeType=application/json` and puts the JSON schema in the prompt, instead of the typed `JsonSchema` builder · building typed schemas from our JSON was too slow to do in this run; our validator still rejects anything wrong · alternatives: write a JSON → `JsonSchema` converter (TODO).
- Agent decision (2026-11-21): a fourth environment, `demo` (fakes with canned AI replies + real connectivity check), is used when no Firebase config exists, and is labelled "DEMO: canned AI, no Firebase" in the status bar · lets judges and CI open the app without secrets · alternatives: crash without config.
- Agent decision (2026-11-21): Android uses the `test` fakes when there is no `google-services.json` (the Google services plugin is applied only if the file exists) · builds without secrets · alternatives: commit a placeholder json (risky).
- Agent decision (2026-11-21): toolchain: Vite 8 + React 19 + Vitest 5 + TypeScript 7; Android AGP 9.5.2 (built-in Kotlin) + Compose plugin 2.4.21 + Compose BOM 2026.11.01, compileSdk 37.2 (required by the Compose BOM), minSdk 26, JDK 21; firebase-ai 18.2.0 + firebase-ai-ondevice 16.0.0-beta08 (API checked against the AAR: `OnDeviceConfig(mode=…)`, `FirebaseAIOnDevice.checkStatus()`, `response.inferenceSource`); firebase JS 13.4.0 · latest stable at build time · alternatives: older pinned versions.
- Agent decision (2026-11-21): offline map = a shelter-centred SVG drawn from the region-pack grid (HAND shading, shelters, places), not MapLibre + PMTiles · the "offline map view" is third on the cut line; the list + higher-place card are kept · alternatives: MapLibre GL JS with service-worker tile caching (later).
- Agent decision (2026-11-21): the demo region pack `aceh-demo.json` is a SYNTHETIC 4×4 HAND grid and 3 invented shelters, clearly labelled · real HAND/DEM clipping needs GDAL and downloads (PENDING HUMAN) · alternatives: none in time.
- Agent decision (2026-11-21): photos for batch mode are kept in memory only (never persisted to localStorage, never synced) · localStorage quota and privacy · alternatives: IndexedDB (TODO: needed so photos survive a restart).
- Agent decision (2026-11-21): the Shelter Wi-Fi hub (`hub/server.mjs`) is a dependency-free Node server: GET/POST `/api/news` (POST from localhost only), POST `/api/help` from phones · D15, cheapest version · alternatives: a separate Express server.
- Agent decision (2026-11-21): announcement language = the staff UI language toggle (EN/ID), not automatic detection · no extra unmeasured logic · alternatives: language detection via Nano.
- Agent decision (2026-11-21): F5, F6, F7, F10, BLE beacon and speech input are not built in this run · the cut line in §10 · alternatives: none.
- Agent decision (2026-11-21): Android Field Check uses the demo field entries and hazard only (no GPS report flow yet); Android Announce shows keyword cards + the reach list, without AI cards yet · time; Android is the submission platform, so these are next · alternatives: none.
- Agent decision (2026-11-21): created GCP project `nobody-missing-hk26` with the active gcloud account (therush515@gmail.com, as the user allowed), no billing linked; enabled the Firebase, Firestore, Identity Toolkit, App Check, Remote Config, Rules, Firebase AI Logic, Generative Language, reCAPTCHA Enterprise and API Keys APIs; Firestore `(default)` in asia-southeast2 (Jakarta, nearest to Aceh); `firestore.rules` released through the Rules API; reCAPTCHA Enterprise web key `6LeR8SouAAAAAP1Ldis_FXoQJddFgosdFzRUsW1k` (a public site key) for localhost + web.app · free tier only · alternatives: link the free-trial billing (not needed yet). BLOCKED: `addFirebase` returns 403, most likely because the Firebase Terms have not been accepted for this account (PENDING HUMAN). Hosting is NOT deployed (public).
- Agent decision (2026-11-21): switched to the user-created Firebase project `nobodymissing` (number 446708893365); the earlier `nobody-missing-hk26` GCP project is unused (the user may delete it). Done via the CLI/REST: APIs enabled; web app + Android app (`org.nobodymissing`) registered; `web/.env.local` + `android/app/google-services.json` written locally (gitignored); Firestore `(default)` in asia-southeast2; `firestore.rules` released; reCAPTCHA Enterprise key `6LdIICsuAAAAABUGsuyzrhaw54fBuKiOJpSWZEab` registered as the web App Check provider; 1 web App Check debug token registered; Remote Config v1 published (`rules`, `promptPack`). Not possible from the CLI without billing: anonymous Auth (Identity Platform initialize needs billing) and AI Logic Gemini Developer API setup ("genai config not found") · PENDING HUMAN (2 console clicks).
- Agent decision (2026-11-21): commits from now on carry no Claude co-author trailer (user request).
- Agent decision (2026-11-21): console steps done in Chrome at the user's request: Anonymous Auth enabled; AI Logic set up with the **Gemini Developer API (no-cost, Spark, no billing)**, accepting the Gemini API Additional Terms (user said yes); App Check is **enforced** for AI Logic (web registered, reCAPTCHA Enterprise); optional AI monitoring left OFF (has its own pricing). Android App Check: Play Integrity registered + debug SHA-1/SHA-256 added; its debug token is PENDING HUMAN (needs a phone; the emulator image would not fit on the disk, 5.6 GB free).
- Agent decision (2026-11-21): device timeout 8 s → **15 s** (Remote Config v2) · measured Nano on this laptop (Chrome 156, 16 GB): about 3.7 s warm with a small schema, about 9–10 s with the full card schema, 23 s cold · alternatives: shrink the schema (TODO: measure).
- Agent decision (2026-11-21): AI card times are re-parsed by the deterministic time parser ("8 am" → 08:00) · Nano formats times freely · alternatives: trust the model.
- Agent decision (2026-11-21): backup cloud model on failure (primary `gemini-3.8-flash` → `gemini-3.5-flash-lite`) · `gemini-3.8-flash` returned 500 "high demand" during testing · alternatives: fail the task.
- Agent decision (2026-11-21): prompts now say "copy the place exactly as written, never translate it" (prompt pack v2, Remote Config v3) · eval set B: cloud-lite scored only 42% on Indonesian places because it translated "aula" → "hall" · alternatives: score translated places as correct (that would break §2.8).
- Agent decision (2026-11-21): Firestore rules fixed: the shelter wildcard no longer covers `members` · a rules unit test showed any member could add or promote other devices · alternatives: none (security bug).
- Agent decision (2026-11-21): batch mode keeps an approved photo in memory when there is no signal, and reads it when signal returns, the device starts charging, or staff tap "Process now" · Playwright found the photo was dropped · alternatives: IndexedDB (TODO: survive restarts).
- Agent decision (2026-11-21): all demo-only material lives in its own folders: `shared/demo/` (synthetic shelter, synthetic region pack, demo drug list), `web/src/demo/` (canned AI replies, demo seed) and the Android `org.nobodymissing.demo` package. A real shelter starts EMPTY; the demo loads in demo mode or from Settings → "Load demo shelter" · user request: keep demo apart from real code · alternatives: seed always (bundles fake people into a real deployment).
- Agent decision (2026-11-21): the Android staff app mirrors web: the same 5 tabs + ⚙️, Kit status bar (vector converted from the same SVG), the 2×3 Today tile grid, the same badges, a review screen with the photo on top + ✓ Confirm, Settings (Shelter-ready model download, language, private mode, Signal Scout, QR courier with zxing, join QRs, load demo), a full-screen Scout alert with vibration; UI strings come from `shared/i18n/{en,id}.json` on both platforms (key + placeholder parity is tested) · user request: the same app on laptop and phone · alternatives: platform-specific screens (they drift).
- Agent decision (2026-11-21): design revamp (user request), web + Android identical:
  - **Kit redrawn as a proper character**: a blue British Shorthair firefighter, drawn around the breed traits (broad round head wider than tall, heavy jowls, small ears set wide and low, huge round copper eyes, puffy whisker pads with a small chin, cobby body, short thick tail), with a fire helmet and gold shield, a turnout coat with reflective tape, holding a lantern in both paws, and a curled tail. One definition (`shared/mascot/kit.json`), drawn natively (SVG on web, Compose Canvas on Android) and animated the same way. Moods: idle = breathing, blink, tail sway; device = amber lantern glow; cloud = blue lantern, walking; alert = lantern flashing red/blue, hop; sleep = eyes closed, lantern off. (A fire-truck version was tried and dropped: the user wants the cat.)
  - **Original line-icon set** (`shared/icons/icons.json`, 50 icons); **all emoji removed** from both apps.
  - **Flat design**: no gradient backgrounds and no coloured side bars (user feedback).
  - **Motion**: floating dark nav with a spring indicator, screens and list items rising in with a stagger, a scanning line over the photo while the AI reads, a pulsing can't-miss icon, and Kit popping up with ripples for the Scout alert.
  - **Big Screen**: dark theme with a slide progress bar.
  - `prefers-reduced-motion` turns the motion off on web.

**Measured, eval set B (24 synthetic announcements, 12 EN + 12 ID), 2026-11-21, prompt pack v1:**
| Path | EN all-correct | ID all-correct | Notes |
|---|---|---|---|
| Keywords (no AI) | 50% | 75% | icon 92/100, time 92/100; misses English place phrases |
| Cloud `gemini-3.5-flash-lite` | 92% | 42% | ID failures = translated place names (fixed in prompt v2, re-run pending); median 1.9 s / 1.5 s |
| Device (Nano, Chrome 156) | pending | pending | the run was cut by a browser-extension disconnect; re-run with `await (await import('/src/eval/runB.ts')).runB()` in a foreground tab |
`routerPolicy.announcement_id` stays at PLACEHOLDER 70/92 until the device numbers exist.

**Day-1 check results (2026-11-21, builder agent):**
1. Laptop: ✅ Gemini Nano `available` in Chrome 156 (text + image) and answers through Firebase AI Logic (`inferenceSource: on_device`), shown with the 🏮 Device badge in the app. Pixel: **PENDING HUMAN**.
2. Nano on 10 Indonesian roster sheets: **PENDING HUMAN** (needs photos + Nano). Use `tools/eval/score.mjs`.
3. Android offline speech `id-ID`: **PENDING HUMAN** (device). Speech input is cut-line item #1.
4. Keyword-icon list: built (`shared/rules/keywords.json`) and tested on 9 fixture announcements (web + Android); the full set B (50) is still to write.
5. (removed)
6. HAND 30 m + DEM clip: **PENDING HUMAN** (GDAL + downloads, see `tools/region-pack/README.md`). The demo pack is synthetic.
7. ⚠️ items verified in code: Android API names (from the AAR), web `inferenceSource`, `responseConstraint`, `responseJsonSchema`. Not verified: Remote Config / App Check in a real Firebase project (PENDING HUMAN: console).

---

- Agent decision (2026-11-22): three new hybrid features chosen with the user from the research note (docs/research/hybrid-ai.md): **1 Medicine check** (`med_check`, cloud only, schema `medCheck`), **2 Find it open** (`find_open`, cloud only, **Grounding with Google Maps**), **5 Ask Kit** (`ask_kit`, device first, schema `askKit`) · why: today the cloud was only a fallback; these give the cloud jobs the device cannot do (live places, drug knowledge) while the person stays on the phone, and give evacuees their first AI · alternatives: re-check unclear rows, translation, SMS, health tallies (kept for later).
- Agent decision (2026-11-22): **privacy split is code, not prompt**: `medCheckInput` builds the cloud request from a whitelist (drug name + strength); `coarseLocation` rounds to `findOpen.precisionDecimals` (2 ≈ 1 km) before Maps grounding; both pinned by shared fixtures (`shared/fixtures/hybrid.json`, Vitest + JUnit) · alternatives: "please don't use names" in the prompt (not enforceable).
- Agent decision (2026-11-22): **Find it open shows only places Google Maps itself returned** (grounding chunks matched by name, `groundedPlaces`); no grounding chunks = nothing shown. Results are a list with "Open in Google Maps" links, not map pins (Place IDs need the Places API for coordinates) · alternatives: trust the model's JSON (could invent places), Places API (extra key/billing).
- Agent decision (2026-11-22): medicine check output is **for a pharmacist**: never auto-accepted, never a dose; the cloud generic is cross-checked against the offline drug list (`crossCheck`: agree / mismatch / cloud_only) and a mismatch is shown, not hidden. Offline it waits and runs when the Scout says signal is back · alternatives: auto-fill the generic (unsafe).
- Agent decision (2026-11-22): **Ask Kit answers only from today's shown notices**: the answer must cite a real `cardId` (`askKitAnswer`), otherwise "Please ask a volunteer"; the question is never stored or synced; device first (`prefer_on_device`) · alternatives: open chat (hallucination risk for elderly evacuees).
- Agent decision (2026-11-22): Remote Config merge is now key-by-key for `routerPolicy` and prompts (web + Android), so the older published template cannot drop the new tasks · alternatives: republish the template (still worth doing; PENDING HUMAN).
- Agent decision (2026-11-22): UI rebuilt to docs/design (calm authority: navy + amber + ivory, Manrope + Atkinson Hyperlegible, bundled for offline). Web: evacuee `#/public` (Home, Help, Map, Family, Ask Kit) + staff `#/staff` (Now, Map, Camera centre, Announce, People, Care card, Round, Settings) + `#/big`. Android: same staff screens + an evacuee mode (Settings → Evacuees; long-press Kit to return); Android camera stays the system camera (TakePicturePreview) + photo picker · alternatives: CameraX viewfinder (more code, later).

- Agent decision (2026-11-22): **maps use OpenStreetMap raster tiles** (`rules.map.tiles`, attribution shown) drawn with our own Web Mercator code on web and Android (same `fitView` rule), under the hazard overlay and markers. Tiles a device has seen are cached (service worker on web, disk on Android) so the map still shows offline; no bulk download (OSM tile policy) · alternatives: Leaflet/MapLibre (a library per platform, different looks), Google Maps SDK (key + billing).
- Agent decision (2026-11-22): **"I need help" in the evacuee's own words** (`help_triage`, sensitive = on this phone only): Nano picks the kind, urgency and a short summary for staff; with no device model, keyword rules + the original words; **urgency can only go up** (`rules.urgentWords` beats a calm AI answer); never sent until the evacuee taps Send; urgent requests go to the top of staff Now · alternatives: cloud triage (health words leave the phone), no AI (tiles only).
- Agent decision (2026-11-22): **family group by QR**: each phone shows `NMF1:familyId:memberId:shelter:status:at:name`; scanning merges (newer wins) and works with no signal face to face; online, members sync via `families/{f-20hex}/members/{id}` (rules: minimal fields only, random id is the key) · alternatives: accounts/phone numbers (not for elderly evacuees), cloud-only matching.

- Agent decision (2026-11-22): **the demo region is now real data**: `tools/region-pack/build.mjs` builds Kuala Simpang (Aceh Tamiang, flooded Nov 2025) from Global HAND 30 m (COG window, no GDAL) + OSM shelter candidates; only the demo shelter SD3, its in-shelter places and the people are synthetic. Map shading = HAND bands, blurred so it reads as terrain · alternatives: keep the invented town (looked fake on real tiles), live flood layers (need signal).
- Agent decision (2026-11-22): **family QR v2 carries the whole group** (max 8, `NMF2:fam|id,shelter,status,at,name|…`) so news hops phone to phone offline; online, **gossip sync** writes everyone a phone knows into every family id it has seen, so groups that met through different relatives join up · alternatives: one member per QR (stranded relatives), a central family account.
- Agent decision (2026-11-22): **hybrid family search**: the description ("my grandmother Nurhayati, about 80, from Sukajadi") is understood on the device (`family_query`, sensitive; no model -> `parseLookFor` rules); online, only the name goes to the cloud for Indonesian/Acehnese spellings (`name_variants`; offline -> `nameRules`); matching is deterministic (`searchVariants`, exact first, age ±10) and labelled "possible match", confirm with staff · alternatives: let the model match people (unsafe), exact search only (misses spellings).
- Agent decision (2026-11-22): Android scans family QR codes with the Google Play services code scanner (live camera UI, no camera permission, module bundled at install) · alternatives: still photo + zxing (too low resolution), CameraX + analyzer (more code).

- Agent decision (2026-11-22): **product focus = the first hours** (user direction): get out, get to a higher place, get registered. Hybrid AI per step:
- Agent decision (2026-11-22): **Ask Kit "where do I go?"** is answered from the offline flood map (`whereWords` rule, shared fixture `whereToGo`), instantly and with no model, with Show the way + I need pickup. Kit only reads notices, so before this the most important question got "ask a volunteer" after up to 15 s of on-device wait with no feedback. Other questions still go device -> cloud, now with a visible "Kit is thinking" line. Demo homes loaded on a phone before the real-data move are re-placed on startup (demo ids only).
  - **Go now** (`warning_plain`, device first): any warning text (SMS, WhatsApp, loudspeaker words) becomes plain words; `goNowWords` in the original can only raise the level to go-now. Online: `warning_latest` (**Grounding with Google Search**, rounded location) fetches the latest official BMKG/BPBD warning with source links; a newer answer never lowers a go-now.
  - **Show the way** (no AI, offline): GPS + compass arrow to the nearest higher place from the HAND pack; vibrates when off course; honest note "follow roads, avoid shaded low ground" (no routing graph offline).
  - **I need pickup** (`pickup_request`, sensitive, device only; keywords otherwise; mobility and needs only go up): sent on tap through the outbox to `shelters/{nearest}/pickups/{id}` (rules: anyone may create minimal fields, only members read), plus a QR a passing volunteer scans. Staff see it as a Field Check entry ranked by the same priority rules (PICKUP FIRST).
  - **Check-in** (`checkin_parse`, sensitive, device only; keyword flags never dropped): one spoken sentence or the evacuee's self-chosen card QR -> confirmed person with flags -> tell-in-person list and medicine photo prompt follow automatically. Staff tab "Check-in" replaces "Announce" (Announce moves to the Now header).
  · alternatives: keep the in-shelter features first (judged less impactful by the user), cloud-only warnings (useless without signal).

## 13. Local files (research archive)
- `SUMMARY.md`: hackathon summary. `rules.txt`, `index.txt`, `details_faqs.txt`, `resources.txt`, `upd_*.txt`, `forum/`: raw hackathon pages.
- `docs/`: saved Firebase AI Logic docs (hybrid, web get-started, models, production checklist, FriendlyMeals sample, agent skills).
- `RESEARCH_usecases.md`: platform capability table and hybrid patterns.
- `RESEARCH_ideas_world.md`: 9 world-problem options with evidence.
- `examples/RANKED.md`: all 366 competitor projects, ranked by criteria. `examples/README.md`: the first 59.
- `chromeai2025/`: the 2025 Chrome Built-in AI Challenge pages and winner write-ups.

## 14. Key sources
- Hackathon: https://firebasehybridaihackathon.devpost.com/ (overview, rules, FAQ, resources, updates)
- Firebase hybrid docs: https://firebase.google.com/docs/ai-logic/hybrid, https://firebase.google.com/docs/ai-logic/hybrid/ios/get-started
- Chrome Prompt API: https://developer.chrome.com/docs/ai/prompt-api
- Android hybrid blog: https://android-developers.googleblog.com/2026/04/Hybrid-inference-and-new-AI-models-are-coming-to-Android.html
- Past winners: https://developer.chrome.com/blog/ai-challenge-winners-2025, https://developer.chrome.com/blog/ai-challenge-winners
- Noto: https://www.nippon.com/en/articles/-/16395, https://mdpi.com/2313-576X/10/3/62, https://nippon.com/en/japan-data/h02234, https://www.japantimes.co.jp/news/2024/12/27/japan/noto-quake-elderly-evacuees, https://www.frontiersin.org/journals/medicine/articles/10.3389/fmed.2024.1420495/full
- Tohoku elderly: https://www.undrr.org/news/japan-quake-took-toll-women-and-elderly, https://www.japantimes.co.jp/news/2012/05/12/national/elderly-accounted-for-90-of-indirect-311-death-toll-government-says/
- Evacuation plans: https://japantimes.co.jp/news/2024/03/01/japan/society/evacuation-plans-for-people-in-need
- Deaf: https://eos.org/articles/improving-earthquake-early-warning-access-for-the-deaf-community, https://www.jfd.or.jp/en/docs/2011/wfd2011-eq-presentation-jfd.pdf, https://link.springer.com/article/10.1007/s11069-024-06719-6, https://theconversation.com/we-always-come-last-deaf-people-are-vulnerable-to-disaster-risk-but-excluded-from-preparedness-158213
- Smartphone use: https://www.soumu.go.jp/johotsusintokei/whitepaper/eng/WP2025/pdf/11-chap1_sec1.pdf
- Networks: https://www.itu.int/en/ITU-D/Emergency-Telecommunications/Documents/ET-OnlineToolkit/bp/The%20Role%20of%20Mobiles%20in%20Disasters%20and%20Emergencies%20–%20‘05.pdf, https://journals.sagepub.com/doi/10.1177/1550147719829960
- Warning systems: https://www.wikipedia.org/wiki/J-Alert, https://group.ntt/en/disaster/service/171.html, https://blog.google/products-and-platforms/platforms/android/android-earthquake-alerts-expansion-2026/
- Maui: https://www.npr.org/2024/04/17/1245396324/maui-fire-report-communications-breakdown ; Black Summer: https://www.infrastructure.gov.au/what-we-do/phone/communications-emergencies/findings-2019-20-bushfire-season ; Tonga: https://www.npr.org/2022/02/22/1082483555/tongas-internet-restored-5-weeks-after-big-eruption
- Misinformation: https://www.thejakartapost.com/news/2020/01/08/indonesia-awash-with-disinformation-following-disasters.html
- Sumatra 2025 floods: https://www.nu.or.id/nasional/korban-banjir-dan-longsor-aceh-sumatra-capai-1-204-jiwa-bnpb-kebut-huntara-AyRvs, https://www.idxchannel.com/news/pengungsi-korban-bencana-sumatera-capai-242174-jiwa-paling-banyak-di-aceh/all, https://kabar24.bisnis.com/read/20251208/15/1934903/update-bencana-di-sumatra-aceh-921-meninggal-dunia-392-hilang, https://nu.or.id/nasional/bnpb-korban-bencana-di-sumatra-tembus-1-059-jiwa-pengungsi-didominasi-perempuan-QkSfU
- Aceh signal/power 2025: https://regional.kompas.com/read/2025/11/30/162633778/kisah-pengungsi-banjir-aceh-4-hari-tanpa-sinyal-listrik-padam-logistik, https://regional.kompas.com/read/2025/11/29/111541378/listrik-padam-sinyal-lumpuh-bupati-aceh-utara-telepon-wamenkomdigi-minta, https://regional.kompas.com/read/2025/12/18/075010178/21-malam-tanpa-cahaya-pengungsi-banjir-aceh-utara-bertahan-dengan-lampu, https://www.tempo.co/ekonomi/banjir-bandang-robohkan-5-tower-pln-di-aceh-2093673, https://makassar.tribunnews.com/news/1820919/korban-banjir-di-aceh-kami-mulai-kelaparan-akses-jalan-putus-listrik-padam
- Evacuee data problems: https://www.cnnindonesia.com/ekonomi/20260128200353-92-1322142/bps-temukan-banyak-data-ganda-korban-bencana-sumatra, https://nu.or.id/daerah/data-pengungsi-banjir-aceh-tak-sinkron-klaim-pemulihan-hampir-100-persen-dipertanyakan-qhVzM, https://www.metrotvnews.com/read/NQAC0LaD-mendagri-instruksikan-pemda-bentuk-tim-rapikan-data-warga-terdampak-bencana, https://indonesia.unfpa.org/sites/default/files/pub-pdf/bnpb_juklak_no_9_2023_data_terpilah.pdf, https://kemenkopmk.go.id/node/1625, https://kalbar.antaranews.com/berita/368828/penyaluran-logistik-tidak-merata-karena-pengungsi-bodong
- Aceh Nov 2026: https://nasional.kompas.com/read/2026/11/15/13384171/bnpb-banjir-aceh-tamiang-terjang-8-kecamatan-5088-warga-mengungsi, https://humas.acehprov.go.id/sekda-aceh-paparkan-kondisi-bencana-dan-capaian-pemulihan-17-kabupaten-kota-masih-dalam-penanganan, https://masakini.co/2026/11/20/pemulihan-bencana-belum-tuntas-banjir-kembali-berdampak-pada-25-ribu-warga-aceh, https://tirto.id/banjir-melanda-5-wilayah-aceh-21-ribu-jiwa-terdampak-idSW
- BNPB 2026: https://www.bnpb.go.id/berita/bnpb-catat-bencana-hidrometeorologi-dominasi-kejadian-sepanjang-2026, https://sulsel.fajar.co.id/2026/11/20/bnpb-peringatkan-potensi-banjir-dan-longsor-di-sejumlah-daerah-jawa-tengah-jadi-perhatian/
- Karhutla 2026: https://databoks.katadata.co.id/demografi/statistik/c41d1243366cd51/arah-angin-asap-karhutla-sepekan-titik-api-terpanas-843-mw-di-kalimantan, https://www.kompas.id/artikel/15-bulan-berlangsung-area-karhutla-kalimantan-nyaris-seluas-kota-surabaya, https://katadata.co.id/berita/nasional/6a893956e1d50/kemenkes-tiga-juta-orang-lebih-terpapar-asap-kasus-ispa-melonjak-di-kalimantan, https://regional.kompas.com/read/2026/08/23/103526478/asap-karhutla-belum-reda-pemkot-pontianak-berlakukan-belajar-daring-untuk, https://ecoedu.id/artikel/asap-yang-menyeberang-lagi-anatomi-karhutla-2026-dan-utang-lama-tata-kelola-gambut-686
- PetaBencana: https://info.petabencana.id/about/, https://publicadministration.un.org/unpsa/database/Winners/2019-Winners/PetaBencanaid
- Languages: https://developers.google.com/ml-kit/genai/prompt/android/get-started, https://www.apple.com/id/newsroom/2026/06/apple-unveils-next-generation-of-apple-intelligence-siri-ai-and-more/
