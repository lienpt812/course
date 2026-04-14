# GraphWalker MBT Models

Tổng hợp các model MBT (Model-Based Testing) cho hệ thống đăng ký khóa học.

## Danh sách Models

| # | File | Luồng kiểm thử | Role liên quan |
|---|------|----------------|----------------|
| 1 | `Test_Auth.json` | Đăng ký (3 role), đăng nhập, đăng xuất, quên/reset mật khẩu | Guest, Student, Instructor, Admin |
| 2 | `Test_Explorecourse.json` | Xem danh sách khóa học, lọc, tìm kiếm, xem chi tiết. Phân biệt nút theo role | Guest, Student, Instructor, Admin |
| 3 | `Registration_Course.json` | Đăng ký khóa học: PENDING, WAITLIST, lỗi đóng đăng ký, lỗi trùng, hủy đăng ký | Student |
| 4 | `Admin_Approval_Management.json` | Duyệt/từ chối từng đơn, duyệt tất cả, ban user, promote waitlist | Admin, Instructor |
| 5 | `Create_Course.json` | Tạo khóa học, chỉnh sửa (chỉ DRAFT), publish (khóa chỉnh sửa) | Instructor |
| 6 | `Content_Management.json` | Tạo Section và Lesson trong khóa học chưa published | Instructor |
| 7 | `Section_Lesson_Management.json` | Quản lý Section/Lesson chi tiết: validation, sharedState, published block | Instructor |
| 8 | `Update_Learning_Progress.json` | Học bài, đánh dấu hoàn thành, nhận chứng chỉ thủ công | Student |
| 9 | `Learning_Progress_Strict.json` | Kiểm soát truy cập trang học: chỉ CONFIRMED, sharedState | Student |
| 10 | `Certification.json` | Nhận cert, cert đã tồn tại (idempotent), xác minh cert (public) | Student, Guest |
| 11 | `Instructor_Dashboard.json` | Dashboard giảng viên: quản lý khóa, duyệt đăng ký, quản lý nội dung | Instructor |
| 12 | `Student_Dashboard.json` | Dashboard học viên: card tiến trình, chứng chỉ, lịch sử đăng ký | Student |
| 13 | `Notification.json` | Nhận và đọc thông báo 4 loại: REGISTRATION, CERTIFICATE, SYSTEM, LEARNING | Student, Instructor, Admin |
| 14 | `Course_Status_Lifecycle.json` | Vòng đời trạng thái khóa học: DRAFT→PUBLISHED→ARCHIVED, cửa sổ đăng ký | Instructor, Student |
| 15 | `Waitlist_AutoPromote.json` | Hàng chờ tự động promote: hủy CONFIRMED, ban user, tăng capacity, expire pending | Student, Admin, Instructor |

## Phủ sóng các API Endpoints

### Auth (`/api/v1/auth`)
- ✅ POST /auth/register (3 role → 3 dashboard khác nhau)
- ✅ POST /auth/login (3 role)
- ✅ POST /auth/refresh
- ✅ GET /auth/me
- ✅ POST /auth/forgot-password
- ✅ POST /auth/reset-password

### Courses (`/api/v1/courses`)
- ✅ GET /courses (filter PUBLISHED, lọc category/level)
- ✅ GET /courses/{id}
- ✅ POST /courses (Instructor tạo mới)
- ✅ PATCH /courses/{id} (chỉ DRAFT, chỉ chủ khóa)

### Registrations (`/api/v1/registrations`)
- ✅ POST /registrations (PENDING hoặc WAITLIST)
- ✅ GET /registrations
- ✅ POST /registrations/{id}/approve
- ✅ POST /registrations/{id}/reject
- ✅ POST /registrations/bulk-approve
- ✅ POST /registrations/{id}/cancel
- ✅ GET /registrations/{id}/logs

### Learning (`/api/v1/learning`)
- ✅ POST /learning/sections
- ✅ POST /learning/lessons
- ✅ GET /learning/courses/{id}/outline
- ✅ POST /learning/progress
- ✅ GET /learning/courses/{id}/progress
- ✅ GET /learning/courses/{id}/progress-detail

### Certificates (`/api/v1/certificates`)
- ✅ POST /certificates/issue/{course_id}
- ✅ GET /certificates/verify/{code}
- ✅ GET /certificates/me

### Notifications (`/api/v1/notifications`)
- ✅ GET /notifications/me
- ✅ POST /notifications/{id}/read

### Dashboards (`/api/v1/dashboards`)
- ✅ GET /dashboards/student
- ✅ GET /dashboards/instructor
- ✅ GET /dashboards/admin

### Admin (`/api/v1/admin`)
- ✅ POST /admin/seed
- ✅ POST /admin/jobs/expire-pending
- ✅ POST /admin/users/{id}/ban
- ✅ GET /admin/users
- ✅ PUT /admin/users/{id}/role

## Các luồng nghiệp vụ quan trọng

### Luồng chính (Happy Path)
1. Guest → Đăng ký tài khoản Student → Xem khóa học → Đăng ký → Chờ duyệt → Được duyệt → Học → Hoàn thành → Nhận cert

### Luồng Waitlist
2. Student đăng ký khi lớp đầy → WAITLIST → Student CONFIRMED hủy → Auto-promote → Nhận notification

### Luồng Instructor
3. Instructor tạo khóa → Thêm Section/Lesson → Publish → Học sinh đăng ký → Instructor duyệt

### Luồng Admin
4. Admin xem đơn PENDING → Duyệt tất cả (trong slot → CONFIRMED, vượt slot → REJECTED) → Ban user → Auto-promote waitlist

### Luồng Cert
5. Hoàn thành 100% bài học → Bấm Nhận chứng chỉ → Modal chúc mừng → Xem cert trong Dashboard → Xác minh cert (public)

## Shared States (kết nối giữa các model)
- `REGISTRATION_CHECK` - Learning_Progress_Strict ↔ Registration_Course
- `READY_FOR_CERTIFICATE` - Learning_Progress_Strict ↔ Certification
- `COURSE_CREATED` - Create_Course ↔ Section_Lesson_Management
