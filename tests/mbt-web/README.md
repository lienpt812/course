# MBT Web Tests — AltWalker + Selenium (C#/.NET 8)

Dự án test **hoàn toàn mới** — độc lập, không phụ thuộc `tests/altwalker` cũ.

## Cấu trúc

```
tests/mbt-web/
├── MbtWeb.csproj
├── Program.cs                  ← đăng ký Setup + 8 model classes
├── Setup.cs                    ← setUpRun / tearDownRun / afterStep
├── run.ps1                     ← runner (Java, build, altwalker online)
├── Helpers/
│   ├── TestEnv.cs              ← biến môi trường tập trung
│   ├── DriverFactory.cs        ← tạo ChromeDriver
│   ├── PageHelper.cs           ← facade Selenium (click, fill, wait…)
│   └── ApiHelper.cs            ← HTTP API (seed, tokens, helpers)
├── Models/                     ← 1 class C# per model JSON (bắt buộc khớp "name")
│   ├── Auth.cs
│   ├── CourseCatalog.cs
│   ├── StudentFlow.cs
│   ├── InstructorFlow.cs
│   ├── AdminFlow.cs
│   ├── LearningFlow.cs
│   ├── CertificationFlow.cs
│   └── NotificationFlow.cs
└── models/                     ← GraphWalker JSON models
    ├── Auth.json
    ├── CourseCatalog.json
    ├── StudentFlow.json
    ├── InstructorFlow.json
    ├── AdminFlow.json
    ├── LearningFlow.json
    ├── CertificationFlow.json
    └── NotificationFlow.json
```

## Yêu cầu

| Thứ cần | Ghi chú |
|---------|---------|
| .NET SDK 8 | `dotnet --version` |
| Python 3.8+ + AltWalker | `pip install altwalker` |
| Java 17+ | GraphWalker dùng JVM |
| Google Chrome 124 + ChromeDriver 124 | bao gồm trong NuGet package |

## Biến môi trường

| Biến | Mặc định |
|------|----------|
| `APP_BASE_URL` | `http://localhost:3000` |
| `API_BASE_URL` | `http://localhost:8000/api/v1` |
| `API_HEALTH_URL` | `http://localhost:8000/health` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin@test.com` / `Password123!` |
| `INSTRUCTOR_EMAIL` / `INSTRUCTOR_PASSWORD` | `instructor@test.com` / `Password123!` |
| `STUDENT_EMAIL` / `STUDENT_PASSWORD` | `student@test.com` / `Password123!` |
| `HEADLESS` | `` (bỏ trống = visible), `1` = headless |
| `WAIT_SECONDS` | `15` |

## Chạy test

```powershell
# Chạy tất cả models
.\tests\mbt-web\run.ps1

# Chạy 1 model cụ thể
.\tests\mbt-web\run.ps1 -Model Auth

# Build lại trước khi chạy
.\tests\mbt-web\run.ps1 -Rebuild

# Headless
.\tests\mbt-web\run.ps1 -Headless

# Chỉ verify model vs code (không chạy browser)
.\tests\mbt-web\run.ps1 -VerifyOnly

# Trỏ API sang cổng khác
.\tests\mbt-web\run.ps1 -ApiBaseUrl http://localhost:8000/api/v1
```

## Danh sách model

| Model | Mô tả | Role cần |
|-------|-------|----------|
| `Auth` | Đăng nhập, đăng ký, quên mật khẩu | Không cần |
| `CourseCatalog` | Duyệt danh sách khóa học công khai | Không cần |
| `StudentFlow` | Học viên đăng ký khóa học | Student |
| `InstructorFlow` | Instructor tạo và publish khóa học | Instructor |
| `AdminFlow` | Admin duyệt/từ chối đăng ký | Admin |
| `LearningFlow` | Học viên học bài, lưu tiến độ | Student |
| `CertificationFlow` | Xem và xác minh chứng chỉ | Student |
| `NotificationFlow` | Xem thông báo, đánh dấu đã đọc | Student |

## Seed data

`ApiHelper.SeedAll()` gọi `POST /admin/seed` (tạo `altwalker-e2e-cert` + users + registration CONFIRMED).
Sau khi seed, xác minh: có khóa published đúng slug, outline ≥ 2 lesson, login student được.

## Thêm model mới

1. Tạo `models/TenModel.json` — `"name": "TenModel"`.
2. Tạo `Models/TenModel.cs` — `public class TenModel` với method khớp tên đỉnh/cạnh JSON.
3. `service.RegisterModel<TenModel>()` trong `Program.cs`.
