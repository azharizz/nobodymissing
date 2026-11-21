import { expect, test } from '@playwright/test';

test.beforeEach(async ({ page }) => { await page.goto('/'); await page.evaluate(() => localStorage.clear()); });

test('Today shows the shelter tiles and demo label', async ({ page }) => {
  await page.goto('/#/staff/today');
  await expect(page.getByText('DEMO: canned AI, no Firebase')).toBeVisible();
  await expect(page.getByText("Can't-miss")).toBeVisible();
  await expect(page.getByText('P1 visits')).toBeVisible();
});

test('roster scan with no on-device AI: asks staff first; with no signal the photo waits in the batch', async ({ page }) => {
  await page.goto('/#/staff/people');
  await page.locator('input[type=file]').setInputFiles({ name: 'list.png', mimeType: 'image/png', buffer: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==', 'base64') });
  await expect(page.getByText(/Send this to the cloud\?/)).toBeVisible();
  await page.getByRole('button', { name: /Send to cloud/ }).click();
  await expect(page.getByText(/Kit will send this when the signal comes back/)).toBeVisible();
  await expect(page.getByText(/1 photo\(s\) waiting/)).toBeVisible();
});

test('announcement: keyword card offline-safe, approve, Big Screen + public News show it', async ({ page }) => {
  await page.goto('/#/staff/announce');
  await page.locator('textarea').fill('Makan siang jam 12.00 di aula');
  await page.getByRole('button', { name: /Make cards/ }).click();
  await expect(page.locator('.kcard-place', { hasText: 'aula' }).first()).toBeVisible();
  await expect(page.getByText(/Tell in person: \d+ left/)).toBeVisible();
  await page.getByRole('button', { name: /^Show$/ }).click();
  await page.goto('/#/public/news');
  await expect(page.getByText(/12:00/).first()).toBeVisible();
  await expect(page.getByRole('button', { name: 'Read aloud' })).toBeVisible();
});

test('medicine: can-miss alerts pinned and supply request queued', async ({ page }) => {
  await page.goto('/#/staff/medicine');
  await expect(page.locator('.card.danger', { hasText: 'Pak Hasan Basri' })).toContainText("Can't-miss");
  await page.getByRole('button', { name: /Request supplies/ }).click();
  await expect(page.locator('.chip', { hasText: 'Waiting' })).toContainText('2');
});

test('field check lists P1 first with reasons and a higher place', async ({ page }) => {
  await page.goto('/#/staff/field');
  const first = page.locator('.card.visit').first();
  await expect(first.locator('.tier')).toHaveText('P1');
  await expect(first.locator('.why .pill-tag').first()).toBeVisible();
  await expect(page.getByText(/Higher place:/).first()).toBeVisible();
});

test('public help request reaches the staff Today tab', async ({ page }) => {
  await page.goto('/#/public/help');
  await page.getByPlaceholder('Bed 12 / Area B').fill('Bed 7');
  await page.getByRole('button', { name: 'medicine' }).click();
  await page.goto('/#/staff/today');
  await expect(page.locator('.card', { hasText: 'Bed 7' })).toContainText('medicine');
});
