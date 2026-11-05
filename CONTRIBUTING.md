# Đóng góp cho Nhật Nguyệt

Cảm ơn bạn. Vài quy ước để mã vẫn chạy được trên máy Android cũ:

- **Java thuần, không AndroidX, không Compose, không Kotlin.** Giao diện dựng bằng View thường. Mọi API mới hơn Android 4.0.3 (API 15) phải có kiểm tra `Build.VERSION.SDK_INT`; lint chạy với `NewApi` là lỗi nên build sẽ hỏng nếu quên.
- **Mã, chú thích và chuỗi hiển thị bằng tiếng Việt.** Tên lớp, hàm, biến bằng tiếng Anh ngắn gọn.
- **Không thêm phụ thuộc** nếu không thật cần. Thư viện hiện có duy nhất là Conscrypt (chỉ cho Android < 5).
- **Kiểm thử trước khi gửi:**
  ```bash
  ./gradlew :app:testGithubDebugUnitTest :app:lintGithubDebug :app:assembleGithubDebug
  ```
  Cần JDK 17 trở lên và Android SDK (platform 34). Bộ test chạy trên dữ liệu thật đã lưu trong `app/src/test/resources`; thêm fixture mới khi đổi bộ đọc nguồn dữ liệu.
- **Thử trên máy ảo API 16 và một máy đời mới** nếu đổi phần mạng, màn hình, cảm biến hay cập nhật.
- **Bản debug** có tên gói `vn.nhatnguyet.debug`, cài song song với bản chính và không tự cập nhật.
- **Lịch nghỉ hằng năm:** khi Bộ Nội vụ công bố lịch nghỉ Tết Âm lịch và Quốc khánh năm sau, thêm một dòng vào `DaysOff.OFFICIAL` (số ngày nghỉ cuối năm âm lịch, ngày liền kề Quốc khánh, ngày hoán đổi và làm bù), ghi số văn bản trong chú thích, thêm ca kiểm thử trong `DaysOffTest`, và thêm cùng dòng vào `OFFICIAL` trong `docs/lich.js` (trang giới thiệu). CI đối chiếu lịch của trang với app từng ngày 2020–2040, lệch là build hỏng.
- Hai flavor: `github` (có OTA, Android 4.0.3+) và `store` (không OTA, Android 7+). Tính năng mới phải chạy ở cả hai.

Gửi pull request vào nhánh `main`. Mô tả ngắn gọn thay đổi và máy đã thử. Mã đóng góp được phát hành theo giấy phép Apache 2.0 của dự án.
