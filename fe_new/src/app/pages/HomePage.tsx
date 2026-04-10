import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router';
import { ArrowRight, BookOpen, Users, Award, TrendingUp } from 'lucide-react';
import { CourseCard } from '../components/CourseCard';
import { courseApi, CourseItem } from '../lib/api';

export function HomePage() {
  const [courses, setCourses] = useState<CourseItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    courseApi
      .list()
      .then((items) => setCourses(items))
      .catch((err) => setError(err instanceof Error ? err.message : 'Không tải được khóa học'))
      .finally(() => setIsLoading(false));
  }, []);

  const featuredCourses = useMemo(() => courses.slice(0, 3), [courses]);
  const categories = useMemo(() => {
    const counter = new Map<string, number>();
    courses.forEach((course) => {
      const name = course.category || 'General';
      counter.set(name, (counter.get(name) ?? 0) + 1);
    });
    return Array.from(counter.entries()).slice(0, 6);
  }, [courses]);

  return (
    <div>
      {/* Hero Section */}
      <section className="relative bg-gradient-to-br from-emerald-600 to-emerald-800 text-white">
        <div className="max-w-7xl mx-auto px-6 py-24">
          <div className="grid grid-cols-2 gap-12 items-center">
            <div>
              <h1 className="text-5xl mb-6 leading-tight">
                Nền Tảng Đào Tạo<br />
                Trực Tuyến Hàng Đầu
              </h1>
              <p className="text-xl mb-8 text-emerald-100 leading-relaxed">
                Học từ các chuyên gia hàng đầu. Nhận chứng chỉ được công nhận.
                Phát triển sự nghiệp của bạn.
              </p>
              <div className="flex gap-4">
                <Link
                  to="/courses"
                  className="inline-flex items-center gap-2 bg-white text-emerald-700 px-8 py-4 hover:bg-emerald-50 transition-colors"
                >
                  Khám Phá Khóa Học
                  <ArrowRight className="w-5 h-5" />
                </Link>
                <Link
                  to="/student/dashboard"
                  className="inline-flex items-center gap-2 border-2 border-white px-8 py-4 hover:bg-white/10 transition-colors"
                >
                  Dashboard
                </Link>
              </div>
            </div>

            <div className="relative">
              <img
                src="https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=800&q=80"
                alt="Students learning"
                className="w-full h-[500px] object-cover"
              />
            </div>
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="bg-white border-b border-neutral-200">
        <div className="max-w-7xl mx-auto px-6 py-16">
          <div className="grid grid-cols-4 gap-8">
            <div className="text-center">
              <div className="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center mx-auto mb-4">
                <BookOpen className="w-8 h-8 text-emerald-600" />
              </div>
              <div className="text-4xl mb-2">{courses.length}+</div>
              <div className="text-neutral-600">Khóa Học</div>
            </div>

            <div className="text-center">
              <div className="w-16 h-16 bg-green-50 rounded-full flex items-center justify-center mx-auto mb-4">
                <Users className="w-8 h-8 text-green-600" />
              </div>
              <div className="text-4xl mb-2">10,000+</div>
              <div className="text-neutral-600">Học Viên</div>
            </div>

            <div className="text-center">
              <div className="w-16 h-16 bg-amber-50 rounded-full flex items-center justify-center mx-auto mb-4">
                <Award className="w-8 h-8 text-amber-600" />
              </div>
              <div className="text-4xl mb-2">5,000+</div>
              <div className="text-neutral-600">Chứng Chỉ</div>
            </div>

            <div className="text-center">
              <div className="w-16 h-16 bg-purple-50 rounded-full flex items-center justify-center mx-auto mb-4">
                <TrendingUp className="w-8 h-8 text-purple-600" />
              </div>
              <div className="text-4xl mb-2">95%</div>
              <div className="text-neutral-600">Hài Lòng</div>
            </div>
          </div>
        </div>
      </section>

      {/* Featured Courses */}
      <section className="max-w-7xl mx-auto px-6 py-20">
        <div className="flex items-end justify-between mb-12">
          <div>
            <h2 className="text-4xl mb-3">Khóa Học Nổi Bật</h2>
            <p className="text-xl text-neutral-600">
              Các khóa học được đánh giá cao nhất từ học viên
            </p>
          </div>
          <Link
            to="/courses"
            className="inline-flex items-center gap-2 text-emerald-700 hover:gap-3 transition-all"
          >
            Xem Tất Cả
            <ArrowRight className="w-5 h-5" />
          </Link>
        </div>

        <div className="grid grid-cols-3 gap-6">
          {featuredCourses.map((course) => (
            <CourseCard key={course.id} course={course} />
          ))}
        </div>

        {isLoading && <p className="mt-6 text-neutral-500">Đang tải khóa học...</p>}
        {error && <p className="mt-6 text-red-600">{error}</p>}
      </section>

      {/* Categories */}
      <section className="bg-emerald-50/50 py-20">
        <div className="max-w-7xl mx-auto px-6">
          <h2 className="text-4xl mb-12 text-center">Danh Mục Khóa Học</h2>

          <div className="grid grid-cols-3 gap-6">
            {categories.map(([name, count]) => (
              <Link
                key={name}
                to="/courses"
                className="bg-white border border-neutral-200 p-8 hover:border-neutral-300 hover:shadow-lg transition-all group"
              >
                <h3 className="text-xl mb-2 group-hover:text-emerald-700 transition-colors">
                  {name}
                </h3>
                <p className="text-neutral-600">{count} khóa học</p>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="bg-emerald-700 text-white py-20">
        <div className="max-w-4xl mx-auto px-6 text-center">
          <h2 className="text-4xl mb-6">Bắt Đầu Học Hôm Nay</h2>
          <p className="text-xl mb-8 text-emerald-100">
            Đăng ký ngay để nhận ưu đãi đặc biệt cho học viên mới
          </p>
          <Link
            to="/courses"
            className="inline-flex items-center gap-2 bg-white text-emerald-700 px-8 py-4 hover:bg-emerald-50 transition-colors"
          >
            Khám Phá Khóa Học
            <ArrowRight className="w-5 h-5" />
          </Link>
        </div>
      </section>
    </div>
  );
}
