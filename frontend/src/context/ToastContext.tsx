import { createContext, useCallback, useContext, useState, type ReactNode } from "react";
import { CheckCircle2, Info, AlertTriangle, X, XCircle } from "lucide-react";

type ToastType = "success" | "error" | "warning" | "info";

interface Toast {
  id: number;
  type: ToastType;
  message: string;
  exiting?: boolean;
}

interface ToastContextType {
  toast: (type: ToastType, message: string) => void;
  success: (message: string) => void;
  error: (message: string) => void;
  warning: (message: string) => void;
  info: (message: string) => void;
  dismiss: (id: number) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

let nextId = 0;

const typeConfig: Record<ToastType, { icon: typeof CheckCircle2; styles: string }> = {
  success: {
    icon: CheckCircle2,
    styles: "border-[var(--mq-success-border)] bg-[var(--mq-success-light)] text-[var(--mq-success)]",
  },
  error: {
    icon: XCircle,
    styles: "border-[var(--mq-error-border)] bg-[var(--mq-error-light)] text-[var(--mq-error)]",
  },
  warning: {
    icon: AlertTriangle,
    styles: "border-[var(--mq-warning-border)] bg-[var(--mq-warning-light)] text-[var(--mq-warning)]",
  },
  info: {
    icon: Info,
    styles: "border-[var(--mq-primary-border)] bg-[var(--mq-primary-light)] text-[var(--mq-primary)]",
  },
};

const iconColors: Record<ToastType, string> = {
  success: "text-[var(--mq-success)]",
  error: "text-[var(--mq-error)]",
  warning: "text-[var(--mq-warning)]",
  info: "text-[var(--mq-primary)]",
};

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const dismiss = useCallback((id: number) => {
    setToasts((prev) =>
      prev.map((t) => (t.id === id ? { ...t, exiting: true } : t)),
    );
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 200);
  }, []);

  const addToast = useCallback(
    (type: ToastType, message: string) => {
      const id = ++nextId;
      setToasts((prev) => [...prev, { id, type, message }]);
      setTimeout(() => dismiss(id), 4000);
    },
    [dismiss],
  );

  const contextValue: ToastContextType = {
    toast: addToast,
    success: (msg) => addToast("success", msg),
    error: (msg) => addToast("error", msg),
    warning: (msg) => addToast("warning", msg),
    info: (msg) => addToast("info", msg),
    dismiss,
  };

  return (
    <ToastContext.Provider value={contextValue}>
      {children}

      {/* Toast container */}
      <div
        aria-live="polite"
        aria-label="Notifications"
        className="pointer-events-none fixed inset-x-0 top-4 z-[200] flex flex-col items-center gap-2 px-4"
      >
        {toasts.map((t) => {
          const config = typeConfig[t.type];
          const Icon = config.icon;
          return (
            <div
              key={t.id}
              role="alert"
              className={`pointer-events-auto flex w-full max-w-sm items-center gap-3 rounded-xl border px-4 py-3 shadow-lg
                ${config.styles}
                ${t.exiting ? "mq-toast-exit" : "mq-toast-enter"}`}
            >
              <Icon size={18} className={`shrink-0 ${iconColors[t.type]}`} />
              <p className="flex-1 text-sm font-medium">{t.message}</p>
              <button
                onClick={() => dismiss(t.id)}
                className="shrink-0 rounded-lg p-1 opacity-60 transition hover:opacity-100"
                aria-label="Dismiss notification"
              >
                <X size={14} />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextType {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used within a ToastProvider");
  }
  return context;
}
