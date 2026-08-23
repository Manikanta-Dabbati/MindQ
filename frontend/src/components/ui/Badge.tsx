import type { ReactNode } from "react";

type BadgeVariant = "primary" | "ai" | "success" | "warning" | "error" | "neutral";

interface BadgeProps {
  children: ReactNode;
  variant?: BadgeVariant;
  size?: "sm" | "md";
  icon?: ReactNode;
  className?: string;
}

const variantStyles: Record<BadgeVariant, string> = {
  primary: "bg-[var(--mq-primary-light)] text-[var(--mq-primary)]",
  ai: "bg-[var(--mq-ai-light)] text-[var(--mq-ai)]",
  success: "bg-[var(--mq-success-light)] text-[var(--mq-success)]",
  warning: "bg-[var(--mq-warning-light)] text-[var(--mq-warning)]",
  error: "bg-[var(--mq-error-light)] text-[var(--mq-error)]",
  neutral: "bg-[var(--mq-surface-hover)] text-[var(--mq-text-secondary)]",
};

const sizeStyles = {
  sm: "px-2 py-0.5 text-[11px]",
  md: "px-2.5 py-1 text-xs",
};

export default function Badge({
  children,
  variant = "neutral",
  size = "md",
  icon,
  className = "",
}: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center gap-1 font-semibold rounded-full
        ${variantStyles[variant]}
        ${sizeStyles[size]}
        ${className}`}
    >
      {icon && <span className="shrink-0">{icon}</span>}
      {children}
    </span>
  );
}
