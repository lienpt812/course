# test-web — MBT cho web đăng ký khóa học (Java + GraphWalker + Selenide)

Bộ test Model-Based Testing mới, độc lập với `tests/` hiện có.
Mục tiêu: phủ toàn bộ chức năng của hệ thống EduPlatform (frontend `fe_new` + backend FastAPI `app/`) theo từng module nghiệp vụ.

## Stack

- **GraphWalker 4.3.x** — sinh đường đi từ mô hình đồ thị.
- **Selenide 7.x** — tương tác trình duyệt (Chrome/Edge headless hỗ trợ sẵn).
- **JUnit 5** — runner cho các test-class.
- **Maven** — build / chạy model.

## Cấu trúc

```
test-web/
├── pom.xml
├── README.md
└── src/
    ├── main/java/com/eduplatform/mbt/
    │   ├── models/               # (bước 2) Interfaces GraphWalker sinh từ model JSON
    │   └── pages/                # (bước 2) Selenide Page Objects
    └── test/
        ├── java/com/eduplatform/mbt/
        │   └── impl/             # (bước 2) Các class implement interface GraphWalker
        └── resources/com/eduplatform/mbt/models/
            ├── 01_AuthModel.json               # Module A - Xác thực & phân quyền
            ├── 02_CourseExplorationModel.json  # Module B - Khám phá khóa học
            ├── 03_RegistrationModel.json       # Module C - Đăng ký & duyệt đơn
            ├── 04_LearningModel.json           # Module D - Học & tiến độ
            ├── 05_CertificateModel.json        # Module E - Cấp chứng chỉ
            ├── 06_InstructorContentModel.json  # Module F - Quản lý nội dung (Instructor)
            └── 07_AdminManagementModel.json    # Module G - Quản trị user & seed
```

## Mapping module ↔ source code của web

| Module | Vertex/Edge chính | Trang FE | API backend | Ràng buộc nghiệp vụ khớp với code |
|--------|-------------------|----------|-------------|-----------------------------------|
| A. Auth | `v_LoginPage`, `v_RegisterPage`, `v_ForgotPasswordPage`, `v_ResetPasswordPage`, `v_*Dashboard`, `v_ProfilePage` | `LoginPage.tsx`, `RegisterPage.tsx`, `ForgotPasswordPage.tsx`, `ResetPasswordPage.tsx`, `ProfilePage.tsx` | `/api/v1/auth/login`, `/register`, `/refresh`, `/me`, `/forgot-password`, `/reset-password` | Password regex `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,64}$`, email regex, phone `^\+?[0-9]{9,15}$`, role STUDENT cần `student_major \|\| learning_goal`, role INSTRUCTOR cần `expertise`, ADMIN không đăng ký public, reset token TTL 30 phút và `used=false`. |
| B. Explore | `v_CoursesPage`, `v_CourseDetail` | `CoursesPage.tsx`, `CourseDetailPage.tsx` | `/api/v1/courses`, `/courses/{id}` | Lọc theo category/level/search, hiển thị confirmed_slots/max_capacity, open/close date. |
| C. Registration | `v_PendingRegistration`, `v_ConfirmedRegistration`, `v_RejectedRegistration`, `v_AdminDashboard` | `CourseDetailPage.tsx`, `AdminDashboard.tsx`, `StudentDashboard.tsx` | `/registrations`, `/{id}/approve`, `/{id}/reject`, `/bulk-approve`, `/{id}/cancel` | Chỉ STUDENT `ACTIVE` & course `PUBLISHED` & `open_at<=now<=close_at` mới đăng ký được. Approve chỉ khi `confirmed < max_capacity`. Bulk approve xếp theo `created_at ASC`, phần vượt slot → REJECTED với lý do "Lớp đã đủ". |
| D. Learning | `v_LearningPage`, `v_LessonCompleted`, `v_AllLessonsCompleted` | `LearningPage.tsx` | `/learning/courses/{id}/outline`, `/learning/progress`, `/learning/courses/{id}/progress`, `/progress-detail` | Chỉ học viên `CONFIRMED` hoặc lesson `is_preview=true` mới cập nhật tiến độ. `completed = (pct>=80 nếu VIDEO) \|\| (pct>=100 nếu khác)`. |
| E. Certificate | `v_CertificateIssued`, `v_CertificateVerified` | `LearningPage.tsx`, `StudentDashboard.tsx` | `/certificates/issue/{id}`, `/verify/{code}`, `/me` | Chỉ cấp khi `course.certificate_enabled==true` & tất cả lesson đã hoàn thành. Cấp lần 2 trả về cert hiện có (idempotent). Mã verification là UUID 32 ký tự. |
| F. Instructor | `v_InstructorCreateCourse`, `v_InstructorCreateSection`, `v_InstructorCreateLesson`, `v_EditCourse` | `InstructorDashboard.tsx`, `CourseDetailPage.tsx` | `/courses` POST/PUT, `/learning/sections`, `/learning/lessons` | Title 3–255, description ≥20, slug ≤255, category ≤100, image_url http(s)://, max_capacity 1–10000, estimated_hours 1–1000, lesson duration 1–600. Chỉ instructor chủ sở hữu mới edit được, và chỉ khi `status != PUBLISHED`. |
| G. Admin | `v_AdminUsers`, `v_SeedDone` | `AdminDashboard.tsx` (+ Admin API) | `/admin/seed`, `/admin/seed/courses`, `/admin/users`, `/admin/users/{id}/role`, `/admin/users/{id}/ban`, `/admin/jobs/expire-pending` | Chỉ ADMIN mới gọi được. Ban user → tự động CANCELLED các registration CONFIRMED và rebalance waitlist. Seed là idempotent. |

## Lộ trình

- **Bước 1 (xong)** — Tạo mô hình (các file `.json` trong `src/test/resources/models`) có đầy đủ:
  - `guard` — điều kiện chuyển tiếp đúng với rule của backend/frontend.
  - `actions` — cập nhật biến trạng thái (role, login, token, capacity, registration status, progress, ...).
  - `weight` — ưu tiên đường đi hợp lệ (happy path) cao hơn đường lỗi.
  - `requirements` — tag truy vết yêu cầu (REQ-A-001, REQ-C-003, ...).
- **Bước 2** — Sinh interface Java từ model + implement bằng Selenide (page objects), viết các Runner JUnit 5.
- **Bước 3** — Tích hợp CI, chạy song song theo module, xuất báo cáo coverage edges/vertices.

## Chạy mô hình

### Bước 2a — Sinh interface Java từ model

GraphWalker Maven plugin quét `src/test/resources/**/*.json` và xuất ra
`target/generated-test-sources/graphwalker/com/eduplatform/mbt/models/<Model>.java`.
Các interface này chứa method ký tên theo mỗi vertex và edge (có chung tên
`v_xxx`, `e_xxx` với JSON model) để bạn implement bằng Selenide.

```bash
cd test-web
mvn -q generate-test-sources       # chỉ sinh code, không chạy test
mvn -q compile test-compile        # sinh + compile
```

Sau lệnh trên IDE (IntelliJ/VSCode) sẽ autocomplete cho các interface:

- `com.eduplatform.mbt.models.AuthModel`
- `com.eduplatform.mbt.models.CourseExplorationModel`
- `com.eduplatform.mbt.models.RegistrationModel`
- `com.eduplatform.mbt.models.LearningModel`
- `com.eduplatform.mbt.models.CertificateModel`
- `com.eduplatform.mbt.models.InstructorContentModel`
- `com.eduplatform.mbt.models.AdminManagementModel`

### Bước 2b — Chạy Runner (đã implement)

Runner là class JUnit 5 annotated `@GraphWalker(...)` + Selenide page objects.
Failsafe plugin chạy các class theo pattern `*Runner.java` / `*IT.java` trong phase `verify`.

Các `impl/*Impl.java` hiện dùng generator `random(length(N))` để giới hạn số step mỗi lần chạy.
Khi muốn phủ 100% edges/vertices, sửa `@GraphWalker(value = ...)` trong class impl sang
`random(edge_coverage(100))` hoặc `random(edge_coverage(100) && vertex_coverage(100))`
(chạy có thể lâu vài chục phút cho mỗi module tuỳ luồng negative path).

```bash
# Chạy toàn bộ module
mvn -q verify

# Chạy 1 module (dùng profile)
mvn -q -Pmodule-auth verify
mvn -q -Pmodule-registration verify
mvn -q -Pmodule-learning verify

# Chạy headed (có UI trình duyệt) khi debug
mvn -q -Pheaded -Pmodule-auth verify

# Override URL khi chạy với staging
mvn -q verify -Dweb.base.url=https://staging.example.com -Dapi.base.url=https://api.staging.example.com
```

Unit test (helper/util) chạy bằng Surefire ở phase `test`:

```bash
mvn -q test
```

## Cấu hình môi trường (dự kiến, sẽ dùng ở bước 2)

| Biến | Mô tả | Mặc định |
|------|-------|----------|
| `WEB_BASE_URL` | URL của frontend Vite | `http://localhost:3000` |
| `API_BASE_URL` | URL của backend FastAPI | `http://localhost:8000` |
| `SELENIDE_BROWSER` | Trình duyệt Selenide | `chrome` |
| `SELENIDE_HEADLESS` | Headless on/off | `true` |
