interface SkeletonProps {
  className?: string;
  lines?: number;
  variant?: "text" | "circle" | "rect";
}

export function Skeleton({ className = "", variant = "text", lines = 1 }: SkeletonProps) {
  if (variant === "circle") {
    return <div className={`mq-skeleton rounded-full ${className}`} />;
  }
  if (variant === "rect") {
    return <div className={`mq-skeleton rounded-xl ${className}`} />;
  }
  return (
    <div className={`space-y-2.5 ${className}`}>
      {Array.from({ length: lines }).map((_, i) => (
        <div
          key={i}
          className="mq-skeleton h-4"
          style={{ width: i === lines - 1 ? "70%" : "100%" }}
        />
      ))}
    </div>
  );
}

export function SkeletonCard({ className = "" }: { className?: string }) {
  return (
    <div className={`rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5 shadow-[var(--mq-shadow-sm)] ${className}`}>
      <div className="flex items-start gap-3">
        <Skeleton variant="rect" className="h-11 w-11 shrink-0 rounded-xl" />
        <div className="flex-1 space-y-2">
          <Skeleton className="h-4 w-3/4" />
          <Skeleton className="h-3 w-1/2" />
        </div>
      </div>
      <div className="mt-4 space-y-2">
        <Skeleton lines={2} />
      </div>
    </div>
  );
}

export function SkeletonDashboard() {
  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <div className="flex items-end justify-between">
        <div className="space-y-2">
          <Skeleton className="h-3 w-24" />
          <Skeleton className="h-8 w-64" />
          <Skeleton className="h-4 w-48" />
        </div>
        <div className="flex gap-2">
          <Skeleton variant="rect" className="h-10 w-36 rounded-xl" />
          <Skeleton variant="rect" className="h-10 w-36 rounded-xl" />
        </div>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <SkeletonCard key={i} />
        ))}
      </div>
      <div className="grid gap-6 xl:grid-cols-[1.4fr_0.8fr]">
        <SkeletonCard />
        <Skeleton variant="rect" className="h-56 rounded-2xl" />
      </div>
    </div>
  );
}

export function SkeletonVault() {
  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <div className="flex items-end justify-between">
        <div className="space-y-2">
          <Skeleton className="h-3 w-32" />
          <Skeleton className="h-8 w-48" />
        </div>
        <Skeleton variant="rect" className="h-10 w-36 rounded-xl" />
      </div>
      <Skeleton variant="rect" className="h-16 rounded-2xl" />
      <Skeleton variant="rect" className="h-14 rounded-2xl" />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <SkeletonCard key={i} />
        ))}
      </div>
    </div>
  );
}

export function SkeletonQuizHistory() {
  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div className="space-y-2">
        <Skeleton variant="rect" className="h-6 w-32 rounded-full" />
        <Skeleton className="h-8 w-40" />
        <Skeleton className="h-4 w-64" />
      </div>
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <SkeletonCard key={i} />
        ))}
      </div>
    </div>
  );
}

export function SkeletonAnalytics() {
  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <div className="space-y-2">
        <Skeleton className="h-8 w-32" />
        <Skeleton className="h-4 w-56" />
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <SkeletonCard key={i} />
        ))}
      </div>
      <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <SkeletonCard />
        <div className="space-y-4">
          <SkeletonCard />
          <SkeletonCard />
        </div>
      </div>
    </div>
  );
}
