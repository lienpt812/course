import { useEffect, useMemo, useState } from 'react';
import { Search } from 'lucide-react';
import { CourseCard } from '../components/CourseCard';
import { courseApi, registrationApi, learningApi, CourseItem, RegistrationItem } from '../lib/api';

type CourseLevel = 'Beginner' | 'Intermediate' | 'Advanced';

interface CertItem { course_id: number; }
interface ProgressMap { [courseId: number]: number; } // courseId -> completion_pct

export function CoursesPage() {
  const [courses, setCourses] = useState<CourseItem[]>([]);
  const [registrations, setRegistrations] = useState<RegistrationItem[]>([]);
  const [certificates, setCertificates] = useState<CertItem[]>([]);
  const [progressMap, setProgressMap] = useState<ProgressMap>({});
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [selectedLevel, setSelectedLevel] = useState<CourseLevel | 'all'>('all');

  const isLoggedIn = !!localStorage.getItem('access_token');
  const authUser = (() => {
    try { return JSON.parse(localStorage.getItem('auth_user') || 'null'); } catch { return null; }
  })();
  const userRole = authUser?.role ?? null;

  useEffect(() => {
    const baseRequests: Promise<any>[] = [courseApi.list()];

    if (isLoggedIn) {
      baseRequests.push(registrationApi.list());
      baseRequests.push(learningApi.myCertificates());
    }

    Promise.all(baseRequests)
      .then(async ([courseItems, regItems, certItems]) => {
        setCourses(courseItems);

        if (isLoggedIn) {
          setRegistrations(regItems ?? []);
          setCertificates((certItems ?? []) as CertItem[]);

          // Fetch progress for confirmed courses
          const confirmedCourseIds = (regItems as RegistrationItem[])
            .filter((r) => r.status === 'CONFIRMED')
            .map((r) => r.course_id);

          if (confirmedCourseIds.length > 0) {
            const progressResults = await Promise.allSettled(
              confirmedCourseIds.map((id) => learningApi.progress(id).then((p) => ({ id, pct: p.completion_pct })))
            );
            const map: ProgressMap = {};
            progressResults.forEach((r) => {
              if (r.status === 'fulfilled') map[r.value.id] = r.value.pct;
            });
            setProgressMap(map);
          }
        }
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Không tải được khóa học'))
      .finally(() => setIsLoading(false));
  }, []);

  const certCourseIds = useMemo(() => new Set(certificates.map((c) => c.course_id)), [certificates]);

  // Map courseId -> registration
  const regByCourse = useMemo(() => {
    const map = new Map<number, RegistrationItem>();
    registrations.forEach((r) => {
      // Keep the most relevant status (CONFIRMED > PENDING > WAITLIST > others)
      const existing = map.get(r.course_id);
      if (!existing) { map.set(r.course_id, r); return; }
      const priority = (s: string) => s === 'CONFIRMED' ? 4 : s === 'WAITLIST' ? 3 : s === 'PENDING' ? 2 : 1;
      if (priority(r.status) > priority(existing.status)) map.set(r.course_id, r);
    });
    return map;
  }, [registrations]);

  const categories = useMemo(
    () => Array.from(new Set(courses.map((c) => c.category || 'General'))),
    [courses]
  );

  const filteredCourses = courses.filter((course) => {
    const matchesSearch =
      course.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      course.description.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory = selectedCategory === 'all' || (course.category || 'General') === selectedCategory;
    const matchesLevel = selectedLevel === 'all' || course.level === selectedLevel;
    return matchesSearch && matchesCategory && matchesLevel;
  });

  return (
    <div className="bg-background min-h-screen">
      <div className="max-w-7xl mx-auto px-6 pt-7 pb-2">
        <h1 className="text-2xl mb-1 text-emerald-900">Khóa Học</h1>
        <p className="text-base text-emerald-700">Tìm khóa học phù hợp với mục tiêu của bạn</p>
      </div>

      <div className="max-w-7xl mx-auto px-6 py-6">
        {/* Filters */}
        <div className="bg-white/95 border border-emerald-100 rounded-2xl p-4 mb-6 shadow-sm shadow-emerald-100/40">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-3 items-end">
            <div className="md:col-span-2">
              <label className="block text-xs mb-1.5 text-emerald-800">Tìm kiếm</label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-emerald-500" />
                <input
                  type="text"
                  placeholder="Tìm khóa học..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 rounded-xl border border-emerald-200 bg-emerald-50/40 focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs mb-1.5 text-emerald-800">Danh mục</label>
              <select
                value={selectedCategory}
                onChange={(e) => setSelectedCategory(e.target.value)}
                className="w-full px-3 py-2.5 rounded-xl border border-emerald-200 bg-emerald-50/40 focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
              >
                <option value="all">Tất cả danh mục</option>
                {categories.map((cat) => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs mb-1.5 text-emerald-800">Cấp độ</label>
              <select
                value={selectedLevel}
                onChange={(e) => setSelectedLevel(e.target.value as CourseLevel | 'all')}
                className="w-full px-3 py-2.5 rounded-xl border border-emerald-200 bg-emerald-50/40 focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
              >
                <option value="all">Tất cả cấp độ</option>
                <option value="Beginner">Beginner</option>
                <option value="Intermediate">Intermediate</option>
                <option value="Advanced">Advanced</option>
              </select>
            </div>
          </div>
        </div>

        <div className="mb-5 text-neutral-600 text-sm">
          Tìm thấy <span className="text-neutral-900">{filteredCourses.length}</span> khóa học
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredCourses.map((course) => {
            const reg = regByCourse.get(course.id);
            const isCompleted = certCourseIds.has(course.id);
            const pct = progressMap[course.id] ?? null;

            return (
              <CourseCard
                key={course.id}
                course={course}
                userRole={userRole}
                registrationStatus={reg?.status ?? null}
                progressPct={pct}
                isCompleted={isCompleted}
              />
            );
          })}
        </div>

        {isLoading && <p className="text-neutral-500 mt-6">Đang tải khóa học...</p>}
        {error && <p className="text-red-600 mt-6">{error}</p>}

        {!isLoading && filteredCourses.length === 0 && (
          <div className="text-center py-20">
            <div className="text-neutral-400 mb-2">Không tìm thấy khóa học phù hợp</div>
            <div className="text-sm text-neutral-500">Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm</div>
          </div>
        )}
      </div>
    </div>
  );
}
