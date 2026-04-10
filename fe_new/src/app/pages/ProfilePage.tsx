import { FormEvent, useEffect, useState } from 'react';
import { authApi, AuthUser } from '../lib/api';

export function ProfilePage() {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const [name, setName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [bio, setBio] = useState('');
  const [interests, setInterests] = useState('');
  const [education, setEducation] = useState('');
  const [expertise, setExpertise] = useState('');
  const [studentMajor, setStudentMajor] = useState('');
  const [learningGoal, setLearningGoal] = useState('');

  useEffect(() => {
    authApi
      .me()
      .then((data) => {
        setUser(data);
        setName(data.name || '');
        setPhoneNumber(data.phone_number || '');
        setAvatarUrl(data.avatar_url || '');
        setBio(data.bio || '');
        setInterests(data.interests || '');
        setEducation(data.education || '');
        setExpertise(data.expertise || '');
        setStudentMajor(data.student_major || '');
        setLearningGoal(data.learning_goal || '');
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Không tải được profile'))
      .finally(() => setIsLoading(false));
  }, []);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    setMessage('');
    setIsSaving(true);

    try {
      const updated = await authApi.updateMe({
        name,
        phone_number: phoneNumber || undefined,
        avatar_url: avatarUrl || undefined,
        bio: bio || undefined,
        interests: interests || undefined,
        education: education || undefined,
        expertise: expertise || undefined,
        student_major: studentMajor || undefined,
        learning_goal: learningGoal || undefined,
      });
      setUser(updated);
      localStorage.setItem('auth_user', JSON.stringify(updated));
      setMessage('Cập nhật profile thành công');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Cập nhật profile thất bại');
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return <div className="max-w-5xl mx-auto px-6 py-10 text-neutral-600">Đang tải profile...</div>;
  }

  if (!user) {
    return <div className="max-w-5xl mx-auto px-6 py-10 text-red-600">Không có dữ liệu profile</div>;
  }

  return (
    <div className="max-w-5xl mx-auto px-6 py-10">
      <div className="bg-white border border-emerald-100 rounded-2xl p-8 shadow-lg shadow-emerald-100/30">
        <h1 className="text-3xl mb-2 text-emerald-900">Profile</h1>
        <p className="text-emerald-700 mb-8">Cập nhật thông tin cá nhân theo vai trò {user.role}</p>

        <form onSubmit={onSubmit} className="space-y-5">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm mb-2 text-neutral-700">Họ tên</label>
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
              />
            </div>
            <div>
              <label className="block text-sm mb-2 text-neutral-700">Email</label>
              <input value={user.email} disabled className="w-full px-4 py-2.5 border border-neutral-200 bg-neutral-100" />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm mb-2 text-neutral-700">Số điện thoại</label>
              <input
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
              />
            </div>
            <div>
              <label className="block text-sm mb-2 text-neutral-700">Avatar URL</label>
              <input
                value={avatarUrl}
                onChange={(e) => setAvatarUrl(e.target.value)}
                className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm mb-2 text-neutral-700">Bio</label>
            <textarea
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              rows={3}
              className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
            />
          </div>

          <div>
            <label className="block text-sm mb-2 text-neutral-700">Sở thích</label>
            <input
              value={interests}
              onChange={(e) => setInterests(e.target.value)}
              className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
              placeholder="React, Python, Data..."
            />
          </div>

          <div>
            <label className="block text-sm mb-2 text-neutral-700">Học vấn</label>
            <input
              value={education}
              onChange={(e) => setEducation(e.target.value)}
              className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
            />
          </div>

          {user.role === 'INSTRUCTOR' && (
            <div>
              <label className="block text-sm mb-2 text-neutral-700">Chuyên môn giảng dạy</label>
              <input
                value={expertise}
                onChange={(e) => setExpertise(e.target.value)}
                className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
              />
            </div>
          )}

          {user.role === 'STUDENT' && (
            <>
              <div>
                <label className="block text-sm mb-2 text-neutral-700">Chuyên ngành</label>
                <input
                  value={studentMajor}
                  onChange={(e) => setStudentMajor(e.target.value)}
                  className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
                />
              </div>
              <div>
                <label className="block text-sm mb-2 text-neutral-700">Mục tiêu học tập</label>
                <input
                  value={learningGoal}
                  onChange={(e) => setLearningGoal(e.target.value)}
                  className="w-full px-4 py-2.5 border border-emerald-200 rounded-xl focus:outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
                />
              </div>
            </>
          )}

          {error && <p className="text-sm text-red-600">{error}</p>}
          {message && <p className="text-sm text-green-600">{message}</p>}

          <button
            type="submit"
            disabled={isSaving}
            className="px-6 py-2.5 rounded-xl bg-emerald-600 text-white hover:bg-emerald-700 transition-colors disabled:bg-neutral-400 shadow-lg shadow-emerald-200"
          >
            {isSaving ? 'Đang lưu...' : 'Lưu thay đổi'}
          </button>
        </form>
      </div>
    </div>
  );
}
