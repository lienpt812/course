# Chạy từng bài test MBT (GraphWalker + Selenide)

Làm việc trong thư mục `test-web` (hoặc thêm `-f test-web/pom.xml` nếu chạy từ root repo).

## Điều kiện

- Backend FastAPI: `http://localhost:8000` (hoặc override bằng `-Dapi.base.url=...`)
- Frontend Vite: `http://localhost:3000` (hoặc `-Dweb.base.url=...`)

## WebDriver có cửa sổ trình duyệt (headed)

Profile `-Pheaded` tắt headless (`selenide.headless=false`) để bạn **nhìn thấy** Chrome điều khiển bởi Selenium/WebDriver.

Kết hợp với từng lệnh bên dưới, ví dụ:

```powershell
mvn -q verify -Pheaded "-Dit.test=**/AuthRunner.java"
```

Trên **Linux/macOS** (bash):

```bash
mvn -q verify -Pheaded '-Dit.test=**/AuthRunner.java'
```

Mặc định không có `-Pheaded` thì chạy **headless** (không hiện cửa sổ).

## Đổi trình duyệt (tuỳ chọn)

```text
-Dselenide.browser=chrome
-Dselenide.browser=edge
```

## Chạy riêng từng runner (integration test)

Các class nằm trong `src/test/java/com/eduplatform/mbt/runners/`. Dùng thuộc tính Failsafe `it.test` để chỉ chạy **một** class.

| Module | Runner | Lệnh (PowerShell) |
|--------|--------|-------------------|
| A — Auth | `AuthRunner` | `mvn -q verify -Pheaded "-Dit.test=**/AuthRunner.java"` |
| B — Course exploration | `CourseExplorationRunner` | `mvn -q verify "-Dit.test=**/CourseExplorationRunner.java"` |
| C — Registration | `RegistrationRunner` | `mvn -q verify -Pheaded "-Dit.test=**/RegistrationRunner.java"` |
| D — Learning | `LearningRunner` | `mvn -q verify -Pheaded "-Dit.test=**/LearningRunner.java"` |
| E — Certificate | `CertificateRunner` | `mvn -q verify "-Dit.test=**/CertificateRunner.java"` |
| F — Instructor content | `InstructorContentRunner` | `mvn -q verify -Pheaded "-Dit.test=**/InstructorContentRunner.java"` |
| G — Admin management | `AdminManagementRunner` | `mvn -q verify "-Dit.test=**/AdminManagementRunner.java"` |

### Có giao diện trình duyệt (headed)

Thêm `-Pheaded` vào **mỗi** lệnh, ví dụ Auth:

```powershell
mvn -q verify -Pheaded "-Dit.test=**/AuthRunner.java"
```

Ví dụ Admin:

```powershell
mvn -q verify -Pheaded "-Dit.test=**/AdminManagementRunner.java"
```

### Chỉ định class đầy đủ (tuỳ chọn)

```powershell
mvn -q verify "-Dit.test=com.eduplatform.mbt.runners.AuthRunner"
```

## Profile có sẵn trong `pom.xml` (cách khác)

Có thể dùng profile module (không cần `it.test`):

```powershell
mvn -q verify -Pmodule-auth
mvn -q verify -Pmodule-registration
mvn -q verify -Pmodule-learning
```

Các module còn lại hiện **không** có profile riêng — dùng bảng `it.test` ở trên.

## Chạy tất cả module MBT

```powershell
mvn -q verify
```

Headed:
mvn -o verify "-Dit.test=**/AuthRunner.java"

```powershell
mvn -q verify -Pheaded
```

## Báo cáo

- Failsafe: `target/failsafe-reports/`
- Ảnh Selenide khi lỗi: `target/selenide-reports/`
- Thống kê GraphWalker (coverage cạnh/đỉnh, danh sách chưa thăm): mỗi lần chạy xong một `*Runner`, log dòng `GraphWalker statistics [TênImpl]: ...` và ghi JSON đầy đủ vào `target/graphwalker-reports/<TênImpl>-<timestamp>.json`. Tắt ghi file: `-Dgraphwalker.statistics.file=false`; đổi thư mục: `-Dgraphwalker.report.dir=...`. Chi tiết đầy đủ trong log khi bật level DEBUG cho `com.eduplatform.mbt.runners`.

## Xem kết quả — pass bao nhiêu test?

### Vì sao dùng `-q` thì không thấy số liệu?

Tùy chọn **`-q` (quiet)** làm Maven **ẩn hầu hết log**, kể cả dòng tóm tắt Failsafe kiểu `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`. Muốn thấy ngay trên console thì **bỏ `-q`**:

```powershell
mvn verify "-Dit.test=**/AuthRunner.java"
```

Cuối log sẽ có (ví dụ):

```text
[INFO] Results:
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[...]
[INFO] BUILD SUCCESS
```

Chạy **cả 7 runner** (không lọc `it.test`):

```powershell
mvn verify
```

Khi thành công thường thấy **`Tests run: 7`** (mỗi file `*Runner` = **một** test method JUnit `@Test`).

### Một runner ≠ số bước GraphWalker

GraphWalker sinh **một** lần chạy model trong mỗi `*Runner` (một method `@Test`). Số **vertex/edge** (bước trong model) **không** hiện thành “Tests run: 30” trên Maven — chỉ thấy **1** test pass/fail cho mỗi runner. Chi tiết bước nằm trong log (logger `com.eduplatform.mbt.impl.*`).

### File tóm tắt XML

Sau `verify`, mở `target/failsafe-reports/failsafe-summary.xml`:

- `<completed>` — số test đã chạy xong
- `<failures>` / `<errors>` — số fail/error

### Báo cáo từng class

Từng runner cũng có file TXT/XML, ví dụ:

- `target/failsafe-reports/com.eduplatform.mbt.runners.AuthRunner.txt`

## Generator GraphWalker (coverage / độ dài chạy)

Mặc định mỗi `*Impl` dùng chuỗi trong `@GraphWalker(value=...)`. Có thể **ghi đè không cần sửa code**:

```powershell
mvn verify "-Dgraphwalker.generator=random(edge_coverage(100))"
```

Hoặc biến môi trường `GRAPHWALKER_GENERATOR`.

**Profile Maven** (trong `pom.xml`):

| Profile | Generator (gần đúng) |
|---------|----------------------|
| `gw-smoke` | `random(length(30))` — nhanh |
| `gw-full-edge` | `random(edge_coverage(100))` + timeout fork 7200s |
| `gw-full-edge-vertex` | `random(edge_coverage(100) && vertex_coverage(100))` + 10800s |

Ví dụ: `mvn verify -Pgw-full-edge "-Dit.test=**/AdminManagementRunner.java"`

**Lưu ý DSL:** `a_star(...)` chỉ dùng với `reached_vertex(tên)` hoặc `reached_edge(tên)`, **không** dùng `a_star(edge_coverage(100))` (sẽ lỗi `ClassCastException` khi parse). Để phủ cạnh/đỉnh dùng `random(edge_coverage(...))` hoặc `random(vertex_coverage(...))`.

## GraphWalker CLI — `offline` (sinh dãy bước, không cần Maven / Selenium)

Trong repo có JAR: **`mbt-gw/graphwalker-cli-4.3.3.jar`**. Lệnh tương đương `gw` là gọi:

```text
java -jar graphwalker-cli-4.3.3.jar offline [tùy chọn] -m <file.json> "<GENERATOR(điều_kiện_dừng)>" [-e <v_Start>]
```

Ví dụ từ thư mục **`mbt-gw`** (PowerShell, đường dẫn đầy đủ cho model 01_Auth):

```powershell
Set-Location d:\path\to\mbt-gw
java -jar graphwalker-cli-4.3.3.jar offline `
  -m "src/test/resources/com/eduplatform/mbt/models/01_AuthModel.json" `
  "random(edge_coverage(100) || length(50))" `
  -e v_GuestOnCourses
```

- **`-m`**: mỗi cặp là **(đường dẫn file JSON) + (chuỗi generator đứng ngay sau)** — generator là **một tham số riêng** (thường đặt trong dấu ngoặc kép).
- **`-e`**: đỉnh bắt đầu (khớp `startElementId` hoặc tên `v_...` trong model).
- **`-d <số>`**: seed — sinh cùng một đường đi mỗi lần.
- **`--unvisited -u`**: in thêm cạnh/đỉnh chưa thăm.

**Guards `gwGuard_*` với CLI:** MBT dùng tên method Java làm chuỗi guard. Trong **test Selenium** (`*Runner`), `BaseImpl` gọi đúng method Java. Trong **CLI**, guard được tính bằng **JavaScript** — các model JSON đã có `actions` ở cấp model (sinh bởi `scripts/inject_gw_cli_guard_stubs.py`) gán mỗi `gwGuard_* = true;` để `offline` / `check` không báo `ReferenceError`. (Đây là stub cho thăm đồ thị, không mô phỏng logic thật.)

Nếu thêm cạnh guard mới: chạy lại:

`python mbt-gw/scripts/inject_gw_cli_guard_stubs.py`

Ví dụ `methods` / `check`:

```powershell
java -jar graphwalker-cli-4.3.3.jar methods -m "src/test/resources/com/eduplatform/mbt/models/01_AuthModel.json"
java -jar graphwalker-cli-4.3.3.jar check -m "src/test/resources/com/eduplatform/mbt/models/01_AuthModel.json" "random(length(5))"
```

Để chạy **đầy đủ** guard + UI, vẫn dùng **`mvn verify`** với `*Runner` ở trên.

Tiện dụng: có script `mbt-gw/scripts/gw-offline.ps1` (wrapper `java -jar` + đường dẫn model mặc định → `.../models/`).

## Ghi chú

- `verify` gồm `integration-test` + `verify`; test MBT nằm ở Failsafe, **không** dùng `mvn test` cho các `*Runner`.
- Nếu IDE chạy từng class: chọn method `@Test` trong `*Runner` — vẫn cần backend/frontend và biến hệ thống giống Maven (hoặc cấu hình tương đương).
