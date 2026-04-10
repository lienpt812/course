import { Clock, CheckCircle, Timer, X, XCircle, AlertCircle } from 'lucide-react';

interface Props {
  status: 'PENDING' | 'CONFIRMED' | 'WAITLIST' | 'CANCELLED' | 'REJECTED' | 'EXPIRED';
  waitlistPosition?: number;
}

export function RegistrationStatusBadge({ status, waitlistPosition }: Props) {
  const configs = {
    PENDING: {
      label: 'Chờ Duyệt',
      icon: Clock,
      className: 'bg-amber-50 text-amber-700 border-amber-200',
    },
    CONFIRMED: {
      label: 'Đã Xác Nhận',
      icon: CheckCircle,
      className: 'bg-green-50 text-green-700 border-green-200',
    },
    WAITLIST: {
      label: waitlistPosition ? `Hàng Chờ #${waitlistPosition}` : 'Hàng Chờ',
      icon: Timer,
      className: 'bg-blue-50 text-blue-700 border-blue-200',
    },
    CANCELLED: {
      label: 'Đã Hủy',
      icon: X,
      className: 'bg-neutral-100 text-neutral-600 border-neutral-300',
    },
    REJECTED: {
      label: 'Từ Chối',
      icon: XCircle,
      className: 'bg-red-50 text-red-700 border-red-200',
    },
    EXPIRED: {
      label: 'Hết Hạn',
      icon: AlertCircle,
      className: 'bg-neutral-100 text-neutral-500 border-neutral-300',
    },
  };

  const config = configs[status];
  const Icon = config.icon;

  return (
    <div className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full border text-sm ${config.className}`}>
      <Icon className="w-4 h-4" />
      {config.label}
    </div>
  );
}
