// UI strings (plain i18n files, not AI). English required by the rules; Indonesian for the Aceh demo.
const en = {
  today: 'Today', people: 'People', medicine: 'Medicine', announce: 'Announce', field: 'Field', settings: 'Settings',
  device: 'Device', cloud: 'Cloud', waiting: 'Waiting', review: 'Needs review', keywords: 'Keywords (no AI)',
  scanList: 'Scan list', confirm: 'Confirm', fix: 'Fix', given: 'Given', refused: 'Refused', missing: 'Missing',
  show: 'Show', print: 'Print', report: 'Report', news: 'News', map: 'Map', help: 'Help', family: 'Family',
  noSignal: 'Kit is holding the lantern: working on this device.', syncing: 'Kit is carrying your list to the other shelters.',
  airplaneTip: "No signal here. Turn on airplane mode to save battery. I'll keep working, and the Scout phone will tell us when signal is back.",
  signalBack: 'Signal is back here! Bring devices to sync.', allChecked: 'All checked. Take a breath.', noMeds: 'No one waiting for medicine now.',
  askCloud: "On-device AI isn't ready on this device. Send this to the cloud? Private data will leave the device.", sendCloud: 'Send to cloud', keepLocal: 'Keep on device',
  queued: 'Kit will send this when the signal comes back.', invalid: 'The AI answer did not pass the check. Please type it in or try again.',
  blocked: 'Private mode: nothing leaves this device.', search: 'Search', notFound: 'No match in the shelters we can reach.',
  thisShelterOnly: 'Offline: this shelter only', allShelters: 'All shelters (online)', requestSupplies: 'Request supplies', cantMiss: "Can't-miss",
};
type Dict = typeof en;
const id: Dict = {
  today: 'Hari ini', people: 'Orang', medicine: 'Obat', announce: 'Umumkan', field: 'Lapangan', settings: 'Pengaturan',
  device: 'Perangkat', cloud: 'Cloud', waiting: 'Menunggu', review: 'Perlu dicek', keywords: 'Kata kunci (tanpa AI)',
  scanList: 'Pindai daftar', confirm: 'Benar', fix: 'Perbaiki', given: 'Diberikan', refused: 'Menolak', missing: 'Obat habis',
  show: 'Tampilkan', print: 'Cetak', report: 'Lapor', news: 'Berita', map: 'Peta', help: 'Bantuan', family: 'Keluarga',
  noSignal: 'Kit memegang lentera: bekerja di perangkat ini.', syncing: 'Kit membawa daftar ke posko lain.',
  airplaneTip: 'Tidak ada sinyal. Nyalakan mode pesawat untuk hemat baterai. Kit tetap bekerja, HP Pengintai akan memberi tahu saat sinyal kembali.',
  signalBack: 'Sinyal kembali di sini! Bawa perangkat untuk sinkron.', allChecked: 'Semua sudah dicek. Tarik napas.', noMeds: 'Tidak ada yang menunggu obat.',
  askCloud: 'AI di perangkat belum siap. Kirim ke cloud? Data pribadi akan keluar dari perangkat.', sendCloud: 'Kirim ke cloud', keepLocal: 'Tetap di perangkat',
  queued: 'Kit akan mengirim saat sinyal kembali.', invalid: 'Jawaban AI tidak lolos pemeriksaan. Ketik manual atau coba lagi.',
  blocked: 'Mode privat: tidak ada yang keluar dari perangkat.', search: 'Cari', notFound: 'Tidak ditemukan di posko yang terjangkau.',
  thisShelterOnly: 'Offline: posko ini saja', allShelters: 'Semua posko (online)', requestSupplies: 'Minta pasokan', cantMiss: 'Tidak boleh terlewat',
};
export const dict = { en, id };
export type T = Dict;
