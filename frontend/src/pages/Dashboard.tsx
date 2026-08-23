import {
  ArrowRight,
  BookOpen,
  Brain,
  CheckCircle2,
  FileText,
  HelpCircle,
  Sparkles,
  Target,
  TrendingDown,
  Trophy,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { SkeletonDashboard } from "../components/ui/Skeleton";
import * as dashboardService from "../services/dashboardService";
import type { DashboardStats } from "../types/dashboard";
import { StorageUsageAlert } from "../components/usage/UsageAlerts";

function getGreeting(): string {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 17) return "Good afternoon";
  return "Good evening";
}

export default function Dashboard() {
  const { user } = useAuth();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const data = await dashboardService.getDashboardStats();
        if (!cancelled) setStats(data);
      } catch {
        if (!cancelled) setError("Failed to load dashboard data.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, []);

  const firstName = user?.fullName?.split(" ")[0] ?? "there";

  if (loading) {
    return <SkeletonDashboard />;
  }

  return (
    <>
      <div className="mx-auto max-w-7xl space-y-6">
      {/* Welcome */}
      <section className="flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
        <div>
          <p className="text-sm font-medium text-[var(--mq-primary)]">Welcome back</p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-[var(--mq-text)] sm:text-3xl">
            {getGreeting()}, {firstName}
          </h1>
          <p className="mt-2 text-sm text-[var(--mq-text-secondary)] sm:text-base">
            Ready to continue learning?
          </p>
        </div>

        <div className="flex flex-col gap-2 sm:flex-row">
          <Link
            to="/vault"
            className="inline-flex items-center justify-center gap-2 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 py-2.5 text-sm font-semibold text-[var(--mq-text)] transition hover:bg-[var(--mq-bg)]"
          >
            <BookOpen size={17} />
            Continue Learning
          </Link>

          <Link
            id="dashboard-generate"
            to="/ai-studio"
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-[var(--mq-primary)] to-[var(--mq-ai)] px-4 py-2.5 text-sm font-semibold text-white transition hover:opacity-95"
          >
            <Sparkles size={17} />
            Generate MCQs
          </Link>
        </div>
      </section>

      {error && (
        <div className="rounded-xl border border-[var(--mq-error-border)] bg-[var(--mq-error-light)] px-4 py-3 text-sm text-[var(--mq-error)]">
          {error}
        </div>
      )}

      {/* Storage usage alert */}
      <StorageUsageAlert />

      {/* Stats */}
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          icon={FileText}
          label="Study Materials"
          value={String(stats?.totalMaterials ?? 0)}
          description="In your Knowledge Vault"
          iconClass="bg-[var(--mq-primary-light)] text-[var(--mq-primary)]"
        />

        <StatCard
          icon={Brain}
          label="Quizzes Taken"
          value={String(stats?.totalQuizzes ?? 0)}
          description="Practice quizzes completed"
          iconClass="bg-[var(--mq-ai-light)] text-[var(--mq-ai)]"
        />

        <StatCard
          icon={Trophy}
          label="Average Score"
          value={
            stats && stats.totalQuizzes > 0
              ? `${stats.averageScore}%`
              : "\u2014"
          }
          description="Across your quizzes"
          iconClass="bg-[var(--mq-success-light)] text-[var(--mq-success)]"
        />

        <StatCard
          icon={Target}
          label="Questions Answered"
          value={String(stats?.totalQuestionsAnswered ?? 0)}
          description="Total practice questions"
          iconClass="bg-[var(--mq-warning-light)] text-[var(--mq-warning)]"
        />
      </section>

      {/* Main content */}
      <div className="grid gap-6 xl:grid-cols-[1.4fr_0.8fr]">
        {/* Recent materials */}
        <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5 shadow-[var(--mq-shadow-sm)] sm:p-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-semibold text-[var(--mq-text)]">
                Recent Materials
              </h2>
              <p className="mt-1 text-sm text-[var(--mq-text-secondary)]">
                Pick up where you left off.
              </p>
            </div>

            <Link
              to="/vault"
              className="inline-flex items-center gap-1 text-sm font-medium text-[var(--mq-primary)] hover:text-[var(--mq-primary-hover)]"
            >
              View all
              <ArrowRight size={15} />
            </Link>
          </div>

          <div className="mt-5 space-y-3">
            {stats && stats.recentMaterials.length === 0 && (
              <div className="rounded-xl border border-dashed border-[var(--mq-text-muted)] bg-[var(--mq-bg)] px-4 py-8 text-center">
                <p className="text-sm text-[var(--mq-text-secondary)]">No materials yet.</p>
                <Link
                  to="/vault"
                  className="mt-3 inline-flex items-center gap-2 rounded-xl bg-[var(--mq-primary)] px-4 py-2 text-sm font-semibold text-white hover:bg-[var(--mq-primary-hover)]"
                >
                  Upload your first material
                </Link>
              </div>
            )}

            {stats?.recentMaterials.map((material) => (
              <Link
                key={material.id}
                to={`/vault/${material.id}`}
                className="flex items-center gap-4 rounded-xl border border-[var(--mq-border)] p-4 transition hover:border-[var(--mq-primary-border)] hover:bg-[var(--mq-bg)]"
              >
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[var(--mq-primary-light)] text-[var(--mq-primary)]">
                  <FileText size={20} />
                </div>

                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold text-[var(--mq-text)]">
                    {material.title}
                  </p>
                  <p className="mt-1 text-xs text-[var(--mq-text-secondary)]">
                    {material.materialType === "PDF_UPLOAD" ? "PDF" : "Notes"}{" "}
                    &middot;{" "}
                    {new Date(material.updatedAt).toLocaleDateString()}
                  </p>
                </div>

                <ArrowRight size={17} className="shrink-0 text-[var(--mq-text-muted)]" />
              </Link>
            ))}
          </div>
        </section>

        {/* AI Studio card */}
        <section className="overflow-hidden rounded-2xl bg-gradient-to-br from-[var(--mq-primary)] to-[var(--mq-ai)] p-6 text-white shadow-[0_8px_30px_rgba(37,99,235,0.12)]">
          <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-white/15 dark:bg-white/20">
            <Sparkles size={21} />
          </div>

          <p className="mt-6 text-xs font-semibold uppercase tracking-wider text-white/70">
            AI Studio
          </p>

          <h2 className="mt-2 text-xl font-bold">
            Turn your notes into practice.
          </h2>

          <p className="mt-3 text-sm leading-6 text-white/80">
            Select a study material and let MindQ create personalized MCQs to
            test your understanding.
          </p>

          <Link
            to="/ai-studio"
            className="mt-6 inline-flex items-center gap-2 rounded-xl bg-[var(--mq-surface)] px-4 py-2.5 text-sm font-semibold text-[var(--mq-primary)] transition hover:bg-[var(--mq-primary-light)]"
          >
            Generate MCQs
            <ArrowRight size={16} />
          </Link>
        </section>
      </div>

      {/* Continue Learning */}
      {stats?.continueLearning && (
        <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5 shadow-[var(--mq-shadow-sm)] sm:p-6">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[var(--mq-success-light)] text-[var(--mq-success)]">
              <BookOpen size={19} />
            </div>
            <div>
              <h2 className="font-semibold text-[var(--mq-text)]">Continue Learning</h2>
              <p className="mt-0.5 text-xs text-[var(--mq-text-secondary)]">Pick up where you left off</p>
            </div>
          </div>

          <div className="mt-4 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-bg)] p-4">
            <div className="flex items-center justify-between">
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-[var(--mq-text)]">
                  {stats.continueLearning.materialTitle}
                </p>
                <p className="mt-1 text-xs text-[var(--mq-text-secondary)]">
                  {stats.continueLearning.quizTitle}
                </p>
              </div>
              <span className="shrink-0 rounded-lg bg-[var(--mq-success-light)] px-2.5 py-1 text-xs font-semibold text-[var(--mq-success)]">
                {stats.continueLearning.lastScore}%
              </span>
            </div>
            <div className="mt-3">
              <div className="h-1.5 overflow-hidden rounded-full bg-[var(--mq-border)]">
                <div
                  className="h-full rounded-full bg-[var(--mq-success)]"
                  style={{ width: `${stats.continueLearning.lastScore}%` }}
                />
              </div>
            </div>
            <Link
              to="/ai-studio"
              className="mt-3 inline-flex items-center gap-1.5 text-sm font-medium text-[var(--mq-primary)] hover:text-[var(--mq-primary-hover)]"
            >
              Practice this topic <ArrowRight size={14} />
            </Link>
          </div>
        </section>
      )}

      {/* Weak Areas */}
      {stats && stats.weakAreas.length > 0 && (
        <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5 shadow-[var(--mq-shadow-sm)] sm:p-6">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[var(--mq-error-light)] text-[var(--mq-error)]">
              <TrendingDown size={19} />
            </div>
            <div>
              <h2 className="font-semibold text-[var(--mq-text)]">Weak Areas</h2>
              <p className="mt-0.5 text-xs text-[var(--mq-text-secondary)]">Topics that need more practice</p>
            </div>
          </div>

          <div className="mt-4 space-y-3">
            {stats.weakAreas.map((area) => (
              <div key={area.quizId} className="flex items-center gap-3 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-bg)] p-3">
                <HelpCircle size={16} className="shrink-0 text-[var(--mq-error)]" />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-[var(--mq-text)]">{area.title}</p>
                </div>
                <span className="shrink-0 rounded-lg bg-[var(--mq-error-light)] px-2 py-1 text-xs font-semibold text-[var(--mq-error)]">
                  {area.score}%
                </span>
              </div>
            ))}
          </div>

          <Link
            to="/ai-studio"
            className="mt-4 inline-flex items-center gap-1.5 text-sm font-medium text-[var(--mq-primary)] hover:text-[var(--mq-primary-hover)]"
          >
            Practice weak areas <ArrowRight size={14} />
          </Link>
        </section>
      )}

      {/* Recent activity */}
      <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5 shadow-[var(--mq-shadow-sm)] sm:p-6">
        <div>
          <h2 className="font-semibold text-[var(--mq-text)]">Recent Activity</h2>
          <p className="mt-1 text-sm text-[var(--mq-text-secondary)]">
            Your latest activity on MindQ.
          </p>
        </div>

        <div className="mt-5 divide-y divide-[var(--mq-border-light)]">
          {stats && stats.recentActivity.length === 0 && (
            <div className="py-8 text-center text-sm text-[var(--mq-text-secondary)]">
              No activity yet. Start by uploading a material!
            </div>
          )}

          {stats?.recentActivity.map((activity) => {
            const Icon = activity.type === "quiz" ? CheckCircle2 : BookOpen;

            return (
              <div
                key={`${activity.type}-${activity.id}`}
                className="flex items-center gap-4 py-4 first:pt-0 last:pb-0"
              >
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[var(--mq-bg)] text-[var(--mq-primary)]">
                  <Icon size={18} />
                </div>

                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-[var(--mq-text)]">
                    {activity.title}
                  </p>
                  <p className="mt-1 text-xs text-[var(--mq-text-secondary)]">{activity.time}</p>
                </div>

                {activity.score != null && (
                  <span className="rounded-lg bg-[var(--mq-success-light)] px-2.5 py-1 text-xs font-semibold text-[var(--mq-success)]">
                    {activity.score}%
                  </span>
                )}
              </div>
            );
          })}
        </div>
      </section>
    </div>
    </>
  );
}

interface StatCardProps {
  icon: typeof FileText;
  label: string;
  value: string;
  description: string;
  iconClass: string;
}

function StatCard({
  icon: Icon,
  label,
  value,
  description,
  iconClass,
}: StatCardProps) {
  return (
    <div className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5 shadow-[var(--mq-shadow-sm)]">
      <div className="flex items-start justify-between">
        <div
          className={`flex h-10 w-10 items-center justify-center rounded-xl ${iconClass}`}
        >
          <Icon size={19} />
        </div>
      </div>
      <p className="mt-5 text-sm text-[var(--mq-text-secondary)]">{label}</p>
      <p className="mt-1 text-2xl font-bold text-[var(--mq-text)]">{value}</p>
      <p className="mt-1 text-xs text-[var(--mq-text-muted)]">{description}</p>
    </div>
  );
}
