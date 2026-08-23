import {
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  CircleAlert,
  Clock3,
  Download,
  Flag,
  Loader2,
  RotateCcw,
  Save,
  XCircle,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import type { McqSetResponse, QuizMode } from "../types/mcq";
import type { AnswerResult } from "../services/quizService";
import * as quizService from "../services/quizService";
import * as mcqService from "../services/mcqService";
import { useToast, FocusTrap } from "../components/ui";

interface QuizState {
  mcqSet: McqSetResponse;
  quizMode?: QuizMode;
}

export default function MCQQuiz() {
  const location = useLocation();
  const state = location.state as QuizState | null;
  const mcqSet = state?.mcqSet;

  if (!mcqSet) {
    return (
      <div className="mx-auto max-w-4xl space-y-6">
        <Link to="/ai-studio" className="inline-flex items-center gap-2 text-sm font-medium text-[var(--mq-text-secondary)] transition hover:text-[var(--mq-primary)]">
          <ArrowLeft size={17} /> Back to AI Studio
        </Link>
        <div className="rounded-2xl border border-dashed border-[var(--mq-text-muted)] bg-[var(--mq-surface)] px-6 py-16 text-center">
          <p className="text-sm text-[var(--mq-text-secondary)]">No quiz data found. Please generate a quiz from AI Studio first.</p>
          <Link to="/ai-studio" className="mt-4 inline-flex items-center gap-2 rounded-xl bg-[var(--mq-primary)] px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-[var(--mq-primary-hover)]">
            Go to AI Studio
          </Link>
        </div>
      </div>
    );
  }

  const questions = mcqSet.questions;
  const [currentQuestion, setCurrentQuestion] = useState(0);
  const [selectedAnswers, setSelectedAnswers] = useState<Record<number, number>>({});
  const [finished, setFinished] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [resultAnswers, setResultAnswers] = useState<AnswerResult[] | null>(null);
  const [downloading, setDownloading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const toast = useToast();

  // Quiz mode from AI Studio
  const quizMode = state?.quizMode ?? "PRACTICE";
  const isTimed = quizMode === "TIMED" || quizMode === "EXAM";

  // Timer: 1 min per question for TIMED, 2 min for EXAM
  const timerMinutes = quizMode === "EXAM" ? questions.length * 2 : questions.length;
  const [timeRemaining, setTimeRemaining] = useState(timerMinutes * 60);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const submitRef = useRef(false);

  const handleAutoSubmit = useCallback(() => {
    if (submitRef.current || finished) return;
    submitRef.current = true;
    handleSubmit();
  }, [finished]);

  // Timer countdown
  useEffect(() => {
    if (!isTimed || finished) return;

    timerRef.current = setInterval(() => {
      setTimeRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(timerRef.current!);
          handleAutoSubmit();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isTimed, finished, handleAutoSubmit]);

  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, "0")}`;
  };

  const timerUrgent = timeRemaining <= 60;

  const question = questions[currentQuestion];
  const selectedOptionId = selectedAnswers[question.id];
  const progress = ((currentQuestion + 1) / questions.length) * 100;
  const answeredCount = Object.keys(selectedAnswers).length;
  const unansweredCount = questions.length - answeredCount;

  const handleSelect = (optionId: number) => {
    if (finished || submitting) return;
    setSelectedAnswers((prev) => ({ ...prev, [question.id]: optionId }));
  };

  const handleNext = () => {
    if (currentQuestion < questions.length - 1) {
      setCurrentQuestion((prev) => prev + 1);
    } else {
      // On last question, show confirmation
      setShowConfirm(true);
    }
  };

  const handlePrevious = () => {
    if (currentQuestion > 0) setCurrentQuestion((prev) => prev - 1);
  };

  const handleConfirmSubmit = () => {
    setShowConfirm(false);
    handleSubmit();
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    setSubmitError("");
    try {
      const answers: quizService.QuizAnswerSubmission[] = Object.entries(selectedAnswers).map(
        ([qId, optId]) => ({ questionId: Number(qId), selectedOptionId: optId }),
      );
      const result = await quizService.submitQuiz(mcqSet.id, answers, {
        quizMode,
        timeLimitMinutes: isTimed ? timerMinutes : undefined,
        timeSpentSeconds: isTimed ? timerMinutes * 60 - timeRemaining : undefined,
      });
      setResultAnswers(result.answers);
      setFinished(true);
    } catch (err: unknown) {
      if (typeof err === "object" && err !== null && "response" in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        setSubmitError(axiosErr.response?.data?.message ?? "Failed to submit quiz.");
      } else {
        setSubmitError("Failed to submit quiz.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  const restartQuiz = () => {
    setCurrentQuestion(0);
    setSelectedAnswers({});
    setFinished(false);
    setResultAnswers(null);
    setSubmitError("");
  };

  const handleDownload = async () => {
    setDownloading(true);
    try {
      const blob = await mcqService.downloadQuiz(mcqSet.id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${mcqSet.title.replace(/[^a-zA-Z0-9\s-]/g, "").replace(/\s+/g, "_")}.pdf`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      toast.success("Quiz downloaded successfully");
    } catch {
      toast.error("Failed to download quiz. Please try again.");
    } finally {
      setDownloading(false);
    }
  };

  const handleSaveToVault = async () => {
    setSaving(true);
    try {
      await mcqService.saveQuizToVault(mcqSet.id);
      setSaved(true);
      toast.success("Quiz saved to Knowledge Vault");
    } catch {
      toast.error("Failed to save quiz. Please try again.");
    } finally {
      setSaving(false);
    }
  };

  const score = useMemo(() => resultAnswers?.filter((a) => a.isCorrect).length ?? 0, [resultAnswers]);
  const percentage = useMemo(() => {
    if (!resultAnswers || questions.length === 0) return 0;
    return Math.round((score / questions.length) * 100);
  }, [score, resultAnswers, questions.length]);

  /* RESULT SCREEN */
  if (finished && resultAnswers) {
    const incorrect = questions.length - score;
    return (
      <div className="mx-auto max-w-5xl space-y-6">
        <div className="flex items-center justify-between">
          <Link to="/ai-studio" className="inline-flex items-center gap-2 text-sm font-medium text-[var(--mq-text-secondary)] transition hover:text-[var(--mq-primary)]">
            <ArrowLeft size={17} /> Back to AI Studio
          </Link>
          <span className="inline-flex items-center gap-2 text-sm text-[var(--mq-text-secondary)]">
            <CheckCircle2 size={17} /> Quiz completed
          </span>
        </div>

        <section className="overflow-hidden rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] shadow-[var(--mq-shadow-sm)]">
          <div className="bg-gradient-to-r from-[var(--mq-primary)] to-[var(--mq-ai)] px-6 py-8 text-white sm:px-10">
            <p className="text-sm font-medium text-white/80">{mcqSet.title}</p>
            <h1 className="mt-1 text-2xl font-bold sm:text-3xl">Quiz Results</h1>
            <p className="mt-2 text-sm text-white/75">Here&apos;s how you performed in this practice quiz.</p>
          </div>
          <div className="grid gap-6 p-6 sm:grid-cols-3 sm:p-8">
            <div className="rounded-xl bg-[var(--mq-bg)] p-5 text-center">
              <p className="text-xs font-semibold uppercase tracking-wide text-[var(--mq-text-secondary)]">Your Score</p>
              <p className="mt-2 text-4xl font-bold text-[var(--mq-primary)]">{percentage}%</p>
              <p className="mt-1 text-sm text-[var(--mq-text-secondary)]">{score} of {questions.length} correct</p>
            </div>
            <div className="rounded-xl bg-[var(--mq-success-light)] p-5 text-center">
              <CheckCircle2 size={24} className="mx-auto text-[var(--mq-success)]" />
              <p className="mt-2 text-2xl font-bold text-[var(--mq-success)]">{score}</p>
              <p className="text-sm text-[var(--mq-success)]">Correct answers</p>
            </div>
            <div className="rounded-xl bg-[var(--mq-error-light)] p-5 text-center">
              <XCircle size={24} className="mx-auto text-[var(--mq-error)]" />
              <p className="mt-2 text-2xl font-bold text-[var(--mq-error)]">{incorrect}</p>
              <p className="text-sm text-[var(--mq-error)]">Incorrect answers</p>
            </div>
          </div>
          <div className="flex flex-col gap-3 border-t border-[var(--mq-border)] p-6 sm:flex-row sm:justify-center">
            <button type="button" onClick={restartQuiz} className="inline-flex items-center justify-center gap-2 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-5 py-3 text-sm font-semibold text-[var(--mq-text)] transition hover:bg-[var(--mq-bg)]">
              <RotateCcw size={17} /> Try Again
            </button>
            <button type="button" onClick={handleDownload} disabled={downloading} className="inline-flex items-center justify-center gap-2 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-5 py-3 text-sm font-semibold text-[var(--mq-text)] transition hover:bg-[var(--mq-bg)] disabled:opacity-50">
              <Download size={17} /> {downloading ? "Downloading..." : "Download Quiz"}
            </button>
            <button type="button" onClick={handleSaveToVault} disabled={saving || saved} className="inline-flex items-center justify-center gap-2 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-5 py-3 text-sm font-semibold text-[var(--mq-text)] transition hover:bg-[var(--mq-bg)] disabled:opacity-50">
              <Save size={17} /> {saved ? "Saved!" : saving ? "Saving..." : "Save to Vault"}
            </button>
            <Link to="/ai-studio" className="inline-flex items-center justify-center gap-2 rounded-xl bg-[var(--mq-primary)] px-5 py-3 text-sm font-semibold text-white transition hover:bg-[var(--mq-primary-hover)]">
              Generate Another Quiz <ArrowRight size={17} />
            </Link>
          </div>
        </section>

        <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-6 shadow-[var(--mq-shadow-sm)] sm:p-8">
          <div className="border-b border-[var(--mq-border)] pb-5">
            <h2 className="text-xl font-bold text-[var(--mq-text)]">Answer Review</h2>
            <p className="mt-1 text-sm text-[var(--mq-text-secondary)]">Review your answers and learn from your mistakes.</p>
          </div>
          <div className="mt-6 space-y-5">
            {resultAnswers.map((answer, index) => (
              <div key={answer.questionId} className={`rounded-xl border p-5 ${answer.isCorrect ? "border-[var(--mq-success-border)] bg-[var(--mq-success-light)]/40" : "border-[var(--mq-error-border)] bg-[var(--mq-error-light)]/40"}`}>
                <div className="flex items-start gap-3">
                  {answer.isCorrect ? <CheckCircle2 size={21} className="mt-0.5 shrink-0 text-[var(--mq-success)]" /> : <XCircle size={21} className="mt-0.5 shrink-0 text-[var(--mq-error)]" />}
                  <div className="min-w-0">
                    <p className="text-xs font-semibold uppercase tracking-wide text-[var(--mq-text-secondary)]">Question {index + 1}</p>
                    <h3 className="mt-1 font-semibold leading-6 text-[var(--mq-text)]">{answer.questionText}</h3>
                  </div>
                </div>
                <div className="mt-4 rounded-lg bg-[var(--mq-surface)] p-4">
                  <p className="text-xs font-semibold text-[var(--mq-text-secondary)]">Your answer</p>
                  <p className={`mt-1 text-sm font-medium ${answer.isCorrect ? "text-[var(--mq-success)]" : "text-[var(--mq-error)]"}`}>{answer.selectedOptionText ?? "Not answered"}</p>
                </div>
                {!answer.isCorrect && (
                  <div className="mt-3 rounded-lg border border-[var(--mq-success-border)] bg-[var(--mq-success-light)] p-4">
                    <p className="text-xs font-semibold text-[var(--mq-success)]">Correct answer</p>
                    <p className="mt-1 text-sm font-medium text-[var(--mq-success)]">{answer.correctOptionText}</p>
                  </div>
                )}
                <div className="mt-3 flex gap-3 rounded-lg bg-[var(--mq-surface)] p-4">
                  <CircleAlert size={18} className="mt-0.5 shrink-0 text-[var(--mq-primary)]" />
                  <div>
                    <p className="text-xs font-semibold text-[var(--mq-primary)]">Explanation</p>
                    <p className="mt-1 text-sm leading-6 text-[var(--mq-text-secondary)]">{answer.explanation}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>
    );
  }

  /* QUIZ SCREEN */
  return (
    <>
      <div className="mx-auto max-w-4xl space-y-6">
        <div className="flex items-center justify-between">
          <Link to="/ai-studio" className="inline-flex items-center gap-2 text-sm font-medium text-[var(--mq-text-secondary)] transition hover:text-[var(--mq-primary)]">
            <ArrowLeft size={17} /> Back to AI Studio
          </Link>
          <div className="flex items-center gap-3">
            {isTimed && (
              <div
                role="timer"
                aria-live="polite"
                aria-atomic="true"
                className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-semibold ${timerUrgent ? "bg-[var(--mq-error-light)] text-[var(--mq-error)]" : "bg-[var(--mq-primary-light)] text-[var(--mq-primary)]"}`}
              >
                <Clock3 size={15} />
                {formatTime(timeRemaining)}
              </div>
            )}
            <span className="text-xs font-medium text-[var(--mq-text-secondary)]">
              {quizMode === "EXAM" ? "📝 Exam Mode" : quizMode === "TIMED" ? "⏱ Timed Quiz" : "🎯 Practice Quiz"}
            </span>
          </div>
        </div>

        <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5 shadow-[var(--mq-shadow-sm)] sm:p-6">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-xs font-medium text-[var(--mq-text-secondary)]">{mcqSet.title}</p>
              <h1 className="mt-1 text-lg font-bold text-[var(--mq-text)]">AI Generated Quiz</h1>
            </div>
            <div className="text-left sm:text-right">
              <p className="text-sm font-semibold text-[var(--mq-text)]">Question {currentQuestion + 1} of {questions.length}</p>
              <p className="mt-1 text-xs text-[var(--mq-text-secondary)]">{mcqSet.difficulty} difficulty</p>
            </div>
          </div>
          <div className="mt-5">
            <div className="h-2 overflow-hidden rounded-full bg-[var(--mq-border)]">
              <div className="h-full rounded-full bg-[var(--mq-primary)] transition-all duration-300" style={{ width: `${progress}%` }} />
            </div>
          </div>
        </section>

        {submitError && <div className="rounded-xl border border-[var(--mq-error-border)] bg-[var(--mq-error-light)] px-4 py-3 text-sm text-[var(--mq-error)]">{submitError}</div>}

        <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-6 shadow-[var(--mq-shadow-sm)] sm:p-8">
          <div className="flex items-start justify-between gap-4">
            <span className="rounded-lg bg-[var(--mq-primary-light)] px-3 py-1.5 text-xs font-semibold text-[var(--mq-primary)]">Question {currentQuestion + 1}</span>
            <button type="button" className="rounded-lg p-2 text-[var(--mq-text-muted)] transition hover:bg-[var(--mq-bg)] hover:text-[var(--mq-text-secondary)]" title="Flag question" aria-label="Flag question">
              <Flag size={17} />
            </button>
          </div>
          <h2 className="mt-6 text-xl font-semibold leading-8 text-[var(--mq-text)] sm:text-2xl">{question.questionText}</h2>
          <div className="mt-7 space-y-3">
            {question.options.map((option, optionIndex) => {
              const selected = selectedOptionId === option.id;
              return (
                <button key={option.id} type="button" onClick={() => handleSelect(option.id)}
                  className={`flex w-full items-center gap-4 rounded-xl border p-4 text-left transition ${selected ? "border-[var(--mq-primary)] bg-[var(--mq-primary-light)] text-[var(--mq-text)]" : "border-[var(--mq-border)] bg-[var(--mq-surface)] text-[var(--mq-text-secondary)] hover:border-[var(--mq-primary-border)] hover:bg-[var(--mq-bg)]"}`}>
                  <span className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border text-sm font-semibold ${selected ? "border-[var(--mq-primary)] bg-[var(--mq-primary)] text-white" : "border-[var(--mq-text-muted)] bg-[var(--mq-surface)] text-[var(--mq-text-secondary)]"}`}>
                    {String.fromCharCode(65 + optionIndex)}
                  </span>
                  <span className="text-sm font-medium sm:text-base">{option.optionText}</span>
                </button>
              );
            })}
          </div>
          <div className="mt-8 flex items-center justify-between border-t border-[var(--mq-border)] pt-6">
            <button type="button" onClick={handlePrevious} disabled={currentQuestion === 0}
              className="inline-flex items-center gap-2 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 py-2.5 text-sm font-semibold text-[var(--mq-text)] transition hover:bg-[var(--mq-bg)] disabled:cursor-not-allowed disabled:opacity-40">
              <ArrowLeft size={17} /> Previous
            </button>
            <button type="button" onClick={handleNext} disabled={submitting}
              className="inline-flex items-center gap-2 rounded-xl bg-[var(--mq-primary)] px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-[var(--mq-primary-hover)] disabled:cursor-not-allowed disabled:opacity-60">
              {submitting ? <><Loader2 size={17} className="animate-spin" /> Submitting...</> : currentQuestion === questions.length - 1 ? "Finish Quiz" : <>Next Question <ArrowRight size={17} /></>}
            </button>
          </div>
        </section>

        {/* Question Navigator — Mobile: collapsible, Desktop: full grid */}
        <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5 shadow-[var(--mq-shadow-sm)]">
          <div className="mb-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <p className="text-xs font-semibold uppercase tracking-wide text-[var(--mq-text-muted)]">Questions</p>
              {/* Progress summary */}
              <span className="rounded-full bg-[var(--mq-primary-light)] px-2.5 py-0.5 text-xs font-semibold text-[var(--mq-primary)]">
                {answeredCount}/{questions.length}
              </span>
            </div>
            <div className="flex items-center gap-3 text-xs">
              <span className="hidden items-center gap-1.5 sm:flex">
                <span className="h-2.5 w-2.5 rounded-full bg-[var(--mq-primary)]" />
                <span className="text-[var(--mq-text-secondary)]">Current</span>
              </span>
              <span className="hidden items-center gap-1.5 sm:flex">
                <span className="h-2.5 w-2.5 rounded-full bg-[var(--mq-primary-light)]" />
                <span className="text-[var(--mq-text-secondary)]">Answered</span>
              </span>
              <span className="hidden items-center gap-1.5 sm:flex">
                <span className="h-2.5 w-2.5 rounded-full bg-[var(--mq-surface-hover)]" />
                <span className="text-[var(--mq-text-secondary)]">Unanswered</span>
              </span>
            </div>
          </div>
          {/* Mobile: compact horizontal scroll, Desktop: full grid */}
          <div className="flex gap-2 overflow-x-auto pb-2 sm:overflow-visible sm:pb-0">
            <div className="grid min-w-max grid-cols-10 gap-2 sm:w-full sm:min-w-0">
              {questions.map((item, index) => {
                const answered = selectedAnswers[item.id] !== undefined;
                const active = index === currentQuestion;
                return (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => setCurrentQuestion(index)}
                    className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl text-xs font-semibold transition sm:w-full
                      ${active
                        ? "bg-[var(--mq-primary)] text-white shadow-sm ring-2 ring-[var(--mq-primary)]/20"
                        : answered
                          ? "bg-[var(--mq-primary-light)] text-[var(--mq-primary)] hover:bg-[var(--mq-primary-border)]"
                          : "bg-[var(--mq-bg)] text-[var(--mq-text-secondary)] hover:bg-[var(--mq-surface-hover)]"
                      }`}
                    aria-label={`Question ${index + 1}${answered ? " (answered)" : " (unanswered)"}`}
                  >
                    {index + 1}
                  </button>
                );
              })}
            </div>
          </div>
          {/* Mobile scroll hint */}
          <p className="mt-2 text-center text-xs text-[var(--mq-text-muted)] sm:hidden">
            Swipe to see all questions
          </p>
        </section>
      </div>

      {/* Submit Confirmation Dialog */}
      {showConfirm && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/30 px-4 backdrop-blur-[2px]" role="dialog" aria-modal="true" aria-label="Submit quiz confirmation">
          <FocusTrap onEscape={() => setShowConfirm(false)}>
          <div className="w-full max-w-md overflow-hidden rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] shadow-[0_20px_60px_rgba(0, 0, 0, 0.15)]">
            <div className="p-6 text-center">
              <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-[var(--mq-primary-light)] text-[var(--mq-primary)]">
                <CheckCircle2 size={28} />
              </div>
              <h3 className="mt-4 text-lg font-bold text-[var(--mq-text)]">Submit Quiz?</h3>
              {unansweredCount > 0 ? (
                <p className="mt-2 text-sm text-[var(--mq-text-secondary)]">
                  You have <span className="font-semibold text-[var(--mq-primary)]">{unansweredCount} unanswered question{unansweredCount !== 1 ? "s" : ""}</span>. These will be marked as incorrect.
                </p>
              ) : (
                <p className="mt-2 text-sm text-[var(--mq-text-secondary)]">
                  All questions answered. Ready to submit your quiz.
                </p>
              )}
            </div>
            <div className="flex gap-3 border-t border-[var(--mq-border)] px-6 py-4">
              <button
                type="button"
                onClick={() => setShowConfirm(false)}
                className="flex-1 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 py-2.5 text-sm font-semibold text-[var(--mq-text)] transition hover:bg-[var(--mq-bg)]"
              >
                Continue Quiz
              </button>
              <button
                type="button"
                onClick={handleConfirmSubmit}
                disabled={submitting}
                className="flex-1 inline-flex items-center justify-center gap-2 rounded-xl bg-[var(--mq-primary)] px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-[var(--mq-primary-hover)] disabled:opacity-60"
              >
                {submitting ? <><Loader2 size={16} className="animate-spin" /> Submitting...</> : "Submit Quiz"}
              </button>
            </div>
          </div>
          </FocusTrap>
        </div>
      )}
    </>
  );
}
