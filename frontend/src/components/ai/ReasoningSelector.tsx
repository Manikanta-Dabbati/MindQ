import { REASONING_OPTIONS } from "../../types/ai-models";

interface ReasoningSelectorProps {
  value: "auto" | "standard" | "deep";
  onChange: (value: "auto" | "standard" | "deep") => void;
  modelCode: string | null;
  disabled?: boolean;
}

export default function ReasoningSelector({
  value,
  onChange,

  disabled = false,
}: ReasoningSelectorProps) {
  return (
    <div>
      <label className="mb-2 block text-sm font-semibold text-[var(--mq-text)]">
        Reasoning
      </label>
      <div className="grid grid-cols-3 gap-3">
        {REASONING_OPTIONS.map((option) => (
          <button
            key={option.value}
            type="button"
            onClick={() => onChange(option.value)}
            disabled={disabled}
            className={`rounded-xl border px-4 py-3 text-left transition ${
              value === option.value
                ? "border-[var(--mq-primary)] bg-[var(--mq-primary-light)]"
                : "border-[var(--mq-border)] bg-[var(--mq-surface)] hover:border-[var(--mq-text-muted)]"
            } ${disabled ? "cursor-not-allowed opacity-60" : ""}`}
          >
            <span className={`text-sm font-medium ${
              value === option.value ? "text-[var(--mq-primary)]" : "text-[var(--mq-text)]"
            }`}>{option.label}</span>
            <p className="mt-1 text-xs text-[var(--mq-text-secondary)]">{option.description}</p>
          </button>
        ))}
      </div>
    </div>
  );
}