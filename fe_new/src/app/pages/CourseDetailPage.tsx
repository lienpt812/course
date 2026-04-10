import { useEffect, useMemo, useState } from 'react';
import { useParams, Link } from 'react-router';
import { Clock, Users, Calendar, Award, ArrowLeft, AlertCircle } from 'lucide-react';
import { CourseDetail, courseApi, registrationApi, RegistrationItem } from '../lib/api';
import { RegistrationStatusBadge } from '../components/RegistrationStatusBadge';

function formatPrice(price: number | undefined): string {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price ?? 0);
}

function formatDate(value: string | null | undefined): string {
  if (!value) return '-';
  return new Date(value).toLocaleDateString('vi-VN');
}

export function CourseDetailPage() {
  const { courseId } = useParams<{ courseId: string }>();
  const [course, setCourse] = useState<CourseDetail | null>(null);
  const [registrations, setRegistrations] = useState<RegistrationItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRegistering, setIsRegistering] = useState(false);
  const [error, setError] = useState('');

  const authUserRaw = localStorage.getItem('auth_user');
  const authUser = authUserRaw ? JSON.parse(authUserRaw) : null;

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
      </div>

      <div className="max-w-7xl mx-auto px-6 py-12">
        <div className="grid grid-cols-3 gap-12">
          <div className="col-span-2">
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
                <button
                  onClick={handleRegister}
                  disabled={isRegistering}
                  className="w-full bg-emerald-600 text-white py-3 hover:bg-emerald-700 transition-colors disabled:bg-neutral-300 disabled:cursor-not-allowed mb-4 rounded-xl"
                >
                  {isRegistering ? 'Đang gửi...' : 'Đăng Ký Ngay'}
                </button>
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
    </div>
  );
}
