// Đối chiếu lich.js với bản Java của app trên từng ngày 2020–2040.
// Chạy: LICH_DUMP=/tmp/lich.txt ./gradlew :app:testGithubDebugUnitTest --tests '*LichDumpTest' && node docs/lich.test.mjs /tmp/lich.txt
import { readFileSync } from 'node:fs';
import * as L from './lich.js';

const lines = readFileSync(process.argv[2], 'utf8').trim().split('\n');
let mismatches = 0;
for (const expected of lines) {
  const date = expected.split('|')[0];
  const [d, m, y] = date.split('/').map(Number);
  const ld = L.solarToLunar(d, m, y);
  const holiday = L.holidayOn(d, m, ld);
  const note = L.dayNote(d, m, y);
  const actual = [
    date,
    `${ld.day}/${ld.month}/${ld.year}${ld.leap ? 'L' : ''}`,
    L.dayCanChi(ld.jd),
    L.yearCanChi(ld.year),
    holiday ? holiday.name : '',
    L.dayOffName(d, m, y) ?? '',
    L.isMakeUpWorkday(d, m, y) ? 1 : 0,
    L.isRed(d, m, y, ld) ? 1 : 0,
    L.isHoangDao(ld.jd, ld.month),
    L.gioHoangDao(ld.jd).join(','),
    note ? note.text.join('~') : '',
  ].join('|');
  if (actual !== expected && mismatches++ < 5) console.error(`Java: ${expected}\nJS:   ${actual}`);
}
if (lines.length < 7000 || mismatches > 0) {
  console.error(`${mismatches} ngày lệch trên ${lines.length} ngày`);
  process.exit(1);
}
console.log(`lich.js khớp bản Java trên ${lines.length} ngày`);
