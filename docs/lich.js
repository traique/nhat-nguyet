// Âm lịch, can chi, ngày lễ và ngày nghỉ cho trang giới thiệu: bản JavaScript của
// app/src/main/java/vn/nhatnguyet/lunar/Lunar.java và DaysOff.java (thuật toán của Hồ Ngọc Đức,
// múi giờ +7). Sửa ở Java thì sửa cả ở đây; lich.test.mjs đối chiếu hai bên.

const TZ = 7;

export const CAN = ['Giáp', 'Ất', 'Bính', 'Đinh', 'Mậu', 'Kỷ', 'Canh', 'Tân', 'Nhâm', 'Quý'];
export const CHI = ['Tý', 'Sửu', 'Dần', 'Mão', 'Thìn', 'Tỵ', 'Ngọ', 'Mùi', 'Thân', 'Dậu', 'Tuất', 'Hợi'];
export const MONTH_NAME = ['', 'Giêng', 'Hai', 'Ba', 'Tư', 'Năm', 'Sáu', 'Bảy', 'Tám', 'Chín', 'Mười', 'Một', 'Chạp'];
export const WEEKDAYS = ['Chủ nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];
const TIET_KHI = [
  'Xuân phân', 'Thanh minh', 'Cốc vũ', 'Lập hạ', 'Tiểu mãn', 'Mang chủng', 'Hạ chí', 'Tiểu thử', 'Đại thử',
  'Lập thu', 'Xử thử', 'Bạch lộ', 'Thu phân', 'Hàn lộ', 'Sương giáng', 'Lập đông', 'Tiểu tuyết', 'Đại tuyết',
  'Đông chí', 'Tiểu hàn', 'Đại hàn', 'Lập xuân', 'Vũ thủy', 'Kinh trập',
];
const GOOD_STAR = [true, true, false, false, true, true, false, true, false, false, true, false];
const GIO_HD = ['110100101100', '001101001011', '110011010010', '101100110100', '001011001101', '010010110011'];

// [tên, âm lịch?, ngày, tháng, nghỉ?, từ năm]
const HOLIDAYS = [
  ['Tết Dương lịch', false, 1, 1, true],
  ['Valentine', false, 14, 2, false],
  ['Quốc tế Phụ nữ', false, 8, 3, false],
  ['Giải phóng miền Nam', false, 30, 4, true],
  ['Quốc tế Lao động', false, 1, 5, true],
  ['Quốc tế Thiếu nhi', false, 1, 6, false],
  ['Quốc khánh', false, 2, 9, true],
  ['Phụ nữ Việt Nam', false, 20, 10, false],
  ['Nhà giáo Việt Nam', false, 20, 11, false],
  ['Ngày Văn hóa Việt Nam', false, 24, 11, true, 2026],
  ['Giáng sinh', false, 25, 12, false],
  ['Tết Nguyên đán', true, 1, 1, true],
  ['Mùng 2 Tết', true, 2, 1, true],
  ['Mùng 3 Tết', true, 3, 1, true],
  ['Rằm tháng Giêng', true, 15, 1, false],
  ['Tết Hàn thực', true, 3, 3, false],
  ['Giỗ Tổ Hùng Vương', true, 10, 3, true],
  ['Lễ Phật đản', true, 15, 4, false],
  ['Tết Đoan ngọ', true, 5, 5, false],
  ['Lễ Vu Lan', true, 15, 7, false],
  ['Tết Trung thu', true, 15, 8, false],
  ['Ông Công ông Táo', true, 23, 12, false],
  ['Giao thừa', true, 30, 12, false],
].map(([name, lunar, day, month, major, since = 0]) => ({ name, lunar, day, month, major, since }));

export const SAYINGS = [
  'Uống nước nhớ nguồn.',
  'Có công mài sắt, có ngày nên kim.',
  'Ăn quả nhớ kẻ trồng cây.',
  'Một cây làm chẳng nên non, ba cây chụm lại nên hòn núi cao.',
  'Lá lành đùm lá rách.',
  'Đi một ngày đàng, học một sàng khôn.',
  'Gần mực thì đen, gần đèn thì rạng.',
  'Tốt gỗ hơn tốt nước sơn.',
  'Học thầy không tày học bạn.',
  'Nước chảy đá mòn.',
  'Thất bại là mẹ thành công.',
  'Có chí thì nên.',
  'Bầu ơi thương lấy bí cùng, tuy rằng khác giống nhưng chung một giàn.',
  'Lời nói chẳng mất tiền mua, lựa lời mà nói cho vừa lòng nhau.',
  'Chớ thấy sóng cả mà ngã tay chèo.',
  'Kiến tha lâu cũng đầy tổ.',
  'Không thầy đố mày làm nên.',
  'Công cha như núi Thái Sơn, nghĩa mẹ như nước trong nguồn chảy ra.',
  'Đói cho sạch, rách cho thơm.',
  'Thương người như thể thương thân.',
  'Một miếng khi đói bằng một gói khi no.',
  'Ăn vóc học hay.',
  'Cẩn tắc vô áy náy.',
  'Năng nhặt chặt bị.',
  'Tích tiểu thành đại.',
  'Nhiễu điều phủ lấy giá gương, người trong một nước phải thương nhau cùng.',
  'Mưa dầm thấm lâu.',
  'Ăn có nhai, nói có nghĩ.',
  'Muốn sang thì bắc cầu kiều, muốn con hay chữ thì yêu lấy thầy.',
  'Chuồn chuồn bay thấp thì mưa, bay cao thì nắng, bay vừa thì râm.',
  'Một con ngựa đau, cả tàu bỏ cỏ.',
  'Chim có tổ, người có tông.',
];

const mod = (a, n) => ((a % n) + n) % n;
const isLeapYear = (y) => (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0;

export function jdFromDate(dd, mm, yy) {
  const a = Math.floor((14 - mm) / 12);
  const y = yy + 4800 - a;
  const m = mm + 12 * a - 3;
  let jd = dd + Math.floor((153 * m + 2) / 5) + 365 * y + Math.floor(y / 4) - Math.floor(y / 100) + Math.floor(y / 400) - 32045;
  if (jd < 2299161) jd = dd + Math.floor((153 * m + 2) / 5) + 365 * y + Math.floor(y / 4) - 32083;
  return jd;
}

/** @returns {[number, number, number]} ngày, tháng, năm dương lịch */
export function jdToDate(jd) {
  let b, c;
  if (jd > 2299160) {
    const a = jd + 32044;
    b = Math.floor((4 * a + 3) / 146097);
    c = a - Math.floor((b * 146097) / 4);
  } else {
    b = 0;
    c = jd + 32082;
  }
  const d = Math.floor((4 * c + 3) / 1461);
  const e = c - Math.floor((1461 * d) / 4);
  const m = Math.floor((5 * e + 2) / 153);
  return [e - Math.floor((153 * m + 2) / 5) + 1, m + 3 - 12 * Math.floor(m / 10), b * 100 + d - 4800 + Math.floor(m / 10)];
}

function newMoonDay(k) {
  const T = k / 1236.85;
  const T2 = T * T;
  const T3 = T2 * T;
  const dr = Math.PI / 180;
  let jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * T2 - 0.000000155 * T3;
  jd1 += 0.00033 * Math.sin((166.56 + 132.87 * T - 0.009173 * T2) * dr);
  const M = 359.2242 + 29.10535608 * k - 0.0000333 * T2 - 0.00000347 * T3;
  const Mpr = 306.0253 + 385.81691806 * k + 0.0107306 * T2 + 0.00001236 * T3;
  const F = 21.2964 + 390.67050646 * k - 0.0016528 * T2 - 0.00000239 * T3;
  let C1 = (0.1734 - 0.000393 * T) * Math.sin(M * dr) + 0.0021 * Math.sin(2 * dr * M);
  C1 = C1 - 0.4068 * Math.sin(Mpr * dr) + 0.0161 * Math.sin(dr * 2 * Mpr);
  C1 = C1 - 0.0004 * Math.sin(dr * 3 * Mpr);
  C1 = C1 + 0.0104 * Math.sin(dr * 2 * F) - 0.0051 * Math.sin(dr * (M + Mpr));
  C1 = C1 - 0.0074 * Math.sin(dr * (M - Mpr)) + 0.0004 * Math.sin(dr * (2 * F + M));
  C1 = C1 - 0.0004 * Math.sin(dr * (2 * F - M)) - 0.0006 * Math.sin(dr * (2 * F + Mpr));
  C1 = C1 + 0.001 * Math.sin(dr * (2 * F - Mpr)) + 0.0005 * Math.sin(dr * (2 * Mpr + M));
  const deltat = T < -11
    ? 0.001 + 0.000839 * T + 0.0002261 * T2 - 0.00000845 * T3 - 0.000000081 * T * T3
    : -0.000278 + 0.000265 * T + 0.000262 * T2;
  return Math.floor(jd1 + C1 - deltat + 0.5 + TZ / 24);
}

function sunLongitudeRad(jdn) {
  const T = (jdn - 2451545.5 - TZ / 24) / 36525;
  const T2 = T * T;
  const dr = Math.PI / 180;
  const M = 357.5291 + 35999.0503 * T - 0.0001559 * T2 - 0.00000048 * T * T2;
  const L0 = 280.46645 + 36000.76983 * T + 0.0003032 * T2;
  let DL = (1.9146 - 0.004817 * T - 0.000014 * T2) * Math.sin(dr * M);
  DL = DL + (0.019993 - 0.000101 * T) * Math.sin(dr * 2 * M) + 0.00029 * Math.sin(dr * 3 * M);
  const omega = 125.04 - 1934.136 * T;
  let L = (L0 + DL - 0.00569 - 0.00478 * Math.sin(omega * dr)) * dr;
  return L - Math.PI * 2 * Math.floor(L / (Math.PI * 2));
}

const sunSector12 = (jdn) => Math.floor((sunLongitudeRad(jdn) / Math.PI) * 6);

function lunarMonth11(yy) {
  const k = Math.floor((jdFromDate(31, 12, yy) - 2415021) / 29.530588853);
  const nm = newMoonDay(k);
  return sunSector12(nm) >= 9 ? newMoonDay(k - 1) : nm;
}

function leapMonthOffset(a11) {
  const k = Math.floor((a11 - 2415021.076998695) / 29.530588853 + 0.5);
  let last;
  let i = 1;
  let arc = sunSector12(newMoonDay(k + i));
  do {
    last = arc;
    i++;
    arc = sunSector12(newMoonDay(k + i));
  } while (arc !== last && i < 14);
  return i - 1;
}

/** @returns {{day: number, month: number, year: number, leap: boolean, jd: number}} */
export function solarToLunar(dd, mm, yy) {
  const dayNumber = jdFromDate(dd, mm, yy);
  let k = Math.floor((dayNumber - 2415021.076998695) / 29.530588853);
  let monthStart = newMoonDay(k + 1);
  while (monthStart > dayNumber) monthStart = newMoonDay(k--);
  let a11 = lunarMonth11(yy);
  let b11 = a11;
  let year;
  if (a11 >= monthStart) {
    year = yy;
    a11 = lunarMonth11(yy - 1);
  } else {
    year = yy + 1;
    b11 = lunarMonth11(yy + 1);
  }
  const diff = Math.floor((monthStart - a11) / 29);
  let leap = false;
  let month = diff + 11;
  if (b11 - a11 > 365) {
    const leapMonthDiff = leapMonthOffset(a11);
    if (diff >= leapMonthDiff) {
      month = diff + 10;
      if (diff === leapMonthDiff) leap = true;
    }
  }
  if (month > 12) month -= 12;
  if (month >= 11 && diff < 4) year -= 1;
  return { day: dayNumber - monthStart + 1, month, year, leap, jd: dayNumber };
}

/** @returns {[number, number, number] | null} ngày dương, hoặc null nếu tháng nhuận không có */
export function lunarToSolar(lunarDay, lunarMonth, lunarYear, lunarLeap) {
  const [a11, b11] = lunarMonth < 11
    ? [lunarMonth11(lunarYear - 1), lunarMonth11(lunarYear)]
    : [lunarMonth11(lunarYear), lunarMonth11(lunarYear + 1)];
  const k = Math.floor(0.5 + (a11 - 2415021.076998695) / 29.530588853);
  let off = mod(lunarMonth - 11, 12);
  if (b11 - a11 > 365) {
    const leapOff = leapMonthOffset(a11);
    const leapMonth = mod(leapOff - 2, 12);
    if (lunarLeap && lunarMonth !== leapMonth) return null;
    if (lunarLeap || off >= leapOff) off += 1;
  } else if (lunarLeap) {
    return null;
  }
  return jdToDate(newMoonDay(k + off) + lunarDay - 1);
}

function lunarMonthLength(ld) {
  const start = ld.jd - ld.day + 1;
  const k = Math.floor((start - 2415021.076998695) / 29.530588853 + 0.5);
  let next = newMoonDay(k + 1);
  if (next - start < 25) next = newMoonDay(k + 2);
  else if (next - start > 31) next = newMoonDay(k);
  return next - start;
}

const solarTermIndex = (jd) => Math.floor((sunLongitudeRad(jd + 1) / Math.PI) * 12);
export const solarTerm = (jd) => TIET_KHI[solarTermIndex(jd)];
export const solarTermStarts = (jd) => solarTermIndex(jd) !== solarTermIndex(jd - 1);

export const yearCanChi = (y) => `${CAN[mod(y + 6, 10)]} ${CHI[mod(y + 8, 12)]}`;
export const dayCanChi = (jd) => `${CAN[mod(jd + 9, 10)]} ${CHI[mod(jd + 1, 12)]}`;
export const isHoangDao = (jd, lunarMonth) => GOOD_STAR[mod(mod(jd + 1, 12) - ((lunarMonth - 1) % 6) * 2, 12)];

/** Tên các giờ hoàng đạo của ngày, vd ["Dần", "Mão", …]. */
export const gioHoangDao = (jd) => [...GIO_HD[mod(jd + 1, 12) % 6]].flatMap((bit, i) => (bit === '1' ? [CHI[i]] : []));

function holidayIsOn(h, dd, mm, ld) {
  const year = jdToDate(ld.jd)[2];
  if (h.since > 0 && year < h.since) return false;
  if (!h.lunar) {
    if (h.day === dd && h.month === mm) return true;
    return h.day === 29 && h.month === 2 && dd === 28 && mm === 2 && !isLeapYear(year);
  }
  if (ld.leap || ld.month !== h.month) return false;
  return ld.day === h.day || (h.day === 30 && ld.day === 29 && lunarMonthLength(ld) === 29);
}

export const holidayOn = (dd, mm, ld) => HOLIDAYS.find((h) => holidayIsOn(h, dd, mm, ld)) ?? null;

const PERSONAL_LINE = /^(\d{1,2})\s*[/.-]\s*(\d{1,2})(?:\s*[/.-]\s*\d{2,4})?\s*(?:[([]?(âm lịch|am lich|âm|am|âl|al)[)\]]?(?=[\s:,.–-]|$))?\s*[:,.–-]?\s*(.*)$/iu;
const SOLAR_DAYS = [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

/** Đọc một dòng ngày riêng như PersonalDays.java: "15/8 âm Giỗ ông nội", "20/11 Sinh nhật mẹ"; null nếu sai. */
export function parsePersonal(line) {
  const m = PERSONAL_LINE.exec(line.trim());
  if (!m) return null;
  const day = Number(m[1]);
  const month = Number(m[2]);
  const lunar = m[3] !== undefined;
  const name = m[4].trim();
  if (month < 1 || month > 12 || day < 1 || day > (lunar ? 30 : SOLAR_DAYS[month - 1])) return null;
  if (!name || /^[/.\-\d]/.test(name)) return null;
  return { name, lunar, day, month, major: true, since: 0 };
}

/** Lần tới (tính cả hôm nay) của một ngày lễ hoặc ngày riêng trong vòng 400 ngày: {days, solar} hoặc null. */
export function nextDate(h, dd, mm, yy) {
  const jd0 = jdFromDate(dd, mm, yy);
  for (let i = 0; i <= 400; i++) {
    const s = jdToDate(jd0 + i);
    if (holidayIsOn(h, s[0], s[1], solarToLunar(...s))) return { days: i, solar: s };
  }
  return null;
}

/** Các ngày lễ sắp tới (không gồm hôm nay) trong vòng 400 ngày. */
export function upcoming(dd, mm, yy, count, majorOnly) {
  const out = [];
  const jd0 = jdFromDate(dd, mm, yy);
  for (let i = 1; i <= 400 && out.length < count; i++) {
    const s = jdToDate(jd0 + i);
    const h = holidayOn(s[0], s[1], solarToLunar(...s));
    if (h && (!majorOnly || h.major)) out.push({ holiday: h, days: i, solar: s });
  }
  return out;
}

export function daysToTet(dd, mm, yy) {
  const ld = solarToLunar(dd, mm, yy);
  const target = ld.month === 1 && ld.day === 1 && !ld.leap ? ld.year : ld.year + 1;
  return jdFromDate(...lunarToSolar(1, 1, target, false)) - jdFromDate(dd, mm, yy);
}

// Ngày nghỉ hưởng lương theo Bộ luật Lao động 2019 (Điều 111, 112), Nghị quyết 28/2026/QH16
// và lịch nghỉ khối nhà nước đã công bố; xem DaysOff.java.
// [năm, số ngày nghỉ cuối năm âm, ngày liền kề Quốc khánh trong tháng 9, nghỉ hoán đổi, làm bù]
const OFFICIAL = [
  [2026, 1, 1, [[31, 8]], [[22, 8]]],
  [2027, 2, 3, [], []],
];
const TET = 'Nghỉ Tết Nguyên đán';
const CULTURE_DAY = 'Ngày Văn hóa Việt Nam';
const yearCache = new Map();

/** Thứ Bảy hoặc Chủ nhật (số ngày Julius chia 7 dư 0 là thứ Hai). */
const weekend = (jd) => jd % 7 === 5 || jd % 7 === 6;

function tetDay(y) {
  return jdFromDate(...lunarToSolar(1, 1, y, false));
}

function legal(y, tetDaysBefore, nationalDayExtra) {
  const m = new Map([[jdFromDate(1, 1, y), 'Tết Dương lịch']]);
  if (tetDaysBefore > 0) {
    const m1 = tetDay(y);
    for (let i = -tetDaysBefore; i < 5 - tetDaysBefore; i++) m.set(m1 + i, TET);
  }
  const g = lunarToSolar(10, 3, y, false);
  if (g) m.set(jdFromDate(...g), 'Giỗ Tổ Hùng Vương');
  m.set(jdFromDate(30, 4, y), 'Giải phóng miền Nam');
  m.set(jdFromDate(1, 5, y), 'Quốc tế Lao động');
  m.set(jdFromDate(2, 9, y), 'Quốc khánh');
  if (nationalDayExtra > 0) m.set(jdFromDate(nationalDayExtra, 9, y), 'Quốc khánh');
  if (y >= 2026) m.set(jdFromDate(24, 11, y), CULTURE_DAY);
  return m;
}

/** Nghỉ bù (khoản 3 Điều 111): lễ trùng cuối tuần thì nghỉ ngày làm việc kế tiếp chưa nghỉ, xét theo thứ tự ngày. */
function withCompensation(days, noComp) {
  const out = new Map(days);
  for (const [jd, name] of [...days].sort((a, b) => a[0] - b[0])) {
    if (!weekend(jd) || noComp.has(jd) || name === CULTURE_DAY) continue;
    let k = jd + 1;
    while (weekend(k) || out.has(k)) k++;
    out.set(k, 'Nghỉ bù ' + (name.startsWith('Nghỉ ') ? name.slice(5) : name));
  }
  return out;
}

function daysOffYear(y) {
  if (yearCache.has(y)) return yearCache.get(y);
  const official = OFFICIAL.find((o) => o[0] === y);
  let off;
  let work = new Set();
  if (official) {
    const [, tetBefore, extra, swapOff, swapWork] = official;
    off = withCompensation(legal(y, tetBefore, extra), new Set());
    for (const [d, m] of swapOff) off.set(jdFromDate(d, m, y), 'Nghỉ hoán đổi');
    work = new Set(swapWork.map(([d, m]) => jdFromDate(d, m, y)));
  } else {
    // Năm chưa có lịch: chỉ ngày chắc chắn nghỉ, kể cả ngày cuối năm, mùng 1 và mùng 2 Tết.
    const fixed = legal(y, 0, 0);
    const m1 = tetDay(y);
    const noComp = new Set([jdFromDate(2, 9, y)]);
    for (let i = -1; i <= 1; i++) {
      fixed.set(m1 + i, TET);
      noComp.add(m1 + i);
    }
    off = withCompensation(fixed, noComp);
  }
  const entry = { off, work };
  yearCache.set(y, entry);
  return entry;
}

/** Tên ngày nghỉ hưởng lương (lễ, nghỉ Tết, nghỉ bù, hoán đổi) hoặc null. */
export const dayOffName = (d, m, y) => daysOffYear(y).off.get(jdFromDate(d, m, y)) ?? null;
export const isMakeUpWorkday = (d, m, y) => daysOffYear(y).work.has(jdFromDate(d, m, y));

/** Tô đỏ: ngày lễ được nghỉ, ngày nghỉ theo lịch nhà nước, hoặc Chủ nhật. */
export function isRed(d, m, y, ld) {
  const h = holidayOn(d, m, ld);
  return (h !== null && h.major) || dayOffName(d, m, y) !== null || ld.jd % 7 === 6;
}

function countdown({ holiday, days, solar }) {
  const name = holiday.name;
  const short = name.startsWith('Ngày ') ? name.slice(5) : name;
  if (days === 1) return ['Ngày mai: ' + name, 'Mai: ' + short, 'Ngày mai có lễ'];
  const date = `${String(solar[0]).padStart(2, '0')}/${String(solar[1]).padStart(2, '0')}`;
  return [`Còn ${days} ngày: ${name}`, `Còn ${days} ngày: ${short}`, `${date}: ${short}`, `${date} · còn ${days} ngày`];
}

/** Dòng sự kiện dưới ngày âm, như trên bảng lịch của app: {lunar: tô màu âm lịch?, text: [dài … ngắn]} hoặc null. */
export function dayNote(d, m, y) {
  const ld = solarToLunar(d, m, y);
  const hol = holidayOn(d, m, ld);
  if (hol) return { lunar: true, text: ['Hôm nay: ' + hol.name, hol.name] };
  const off = dayOffName(d, m, y);
  if (off) return { lunar: true, text: ['Hôm nay: ' + off, off] };
  if (ld.day === 1 && !ld.leap) return { lunar: true, text: ['Mùng 1 tháng ' + MONTH_NAME[ld.month]] };
  if (ld.day === 15) return { lunar: true, text: ['Rằm tháng ' + MONTH_NAME[ld.month]] };
  if (solarTermStarts(ld.jd)) {
    return { lunar: false, text: ['Bắt đầu tiết ' + solarTerm(ld.jd), 'Tiết ' + solarTerm(ld.jd)] };
  }
  const next = upcoming(d, m, y, 1, true)[0];
  if (next && next.days <= 45) return { lunar: false, text: countdown(next) };
  const tet = daysToTet(d, m, y);
  if (tet > 0 && tet <= 60) return { lunar: false, text: [`Còn ${tet} ngày đến Tết`, `Tết: còn ${tet} ngày`] };
  return null;
}

const WEATHER = {
  0: 'Trời quang', 1: 'Ít mây', 2: 'Có mây', 3: 'Nhiều mây, u ám', 45: 'Sương mù', 48: 'Sương mù đóng băng',
  51: 'Mưa phùn nhẹ', 53: 'Mưa phùn', 55: 'Mưa phùn dày', 56: 'Mưa phùn lạnh', 57: 'Mưa phùn lạnh dày',
  61: 'Mưa nhỏ', 63: 'Mưa vừa', 65: 'Mưa to', 66: 'Mưa lạnh', 67: 'Mưa lạnh to', 80: 'Mưa rào nhẹ',
  81: 'Mưa rào', 82: 'Mưa rào rất to', 95: 'Dông', 96: 'Dông kèm mưa đá', 99: 'Dông mạnh kèm mưa đá',
};

export const weatherText = (code) => WEATHER[code] ?? 'Không rõ';
