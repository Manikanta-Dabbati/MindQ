import type { ReactNode } from "react";
import { BookOpen } from "lucide-react";

interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description: string;
  action?: ReactNode;
  className?: string;
}

export default function EmptyState({
  icon,
  title,
  description,
  action,
  className = "",
}: EmptyStateProps) {
  return (
    <div
      className={`rounded-2xl border border-dashed border-[var(--mq-border)] bg-[var(--mq-surface)] px-6 py-16 text-center ${className}`}
    >
      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-[var(--mq-primary-light)] text-[var(--mq-primary)]">
        {icon || <BookOpen size={21} />}
      </div>
      <h3 className="mt-4 font-semibold text-[var(--mq-text)]">{title}</h3>
      <p className="mx-auto mt-2 max-w-sm text-sm text-[var(--mq-text-secondary)]">
        {description}
      </p>
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}
