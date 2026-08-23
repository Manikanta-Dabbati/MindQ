import { type ButtonHTMLAttributes, type ReactNode, forwardRef } from "react";
import { Loader2 } from "lucide-react";

type Variant = "primary" | "secondary" | "ghost" | "danger" | "ai";
type Size = "sm" | "md" | "lg";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
  icon?: ReactNode;
  children: ReactNode;
}

const variantStyles: Record<Variant, string> = {
  primary:
    "mq-btn-primary bg-[var(--mq-primary)] text-white hover:bg-[var(--mq-primary-hover)] shadow-sm",
  secondary:
    "mq-btn-secondary border border-[var(--mq-border)] bg-[var(--mq-surface)] text-[var(--mq-text)] hover:bg-[var(--mq-surface-hover)]",
  ghost:
    "bg-transparent text-[var(--mq-text-secondary)] hover:bg-[var(--mq-surface-hover)] hover:text-[var(--mq-text)]",
  danger:
    "border border-[var(--mq-error-border)] bg-[var(--mq-surface)] text-[var(--mq-error)] hover:bg-[var(--mq-error-light)]",
  ai: "mq-btn-primary bg-gradient-to-r from-[var(--mq-primary)] to-[var(--mq-ai)] text-white hover:opacity-95 shadow-sm",
};

const sizeStyles: Record<Size, string> = {
  sm: "h-9 px-3.5 text-xs gap-1.5 rounded-lg",
  md: "h-10 px-5 text-sm gap-2 rounded-xl",
  lg: "h-12 px-6 text-sm gap-2 rounded-xl",
};

const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      variant = "primary",
      size = "md",
      loading = false,
      icon,
      disabled,
      className = "",
      children,
      ...props
    },
    ref,
  ) => {
    return (
      <button
        ref={ref}
        disabled={disabled || loading}
        className={`inline-flex items-center justify-center font-semibold transition
          ${variantStyles[variant]}
          ${sizeStyles[size]}
          ${(disabled || loading) ? "cursor-not-allowed opacity-60" : ""}
          ${className}`}
        {...props}
      >
        {loading ? (
          <Loader2 size={size === "sm" ? 14 : 16} className="animate-spin" />
        ) : icon ? (
          <span className="shrink-0">{icon}</span>
        ) : null}
        {children}
      </button>
    );
  },
);

Button.displayName = "Button";

export default Button;
