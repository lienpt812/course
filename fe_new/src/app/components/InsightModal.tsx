import { X } from 'lucide-react';

interface InsightModalProps {
  open: boolean;
  title: string;
  description: string;
  onClose: () => void;
}

export function InsightModal({ open, title, description, onClose }: InsightModalProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[70] bg-black/35 backdrop-blur-sm flex items-center justify-center p-4" onClick={onClose}>
      <div
        className="w-full max-w-lg bg-white border border-emerald-100 rounded-2xl shadow-2xl shadow-emerald-200/40"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-6 py-4 border-b border-emerald-100">
          <h3 className="text-xl text-emerald-900">{title}</h3>
          <button onClick={onClose} className="p-2 rounded-lg hover:bg-emerald-50" aria-label="Close detail modal">
            <X className="w-4 h-4 text-emerald-700" />
          </button>
        </div>
        <div className="px-6 py-5">
          <p className="text-neutral-700 leading-relaxed">{description}</p>
        </div>
        <div className="px-6 pb-5">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-emerald-600 text-white hover:bg-emerald-700 transition-colors"
          >
            Đã hiểu
          </button>
        </div>
      </div>
    </div>
  );
}
