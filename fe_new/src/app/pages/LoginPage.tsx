import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { authApi } from '../lib/api';

function dashboardPathByRole(role: string): string {
  if (role === 'ADMIN') return '/admin/dashboard';
  if (role === 'INSTRUCTOR') return '/instructor/dashboard';
  return '/student/dashboard';
}

export function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('student@example.com');
  const [password, setPassword] = useState('password123');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      const tokens = await authApi.login(email, password);
      localStorage.setItem('access_token', tokens.access_token);
      localStorage.setItem('refresh_token', tokens.refresh_token);

      const me = await authApi.me();
      localStorage.setItem('auth_user', JSON.stringify(me));
      navigate(dashboardPathByRole(me.role));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Đăng nhập thất bại');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[72vh] flex items-center justify-center px-6 py-10 bg-gradient-to-b from-emerald-50 to-neutral-50">
      <div className="w-full max-w-md bg-white border border-emerald-100 rounded-2xl p-8 shadow-xl shadow-emerald-100/40">
        <h1 className="text-3xl mb-2 text-emerald-900">Đăng nhập</h1>
        <p className="text-emerald-700 mb-6">Sử dụng tài khoản để truy cập dashboard theo role.</p>

        <form onSubmit={onSubmit} className="space-y-4">
          <div>
            <label className="block text-sm mb-2 text-neutral-700">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
              required
            />
          </div>

          <div>
            <label className="block text-sm mb-2 text-neutral-700">Mật khẩu</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
              required
            />
          </div>

          {error && <p className="text-sm text-red-600">{error}</p>}

          <button
            type="submit"
            disabled={isLoading}
            className="w-full rounded-xl bg-emerald-600 text-white py-2.5 hover:bg-emerald-700 transition-colors disabled:bg-neutral-400 shadow-lg shadow-emerald-200"
          >
            {isLoading ? 'Đang đăng nhập...' : 'Đăng nhập'}
          </button>

          <p className="text-sm text-neutral-600 text-center">
            Chưa có tài khoản?{' '}
            <Link to="/register" className="text-emerald-700 hover:underline">
              Đăng ký ngay
            </Link>
          </p>
          <p className="text-sm text-neutral-600 text-center">
            <Link to="/forgot-password" className="text-emerald-700 hover:underline">
              Quên mật khẩu?
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
