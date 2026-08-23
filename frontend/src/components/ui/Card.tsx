import type { ReactNode } from "react";

interface CardProps {
  children: ReactNode;
  className?: string;
  padding?: "none" | "sm" | "md" | "lg";
  hover?: boolean;
}

const paddingStyles = {
  none: "",
  sm: "p-4",
  md: "p-5 sm:p-6",
  lg: "p-6 sm:p-8",
};

export default function Card({
  children,
  className = "",
  padding = "md",
  hover = false,
}: CardProps) {
  return (
    <div
      className={`rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)]
        shadow-[var(--mq-shadow-sm)]
        ${paddingStyles[padding]}
        ${hover ? "transition duration-200 hover:-translate-y-0.5 hover:shadow-[var(--mq-shadow-md)]" : ""}
        ${className}`}
    >
      {children}
    </div>
  );
}
