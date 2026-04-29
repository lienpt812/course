import { useEffect, useMemo, useState } from 'react';
import { Users, BookOpen, Star, TrendingUp, Plus } from 'lucide-react';
import { Link } from 'react-router';
import { courseApi, dashboardApi, learningApi, LearningOutlineItem } from '../lib/api';
import { InsightModal } from '../components/InsightModal';

interface InstructorDashboardData {
  total_courses: number;
  total_students: number;
  total_registrations: number;
  courses: Array<{
    id: number;
    title: string;
    status: string;
    current_participants: number;
    max_participants: number;
    estimated_hours: number;
    instructor_name: string;
  }>;
}

export function InstructorDashboard() {
  const [data, setData] = useState<InstructorDashboardData | null>(null);
  const [error, setError] = useState('');
  const [selectedInsight, setSelectedInsight] = useState<'courses' | 'students' | 'registrations' | 'fillRate' | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [isCreating, setIsCreating] = useState(false);

  const [title, setTitle] = useState('');
  const [slug, setSlug] = useState('');
  const [description, setDescription] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [category, setCategory] = useState('');
  const [maxCapacity, setMaxCapacity] = useState(30);
  const [estimatedHours, setEstimatedHours] = useState(20);
  const [level, setLevel] = useState('Beginner');
  const [status, setStatus] = useState<'DRAFT' | 'PUBLISHED' | 'COMING_SOON'>('DRAFT');

  const [selectedCourseId, setSelectedCourseId] = useState<string>('');
  const [outline, setOutline] = useState<LearningOutlineItem[]>([]);
  const [outlineError, setOutlineError] = useState('');

  const [sectionTitle, setSectionTitle] = useState('');
  const [sectionPosition, setSectionPosition] = useState(1);
  const [isCreatingSection, setIsCreatingSection] = useState(false);

  const [selectedSectionId, setSelectedSectionId] = useState<string>('');
  const [lessonTitle, setLessonTitle] = useState('');
  const [lessonType, setLessonType] = useState<'VIDEO' | 'QUIZ' | 'DOC' | 'TEXT'>('VIDEO');
  const [lessonDuration, setLessonDuration] = useState(10);
  const [lessonPosition, setLessonPosition] = useState(1);
  const [lessonContentUrl, setLessonContentUrl] = useState('');
  const [lessonIsPreview, setLessonIsPreview] = useState(false);
  const [isCreatingLesson, setIsCreatingLesson] = useState(false);

  const normalizeSlug = (value: string) =>
    value
      .toLowerCase()
      .trim()
      .replace(/[^a-z0-9\s-]/g, '')
      .replace(/\s+/g, '-')
      .replace(/-+/g, '-');

  const loadData = () => {
    dashboardApi
      .instructor()
      .then((response) => setData(response))
      .catch((err) => setError(err instanceof Error ? err.message : 'Không tải được dashboard giảng viên'));
  };

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    if (!selectedCourseId && data?.courses?.length) {
      setSelectedCourseId(String(data.courses[0].id));
    }
  }, [data, selectedCourseId]);

  useEffect(() => {
    if (!selectedCourseId) {
      setOutline([]);
      setSelectedSectionId('');
      return;
    }

    learningApi
      .outline(Number(selectedCourseId))
      .then((items) => {
        setOutline(items);
        setOutlineError('');
        if (items.length > 0 && !selectedSectionId) {
          setSelectedSectionId(String(items[0].section.id));
        }
      })
      .catch((err) => {
        setOutline([]);
        setOutlineError(err instanceof Error ? err.message : 'Không tải được outline');
      });
  }, [selectedCourseId]);

  const avgFillRate =
    data && data.courses.length > 0
      ? Math.round(
          (data.courses.reduce((sum, c) => sum + c.current_participants, 0) /
            data.courses.reduce((sum, c) => sum + (c.max_participants || 1), 0)) *
            100
        )
      : 0;

  const insightDetails = useMemo(() => {
    if (!data || !selectedInsight) return null;

    if (selectedInsight === 'courses') {
      return `Bạn đang quản lý ${data.total_courses} khóa học. ${data.courses.filter((c) => c.status === 'PUBLISHED').length} khóa đang PUBLISHED.`;
    }
    if (selectedInsight === 'students') {
      return `Tổng học viên đã được xác nhận: ${data.total_students}. Đây là tổng active participants trên tất cả khóa học của bạn.`;
    }
    if (selectedInsight === 'registrations') {
      return `Tổng số registrations hiện có: ${data.total_registrations}. Bao gồm cả đơn đã duyệt/chờ duyệt/hàng chờ.`;
    }
    return `Tỷ lệ lấp đầy trung bình hiện tại là ${avgFillRate}%. Theo dõi mục này để tối ưu sĩ số từng khóa.`;
  }, [data, selectedInsight, avgFillRate]);

  const handleCreateCourse = async () => {
    setError('');
    const cleanSlug = normalizeSlug(slug || title);
    const cleanTitle = title.trim();
    const cleanDescription = description.trim();
    const cleanImageUrl = imageUrl.trim();
    const cleanCategory = category.trim();

    if (cleanTitle.length < 3 || cleanTitle.length > 255) {
      setError('Tên khóa học phải từ 3 đến 255 ký tự');
      return;
    }
    if (cleanDescription.length < 20) {
      setError('Mô tả cần ít nhất 20 ký tự');
      return;
    }
    if (!cleanSlug) {
      setError('Slug không hợp lệ. Vui lòng dùng chữ cái và số');
      return;
    }
    if (cleanSlug.length > 255) {
      setError('Slug tối đa 255 ký tự');
      return;
    }
    if (cleanCategory.length > 100) {
      setError('Danh mục tối đa 100 ký tự');
      return;
    }
    if (cleanImageUrl && !/^https?:\/\//i.test(cleanImageUrl)) {
      setError('Ảnh khóa học phải là URL bắt đầu bằng http:// hoặc https://');
      return;
    }
    if (maxCapacity < 1) {
      setError('Sĩ số tối đa phải lớn hơn hoặc bằng 1');
      return;
    }
    if (maxCapacity > 10000) {
      setError('Sĩ số tối đa quá lớn (tối đa 10000)');
      return;
    }
    if (estimatedHours < 1 || estimatedHours > 1000) {
      setError('Số giờ học phải từ 1 đến 1000');
      return;
    }

    setIsCreating(true);
    try {
      await courseApi.create({
        title: cleanTitle,
        slug: cleanSlug,
        description: cleanDescription,
        image_url: cleanImageUrl || undefined,
        category: cleanCategory || undefined,
        max_capacity: maxCapacity,
        level,
        status,
        estimated_hours: estimatedHours,
        price: 0,
        certificate_enabled: false,
      });
      setShowCreateForm(false);
      setTitle('');
      setSlug('');
      setDescription('');
      setImageUrl('');
      setCategory('');
      setMaxCapacity(30);
      setEstimatedHours(20);
      setLevel('Beginner');
      setStatus('DRAFT');
      loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Tạo khóa học thất bại');
    } finally {
      setIsCreating(false);
    }
  };

  const handleCreateSection = async () => {
    if (!selectedCourseId) {
      setError('Vui lòng chọn khóa học để tạo section');
      return;
    }
    const cleanSectionTitle = sectionTitle.trim();
    if (cleanSectionTitle.length < 2 || cleanSectionTitle.length > 255) {
      setError('Tên section phải từ 2 đến 255 ký tự');
      return;
    }
    if (sectionPosition < 1) {
      setError('Vị trí section phải lớn hơn hoặc bằng 1');
      return;
    }

    setIsCreatingSection(true);
    setError('');
    try {
      const section = await learningApi.createSection({
        course_id: Number(selectedCourseId),
        title: cleanSectionTitle,
        position: sectionPosition,
      });
      const items = await learningApi.outline(Number(selectedCourseId));
      setOutline(items);
      setSelectedSectionId(String(section.id));
      setSectionTitle('');
      setSectionPosition(sectionPosition + 1);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Tạo section thất bại');
    } finally {
      setIsCreatingSection(false);
    }
  };

  const handleCreateLesson = async () => {
    if (!selectedSectionId) {
      setError('Vui lòng chọn section để tạo lesson');
      return;
    }
    const cleanLessonTitle = lessonTitle.trim();
    const cleanLessonContentUrl = lessonContentUrl.trim();
    if (cleanLessonTitle.length < 2 || cleanLessonTitle.length > 255) {
      setError('Tên lesson phải từ 2 đến 255 ký tự');
      return;
    }
    if (lessonPosition < 1) {
      setError('Thứ tự lesson phải lớn hơn hoặc bằng 1');
      return;
    }
    if (lessonDuration < 1 || lessonDuration > 600) {
      setError('Thời lượng lesson phải từ 1 đến 600 phút');
      return;
    }
    if (cleanLessonContentUrl && !/^https?:\/\//i.test(cleanLessonContentUrl)) {
      setError('Content URL phải bắt đầu bằng http:// hoặc https://');
      return;
    }

    setIsCreatingLesson(true);
    setError('');
    try {
      await learningApi.createLesson({
        section_id: Number(selectedSectionId),
        title: cleanLessonTitle,
        type: lessonType,
        content_url: cleanLessonContentUrl || undefined,
        is_preview: lessonIsPreview,
        position: lessonPosition,
        duration_minutes: lessonDuration,
      });
      const items = await learningApi.outline(Number(selectedCourseId));
      setOutline(items);
      setLessonTitle('');
      setLessonContentUrl('');
      setLessonDuration(10);
      setLessonPosition(lessonPosition + 1);
      setLessonIsPreview(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Tạo lesson thất bại');
    } finally {
      setIsCreatingLesson(false);
    }
  };

  const sectionOptions = outline.map((x) => x.section);

  return (
    <div className="bg-background min-h-screen">
      <div className="max-w-7xl mx-auto px-6 pt-7 pb-2">
        <h1 className="text-2xl mb-1 text-emerald-900">Dashboard Giảng Viên</h1>
        <p className="text-base text-emerald-700">Quản lý khóa học và học viên</p>
      </div>

      <div className="max-w-7xl mx-auto px-6 py-8">
        {error && <p className="mb-6 text-red-600">{error}</p>}

        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <button onClick={() => setSelectedInsight('courses')} className="bg-white border border-emerald-100 rounded-xl p-5 text-left hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-3">
              <div className="text-sm text-neutral-600">Khóa Học</div>
              <BookOpen className="w-5 h-5 text-emerald-600" />
            </div>
            <div className="text-3xl">{data?.total_courses ?? 0}</div>
          </button>

          <button onClick={() => setSelectedInsight('students')} className="bg-white border border-emerald-100 rounded-xl p-5 text-left hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-3">
              <div className="text-sm text-neutral-600">Tổng Học Viên</div>
              <Users className="w-5 h-5 text-emerald-600" />
            </div>
            <div className="text-3xl">{data?.total_students ?? 0}</div>
          </button>

          <button onClick={() => setSelectedInsight('registrations')} className="bg-white border border-emerald-100 rounded-xl p-5 text-left hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-3">
              <div className="text-sm text-neutral-600">Tổng Đăng Ký</div>
              <Star className="w-5 h-5 text-emerald-600" />
            </div>
            <div className="text-3xl">{data?.total_registrations ?? 0}</div>
          </button>

          <button onClick={() => setSelectedInsight('fillRate')} className="bg-white border border-emerald-100 rounded-xl p-5 text-left hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-3">
              <div className="text-sm text-neutral-600">Tỷ Lệ Lấp Đầy</div>
              <TrendingUp className="w-5 h-5 text-emerald-600" />
            </div>
            <div className="text-3xl">{avgFillRate}%</div>
          </button>
        </div>

        <InsightModal
          open={!!selectedInsight}
          title={
            selectedInsight === 'courses'
              ? 'Chi tiết tổng khóa học'
              : selectedInsight === 'students'
              ? 'Chi tiết tổng học viên'
              : selectedInsight === 'registrations'
              ? 'Chi tiết tổng đăng ký'
              : 'Chi tiết tỷ lệ lấp đầy'
          }
          description={insightDetails || ''}
          onClose={() => setSelectedInsight(null)}
        />

        <div className="mb-8">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-2xl">Khóa Học Của Tôi</h2>
            <button
              onClick={() => setShowCreateForm((prev) => !prev)}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-600 text-white hover:bg-emerald-700 transition-colors"
            >
              <Plus className="w-4 h-4" />
              Thêm khóa học
            </button>
          </div>

          {showCreateForm && (
            <div className="mb-6 bg-white border border-emerald-100 rounded-xl p-5 grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs mb-1.5 text-emerald-800">Tên khóa học <span className="text-red-600">*</span></label>
                <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Tên khóa học" className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl bg-emerald-50/40 focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100" required minLength={3} maxLength={255} />
              </div>
              <div>
                <label className="block text-xs mb-1.5 text-emerald-800">Slug</label>
                <input value={slug} onChange={(e) => setSlug(e.target.value)} placeholder="Để trống sẽ tự tạo theo tên" className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl bg-emerald-50/40 focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100" maxLength={255} />
              </div>
              <div>
                <label className="block text-xs mb-1.5 text-emerald-800">Danh mục</label>
                <input value={category} onChange={(e) => setCategory(e.target.value)} placeholder="Danh mục" className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl bg-emerald-50/40 focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100" maxLength={100} />
              </div>
              <div>
                <label className="block text-xs mb-1.5 text-emerald-800">Ảnh khóa học (URL)</label>
                <input value={imageUrl} onChange={(e) => setImageUrl(e.target.value)} placeholder="https://..." className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl bg-emerald-50/40 focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100" maxLength={1000} />
              </div>
              <div>
                <label className="block text-xs mb-1.5 text-emerald-800">Sĩ số <span className="text-red-600">*</span></label>
                <input
                  type="number"
                  min={1}
                  max={10000}
                  value={maxCapacity}
                  onChange={(e) => setMaxCapacity(Number(e.target.value))}
                  placeholder="Sĩ số"
                  className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl bg-emerald-50/40 focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
                />
              </div>
              <div>
                <label className="block text-xs mb-1.5 text-emerald-800">Số giờ học <span className="text-red-600">*</span></label>
                <input
                  type="number"
                  min={1}
                  max={1000}
                  value={estimatedHours}
                  onChange={(e) => setEstimatedHours(Number(e.target.value))}
                  placeholder="Số giờ học"
                  className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl bg-emerald-50/40 focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
                />
              </div>
              <div>
                <label className="block text-xs mb-1.5 text-emerald-800">Cấp độ <span className="text-red-600">*</span></label>
                <select value={level} onChange={(e) => setLevel(e.target.value)} className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl bg-emerald-50/40 focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100">
                  <option>Beginner</option>
                  <option>Intermediate</option>
                  <option>Advanced</option>
                </select>
              </div>
              <div>
                <label className="block text-xs mb-1.5 text-emerald-800">Trạng thái <span className="text-red-600">*</span></label>
                <select value={status} onChange={(e) => setStatus(e.target.value as any)} className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl bg-emerald-50/40 focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100">
                  <option value="DRAFT">DRAFT</option>
                  <option value="PUBLISHED">PUBLISHED</option>
                  <option value="COMING_SOON">COMING_SOON</option>
                </select>
              </div>
              <div className="md:col-span-2">
                <label className="block text-xs mb-1.5 text-emerald-800">Mô tả khóa học <span className="text-red-600">*</span></label>
                <textarea value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Mô tả khóa học" className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl bg-emerald-50/40 focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100" rows={3} minLength={20} maxLength={5000} />
              </div>
              <div className="md:col-span-2 flex gap-3">
                <button
                  onClick={handleCreateCourse}
                  disabled={isCreating || !title || !description}
                  className="px-4 py-2 rounded-xl bg-emerald-600 text-white hover:bg-emerald-700 disabled:bg-neutral-300"
                >
                  {isCreating ? 'Đang tạo...' : 'Lưu khóa học'}
                </button>
                <button onClick={() => setShowCreateForm(false)} className="px-4 py-2 rounded-xl border border-neutral-300 hover:bg-neutral-100">
                  Hủy
                </button>
              </div>
            </div>
          )}

          <div className="bg-white border border-emerald-100 rounded-2xl overflow-hidden shadow-sm shadow-emerald-100/40">
            <table className="w-full">
              <thead className="bg-emerald-50 border-b border-emerald-100">
                <tr>
                  <th className="px-6 py-3 text-left text-xs uppercase tracking-wide text-emerald-800">Tên Khóa Học</th>
                  <th className="px-6 py-3 text-left text-xs uppercase tracking-wide text-emerald-800">Giảng Viên</th>
                  <th className="px-6 py-3 text-left text-xs uppercase tracking-wide text-emerald-800">Giờ Học</th>
                  <th className="px-6 py-3 text-left text-xs uppercase tracking-wide text-emerald-800">Sĩ Số</th>
                  <th className="px-6 py-3 text-left text-xs uppercase tracking-wide text-emerald-800">Trạng Thái</th>
                  <th className="px-6 py-3 text-left text-xs uppercase tracking-wide text-emerald-800">Hành Động</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-emerald-50">
                {data?.courses.map((course) => {
                  const fillPercentage =
                    course.max_participants > 0
                      ? (course.current_participants / course.max_participants) * 100
                      : 0;

                  return (
                    <tr key={course.id} className="hover:bg-emerald-50/50">
                      <td className="px-6 py-4">
                        <div className="text-sm">{course.title}</div>
                      </td>
                      <td className="px-6 py-4 text-sm text-neutral-700">{course.instructor_name || 'Đang cập nhật'}</td>
                      <td className="px-6 py-4 text-sm text-neutral-700">{course.estimated_hours ?? 0}h</td>
                      <td className="px-6 py-4">
                        <div className="text-sm">
                          {course.current_participants}/{course.max_participants}
                        </div>
                        <div className="text-xs text-neutral-500">{Math.round(fillPercentage)}% lấp đầy</div>
                      </td>
                      <td className="px-6 py-4">
                        <span className="px-3 py-1 bg-emerald-50 text-emerald-700 border border-emerald-200 text-xs rounded-full">
                          {course.status}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <Link to={`/courses/${course.id}`} className="text-sm text-emerald-700 hover:underline">
                          Chỉnh Sửa
                        </Link>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>

            {!data?.courses.length && <div className="text-center py-12 text-neutral-500">Chưa có khóa học nào</div>}
          </div>
        </div>

        <div className="bg-white border border-emerald-100 rounded-2xl p-5 shadow-sm shadow-emerald-100/40">
          <h2 className="text-2xl mb-4">Quản Lý Nội Dung Khóa Học</h2>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="space-y-4">
              <div>
                <label className="block text-xs mb-1.5 text-emerald-800">Khóa học <span className="text-red-600">*</span></label>
                <select
                  data-testid="instructor-manage-course-select"
                  value={selectedCourseId}
                  onChange={(e) => setSelectedCourseId(e.target.value)}
                  className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl bg-emerald-50/40 focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
                >
                  <option value="">Chọn khóa học</option>
                  {data?.courses.map((course) => (
                    <option key={course.id} value={course.id}>{course.title}</option>
                  ))}
                </select>
              </div>

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
                  disabled={isCreatingSection || !selectedCourseId || !sectionTitle.trim()}
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
                      placeholder="Thứ tự"
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
                      placeholder="Thời lượng"
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
                {selectedCourseId && outline.length === 0 && <div className="p-4 text-sm text-neutral-500">Khóa học chưa có section.</div>}
                {!selectedCourseId && <div className="p-4 text-sm text-neutral-500">Chọn khóa học để quản lý nội dung.</div>}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
