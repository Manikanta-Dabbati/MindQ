import { Check, ChevronDown } from "lucide-react";
import { useRef, useEffect, useState } from "react";
import {
  MODEL_OPTIONS,
  getModelOption,
} from "../../types/ai-models";

interface ModelSelectorProps {
  value: string | null;
  onChange: (modelCode: string | null) => void;
  recommendedModel?: string | null;
}

export default function ModelSelector({
  value,
  onChange,
  recommendedModel,
}: ModelSelectorProps) {
  const [isOpen, setIsOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const current = getModelOption(value);

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, []);

  return (
    <div className="relative" ref={ref}>
      <label className="mb-2 block text-sm font-semibold text-[var(--mq-text)]">
        AI Model
      </label>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="flex h-12 w-full items-center justify-between rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 text-left transition hover:border-[var(--mq-primary-border)] focus:border-[var(--mq-primary)] focus:ring-4 focus:ring-[var(--mq-primary)]/10"
      >
        <div className="flex items-center gap-3">
          <current.icon size={18} className="text-[var(--mq-primary)]" />
          <div>
            <span className="text-sm font-medium text-[var(--mq-text)]">{current.role}</span>
            <span className="ml-2 text-xs text-[var(--mq-text-secondary)]">{current.name}</span>
          </div>
        </div>
        <ChevronDown size={18} className={`text-[var(--mq-text-secondary)] transition-transform ${isOpen ? "rotate-180" : ""}`} />
      </button>
      {isOpen && (
        <div className="absolute z-30 mt-1 w-full overflow-hidden rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] shadow-lg">
          <div className="p-2">
            {MODEL_OPTIONS.map((option) => {
              const Icon = option.icon;
              const isSelected = option.modelCode === value;
              const isRecommended = recommendedModel && option.modelCode === recommendedModel;
              return (
                <button
                  key={option.modelCode ?? "auto"}
                  type="button"
                  onClick={() => { onChange(option.modelCode); setIsOpen(false); }}
                  className={`flex w-full items-start gap-3 rounded-lg p-3 text-left transition ${isSelected ? "bg-[var(--mq-primary-light)]" : "hover:bg-[var(--mq-bg)]"}`}
                >
                  <div className={`mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl ${isSelected ? "bg-[var(--mq-primary)] text-white" : "bg-[var(--mq-bg)] text-[var(--mq-text-secondary)]"}`}>
                    <Icon size={18} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className={`text-sm font-semibold ${isSelected ? "text-[var(--mq-primary)]" : "text-[var(--mq-text)]"}`}>{option.role}</span>
                      <span className={`rounded-md px-1.5 py-0.5 text-[10px] font-bold uppercase ${isSelected ? "bg-[var(--mq-primary)]/10 text-[var(--mq-primary)]" : "bg-[var(--mq-bg)] text-[var(--mq-text-muted)]"}`}>{option.badge}</span>
                      {isRecommended && !isSelected && (
                        <span className="rounded-md bg-[var(--mq-success-light)] px-1.5 py-0.5 text-[10px] font-bold text-[var(--mq-success)]">FOR THIS DIFFICULTY</span>
                      )}
                    </div>
                    <p className="mt-0.5 text-xs text-[var(--mq-text-secondary)]">{option.name}</p>
                    <p className="mt-1 text-xs text-[var(--mq-text-muted)]">{option.description}</p>
                  </div>
                  {isSelected && <Check size={18} className="mt-1 shrink-0 text-[var(--mq-primary)]" />}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}