import {
  ArrowRight,
  FileText,
  MoreHorizontal,
} from "lucide-react";
import { Link } from "react-router-dom";

export interface Material {
  id: number;
  title: string;
  category: string;
  type: string;
  updated: string;
  pages: number;
  description?: string;
}
interface MaterialCardProps {
  material: Material;
}

export default function MaterialCard({
  material,
}: MaterialCardProps) {
  return (
    <Link
      to={`/vault/${material.id}`}
      className="mq-card group block rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5 shadow-[var(--mq-shadow-sm)] hover:border-[var(--mq-primary-border)]"
    >
      {/* Top section */}
      <div className="flex items-start justify-between gap-4">
        <div className="flex min-w-0 items-center gap-3">
          {/* File icon */}
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[var(--mq-primary-light)] text-[var(--mq-primary)] transition group-hover:bg-[var(--mq-primary-light)]">
            <FileText size={20} />
          </div>

          {/* Title */}
          <div className="min-w-0">
            <h3 className="truncate text-sm font-semibold text-[var(--mq-text)] group-hover:text-[var(--mq-primary)]">
              {material.title}
            </h3>

            <p className="mt-1 text-xs text-[var(--mq-text-secondary)]">
              {material.category}
            </p>
          </div>
        </div>

        {/* More icon */}
        <div
          className="shrink-0 rounded-lg p-1.5 text-[var(--mq-text-muted)] transition group-hover:text-[var(--mq-text-secondary)]"
          aria-hidden="true"
        >
          <MoreHorizontal size={18} />
        </div>
      </div>

      {/* Description */}
      {material.description && (
        <p className="mt-4 line-clamp-2 text-sm leading-5 text-[var(--mq-text-secondary)]">
          {material.description}
        </p>
      )}

      {/* Bottom section */}
      <div className="mt-5 flex items-center justify-between border-t border-[var(--mq-border-light)] pt-4">
        <div className="flex items-center gap-2">
          {/* Type */}
          <span className="rounded-md bg-[var(--mq-bg)] px-2 py-1 text-[11px] font-semibold text-[var(--mq-text-secondary)]">
            {material.type}
          </span>

          {/* Updated */}
          <span className="text-xs text-[var(--mq-text-muted)]">
            {material.updated}
          </span>
        </div>

        {/* Open */}
        <span className="inline-flex items-center gap-1 text-xs font-semibold text-[var(--mq-primary)] opacity-0 transition duration-200 group-hover:opacity-100">
          Open
          <ArrowRight
            size={14}
            className="transition-transform duration-200 group-hover:translate-x-0.5"
          />
        </span>
      </div>
    </Link>
  );
}