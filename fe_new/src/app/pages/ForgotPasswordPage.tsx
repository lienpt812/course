import { FormEvent, useState } from 'react';
import { Link } from 'react-router';
import { authApi } from '../lib/api';

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [error, setError] = useState('');

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setIsLoading(true);
    try {
      const res = await authApi.forgotPassword(email);
      setMessage(res.message ?? 'Yêu cầu đặt lại mật khẩu đã được gửi.');
      if (res.reset_token) setResetToken(res.reset_token);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[72vh] flex items-center justify-center px-6 py-10 bg-gradient-to-b from-emerald-50 to-neutral-50">
      <div className="w-full max-w-md bg-white border border-emerald-100 rounded-2xl p-8 shadow-xl shadow-emerald-100/40">
        <h1 className="text-3xl mb-2 text-emerald-900">Quên mật khẩu</h1>
        <p className="text-emerald-700 mb-6">Nhập email để nhận liên kết đặt lại mật khẩu.</p>

        <form onSubmit={onSubmit} className="space-y-4">
          <div>
            <label className="block text-sm mb-2 text-neutral-700">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
              required
            />
          </div>

          {error && (
            <p className="text-sm text-red-600" data-testid="forgot-password-form-error">
              {error}
            </p>
          )}

          {message && (
            <div
              data-testid="forgot-password-success"
              className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-sm text-emerald-800"
            >
              <p>{message}</p>
              {resetToken && (
                <p className="mt-2 font-mono text-xs break-all">
                  Token (dev only): <span className="font-semibold">{resetToken}</span>
                </p>
              )}
              {resetToken && (
                <Link
                  to={`/reset-password?token=${resetToken}`}
                  className="inline-block mt-3 text-emerald-700 hover:underline font-medium"
                >
                  Đặt lại mật khẩu ngay →
                </Link>
              )}
            </div>
          )}

          <button
            type="submit"
            disabled={isLoading}
            className="w-full rounded-xl bg-emerald-600 text-white py-2.5 hover:bg-emerald-700 transition-colors disabled:bg-neutral-400 shadow-lg shadow-emerald-200"
          >
            {isLoading ? 'Đang gửi...' : 'Gửi yêu cầu'}
          </button>

          <p className="text-sm text-neutral-600 text-center">
            <Link to="/login" className="text-emerald-700 hover:underline">
              ← Quay lại đăng nhập
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
