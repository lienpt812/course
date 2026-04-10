
TÀI LIỆU TỔNG QUAN HỆ THỐNG
Hệ Thống Quản Lý Đăng Ký Khóa Học Trực Tuyến

Phiên bản:	1.0.0
Ngày lập:	Tháng 4, 2026
Loại tài liệu:	Tài liệu đặc tả nghiệp vụ (BRD)
Đối tượng:	Đội phát triển (Dev Team)

⚠  Tài liệu nội bộ – Không phát tán ra ngoài
 
1. TỔNG QUAN DỰ ÁN

1.1. Mục Tiêu Hệ Thống
Xây dựng nền tảng quản lý đăng ký khóa học trực tuyến cho các tổ chức đào tạo, trung tâm giáo dục hoặc doanh nghiệp nội bộ. Hệ thống hỗ trợ quản lý toàn bộ vòng đời của khóa học — từ tạo lập, công bố, đăng ký, phê duyệt, học tập cho đến đánh giá và cấp chứng chỉ.

•	Cho phép học viên tìm kiếm, đăng ký và theo dõi tiến độ học tập.
•	Giảng viên/Admin quản lý khóa học, phê duyệt đăng ký, kiểm soát sĩ số.
•	Hệ thống tự động xử lý danh sách chờ, thông báo và cấp chứng chỉ.
•	Đảm bảo tính nhất quán dữ liệu và ràng buộc nghiệp vụ nghiêm ngặt.

1.2. Phạm Vi Hệ Thống
Hệ thống bao gồm các phân hệ chính sau:

Phân Hệ	Mô Tả
Quản lý người dùng & phân quyền	Đăng ký tài khoản, đăng nhập, phân vai trò, quản lý hồ sơ
Quản lý khóa học	CRUD khóa học, phân loại, nội dung, tài liệu, video, sĩ số
Đăng ký & phê duyệt	Luồng đăng ký, chờ duyệt, xác nhận, danh sách chờ, hủy
Học tập & tiến độ	Truy cập bài học, theo dõi % hoàn thành, quiz, bài tập
Đánh giá & phản hồi	Rating, review, hỏi đáp, thảo luận, Q&A
Thông báo & truyền thông	Email, in-app notification, SMS (tuỳ chọn), nhắc nhở
Thanh toán (tuỳ chọn)	Tích hợp cổng thanh toán, hóa đơn, mã giảm giá
Báo cáo & thống kê	Dashboard, báo cáo đăng ký, hoàn thành, doanh thu
Cấp chứng chỉ	Tự động cấp và xác thực chứng chỉ khi hoàn thành khóa học

2. VAI TRÒ NGƯỜI DÙNG (USER ROLES)

Hệ thống có 4 vai trò chính với quyền hạn được phân cấp rõ ràng:

Vai Trò	Tên Hiển Thị	Mô Tả & Quyền Hạn
GUEST	Khách	Xem danh sách và thông tin khóa học công khai. Không thể đăng ký hoặc tương tác. Phải tạo tài khoản để tiếp tục.
STUDENT	Học viên	Đăng ký khóa học, học nội dung, làm bài tập, đánh giá, theo dõi tiến độ cá nhân, nhận chứng chỉ.
INSTRUCTOR	Giảng viên	Tạo và quản lý khóa học được phân công, xem danh sách học viên đã duyệt, đăng nội dung bài học, xem báo cáo tiến độ.
ADMIN	Quản trị viên	Toàn quyền: quản lý tài khoản, phê duyệt đăng ký, quản lý tất cả khóa học, cấu hình hệ thống, xem tất cả báo cáo.

3. NGHIỆP VỤ CHI TIẾT

3.1. Quản Lý Tài Khoản & Xác Thực
3.1.1. Đăng Ký Tài Khoản
1.	Người dùng nhập: họ tên, email, mật khẩu, số điện thoại (tuỳ chọn).
2.	Hệ thống gửi email xác thực — tài khoản chỉ được kích hoạt sau khi xác thực.
3.	Mặc định vai trò là STUDENT. INSTRUCTOR phải được Admin cấp quyền.
4.	Hỗ trợ đăng ký/đăng nhập bằng Google OAuth hoặc Microsoft SSO.

3.1.2. Quản Lý Hồ Sơ Cá Nhân
•	Học viên cập nhật avatar, tiểu sử, lĩnh vực quan tâm.
•	Quản lý lịch sử học tập, chứng chỉ đạt được, wishlist khóa học.
•	Đổi mật khẩu, quản lý thông báo, xóa tài khoản (soft delete).

3.1.3. Quên Mật Khẩu & Bảo Mật
•	Yêu cầu reset qua email — link có hiệu lực 30 phút.
•	Hỗ trợ xác thực 2 bước (2FA) bằng Authenticator App (tuỳ chọn).
•	Ghi log hoạt động đăng nhập bất thường, cảnh báo đăng nhập từ thiết bị mới.

3.2. Quản Lý Khóa Học
3.2.1. Tạo & Cấu Hình Khóa Học
Giảng viên/Admin tạo khóa học với các thuộc tính:

Thuộc Tính	Kiểu Dữ Liệu	Mô Tả
Tên khóa học	String (max 255)	Tên hiển thị, bắt buộc
Mô tả / Slug URL	Text / String	Mô tả chi tiết, URL thân thiện SEO
Danh mục / Thẻ tag	Enum / Array	Phân loại đa cấp, tìm kiếm nhanh
Sĩ số tối đa	Integer (≥ 1)	Số lượng học viên tối đa được xác nhận
Thời gian học ước tính	Integer (giờ)	Tổng giờ học dự kiến
Cấp độ	Enum	Beginner / Intermediate / Advanced
Trạng thái xuất bản	Enum	DRAFT / PUBLISHED / ARCHIVED
Ngày mở / đóng đăng ký	DateTime	Thời hạn nhận đăng ký
Học phí	Decimal / 0	0 = miễn phí; > 0 = có tính phí
Yêu cầu đầu vào	Text	Điều kiện học viên phải đáp ứng
Chứng chỉ sau hoàn thành	Boolean	Bật/tắt cấp chứng chỉ tự động

3.2.2. Quản Lý Nội Dung Bài Học
•	Khóa học gồm nhiều Chương (Section), mỗi chương có nhiều Bài học (Lesson).
•	Loại nội dung bài học: Video (upload/embed YouTube/Vimeo), PDF/Tài liệu, Quiz trắc nghiệm, Bài tập nộp file, Nội dung text/HTML, Link ngoài.
•	Hỗ trợ drag-and-drop sắp xếp thứ tự chương và bài học.
•	Đặt bài học là xem trước miễn phí (preview) để thu hút học viên.
•	Khóa bài học theo điều kiện (phải hoàn thành bài trước mới mở bài sau).

3.2.3. Trạng Thái Xuất Bản Khóa Học
•	DRAFT: Đang soạn thảo, chỉ Giảng viên/Admin thấy. Học viên không thể đăng ký.
•	PUBLISHED: Công khai, học viên có thể tìm kiếm và đăng ký.
•	ARCHIVED: Dừng hoạt động, không nhận đăng ký mới, dữ liệu lịch sử được giữ lại.
•	COMING SOON: Hiển thị công khai nhưng chưa mở đăng ký, cho phép đăng ký quan tâm (Interested).

3.3. Quy Trình Đăng Ký Khóa Học (Core Business Flow)

📌  Đây là nghiệp vụ trọng tâm — cần implement chính xác toàn bộ các trạng thái và chuyển đổi.
Mọi chuyển đổi trạng thái phải được ghi log với timestamp, actor và lý do thay đổi.

3.3.1. Sơ Đồ Trạng Thái Đăng Ký

Trạng Thái	Ký Hiệu	Ý Nghĩa
PENDING	⏳ Chờ duyệt	Học viên vừa nộp đơn, chưa được xử lý. Chiếm 1 chỗ "chờ", KHÔNG chiếm slot chính thức.
CONFIRMED	✅ Đã xác nhận	Admin duyệt, còn slot → học viên được vào học chính thức. Chiếm 1 slot trong sĩ số tối đa.
WAITLIST	🕐 Danh sách chờ	Admin duyệt nhưng hết slot → xếp hàng chờ. Có số thứ tự trong hàng chờ.
CANCELLED	❌ Đã hủy	Học viên chủ động hủy hoặc Admin hủy. Slot được giải phóng nếu ở trạng thái CONFIRMED.
REJECTED	🚫 Từ chối	Admin từ chối đơn (ví dụ: không đủ điều kiện đầu vào). Học viên nhận thông báo + lý do.
EXPIRED	💤 Hết hạn	Đơn PENDING quá X ngày không được xử lý → tự động chuyển EXPIRED.

3.3.2. Luồng Đăng Ký Chi Tiết
Bước 1: Học viên nộp đơn đăng ký
5.	Học viên vào trang chi tiết khóa học, bấm nút "Đăng ký ngay".
6.	Hệ thống kiểm tra: (a) học viên đã đăng nhập chưa, (b) khóa học đang mở đăng ký, (c) không có bản ghi PENDING/CONFIRMED hiện tại.
7.	Nếu hợp lệ → tạo bản ghi Registration với trạng thái PENDING.
8.	Gửi email xác nhận "Đơn đăng ký đã nhận" cho học viên.
9.	Gửi thông báo cho Admin/Giảng viên có đơn mới cần duyệt.

Bước 2: Admin xử lý đơn
10.	Admin vào trang quản lý đăng ký, xem danh sách đơn PENDING theo từng khóa.
11.	Admin có thể duyệt (Approve) hoặc từ chối (Reject) kèm lý do.
12.	
12.1.	Còn slot → chuyển CONFIRMED, gửi email "Đăng ký thành công + link vào học".
12.2.	Hết slot → chuyển WAITLIST, gửi email "Bạn đang ở hàng chờ số X".
13.	

Bước 3: Hủy đăng ký
14.	Học viên có thể hủy khi đơn đang PENDING → chuyển CANCELLED, không ảnh hưởng slot.
15.	Học viên hủy khi đang CONFIRMED → chuyển CANCELLED, giải phóng 1 slot.
16.	Học viên hủy khi đang WAITLIST → chuyển CANCELLED, cập nhật lại số thứ tự các người chờ phía sau.
17.	Admin có thể hủy bất kỳ đơn nào với lý do bắt buộc nhập.

Bước 4: Tự động kéo từ Waitlist
18.	Khi có slot trống (do hủy CONFIRMED hoặc Admin tăng sĩ số), hệ thống tự động trigger.
19.	Lấy người đầu tiên trong hàng chờ theo thứ tự (created_at ASC, position ASC).
20.	Chuyển trạng thái từ WAITLIST → CONFIRMED.
21.	Gửi email thông báo "Bạn đã được xác nhận vào học" kèm link truy cập.
22.	Nếu người đó không phản hồi trong 48h (tuỳ config) → có thể chuyển EXPIRED và kéo người tiếp theo.

3.3.3. Ràng Buộc Nghiệp Vụ Đăng Ký

⚠  Các ràng buộc sau phải được validate ở cả tầng Backend (API) và Frontend (UI). Không được bỏ qua ràng buộc nào.
•	Một học viên chỉ có TỐI ĐA 1 bản ghi đăng ký đang hoạt động (PENDING / CONFIRMED / WAITLIST) cho mỗi khóa học tại 1 thời điểm.
•	Sau khi CANCELLED hoặc REJECTED, học viên CÓ THỂ đăng ký lại (tạo bản ghi mới).
•	Slot được tính theo số bản ghi CONFIRMED — PENDING và WAITLIST không chiếm slot.
•	Phải dùng database transaction + lock khi xử lý slot để tránh race condition.
•	Khóa học hết hạn (ngày đóng đăng ký) → không tiếp nhận đơn mới.
•	Học viên bị khóa tài khoản không thể thực hiện đăng ký mới.

3.4. Học Tập & Theo Dõi Tiến Độ
3.4.1. Truy Cập Nội Dung
•	Chỉ học viên CONFIRMED mới truy cập được nội dung đầy đủ.
•	Bài học preview có thể xem tự do (cho cả Guest).
•	Hệ thống ghi nhận thời gian học, bài nào đã xem/hoàn thành.
•	Hỗ trợ tiếp tục học từ điểm dừng (resume playback cho video).

3.4.2. Theo Dõi Tiến Độ
•	Tỷ lệ hoàn thành = (số bài đã hoàn thành / tổng số bài) × 100%.
•	Mỗi bài học có ngưỡng hoàn thành riêng: video phải xem ≥ 80%, quiz phải đạt điểm tối thiểu, bài tập phải nộp.
•	Hiển thị progress bar trực quan trên trang danh sách khóa học và trang học.
•	Gửi thông báo nhắc nhở khi học viên không học trong X ngày liên tiếp.

3.4.3. Bài Kiểm Tra & Quiz
•	Hỗ trợ nhiều dạng câu hỏi: trắc nghiệm 1 đáp án, trắc nghiệm nhiều đáp án, điền vào chỗ trống, ghép đôi.
•	Cấu hình: số lần làm lại (max attempts), thời gian giới hạn, điểm đạt tối thiểu.
•	Hiển thị kết quả và giải thích đáp án sau khi nộp bài.
•	Bài tập nộp file: học viên upload, Giảng viên chấm điểm và cho feedback.

3.5. Hệ Thống Thông Báo
3.5.1. Các Sự Kiện Kích Hoạt Thông Báo

Sự Kiện	Đối Tượng Nhận	Kênh
Nộp đơn đăng ký thành công	Học viên	Email + In-app
Có đơn mới cần duyệt	Admin/Giảng viên	Email + In-app
Đăng ký được xác nhận (CONFIRMED)	Học viên	Email + In-app
Đăng ký vào danh sách chờ (WAITLIST)	Học viên	Email + In-app
Từ Waitlist lên CONFIRMED	Học viên	Email + In-app (Ưu tiên cao)
Đơn bị từ chối (REJECTED)	Học viên	Email
Học viên hủy đăng ký	Học viên + Admin	Email
Nhắc nhở học tập (X ngày không học)	Học viên	Email + In-app
Hoàn thành khóa học	Học viên	Email + In-app
Chứng chỉ được cấp	Học viên	Email (kèm PDF chứng chỉ)
Khóa học sắp bắt đầu (1-7 ngày)	Học viên CONFIRMED	Email

3.6. Đánh Giá & Tương Tác
3.6.1. Đánh Giá Khóa Học
•	Chỉ học viên CONFIRMED (đang hoặc đã học xong) mới được đánh giá.
•	Rating từ 1-5 sao + nhận xét văn bản (tối thiểu 20 ký tự).
•	Mỗi học viên chỉ đánh giá 1 lần, có thể chỉnh sửa trong 30 ngày.
•	Admin có thể ẩn review vi phạm. Điểm trung bình cập nhật real-time.

3.6.2. Q&A & Thảo Luận
•	Học viên đặt câu hỏi trong từng bài học.
•	Giảng viên và học viên khác có thể trả lời, upvote câu trả lời hữu ích.
•	Giảng viên đánh dấu câu trả lời chính thức (Official Answer).
•	Thông báo khi câu hỏi của học viên được trả lời.

3.7. Cấp Chứng Chỉ
•	Tự động cấp khi học viên hoàn thành 100% nội dung bắt buộc.
•	Chứng chỉ có mã xác thực duy nhất (UUID), có thể verify online qua URL công khai.
•	Xuất PDF với tên học viên, tên khóa học, ngày cấp, chữ ký điện tử của Giảng viên/Tổ chức.
•	Tích hợp chia sẻ lên LinkedIn dưới dạng Certification (optional).

3.8. Báo Cáo & Thống Kê (Dashboard)
3.8.1. Dashboard Học Viên
•	Khóa học đang học, tiến độ từng khóa, ngày học gần nhất.
•	Lịch sử đăng ký (tất cả trạng thái), chứng chỉ đã đạt.
•	Biểu đồ giờ học theo tuần/tháng.

3.8.2. Dashboard Giảng Viên
•	Tổng số học viên theo từng khóa, tỷ lệ hoàn thành, rating trung bình.
•	Danh sách học viên: CONFIRMED, WAITLIST, CANCELLED theo từng khóa.
•	Báo cáo tiến độ từng học viên (ai đang bị kẹt ở bài nào).

3.8.3. Dashboard Admin
•	Tổng quan toàn hệ thống: số khóa học, học viên, tỷ lệ lấp đầy sĩ số.
•	Đơn đăng ký đang chờ duyệt theo khóa (sorted by created_at ASC).
•	Thống kê doanh thu nếu có tính phí (tuỳ chọn).
•	Báo cáo xuất Excel: danh sách học viên, trạng thái đăng ký, tiến độ.

4. QUY TẮC NGHIỆP VỤ TỔNG HỢP


#	Quy Tắc	Xử Lý Vi Phạm
1	Học viên không đăng ký 2 lần cùng khóa ở trạng thái PENDING/CONFIRMED/WAITLIST	API trả 409 Conflict + thông báo rõ trạng thái hiện tại
2	Slot được tính theo CONFIRMED, tối đa = max_capacity	Khi confirmed_count = max_capacity → bản ghi tiếp theo phải vào WAITLIST
3	Admin tăng sĩ số → tự động kéo Waitlist lên CONFIRMED	Trigger sau khi Admin lưu thay đổi max_capacity
4	Đơn PENDING quá hạn (config X ngày) → tự động EXPIRED	Cronjob chạy hàng ngày, ghi log, KHÔNG tự động refund
5	Xử lý slot dùng database transaction + row-level lock (SELECT FOR UPDATE)	Tránh race condition khi 2 Admin duyệt cùng lúc
6	Học viên bị khóa tài khoản → tất cả CONFIRMED bị chuyển CANCELLED	Gửi thông báo giải thích, giải phóng slot, kéo Waitlist
7	Khóa học ARCHIVED → không tiếp nhận đăng ký mới, hủy toàn bộ PENDING	Giữ nguyên CONFIRMED để học viên tiếp tục học
8	Mọi thay đổi trạng thái phải ghi audit log	Lưu: actor, from_status, to_status, reason, timestamp

5. YÊU CẦU KỸ THUẬT & TÍCH HỢP

5.1. Yêu Cầu API
•	RESTful API (hoặc GraphQL) với versioning: /api/v1/...
•	Xác thực bằng JWT (Access Token 15 phút + Refresh Token 7 ngày).
•	Rate limiting: 100 req/min cho public, 500 req/min cho authenticated.
•	Response format chuẩn: { data, meta, errors }, HTTP status codes đúng chuẩn.
•	API documentation: Swagger/OpenAPI 3.0.

5.2. Tích Hợp Bên Ngoài
•	Email: SendGrid hoặc AWS SES — giao dịch email, template HTML có brand.
•	Storage: AWS S3 hoặc GCS — lưu trữ video, tài liệu, ảnh, chứng chỉ PDF.
•	Video: Tích hợp Vimeo/Cloudflare Stream hoặc tự host — streaming bảo mật.
•	Thanh toán: VNPay / Stripe / MoMo (tuỳ thị trường) — sandbox trước khi go-live.
•	OAuth: Google, Microsoft — đăng nhập nhanh.
•	Notification: Firebase Cloud Messaging (push mobile nếu có app) hoặc WebSocket.

5.3. Yêu Cầu Phi Chức Năng
•	Performance: Trang danh sách khóa học load < 2s; API response < 500ms p95.
•	Availability: Uptime ≥ 99.5%.
•	Security: HTTPS bắt buộc, OWASP Top 10, mã hóa dữ liệu nhạy cảm at-rest.
•	Scalability: Hệ thống phải scale được khi đồng thời 500 người học online.
•	Accessibility: Tuân thủ WCAG 2.1 AA cho giao diện web.
•	Mobile Responsive: Giao diện hoạt động tốt trên mobile và tablet.

6. MÔ HÌNH DỮ LIỆU (DATA MODEL — TÓM TẮT)

Các entity chính và quan hệ:

Entity	Các Trường & Quan Hệ Chính
User	id, name, email, password_hash, role (GUEST/STUDENT/INSTRUCTOR/ADMIN), status (ACTIVE/BANNED), avatar_url, created_at
Course	id, title, slug, description, instructor_id (FK→User), category_id, max_capacity, level, status, price, start_date, end_date, certificate_enabled
Section	id, course_id (FK→Course), title, position, created_at
Lesson	id, section_id (FK→Section), title, type (VIDEO/QUIZ/DOC/TEXT), content_url, is_preview, position, duration_minutes
Registration	id, user_id (FK→User), course_id (FK→Course), status (PENDING/CONFIRMED/WAITLIST/CANCELLED/REJECTED/EXPIRED), waitlist_position, created_at, updated_at, cancelled_by, cancel_reason
RegistrationLog	id, registration_id (FK→Registration), from_status, to_status, actor_id (FK→User), reason, created_at
Progress	id, user_id, lesson_id (FK→Lesson), completed, completion_pct, last_accessed_at
Quiz / QuizAttempt	Quiz: id, lesson_id, pass_score, max_attempts. Attempt: id, quiz_id, user_id, score, started_at, submitted_at
Review	id, user_id, course_id, rating (1-5), comment, is_visible, created_at
Certificate	id, user_id, course_id, issued_at, verification_code (UUID), pdf_url
Notification	id, user_id, type, title, body, is_read, ref_id, ref_type, created_at

💡  Index quan trọng cần tạo
•	(user_id, course_id, status) trên bảng Registration — dùng cho ràng buộc unique active registration.
•	(course_id, status, waitlist_position) — dùng cho query kéo Waitlist.
•	(user_id, lesson_id) UNIQUE trên bảng Progress.

7. YÊU CẦU GIAO DIỆN (UX/UI)

7.1. Các Trang Chính

Trang	Chức Năng Chính
Trang chủ / Landing	Giới thiệu, danh sách khóa nổi bật, tìm kiếm nhanh, CTA đăng ký
Danh sách khóa học	Lọc theo danh mục/cấp độ/giá/rating, sắp xếp, phân trang, view dạng grid/list
Chi tiết khóa học	Mô tả, giảng viên, nội dung (outline), review, nút đăng ký + trạng thái slot
Trang học (Classroom)	Player video, outline bài học sidebar, progress, notes, Q&A
Dashboard học viên	Khóa đang học, chứng chỉ, lịch sử đăng ký, thống kê cá nhân
Quản lý đăng ký (Admin)	Bảng đơn theo khóa, filter status, bulk approve/reject, timeline trạng thái
Quản lý khóa học (Admin/GV)	CRUD khóa, builder nội dung, cài đặt sĩ số, xem thống kê
Trang xác thực chứng chỉ	Public URL, nhập mã → hiển thị thông tin chứng chỉ hợp lệ/không hợp lệ

7.2. Nguyên Tắc UX
•	Trạng thái đăng ký phải hiển thị rõ ràng ở mọi nơi (badge màu sắc nhất quán).
•	Nút hành động thay đổi theo trạng thái: "Đăng ký" / "Chờ duyệt" / "Vào học" / "Hủy" / "Đã hủy".
•	Số slot còn trống hiển thị trên trang chi tiết khóa học ("Còn X / Y chỗ").
•	Khi hết slot: hiển thị "Hết chỗ — Tham gia danh sách chờ" thay vì ẩn nút đăng ký.
•	Mọi action destructive (hủy đăng ký) yêu cầu confirm dialog.
•	Loading state và error state phải được xử lý đầy đủ.

8. CÁC VẤN ĐỀ CẦN LÀM RÕ (OPEN QUESTIONS)


#	Câu Hỏi	Người Phụ Trách	Deadline
1	Khi học viên CONFIRMED hủy, slot có ưu tiên cho người vừa đăng ký mới hay WAITLIST cũ?	BA / PO	TBD
2	Thời hạn để người WAITLIST xác nhận trước khi hệ thống kéo người tiếp theo?	BA / PO	TBD
3	Giảng viên có được quyền tự duyệt đăng ký khóa của mình không?	BA / PO	TBD
4	Hệ thống có hỗ trợ khóa học miễn phí tự xác nhận ngay (không cần duyệt) không?	BA / PO	TBD
5	Chính sách refund khi học viên hủy sau khi đã thanh toán?	Legal / Finance	TBD
6	Có cần hỗ trợ đa ngôn ngữ (i18n) ngay từ v1 không?	PM	TBD
7	Email template dùng brand nào, có cần approval từ Marketing không?	Marketing	TBD

Tài liệu này là phiên bản 1.0 — sẽ được cập nhật sau khi Open Questions được giải đáp. Mọi thay đổi nghiệp vụ cần thông báo cho Tech Lead và BA trước khi implement.
