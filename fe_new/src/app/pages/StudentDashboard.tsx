import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router';
import { BookOpen, Clock, Award, TrendingUp } from 'lucide-react';
import { courseApi, dashboardApi, registrationApi, CourseItem, RegistrationItem } from '../lib/api';
import { RegistrationStatusBadge } from '../components/RegistrationStatusBadge';
import { InsightModal } from '../components/InsightModal';

export function StudentDashboard() {
  const [stats, setStats] = useState<{ current_courses: number; registration_history: number } | null>(null);
  const [registrations, setRegistrations] = useState<RegistrationItem[]>([]);
  const [courses, setCourses] = useState<CourseItem[]>([]);
  const [error, setError] = useState('');
  const [selectedInsight, setSelectedInsight] = useState<'learning' | 'pending' | 'waitlist' | 'history' | null>(null);

  useEffect(() => {
    Promise.all([dashboardApi.student(), registrationApi.list(), courseApi.list()])
      .then(([studentStats, registrationItems, courseItems]) => {
        setStats(studentStats);
        setRegistrations(registrationItems);
        setCourses(courseItems);
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Không tải được dashboard'));
  }, []);

  const courseById = useMemo(() => {
    const map = new Map<number, CourseItem>();
    courses.forEach((course) => map.set(course.id, course));
    return map;
  }, [courses]);

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
            <h2 className="text-2xl mb-6">Khóa Học Đang Học</h2>
            <div className="space-y-4">
              {confirmedRegs.map((reg) => {
                const course = courseById.get(reg.course_id);
                if (!course) return null;

                return (
                  <div key={reg.id} className="bg-white border border-neutral-200 p-6">
                    <div className="flex items-start justify-between gap-6">
                      <div className="flex-1 min-w-0">
                        <h3 className="text-lg mb-1">{course.title}</h3>
                        <div className="text-sm text-neutral-600">{course.category || 'General'}</div>
                        <Link
                          to={`/learn/${course.id}`}
                          className="inline-block mt-4 px-6 py-2 rounded-xl bg-emerald-600 text-white hover:bg-emerald-700 transition-colors"
                        >
                          Tiếp Tục Học
                        </Link>
                      </div>

                      <RegistrationStatusBadge status={reg.status as any} />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

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
