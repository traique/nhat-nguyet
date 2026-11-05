<p align="center">
  <img src="docs/banner.png" alt="Nhật Nguyệt – lịch treo tường cho điện thoại Android" width="100%">
</p>

<p align="center">
  <a href="https://github.com/traique/nhat-nguyet/releases/latest"><img src="https://img.shields.io/github/v/release/traique/nhat-nguyet?label=phi%C3%AAn%20b%E1%BA%A3n&color=5FD3B0" alt="Phiên bản mới nhất"></a>
  <img src="https://img.shields.io/badge/Android-4.0.3%2B-3DDC84?logo=android&logoColor=white" alt="Android 4.0.3 trở lên">
  <a href="https://github.com/traique/nhat-nguyet/actions/workflows/android.yml"><img src="https://github.com/traique/nhat-nguyet/actions/workflows/android.yml/badge.svg" alt="Trạng thái build"></a>
</p>

**Nhật Nguyệt** biến một chiếc điện thoại Android, kể cả máy rất cũ, thành tấm lịch treo tường: đồng hồ cỡ lớn, lịch âm dương, thời tiết, giá vàng, xăng dầu, tỷ giá, chứng khoán và tin tức trên một màn hình nằm ngang.

Không cần tài khoản, không quảng cáo, không qua máy chủ trung gian: ứng dụng lấy dữ liệu thẳng từ các nguồn công khai. Mã nguồn mở theo giấy phép Apache 2.0, xem [Quyền riêng tư](PRIVACY.md).

<p align="center">
  <img src="docs/screens/board-6.7.png" alt="Bảng lịch 3 cột trên màn hình 6,7 inch" width="100%">
</p>

<table>
  <tr>
    <td width="50%"><img src="docs/screens/board-5.5.png" alt="Bảng lịch trên màn hình 5,5 inch, trang giá vàng"></td>
    <td width="50%"><img src="docs/screens/board-3.5.png" alt="Bảng lịch trên màn hình 3,5 inch, trang dự báo thời tiết"></td>
  </tr>
  <tr>
    <td align="center"><sub>5,5" · trang giá vàng</sub></td>
    <td align="center"><sub>3,5" · trang dự báo thời tiết</sub></td>
  </tr>
  <tr>
    <td><img src="docs/screens/flip.png" alt="Chế độ đồng hồ lật cỡ lớn"></td>
    <td><img src="docs/screens/night.png" alt="Bảng lịch ban đêm với tông chữ ấm"></td>
  </tr>
  <tr>
    <td align="center"><sub>Đồng hồ lật</sub></td>
    <td align="center"><sub>Ban đêm, chữ tông ấm</sub></td>
  </tr>
</table>

## Tính năng

- **Bảng lịch tự co giãn.** Cột trái luôn hiện giờ, thứ ngày, âm lịch, can chi, ngày lễ, thời tiết hiện tại và pin. Mỗi dữ kiện nằm gọn một dòng; màn nhỏ tự ẩn dòng kém quan trọng thay vì thu chữ.
- **Tờ lịch.** Ngày dương thật lớn, ngày âm màu đỏ son, can chi, giờ hoàng đạo và một câu ca dao mỗi ngày, như tờ lịch bloc treo tường.
- **Lịch âm dương chính xác.** Thuật toán Hồ Ngọc Đức theo múi giờ +7: tháng nhuận, can chi ngày tháng năm, tiết khí, ngày và giờ hoàng đạo, ngày lễ (gồm Ngày Văn hóa Việt Nam 24/11 từ năm 2026), đếm ngược Tết.
- **Thông tin cập nhật.** Thời tiết theo giờ và 6 ngày, giá vàng, giá xăng dầu, tỷ giá Vietcombank, chứng khoán trong phiên, tin tức RSS. Các trang tự xoay vòng; chạm để sang trang, vuốt ngang để tiến hoặc lùi.
- **Đồng hồ lật.** Chế độ chỉ đồng hồ với hai thẻ giờ phút cỡ lớn, kèm ngày, âm lịch và thời tiết nếu muốn.
- **Vuốt để chỉnh độ sáng.** Vuốt lên hoặc xuống trên đồng hồ để tăng giảm độ sáng ngay; mức mới được lưu cho ban ngày hoặc ban đêm tuỳ lúc chỉnh.
- **Thời tiết theo vị trí.** Lần đầu mở app hỏi nơi ở; sau đó gõ tên thành phố hoặc bấm *Dùng vị trí hiện tại* (vị trí qua Wi‑Fi, cấp thành phố là đủ). Kèm chất lượng không khí (AQI, bụi mịn PM2.5, tô màu theo mức) và dòng nhắc "Sắp mưa khoảng 15h" từ dự báo theo giờ.
- **Ngày giỗ, sinh nhật.** Thêm bằng hộp thoại hoặc ghi mỗi dòng một ngày theo âm hoặc dương lịch (vd. `15/8 âm Giỗ ông nội`, ghi kèm năm cũng được). Bảng lịch nhắc trước 7 ngày, tờ lịch ghi tên ngày, lịch tháng đánh dấu chấm đỏ; ngày 30 âm rơi vào tháng thiếu thì tính là ngày 29, ngày 29/2 dương ở năm không nhuận tính là 28/2.
- **Khoá Cài đặt.** Đặt mã PIN để người khác không đổi được cài đặt khi máy treo ở nơi chung. Phím Back không thoát nhầm bảng lịch.
- **Ngày và đêm.** Giờ ngày màn hình luôn sáng. Ban đêm chọn giảm sáng, tắt theo thời gian chờ hoặc tắt hẳn; chữ chuyển tông ấm để bớt ánh sáng xanh. Vẫy tay qua cảm biến tiệm cận để bật màn, che cảm biến khi màn đang sáng để tắt; tự bật lúc sáng, chống lưu ảnh.
- **Chạy được ngoại tuyến.** Dữ liệu được lưu lại; mất mạng vẫn hiện số liệu gần nhất kèm nhãn "dữ liệu cũ".
- **Canh giờ máy.** So với giờ của các máy chủ dữ liệu; giờ máy lệch từ 2 phút (hay bị về năm cũ khi hết pin) thì dòng trạng thái báo, chạm để mở cài đặt ngày giờ.
- **Tự cập nhật** (bản GitHub). Tải bản mới từ GitHub Releases, kiểm tra SHA-256 và tên gói trước khi cài.
- **Hợp máy cũ.** Android 4.0.3 trở lên, không cần root. Tự nhúng TLS 1.2 và bộ chứng chỉ gốc nên máy đời cũ vẫn kết nối được các trang HTTPS hiện nay.

## Cài đặt

GitHub Releases là kênh phát hành chính: bản `NhatNguyet-x.y.z.apk` chạy từ Android 4.0.3 và tự cập nhật. Cửa hàng ứng dụng (Google Play, F-Droid) không còn phục vụ máy Android cũ, nên nếu có cũng chỉ là kênh phụ với bản `-store` (Android 7 trở lên, không tự cập nhật).

1. Tải tệp `NhatNguyet-x.y.z.apk` mới nhất ở trang [Releases](https://github.com/traique/nhat-nguyet/releases/latest). Có gì mới: xem [CHANGELOG.md](CHANGELOG.md).
2. Mở tệp để cài. Lần đầu máy sẽ hỏi quyền cài ứng dụng từ nguồn ngoài:
   - Android 4.x đến 7.x: *Cài đặt > Bảo mật > Nguồn không xác định*.
   - Android 8 trở lên: cho phép trình duyệt hoặc trình quản lý tệp *cài ứng dụng không rõ nguồn*.
3. Mở **Nhật Nguyệt**, chọn nơi ở khi được hỏi, đặt máy nằm ngang và cắm sạc. Bấm biểu tượng bánh răng ở góc trên bên phải để vào **Cài đặt** (hoặc giữ lâu vào đồng hồ). Vuốt dọc trên đồng hồ để chỉnh độ sáng.

Gợi ý khi dùng làm lịch treo tường:

- Đặt khoá màn hình là *Không* hoặc *Vuốt* để bảng tự hiện lại sau khi màn hình bật.
- Nếu chọn "Tắt theo thời gian chờ" ban đêm, đặt thời gian chờ màn hình của máy khoảng 15–30 giây.
- Android 10 trở lên: cấp quyền *Hiện trên ứng dụng khác* (mục *Để lịch tự bật lại* trong Cài đặt, có dấu ✓/✗ cho từng quyền) thì bảng lịch mới tự mở lại được sau khi khởi động máy hay sau khi thoát. Dòng trạng thái sẽ nhắc nếu còn thiếu.
- Samsung, Xiaomi, Oppo: cho Nhật Nguyệt chạy nền và tự khởi động trong phần quản lý pin của máy.

## Tự động cập nhật

Ứng dụng định kỳ đọc `update.json` của bản phát hành mới nhất, tải APK ngầm, kiểm tra SHA-256 và tên gói rồi mới mời cài:

| Android | Cách cài bản mới |
|---|---|
| 4.x – 6.x | Mở trình cài đặt của máy, bấm **Cài đặt** một lần (máy không root không cài im lặng được). |
| 7 trở lên | Cài qua PackageInstaller. Android 8+ cần cho phép Nhật Nguyệt cài ứng dụng; Android 12+ có thể không cần bấm từ lần cập nhật thứ hai. |

Khi có bản mới, dòng trạng thái hiện "Có bản mới … chạm để cài"; nếu đã được phép cài, màn cài sẽ tự mở lúc 10 giờ sáng. Cài xong ứng dụng tự mở lại. Bản `-store` không có cơ chế này.

## Màn hình hỗ trợ

| Kích thước | Bố cục |
|---|---|
| 3,5" – 4" | 2 cột; các dòng phụ tự ẩn khi thiếu chỗ, chữ không nhỏ hơn mức đọc được |
| 4,5" – 5,5" | 2 cột, hiện đủ can chi và dự báo theo giờ |
| 6,1" – 6,7" (tỉ lệ khoảng 20:9) | 3 cột: giờ và âm lịch · lịch tháng · trang xoay vòng |

Có thể ép 2 cột hoặc 3 cột và chỉnh cỡ chữ trong Cài đặt.

## Nguồn dữ liệu

| Trang | Nguồn | Làm mới |
|---|---|---|
| Tờ lịch, lịch tháng, âm lịch | Tính ngay trên máy | – |
| Thời tiết, chất lượng không khí | [Open-Meteo](https://open-meteo.com) (CC BY 4.0, dùng phi thương mại) | 15 phút |
| Tìm thành phố theo tên | Open-Meteo Geocoding (dữ liệu GeoNames, CC BY) | khi bấm *Tìm* |
| Tên nơi theo vị trí hiện tại | [OpenStreetMap Nominatim](https://nominatim.org) (dữ liệu ODbL, tối đa 1 yêu cầu/giây) | khi bấm *Dùng vị trí hiện tại* |
| Giá vàng (SJC, nhẫn, DOJI, PNJ, Bảo Tín, thế giới) | vang.today (API miễn phí, nguồn không chính thức) | 10 phút |
| Giá xăng dầu Petrolimex | giaxanghomnay.com (yêu cầu ghi nguồn) | 1 giờ |
| Tỷ giá | Vietcombank (XML công khai, "chỉ tham khảo", tối đa 1 yêu cầu/5 phút) | 30 phút |
| Chứng khoán (chỉ số và mã tự chọn, tắt sẵn) | Entrade / DNSE (endpoint không có tài liệu, chỉ tham khảo) | 1 phút trong phiên, 30 phút ngoài phiên |
| Tin tức (tắt sẵn) | RSS của báo tự chọn (các báo cho dùng lại miễn phí, phi lợi nhuận, có ghi nguồn) | 10 phút |

Ứng dụng chỉ tải dữ liệu khi màn hình đang sáng. Lỗi ở một nguồn chỉ ảnh hưởng trang đó và được báo trên dòng trạng thái. Mặc định bật 5 trang (Tờ lịch, Lịch tháng, Thời tiết, Giá vàng, Xăng dầu); Chứng khoán và Tin tức bật thêm trong Cài đặt. Chi tiết dữ liệu gửi đi: [PRIVACY.md](PRIVACY.md).

## Quyền ứng dụng

| Quyền | Dùng để |
|---|---|
| Internet, trạng thái mạng | Tải dữ liệu từ các nguồn ở trên |
| Vị trí gần đúng (tuỳ chọn) | Chỉ khi bấm *Dùng vị trí hiện tại*, để lấy thời tiết đúng nơi |
| Giữ màn hình sáng, bật màn hình | Giờ ngày luôn sáng, tự bật màn lúc bắt đầu giờ ngày (báo thức thường, không cần quyền báo thức chính xác) |
| Khởi động cùng máy | Tự mở bảng lịch sau khi bật máy |
| Dịch vụ nền và thông báo | Giữ cảm biến tiệm cận để vẫy tay bật màn |
| Hiện trên ứng dụng khác, bỏ tối ưu pin | Để bảng lịch tự hiện lại trên Android đời mới |
| Cài gói ứng dụng (chỉ bản GitHub) | Cập nhật bản mới |
| Quản trị thiết bị (tuỳ chọn) | Tắt hẳn màn hình ban đêm hoặc khi che cảm biến; gỡ quyền này trong Cài đặt trước khi gỡ ứng dụng |

## Dành cho nhà phát triển

### Build trên máy

```bash
./gradlew :app:testGithubDebugUnitTest :app:lintGithubDebug :app:assembleGithubDebug
```

Cần JDK 17 trở lên và Android SDK (platform 34). Giao diện dựng bằng View thường, không dùng AndroidX hay Compose; lint chạy với `NewApi` là lỗi nên build sẽ hỏng nếu lỡ gọi API mới hơn Android 4.0.3 mà không kiểm tra phiên bản.

Hai flavor: `github` (Android 4.0.3+, có Conscrypt và tự cập nhật OTA) và `store` (Android 7+, không thư viện native, không OTA, không quyền cài gói; dành cho F-Droid hoặc Google Play). Bản debug có tên gói `vn.nhatnguyet.debug`, cài song song với bản chính thức và không tự cập nhật. Muốn thử cập nhật với máy chủ khác (vd. máy ảo gọi `http://10.0.2.2:8000/update.json`), đặt biến `UPDATE_URL` khi build. Xem thêm [CONTRIBUTING.md](CONTRIBUTING.md).

### Phát hành bản mới

Workflow `.github/workflows/android.yml` chạy kiểm thử và lint cho cả hai flavor, build bản release đã ký, rồi tạo GitHub Release chứa `NhatNguyet-X.Y.Z.apk`, `update.json`, bản `-store.apk` và `-store.aab`. Ghi chú phát hành lấy từ mục `## X.Y.Z` trong `CHANGELOG.md` (thiếu là workflow dừng).

1. Tạo khoá ký một lần và **giữ thật kỹ** (mất khoá là không cập nhật được nữa; dùng RSA để Android 4.x đọc được chữ ký):
   ```bash
   keytool -genkeypair -v -keystore nhatnguyet.jks -alias nhatnguyet -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -validity 36500
   base64 -w0 nhatnguyet.jks > nhatnguyet.jks.b64
   ```
2. Trong repo: *Settings > Secrets and variables > Actions*, thêm 4 secret: `KEYSTORE_BASE64` (nội dung tệp `.b64`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
3. Tăng `APP_VERSION` trong `app/build.gradle`, thêm mục `## X.Y.Z` vào `CHANGELOG.md`, commit. Rồi mở *Actions > Android > Run workflow*, nhập đúng số phiên bản đó (vd. `1.0.2`) và chạy; workflow tự tạo tag `v1.0.2` và Release. Cách khác: `git tag v1.0.2 && git push origin v1.0.2`.

`versionCode = X×10000 + Y×100 + Z`, nên mỗi bản sau phải có số phiên bản lớn hơn. Workflow từ chối phát hành nếu tag không khớp `APP_VERSION`, thiếu mục CHANGELOG, hoặc APK bị ký bằng khoá debug. Các action được ghim theo mã commit và Dependabot nhắc nâng hằng tháng.

### Cấu trúc mã

```
app/src/main/java/vn/nhatnguyet/
  App, Prefs, Fmt, Theme   cài đặt, định dạng số kiểu Việt Nam, bảng màu ngày/đêm
  net/      Tls (Conscrypt + kho chứng chỉ gốc), Http, ClockCheck (phát hiện giờ máy lệch)
  lunar/    Lunar (âm lịch, can chi, tiết khí, hoàng đạo, ngày lễ), LunarDate, PersonalDays (ngày giỗ, sinh nhật)
  data/     DataHub (lịch tải, bộ nhớ đệm), các nguồn Weather, Gold, Fuel, Fx, Stock, News; Html, J (tiện ích đọc)
  ui/       MainActivity (bảng lịch), NightPolicy (chính sách màn hình ngày/đêm), Pages, SettingsActivity, các view tự vẽ
  sys/      WakeService + ProximityGesture (cảm biến tiệm cận), Screen, BootReceiver, Scheduler, ScheduleReceiver, AdminReceiver
  update/   Updater, SessionInstall, InstallReceiver (chỉ flavor github)
app/src/store/AndroidManifest.xml   flavor store gỡ quyền cài gói
```

Luồng dữ liệu: `Source.fetch()` chạy trên pool 2 luồng, kết quả về `DataHub.done()` trên luồng chính, `DataHub` báo listener, `MainActivity` so chữ ký rồi vẽ lại đúng phần đổi; bộ nhớ đệm là `cache_<key>.json` trong thư mục riêng của app.

## Đã kiểm thử

- Kiểm thử đơn vị (45 ca): âm lịch (Tết 2020–2033 và 1985, tháng nhuận 2023 và 2025, mùng 2 mùng 3 Tết, Giao thừa, tiết khí sát nửa đêm, hai ngày xa 2054 và 2062, đổi qua lại 1990–2060), ngày giỗ (kèm năm, 29/2), dãy giờ dự báo theo giờ hiện tại, định dạng số, cử chỉ cảm biến, bộ đọc giá vàng, xăng dầu, tỷ giá, thời tiết và RSS trên dữ liệu thật. Chưa có test giao diện.
- Máy ảo Android 4.1.2 (API 16): cài APK ký v1, TLS qua Conscrypt, tải đủ các nguồn dữ liệu, font tiếng Việt, xoay trang, vẫy tay bật màn và trọn quy trình tự cập nhật.
- Bố cục dựng thử ở 480×320 dp (3,5"), 640×360 dp (5,5") và 915×411 dp (6,7").
- Chưa thử trên máy thật Android 4.0.3 và máy 6,7" thật.

## Lưu ý

- vang.today, giaxanghomnay.com và Entrade/DNSE là nguồn không chính thức, có thể thay đổi bất cứ lúc nào.
- Nhiều máy đời mới chỉ bật cảm biến tiệm cận khi đang gọi, nên vẫy tay có thể không đánh thức được màn hình đã tắt.
- Điện thoại cũ cắm sạc liên tục dễ phồng pin: giữ độ sáng đêm thấp và thỉnh thoảng kiểm tra máy. Dòng trạng thái cảnh báo khi pin nóng từ 45°C.

## Giấy phép

Mã nguồn Nhật Nguyệt phát hành theo [Apache License 2.0](LICENSE). Phần lịch âm cài đặt lại thuật toán do [Hồ Ngọc Đức](https://www.informatik.uni-leipzig.de/~duc/amlich/) công bố (mã JavaScript gốc ghi "Copyright 2004 Ho Ngoc Duc"); xem [NOTICE](NOTICE). Báo lỗ hổng: [SECURITY.md](SECURITY.md).

## Thành phần bên thứ ba

Liệt kê đầy đủ trong [NOTICE](NOTICE), cũng được đóng gói trong app (Cài đặt > Giới thiệu > Giấy phép bên thứ ba).

- Font [Inter](https://rsms.me/inter/) (bản rút gọn): SIL Open Font License 1.1, xem `app/src/main/assets/fonts/OFL.txt`.
- [Conscrypt](https://github.com/google/conscrypt) 2.5.3: Apache License 2.0, chỉ trong bản `github`, chỉ nạp trên Android < 5 (không còn nhận bản vá TLS mới, nên dùng Wi‑Fi gia đình tin cậy).
- Bộ chứng chỉ gốc `res/raw/cacerts.pem`: từ kho chứng chỉ của Mozilla (MPL 2.0).
- Dữ liệu thời tiết, không khí, tìm thành phố: [Open-Meteo](https://open-meteo.com) (CC BY 4.0).
- Tên nơi theo toạ độ: dữ liệu © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors (ODbL).

<p align="center"><sub>design by LCTA</sub></p>
