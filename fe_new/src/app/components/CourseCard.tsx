import { Link } from 'react-router';
import { BookOpen, Users, User, Clock3 } from 'lucide-react';
import { CourseItem } from '../lib/api';

interface Props {
  course: CourseItem;
}

function formatPrice(price: number | undefined): string {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price ?? 0);
}

export function CourseCard({ course }: Props) {
  return (
    <Link
      to={`/courses/${course.id}`}
      className="block group"
    >
      <div className="bg-white border border-neutral-200 overflow-hidden transition-all hover:border-neutral-300 hover:shadow-lg">
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

          <div className="flex items-center justify-between text-sm text-neutral-600 mb-4">
            <div className="flex items-center gap-1">
              <BookOpen className="w-4 h-4" />
              <span>{course.status}</span>
            </div>
            <div className="flex items-center gap-1">
              <Users className="w-4 h-4" />
              <span>Tối đa {course.max_capacity}</span>
            </div>
          </div>

          <div className="space-y-1 mb-4 text-sm text-neutral-600">
            <div className="flex items-center gap-1.5">
              <User className="w-4 h-4 text-emerald-600" />
              <span>Giảng viên: {course.instructor_name || 'Đang cập nhật'}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <Clock3 className="w-4 h-4 text-emerald-600" />
              <span>Số giờ học: {course.estimated_hours ?? 0}h</span>
            </div>
          </div>

          <div className="flex items-center justify-between pt-4 border-t border-neutral-100">
            <div>
              <div className="text-sm text-neutral-500">Định dạng</div>
              <div className="text-sm">Online</div>
            </div>
            <div className="text-right">
              <div className="text-xl text-blue-600">{formatPrice(course.price)}</div>
            </div>
          </div>
        </div>
      </div>
    </Link>
  );
}
