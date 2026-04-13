import { useEffect, useMemo, useState, useCallback } from 'react';
import { useNavigate, useParams, Link } from 'react-router';
import { Clock, Users, Calendar, Award, ArrowLeft, AlertCircle } from 'lucide-react';
import { CourseDetail, courseApi, registrationApi, RegistrationItem, learningApi, LearningOutlineItem } from '../lib/api';
import { RegistrationStatusBadge } from '../components/RegistrationStatusBadge';

function formatPrice(price: number | undefined): string {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price ?? 0);
}

function formatDate(value: string | null | undefined): string {
  if (!value) return '-';
  return new Date(value).toLocaleDateString('vi-VN');
}

// --- Outline Manager Component ---
function CourseOutlineManager({ courseId }: { courseId: number }) {
  const [outline, setOutline] = useState<LearningOutlineItem[]>([]);
  const [outlineError, setOutlineError] = useState('');
  const [sectionTitle, setSectionTitle] = useState('');
  const [sectionPosition, setSectionPosition] = useState(1);
  const [isCreatingSection, setIsCreatingSection] = useState(false);
  const [selectedSectionId, setSelectedSectionId] = useState('');
  const [lessonTitle, setLessonTitle] = useState('');
  const [lessonType, setLessonType] = useState<'VIDEO' | 'QUIZ' | 'DOC' | 'TEXT'>('VIDEO');
  const [lessonDuration, setLessonDuration] = useState(10);
  const [lessonPosition, setLessonPosition] = useState(1);
  const [lessonContentUrl, setLessonContentUrl] = useState('');
  const [lessonIsPreview, setLessonIsPreview] = useState(false);
  const [isCreatingLesson, setIsCreatingLesson] = useState(false);

  const fetchOutline = useCallback(() => {
    learningApi.outline(courseId)
      .then(setOutline)
      .catch(() => setOutlineError('Không tải được outline'));
  }, [courseId]);

  useEffect(() => {
    fetchOutline();
  }, [fetchOutline]);

  const handleCreateSection = async () => {
    if (!sectionTitle.trim()) return;
    setIsCreatingSection(true);
    try {
      await learningApi.createSection({ course_id: courseId, title: sectionTitle, position: sectionPosition });
      setSectionTitle('');
      setSectionPosition(1);
      fetchOutline();
    } finally {
      setIsCreatingSection(false);
    }
  };

  const handleCreateLesson = async () => {
    if (!selectedSectionId || !lessonTitle.trim()) return;
    setIsCreatingLesson(true);
    try {
      await learningApi.createLesson({
        section_id: Number(selectedSectionId),
        title: lessonTitle,
        type: lessonType,
        content_url: lessonContentUrl,
        is_preview: lessonIsPreview,
        position: lessonPosition,
        duration_minutes: lessonDuration,
      });
      setLessonTitle('');
      setLessonType('VIDEO');
      setLessonDuration(10);
      setLessonPosition(1);
      setLessonContentUrl('');
      setLessonIsPreview(false);
      fetchOutline();
    } finally {
      setIsCreatingLesson(false);
    }
  };

  const sectionOptions = outline.map((item) => item.section);

  return (
    <div className="bg-white border border-emerald-100 rounded-2xl p-5 shadow-sm shadow-emerald-100/40 mt-12">
      <h2 className="text-2xl mb-4">Quản Lý Nội Dung Khóa Học</h2>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="space-y-4">
          <div className="border border-emerald-100 rounded-xl p-4 bg-emerald-50/30">
            <h3 className="text-base mb-3 text-emerald-900">Tạo Section</h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <div className="md:col-span-2">
                <label className="block text-xs mb-1.5 text-emerald-800">Tên section <span className="text-red-600">*</span></label>
                <input
                  value={sectionTitle}
                  onChange={(e) => setSectionTitle(e.target.value)}
                  placeholder="Tên section"
                  className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl bg-white focus:outline-none focus:border-emerald-500"
                  minLength={2}
                  maxLength={255}
                />
              </div>
              <div>
                <label className="block text-xs mb-1.5 text-emerald-800">Vị trí <span className="text-red-600">*</span></label>
                <input
                  type="number"
                  min={1}
                  value={sectionPosition}
                  onChange={(e) => setSectionPosition(Number(e.target.value))}
                  className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl bg-white focus:outline-none focus:border-emerald-500"
                />
              </div>
            </div>
            <button
              onClick={handleCreateSection}
              disabled={isCreatingSection || !sectionTitle.trim()}
              className="mt-3 px-4 py-2 rounded-xl bg-emerald-600 text-white hover:bg-emerald-700 disabled:bg-neutral-300"
            >
              {isCreatingSection ? 'Đang tạo section...' : 'Lưu section'}
            </button>
          </div>

          <div className="border border-emerald-100 rounded-xl p-4 bg-emerald-50/30">
            <h3 className="text-base mb-3 text-emerald-900">Tạo Lesson</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs mb-1.5 text-emerald-800">Section <span className="text-red-600">*</span></label>
                <select
                  value={selectedSectionId}
                  onChange={(e) => setSelectedSectionId(e.target.value)}
                  className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl bg-white focus:outline-none focus:border-emerald-500"
                >
                  <option value="">Chọn section</option>
                  {sectionOptions.map((section) => (
                    <option key={section.id} value={section.id}>{section.title}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs mb-1.5 text-emerald-800">Loại lesson <span className="text-red-600">*</span></label>
                <select
                  value={lessonType}
                  onChange={(e) => setLessonType(e.target.value as 'VIDEO' | 'QUIZ' | 'DOC' | 'TEXT')}
                  className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl bg-white focus:outline-none focus:border-emerald-500"
                >
                  <option value="VIDEO">VIDEO</option>
                  <option value="TEXT">TEXT</option>
                  <option value="DOC">DOC</option>
                  <option value="QUIZ">QUIZ</option>
                </select>
              </div>
              <div className="md:col-span-2">
                <label className="block text-xs mb-1.5 text-emerald-800">Tên lesson <span className="text-red-600">*</span></label>
                <input
                  value={lessonTitle}
                  onChange={(e) => setLessonTitle(e.target.value)}
                  placeholder="Tên lesson"
                  className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl bg-white focus:outline-none focus:border-emerald-500"
                  minLength={2}
                  maxLength={255}
                />
              </div>
              <div>
                <label className="block text-xs mb-1.5 text-emerald-800">Thứ tự <span className="text-red-600">*</span></label>
                <input
                  type="number"
                  min={1}
                  value={lessonPosition}
                  onChange={(e) => setLessonPosition(Number(e.target.value))}
                  className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl bg-white focus:outline-none focus:border-emerald-500"
                />
              </div>
              <div>
                <label className="block text-xs mb-1.5 text-emerald-800">Thời lượng (phút) <span className="text-red-600">*</span></label>
                <input
                  type="number"
                  min={1}
                  max={600}
                  value={lessonDuration}
                  onChange={(e) => setLessonDuration(Number(e.target.value))}
                  className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl bg-white focus:outline-none focus:border-emerald-500"
                />
              </div>
              <input
                value={lessonContentUrl}
                onChange={(e) => setLessonContentUrl(e.target.value)}
                placeholder="Content URL (optional)"
                className="md:col-span-2 w-full px-4 py-2.5 border border-emerald-200 rounded-xl bg-white focus:outline-none focus:border-emerald-500"
              />
              <label className="md:col-span-2 inline-flex items-center gap-2 text-sm text-neutral-700">
                <input
                  type="checkbox"
                  checked={lessonIsPreview}
                  onChange={(e) => setLessonIsPreview(e.target.checked)}
                />
                Cho phép xem trước (preview)
              </label>
            </div>
            <button
              onClick={handleCreateLesson}
              disabled={isCreatingLesson || !selectedSectionId || !lessonTitle.trim()}
              className="mt-3 px-4 py-2 rounded-xl bg-emerald-600 text-white hover:bg-emerald-700 disabled:bg-neutral-300"
            >
              {isCreatingLesson ? 'Đang tạo lesson...' : 'Lưu lesson'}
            </button>
          </div>
        </div>

        <div>
          <h3 className="text-base mb-3 text-emerald-900">Outline hiện tại</h3>
          {outlineError && <p className="text-sm text-red-600 mb-3">{outlineError}</p>}
          <div className="border border-emerald-100 rounded-xl bg-white max-h-[520px] overflow-auto">
            {outline.map((item) => (
              <div key={item.section.id} className="border-b border-emerald-50 last:border-b-0">
                <div className="px-4 py-3 bg-emerald-50 text-sm text-emerald-900">
                  Section {item.section.position}: {item.section.title}
                </div>
                <div className="px-4 py-2">
                  {item.lessons.length === 0 && <div className="text-xs text-neutral-500 py-2">Chưa có lesson</div>}
                  {item.lessons.map((lesson) => (
                    <div key={lesson.id} className="py-2 text-sm flex items-center justify-between border-b border-neutral-100 last:border-b-0">
                      <div>{lesson.position}. {lesson.title}</div>
                      <div className="text-xs text-neutral-500">{lesson.type} • {lesson.duration_minutes} phút</div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
            {outline.length === 0 && <div className="p-4 text-sm text-neutral-500">Khóa học chưa có section.</div>}
          </div>
        </div>
      </div>
    </div>
  );
}

// --- Main Page Component ---
export function CourseDetailPage() {
  const { courseId } = useParams<{ courseId: string }>();
  const [course, setCourse] = useState<CourseDetail | null>(null);
  const [registrations, setRegistrations] = useState<RegistrationItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRegistering, setIsRegistering] = useState(false);
  const [error, setError] = useState('');
  const [isEditing, setIsEditing] = useState(false);
  const [editData, setEditData] = useState<any>(null);
  const [isSaving, setIsSaving] = useState(false);

  const authUserRaw = localStorage.getItem('auth_user');
  const authUser = authUserRaw ? JSON.parse(authUserRaw) : null;
  // Chỉ instructor mới được chỉnh sửa, và chỉ khóa học của chính mình
  const isInstructor = authUser?.role === 'INSTRUCTOR' && course?.instructor_id === authUser?.id;

  useEffect(() => {
    if (!courseId) return;
    const numericId = Number(courseId);
    setIsLoading(true);
    Promise.all([
      courseApi.detail(numericId),
      localStorage.getItem('access_token') ? registrationApi.list() : Promise.resolve([]),
    ])
      .then(([courseData, registrationData]) => {
        setCourse(courseData);
        setEditData(courseData);
        setRegistrations(registrationData as RegistrationItem[]);
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Không tải được khóa học'))
      .finally(() => setIsLoading(false));
  }, [courseId]);

  const registration = useMemo(() => {
    if (!authUser || !course) return null;
    return registrations.find((item) => item.course_id === course.id && item.user_id === authUser.id) || null;
  }, [authUser, course, registrations]);

  const hasActiveRegistration = registration && ['PENDING', 'CONFIRMED', 'WAITLIST'].includes(registration.status);

  const handleRegister = async () => {
    if (!course) return;
    setIsRegistering(true);
    setError('');
    try {
      await registrationApi.create(course.id);
      const registrationData = await registrationApi.list();
      setRegistrations(registrationData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Đăng ký thất bại');
    } finally {
      setIsRegistering(false);
    }
  };

  const handleEditChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    const checked = (e.target as HTMLInputElement).checked;
    setEditData((prev: any) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!course || !editData) return;
    setIsSaving(true);
    setError('');
    try {
      await courseApi.update(course.id, {
        title: editData.title,
        description: editData.description,
        image_url: editData.image_url || undefined,
        category: editData.category || undefined,
        max_capacity: Number(editData.max_capacity),
        estimated_hours: Number(editData.estimated_hours),
        level: editData.level,
        status: editData.status,
        price: Number(editData.price),
        certificate_enabled: editData.certificate_enabled,
        // Send null explicitly to clear dates, or ISO string if set
        registration_open_at: editData.registration_open_at ? editData.registration_open_at : undefined,
        registration_close_at: editData.registration_close_at ? editData.registration_close_at : undefined,
      });
      setIsEditing(false);
      const updated = await courseApi.detail(course.id);
      setCourse(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Cập nhật thất bại');
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return <div className="max-w-7xl mx-auto px-6 py-20 text-neutral-600">Đang tải chi tiết khóa học...</div>;
  }

  if (!course) {
    return (
      <div className="max-w-7xl mx-auto px-6 py-20 text-center">
        <h1 className="text-2xl mb-4">Không tìm thấy khóa học</h1>
        <Link to="/courses" className="text-emerald-700 hover:underline">
          Quay lại danh sách
        </Link>
      </div>
    );
  }

  return (
    <div>
      <div className="max-w-7xl mx-auto px-6 pt-6 pb-2">
        <Link to="/courses" className="inline-flex items-center gap-2 text-emerald-700 hover:text-emerald-900 mb-2">
          <ArrowLeft className="w-4 h-4" />
          Quay lại danh sách
        </Link>
        {isInstructor && !isEditing && (
          <button
            className="ml-4 px-4 py-2 bg-emerald-600 text-white rounded-xl hover:bg-emerald-700"
            onClick={() => setIsEditing(true)}
          >
            Chỉnh sửa
          </button>
        )}
      </div>

      <div className="max-w-7xl mx-auto px-6 py-12">
        <div className="grid grid-cols-3 gap-12">
          <div className="col-span-2">
            {!isEditing ? (
              <>
                <div className="mb-6">
                  {course.image_url && (
                    <div className="mb-5 overflow-hidden rounded-2xl border border-emerald-100">
                      <img src={course.image_url} alt={course.title} className="w-full h-72 object-cover" />
                    </div>
                  )}
                  <div className="flex items-center gap-3 mb-4">
                    <span className="px-3 py-1 bg-emerald-50 text-emerald-700 text-sm border border-emerald-200">
                      {course.category || 'General'}
                    </span>
                    <span className="px-3 py-1 bg-neutral-100 text-neutral-700 text-sm border border-neutral-300">
                      {course.level}
                    </span>
                    <span className="px-3 py-1 bg-neutral-100 text-neutral-700 text-sm border border-neutral-300">
                      {course.status}
                    </span>
                  </div>
                  <h1 className="text-4xl mb-4">{course.title}</h1>
                </div>
                <div className="prose max-w-none mb-12">
                  <h2 className="text-2xl mb-4">Về Khóa Học Này</h2>
                  <p className="text-neutral-700 leading-relaxed">{course.description}</p>
                </div>
              </>
            ) : (
              <form onSubmit={handleSave} className="space-y-4 bg-white border border-emerald-100 rounded-xl p-6">
                <div>
                  <label className="block text-xs mb-1.5 text-emerald-800">Tên khóa học</label>
                  <input name="title" value={editData?.title || ''} onChange={handleEditChange} className="w-full border rounded px-3 py-2" />
                </div>
                <div>
                  <label className="block text-xs mb-1.5 text-emerald-800">Mô tả</label>
                  <textarea name="description" value={editData?.description || ''} onChange={handleEditChange} className="w-full border rounded px-3 py-2" />
                </div>
                <div className="flex gap-4">
                  <div className="flex-1">
                    <label className="block text-xs mb-1.5 text-emerald-800">Trạng thái</label>
                    <select name="status" value={editData?.status || ''} onChange={handleEditChange} className="w-full border rounded px-3 py-2">
                      <option value="DRAFT">Nháp</option>
                      <option value="PUBLISHED">Công khai</option>
                      <option value="ARCHIVED">Lưu trữ</option>
                      <option value="COMING_SOON">Sắp mở</option>
                    </select>
                  </div>
                  <div className="flex-1">
                    <label className="block text-xs mb-1.5 text-emerald-800">Cấp chứng chỉ</label>
                    <input type="checkbox" name="certificate_enabled" checked={!!editData?.certificate_enabled} onChange={handleEditChange} />
                  </div>
                </div>
                <div className="flex gap-4">
                  <div className="flex-1">
                    <label className="block text-xs mb-1.5 text-emerald-800">Danh mục</label>
                    <input name="category" value={editData?.category || ''} onChange={handleEditChange} className="w-full border rounded px-3 py-2" />
                  </div>
                  <div className="flex-1">
                    <label className="block text-xs mb-1.5 text-emerald-800">Cấp độ</label>
                    <input name="level" value={editData?.level || ''} onChange={handleEditChange} className="w-full border rounded px-3 py-2" />
                  </div>
                </div>
                <div className="flex gap-4">
                  <div className="flex-1">
                    <label className="block text-xs mb-1.5 text-emerald-800">Giá</label>
                    <input
                      type="number"
                      name="price"
                      value={editData?.price ?? ''}
                      onChange={handleEditChange}
                      min={0}
                      placeholder="0"
                      className="w-full border rounded px-3 py-2"
                    />
                  </div>
                  <div className="flex-1">
                    <label className="block text-xs mb-1.5 text-emerald-800">Số lượng tối đa</label>
                    <input type="number" name="max_capacity" value={editData?.max_capacity || 0} onChange={handleEditChange} className="w-full border rounded px-3 py-2" />
                  </div>
                  <div className="flex-1">
                    <label className="block text-xs mb-1.5 text-emerald-800">Số giờ học</label>
                    <input type="number" name="estimated_hours" value={editData?.estimated_hours || 0} onChange={handleEditChange} className="w-full border rounded px-3 py-2" />
                  </div>
                </div>
                <div>
                  <label className="block text-xs mb-1.5 text-emerald-800">Ảnh đại diện (URL)</label>
                  <input name="image_url" value={editData?.image_url || ''} onChange={handleEditChange} className="w-full border rounded px-3 py-2" />
                </div>
                <div className="flex gap-4">
                  <div className="flex-1">
                    <label className="block text-xs mb-1.5 text-emerald-800">Mở đăng ký</label>
                    <input type="date" name="registration_open_at" value={editData?.registration_open_at?.slice(0, 10) || ''} onChange={handleEditChange} className="w-full border rounded px-3 py-2" />
                  </div>
                  <div className="flex-1">
                    <label className="block text-xs mb-1.5 text-emerald-800">Hạn đăng ký</label>
                    <input type="date" name="registration_close_at" value={editData?.registration_close_at?.slice(0, 10) || ''} onChange={handleEditChange} className="w-full border rounded px-3 py-2" />
                  </div>
                </div>
                <div className="flex gap-2 mt-4">
                  <button type="submit" className="px-4 py-2 bg-emerald-600 text-white rounded-xl hover:bg-emerald-700" disabled={isSaving}>
                    {isSaving ? 'Đang lưu...' : 'Lưu thay đổi'}
                  </button>
                  <button type="button" className="px-4 py-2 bg-neutral-200 text-neutral-700 rounded-xl hover:bg-neutral-300" onClick={() => setIsEditing(false)} disabled={isSaving}>
                    Hủy
                  </button>
                </div>
                {error && <div className="text-red-600 mt-2">{error}</div>}
              </form>
            )}
          </div>

          <div>
            <div className="bg-white border border-neutral-200 p-6 sticky top-24">
              <div className="text-3xl text-emerald-700 mb-6">{formatPrice(course.price)}</div>

              {registration && (
                <div className="mb-6">
                  <RegistrationStatusBadge
                    status={registration.status as any}
                    waitlistPosition={registration.waitlist_position || undefined}
                  />
                </div>
              )}

              {!localStorage.getItem('access_token') && (
                <Link
                  to="/login"
                  className="block w-full bg-emerald-600 text-white py-3 text-center hover:bg-emerald-700 transition-colors mb-4 rounded-xl"
                >
                  Đăng nhập để đăng ký
                </Link>
              )}

              {!hasActiveRegistration && authUser?.role === 'STUDENT' && (
                <>
                  {(() => {
                    const now = new Date();
                    const openAt = course.registration_open_at ? new Date(course.registration_open_at) : null;
                    const closeAt = course.registration_close_at ? new Date(course.registration_close_at) : null;
                    if (course.status !== 'PUBLISHED') {
                      return <p className="text-sm text-amber-600 mb-3">Khóa học chưa được công khai.</p>;
                    }
                    if (openAt && now < openAt) {
                      return <p className="text-sm text-amber-600 mb-3">Đăng ký mở từ {formatDate(course.registration_open_at)}.</p>;
                    }
                    if (closeAt && now > closeAt) {
                      return <p className="text-sm text-red-600 mb-3">Đã hết hạn đăng ký.</p>;
                    }
                    return null;
                  })()}
                  <button
                    onClick={handleRegister}
                    disabled={isRegistering || course.status !== 'PUBLISHED' || (() => {
                      const now = new Date();
                      const openAt = course.registration_open_at ? new Date(course.registration_open_at) : null;
                      const closeAt = course.registration_close_at ? new Date(course.registration_close_at) : null;
                      return (openAt !== null && now < openAt) || (closeAt !== null && now > closeAt);
                    })()}
                    className="w-full bg-emerald-600 text-white py-3 hover:bg-emerald-700 transition-colors disabled:bg-neutral-300 disabled:cursor-not-allowed mb-4 rounded-xl"
                  >
                    {isRegistering ? 'Đang gửi...' : 'Đăng Ký Ngay'}
                  </button>
                </>
              )}

              {registration?.status === 'CONFIRMED' && (
                <Link
                  to={`/learn/${course.id}`}
                  className="block w-full bg-green-600 text-white py-3 text-center hover:bg-green-700 transition-colors mb-4"
                >
                  Vào Học
                </Link>
              )}

              <div className="space-y-4 mb-6">
                <div className="flex items-center justify-between text-sm">
                  <div className="flex items-center gap-2 text-neutral-600">
                    <Users className="w-4 h-4" />
                    <span>Đã xác nhận</span>
                  </div>
                  <span>{course.confirmed_slots ?? 0}/{course.max_capacity}</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <div className="flex items-center gap-2 text-neutral-600">
                    <Clock className="w-4 h-4" />
                    <span>Hạn đăng ký</span>
                  </div>
                  <span>{formatDate(course.registration_close_at)}</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <div className="flex items-center gap-2 text-neutral-600">
                    <Calendar className="w-4 h-4" />
                    <span>Mở đăng ký</span>
                  </div>
                  <span>{formatDate(course.registration_open_at)}</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <div className="flex items-center gap-2 text-neutral-600">
                    <Award className="w-4 h-4" />
                    <span>Chứng chỉ</span>
                  </div>
                  <span>{course.certificate_enabled ? 'Có' : 'Không'}</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <div className="flex items-center gap-2 text-neutral-600">
                    <Users className="w-4 h-4" />
                    <span>Giảng viên</span>
                  </div>
                  <span>{course.instructor_name || 'Đang cập nhật'}</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <div className="flex items-center gap-2 text-neutral-600">
                    <Clock className="w-4 h-4" />
                    <span>Số giờ học</span>
                  </div>
                  <span>{course.estimated_hours ?? 0}h</span>
                </div>
              </div>

              {error && (
                <div className="bg-red-50 border border-red-200 p-4">
                  <div className="flex items-start gap-2">
                    <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0" />
                    <div className="text-sm text-red-800">{error}</div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Quản lý nội dung bài học cho giảng viên */}
      {isInstructor && course && (
        <div className="max-w-7xl mx-auto px-6 pb-12">
          <CourseOutlineManager courseId={course.id} />
        </div>
      )}
    </div>
  );
}
