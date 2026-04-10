# README_NEW - Tổng Quan Chức Năng Theo Role

Tài liệu này mô tả tổng quan hệ thống theo từng vai trò người dùng.

## 1) Tổng quan hệ thống

Hệ thống Course Registration gồm các nhóm chức năng chính:

- Quản lý tài khoản và xác thực JWT
- Quản lý khóa học và nội dung học tập (section/lesson)
- Đăng ký khóa học theo quy trình phê duyệt
- Theo dõi tiến độ học tập
- Thông báo và chứng chỉ
- Dashboard theo role

## 2) Chức năng theo role

### 2.1 Khách (chưa đăng nhập)

- Xem danh sách khóa học
- Xem chi tiết khóa học (mô tả, giảng viên, số giờ học, số lượng, giá, hình ảnh)
- Đăng ký tài khoản
- Đăng nhập hệ thống

### 2.2 Học viên (Student)

- Đăng ký khóa học (tạo đơn PENDING)
- Theo dõi trạng thái đơn: PENDING / CONFIRMED / WAITLIST / REJECTED / CANCELLED / EXPIRED
- Xem dashboard học viên (đang học, chờ duyệt, hàng chờ, lịch sử)
- Vào học với khóa học đã CONFIRMED
- Đánh dấu hoàn thành bài học, cập nhật tiến độ
- Nhận certificate nếu khóa học bật cert và hoàn thành 100%
- Nhận thông báo hệ thống
- Cập nhật thông tin cá nhân

### 2.3 Giảng viên (Instructor)

- Tạo khóa học mới (slug, mô tả, giá, số giờ học, hình ảnh)
- Quản lý nội dung khóa học:
  - Tạo section
  - Tạo lesson
- Xem dashboard giảng viên:
  - Tổng số khóa học
  - Tổng học viên
  - Tổng đăng ký
  - Tỷ lệ lấp đầy
- Theo dõi danh sách khóa học đang quản lý

### 2.4 Quản trị viên (Admin)

- Quản lý toàn bộ đăng ký khóa học
- Duyệt / Từ chối từng đơn đăng ký
- Duyệt hàng loạt (bulk approve) theo quy tắc số lượng slot
- Từ chối khi lớp đầy và hiển thị danh sách học viên bị từ chối
- Ban user, đổi role user
- Seed dữ liệu demo (khóa học, tài khoản)
- Xem dashboard tổng quan hệ thống

## 3) Quy trình đăng ký khóa học

1. Học viên đăng ký -> tạo đơn `PENDING`
2. Admin duyệt:
   - Còn slot -> `CONFIRMED`
   - Hết slot -> từ chối với lý do lớp đầy
3. Học viên học và cập nhật progress
4. Hoàn thành 100% + khóa học có cert -> cấp certificate

## 4) Dashboard theo role

- Student Dashboard: tiến độ học tập và lịch sử đăng ký
- Instructor Dashboard: thống kê khóa học và học viên, tạo nội dung
- Admin Dashboard: duyệt đăng ký, thống kê hệ thống, thao tác quản trị

## 5) Dữ liệu demo khi khởi động

Hệ thống được cấu hình để có dữ liệu demo ngay từ lần chạy đầu:

- Tự động tạo tài khoản giảng viên demo nếu chưa có
- Tự động tạo/cập nhật 10 khóa học demo (có hình ảnh, giá, số giờ học)
- Đảm bảo khi mở trang Courses sẽ có dữ liệu để hiển thị

## 6) Ghi chú

- Frontend đang sử dụng: `fe_new/`
- Backend API prefix: `/api/v1`
- MBT GraphWalker model: `tests/graphwalker/course-registration-flow.graphml`
