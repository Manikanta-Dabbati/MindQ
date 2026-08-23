import { Brain, CheckCircle2, ChevronDown, ChevronUp, Loader2, X, XCircle } from "lucide-react";
import { createPortal } from "react-dom";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { SkeletonQuizHistory } from "../components/ui/Skeleton";
import * as mcqService from "../services/mcqService";
import type { McqSetResponse } from "../types/mcq";
import type { QuizAnswerData } from "../services/mcqService";

interface HistoryItem {
  id: number;
  mcqSetId: number;
  mcqSetTitle: string;
  materialTitle: string | null;
  score: number;
  totalQuestions: number;
  percentage: number;
  timeSpentSeconds: number;
  status: string;
  startedAt: string;
  completedAt: string | null;
}

export default function QuizHistory() {
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Modal state
  const [selectedQuiz, setSelectedQuiz] = useState<McqSetResponse | null>(null);
  const [attemptAnswers, setAttemptAnswers] = useState<Map<number, QuizAnswerData>>(new Map());
  const [loadingQuiz, setLoadingQuiz] = useState(false);
  const [expandedQuestions, setExpandedQuestions] = useState<Set<number>>(new Set());


  useEffect(() => {
    mcqService
      .getQuizHistory()
      .then(setHistory)
      .catch(() => setError("Failed to load quiz history."))
      .finally(() => setLoading(false));
  }, []);

  const handleViewQuestions = async (item: HistoryItem) => {
    setLoadingQuiz(true);
    setSelectedQuiz(null);
    setAttemptAnswers(new Map());
    setExpandedQuestions(new Set());

    try {
      // Fetch both the MCQ set and the attempt answers in parallel
      const [quiz, answers] = await Promise.all([
        mcqService.getMcqSet(item.mcqSetId),
        mcqService.getAttemptAnswers(item.id),
      ]);
      setSelectedQuiz(quiz);
      // Build a map of questionId -> answer for quick lookup
      const answerMap = new Map<number, QuizAnswerData>();
      answers.forEach((a) => answerMap.set(a.questionId, a));
      setAttemptAnswers(answerMap);
    } catch {
      // silently fail
    } finally {
      setLoadingQuiz(false);
    }
  };

  const toggleQuestion = (questionId: number) => {
    setExpandedQuestions((prev) => {
      const next = new Set(prev);
      if (next.has(questionId)) {
        next.delete(questionId);
      } else {
        next.add(questionId);
      }
      return next;
    });
  };

  const formatTime = (seconds: number) => {
    if (seconds < 60) return `${seconds}s`;
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}m ${secs}s`;
  };

  // Check if we have answer data for this quiz
  const hasAnswers = attemptAnswers.size > 0;

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <section>
        <div className="mb-3 inline-flex items-center gap-2 rounded-full bg-[var(--mq-ai-light)] px-3 py-1.5 text-xs font-semibold text-[var(--mq-ai)]">
          <Brain size={14} /> Quiz History
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-[var(--mq-text)] sm:text-3xl">Quiz History</h1>
        <p className="mt-2 text-sm text-[var(--mq-text-secondary)]">Review your past quiz attempts and track your progress.</p>
      </section>

      {loading ? (
        <SkeletonQuizHistory />
      ) : error ? (
        <div className="rounded-2xl border border-dashed border-[var(--mq-error-border)] bg-[var(--mq-surface)] px-6 py-16 text-center">
          <p className="text-sm text-[var(--mq-error)]">{error}</p>
          <button onClick={() => window.location.reload()} className="mt-3 text-sm font-medium text-[var(--mq-primary)] hover:text-[var(--mq-primary-hover)]">Try again</button>
        </div>
      ) : history.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-[var(--mq-text-muted)] bg-[var(--mq-surface)] px-6 py-16 text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-[var(--mq-primary-light)] text-[var(--mq-primary)]">
            <Brain size={21} />
          </div>
          <h3 className="mt-4 font-semibold text-[var(--mq-text)]">No quizzes yet</h3>
          <p className="mx-auto mt-2 max-w-sm text-sm text-[var(--mq-text-secondary)]">Generate your first quiz from AI Studio to see your history here.</p>
          <Link to="/ai-studio" className="mt-4 inline-flex items-center gap-2 rounded-xl bg-[var(--mq-primary)] px-5 py-2.5 text-sm font-semibold text-white hover:bg-[var(--mq-primary-hover)]">Generate MCQs</Link>
        </div>
      ) : (
        <div className="space-y-3">
          {history.map((item) => (
            <button
              key={item.id}
              onClick={() => handleViewQuestions(item)}
              className="flex w-full items-center gap-4 rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5 text-left shadow-[var(--mq-shadow-sm)] transition hover:border-[var(--mq-primary-border)] hover:shadow-md cursor-pointer"
            >
              <div className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-xl ${item.status === "COMPLETED" ? "bg-[var(--mq-success-light)]" : "bg-[var(--mq-error-light)]"}`}>
                {item.status === "COMPLETED" ? <CheckCircle2 size={22} className="text-[var(--mq-success)]" /> : <XCircle size={22} className="text-[var(--mq-error)]" />}
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold text-[var(--mq-text)]">{item.mcqSetTitle}</p>
                <p className="mt-1 text-xs text-[var(--mq-text-secondary)]">
                  {item.totalQuestions} questions · {new Date(item.startedAt).toLocaleDateString()}
                  {item.timeSpentSeconds > 0 && <> · {formatTime(item.timeSpentSeconds)}</>}
                </p>
              </div>
              <div className="text-right">
                <p className="text-lg font-bold text-[var(--mq-primary)]">{Math.round(item.percentage)}%</p>
                <p className="text-xs text-[var(--mq-text-secondary)]">{item.score}/{item.totalQuestions}</p>
              </div>
              <div className="ml-2 text-[var(--mq-text-muted)]">
                <ChevronDown size={18} />
              </div>
            </button>
          ))}
        </div>
      )}

      {createPortal(
        (selectedQuiz || loadingQuiz) && (
        <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/50 backdrop-blur-sm p-4 pt-8 pb-8">
          <div className="w-full max-w-3xl rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] shadow-2xl">
            {/* Modal Header */}
            <div className="sticky top-0 z-10 flex items-center justify-between border-b border-[var(--mq-border)] bg-[var(--mq-surface)] px-6 py-4 rounded-t-2xl">
              {loadingQuiz ? (
                <div className="flex items-center gap-3">
                  <Loader2 size={20} className="animate-spin text-[var(--mq-primary)]" />
                  <span className="text-sm text-[var(--mq-text-secondary)]">Loading questions...</span>
                </div>
              ) : selectedQuiz ? (
                <div>
                  <h2 className="text-lg font-semibold text-[var(--mq-text)]">{selectedQuiz.title}</h2>
                  <p className="mt-0.5 text-xs text-[var(--mq-text-secondary)]">
                    {selectedQuiz.totalQuestions} questions · {selectedQuiz.difficulty}
                    {selectedQuiz.topic && <> · {selectedQuiz.topic}</>}
                    {hasAnswers && <> · Your answers shown below</>}
                  </p>
                </div>
              ) : null}
              <button
                onClick={() => { setSelectedQuiz(null); setExpandedQuestions(new Set()); setAttemptAnswers(new Map()); }}
                className="rounded-lg p-2 text-[var(--mq-text-muted)] hover:bg-[var(--mq-bg)] hover:text-[var(--mq-text)]"
              >
                <X size={18} />
              </button>
            </div>

            {/* Score Summary */}
            {selectedQuiz && hasAnswers && (() => {
              const total = attemptAnswers.size;
              const correct = Array.from(attemptAnswers.values()).filter(a => a.isCorrect).length;
              const wrong = total - correct;
              const pct = total > 0 ? Math.round((correct / total) * 100) : 0;
              return (
                <div className="mx-6 mt-4 flex items-center gap-3 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-bg)] px-4 py-3">
                  <div className="flex items-center gap-1.5">
                    <span className="flex h-6 w-6 items-center justify-center rounded-full bg-[var(--mq-success)] text-[10px] font-bold text-white">✓</span>
                    <span className="text-sm font-semibold text-[var(--mq-success)]">{correct}</span>
                    <span className="text-xs text-[var(--mq-text-muted)]">correct</span>
                  </div>
                  <div className="h-4 w-px bg-[var(--mq-border)]" />
                  <div className="flex items-center gap-1.5">
                    <span className="flex h-6 w-6 items-center justify-center rounded-full bg-[var(--mq-error)] text-[10px] font-bold text-white">✗</span>
                    <span className="text-sm font-semibold text-[var(--mq-error)]">{wrong}</span>
                    <span className="text-xs text-[var(--mq-text-muted)]">wrong</span>
                  </div>
                  <div className="h-4 w-px bg-[var(--mq-border)]" />
                  <div className="flex items-center gap-1.5">
                    <span className="text-sm font-bold text-[var(--mq-primary)]">{pct}%</span>
                    <span className="text-xs text-[var(--mq-text-muted)]">score</span>
                  </div>
                  <div className="ml-auto">
                    <div className="h-1.5 w-24 overflow-hidden rounded-full bg-[var(--mq-border)]">
                      <div className="h-full rounded-full transition-all" style={{ width: pct + '%', backgroundColor: pct >= 70 ? 'var(--mq-success)' : pct >= 40 ? '#f59e0b' : 'var(--mq-error)' }} />
                    </div>
                  </div>
                </div>
              );
            })()}

            {/* Modal Body */}
            {selectedQuiz && (
              <div className="px-6 py-4 space-y-3">
                {selectedQuiz.questions.map((q, idx) => {
                  const isExpanded = expandedQuestions.has(q.id);
                  const answer = attemptAnswers.get(q.id);
                  const isAnswerCorrect = answer?.isCorrect ?? false;

                  return (
                    <div
                      key={q.id}
                      className={`rounded-xl border transition ${
                        isExpanded
                          ? "border-[var(--mq-primary)] bg-[var(--mq-primary-light)]/30"
                          : answer
                            ? isAnswerCorrect
                              ? "border-l-4 border-l-[var(--mq-success)] border-[var(--mq-border)] bg-[var(--mq-bg)]"
                              : "border-l-4 border-l-[var(--mq-error)] border-[var(--mq-border)] bg-[var(--mq-bg)]"
                            : "border-[var(--mq-border)] bg-[var(--mq-bg)]"
                      }`}
                    >
                      <button
                        onClick={() => toggleQuestion(q.id)}
                        className="flex w-full items-start gap-3 px-4 py-3 text-left"
                      >
                        <span className={`mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[10px] font-bold ${
                          answer ? (isAnswerCorrect ? "bg-[var(--mq-success)] text-white" : "bg-[var(--mq-error)] text-white") : "bg-[var(--mq-primary)] text-white"
                        }`}>
                          {answer ? (isAnswerCorrect ? "✓" : "✗") : idx + 1}
                        </span>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-[var(--mq-text)]">{q.questionText}</p>
                          {answer && !isExpanded && (
                            <div className="mt-1.5 flex items-center gap-2 flex-wrap">
                              <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                                isAnswerCorrect
                                  ? "bg-[var(--mq-success-light)] text-[var(--mq-success)]"
                                  : "bg-[var(--mq-error-light)] text-[var(--mq-error)]"
                              }`}>
                                {isAnswerCorrect ? "✓ Correct" : "✗ Wrong"}
                              </span>
                              {!isAnswerCorrect && answer.selectedOptionText && (
                                <span className="text-xs text-[var(--mq-text-muted)]">
                                  Your answer: <span className="text-[var(--mq-error)] font-medium">{answer.selectedOptionText}</span>
                                </span>
                              )}
                              {!isAnswerCorrect && answer.correctOptionText && (
                                <span className="text-xs text-[var(--mq-text-muted)]">
                                  Correct: <span className="text-[var(--mq-success)] font-medium">{answer.correctOptionText}</span>
                                </span>
                              )}
                            </div>
                          )}
                        </div>
                        {isExpanded ? (
                          <ChevronUp size={16} className="mt-0.5 shrink-0 text-[var(--mq-text-muted)]" />
                        ) : (
                          <ChevronDown size={16} className="mt-0.5 shrink-0 text-[var(--mq-text-muted)]" />
                        )}
                      </button>

                      {isExpanded && (
                        <div className="px-4 pb-4 space-y-2">
                          {/* Options */}
                          {q.options
                            .sort((a, b) => a.optionOrder - b.optionOrder)
                            .map((option) => {
                              const isThisCorrect = answer?.correctOptionId === option.id;
                              const isThisSelected = answer?.selectedOptionId === option.id;
                              const showAsCorrect = isThisCorrect;
                              const showAsWrong = isThisSelected && !isThisCorrect;

                              return (
                                <div
                                  key={option.id}
                                  className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition ${
                                    showAsCorrect
                                      ? "bg-[var(--mq-success-light)] text-[var(--mq-success)] font-medium border border-[var(--mq-success)]/30"
                                      : showAsWrong
                                        ? "bg-[var(--mq-error-light)] text-[var(--mq-error)] font-medium border border-[var(--mq-error)]/30"
                                        : "bg-[var(--mq-surface)] text-[var(--mq-text)]"
                                  }`}
                                >
                                  <span className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[10px] font-bold ${
                                    showAsCorrect
                                      ? "bg-[var(--mq-success)] text-white"
                                      : showAsWrong
                                        ? "bg-[var(--mq-error)] text-white"
                                        : "bg-[var(--mq-border)] text-[var(--mq-text-secondary)]"
                                  }`}>
                                    {option.optionOrder + 1}
                                  </span>
                                  <span className="flex-1">{option.optionText}</span>
                                  {showAsCorrect && (
                                    <span className="flex items-center gap-1 text-[10px] font-semibold">
                                      <CheckCircle2 size={12} />
                                      {isThisSelected ? "Your answer ✓" : "Correct answer"}
                                    </span>
                                  )}
                                  {showAsWrong && (
                                    <span className="flex items-center gap-1 text-[10px] font-semibold">
                                      <XCircle size={12} />
                                      Your answer ✗
                                    </span>
                                  )}
                                </div>
                              );
                            })}

                          {/* Explanation */}
                          {q.explanation && (
                            <div className="mt-2 rounded-lg border border-[var(--mq-border)] bg-[var(--mq-bg)] px-3 py-2.5">
                              <p className="text-[10px] font-semibold uppercase tracking-wider text-[var(--mq-text-muted)] mb-1">Explanation</p>
                              <p className="text-xs text-[var(--mq-text-secondary)] leading-relaxed">{q.explanation}</p>
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
        ),
        document.body
      )}
    </div>
  );
}
