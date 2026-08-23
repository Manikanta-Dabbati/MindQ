import { createContext, useContext, useEffect, useState, useCallback, useMemo, type ReactNode } from "react";

type OnboardingStatus = "not_started" | "in_progress" | "completed" | "skipped";

interface OnboardingContextType {
  status: OnboardingStatus;
  currentStep: number;
  totalSteps: number;
  isActive: boolean;
  startTour: () => void;
  skipTour: () => void;
  completeTour: () => void;
  nextStep: () => void;
  prevStep: () => void;
  goToStep: (step: number) => void;
  resetTour: () => void;
}

const STORAGE_KEY = "mindq_onboarding";
const TOTAL_STEPS = 7;

const OnboardingContext = createContext<OnboardingContextType | undefined>(undefined);

function getSavedStatus(): OnboardingStatus {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === "completed" || saved === "skipped") return saved;
  } catch { /* localStorage unavailable */ }
  return "not_started";
}

export function OnboardingProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<OnboardingStatus>(getSavedStatus);
  const [currentStep, setCurrentStep] = useState(0);

  // Persist status changes
  useEffect(() => {
    try {
      if (status === "completed" || status === "skipped") {
        localStorage.setItem(STORAGE_KEY, status);
      }
    } catch { /* localStorage unavailable */ }
  }, [status]);

  const isActive = status === "in_progress";

  const startTour = useCallback(() => {
    setStatus("in_progress");
    setCurrentStep(0);
  }, []);

  const skipTour = useCallback(() => {
    setStatus("skipped");
    setCurrentStep(0);
  }, []);

  const completeTour = useCallback(() => {
    setStatus("completed");
    setCurrentStep(0);
  }, []);

  const nextStep = useCallback(() => {
    setCurrentStep((prev) => {
      if (prev >= TOTAL_STEPS - 1) {
        return prev;
      }
      return prev + 1;
    });
  }, []);

  const prevStep = useCallback(() => {
    setCurrentStep((prev) => Math.max(0, prev - 1));
  }, []);

  const goToStep = useCallback((step: number) => {
    setCurrentStep(Math.max(0, Math.min(step, TOTAL_STEPS - 1)));
  }, []);

  const resetTour = useCallback(() => {
    setStatus("not_started");
    setCurrentStep(0);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch { /* localStorage unavailable */ }
  }, []);

  const value = useMemo(
    () => ({
      status,
      currentStep,
      totalSteps: TOTAL_STEPS,
      isActive,
      startTour,
      skipTour,
      completeTour,
      nextStep,
      prevStep,
      goToStep,
      resetTour,
    }),
    [status, currentStep, isActive, startTour, skipTour, completeTour, nextStep, prevStep, goToStep, resetTour],
  );

  return (
    <OnboardingContext.Provider value={value}>
      {children}
    </OnboardingContext.Provider>
  );
}

export function useOnboarding(): OnboardingContextType {
  const ctx = useContext(OnboardingContext);
  if (!ctx) throw new Error("useOnboarding must be used within an OnboardingProvider");
  return ctx;
}
