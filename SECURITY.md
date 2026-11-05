# Báo lỗ hổng bảo mật

Nếu bạn phát hiện lỗ hổng trong Nhật Nguyệt, xin **đừng** đăng công khai ngay. Hãy dùng tính năng
*Report a vulnerability* (Security > Advisories) của repo này trên GitHub, hoặc mở issue chỉ ghi
"vấn đề bảo mật, cần liên hệ riêng" để tác giả liên lạc lại.

Phạm vi đáng quan tâm nhất:

- Cơ chế tự cập nhật: `update/Updater.java`, `update/SessionInstall.java`, `update.json` trên GitHub Releases và workflow `.github/workflows/android.yml` (khoá ký nằm trong secret của repo).
- Kết nối TLS trên Android cũ: `net/Tls.java` (Conscrypt, bộ chứng chỉ gốc nhúng `res/raw/cacerts.pem`), `net/Http.java`.
- Khoá Cài đặt bằng PIN (`Prefs.java`, `ui/MainActivity.java`): đây là khoá mềm, xoá dữ liệu ứng dụng là bỏ được; không coi đây là biện pháp bảo mật.

Bản được hỗ trợ: phiên bản mới nhất trên [GitHub Releases](https://github.com/traique/nhat-nguyet/releases/latest).
