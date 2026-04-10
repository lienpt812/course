import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router';
import { ArrowLeft, CheckCircle, Circle, PlayCircle, FileText, HelpCircle } from 'lucide-react';
import {
  courseApi,
  registrationApi,
  learningApi,
  CourseDetail,
  RegistrationItem,
  LearningOutlineItem,
  LearningLessonItem,
} from '../lib/api';

export function LearningPage() {
  const { courseId } = useParams<{ courseId: string }>();
  const [course, setCourse] = useState<CourseDetail | null>(null);
  const [registrations, setRegistrations] = useState<RegistrationItem[]>([]);
  const [outline, setOutline] = useState<LearningOutlineItem[]>([]);
  const [completedLessons, setCompletedLessons] = useState<Set<number>>(new Set());
  const [selectedLesson, setSelectedLesson] = useState<number | null>(null);
  const [isCompleting, setIsCompleting] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (!courseId) return;
    const numericCourseId = Number(courseId);

    Promise.all([
      courseApi.detail(numericCourseId),
      registrationApi.list(),
      learningApi.outline(numericCourseId),
      learningApi.progressDetail(numericCourseId),
    ])
      .then(([courseData, registrationData, outlineData, progressDetail]) => {
        setCourse(courseData);
        setRegistrations(registrationData);
        setOutline(outlineData);
        setCompletedLessons(new Set(progressDetail.completed_lesson_ids));

        const firstLessonId = outlineData.flatMap((x) => x.lessons).sort((a, b) => a.position - b.position)[0]?.id ?? null;
        setSelectedLesson(firstLessonId);
      })
      .catch(() => {
        setCourse(null);
      });
  }, [courseId]);

  if (!course) {
    return (
      <div className="max-w-7xl mx-auto px-6 py-20 text-center">
        <h1 className="text-2xl mb-4">Không tìm thấy khóa học</h1>
        <Link to="/student/dashboard" className="text-blue-600 hover:underline">
          Quay lại dashboard
        </Link>
      </div>
    );
  }

  const authUserRaw = localStorage.getItem('auth_user');
  const authUser = authUserRaw ? JSON.parse(authUserRaw) : null;

  const registration = registrations.find(
    (item) => item.course_id === course.id && item.user_id === authUser?.id
  );

  if (registration?.status !== 'CONFIRMED') {
    return (
      <div className="max-w-7xl mx-auto px-6 py-20 text-center">
        <h1 className="text-2xl mb-4">Bạn chưa được xác nhận vào khóa học này</h1>
        <Link to="/student/dashboard" className="text-blue-600 hover:underline">
          Quay lại dashboard
        </Link>
      </div>
    );
  }

  const lessons = outline.flatMap((s) => s.lessons);
  const totalLessons = lessons.length;
  const completedCount = lessons.filter((l) => completedLessons.has(l.id)).length;
  const progress = totalLessons > 0 ? Math.round((completedCount / totalLessons) * 100) : 0;
  const currentLesson = lessons.find((l) => l.id === selectedLesson) || null;

  const iconByType = (type: LearningLessonItem['type']) => {
    if (type === 'VIDEO') return PlayCircle;
    if (type === 'QUIZ') return HelpCircle;
    return FileText;
  };

  const completeLesson = async () => {
    if (!currentLesson) return;
    setIsCompleting(true);
    setMessage('');
    try {
      const updated = await learningApi.upsertProgress({ lesson_id: currentLesson.id, completion_pct: 100 });
      if (updated.completed) {
        setCompletedLessons((prev) => {
          const next = new Set(prev);
          next.add(currentLesson.id);
          return next;
        });
      }
      if (updated.certificate_issued) {
        setMessage('Chuc mung! Ban da hoan thanh khoa hoc va duoc cap chung chi.');
      } else {
        setMessage('Da danh dau bai hoc hoan thanh.');
      }
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Khong cap nhat duoc tien do');
    } finally {
      setIsCompleting(false);
    }
  };

  return (
    <div className="flex h-[calc(100vh-4rem)]">
      <div className="w-96 bg-white border-r border-neutral-200 flex flex-col">
        <div className="p-6 border-b border-neutral-200">
          <Link to="/student/dashboard" className="inline-flex items-center gap-2 text-sm text-neutral-600 hover:text-neutral-900 mb-4">
            <ArrowLeft className="w-4 h-4" />
            Quay lại Dashboard
          </Link>
          <h2 className="text-lg mb-2 line-clamp-2">{course.title}</h2>
          <div className="text-sm text-neutral-600 mb-3">
            {completedCount} / {totalLessons} bai hoc
          </div>
          <div className="h-2 bg-neutral-100 rounded-full overflow-hidden">
            <div className="h-full bg-emerald-600" style={{ width: `${progress}%` }} />
          </div>
        </div>

        <div className="flex-1 overflow-y-auto">
          {outline.map((sectionWrap) => (
            <div key={sectionWrap.section.id} className="border-b border-neutral-200">
              <div className="px-6 py-3 bg-neutral-50">
                <h3 className="text-sm">{sectionWrap.section.title}</h3>
              </div>
              <div>
                {sectionWrap.lessons.map((lesson) => {
                  const isActive = lesson.id === selectedLesson;
                  const Icon = iconByType(lesson.type);
                  const isDone = completedLessons.has(lesson.id);

                  return (
                    <button
                      key={lesson.id}
                      onClick={() => setSelectedLesson(lesson.id)}
                      className={`w-full px-6 py-3 flex items-start gap-3 hover:bg-neutral-50 transition-colors border-l-2 ${
                        isActive ? 'border-emerald-600 bg-emerald-50' : 'border-transparent'
                      }`}
                    >
                      {isDone ? (
                        <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0" />
                      ) : (
                        <Circle className="w-5 h-5 text-neutral-300 flex-shrink-0" />
                      )}
                      <div className="flex-1 text-left">
                        <div className="flex items-center gap-2 mb-1">
                          <Icon className="w-4 h-4 text-neutral-400" />
                          <div className={`text-sm ${isActive ? 'text-emerald-700' : ''}`}>{lesson.title}</div>
                        </div>
                        <div className="text-xs text-neutral-500">{lesson.duration_minutes} phut</div>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          ))}
          {outline.length === 0 && <div className="p-6 text-sm text-neutral-500">Khoa hoc chua co bai hoc trong DB.</div>}
        </div>
      </div>

      <div className="flex-1 bg-neutral-900 flex flex-col">
        <div className="flex-1 flex items-center justify-center">
          <div className="text-center text-white">
            <PlayCircle className="w-20 h-20 mx-auto mb-4 opacity-50" />
            <div className="text-xl mb-2">{currentLesson?.title}</div>
            <div className="text-neutral-400 mb-5">
              {currentLesson?.content_url ? currentLesson.content_url : 'Noi dung hoc tap duoc nap tu DB'}
            </div>
            {currentLesson && (
              <button
                onClick={completeLesson}
                disabled={isCompleting || completedLessons.has(currentLesson.id)}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-600 text-white hover:bg-emerald-700 disabled:bg-emerald-800/40 disabled:cursor-not-allowed"
              >
                {completedLessons.has(currentLesson.id) ? (
                  <>
                    <CheckCircle className="w-4 h-4" />
                    Da hoan thanh
                  </>
                ) : (
                  <>
                    <Circle className="w-4 h-4" />
                    {isCompleting ? 'Dang cap nhat...' : 'Danh dau hoan thanh'}
                  </>
                )}
              </button>
            )}
            {message && <div className="mt-3 text-sm text-emerald-300">{message}</div>}
          </div>
        </div>
      </div>
    </div>
  );
}
