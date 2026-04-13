import { Link } from 'react-router';
import { BookOpen, Users, User, Clock3, CheckCircle, PlayCircle, BookMarked, Settings } from 'lucide-react';
import { CourseItem, UserRole } from '../lib/api';

interface Props {
  course: CourseItem;
  userRole?: UserRole | null;
  registrationStatus?: string | null;
  progressPct?: number | null;
  isCompleted?: boolean;
}

function formatPrice(price: number | undefined): string {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price ?? 0);
}

export function CourseCard({ course, userRole, registrationStatus, progressPct, isCompleted }: Props) {
  const isStudent = !userRole || userRole === 'STUDENT' || userRole === 'GUEST';
  const isAdminOrInstructor = userRole === 'ADMIN' || userRole === 'INSTRUCTOR';

  const isConfirmed = registrationStatus === 'CONFIRMED';
  const isPending = registrationStatus === 'PENDING' || registrationStatus === 'WAITLIST';
  const isNotRegistered = !registrationStatus || registrationStatus === 'CANCELLED' || registrationStatus === 'REJECTED' || registrationStatus === 'EXPIRED';

  const renderStatus = () => {
    // Admin/Instructor: chỉ hiện nút quản lý
    if (isAdminOrInstructor) {
      return (
        <div className="mt-3">
          <Link
            to={`/courses/${course.id}`}
            onClick={(e) => e.stopPropagation()}
            className="flex items-center justify-center gap-1.5 w-full py-2 bg-neutral-100 text-neutral-700 text-sm rounded-lg hover:bg-neutral-200 transition-colors font-medium"
          >
            <Settings className="w-4 h-4" />
            Xem chi tiết
          </Link>
        </div>
      );
    }

    // Đã hoàn thành
    if (isCompleted) {
      return (
        <div className="mt-3">
          <div className="flex items-center justify-center gap-1.5 w-full py-2 bg-green-50 border border-green-200 rounded-lg text-green-700 text-sm font-medium">
            <CheckCircle className="w-4 h-4" />
            Đã hoàn thành
          </div>
        </div>
      );
    }

    // Đang học
    if (isConfirmed) {
      const pct = progressPct ?? 0;
      return (
        <div className="mt-3 space-y-2">
          <div className="flex items-center justify-between text-xs text-neutral-500">
            <span>Tiến trình</span>
            <span className="font-medium text-emerald-700">{pct}%</span>
          </div>
          <div className="h-1.5 bg-neutral-100 rounded-full overflow-hidden">
            <div className="h-full bg-emerald-500 rounded-full transition-all" style={{ width: `${pct}%` }} />
          </div>
          <Link
            to={`/learn/${course.id}`}
            onClick={(e) => e.stopPropagation()}
            className="flex items-center justify-center gap-1.5 w-full py-2 bg-emerald-600 text-white text-sm rounded-lg hover:bg-emerald-700 transition-colors font-medium"
          >
            <PlayCircle className="w-4 h-4" />
            Tiếp tục học
          </Link>
        </div>
      );
    }

    // Đang chờ duyệt / hàng chờ
    if (isPending) {
      return (
        <div className="mt-3">
          <div className="flex items-center justify-center gap-1.5 w-full py-2 bg-amber-50 border border-amber-200 text-amber-700 text-sm rounded-lg font-medium">
            <BookMarked className="w-4 h-4" />
            {registrationStatus === 'WAITLIST' ? 'Đang trong hàng chờ' : 'Đang chờ duyệt'}
          </div>
        </div>
      );
    }

    // Chưa đăng ký (chỉ student)
    if (isStudent && isNotRegistered) {
      return (
        <div className="mt-3">
          <Link
            to={`/courses/${course.id}`}
            onClick={(e) => e.stopPropagation()}
            className="flex items-center justify-center gap-1.5 w-full py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 transition-colors font-medium"
          >
            Đăng ký ngay
          </Link>
        </div>
      );
    }

    return null;
  };

  return (
    <Link to={`/courses/${course.id}`} className="block group">
      <div className="bg-white border border-neutral-200 overflow-hidden transition-all hover:border-neutral-300 hover:shadow-lg rounded-xl">
        <div className="aspect-[16/10] overflow-hidden bg-gradient-to-br from-blue-100 to-cyan-100">
          {course.image_url ? (
            <img
              src={course.image_url}
              alt={course.title}
              className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <BookOpen className="w-16 h-16 text-blue-600/60" />
            </div>
          )}
        </div>

        <div className="p-5">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-xs text-neutral-500">{course.category || 'General'}</span>
            <span className="text-neutral-300">•</span>
            <span className="text-xs text-neutral-500">{course.level}</span>
          </div>

          <h3 className="text-lg mb-3 line-clamp-2 group-hover:text-blue-600 transition-colors">
            {course.title}
          </h3>

          <div className="flex items-center justify-between text-sm text-neutral-600 mb-3">
            <div className="flex items-center gap-1">
              <Users className="w-4 h-4" />
              <span>Tối đa {course.max_capacity}</span>
            </div>
            <div className="flex items-center gap-1">
              <Clock3 className="w-4 h-4" />
              <span>{course.estimated_hours ?? 0}h</span>
            </div>
          </div>

          <div className="flex items-center gap-1.5 text-sm text-neutral-600 mb-3">
            <User className="w-4 h-4 text-emerald-600" />
            <span className="truncate">{course.instructor_name || 'Đang cập nhật'}</span>
          </div>

          <div className="flex items-center justify-between pt-3 border-t border-neutral-100">
            <div className="text-xl font-semibold text-blue-600">{formatPrice(course.price)}</div>
            {course.certificate_enabled && (
              <span className="text-xs px-2 py-0.5 bg-yellow-50 border border-yellow-200 text-yellow-700 rounded-full">
                Có chứng chỉ
              </span>
            )}
          </div>

          {renderStatus()}
        </div>
      </div>
    </Link>
  );
}
