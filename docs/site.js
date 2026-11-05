import * as L from './lich.js';

const REPO = 'traique/nhat-nguyet';
const PLACE = { name: 'TP. Hồ Chí Minh', lat: 10.8231, lon: 106.6297 };
const pad = (n) => String(n).padStart(2, '0');
const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

// Lịch tính theo giờ Việt Nam, kể cả khi người xem ở múi giờ khác.
const vnParts = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: 'numeric', day: 'numeric',
  hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
});
function vnNow() {
  const p = Object.fromEntries(vnParts.formatToParts(new Date()).map((x) => [x.type, x.value]));
  return { d: +p.day, m: +p.month, y: +p.year, hh: +p.hour, mm: +p.minute };
}
const weekday = (jd) => L.WEEKDAYS[(jd + 1) % 7];
const lunarLabel = (ld) => `${ld.day}/${ld.month}${ld.leap ? ' nhuận' : ''}`;

/* ---------- nút tải: APK của bản phát hành mới nhất ---------- */

async function linkApk() {
  try {
    const res = await fetch(`https://api.github.com/repos/${REPO}/releases/latest`);
    if (!res.ok) return;
    const release = await res.json();
    const apk = release.assets.find((a) => /^NhatNguyet-[\d.]+\.apk$/.test(a.name));
    if (!apk) return;
    for (const a of $$('[data-apk], [data-apk-plain]')) a.href = apk.browser_download_url;
    const mb = (apk.size / 1e6).toFixed(1).replace('.', ',');
    const meta = $('[data-apk-meta]');
    if (meta) meta.textContent = `Bản ${release.tag_name.replace(/^v/, '')} · ${mb} MB · Android 4.0.3 trở lên`;
  } catch {
    // Mất mạng hoặc bị giới hạn lượt gọi: liên kết vẫn trỏ tới trang bản phát hành mới nhất.
  }
}

/* ---------- bảng lịch sống ---------- */

const ICONS = {
  sun: '<circle cx="20" cy="20" r="7.5" fill="#f2b33d"/><g stroke="#f2b33d" stroke-width="2.4" stroke-linecap="round"><path d="M20 4v4M20 32v4M4 20h4M32 20h4M8.7 8.7l2.8 2.8M28.5 28.5l2.8 2.8M8.7 31.3l2.8-2.8M28.5 11.5l2.8-2.8"/></g>',
  moon: '<path d="M25 7a13 13 0 1 0 8 21A11 11 0 0 1 25 7z" fill="#e6e9f0"/>',
  cloud: '<path d="M12 32h18a7 7 0 0 0 0-14 9 9 0 0 0-17-1 7.5 7.5 0 0 0-1 15z" fill="#e3e8ee"/>',
  rain: '<g stroke="#7fc4ff" stroke-width="2.4" stroke-linecap="round"><path d="M14 34l-2 4M21 34l-2 4M28 34l-2 4"/></g>',
  bolt: '<path d="M22 30l-5 8h5l-3 6 8-10h-5l3-4z" fill="#e9b95b"/>',
  fog: '<g stroke="#a9b6b2" stroke-width="2.4" stroke-linecap="round"><path d="M8 30h24M11 35h18"/></g>',
};
function weatherIcon(code, isDay) {
  const sky = isDay ? ICONS.sun : ICONS.moon;
  let body;
  if (code <= 0) body = sky;
  else if (code <= 2) body = `<g transform="translate(-5 -6) scale(.8)">${sky}</g><g transform="translate(3 3) scale(.9)">${ICONS.cloud}</g>`;
  else if (code === 45 || code === 48) body = `<g transform="translate(0 -6)">${ICONS.cloud}</g>${ICONS.fog}`;
  else if (code >= 95) body = `<g transform="translate(0 -6)">${ICONS.cloud}</g><g transform="translate(0 -6)">${ICONS.bolt}</g>`;
  else if (code >= 51) body = `<g transform="translate(0 -6)">${ICONS.cloud}</g><g transform="translate(0 -4)">${ICONS.rain}</g>`;
  else body = ICONS.cloud;
  return `<svg viewBox="0 0 40 40" aria-hidden="true">${body}</svg>`;
}

const GEAR = '<svg class="b-gear" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M10.3 2h3.4l.5 2.6 1.9.8 2.2-1.5 2.4 2.4-1.5 2.2.8 1.9 2.6.5v3.4l-2.6.5-.8 1.9 1.5 2.2-2.4 2.4-2.2-1.5-1.9.8-.5 2.6h-3.4l-.5-2.6-1.9-.8-2.2 1.5-2.4-2.4 1.5-2.2-.8-1.9L2 13.7v-3.4l2.6-.5.8-1.9-1.5-2.2 2.4-2.4 2.2 1.5 1.9-.8zM12 8.5a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7z"/></svg>';

const board = $('#board');
const state = { offset: 0, personal: null, today: null, weather: undefined };

function buildBoard() {
  board.innerHTML = `
    <div class="b-col">
      <p class="b-clock" data-b="clock"></p>
      <p class="b-date" data-b="date"></p>
      <p class="b-lunar" data-b="lunar"></p>
      <p class="b-note" data-b="note"></p>
      <div class="b-wx"><span class="b-icon" data-b="icon"></span><span class="b-temp" data-b="temp"></span><span class="b-desc" data-b="desc"></span></div>
      <p class="b-detail" data-b="detail"></p>
      <p class="b-rain" data-b="rain"></p>
      <p class="b-status">${PLACE.name}</p>
    </div>
    <div class="b-page" data-b="page">
      <div class="b-head"><span>Tờ lịch</span><span class="b-dot"></span>${GEAR}</div>
      <div class="b-faces">
        <div class="b-sol"><p class="b-m" data-b="sm"></p><p class="b-big" data-b="sd"></p><p class="b-wd" data-b="wd"></p></div>
        <div class="b-lun"><p class="b-m" data-b="lm"></p><p class="b-big" data-b="ldd"></p><p class="b-cc" data-b="cc"></p></div>
      </div>
      <p class="b-hol" data-b="hol"></p>
      <p class="b-gio" data-b="gio"></p>
      <p class="b-quote" data-b="quote"></p>
    </div>`;
}
const slot = (name) => board.querySelector(`[data-b="${name}"]`);
function setText(name, text) {
  const el = slot(name);
  el.textContent = text ?? '';
  el.hidden = !text;
}

// Như FitLine của app: hiện phương án dài nhất vừa một dòng, không để chữ xuống dòng.
const measure = document.createElement('canvas').getContext('2d');
function setFit(name, variants) {
  const el = slot(name);
  el.hidden = !variants.length;
  if (!variants.length) return;
  const style = getComputedStyle(el);
  measure.font = `${style.fontWeight} ${style.fontSize} ${style.fontFamily}`;
  const room = el.clientWidth || el.parentElement.clientWidth;
  el.textContent = variants.find((v) => measure.measureText(v).width <= room) ?? variants[variants.length - 1];
}

/** Ngày riêng người xem nhập: hôm nay hoặc trong 7 ngày tới thì bảng lịch nhắc, như app. */
function personalNote(t) {
  if (!state.personal) return null;
  const next = L.nextDate(state.personal, t.d, t.m, t.y);
  if (!next || next.days > 7) return null;
  const name = state.personal.name;
  if (next.days === 0) return { lunar: true, text: ['Hôm nay: ' + name] };
  if (next.days === 1) return { lunar: false, text: ['Ngày mai: ' + name] };
  return { lunar: false, text: [`Còn ${next.days} ngày: ${name}`] };
}

function renderColumn(t) {
  const jd = L.jdFromDate(t.d, t.m, t.y);
  const ld = L.solarToLunar(t.d, t.m, t.y);
  setText('clock', `${pad(t.hh)}:${pad(t.mm)}`);
  const wd = weekday(jd);
  setFit('date', [`${wd}, ${pad(t.d)}/${pad(t.m)}/${t.y}`, `${wd}, ${pad(t.d)}/${pad(t.m)}`]);
  setFit('lunar', [`Âm lịch ${lunarLabel(ld)} · ${L.yearCanChi(ld.year)}`, `Âm lịch ${lunarLabel(ld)}`]);
  const holidayToday = L.holidayOn(t.d, t.m, ld) || L.dayOffName(t.d, t.m, t.y);
  const note = (!holidayToday && personalNote(t)) || L.dayNote(t.d, t.m, t.y);
  setFit('note', note ? note.text : []);
  slot('note').classList.toggle('is-lunar', Boolean(note && note.lunar));
}

function renderWeather() {
  const w = state.weather;
  const row = slot('icon').parentElement;
  row.classList.toggle('is-empty', !w);
  if (!w) {
    slot('icon').innerHTML = '';
    slot('icon').hidden = true;
    setText('temp', '');
    setText('desc', w === undefined ? 'Đang tải thời tiết…' : 'Chưa lấy được thời tiết');
    setText('detail', '');
    setText('rain', '');
    return;
  }
  slot('icon').hidden = false;
  slot('icon').innerHTML = weatherIcon(w.code, w.isDay);
  setText('temp', `${Math.round(w.temp)}°`);
  setText('desc', L.weatherText(w.code));
  setText('detail', `${Math.round(w.min)}°–${Math.round(w.max)}° · cảm giác ${Math.round(w.feels)}°`);
  setText('rain', w.rain);
}

function renderPage(animate) {
  const t = state.today;
  const [d, m, y] = L.jdToDate(L.jdFromDate(t.d, t.m, t.y) + state.offset);
  const ld = L.solarToLunar(d, m, y);
  const red = L.isRed(d, m, y, ld);
  setText('sm', `Tháng ${m}, ${y}`);
  setText('sd', String(d));
  setText('wd', weekday(ld.jd));
  slot('sd').classList.toggle('is-red', red);
  slot('wd').classList.toggle('is-red', red);
  setText('lm', `Tháng ${L.MONTH_NAME[ld.month]}${ld.leap ? ' nhuận' : ''}`);
  setText('ldd', String(ld.day));
  setText('cc', `Ngày ${L.dayCanChi(ld.jd)}`);
  const holiday = L.holidayOn(d, m, ld)?.name ?? L.dayOffName(d, m, y);
  const own = state.personal && L.nextDate(state.personal, d, m, y)?.days === 0 ? state.personal.name : null;
  setText('hol', [own, holiday].filter(Boolean).join(' · '));
  const gio = slot('gio');
  gio.textContent = 'Giờ tốt: ' + L.gioHoangDao(ld.jd).join(', ');
  gio.classList.toggle('is-good', L.isHoangDao(ld.jd, ld.month));
  setText('quote', `“${L.SAYINGS[ld.jd % L.SAYINGS.length]}”`);
  if (animate && !matchMedia('(prefers-reduced-motion: reduce)').matches) {
    const page = slot('page');
    page.classList.remove('is-turning');
    void page.offsetWidth;
    page.classList.add('is-turning');
  }
  for (const b of $$('.tear .chip')) {
    if (b.dataset.step === '0') b.setAttribute('aria-pressed', String(state.offset === 0));
  }
}

function tick() {
  const t = vnNow();
  const dayChanged = !state.today || t.d !== state.today.d || t.m !== state.today.m || t.y !== state.today.y;
  const minuteChanged = dayChanged || t.mm !== state.today.mm || t.hh !== state.today.hh;
  state.today = t;
  if (minuteChanged) renderColumn(t);
  if (dayChanged) {
    renderPage(false);
    renderGio();
  }
  const clock = $('#ft-clock');
  if (clock && minuteChanged) {
    const ld = L.solarToLunar(t.d, t.m, t.y);
    clock.textContent = `${pad(t.hh)}:${pad(t.mm)} · ${lunarLabel(ld)} âm`;
  }
}

/** "Sắp mưa khoảng 15h (80%)" nếu 3 giờ tới có mưa mà lúc này chưa mưa, như WeatherSource.rainSoon. */
function rainSoon(currentCode, hourly) {
  const wet = (c) => (c >= 51 && c <= 67) || (c >= 80 && c <= 82) || c >= 95;
  if (wet(currentCode)) return '';
  const now = Date.now();
  const upcoming = hourly.time.map((time, i) => ({ time, i })).filter(({ time }) => Date.parse(time + ':00+07:00') > now).slice(0, 3);
  for (const { time, i } of upcoming) {
    const code = hourly.weather_code[i];
    const rain = hourly.precipitation_probability[i] ?? 0;
    if ((wet(code) && rain >= 40) || rain >= 70) {
      const what = code >= 95 ? 'Sắp có dông' : 'Sắp mưa';
      return `${what} khoảng ${Number(time.slice(11, 13))}h (${rain}%)`;
    }
  }
  return '';
}

async function loadWeather() {
  const url = 'https://api.open-meteo.com/v1/forecast'
    + `?latitude=${PLACE.lat}&longitude=${PLACE.lon}&timezone=Asia%2FHo_Chi_Minh&forecast_days=1&forecast_hours=5`
    + '&current=temperature_2m,apparent_temperature,weather_code,is_day'
    + '&hourly=weather_code,precipitation_probability&daily=temperature_2m_max,temperature_2m_min';
  try {
    const res = await fetch(url);
    if (!res.ok) throw new Error(`Open-Meteo HTTP ${res.status}`);
    const r = await res.json();
    state.weather = {
      code: r.current.weather_code, isDay: r.current.is_day === 1, temp: r.current.temperature_2m,
      feels: r.current.apparent_temperature, min: r.daily.temperature_2m_min[0], max: r.daily.temperature_2m_max[0],
      rain: rainSoon(r.current.weather_code, r.hourly),
    };
  } catch {
    state.weather = null;
  }
  renderWeather();
}

/* ---------- ngày giỗ người xem tự nhập ---------- */

const gioInput = $('#gio-input');
const gioResult = $('#gio-result');

function renderGio() {
  const text = gioInput.value.trim();
  state.personal = text ? L.parsePersonal(text) : null;
  gioResult.textContent = '';
  if (text && !state.personal) {
    gioResult.textContent = 'Viết ngày/tháng, chữ “âm” nếu là âm lịch, rồi tên. Ví dụ: 15/8 âm Giỗ ông nội.';
  } else if (state.personal) {
    const t = state.today;
    const next = L.nextDate(state.personal, t.d, t.m, t.y);
    if (next) {
      const [d, m, y] = next.solar;
      const when = next.days === 0 ? 'hôm nay' : next.days === 1 ? 'ngày mai' : `còn ${next.days} ngày`;
      gioResult.append(`${state.personal.name}: ${weekday(L.jdFromDate(d, m, y))}, ${pad(d)}/${pad(m)}/${y}, ${when}. `);
      const go = document.createElement('button');
      go.type = 'button';
      go.className = 'link';
      go.textContent = 'Lật tới tờ hôm đó';
      go.addEventListener('click', () => {
        state.offset = next.days;
        renderPage(true);
      });
      gioResult.append(go);
    }
  }
  renderColumn(state.today);
  renderPage(false);
}

function startBoard() {
  if (!board) return;
  buildBoard();
  tick();
  renderWeather();
  setInterval(tick, 1000);
  loadWeather();
  setInterval(loadWeather, 15 * 60 * 1000);
  for (const b of $$('.tear .chip')) {
    b.addEventListener('click', () => {
      const step = Number(b.dataset.step);
      state.offset = step === 0 ? 0 : state.offset + step;
      renderPage(true);
    });
  }
  gioInput.addEventListener('input', renderGio);
  // Bảng lịch đổi giữa nằm ngang và dựng đứng theo bề ngang: chọn lại độ dài các dòng.
  matchMedia('(min-width: 45em)').addEventListener('change', () => renderColumn(state.today));
}

/* ---------- Vừa mọi màn hình: cỡ, kiểu, ban đêm chọn ảnh chụp tương ứng ---------- */

const SIZES = { 35: [480, 320], 55: [640, 360], 67: [800, 360] };
const INCH = { 35: '3,5', 55: '5,5', 67: '6,7' };

function startShowcase() {
  const form = $('#show form');
  const img = $('#show-img');
  if (!form) return;
  form.addEventListener('change', () => {
    const size = form.elements.size.value;
    const mode = form.elements.mode.value;
    const night = form.elements.night.checked;
    const [w, h] = SIZES[size];
    const src = `img/screen-${mode}-${size}${night ? '-night' : ''}.webp`;
    const next = new Image();
    img.classList.add('is-swapping');
    next.onload = () => {
      img.src = src;
      img.style.setProperty('--w', `${w}px`);
      img.alt = `${mode === 'flip' ? 'Đồng hồ lật' : 'Bảng lịch'} trên màn hình ${INCH[size]} inch, ${night ? 'ban đêm' : 'ban ngày'}`;
      img.classList.remove('is-swapping');
    };
    next.src = src;
    $('#v-size').textContent = `${w} × ${h} dp`;
    $('#v-layout').textContent = mode === 'flip' ? 'Hai thẻ lật và ba dòng tin'
      : size === '67' ? 'Đồng hồ, lịch tháng, một trang' : 'Đồng hồ và một trang';
    $('#v-light').textContent = night ? 'Tông ấm, giảm sáng' : 'Ban ngày';
  });
}

/* ---------- Tờ lịch: trỏ vào ghi chú thì phần màn hình tương ứng sáng lên ---------- */

function startPlate() {
  const plate = $('#plate');
  if (!plate) return;
  const show = (note) => {
    const [top, height] = note.dataset.spot.split(' ').map(Number);
    plate.style.setProperty('--spot', `inset(${(top / 6.4).toFixed(2)}% 0 ${(100 - (top + height) / 6.4).toFixed(2)}% 0)`);
    plate.classList.add('is-spot');
  };
  const hide = () => plate.classList.remove('is-spot');
  for (const note of $$('.note', plate)) {
    note.addEventListener('pointerenter', () => show(note));
    note.addEventListener('pointerleave', hide);
    note.addEventListener('focus', () => show(note));
    note.addEventListener('blur', hide);
  }
}

/* ---------- Cài đặt: bước ở giữa màn hình quyết định khung minh hoạ ---------- */

function startSteps() {
  const steps = $$('.step');
  const screens = $$('#phone-stage .droid');
  if (!steps.length) return;
  const io = new IntersectionObserver((entries) => {
    for (const e of entries) {
      if (!e.isIntersecting) continue;
      for (const s of steps) s.classList.toggle('is-active', s === e.target);
      for (const d of screens) d.classList.toggle('is-on', d.dataset.for === e.target.dataset.state);
    }
  }, { rootMargin: '-45% 0px -45% 0px' });
  steps.forEach((s) => io.observe(s));
}

/* ---------- hiện dần khi cuộn tới ---------- */

function startReveals() {
  const stage = $('.stage');
  if (stage) requestAnimationFrame(() => requestAnimationFrame(() => stage.classList.add('is-in')));
  const io = new IntersectionObserver((entries) => {
    for (const e of entries) {
      if (!e.isIntersecting) continue;
      e.target.classList.add('is-in');
      io.unobserve(e.target);
    }
  }, { rootMargin: '0px 0px -12% 0px' });
  $$('[data-reveal]').forEach((el, n) => {
    [...el.children].forEach((child, i) => child.style.setProperty('--i', String(i + (el.classList.contains('tile') ? n % 4 : 0))));
    io.observe(el);
  });
}

startBoard();
startShowcase();
startPlate();
startSteps();
startReveals();
linkApk();
