import { Sparkles } from "lucide-react";
import { useOnboarding } from "../../context/OnboardingContext";

export default function WelcomeModal() {
  const { status, startTour, skipTour } = useOnboarding();

  if (status !== "not_started") return null;

  return (
    <div className="fixed inset-0 z-[1100] flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-md rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-8 text-center shadow-2xl">
        <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-[var(--mq-primary)] to-[var(--mq-ai)] text-white">
          <Sparkles size={26} />
        </div>
        <h2 className="text-xl font-bold text-[var(--mq-text)]">Welcome to MindQ</h2>
        <p className="mt-2 text-sm leading-relaxed text-[var(--mq-text-secondary)]">
          Let’s take a quick tour so you can get the most out of MindQ.
        </p>
        <div className="mt-6 flex flex-col gap-3">
          <button
            onClick={startTour}
            className="flex h-11 items-center justify-center gap-2 rounded-xl bg-[var(--mq-primary)] px-6 text-sm font-semibold text-white transition hover:bg-[var(--mq-primary-hover)]"
          >
            Start Tour
          </button>
          <button
            onClick={skipTour}
            className="text-sm font-medium text-[var(--mq-text-muted)] transition hover:text-[var(--mq-text-secondary)]"
          >
            Skip for now
          </button>
        </div>
      </div>
    </div>
  );
}
