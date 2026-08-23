import { AlertTriangle, Home, RefreshCw } from "lucide-react";
import { Link } from "react-router-dom";

export default function ServerError() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--mq-bg)] px-5">
      <div className="text-center">
        <div className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-3xl bg-[var(--mq-error-light)] text-[var(--mq-warning)]">
          <AlertTriangle size={36} />
        </div>

        <h1 className="text-6xl font-bold text-[var(--mq-text)]">500</h1>

        <p className="mt-3 text-lg font-medium text-[var(--mq-text)]">
          Something went wrong
        </p>

        <p className="mt-2 max-w-sm text-sm text-[var(--mq-text-secondary)]">
          We're experiencing technical difficulties. Please try again in a
          moment.
        </p>

        <div className="mt-8 flex items-center justify-center gap-3">
          <button
            onClick={() => window.location.reload()}
            className="flex h-11 items-center gap-2 rounded-xl bg-[var(--mq-primary)] px-6 text-sm font-semibold text-white transition hover:bg-[var(--mq-primary-hover)]"
          >
            <RefreshCw size={16} />
            Try Again
          </button>
          <Link
            to="/"
            className="flex h-11 items-center gap-2 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-6 text-sm font-medium text-[var(--mq-text-secondary)] transition hover:bg-[var(--mq-bg)] hover:text-[var(--mq-text)]"
          >
            <Home size={16} />
            Go Home
          </Link>
        </div>
      </div>
    </div>
  );
}
