import { Link } from 'react-router';
import { Home } from 'lucide-react';

export function NotFoundPage() {
  return (
    <div className="min-h-[80vh] flex items-center justify-center px-6">
      <div className="text-center">
        <div className="text-8xl mb-6">404</div>
        <h1 className="text-3xl mb-4">Không Tìm Thấy Trang</h1>
        <p className="text-xl text-neutral-600 mb-8">
          Trang bạn đang tìm kiếm không tồn tại hoặc đã bị di chuyển.
        </p>
        <Link
          to="/"
          className="inline-flex items-center gap-2 px-8 py-4 bg-blue-600 text-white hover:bg-blue-700 transition-colors"
        >
          <Home className="w-5 h-5" />
          Về Trang Chủ
        </Link>
      </div>
    </div>
  );
}
