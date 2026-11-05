# Chính sách quyền riêng tư

Cập nhật: 04/11/2026. Áp dụng cho ứng dụng **Nhật Nguyệt** (tên gói `vn.nhatnguyet`).

## Tóm tắt

- Không tài khoản, không quảng cáo, không công cụ phân tích, không máy chủ trung gian.
- Ứng dụng không thu thập hay gửi dữ liệu nào về cho tác giả.
- Mọi cài đặt (nơi ở, ngày giỗ, mã PIN đã băm, trang hiển thị) và thống kê pin 48 giờ gần nhất chỉ lưu trên máy của bạn, không gửi đi đâu, và bị xoá khi gỡ ứng dụng.
- Dữ liệu hiển thị được tải thẳng từ các dịch vụ công khai liệt kê dưới đây. Mỗi lần tải, dịch vụ đó nhận địa chỉ IP của máy bạn như với mọi kết nối Internet.

## Dữ liệu gửi tới dịch vụ bên ngoài

| Dịch vụ | Gửi gì | Khi nào |
|---|---|---|
| Open-Meteo (api.open-meteo.com, air-quality-api.open-meteo.com, geocoding-api.open-meteo.com) | Toạ độ nơi ở (4 chữ số thập phân, khoảng 10 m) hoặc tên thành phố bạn gõ | Thời tiết: mỗi 15 phút khi màn hình sáng. Tìm thành phố: khi bạn bấm Tìm |
| OpenStreetMap Nominatim (nominatim.openstreetmap.org) | Toạ độ do máy định vị (5 chữ số thập phân) | Chỉ khi bạn bấm *Dùng vị trí hiện tại* |
| vang.today | Không có dữ liệu cá nhân | Giá vàng, mỗi 10 phút |
| giaxanghomnay.com | Không có dữ liệu cá nhân | Giá xăng dầu, mỗi giờ |
| Vietcombank (portal.vietcombank.com.vn) | Không có dữ liệu cá nhân | Tỷ giá, mỗi 30 phút |
| DNSE / Entrade (services.entrade.com.vn, api.dnse.com.vn) | Mã chứng khoán bạn theo dõi | Trang Chứng khoán, nếu bật: mỗi phút trong phiên |
| Các báo bạn chọn (RSS) | Không có dữ liệu cá nhân | Trang Tin tức, nếu bật: mỗi 10 phút |
| GitHub (github.com) | Không có dữ liệu cá nhân | Kiểm tra bản mới, mỗi 6 giờ (chỉ bản phát hành trên GitHub, có thể tắt trong Cài đặt) |

Toạ độ mặc định là tâm TP. Hồ Chí Minh. Khi bạn gõ tên thành phố, ứng dụng dùng tâm của thành phố đó. Chỉ khi bạn bấm *Dùng vị trí hiện tại*, toạ độ thật của máy mới được lưu và gửi đi cho dự báo thời tiết. Bạn có thể đổi lại bằng cách gõ tên thành phố.

Mỗi dịch vụ có chính sách riêng về nhật ký kết nối; ứng dụng không kiểm soát được việc đó. Open-Meteo công bố chính sách tại https://open-meteo.com/en/terms, OpenStreetMap tại https://osmfoundation.org/wiki/Privacy_Policy.

## Quyền của ứng dụng

| Quyền | Dùng để |
|---|---|
| Internet, trạng thái mạng | Tải dữ liệu từ các dịch vụ ở trên |
| Vị trí gần đúng (tuỳ chọn) | Chỉ khi bạn bấm *Dùng vị trí hiện tại* |
| Giữ màn hình sáng, bật màn hình, khởi động cùng máy, chạy nền, thông báo | Hoạt động như lịch treo tường: luôn sáng ban ngày, tự bật lại, vẫy tay bật màn |
| Hiện trên ứng dụng khác, bỏ tối ưu pin | Để tự hiện lại trên Android đời mới |
| Quản trị thiết bị (tuỳ chọn) | Chỉ để tắt màn hình ban đêm; gỡ quyền trong Cài đặt trước khi gỡ ứng dụng |
| Cài gói ứng dụng (chỉ bản GitHub) | Tự cập nhật bản mới từ GitHub Releases |

Ứng dụng không đọc danh bạ, tin nhắn, ảnh, micro, máy ảnh hay bất cứ dữ liệu nào khác trên máy.

## Trẻ em

Ứng dụng không có nội dung dành riêng cho trẻ em và không thu thập dữ liệu của bất kỳ ai.

## Liên hệ

Mở issue tại https://github.com/traique/nhat-nguyet/issues.
