import { expect, test, type Page } from '@playwright/test';

test.beforeEach(async ({ page }) => { await page.goto('/'); await page.evaluate(() => localStorage.clear()); });
const PNG = { name: 'list.png', mimeType: 'image/png', buffer: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==', 'base64') };
/** Demo Scout: real connectivity checks until the signal is "back" (the full-screen alert). */
async function signalBack(page: Page) {
  await page.goto('/#/staff/now');
  for (let i = 0; i < 5 && !(await page.locator('.scout').count()); i++) { await page.locator('.signal').click(); await page.waitForTimeout(700); }
  await page.getByRole('button', { name: 'Got it' }).click();
}

test('Now: the three numbers and the can\'t-miss task', async ({ page }) => {
  await page.goto('/#/staff/now');
  await expect(page.locator('.stat', { hasText: "Can't-miss" })).toContainText('2');
  await expect(page.locator('.task', { hasText: 'Pak Hasan Basri' }).first()).toContainText("Can't-miss");
});

test('camera LIST with no on-device AI: asks staff first; with no signal the photo waits in the batch', async ({ page }) => {
  await page.goto('/#/staff/camera');
  await page.locator('.shutter input[type=file], .thumbs input[type=file]').first().setInputFiles(PNG);
  await expect(page.getByText(/Send this to the cloud\?/)).toBeVisible();
  await page.getByRole('button', { name: /Send to cloud/ }).click();
  await expect(page.getByText(/Kit will send this when the signal comes back/)).toBeVisible();
  await page.getByRole('button', { name: 'Now' }).click(); // photos stay in memory only, so no reload
  await expect(page.getByText(/1 photo\(s\) waiting/)).toBeVisible();
});

test('announcement: keyword card offline-safe, show everyone, evacuee home + Big Screen show it', async ({ page }) => {
  await page.goto('/#/staff/announce');
  await page.locator('textarea').fill('Makan siang jam 12.00 di aula');
  await page.getByRole('button', { name: /Make cards/ }).click();
  await expect(page.locator('.mini-news')).toContainText('aula');
  await expect(page.getByText(/\d+ to tell in person/)).toBeVisible();
  await page.getByRole('button', { name: /Show everyone/ }).click();
  await page.goto('/#/public');
  await expect(page.locator('.news')).toContainText('12:00');
  await expect(page.getByRole('button', { name: 'Read aloud' })).toBeVisible();
  await page.goto('/#/big');
  await expect(page.locator('.tv-news')).toContainText('12:00');
});

test('medicine round: can\'t-miss pinned and supply request queued', async ({ page }) => {
  await page.goto('/#/staff/round');
  await expect(page.locator('.task.urgent', { hasText: 'Pak Hasan Basri' })).toContainText("Can't-miss");
  await page.getByRole('button', { name: /Request supplies/ }).click();
  await page.goto('/#/staff/now');
  await expect(page.locator('.signal')).toContainText('2 waiting');
});

test('map: visit-first person with reasons and a higher place', async ({ page }) => {
  await page.goto('/#/staff/map');
  await expect(page.locator('.sheet .prio')).toHaveText('VISIT FIRST');
  await expect(page.locator('.sheet .risk .pill').first()).toBeVisible();
  await expect(page.getByText(/Higher place:/).first()).toBeVisible();
});

test('evacuee help request reaches the staff Now list', async ({ page }) => {
  await page.goto('/#/public/help');
  await page.getByPlaceholder('Bed 12 · Hall A').fill('Bed 7');
  await page.getByRole('button', { name: 'Medicine' }).click();
  await expect(page.getByText(/A volunteer is coming to Bed 7/)).toBeVisible();
  await page.goto('/#/staff/now');
  await expect(page.locator('.task', { hasText: 'Bed 7' })).toContainText('Medicine');
});

test('medicine check + find it open: wait offline, run with names kept on the phone when signal returns', async ({ page }) => {
  await page.goto('/#/staff/person/p-hasan');
  await page.getByRole('button', { name: /Check medicines/ }).click();
  await expect(page.getByText(/Waiting for signal/)).toBeVisible();
  await signalBack(page);
  await page.goto('/#/staff/person/p-hasan');
  await expect(page.getByText('Matches the offline list').first()).toBeVisible();
  await expect(page.getByText(/For a pharmacist to confirm/)).toBeVisible();
  await page.getByRole('button', { name: 'Pharmacy' }).click();
  await expect(page.locator('.place', { hasText: 'Apotek Sehat Keluarga' })).toContainText('Open now');
  await expect(page.getByRole('link', { name: 'Open in Google Maps' }).first()).toBeVisible();
});

test('Ask Kit: notices, greeting, where to go, urgent help with the words carried over', async ({ page }) => {
  await page.goto('/#/staff/announce');
  await page.locator('textarea').fill('Lunch at 12.00 in the hall');
  await page.getByRole('button', { name: /Make cards/ }).click();
  await page.getByRole('button', { name: /Show everyone/ }).click();
  await signalBack(page);
  await page.goto('/#/public/ask');
  await page.locator('textarea').fill('When is lunch?');
  await page.getByRole('button', { name: 'Ask', exact: true }).click();
  await expect(page.locator('.answer')).toContainText("From today's notice");
  await page.locator('textarea').fill('When does the bus leave?');
  await page.getByRole('button', { name: 'Ask', exact: true }).click();
  await expect(page.locator('.answer')).toContainText("Kit isn't sure");
  // Greeting: the app's own words + shortcuts, never model small talk.
  await page.locator('textarea').fill('hi bro');
  await page.getByRole('button', { name: 'Ask', exact: true }).click();
  await expect(page.locator('.answer')).toContainText("I'm Kit");
  // Where to go: offline flood map, instantly, with the way.
  await page.locator('textarea').fill('Hello, where i should go');
  await page.getByRole('button', { name: 'Ask', exact: true }).click();
  await expect(page.locator('.answer')).toContainText('higher than');
  await expect(page.locator('.answer').getByRole('button', { name: 'Show the way' })).toBeVisible();
  // Urgent words always mean help, and the person's words travel with them.
  await page.locator('textarea').fill('tolong ayah saya sesak napas');
  await page.getByRole('button', { name: 'Ask', exact: true }).click();
  await expect(page.locator('.answer')).toContainText('This sounds urgent');
  await page.locator('.answer').getByRole('button', { name: 'I need help' }).click();
  await expect(page.locator('textarea')).toHaveValue('tolong ayah saya sesak napas');
});

test('I need help in my own words: urgent goes to the top of staff Now', async ({ page }) => {
  await page.goto('/#/public/help');
  await page.getByPlaceholder('Bed 12 · Hall A').fill('Bed 4');
  await page.getByLabel('Tell Kit what you need').fill('bapak saya nyeri dada sejak pagi');
  await page.getByRole('button', { name: 'Ask', exact: true }).click();
  await expect(page.locator('.answer')).toContainText('Urgent');
  await page.getByRole('button', { name: /Send to staff/ }).click();
  await expect(page.getByText(/volunteer is coming now/)).toBeVisible();
  await page.goto('/#/staff/now');
  await expect(page.locator('.task').first()).toContainText('Urgent');
  await expect(page.locator('.task').first()).toContainText('Bed 4');
});

test('family group: link by QR code, see each other with status', async ({ page }) => {
  await page.goto('/#/public/family');
  await page.getByLabel('Your name').fill('Cut Nurlaila');
  await page.getByRole('button', { name: /Show my QR/ }).click();
  await expect(page.locator('.qr-show img')).toBeVisible();
  await page.getByRole('button', { name: /Scan family QR/ }).click();
  await page.getByLabel('Or paste the code').fill(`NMF2:f-8c1e2b7a9d4f4a51b2c3|m9,MSJ1,need_help,${Date.now()},Teuku Hasballah|m8,SD1,safe,${Date.now()},M. Rizki`);
  await page.getByRole('button', { name: 'Import' }).click();
  await expect(page.getByText(/Linked with Teuku Hasballah, M. Rizki/)).toBeVisible();
  await expect(page.locator('.family-hit', { hasText: 'Teuku Hasballah' })).toContainText('Needs help');
  await page.getByRole('button', { name: /Scan family QR/ }).click(); // scanning closes after a successful link
  await page.getByLabel('Or paste the code').fill('not a family code');
  await page.getByRole('button', { name: 'Import' }).click();
  await expect(page.getByText(/not a family QR/)).toBeVisible();
});

test('family search: describe the person; spelling variants find them', async ({ page }) => {
  await page.goto('/#/staff/people');
  await page.goto('/#/public/family');
  await page.getByLabel('Name, or describe who you are looking for').fill('My grandfather Hasan Basri, about 80');
  await page.getByRole('button', { name: 'Search', exact: true }).click();
  await expect(page.getByText(/Looking for: Hasan Basri/)).toBeVisible();
  await expect(page.locator('.family-hit', { hasText: 'Pak Hasan Basri' })).toContainText('Same name');
});

test('first hours: warning in plain words, the higher place, pickup reaches the staff map, check-in at the door', async ({ page }) => {
  await page.goto('/#/public');
  await page.getByRole('button', { name: /Go to safety/ }).click();
  await page.getByRole('button', { name: /Use example warning/ }).click();
  await page.getByRole('button', { name: /Make it plain/ }).click();
  await expect(page.locator('.warn-hero')).toContainText('Flood warning');
  await expect(page.locator('.warn-hero.rose')).toBeVisible();                       // evakuasi segera -> go now
  await expect(page.locator('.dest')).toContainText('Masjid Jami');
  await page.getByRole('button', { name: /Show the way/ }).click();
  await expect(page.locator('.navbar')).toContainText(/Walk \d+ m/);
  await page.goto('/#/public/pickup');
  await page.getByLabel('I need pickup').fill('Ibu saya tidak bisa jalan, perlu insulin');
  await page.getByRole('button', { name: 'Ask', exact: true }).click();
  await expect(page.locator('.answer')).toContainText("Can't walk");
  await expect(page.locator('.answer')).toContainText('Insulin');
  await page.getByRole('button', { name: /Send pickup request/ }).click();
  await expect(page.getByText(/Kit sends it the moment any signal appears/)).toBeVisible();
  await expect(page.locator('.qr-show img')).toBeVisible();
  await page.goto('/#/public');
  await expect(page.locator('.alert-banner')).toContainText('Go now');
  await page.goto('/#/staff/checkin');
  await page.getByLabel('Check-in').fill('Hasan Basri, 82, tidak bisa dengar, pakai insulin');
  await page.getByRole('button', { name: 'Add', exact: true }).click();
  await expect(page.locator('.chip.toggle.on')).toHaveCount(2);                       // hard of hearing + needs meds (keywords)
  await expect(page.locator('.dup')).toContainText('Pak Hasan Basri, 82');             // demo already has him: maybe a duplicate
  await page.getByRole('button', { name: 'Not the same: add' }).click();
  await expect(page.getByText(/Added to "tell in person"/)).toBeVisible();
});
