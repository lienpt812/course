import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router';
import { BookOpen, Clock, Award, TrendingUp, ExternalLink, PlayCircle, CheckCircle, BookMarked } from 'lucide-react';
import { courseApi, dashboardApi, registrationApi, learningApi, CourseItem, RegistrationItem } from '../lib/api';
import { RegistrationStatusBadge } from '../components/RegistrationStatusBadge';
import { InsightModal } from '../components/InsightModal';

interface CertificateItem {
  id: number;
  course_id: number;
  verification_code: string;
  issued_at: string;
  pdf_url?: string;
}

type ProgressMap = Record<number, number>; // courseId -> completion_pct

export function StudentDashboard() {
  const [stats, setStats] = useState<{ current_courses: number; registration_history: number } | null>(null);
  const [registrations, setRegistrations] = useState<RegistrationItem[]>([]);
  const [courses, setCourses] = useState<CourseItem[]>([]);
  const [certificates, setCertificates] = useState<CertificateItem[]>([]);
  const [progressMap, setProgressMap] = useState<ProgressMap>({});
  const [error, setError] = useState('');
  const [selectedInsight, setSelectedInsight] = useState<'learning' | 'pending' | 'waitlist' | 'history' | null>(null);

  useEffect(() => {
    Promise.all([dashboardApi.student(), registrationApi.list(), courseApi.list(), learningApi.myCertificates()])
      .then(async ([studentStats, registrationItems, courseItems, certItems]) => {
        setStats(studentStats);
        setRegistrations(registrationItems);
        setCourses(courseItems);
        setCertificates(certItems as CertificateItem[]);

        // Fetch progress for all confirmed courses
        const confirmedIds = (registrationItems as RegistrationItem[])
          .filter((r) => r.status === 'CONFIRMED')
          .map((r) => r.course_id);

        if (confirmedIds.length > 0) {
          const results = await Promise.allSettled(
            confirmedIds.map((id) => learningApi.progress(id).then((p) => ({ id, pct: p.completion_pct })))
          );
          const map: ProgressMap = {};
          results.forEach((r) => { if (r.status === 'fulfilled') map[r.value.id] = r.value.pct; });
          setProgressMap(map);
        }
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Không tải được dashboard'));
  }, []);

  const courseById = useMemo(() => {
    const map = new Map<number, CourseItem>();
    courses.forEach((course) => map.set(course.id, course));
    return map;
  }, [courses]);

  const certCourseIds = useMemo(() => new Set(certificates.map((c) => c.course_id)), [certificates]);
  const confirmedRegs = registrations.filter((r) => r.status === 'CONFIRMED');
  const insightText = (() => {
    if (!selectedInsight) return '';
    if (selectedInsight === 'learning') return `Bạn đang học ${stats?.current_courses ?? 0} khóa.`;
    if (selectedInsight === 'pending') return `Hiện có ${registrations.filter((r) => r.status === 'PENDING').length} đơn đang chờ duyệt.`;
    if (selectedInsight === 'waitlist') return `Hiện có ${registrations.filter((r) => r.status === 'WAITLIST').length} đơn trong hàng chờ.`;
    return `Tổng lịch sử đăng ký của bạn: ${stats?.registration_history ?? 0}.`;
  })();

  return (
    <div className="bg-background min-h-screen">
      <div className="max-w-7xl mx-auto px-6 pt-7 pb-2">
        <h1 className="text-2xl mb-1 text-emerald-900">Dashboard Học Viên</h1>
        <p className="text-base text-emerald-700">Theo dõi khóa học và trạng thái đăng ký của bạn</p>
      </div>

      <div className="max-w-7xl mx-auto px-6 py-8">
        {error && <p className="mb-6 text-red-600">{error}</p>}

        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <button onClick={() => setSelectedInsight('learning')} className="bg-white border border-emerald-100 rounded-xl p-5 text-left hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-3">
              <div className="text-sm text-neutral-600">Đang Học</div>
              <BookOpen className="w-5 h-5 text-emerald-600" />
            </div>
            <div className="text-3xl">{stats?.current_courses ?? 0}</div>
          </button>

          <button onClick={() => setSelectedInsight('pending')} className="bg-white border border-emerald-100 rounded-xl p-5 text-left hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-3">
              <div className="text-sm text-neutral-600">Chờ Duyệt</div>
              <Clock className="w-5 h-5 text-amber-600" />
            </div>
            <div className="text-3xl">{registrations.filter((r) => r.status === 'PENDING').length}</div>
          </button>

          <button onClick={() => setSelectedInsight('waitlist')} className="bg-white border border-emerald-100 rounded-xl p-5 text-left hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-3">
              <div className="text-sm text-neutral-600">Hàng Chờ</div>
              <TrendingUp className="w-5 h-5 text-emerald-600" />
            </div>
            <div className="text-3xl">{registrations.filter((r) => r.status === 'WAITLIST').length}</div>
          </button>

          <button onClick={() => setSelectedInsight('history')} className="bg-white border border-emerald-100 rounded-xl p-5 text-left hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-3">
              <div className="text-sm text-neutral-600">Lịch Sử</div>
              <Award className="w-5 h-5 text-green-600" />
            </div>
            <div className="text-3xl">{stats?.registration_history ?? 0}</div>
          </button>
        </div>

        <InsightModal
          open={!!selectedInsight}
          title={
            selectedInsight === 'learning'
              ? 'Chi tiết khóa đang học'
              : selectedInsight === 'pending'
              ? 'Chi tiết đơn chờ duyệt'
              : selectedInsight === 'waitlist'
              ? 'Chi tiết hàng chờ'
              : 'Chi tiết lịch sử đăng ký'
          }
          description={insightText}
          onClose={() => setSelectedInsight(null)}
        />

        {confirmedRegs.length > 0 && (
          <div className="mb-12">
            <h2 className="text-2xl mb-6">Khóa Học Của Tôi</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {confirmedRegs.map((reg) => {
                const course = courseById.get(reg.course_id);
                if (!course) return null;
                const isCompleted = certCourseIds.has(reg.course_id);
                const pct = progressMap[reg.course_id] ?? 0;

                return (
                  <div key={reg.id} className="bg-white border border-neutral-200 rounded-2xl p-5 shadow-sm hover:shadow-md transition-shadow">
                    {/* Course image */}
                    {course.image_url ? (
                      <img src={course.image_url} alt={course.title} className="w-full h-32 object-cover rounded-xl mb-4" />
                    ) : (
                      <div className="w-full h-32 bg-gradient-to-br from-emerald-100 to-teal-100 rounded-xl mb-4 flex items-center justify-center">
                        <BookOpen className="w-10 h-10 text-emerald-400" />
                      </div>
                    )}

                    <div className="text-xs text-neutral-500 mb-1">{course.category || 'General'} • {course.level}</div>
                    <h3 className="font-semibold text-neutral-900 mb-3 line-clamp-2">{course.title}</h3>

                    {isCompleted ? (
                      // Đã hoàn thành
                      <>
                        <div className="h-1.5 bg-emerald-100 rounded-full mb-2">
                          <div className="h-full bg-emerald-500 rounded-full w-full" />
                        </div>
                        <div className="flex items-center justify-between text-xs text-neutral-500 mb-3">
                          <span>Tiến trình</span>
                          <span className="text-emerald-600 font-medium">100%</span>
                        </div>
                        <div className="flex items-center justify-center gap-2 w-full py-2 bg-green-50 border border-green-200 rounded-xl text-green-700 text-sm font-medium">
                          <CheckCircle className="w-4 h-4" />
                          Đã hoàn thành
                        </div>
                      </>
                    ) : (
                      // Đang học
                      <>
                        <div className="h-1.5 bg-neutral-100 rounded-full mb-2">
                          <div className="h-full bg-emerald-500 rounded-full transition-all" style={{ width: `${pct}%` }} />
                        </div>
                        <div className="flex items-center justify-between text-xs text-neutral-500 mb-3">
                          <span>Tiến trình</span>
                          <span className="text-emerald-600 font-medium">{pct}%</span>
                        </div>
                        <Link
                          to={`/learn/${course.id}`}
                          className="flex items-center justify-center gap-2 w-full py-2 bg-emerald-600 text-white rounded-xl hover:bg-emerald-700 transition-colors text-sm font-medium"
                        >
                          <PlayCircle className="w-4 h-4" />
                          Tiếp tục học
                        </Link>
                      </>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        )}

        <div>
          <h2 className="text-2xl mb-6">Chứng Chỉ Của Tôi</h2>
          {certificates.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-12">
              {certificates.map((cert) => {
                const course = courseById.get(cert.course_id);
                return (
                  <div key={cert.id} className="bg-white border border-yellow-200 rounded-2xl p-6 shadow-sm">
                    <div className="flex items-center gap-3 mb-4">
                      <div className="w-12 h-12 bg-yellow-100 rounded-full flex items-center justify-center flex-shrink-0">
                        <Award className="w-6 h-6 text-yellow-500" />
                      </div>
                      <div>
                        <p className="font-semibold text-neutral-900 text-sm line-clamp-2">
                          {course?.title ?? `Khóa học #${cert.course_id}`}
                        </p>
                        <p className="text-xs text-neutral-500 mt-0.5">
                          Cấp ngày: {new Date(cert.issued_at).toLocaleDateString('vi-VN')}
                        </p>
                      </div>
                    </div>
                    <div className="text-xs text-neutral-400 font-mono bg-neutral-50 rounded-lg px-3 py-2 mb-3 truncate">
                      Mã: {cert.verification_code}
                    </div>
                    <a
                      href={`/api/v1/certificates/verify/${cert.verification_code}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1.5 text-sm text-emerald-700 hover:text-emerald-900 font-medium"
                    >
                      <ExternalLink className="w-3.5 h-3.5" />
                      Xác minh chứng chỉ
                    </a>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="bg-white border border-neutral-100 rounded-2xl p-8 text-center text-neutral-500 mb-12">
              <Award className="w-10 h-10 mx-auto mb-3 text-neutral-300" />
              <p>Bạn chưa có chứng chỉ nào. Hoàn thành khóa học để nhận chứng chỉ!</p>
            </div>
          )}
        </div>

        <div>
          <h2 className="text-2xl mb-6">Lịch Sử Đăng Ký</h2>
          <div className="bg-white border border-emerald-100 rounded-2xl overflow-hidden shadow-sm shadow-emerald-100/40">
            <table className="w-full">
              <thead className="bg-emerald-50 border-b border-emerald-100">
                <tr>
                  <th className="px-6 py-3 text-left text-xs uppercase tracking-wide text-emerald-800">Khóa Học</th>
                  <th className="px-6 py-3 text-left text-xs uppercase tracking-wide text-emerald-800">Ngày Đăng Ký</th>
                  <th className="px-6 py-3 text-left text-xs uppercase tracking-wide text-emerald-800">Trạng Thái</th>
                  <th className="px-6 py-3 text-left text-xs uppercase tracking-wide text-emerald-800">Hành Động</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-emerald-50">
                {registrations.map((reg) => {
                  const course = courseById.get(reg.course_id);
                  if (!course) return null;

                  return (
                    <tr key={reg.id} className="hover:bg-emerald-50/50">
                      <td className="px-6 py-4">
                        <div className="text-sm">{course.title}</div>
                      </td>
                      <td className="px-6 py-4 text-sm text-neutral-600">
                        {new Date(reg.created_at).toLocaleDateString('vi-VN')}
                      </td>
                      <td className="px-6 py-4">
                        <RegistrationStatusBadge
                          status={reg.status as any}
                          waitlistPosition={reg.waitlist_position || undefined}
                        />
                      </td>
                      <td className="px-6 py-4">
                        <Link to={`/courses/${course.id}`} className="text-sm text-emerald-700 hover:underline">
                          Xem Chi Tiết
                        </Link>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>

            {registrations.length === 0 && (
              <div className="text-center py-12 text-neutral-500">Bạn chưa đăng ký khóa học nào</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
