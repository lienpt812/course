# Cấu trúc thư mục tests

Thư mục này tổ chức test theo mức độ và phạm vi:

- `graphwalker/`: mô hình MBT (GraphWalker).
	- `graphwalker/model/`: lưu file JSON dùng để vẽ/mô tả graph test.
- `unit/models/`: test cho model SQLAlchemy.
- `unit/schemas/`: test validate schema Pydantic.
- `unit/services/`: test nghiệp vụ service.
- `unit/api/`: test endpoint mức đơn vị với `TestClient`.
- `integration/`: test luồng tích hợp nhiều lớp.
- `fixtures/`: dữ liệu mẫu dùng lại cho test.

## Quy ước đặt tên

- File test bắt đầu bằng `test_`.
- Hàm test bắt đầu bằng `test_`.
- Mỗi module chính nên có ít nhất 1 file test tương ứng.

## Gợi ý mở rộng

- Bổ sung test cho `course`, `registration`, `learning`, `certificate`.
- Tách fixture DB riêng cho unit và integration để tránh ảnh hưởng lẫn nhau.
