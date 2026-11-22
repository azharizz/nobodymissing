import { expect, it } from 'vitest';
import { refreshDemoLocations } from './seed';
it('fixes stale demo home locations, leaves real entries alone', () => {
  const out = refreshDemoLocations([{ id: 'f-nur', location: { lat: 1, lng: 1 } }, { id: 'real-1', location: { lat: 2, lng: 2 } }] as any);
  expect(out[0].location).toEqual({ lat: 4.2815, lng: 98.0565 });
  expect(out[1].location).toEqual({ lat: 2, lng: 2 });
});
