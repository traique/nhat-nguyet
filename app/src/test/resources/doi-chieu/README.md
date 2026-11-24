# Dữ liệu đối chiếu cho `LunarReferenceTest`

Ba tệp ở đây chỉ dùng khi chạy kiểm thử, không đóng gói trong ứng dụng.

| Tệp | Số ca | Kiểm gì |
|---|---:|---|
| `duong-sang-am.tsv` | 5.975 | Ngày dương 1900–2100 đổi ra ngày âm (UTC+7), can chi ngày, tháng, năm, Nhị Thập Bát Tú |
| `am-sang-duong.tsv` | 2.784 | Ngày âm đổi ra ngày dương, kể cả ca hỏi tháng nhuận |
| `imcce-tiet-khi.tsv` | 4.824 | Giờ bắt đầu 24 tiết khí 1900–2100 (giờ quốc tế, tới giây) |

## Nguồn và giấy phép

Dữ liệu: Nhat Nguyet (nhatnguyet.org), giấy phép
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/), lấy từ repo
<https://github.com/Nhat-Nguyet/nhatnguyet-lunar-ziwei-bazi-numerology> ở commit
`708b5c3cc011e5784d418d6932df956a9199376a`:

- `duong-sang-am.tsv`, `am-sang-duong.tsv`: rút từ `ca-kiem/am-lich.json` (lõi tính 2.3.0). Đã sửa đổi:
  chỉ giữ ca tính theo giờ Việt Nam (không có `muiGio`) và các cột cần dùng, đổi từ JSON sang TSV.
- `imcce-tiet-khi.tsv`: chép nguyên văn `doi-chieu/du-lieu/imcce-tiet-khi-1900-2100.tsv`, thêm ba dòng ghi
  nguồn ở đầu. Các mốc được nội suy từ lịch thiên văn của IMCCE (Đài Thiên văn Paris, dịch vụ Miriade);
  cách nội suy ghi trong phần đầu tệp.

Dữ liệu cung cấp nguyên trạng, không kèm bảo đảm, theo điều khoản của CC BY 4.0.

## Hai chỗ app cố ý khác bộ ca kiểm

- **Tháng nhuận không có thật.** 864 ca trong `am-sang-duong.tsv` hỏi "tháng X nhuận" ở năm không có
  tháng nhuận X. Bộ ca kiểm vẫn trả ngày của tháng X thường (giống mã gốc của Hồ Ngọc Đức), còn
  `Lunar.lunarToSolar` trả `null`. Test chấp nhận `null` cho đúng nhóm ca này và đếm đủ 864 ca.
- **Tên tiết khí của ngày.** Bộ ca kiểm ghi tiết đang hiệu lực lúc 0 giờ, app ghi tiết bắt đầu trong ngày
  (như lịch in). Vì vậy test không so cột tiết khí của `ca-kiem`, mà so ngày bắt đầu tiết với IMCCE.

## Tiết khí so với IMCCE

App đặt đúng ngày cho 4.816 trên 4.824 mốc. Tám mốc còn lại đều rơi trong vòng 5 phút quanh nửa đêm giờ
Việt Nam (1908, 1946, 1948, 1981, 1989, 2003, 2082, 2098), được liệt kê trong `LunarReferenceTest.KNOWN_TERM_MISSES`.
Sửa công thức Mặt Trời mà danh sách này đổi, dù tốt hơn hay kém đi, test đều báo để cập nhật có chủ ý.

## Rút lại từ repo gốc

```bash
git clone https://github.com/Nhat-Nguyet/nhatnguyet-lunar-ziwei-bazi-numerology.git nn
python3 - nn app/src/test/resources/doi-chieu <<'PY'
import json, os, sys
src, out = sys.argv[1], sys.argv[2]
d = json.load(open(os.path.join(src, "ca-kiem/am-lich.json"), encoding="utf-8"))
vn = [x for x in d["ca"] if x["vao"].get("muiGio") is None]
with open(os.path.join(out, "duong-sang-am.tsv"), "w", encoding="utf-8", newline="\n") as f:
    for x in vn:
        v, r = x["vao"], x["ra"]
        if v["phep"] != "duong-sang-am":
            continue
        a = r["am"]
        f.write("\t".join(map(str, [v["dd"], v["mm"], v["yyyy"], a["day"], a["month"], a["year"], int(a["leap"]),
                                   r["canChiNgay"]["label"], r["canChiThang"]["label"], r["canChiNam"]["label"],
                                   r["saoNhiThapBatTu"]])) + "\n")
with open(os.path.join(out, "am-sang-duong.tsv"), "w", encoding="utf-8", newline="\n") as f:
    for x in vn:
        v, r = x["vao"], x["ra"]
        if v["phep"] != "am-sang-duong":
            continue
        s = r["duong"]
        f.write("\t".join(map(str, [v["ngay"], v["thang"], v["nam"], int(v["nhuan"]), int(r["hopLe"]),
                                   s["day"], s["month"], s["year"]])) + "\n")
PY
```

Lệnh trên không ghi các dòng chú thích `#` ở đầu tệp; test bỏ qua dòng bắt đầu bằng `#` nên chạy được ngay.
