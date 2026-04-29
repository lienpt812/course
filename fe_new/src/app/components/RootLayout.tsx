import { useEffect, useState } from 'react';
import { Outlet, Link, useLocation, useNavigate } from 'react-router';
import { BookOpen, User, GraduationCap, Settings } from 'lucide-react';
import { ApiHttpError, authApi, AuthUser } from '../lib/api';

function httpStatusFromError(err: unknown): number {
  if (err instanceof ApiHttpError) return err.status;
  if (err && typeof err === 'object' && 'status' in err) {
    const s = Number((err as { status: unknown }).status);
    return Number.isFinite(s) ? s : 0;
  }
  return 0;
}

export function RootLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(() => {
    const raw = localStorage.getItem('auth_user');
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      return null;
    }
  });

  useEffect(() => {
    const token = localStorage.getItem('access_token');
    const protectedPaths = ['/student/dashboard', '/instructor/dashboard', '/admin/dashboard', '/learn/', '/profile'];
    const isProtectedPath =
      protectedPaths.some((path) => location.pathname === path || location.pathname.startsWith(path));

    if (!token) {
      setCurrentUser(null);
      if (location.pathname === '/' || isProtectedPath) {
        navigate('/courses', { replace: true });
      }
      return;
    }

    authApi
      .me()
      .then((user) => {
        setCurrentUser(user);
        localStorage.setItem('auth_user', JSON.stringify(user));
      })
      .catch((err) => {
        const status = httpStatusFromError(err);
        // Rate limits / transient server errors are not "logged out"; clearing tokens sends users to /courses
        // while the SPA still shows a protected URL (breaks e2e and confuses users).
        if (status !== 401 && status !== 403) {
          return;
        }
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        localStorage.removeItem('auth_user');
        setCurrentUser(null);
        if (location.pathname === '/' || isProtectedPath) {
          navigate('/courses', { replace: true });
        }
      });
  }, [location.pathname, navigate]);

  const isActive = (path: string) => {
    return location.pathname === path || location.pathname.startsWith(path + '/');
  };
  return (
    <div className="min-h-screen bg-background">
      <header className="bg-[#d0efdb] border-b border-emerald-200 sticky top-0 z-50">
        <nav className="max-w-7xl mx-auto px-6 h-14 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2">
            <GraduationCap className="w-7 h-7 text-emerald-700" />
            <span className="text-lg tracking-tight text-emerald-900">EduPlatform</span>
          </Link>

          <div className="flex items-center gap-8">
            <Link
              to="/courses"
              className={`transition-colors ${
                  isActive('/courses') ? 'text-emerald-900' : 'text-emerald-700 hover:text-emerald-900'
              }`}
            >
              Khóa Học
            </Link>

            {currentUser?.role === 'STUDENT' && (
              <Link
                to="/student/dashboard"
                className={`transition-colors ${
                  isActive('/student/dashboard') ? 'text-emerald-900' : 'text-emerald-700 hover:text-emerald-900'
                }`}
              >
                Dashboard
              </Link>
            )}

            {currentUser?.role === 'INSTRUCTOR' && (
              <Link
                to="/instructor/dashboard"
                className={`transition-colors ${
                  isActive('/instructor/dashboard') ? 'text-emerald-900' : 'text-emerald-700 hover:text-emerald-900'
                }`}
              >
                Giảng Viên
              </Link>
            )}

            {currentUser?.role === 'ADMIN' && (
              <Link
                to="/admin/dashboard"
                className={`transition-colors ${
                  isActive('/admin/dashboard') ? 'text-emerald-900' : 'text-emerald-700 hover:text-emerald-900'
                }`}
              >
                Quản Trị
              </Link>
            )}

            {currentUser ? (
              <div className="flex items-center gap-3 pl-6 border-l border-neutral-200">
                <Link to="/profile" className="text-right hover:opacity-80 transition-opacity">
                  <div className="text-sm">{currentUser.name}</div>
                  <div className="text-xs text-emerald-700">{currentUser.role}</div>
                </Link>
                <Link
                  to="/profile"
                  className="w-8 h-8 rounded-full bg-emerald-100 flex items-center justify-center"
                  title="Trang cá nhân"
                >
                  <User className="w-4 h-4 text-emerald-700" />
                </Link>
                <button
                  type="button"
                  onClick={() => {
                    localStorage.removeItem('access_token');
                    localStorage.removeItem('refresh_token');
                    localStorage.removeItem('auth_user');
                    setCurrentUser(null);
                    navigate('/courses', { replace: true });
                  }}
                  className="px-3 py-1.5 text-xs border border-neutral-300 hover:bg-neutral-100 transition-colors"
                  title="Đăng xuất"
                >
                  Logout
                </button>
              </div>
            ) : (
              <div className="pl-6 border-l border-neutral-200">
                <Link to="/login" className="px-4 py-1.5 rounded-lg bg-emerald-600 text-white hover:bg-emerald-700 transition-colors text-sm">
                  Đăng nhập
                </Link>
              </div>
            )}
          </div>
        </nav>
      </header>

      <main>
        <Outlet />
      </main>

      <footer className="bg-neutral-900 text-neutral-300 mt-24">
        <div className="max-w-7xl mx-auto px-6 py-12">
          <div className="grid grid-cols-4 gap-8">
            <div>
              <div className="flex items-center gap-2 mb-4">
                <GraduationCap className="w-6 h-6 text-blue-400" />
                <span className="text-white">EduPlatform</span>
              </div>
              <p className="text-sm text-neutral-400">
                Nền tảng đào tạo trực tuyến hàng đầu Việt Nam
              </p>
            </div>

            <div>
              <h3 className="text-white mb-3 text-sm">Khóa Học</h3>
              <ul className="space-y-2 text-sm">
                <li><Link to="/courses" className="hover:text-white transition-colors">Lập Trình</Link></li>
                <li><Link to="/courses" className="hover:text-white transition-colors">Thiết Kế</Link></li>
                <li><Link to="/courses" className="hover:text-white transition-colors">Marketing</Link></li>
              </ul>
            </div>

            <div>
              <h3 className="text-white mb-3 text-sm">Hỗ Trợ</h3>
              <ul className="space-y-2 text-sm">
                <li><a href="#" className="hover:text-white transition-colors">Trung Tâm Trợ Giúp</a></li>
                <li><a href="#" className="hover:text-white transition-colors">Điều Khoản</a></li>
                <li><a href="#" className="hover:text-white transition-colors">Bảo Mật</a></li>
              </ul>
            </div>

            <div>
              <h3 className="text-white mb-3 text-sm">Liên Hệ</h3>
              <p className="text-sm text-neutral-400">
                Email: support@eduplatform.vn<br />
                Hotline: 1900 1234
              </p>
            </div>
          </div>

          <div className="border-t border-neutral-800 mt-8 pt-8 text-sm text-neutral-500">
            © 2026 EduPlatform. Phiên bản 1.0.0
          </div>
        </div>
      </footer>
    </div>
  );
}
