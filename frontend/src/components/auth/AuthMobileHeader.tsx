import { Brain, Sparkles } from "lucide-react";

interface AuthMobileHeaderProps {
  title?: string;
  subtitle?: string;
}

export default function AuthMobileHeader({
  title,
  subtitle,
}: AuthMobileHeaderProps) {
  return (
    <div className="relative overflow-hidden bg-gradient-to-br from-[var(--mq-primary)] to-[var(--mq-ai)] px-6 pb-10 pt-8 text-white lg:hidden">
      {/* Decorative circles */}
      <div className="absolute -right-12 -top-12 h-40 w-40 rounded-full bg-[var(--mq-surface)]/10" />
      <div className="absolute -bottom-16 -left-10 h-48 w-48 rounded-full bg-[var(--mq-surface)]/10" />

      {/* Brand */}
      <div className="relative flex items-center gap-2.5">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-[var(--mq-surface)]/15">
          <Brain size={18} />
        </div>
        <div>
          <span className="text-xl font-bold">Mind</span>
          <span className="text-xl font-bold text-white/80">Q</span>
        </div>
      </div>

      <p className="relative mt-1.5 text-xs text-white/60">
        Sync Your Mind with AI
      </p>

      {/* Page title */}
      {title && (
        <div className="relative mt-7">
          <div className="mb-3 flex h-9 w-9 items-center justify-center rounded-lg bg-[var(--mq-surface)]/15">
            <Sparkles size={16} />
          </div>
          <h1 className="text-2xl font-bold leading-tight">{title}</h1>
          {subtitle && (
            <p className="mt-2 max-w-xs text-sm leading-6 text-white/75">
              {subtitle}
            </p>
          )}
        </div>
      )}
    </div>
  );
}
