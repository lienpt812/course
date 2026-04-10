import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { authApi, UserRole } from '../lib/api';

function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

function isStrongPassword(value: string): boolean {
  return /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,64}$/.test(value);
}

function isValidPhone(value: string): boolean {
  return /^\+?[0-9]{9,15}$/.test(value);
}

function dashboardPathByRole(role: string): string {
  if (role === 'ADMIN') return '/admin/dashboard';
  if (role === 'INSTRUCTOR') return '/instructor/dashboard';
  return '/student/dashboard';
}

export function RegisterPage() {
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<UserRole>('STUDENT');
  const [phone, setPhone] = useState('');
  const [expertise, setExpertise] = useState('');
  const [learningGoal, setLearningGoal] = useState('');
  const [studentMajor, setStudentMajor] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const inputCls =
    'w-full px-4 py-2.5 border border-emerald-200 rounded-xl focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100';

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError('');

    const cleanName = name.trim();
    const cleanEmail = email.trim().toLowerCase();
    const cleanPhone = phone.trim();
    const cleanExpertise = expertise.trim();
    const cleanLearningGoal = learningGoal.trim();
    const cleanStudentMajor = studentMajor.trim();

    if (cleanName.length < 2 || cleanName.length > 100) {
      setError('Họ tên phải từ 2 đến 100 ký tự.');
      return;
    }
    if (!isValidEmail(cleanEmail)) {
      setError('Email không đúng định dạng.');
      return;
    }
    if (!isStrongPassword(password)) {
      setError('Mật khẩu phải có 8-64 ký tự, gồm chữ hoa, chữ thường và số.');
      return;
    }
    if (cleanPhone && !isValidPhone(cleanPhone)) {
      setError('Số điện thoại không hợp lệ (chỉ số, 9-15 ký tự, có thể bắt đầu bằng +).');
      return;
    }

    if (role === 'INSTRUCTOR' && cleanExpertise.length < 3) {
      setError('Instructor bắt buộc nhập chuyên môn giảng dạy.');
      return;
    }
    if (role === 'STUDENT' && !cleanStudentMajor && !cleanLearningGoal) {
      setError('Student cần nhập chuyên ngành hoặc mục tiêu học tập.');
      return;
    }

    setIsLoading(true);

    try {
      const result = await authApi.register({
        name: cleanName,
        email: cleanEmail,
        password,
        role,
        phone: cleanPhone || undefined,
        expertise: role === 'INSTRUCTOR' ? cleanExpertise || undefined : undefined,
        learning_goal: role === 'STUDENT' ? cleanLearningGoal || undefined : undefined,
        student_major: role === 'STUDENT' ? cleanStudentMajor || undefined : undefined,
      });
      localStorage.setItem('access_token', result.access_token);
      localStorage.setItem('refresh_token', result.refresh_token);
      localStorage.setItem('auth_user', JSON.stringify(result.user));
      navigate(dashboardPathByRole(result.user.role));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Đăng ký thất bại');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[72vh] flex items-center justify-center px-6 py-10 bg-gradient-to-b from-emerald-50 to-neutral-50">
      <div className="w-full max-w-lg bg-white border border-emerald-100 rounded-2xl p-8 shadow-xl shadow-emerald-100/40">
        <h1 className="text-3xl mb-2 text-emerald-900">Đăng ký</h1>
        <p className="text-emerald-700 mb-6">Tạo tài khoản mới để truy cập hệ thống.</p>

        <form onSubmit={onSubmit} className="space-y-4">
          <div>
            <label className="block text-sm mb-2 text-neutral-700">Họ tên <span className="text-red-600">*</span></label>
            <input type="text" value={name} onChange={(e) => setName(e.target.value)} className={inputCls} required minLength={2} maxLength={100} />
          </div>

          <div>
            <label className="block text-sm mb-2 text-neutral-700">Email <span className="text-red-600">*</span></label>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} className={inputCls} required maxLength={255} />
          </div>

          <div>
            <label className="block text-sm mb-2 text-neutral-700">Mật khẩu <span className="text-red-600">*</span></label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} className={inputCls} required minLength={8} maxLength={64} />
            <p className="text-xs text-neutral-500 mt-1">Tối thiểu 8 ký tự, gồm chữ hoa, chữ thường và số.</p>
          </div>

          <div>
            <label className="block text-sm mb-2 text-neutral-700">Vai trò <span className="text-red-600">*</span></label>
            <select value={role} onChange={(e) => setRole(e.target.value as UserRole)} className={`${inputCls} bg-white`}>
              <option value="STUDENT">Student</option>
              <option value="INSTRUCTOR">Instructor</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>

          <div>
            <label className="block text-sm mb-2 text-neutral-700">Số điện thoại</label>
            <input type="text" value={phone} onChange={(e) => setPhone(e.target.value)} className={inputCls} placeholder="Tuỳ chọn" maxLength={15} />
          </div>

          {role === 'INSTRUCTOR' && (
            <div>
              <label className="block text-sm mb-2 text-neutral-700">Chuyên môn giảng dạy <span className="text-red-600">*</span></label>
              <input
                type="text"
                value={expertise}
                onChange={(e) => setExpertise(e.target.value)}
                className={inputCls}
                placeholder="Ví dụ: Backend Python, Data Engineering"
                required
                minLength={3}
                maxLength={500}
              />
            </div>
          )}

          {role === 'STUDENT' && (
            <>
              <div>
                <label className="block text-sm mb-2 text-neutral-700">Chuyên ngành <span className="text-red-600">*</span> <span className="text-xs text-neutral-500">(1 trong 2)</span></label>
                <input
                  type="text"
                  value={studentMajor}
                  onChange={(e) => setStudentMajor(e.target.value)}
                  className={inputCls}
                  placeholder="Ví dụ: Công nghệ thông tin"
                  maxLength={255}
                />
              </div>
              <div>
                <label className="block text-sm mb-2 text-neutral-700">Mục tiêu học tập <span className="text-red-600">*</span> <span className="text-xs text-neutral-500">(1 trong 2)</span></label>
                <input
                  type="text"
                  value={learningGoal}
                  onChange={(e) => setLearningGoal(e.target.value)}
                  className={inputCls}
                  placeholder="Ví dụ: Học để chuyển việc sang backend"
                  maxLength={1000}
                />
              </div>
            </>
          )}

          {error && <p className="text-sm text-red-600">{error}</p>}

          <button
            type="submit"
            disabled={isLoading}
            className="w-full rounded-xl bg-emerald-600 text-white py-2.5 hover:bg-emerald-700 transition-colors disabled:bg-neutral-400 shadow-lg shadow-emerald-200"
          >
            {isLoading ? 'Đang đăng ký...' : 'Tạo tài khoản'}
          </button>

          <p className="text-sm text-neutral-600 text-center">
            Đã có tài khoản? <Link to="/login" className="text-emerald-700 hover:underline">Đăng nhập</Link>
          </p>
        </form>
      </div>
    </div>
  );
}
