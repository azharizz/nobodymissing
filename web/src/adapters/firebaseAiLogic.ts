// The only AI adapter: Firebase AI Logic hybrid inference (Gemini Nano on device, Gemini in the cloud).
import type { FirebaseApp } from 'firebase/app';
import { getAI, getGenerativeModel, GoogleAIBackend, InferenceSource, type Part } from 'firebase/ai';
import type { InferencePort, InferenceRequest } from '../ports';

export class FirebaseAiLogicInference implements InferencePort {
  private ai;
  constructor(app: FirebaseApp) { this.ai = getAI(app, { backend: new GoogleAIBackend() }); }

  /** Our own feature-detect (privacy guard): never trust ONLY_ON_DEVICE alone (firebase-js-sdk#10635). */
  async onDeviceStatus(): Promise<string> {
    const LM = (globalThis as any).LanguageModel;
    if (!LM?.availability) return 'no_api';
    try { return await LM.availability({ expectedInputs: [{ type: 'text' }, { type: 'image' }] }); } catch { return 'unavailable'; }
  }

  /** Must be called from a user gesture (Shelter-ready button). Starts the Nano download. */
  async prepareDevice(): Promise<string> {
    const LM = (globalThis as any).LanguageModel;
    if (!LM?.create) return 'no_api';
    try { (await LM.create({ expectedInputs: [{ type: 'text' }, { type: 'image' }] })).destroy?.(); } catch { /* status below tells the story */ }
    return this.onDeviceStatus();
  }

  async generate(r: InferenceRequest) {
    // A new model per call is cheap; the SDK creates a new on-device session per call anyway.
    // Grounding with Google Maps (cloud only): no JSON mode with tools; the text is parsed + schema-validated by the router.
    const maps = r.grounding === 'maps';
    const model = getGenerativeModel(this.ai, {
      mode: r.mode,
      inCloudParams: maps
        ? { model: r.model, tools: [{ googleMaps: {} }], toolConfig: r.location ? { retrievalConfig: { latLng: { latitude: r.location.lat, longitude: r.location.lng } } } : undefined }
        : { model: r.model, generationConfig: { responseMimeType: 'application/json', responseJsonSchema: r.schema as Record<string, unknown> } },
      onDeviceParams: {
        createOptions: { expectedInputs: r.image ? [{ type: 'text' }, { type: 'image' }] : [{ type: 'text' }] },
        promptOptions: { responseConstraint: r.schema },
      },
    });
    const parts: Part[] = [{ type: 'text', text: r.text ? `${r.prompt}\n\n${r.text}` : r.prompt }];
    if (r.image) parts.push({ type: 'inlineData', inlineData: { mimeType: r.image.mimeType, data: r.image.base64 } });
    const { response } = await model.generateContent(parts);
    const places = response.candidates?.[0]?.groundingMetadata?.groundingChunks?.flatMap(c => (c.maps ? [{ title: c.maps.title, uri: c.maps.uri, placeId: c.maps.placeId }] : []));
    return { raw: response.text(), source: response.inferenceSource === InferenceSource.ON_DEVICE ? 'device' as const : 'cloud' as const, places };
  }
}
