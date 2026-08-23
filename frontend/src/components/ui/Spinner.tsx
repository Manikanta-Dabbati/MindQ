import { Loader2 } from "lucide-react";

interface SpinnerProps {
  size?: number;
  className?: string;
  label?: string;
}

export default function Spinner({ size = 24, className = "", label }: SpinnerProps) {
  return (
    <div className={`flex flex-col items-center justify-center gap-3 ${className}`}>
      <Loader2 size={size} className="animate-spin text-[var(--mq-primary)]" />
      {label && (
        <p className="text-sm text-[var(--mq-text-secondary)]">{label}</p>
      )}
    </div>
  );
}

export function FullPageSpinner({ label = "Loading..." }: { label?: string }) {
  return (
    <div className="flex min-h-[40vh] items-center justify-center">
      <Spinner size={28} label={label} />
    </div>
  );
}
