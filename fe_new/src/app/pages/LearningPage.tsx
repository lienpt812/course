import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router';
import { ArrowLeft, CheckCircle, Circle, PlayCircle, FileText, HelpCircle, Award, X } from 'lucide-react';
import {
  ApiHttpError,
  courseApi,
  registrationApi,
  learningApi,
  CourseDetail,
  RegistrationItem,
  LearningOutlineItem,
  LearningLessonItem,
} from '../lib/api';

// Certificate Congratulation Modal
function CertificateModal({ courseName, onClose }: { courseName: string; onClose: () => void }) {
  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full p-8 text-center relative">
        <button onClick={onClose} className="absolute top-4 right-4 text-neutral-400 hover:text-neutral-600">
          <X className="w-5 h-5" />
        </button>
        <div className="w-20 h-20 bg-yellow-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <Award className="w-10 h-10 text-yellow-500" />
        </div>
        <h2 className="text-2xl font-bold text-neutral-900 mb-2">Chúc mừng! 🎉</h2>
        <p className="text-neutral-600 mb-1">Bạn đã hoàn thành khóa học</p>
        <p className="text-emerald-700 font-semibold text-lg mb-6">"{courseName}"</p>
        <p className="text-neutral-500 text-sm mb-6">
          Chứng chỉ đã được cấp và lưu vào tài khoản của bạn.
        </p>
        <div className="flex gap-3 justify-center">
          <Link
            to="/student/dashboard"
            className="px-5 py-2.5 bg-emerald-600 text-white rounded-xl hover:bg-emerald-700 font-medium"
          >
            Xem chứng chỉ
          </Link>
          <button
            onClick={onClose}
            className="px-5 py-2.5 bg-neutral-100 text-neutral-700 rounded-xl hover:bg-neutral-200 font-medium"
          >
            Tiếp tục học
          </button>
        </div>
      </div>
    </div>
  );
}

export function LearningPage() {
  const { courseId } = useParams<{ courseId: string }>();
  const [course, setCourse] = useState<CourseDetail | null>(null);
  const [registrations, setRegistrations] = useState<RegistrationItem[]>([]);
  const [outline, setOutline] = useState<LearningOutlineItem[]>([]);
  const [completedLessons, setCompletedLessons] = useState<Set<number>>(new Set());
  const [selectedLesson, setSelectedLesson] = useState<number | null>(null);
  const [isCompleting, setIsCompleting] = useState(false);
  const [message, setMessage] = useState('');
  const [showCertModal, setShowCertModal] = useState(false);
  const [hasCert, setHasCert] = useState(false);
  const [isIssuingCert, setIsIssuingCert] = useState(false);
  const [pageLoading, setPageLoading] = useState(true);

  useEffect(() => {
    if (!courseId) return;
    const numericCourseId = Number(courseId);
    let cancelled = false;

    const applyLoaded = (
      courseData: CourseDetail,
      registrationData: RegistrationItem[],
      outlineData: LearningOutlineItem[],
      progressDetail: { completed_lesson_ids: number[] },
      certs: unknown,
    ) => {
      setCourse(courseData);
      setRegistrations(registrationData);
      setOutline(outlineData);

      const completedSet = new Set(progressDetail.completed_lesson_ids);
      setCompletedLessons(completedSet);

      const existingCert = (certs as any[]).find((c) => c.course_id === numericCourseId);
      if (existingCert) {
        setHasCert(true);
        const allLessons = outlineData.flatMap((x) => x.lessons);
        const sessionKey = `cert_modal_shown_${numericCourseId}`;
        if (
          allLessons.length > 0 &&
          allLessons.every((l) => completedSet.has(l.id)) &&
          !sessionStorage.getItem(sessionKey)
        ) {
          sessionStorage.setItem(sessionKey, '1');
          setShowCertModal(true);
        }
      }

      const firstLessonId =
        outlineData.flatMap((x) => x.lessons).sort((a, b) => a.position - b.position)[0]?.id ?? null;
      setSelectedLesson(firstLessonId);
    };

    const load = async () => {
      setPageLoading(true);
      setCourse(null);
      const delaysMs = [0, 700, 1800, 3200];
      for (let attempt = 0; attempt < delaysMs.length; attempt++) {
        if (delaysMs[attempt] > 0) {
          await new Promise((r) => setTimeout(r, delaysMs[attempt]));
        }
        if (cancelled) return;
        try {
          const [courseData, registrationData, outlineData, progressDetail, certs] = await Promise.all([
            courseApi.detail(numericCourseId),
            registrationApi.list(),
            learningApi.outline(numericCourseId),
            learningApi.progressDetail(numericCourseId),
            learningApi.myCertificates(),
          ]);
          if (cancelled) return;
          applyLoaded(courseData, registrationData, outlineData, progressDetail, certs);
          setPageLoading(false);
          return;
        } catch (err) {
          const isRateLimited = err instanceof ApiHttpError && err.status === 429;
          const retryable = isRateLimited || (err instanceof ApiHttpError && err.status >= 500);
          if (attempt === delaysMs.length - 1 || !retryable) {
            if (!cancelled) {
              setCourse(null);
              setPageLoading(false);
            }
            return;
          }
        }
      }
    };

    void load();
    return () => {
      cancelled = true;
    };
  }, [courseId]);

  if (pageLoading) {
    return (
      <div className="max-w-7xl mx-auto px-6 py-20 text-center text-neutral-600" data-testid="learning-page-loading">
        Đang tải khóa học…
      </div>
    );
  }

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

      const newCompleted = new Set(completedLessons);
      if (updated.completed) {
        newCompleted.add(currentLesson.id);
        setCompletedLessons(newCompleted);
      }

      const allDone = lessons.length > 0 && lessons.every((l) => newCompleted.has(l.id));

      if (updated.certificate_issued || allDone) {
        setHasCert(true);
        setShowCertModal(true);
      } else {
        setMessage('Đã đánh dấu bài học hoàn thành.');
      }
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Không cập nhật được tiến độ');
    } finally {
      setIsCompleting(false);
    }
  };

  const claimCertificate = async () => {
    if (!course) return;
    setIsIssuingCert(true);
    try {
      await learningApi.issueCertificate(course.id);
      setHasCert(true);
      setShowCertModal(true);
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Không thể cấp chứng chỉ');
    } finally {
      setIsIssuingCert(false);
    }
  };

  const allLessonsDone = lessons.length > 0 && lessons.every((l) => completedLessons.has(l.id));

  return (
    <div className="flex h-[calc(100vh-4rem)]" data-testid="learning-layout">
      {showCertModal && course && (
        <CertificateModal courseName={course.title} onClose={() => setShowCertModal(false)} />
      )}
      <div
        className="w-96 bg-white border-r border-neutral-200 flex flex-col"
        data-testid="learning-sidebar"
      >
        <div className="p-6 border-b border-neutral-200">
          <Link to="/student/dashboard" className="inline-flex items-center gap-2 text-sm text-neutral-600 hover:text-neutral-900 mb-4">
            <ArrowLeft className="w-4 h-4" />
            Quay lại Dashboard
          </Link>
          <h2 className="text-lg mb-2 line-clamp-2" data-testid="learning-course-title">
            {course.title}
          </h2>
          <div className="text-sm text-neutral-600 mb-3">
            {completedCount} / {totalLessons} bai hoc
          </div>
          <div className="h-2 bg-neutral-100 rounded-full overflow-hidden">
            <div className="h-full bg-emerald-600" style={{ width: `${progress}%` }} />
          </div>
          {hasCert && (
            <div className="mt-3 flex items-center gap-2 bg-yellow-50 border border-yellow-200 rounded-xl px-3 py-2">
              <Award className="w-4 h-4 text-yellow-500 flex-shrink-0" />
              <span className="text-xs text-yellow-800 font-medium">Đã có chứng chỉ</span>
              <button
                onClick={() => setShowCertModal(true)}
                className="ml-auto text-xs text-emerald-700 hover:underline font-medium"
              >
                Xem
              </button>
            </div>
          )}
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
                      type="button"
                      data-testid="learning-lesson-button"
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
                  <><CheckCircle className="w-4 h-4" />Đã hoàn thành</>
                ) : (
                  <><Circle className="w-4 h-4" />{isCompleting ? 'Đang cập nhật...' : 'Đánh dấu hoàn thành'}</>
                )}
              </button>
            )}

            {/* Show claim cert button when all done but no cert yet */}
            {allLessonsDone && !hasCert && (
              <button
                onClick={claimCertificate}
                disabled={isIssuingCert}
                className="mt-4 inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-yellow-500 text-white hover:bg-yellow-600 disabled:opacity-50 font-medium"
              >
                <Award className="w-5 h-5" />
                {isIssuingCert ? 'Đang cấp...' : 'Nhận chứng chỉ'}
              </button>
            )}

            {/* Show view cert button when already has cert */}
            {hasCert && (
              <button
                onClick={() => setShowCertModal(true)}
                className="mt-4 inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-yellow-500 text-white hover:bg-yellow-600 font-medium"
              >
                <Award className="w-5 h-5" />
                Xem chứng chỉ
              </button>
            )}

            {message && <div className="mt-3 text-sm text-red-400">{message}</div>}          </div>
        </div>
      </div>
    </div>
  );
}
