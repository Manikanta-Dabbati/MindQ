import {
  BarChart3,
  Brain,
  Database,
  FileText,
  Loader2,
  Users,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import * as adminService from "../../services/adminService";
import type { AdminDashboardStats } from "../../types/admin";

export default function AdminDashboard() {
  const [stats, setStats] = useState<AdminDashboardStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    adminService
      .getDashboardStats()
      .then(setStats)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <Loader2 size={28} className="animate-spin text-[var(--mq-primary)]" />
      </div>
    );
  }

  if (!stats) return null;

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-[var(--mq-text)]">Admin Dashboard</h1>
        <p className="mt-1 text-sm text-[var(--mq-text-secondary)]">
          System overview and management.
        </p>
      </div>

      {/* Stats */}
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          icon={Users}
          label="Total Users"
          value={String(stats.totalUsers)}
          iconClass="bg-[var(--mq-primary-light)] text-[var(--mq-primary)]"
        />
        <StatCard
          icon={FileText}
          label="Materials"
          value={String(stats.totalMaterials)}
          iconClass="bg-[var(--mq-ai-light)] text-[var(--mq-ai)]"
        />
        <StatCard
          icon={Brain}
          label="Quizzes Generated"
          value={String(stats.totalQuizzes)}
          iconClass="bg-[var(--mq-success-light)] text-[var(--mq-success)]"
        />
        <StatCard
          icon={BarChart3}
          label="AI Generations"
          value={String(stats.totalAiGenerations)}
          iconClass="bg-[var(--mq-warning-light)] text-[var(--mq-warning)]"
        />
      </section>

      <div className="grid gap-6 xl:grid-cols-2">
        {/* Revenue */}
        <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-6">
          <h2 className="font-semibold text-[var(--mq-text)]">Revenue</h2>
          <p className="mt-3 text-3xl font-bold text-[var(--mq-text)]">
            ₹{((stats.totalRevenue / 100).toLocaleString("en-IN"))}
          </p>
          <p className="mt-1 text-sm text-[var(--mq-text-secondary)]">Total from paid subscriptions</p>
        </section>

        {/* Storage */}
        <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-6">
          <h2 className="font-semibold text-[var(--mq-text)]">Storage Used</h2>
          <p className="mt-3 text-3xl font-bold text-[var(--mq-text)]">
            {adminService.formatStorage(stats.storageUsedBytes)}
          </p>
          <p className="mt-1 text-sm text-[var(--mq-text-secondary)]">Across all users</p>
        </section>
      </div>

      {/* Quick links */}
      <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-6">
        <h2 className="font-semibold text-[var(--mq-text)]">Quick Actions</h2>
        <div className="mt-4 grid gap-3 sm:grid-cols-3">
          <Link
            to="/admin/users"
            className="flex items-center gap-3 rounded-xl border border-[var(--mq-border)] p-4 transition hover:border-[var(--mq-primary-border)] hover:bg-[var(--mq-bg)]"
          >
            <Users size={20} className="text-[var(--mq-primary)]" />
            <span className="text-sm font-medium text-[var(--mq-text)]">Manage Users</span>
          </Link>
          <Link
            to="/admin/users"
            className="flex items-center gap-3 rounded-xl border border-[var(--mq-border)] p-4 transition hover:border-[var(--mq-primary-border)] hover:bg-[var(--mq-bg)]"
          >
            <Database size={20} className="text-[var(--mq-ai)]" />
            <span className="text-sm font-medium text-[var(--mq-text)]">System Health</span>
          </Link>
          <Link
            to="/admin/users"
            className="flex items-center gap-3 rounded-xl border border-[var(--mq-border)] p-4 transition hover:border-[var(--mq-primary-border)] hover:bg-[var(--mq-bg)]"
          >
            <BarChart3 size={20} className="text-[var(--mq-success)]" />
            <span className="text-sm font-medium text-[var(--mq-text)]">AI Usage</span>
          </Link>
        </div>
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
  icon: typeof Users;
  label: string;
  value: string;
  iconClass: string;
}) {
  return (
    <div className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-5 shadow-[var(--mq-shadow-sm)]">
      <div className={`flex h-10 w-10 items-center justify-center rounded-xl ${iconClass}`}>
        <Icon size={19} />
      </div>
      <p className="mt-5 text-sm text-[var(--mq-text-secondary)]">{label}</p>
      <p className="mt-1 text-2xl font-bold text-[var(--mq-text)]">{value}</p>
    </div>
  );
}
