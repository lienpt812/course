# MODULE TEST (MBT - GRAPHWALKER)

Tài liệu này mô tả các module test phù hợp với cách kiểm thử Model-Based Testing bằng GraphWalker.

## 1) Phạm vi kiểm thử

- Kiểm thử luồng nghiệp vụ chính theo role: Khách, Học viên, Giảng viên, Quản trị viên.
- Kiểm thử theo mô hình trạng thái (vertex) và chuyển tiếp (edge).
- Kiểm thử cả API backend và hành vi hiển thị trên frontend.

## 2) Danh sách module test theo MBT

### Module A - Xác thực và phân quyền

- Mục tiêu:
  - Đăng nhập/đăng xuất đúng vai trò.
  - Chưa đăng nhập thì điều hướng về trang Khóa học.
- Vertex gợi ý:
  - `v_GuestOnCourses`
  - `v_LoginPage`
  - `v_StudentDashboard`
  - `v_InstructorDashboard`
  - `v_AdminDashboard`
- Edge gợi ý:
  - `e_GoToLogin`
  - `e_LoginAsStudent`
  - `e_LoginAsInstructor`
  - `e_LoginAsAdmin`
  - `e_LogoutToCourses`

### Module B - Khám phá khóa học

- Mục tiêu:
  - Xem danh sách khóa học có dữ liệu từ DB.
  - Xem chi tiết khóa học: ảnh, giảng viên, số giờ học, đơn giá.
- Vertex gợi ý:
  - `v_CourseListVisible`
  - `v_CourseDetailVisible`
- Edge gợi ý:
  - `e_OpenCourseList`
  - `e_OpenCourseDetail`

### Module C - Đăng ký khóa học và duyệt đơn

- Mục tiêu:
  - Học viên đăng ký tạo đơn `PENDING`.
  - Admin duyệt đơn theo rule số lượng slot.
  - Khi lớp đầy thì đơn vượt bị từ chối đúng lý do.
- Vertex gợi ý:
  - `v_PendingRegistration`
  - `v_ConfirmedRegistration`
  - `v_RejectedRegistration`
- Edge gợi ý:
  - `e_RegisterCourse`
  - `e_AdminApprove`
  - `e_AdminRejectWhenFull`
  - `e_AdminBulkApprove`

### Module D - Luồng học tập và tiến độ

- Mục tiêu:
  - Chỉ học viên `CONFIRMED` mới vào học.
  - Đánh dấu hoàn thành bài học cập nhật tiến độ từ DB.
  - Thanh tiến độ phản ánh đúng số bài đã hoàn thành.
- Vertex gợi ý:
  - `v_LearningPage`
  - `v_ProgressUpdated`
- Edge gợi ý:
  - `e_StartLearning`
  - `e_MarkLessonCompleted`

### Module E - Cấp chứng chỉ

- Mục tiêu:
  - Khóa học bật cert và hoàn thành 100% thì tự cấp chứng chỉ.
  - Có thể verify chứng chỉ.
- Vertex gợi ý:
  - `v_CertificateIssued`
  - `v_CertificateVerified`
- Edge gợi ý:
  - `e_CompleteCourseAndIssueCertificate`
  - `e_VerifyCertificate`

### Module F - Quản lý nội dung cho giảng viên

- Mục tiêu:
  - Tạo khóa học có validation bắt buộc.
  - Tạo section/lesson trực tiếp từ dashboard giảng viên.
  - Dữ liệu hiển thị đúng trên trang học của học viên.
- Vertex gợi ý:
  - `v_InstructorCreateCourse`
  - `v_InstructorCreateSection`
  - `v_InstructorCreateLesson`
- Edge gợi ý:
  - `e_CreateCourse`
  - `e_CreateSection`
  - `e_CreateLesson`

### Module G - Quản trị người dùng và seed dữ liệu

- Mục tiêu:
  - Cập nhật role user, ban user.
  - Seed dữ liệu demo idempotent (chạy lại không tạo trùng).
- Vertex gợi ý:
  - `v_AdminUsers`
  - `v_AdminSeedDone`
- Edge gợi ý:
  - `e_UpdateUserRole`
  - `e_BanUser`
  - `e_SeedDemoCourses`

## 3) Mapping module -> API backend

- Module A:
  - `/api/v1/auth/login`
  - `/api/v1/auth/me`
  - `/api/v1/auth/refresh`
- Module B:
  - `/api/v1/courses`
  - `/api/v1/courses/{course_id}`
- Module C:
  - `/api/v1/registrations`
  - `/api/v1/registrations/{id}/approve`
  - `/api/v1/registrations/{id}/reject`
  - `/api/v1/registrations/bulk-approve`
- Module D:
  - `/api/v1/learning/courses/{course_id}/outline`
  - `/api/v1/learning/progress`
  - `/api/v1/learning/courses/{course_id}/progress`
- Module E:
  - `/api/v1/certificates/issue/{course_id}`
  - `/api/v1/certificates/verify/{code}`
- Module F:
  - `/api/v1/courses`
  - `/api/v1/learning/sections`
  - `/api/v1/learning/lessons`
- Module G:
  - `/api/v1/admin/seed/courses`
  - `/api/v1/admin/users`
  - `/api/v1/admin/users/{user_id}/role`
  - `/api/v1/admin/users/{user_id}/ban`

## 4) Artifact GraphWalker cần dùng

- Model chính: `tests/graphwalker/course-registration-flow.graphml`
- Hướng dẫn chạy: `tests/graphwalker/README.md`

## 5) Tiêu chí pass cơ bản

- Không lỗi 5xx trong luồng chuẩn.
- Trạng thái nghiệp vụ chuyển đúng theo edge trong model.
- Dữ liệu hiển thị trên UI khớp dữ liệu trong DB.
- Các rule quan trọng (slot lớp, phân quyền, cấp cert) hoạt động đúng.
