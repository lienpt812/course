# AltWalker MBT Tests - C#/.NET + Selenium

## Cấu trúc project

```
tests/altwalker/
├── models/                          ← GraphWalker model files (JSON)
│   └── (symlink từ ../graphwalker/model/)
├── tests/                           ← C# executor project
│   ├── tests.csproj
│   ├── Program.cs                   ← Entry point, đăng ký tất cả models
│   ├── Setup.cs                     ← Global setUpRun / tearDownRun (WebDriver)
│   ├── Helpers/
│   │   ├── DriverFactory.cs         ← Tạo ChromeDriver
│   │   └── PageHelper.cs            ← Selenium helpers dùng chung
│   └── Models/                      ← 1 class per GraphWalker model
│       ├── Test_Auth.cs
│       ├── Test_Explorecourse.cs
│       ├── Registration_Course.cs
│       ├── Admin_Approval_Management.cs
│       ├── Create_Course.cs
│       ├── Content_Management.cs
│       ├── Update_Learning_Progress.cs
│       ├── Certification.cs
│       ├── Student_Dashboard.cs
│       ├── Notification.cs
│       └── Instructor_Dashboard.cs
└── README.md
```

## Mở trong Visual Studio

1. **File → Open → Project/Solution** → chọn `tests/altwalker/tests/tests.csproj`
2. **Build → Build Solution** (Ctrl+Shift+B)
3. Mở **View → Terminal** để chạy lệnh AltWalker

## Yêu cầu

- .NET 8 SDK
- Python 3.8+ (để chạy AltWalker CLI)
- Java 11+ (GraphWalker chạy bên trong AltWalker)
- Google Chrome + ChromeDriver (phiên bản khớp nhau)

### Cổng TCP (lỗi `bind` / `Address already in use`)

AltWalker CLI khởi động **host .NET trên cổng 5000** và **GraphWalker REST trên 8887**. Nếu một tiến trình khác đã chiếm (ví dụ `java -jar graphwalker-cli …` từ MBT khác, hoặc lần chạy AltWalker trước chưa thoát hết), bạn sẽ thấy lỗi tương tự `GraphWalker Service on port: 8887 … bind`.

Script **`run.ps1` tự `taskkill` theo PID trên các cổng 5000 và 8887** trước lần chạy đầu và **trước mỗi file model**, rồi chờ ngắn để socket được giải phóng. Không cần tự dừng GraphWalker trừ khi chạy `altwalker` thủ công ngoài script.

## Cài đặt

```bash
# 1. Cài AltWalker CLI
pip install altwalker

# 2. Restore .NET packages
cd tests/altwalker/tests
dotnet restore

# 3. Build project
dotnet build
```

## Chuẩn bị test data

`ApiHelper.SeedAll()` gọi `POST /admin/seed` rồi **xác minh** dữ liệu: tài khoản admin/student đăng nhập được, tồn tại khóa published slug `altwalker-e2e-cert`, outline có ít nhất 2 lesson. Nếu sau một lần seed lại vẫn không đạt → **throw** (test run dừng rõ ràng). Dùng `ApiHelper.TryVerifyTestSeedData(out reason)` nếu chỉ cần kiểm tra.

Đảm bảo backend đang chạy và có các tài khoản test:

| Role       | Email                  | Password      |
|------------|------------------------|---------------|
| Student    | student@test.com       | Password123!  |
| Instructor | instructor@test.com    | Password123!  |
| Admin      | admin@test.com         | Password123!  |

Tạo tài khoản test bằng API:
```bash
# Tạo student
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Student","email":"student@test.com","password":"Password123!","role":"STUDENT"}'

# Tạo instructor
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Instructor","email":"instructor@test.com","password":"Password123!","role":"INSTRUCTOR"}'

# Tạo admin
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Admin","email":"admin@test.com","password":"Password123!","role":"ADMIN"}'
```

## Chạy tests

### Dùng PowerShell script (khuyến nghị)
```powershell
# Verify tất cả models
.\tests\altwalker\run.ps1 -VerifyOnly

# Chạy 1 model cụ thể
.\tests\altwalker\run.ps1 -Model Test_Auth

# Chạy tất cả models
.\tests\altwalker\run.ps1

# Rebuild trước khi chạy
.\tests\altwalker\run.ps1 -Model Test_Auth -Rebuild
```

### Dùng lệnh thủ công
```bash
cd tests/altwalker
altwalker verify tests -l dotnet -m ../graphwalker/model/Test_Auth.json
```

### Chạy 1 model
```bash
cd tests/altwalker
altwalker online tests -l dotnet \
  -m ../graphwalker/model/Test_Auth.json \
  "random(edge_coverage(100))"
```

### Chạy nhiều models cùng lúc
```bash
cd tests/altwalker
altwalker online tests -l dotnet \
  -m ../graphwalker/model/Test_Auth.json "random(edge_coverage(100))" \
  -m ../graphwalker/model/Registration_Course.json "random(edge_coverage(100))" \
  -m ../graphwalker/model/Update_Learning_Progress.json "random(edge_coverage(100))"
```

### Chạy tất cả models
```bash
cd tests/altwalker
altwalker online tests -l dotnet \
  -m ../graphwalker/model/Test_Auth.json "random(edge_coverage(100))" \
  -m ../graphwalker/model/Test_Explorecourse.json "random(edge_coverage(100))" \
  -m ../graphwalker/model/Registration_Course.json "random(edge_coverage(100))" \
  -m "../graphwalker/model/Admin_ Approval_Management.json" "random(edge_coverage(100))" \
  -m ../graphwalker/model/Create_Course.json "random(edge_coverage(100))" \
  -m ../graphwalker/model/Content_Management.json "random(edge_coverage(100))" \
  -m ../graphwalker/model/Update_Learning_Progress.json "random(edge_coverage(100))" \
  -m ../graphwalker/model/Certification.json "random(edge_coverage(100))" \
  -m ../graphwalker/model/Student_Dashboard.json "random(edge_coverage(100))" \
  -m ../graphwalker/model/Notification.json "random(edge_coverage(100))" \
  -m ../graphwalker/model/Instructor_Dashboard.json "random(edge_coverage(100))"
```

### Chạy headless (CI/CD)
Trong `DriverFactory.cs`, đổi `headless: false` thành `headless: true`.

## Mapping Model → Class

| JSON Model                        | C# Class                      |
|-----------------------------------|-------------------------------|
| Test_Auth.json                    | Test_Auth                     |
| Test_Explorecourse.json           | Test_Explorecourse            |
| Registration_Course.json          | Registration_Course           |
| Admin_ Approval_Management.json   | Admin_Approval_Management     |
| Create_Course.json                | Create_Course                 |
| Content_Management.json           | Content_Management            |
| Update_Learning_Progress.json     | Update_Learning_Progress      |
| Certification.json                | Certification                 |
| Student_Dashboard.json            | Student_Dashboard             |
| Notification.json                 | Notification                  |
| Instructor_Dashboard.json         | Instructor_Dashboard          |

## Lưu ý

- Mỗi vertex/edge trong JSON model phải có method tương ứng trong C# class
- Method name phải khớp **chính xác** với `name` trong JSON (phân biệt hoa/thường)
- `setUpModel()` chạy trước mỗi model, `tearDownModel()` chạy sau
- `Setup.Driver` là WebDriver dùng chung toàn bộ run
- Dùng `altwalker verify` để kiểm tra mapping trước khi chạy
