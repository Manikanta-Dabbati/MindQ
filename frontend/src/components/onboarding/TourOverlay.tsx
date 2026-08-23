import { useCallback, useEffect, useRef, useState } from "react";
import { useOnboarding } from "../../context/OnboardingContext";
import { ChevronLeft, ChevronRight, X, Sparkles } from "lucide-react";

interface TourStep {
  targetId: string;
  title: string;
  description: string;
  side?: "top" | "bottom" | "left" | "right";
  mobileSide?: "top" | "bottom";
}

const TOUR_STEPS: TourStep[] = [
  { targetId: "sidebar-dashboard", title: "Your Learning Command Center", description: "See your recent activity, continue learning, review your progress, and discover what you should practice next.", side: "right", mobileSide: "bottom" },
  { targetId: "sidebar-vault", title: "Store Your Knowledge", description: "Upload your study materials and keep your learning resources organized in one place.", side: "right", mobileSide: "bottom" },
  { targetId: "sidebar-ai-studio", title: "Turn Knowledge Into Practice", description: "Use MindQ’s AI Studio to create learning experiences from your topics or knowledge materials.", side: "right", mobileSide: "bottom" },
  { targetId: "dashboard-generate", title: "Generate Your First Quiz", description: "Pick a topic, choose difficulty, and let MindQ create personalized MCQs to test your understanding.", side: "bottom", mobileSide: "bottom" },
  { targetId: "sidebar-history", title: "Track Your Progress", description: "Review your quiz history, scores, and improvement over time.", side: "right", mobileSide: "bottom" },
  { targetId: "sidebar-analytics", title: "Learn From Results", description: "MindQ helps you understand what you know and where you need more practice.", side: "right", mobileSide: "bottom" },
  { targetId: "topbar-profile", title: "Manage Your Profile", description: "Access settings, change password, switch theme, or take the tour again anytime.", side: "bottom", mobileSide: "bottom" },
];

interface SpotlightRect { top: number; left: number; width: number; height: number; valid: boolean; }

function computeTooltipPosition(spot: SpotlightRect, side: "top" | "bottom" | "left" | "right", tooltipW: number, tooltipH: number, isMobile: boolean): { top: number; left: number } {
  const gap = 12;
  const vw = window.innerWidth;
  const vh = window.innerHeight;
  const scrollY = window.scrollY;

  if (isMobile) {
    const spaceBelow = vh - (spot.top - scrollY + spot.height);
    const spaceAbove = spot.top - scrollY;
    if (spaceBelow >= tooltipH + gap + 16) return { top: spot.top + spot.height + gap, left: Math.max(16, Math.min(spot.left, vw - tooltipW - 16)) };
    if (spaceAbove >= tooltipH + gap + 16) return { top: spot.top - tooltipH - gap, left: Math.max(16, Math.min(spot.left, vw - tooltipW - 16)) };
    return { top: scrollY + vh - tooltipH - 16, left: Math.max(16, Math.min(spot.left, vw - tooltipW - 16)) };
  }

  let preferredLeft = side === "left" ? spot.left - tooltipW - gap : side === "right" ? spot.left + spot.width + gap : spot.left;
  let preferredTop = side === "top" ? spot.top - tooltipH - gap : side === "bottom" ? spot.top + spot.height + gap : spot.top + spot.height / 2 - tooltipH / 2;
  let left = Math.max(16, Math.min(preferredLeft, vw - tooltipW - 16));
  let top = Math.max(scrollY + 16, Math.min(preferredTop, scrollY + vh - tooltipH - 16));

  if (side === "right" && left < spot.left + spot.width + gap) left = Math.max(16, spot.left - tooltipW - gap);
  if (side === "left" && left + tooltipW > spot.left - gap) left = Math.min(vw - tooltipW - 16, spot.left + spot.width + gap);
  return { top, left };
}
export default function TourOverlay() {
  const { status, currentStep, totalSteps, nextStep, prevStep, skipTour, completeTour } = useOnboarding();
  const [spotlight, setSpotlight] = useState<SpotlightRect>({ top: 0, left: 0, width: 0, height: 0, valid: false });
  const [tooltipPos, setTooltipPos] = useState<{ top: number; left: number }>({ top: 0, left: 0 });
  const [tooltipSize, setTooltipSize] = useState({ w: 320, h: 240 });
  const tooltipRef = useRef<HTMLDivElement>(null);
  const step = TOUR_STEPS[currentStep];
  const isMobile = typeof window !== "undefined" && window.innerWidth < 1024;

  const updatePosition = useCallback(() => {
    if (!step) return;
    const el = document.getElementById(step.targetId);
    if (!el) { setSpotlight({ top: 0, left: 0, width: 0, height: 0, valid: false }); return; }
    el.scrollIntoView({ behavior: "smooth", block: "center", inline: "nearest" });
    setTimeout(() => {
      const r = el.getBoundingClientRect();
      const rect: SpotlightRect = { top: r.top + window.scrollY, left: r.left + window.scrollX, width: r.width, height: r.height, valid: true };
      setSpotlight(rect);
      const side = isMobile ? (step.mobileSide || "bottom") : (step.side || "bottom");
      setTooltipPos(computeTooltipPosition(rect, side, tooltipSize.w, tooltipSize.h, isMobile));
    }, 350);
  }, [step, isMobile, tooltipSize]);

  useEffect(() => {
    if (status === "in_progress") {
      updatePosition();
      window.addEventListener("resize", updatePosition);
      return () => window.removeEventListener("resize", updatePosition);
    }
  }, [status, currentStep, updatePosition]);

  useEffect(() => {
    if (tooltipRef.current) {
      const rect = tooltipRef.current.getBoundingClientRect();
      if (rect.width > 0 && rect.height > 0) {
        setTooltipSize({ w: rect.width, h: rect.height });
        if (step && spotlight.valid) {
          const side = isMobile ? (step.mobileSide || "bottom") : (step.side || "bottom");
          setTooltipPos(computeTooltipPosition(spotlight, side, rect.width, rect.height, isMobile));
        }
      }
    }
  }, [status, currentStep, spotlight, step, isMobile]);

  useEffect(() => {
    if (status !== "in_progress") return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === "Escape") { skipTour(); return; }
      if (e.key === "ArrowRight" || e.key === "Enter") {
        if (currentStep === totalSteps - 1) completeTour(); else nextStep();
      }
      if (e.key === "ArrowLeft") prevStep();
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [status, currentStep, totalSteps, nextStep, prevStep, skipTour, completeTour]);

  if (status !== "in_progress" || !spotlight.valid) return null;
  const isLast = currentStep === totalSteps - 1;
  const isFirst = currentStep === 0;
  return (
    <>
      <div className="fixed inset-0 z-[998] bg-black/40 transition-opacity" onClick={skipTour} aria-hidden="true" />
      <div
        className="fixed z-[999] rounded-xl transition-all duration-300 ease-out"
        style={{
          top: spotlight.top - 6,
          left: spotlight.left - 6,
          width: spotlight.width + 12,
          height: spotlight.height + 12,
          boxShadow: "0 0 0 4px var(--mq-primary), 0 0 0 9999px rgba(0,0,0,0.45)",
        }}
      />
      <div
        ref={tooltipRef}
        role="dialog"
        aria-label={step.title}
        className="fixed z-[1000] w-80 max-w-[calc(100vw-32px)] rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5 shadow-2xl"
        style={{ top: tooltipPos.top, left: tooltipPos.left, transition: "top 0.3s ease-out, left 0.3s ease-out" }}
      >
        <div className="mb-3 flex items-center justify-between">
          <span className="text-xs font-semibold text-[var(--mq-primary)]">{currentStep + 1} of {totalSteps}</span>
          <button onClick={skipTour} className="rounded-lg p-1 text-[var(--mq-text-muted)] transition hover:bg-[var(--mq-bg)] hover:text-[var(--mq-text)]" aria-label="Close tour"><X size={16} /></button>
        </div>
        <div className="mb-4 flex items-start gap-3">
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-[var(--mq-primary)]/10 text-[var(--mq-primary)]"><Sparkles size={18} /></div>
          <div>
            <h3 className="text-sm font-bold text-[var(--mq-text)]">{step.title}</h3>
            <p className="mt-1.5 text-sm leading-relaxed text-[var(--mq-text-secondary)]">{step.description}</p>
          </div>
        </div>
        <div className="mb-4 flex items-center justify-center gap-1.5">
          {Array.from({ length: totalSteps }).map((_, i) => (
            <div key={i} className={"h-1.5 rounded-full transition-all duration-200 " + (i === currentStep ? "w-6 bg-[var(--mq-primary)]" : i < currentStep ? "w-1.5 bg-[var(--mq-primary)]/40" : "w-1.5 bg-[var(--mq-border)]")} />
          ))}
        </div>
        <div className="flex items-center justify-between">
          <button onClick={prevStep} disabled={isFirst} className="flex items-center gap-1 rounded-xl px-3 py-1.5 text-sm font-medium text-[var(--mq-text-secondary)] transition hover:bg-[var(--mq-bg)] disabled:invisible"><ChevronLeft size={16} /> Back</button>
          <div className="flex gap-2">
            <button onClick={skipTour} className="rounded-xl px-3 py-1.5 text-sm font-medium text-[var(--mq-text-muted)] transition hover:bg-[var(--mq-bg)] hover:text-[var(--mq-text-secondary)]">Skip</button>
            <button onClick={isLast ? completeTour : nextStep} className="flex items-center gap-1.5 rounded-xl bg-[var(--mq-primary)] px-4 py-2 text-sm font-semibold text-white transition hover:bg-[var(--mq-primary-hover)]">{isLast ? "Finish" : "Next"} {!isLast && <ChevronRight size={16} />}</button>
          </div>
        </div>
      </div>
    </>
  );
}
