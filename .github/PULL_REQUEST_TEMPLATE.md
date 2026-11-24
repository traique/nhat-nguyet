<!-- Mỗi pull request một việc. Có issue thì ghi "Sửa #123". Quy ước chi tiết: CONTRIBUTING.md. -->

## Thay đổi gì

## Đã thử trên máy nào

<!-- Đổi phần mạng, màn hình, cảm biến hay cập nhật: thử trên máy ảo API 16 và một máy đời mới. -->

## Kiểm tra

- [ ] `./gradlew :app:testGithubDebugUnitTest :app:lintGithubDebug :app:lintStoreDebug` chạy qua.
- [ ] API mới hơn Android 4.0.3 (API 15) đều có kiểm `Build.VERSION.SDK_INT`.
- [ ] Không thêm thư viện, hoặc đã ghi vì sao cần.
- [ ] Chạy được ở cả hai flavor `github` và `store`.
- [ ] Đổi âm lịch, ngày lễ hay ngày nghỉ: đã sửa cùng `docs/lich.js` (CI đối chiếu với app) và thêm ca kiểm thử.
- [ ] Người dùng thấy được thay đổi: đã ghi vào `CHANGELOG.md`.
