import { ArrowLeft, Home, Lock } from "lucide-react";
import { Link } from "react-router-dom";

export default function Forbidden() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--mq-bg)] px-5">
      <div className="text-center">
        <div className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-3xl bg-[var(--mq-error-light)] text-[var(--mq-error)]">
          <Lock size={36} />
        </div>

        <h1 className="text-6xl font-bold text-[var(--mq-text)]">403</h1>

        <p className="mt-3 text-lg font-medium text-[var(--mq-text)]">
          Access denied
        </p>

        <p className="mt-2 max-w-sm text-sm text-[var(--mq-text-secondary)]">
          You don't have permission to access this page. If you believe this is
          an error, please contact support.
        </p>

        <div className="mt-8 flex items-center justify-center gap-3">
          <Link
            to="/dashboard"
            className="flex h-11 items-center gap-2 rounded-xl bg-[var(--mq-primary)] px-6 text-sm font-semibold text-white transition hover:bg-[var(--mq-primary-hover)]"
          >
            <Home size={16} />
            Go to Dashboard
          </Link>
          <button
            onClick={() => window.history.back()}
            className="flex h-11 items-center gap-2 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-6 text-sm font-medium text-[var(--mq-text-secondary)] transition hover:bg-[var(--mq-bg)] hover:text-[var(--mq-text)]"
          >
            <ArrowLeft size={16} />
            Go Back
          </button>
        </div>
      </div>
    </div>
  );
}
