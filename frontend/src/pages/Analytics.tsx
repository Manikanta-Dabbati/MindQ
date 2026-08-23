import {
  BarChart3,
  Brain,
  Calendar,
  CheckCircle2,
  Flame,
  Target,
  TrendingUp,
  Trophy,
  XCircle,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { SkeletonAnalytics } from "../components/ui/Skeleton";
import * as analyticsService from "../services/analyticsService";
import type { AnalyticsOverview, TopicPerformance } from "../types/analytics";

export default function Analytics() {
  const [overview, setOverview] = useState<AnalyticsOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const data = await analyticsService.getAnalyticsOverview();
        if (!cancelled) setOverview(data);
      } catch {
        if (!cancelled) setError("Failed to load analytics data.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return <SkeletonAnalytics />;
  }

  if (error) {
    return (
      <div className="mx-auto max-w-7xl space-y-6">
        <div className="rounded-xl border border-[var(--mq-error-border)] bg-[var(--mq-error-light)] px-4 py-3 text-sm text-[var(--mq-error)]">
          {error}
        </div>
      </div>
    );
  }

  if (!overview) return null;

  const accuracyRate =
    overview.totalQuestions > 0
      ? Math.round((overview.correctAnswers / overview.totalQuestions) * 100)
      : 0;

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-[var(--mq-text)]">
          Analytics
        </h1>
        <p className="mt-1 text-sm text-[var(--mq-text-secondary)]">
          Track your learning progress and identify weak areas.
        </p>
      </div>

      {error && (
        <div className="rounded-xl border border-[var(--mq-error-border)] bg-[var(--mq-error-light)] px-4 py-3 text-sm text-[var(--mq-error)]">
          {error}
        </div>
      )}

      {/* Overview stats */}
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          icon={Brain}
          label="Quizzes Completed"
          value={String(overview.totalQuizzes)}
          iconClass="bg-[var(--mq-ai-light)] text-[var(--mq-ai)]"
        />
        <StatCard
          icon={Target}
          label="Accuracy"
          value={overview.totalQuestions > 0 ? `${accuracyRate}%` : "—"}
          iconClass="bg-[var(--mq-success-light)] text-[var(--mq-success)]"
        />
        <StatCard
          icon={TrendingUp}
          label="Avg. Score"
          value={overview.averageScore > 0 ? `${overview.averageScore}%` : "—"}
          iconClass="bg-[var(--mq-primary-light)] text-[var(--mq-primary)]"
        />
        <StatCard
          icon={Flame}
          label="Study Streak"
          value={`${overview.studyStreak} day${overview.studyStreak !== 1 ? "s" : ""}`}
          iconClass="bg-[var(--mq-warning-light)] text-[var(--mq-warning)]"
        />
      </section>

      <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        {/* Topic performance */}
        <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5 shadow-[var(--mq-shadow-sm)] sm:p-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-semibold text-[var(--mq-text)]">
                Topic Performance
              </h2>
              <p className="mt-1 text-sm text-[var(--mq-text-secondary)]">
                See where you excel and where to improve.
              </p>
            </div>
          </div>

          <div className="mt-5 space-y-4">
            {overview.topicPerformance.length === 0 ? (
              <div className="rounded-xl border border-dashed border-[var(--mq-text-muted)] bg-[var(--mq-bg)] px-4 py-8 text-center">
                <p className="text-sm text-[var(--mq-text-secondary)]">
                  No topic data yet. Take a quiz to see your performance.
                </p>
              </div>
            ) : (
              overview.topicPerformance.map((topic) => (
                <TopicBar key={topic.topic} topic={topic} />
              ))
            )}
          </div>
        </section>

        {/* Quick stats */}
        <section className="space-y-4">
          <div className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5 shadow-[var(--mq-shadow-sm)] sm:p-6">
            <h2 className="font-semibold text-[var(--mq-text)]">Quick Stats</h2>
            <div className="mt-4 space-y-4">
              <div className="flex items-center justify-between rounded-xl bg-[var(--mq-bg)] px-4 py-3">
                <div className="flex items-center gap-3">
                  <CheckCircle2 size={18} className="text-[var(--mq-success)]" />
                  <span className="text-sm text-[var(--mq-text-secondary)]">Correct</span>
                </div>
                <span className="text-sm font-semibold text-[var(--mq-text)]">
                  {overview.correctAnswers}
                </span>
              </div>
              <div className="flex items-center justify-between rounded-xl bg-[var(--mq-bg)] px-4 py-3">
                <div className="flex items-center gap-3">
                  <XCircle size={18} className="text-[var(--mq-error)]" />
                  <span className="text-sm text-[var(--mq-text-secondary)]">Incorrect</span>
                </div>
                <span className="text-sm font-semibold text-[var(--mq-text)]">
                  {overview.incorrectAnswers}
                </span>
              </div>
              <div className="flex items-center justify-between rounded-xl bg-[var(--mq-bg)] px-4 py-3">
                <div className="flex items-center gap-3">
                  <BarChart3 size={18} className="text-[var(--mq-primary)]" />
                  <span className="text-sm text-[var(--mq-text-secondary)]">
                    Total Questions
                  </span>
                </div>
                <span className="text-sm font-semibold text-[var(--mq-text)]">
                  {overview.totalQuestions}
                </span>
              </div>
            </div>
          </div>

          {/* Favorite topic */}
          {overview.favoriteTopic && (
            <div className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5 shadow-[var(--mq-shadow-sm)] sm:p-6">
              <h2 className="font-semibold text-[var(--mq-text)]">Top Topic</h2>
              <div className="mt-3 flex items-center gap-3 rounded-xl bg-[var(--mq-ai-light)] px-4 py-3">
                <Trophy size={18} className="text-[var(--mq-ai)]" />
                <span className="text-sm font-medium text-[var(--mq-text)]">
                  {overview.favoriteTopic}
                </span>
              </div>
            </div>
          )}

          {/* Resources */}
          <div className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5 shadow-[var(--mq-shadow-sm)] sm:p-6">
            <h2 className="font-semibold text-[var(--mq-text)]">Resources</h2>
            <div className="mt-4 space-y-3">
              <div className="flex items-center justify-between text-sm">
                <span className="text-[var(--mq-text-secondary)]">Materials</span>
                <span className="font-medium text-[var(--mq-text)]">
                  {overview.totalMaterials}
                </span>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-[var(--mq-text-secondary)]">Flashcard Sets</span>
                <span className="font-medium text-[var(--mq-text)]">
                  {overview.totalFlashcards}
                </span>
              </div>
            </div>
          </div>
        </section>
      </div>

      {/* Recent activity */}
      {overview.recentActivity.length > 0 && (
        <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5 shadow-[var(--mq-shadow-sm)] sm:p-6">
          <h2 className="font-semibold text-[var(--mq-text)]">
            Recent Activity (Last 7 Days)
          </h2>
          <div className="mt-5 divide-y divide-[var(--mq-border-light)]">
            {overview.recentActivity.map((activity) => (
              <div
                key={activity.date}
                className="flex items-center justify-between py-3 first:pt-0 last:pb-0"
              >
                <div className="flex items-center gap-3">
                  <Calendar size={16} className="text-[var(--mq-text-muted)]" />
                  <span className="text-sm text-[var(--mq-text-secondary)]">
                    {new Date(activity.date).toLocaleDateString("en-US", {
                      weekday: "short",
                      month: "short",
                      day: "numeric",
                    })}
                  </span>
                </div>
                <div className="flex items-center gap-4 text-sm">
                  <span className="text-[var(--mq-text-secondary)]">
                    {activity.quizzesTaken} quiz
                    {activity.quizzesTaken !== 1 ? "zes" : ""}
                  </span>
                  <span className="text-[var(--mq-text-secondary)]">
                    {activity.questionsAnswered} Qs
                  </span>
                  <span className="font-medium text-[var(--mq-text)]">
                    {activity.averageScore > 0
                      ? `${Math.round(activity.averageScore)}%`
                      : "—"}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* CTA */}
      <section className="rounded-2xl bg-gradient-to-br from-[var(--mq-primary)] to-[var(--mq-ai)] p-6 text-white">
        <h2 className="text-lg font-bold">Ready to improve?</h2>
        <p className="mt-2 text-sm text-white/80">
          Generate a new quiz to practice your weak areas and boost your scores.
        </p>
        <Link
          to="/ai-studio"
          className="mt-4 inline-flex items-center gap-2 rounded-xl bg-[var(--mq-surface)] px-4 py-2.5 text-sm font-semibold text-[var(--mq-primary)] transition hover:bg-[var(--mq-primary-light)]"
        >
          Go to AI Studio
        </Link>
      </section>
    </div>
  );
}

function StatCard({
  icon: Icon,
  label,
  value,
  iconClass,
}: {
  icon: typeof Brain;
  label: string;
  value: string;
  iconClass: string;
}) {
  return (
    <div className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5 shadow-[var(--mq-shadow-sm)]">
      <div
        className={`flex h-10 w-10 items-center justify-center rounded-xl ${iconClass}`}
      >
        <Icon size={19} />
      </div>
      <p className="mt-5 text-sm text-[var(--mq-text-secondary)]">{label}</p>
      <p className="mt-1 text-2xl font-bold text-[var(--mq-text)]">{value}</p>
    </div>
  );
}

function TopicBar({ topic }: { topic: TopicPerformance }) {
  const percentage = Math.round(topic.accuracy);
  const barColor =
    percentage >= 70
      ? "bg-[var(--mq-success)]"
      : percentage >= 40
        ? "bg-[var(--mq-warning)]"
        : "bg-[var(--mq-error)]";

  return (
    <div>
      <div className="flex items-center justify-between text-sm">
        <span className="font-medium text-[var(--mq-text)]">{topic.topic}</span>
        <span className="text-[var(--mq-text-secondary)]">
          {topic.correctAnswers}/{topic.totalQuestions} ({percentage}%)
        </span>
      </div>
      <div className="mt-2 h-2.5 overflow-hidden rounded-full bg-[var(--mq-surface-hover)]">
        <div
          className={`h-full rounded-full transition-all ${barColor}`}
          style={{ width: `${percentage}%` }}
        />
      </div>
    </div>
  );
}
