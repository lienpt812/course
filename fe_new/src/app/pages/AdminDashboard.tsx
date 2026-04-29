import { useEffect, useMemo, useState } from 'react';
import { useLocation } from 'react-router';
import { CheckCircle, XCircle, Users, Clock, AlertCircle } from 'lucide-react';
import { courseApi, dashboardApi, registrationApi, CourseItem, RegistrationItem } from '../lib/api';
import { RegistrationStatusBadge } from '../components/RegistrationStatusBadge';
import { InsightModal } from '../components/InsightModal';

export function AdminDashboard() {
  const location = useLocation();
  const [selectedCourse, setSelectedCourse] = useState<string>('all');
  const [selectedStatus, setSelectedStatus] = useState<string>('all');
  const [dashboard, setDashboard] = useState<{ total_courses: number; total_users: number; pending_registrations: number } | null>(null);
  const [courses, setCourses] = useState<CourseItem[]>([]);
  const [registrations, setRegistrations] = useState<RegistrationItem[]>([]);
  const [error, setError] = useState('');
  const [isRunningJob, setIsRunningJob] = useState(false);
  const [infoMessage, setInfoMessage] = useState('');
  const [bulkRejectedItems, setBulkRejectedItems] = useState<
    Array<{
      registration_id: number;
      user_id: number;
      user_name?: string | null;
      user_email?: string | null;
      course_id: number;
      course_title?: string | null;
      reason: string;
    }>
  >([]);
  const [selectedInsight, setSelectedInsight] = useState<'pending' | 'confirmed' | 'waitlist' | 'registrations' | 'courses' | 'users' | null>(null);

  const loadData = () => {
    Promise.all([dashboardApi.admin(), registrationApi.list(), courseApi.list()])
      .then(([dashboardData, registrationItems, courseItems]) => {
        setDashboard(dashboardData);
        setRegistrations(registrationItems);
        setCourses(courseItems);
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Không tải được dashboard admin'));
  };

  useEffect(() => {
    loadData();
  }, [location.key]);

  const pendingRegs = registrations.filter((r) => r.status === 'PENDING');
  const confirmedRegs = registrations.filter((r) => r.status === 'CONFIRMED');
  const waitlistRegs = registrations.filter((r) => r.status === 'WAITLIST');

  const courseById = useMemo(() => {
    const map = new Map<number, CourseItem>();
    courses.forEach((course) => map.set(course.id, course));
    return map;
  }, [courses]);

  const filteredRegistrations = registrations.filter((reg) => {
    const matchesCourse = selectedCourse === 'all' || String(reg.course_id) === selectedCourse;
    const matchesStatus = selectedStatus === 'all' || reg.status === selectedStatus;
    return matchesCourse && matchesStatus;
  });

  const insightText = (() => {
    if (!selectedInsight) return '';
    if (selectedInsight === 'pending') return `Hiện có ${pendingRegs.length} đơn chờ duyệt, ưu tiên xử lý theo thời gian tạo tăng dần.`;
    if (selectedInsight === 'confirmed') return `Hiện có ${confirmedRegs.length} đơn đã xác nhận.`;
    if (selectedInsight === 'waitlist') return `Hiện có ${waitlistRegs.length} đơn trong danh sách chờ.`;
    if (selectedInsight === 'registrations') return `Tổng số đăng ký toàn hệ thống: ${registrations.length}.`;
    if (selectedInsight === 'courses') return `Tổng khóa học hiện có: ${dashboard?.total_courses ?? 0}.`;
    return `Tổng users hiện có: ${dashboard?.total_users ?? 0}.`;
  })();

  const handleApprove = async (regId: number) => {
    try {
      await registrationApi.approve(regId);
      loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Duyệt đăng ký thất bại');
    }
  };

  const handleReject = async (regId: number) => {
    try {
      await registrationApi.reject(regId, 'Rejected by admin');
      loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Từ chối đăng ký thất bại');
    }
  };

  return (
    <div className="bg-background min-h-screen">
      <div className="max-w-7xl mx-auto px-6 pt-7 pb-2">
        <h1 className="text-2xl mb-1 text-emerald-900">Quản Trị Hệ Thống</h1>
        <p className="text-base text-emerald-700">Quản lý đăng ký khóa học</p>
      </div>

      <div className="max-w-7xl mx-auto px-6 py-8">
        {error && <p className="mb-6 text-red-600">{error}</p>}
        {infoMessage && <p className="mb-6 text-emerald-700">{infoMessage}</p>}

        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-6">
          <button onClick={() => setSelectedInsight('pending')} className="bg-white border border-emerald-100 rounded-xl p-5 text-left hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-3">
              <div className="text-sm text-neutral-600">Chờ Duyệt</div>
              <Clock className="w-5 h-5 text-amber-600" />
            </div>
            <div className="text-3xl">{pendingRegs.length}</div>
            <div className="text-xs text-neutral-500 mt-1">Cần xử lý ngay</div>
          </button>

          <button onClick={() => setSelectedInsight('confirmed')} className="bg-white border border-emerald-100 rounded-xl p-5 text-left hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-3">
              <div className="text-sm text-neutral-600">Đã Xác Nhận</div>
              <CheckCircle className="w-5 h-5 text-green-600" />
            </div>
            <div className="text-3xl">{confirmedRegs.length}</div>
            <div className="text-xs text-neutral-500 mt-1">Đang học</div>
          </button>

          <button onClick={() => setSelectedInsight('waitlist')} className="bg-white border border-emerald-100 rounded-xl p-5 text-left hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-3">
              <div className="text-sm text-neutral-600">Hàng Chờ</div>
              <Users className="w-5 h-5 text-emerald-600" />
            </div>
            <div className="text-3xl">{waitlistRegs.length}</div>
            <div className="text-xs text-neutral-500 mt-1">Chờ chỗ trống</div>
          </button>

          <button onClick={() => setSelectedInsight('registrations')} className="bg-white border border-emerald-100 rounded-xl p-5 text-left hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-3">
              <div className="text-sm text-neutral-600">Tổng Đăng Ký</div>
              <AlertCircle className="w-5 h-5 text-purple-600" />
            </div>
            <div className="text-3xl">{registrations.length}</div>
          </button>

          <button onClick={() => setSelectedInsight('courses')} className="bg-white border border-emerald-100 rounded-xl p-5 text-left hover:shadow-md transition-shadow">
            <div className="text-sm text-neutral-600 mb-3">Tổng Khóa Học</div>
            <div className="text-3xl">{dashboard?.total_courses ?? 0}</div>
          </button>

          <button onClick={() => setSelectedInsight('users')} className="bg-white border border-emerald-100 rounded-xl p-5 text-left hover:shadow-md transition-shadow">
            <div className="text-sm text-neutral-600 mb-3">Tổng Users</div>
            <div className="text-3xl">{dashboard?.total_users ?? 0}</div>
          </button>
        </div>

        <InsightModal
          open={!!selectedInsight}
          title={
            selectedInsight === 'pending'
              ? 'Chi tiết đơn chờ duyệt'
              : selectedInsight === 'confirmed'
              ? 'Chi tiết đơn đã xác nhận'
              : selectedInsight === 'waitlist'
              ? 'Chi tiết danh sách chờ'
              : selectedInsight === 'registrations'
              ? 'Chi tiết tổng đăng ký'
              : selectedInsight === 'courses'
              ? 'Chi tiết tổng khóa học'
              : 'Chi tiết tổng users'
          }
          description={insightText}
          onClose={() => setSelectedInsight(null)}
        />

        <div className="bg-white border border-emerald-100 rounded-2xl p-4 mb-6 shadow-sm shadow-emerald-100/30">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 items-end">
            <div>
              <label className="block text-xs mb-1.5 text-emerald-800">Khóa học</label>
              <select
                id="admin-filter-course-select"
                data-testid="admin-filter-course-select"
                value={selectedCourse}
                onChange={(e) => setSelectedCourse(e.target.value)}
                className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl focus:outline-none focus:border-emerald-500 bg-white"
              >
                <option value="all">Tất cả khóa học</option>
                {courses.map((course) => (
                  <option key={course.id} value={course.id}>{course.title}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs mb-1.5 text-emerald-800">Trạng thái</label>
              <select
                id="admin-filter-status-select"
                data-testid="admin-filter-status-select"
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl focus:outline-none focus:border-emerald-500 bg-white"
              >
                <option value="all">Tất cả trạng thái</option>
                <option value="PENDING">Chờ duyệt</option>
                <option value="CONFIRMED">Đã xác nhận</option>
                <option value="WAITLIST">Hàng chờ</option>
                <option value="CANCELLED">Đã hủy</option>
                <option value="REJECTED">Từ chối</option>
                <option value="EXPIRED">Hết hạn</option>
              </select>
            </div>

            <div className="flex items-end">
              <div className="text-sm text-emerald-800 bg-emerald-50 border border-emerald-100 rounded-xl px-3 py-2 w-full text-center">
                Hiển thị {filteredRegistrations.length} đơn đăng ký
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white border border-emerald-100 rounded-2xl overflow-hidden shadow-sm shadow-emerald-100/40">
          <div className="border-b border-emerald-100 px-6 py-4 flex items-center justify-between">
            <h2 className="text-xl">Danh Sách Đăng Ký</h2>
            <button
              type="button"
              data-testid="admin-bulk-approve"
              onClick={async () => {
                setIsRunningJob(true);
                setError('');
                setInfoMessage('');
                setBulkRejectedItems([]);
                try {
                  const result = await registrationApi.bulkApprove({
                    course_id: selectedCourse === 'all' ? undefined : Number(selectedCourse),
                    reason: 'Bulk approved by admin',
                  });
                  setInfoMessage(result.message);
                  setBulkRejectedItems(result.rejected_items || []);
                  loadData();
                } catch (err) {
                  setError(err instanceof Error ? err.message : 'Duyệt tất cả thất bại');
                } finally {
                  setIsRunningJob(false);
                }
              }}
              disabled={isRunningJob || filteredRegistrations.filter((r) => r.status === 'PENDING').length === 0}
              className="px-4 py-2 rounded-xl bg-emerald-600 text-white hover:bg-emerald-700 disabled:bg-neutral-300 transition-colors"
            >
              Duyệt tất cả
            </button>
          </div>

          <table className="w-full">
            <thead className="bg-emerald-50 border-b border-emerald-100">
              <tr>
                <th className="px-6 py-3 text-left text-xs uppercase tracking-wide text-emerald-800">ID</th>
                <th className="px-6 py-3 text-left text-xs uppercase tracking-wide text-emerald-800">Khóa Học</th>
                <th className="px-6 py-3 text-left text-xs uppercase tracking-wide text-emerald-800">Học Viên</th>
                <th className="px-6 py-3 text-left text-xs uppercase tracking-wide text-emerald-800">Ngày Đăng Ký</th>
                <th className="px-6 py-3 text-left text-xs uppercase tracking-wide text-emerald-800">Trạng Thái</th>
                <th className="px-6 py-3 text-left text-xs uppercase tracking-wide text-emerald-800">Hành Động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-emerald-50">
              {filteredRegistrations.map((reg) => {
                const course = courseById.get(reg.course_id);

                return (
                  <tr key={reg.id} className="hover:bg-emerald-50/50">
                    <td className="px-6 py-4 text-sm text-neutral-500">{reg.id}</td>
                    <td className="px-6 py-4">
                      <div className="text-sm">{course?.title || `Course #${reg.course_id}`}</div>
                    </td>
                    <td className="px-6 py-4 text-sm">User {reg.user_id}</td>
                    <td className="px-6 py-4 text-sm text-neutral-600">
                      {new Date(reg.created_at).toLocaleDateString('vi-VN', {
                        year: 'numeric',
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </td>
                    <td className="px-6 py-4">
                      <RegistrationStatusBadge
                        status={reg.status as any}
                        waitlistPosition={reg.waitlist_position || undefined}
                      />
                    </td>
                    <td className="px-6 py-4">
                      {reg.status === 'PENDING' ? (
                        <div className="flex gap-2">
                          <button
                            type="button"
                            data-testid="admin-reg-approve"
                            onClick={() => handleApprove(reg.id)}
                            className="px-3 py-1.5 bg-emerald-600 text-white text-sm hover:bg-emerald-700 transition-colors flex items-center gap-1 rounded-lg"
                          >
                            <CheckCircle className="w-4 h-4" />
                            Duyệt
                          </button>
                          <button
                            type="button"
                            data-testid="admin-reg-reject"
                            onClick={() => handleReject(reg.id)}
                            className="px-3 py-1.5 bg-red-600 text-white text-sm hover:bg-red-700 transition-colors flex items-center gap-1 rounded-lg"
                          >
                            <XCircle className="w-4 h-4" />
                            Từ chối
                          </button>
                        </div>
                      ) : (
                        <span className="text-sm text-neutral-400">-</span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>

          {filteredRegistrations.length === 0 && <div className="text-center py-12 text-neutral-500">Không có đơn đăng ký nào phù hợp</div>}
        </div>

        {dashboard && dashboard.pending_registrations > 0 && (
          <div className="mt-6 bg-amber-50 border border-amber-200 p-6">
            <div className="flex items-start gap-3">
              <AlertCircle className="w-6 h-6 text-amber-600 flex-shrink-0 mt-0.5" />
              <div>
                <div className="text-amber-900 mb-1">Có {dashboard.pending_registrations} đơn đăng ký đang chờ duyệt</div>
                <div className="text-sm text-amber-800">
                  Vui lòng xử lý các đơn chờ duyệt để đảm bảo học viên kịp thời tham gia khóa học.
                </div>
              </div>
            </div>
          </div>
        )}

        {bulkRejectedItems.length > 0 && (
          <div className="mt-6 bg-red-50 border border-red-200 rounded-2xl p-5">
            <h3 className="text-red-900 mb-3">Danh sách học viên bị từ chối do lớp đã đủ</h3>
            <div className="space-y-2">
              {bulkRejectedItems.map((item) => (
                <div key={item.registration_id} className="bg-white border border-red-100 rounded-xl px-3 py-2 text-sm text-neutral-700">
                  <span className="text-neutral-900">{item.user_name || `User ${item.user_id}`}</span>
                  {item.user_email ? ` (${item.user_email})` : ''}
                  {' - '}
                  <span className="text-neutral-900">{item.course_title || `Course #${item.course_id}`}</span>
                  {' - '}
                  <span className="text-red-700">{item.reason}</span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
