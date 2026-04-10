# SETUP GUIDE - Hướng Dẫn Cài Đặt Chi Tiết Cho Người Mới

Tài liệu này là bản hướng dẫn duy nhất để bạn cài và chạy dự án từ đầu.

## 1) Dự án gồm những gì?

- Backend: FastAPI (Python)
- Database: PostgreSQL (chạy bằng Docker)
- Frontend: React + Vite (thư mục `fe_new`)

## 2) Nên chạy theo cách nào?

Khuyến nghị cho người mới:

1. Chạy backend + database bằng Docker
2. Chạy frontend bằng Node.js local

Theo cách này bạn không cần cài PostgreSQL trên máy.

## 3) Yêu cầu cài đặt

- Docker Desktop
- Node.js 18+

Tùy chọn (nếu muốn chạy backend không dùng Docker):

- Python 3.11+
- PostgreSQL local

## 4) Cấu trúc thư mục quan trọng

- `app/`: mã nguồn backend
- `fe_new/`: mã nguồn frontend đang sử dụng
- `docker-compose.yml`: chạy backend + db bằng Docker
- `tests/graphwalker/`: model test MBT

## 5) Chạy dự án (khuyên dùng)

### Bước 1: chạy backend + db

Tại thư mục gốc dự án:

```bash
docker compose up --build
```

Sau khi chạy xong:

- API: http://localhost:8000
- Swagger: http://localhost:8000/docs
- Health: http://localhost:8000/health

### Bước 2: chạy frontend

Mở terminal mới:

```bash
cd fe_new
npm install
npm run dev
```

Frontend chạy tại: http://localhost:3000

## 6) Dữ liệu mẫu có ngay lần chạy đầu

Khi backend khởi động, hệ thống tự seed dữ liệu demo vào DB:

- Tạo tài khoản giảng viên demo nếu chưa có
- Tạo/cập nhật 10 khóa học demo
- Các khóa demo có sẵn: hình ảnh, đơn giá, số giờ học, trạng thái published

Vì vậy bạn mở trang Khóa học là thấy dữ liệu ngay.

## 7) Chức năng theo role (tóm tắt)

### Khách (chưa đăng nhập)

- Xem danh sách/chi tiết khóa học
- Đăng ký, đăng nhập

### Học viên

- Đăng ký khóa học
- Theo dõi trạng thái đơn
- Học bài, cập nhật tiến độ
- Nhận certificate khi hoàn thành (nếu khóa bật cert)

### Giảng viên

- Tạo khóa học
- Tạo section/lesson
- Xem dashboard giảng viên

### Quản trị viên

- Duyệt/từ chối đăng ký
- Bulk approve theo số lượng slot
- Quản lý user/role
- Seed dữ liệu

## 8) Lệnh thường dùng

- Dừng dịch vụ Docker:

```bash
docker compose down
```

- Dừng và xóa cả dữ liệu DB (để reset từ đầu):

```bash
docker compose down -v
```

- Xem log backend/db:

```bash
docker compose logs -f
```

## 9) Lỗi thường gặp và cách xử lý

### Lỗi `uvicorn ... Exit Code 127`

Nguyên nhân: chưa cài backend local hoặc chưa kích hoạt virtualenv.

Giải pháp nhanh: dùng Docker như mục 5 (không cần uvicorn local).

### Frontend không gọi được API

Kiểm tra:

1. Docker backend đã chạy chưa
2. API có mở được `http://localhost:8000/docs` không
3. Frontend có chạy tại `http://localhost:3000` không

### Cổng 5432 hoặc 8000 bị chiếm

Sửa port trong `.env` rồi chạy lại `docker compose up --build`.

## 10) Chạy backend không dùng Docker (tùy chọn)

Chỉ dùng khi bạn đã có PostgreSQL local.

```bash
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload
```

## 11) MBT (GraphWalker)

- Model: `tests/graphwalker/course-registration-flow.graphml`
- Hướng dẫn: `tests/graphwalker/README.md`

## 12) Tài liệu liên quan

- Tổng quan chức năng theo role: `README_NEW.md`
- Danh sách module test: `test.md`
