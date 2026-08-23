import { type InputHTMLAttributes, forwardRef, type ReactNode } from "react";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
  icon?: ReactNode;
  containerClassName?: string;
}

const Input = forwardRef<HTMLInputElement, InputProps>(
  (
    {
      label,
      error,
      hint,
      icon,
      containerClassName = "",
      className = "",
      id,
      ...props
    },
    ref,
  ) => {
    const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, "-") : undefined);

    return (
      <div className={containerClassName}>
        {label && (
          <label
            htmlFor={inputId}
            className="mb-1.5 block text-sm font-medium text-[var(--mq-text)]"
          >
            {label}
          </label>
        )}
        <div className="relative">
          {icon && (
            <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)]">
              {icon}
            </span>
          )}
          <input
            ref={ref}
            id={inputId}
            className={`h-11 w-full rounded-xl border bg-[var(--mq-surface)] px-4 text-sm text-[var(--mq-text)] outline-none transition
              ${icon ? "pl-11" : ""}
              ${error
                ? "border-[var(--mq-error-border)] focus:border-red-500 focus:ring-4 focus:ring-red-500/10"
                : "border-[var(--mq-border)] focus:border-[var(--mq-primary)] focus:ring-4 focus:ring-[var(--mq-primary)]/10"
              }
              placeholder:text-[var(--mq-text-muted)]
              disabled:cursor-not-allowed disabled:opacity-50 disabled:bg-[var(--mq-surface-hover)]
              ${className}`}
            {...props}
          />
        </div>
        {error && (
          <p className="mt-1.5 text-xs text-[var(--mq-error)]">{error}</p>
        )}
        {hint && !error && (
          <p className="mt-1.5 text-xs text-[var(--mq-text-muted)]">{hint}</p>
        )}
      </div>
    );
  },
);

Input.displayName = "Input";

export default Input;
