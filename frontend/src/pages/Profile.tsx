import {
  Calendar,
  CheckCircle2,
  Crown,
  Mail,
  Pencil,
  User,
} from "lucide-react";
import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../components/ui";
import * as authService from "../services/authService";
import * as subscriptionService from "../services/subscriptionService";
import type { Plan } from "../types/subscription";

export default function Profile() {
  const { user } = useAuth();
  const [editing, setEditing] = useState(false);
  const [fullName, setFullName] = useState(user?.fullName ?? "");
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState("");
  const [error, setError] = useState("");
  const [plan, setPlan] = useState<Plan | null>(null);
  const toast = useToast();

  useEffect(() => {
    subscriptionService.getCurrentSubscription()
      .then((sub) => setPlan(sub.plan))
      .catch(() => {});
  }, []);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSuccess("");
    setError("");

    if (fullName.trim().length < 2) {
      setError("Name must be at least 2 characters");
      return;
    }

    setLoading(true);
    try {
      await authService.updateProfile(fullName.trim());
      toast.success("Profile updated successfully");
      setSuccess("");
      setEditing(false);
    } catch (err: unknown) {
      if (typeof err === "object" && err !== null && "response" in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        setError(axiosErr.response?.data?.message ?? "Failed to update profile");
      } else {
        setError("Failed to update profile");
      }
    } finally {
      setLoading(false);
    }
  };

  const initials = user?.fullName
    ?.split(" ")
    .map((n) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2) ?? "U";

  const createdDate = user?.createdAt
    ? new Date(user.createdAt).toLocaleDateString("en-US", {
        year: "numeric",
        month: "long",
        day: "numeric",
      })
    : "Unknown";

  return (
    <div className="mx-auto max-w-2xl space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-[var(--mq-text)]">Profile</h1>
        <p className="mt-1 text-sm text-[var(--mq-text-secondary)]">
          View and update your account information.
        </p>
      </div>

      {/* Profile Card */}
      <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-6">
        <div className="flex items-center gap-5">
          <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-[var(--mq-primary)] to-[var(--mq-ai)] text-2xl font-bold text-white">
            {initials}
          </div>
          <div className="min-w-0">
            <h2 className="text-xl font-bold text-[var(--mq-text)]">
              {user?.fullName}
            </h2>
            <p className="mt-0.5 text-sm text-[var(--mq-text-secondary)]">{user?.email}</p>
            <div className="mt-2 flex items-center gap-4 text-xs text-[var(--mq-text-muted)]">
              <span className="flex items-center gap-1">
                <User size={12} />
                {user?.role?.replace("ROLE_", "") ?? "User"}
              </span>
              <span className="flex items-center gap-1">
                <Calendar size={12} />
                Joined {createdDate}
              </span>
              {plan && (
                <span className="flex items-center gap-1 rounded-full bg-[var(--mq-ai-light)] px-2 py-0.5 text-[11px] font-semibold text-[var(--mq-ai)]">
                  <Crown size={11} />
                  {plan.displayName}
                </span>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* Edit Name */}
      <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-6">
        <div className="mb-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[var(--mq-primary-light)] text-[var(--mq-primary)]">
              <User size={19} />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-[var(--mq-text)]">
                Personal Information
              </h2>
              <p className="text-xs text-[var(--mq-text-secondary)]">
                Update your display name.
              </p>
            </div>
          </div>
          {!editing && (
            <button
              onClick={() => {
                setEditing(true);
                setFullName(user?.fullName ?? "");
                setSuccess("");
                setError("");
              }}
              className="flex items-center gap-1.5 rounded-lg border border-[var(--mq-border)] bg-[var(--mq-surface)] px-3 py-1.5 text-xs font-medium text-[var(--mq-text-secondary)] transition hover:bg-[var(--mq-bg)] hover:text-[var(--mq-text)]"
            >
              <Pencil size={13} />
              Edit
            </button>
          )}
        </div>

        {success && (
          <div className="mb-4 flex items-center gap-2 rounded-xl border border-[var(--mq-success-border)] bg-[var(--mq-success-light)] px-4 py-3 text-sm text-[var(--mq-success)]">
            <CheckCircle2 size={16} />
            {success}
          </div>
        )}

        {error && (
          <div className="mb-4 rounded-xl border border-[var(--mq-error-border)] bg-[var(--mq-error-light)] px-4 py-3 text-sm text-[var(--mq-error)]">
            {error}
          </div>
        )}

        {editing ? (
          <form onSubmit={handleSave} className="space-y-4">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-[var(--mq-text)]">
                Full Name
              </label>
              <input
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
                className="h-11 w-full rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 text-sm text-[var(--mq-text)] outline-none transition focus:border-[var(--mq-primary)] focus:ring-4 focus:ring-[var(--mq-primary)]/10"
                placeholder="Enter your full name"
              />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-[var(--mq-text)]">
                Email
              </label>
              <div className="flex h-11 items-center rounded-xl border border-[var(--mq-border)] bg-[var(--mq-bg)] px-4 text-sm text-[var(--mq-text-muted)]">
                <Mail size={15} className="mr-2" />
                {user?.email}
              </div>
              <p className="mt-1 text-xs text-[var(--mq-text-muted)]">
                Email cannot be changed.
              </p>
            </div>
            <div className="flex gap-3">
              <button
                type="submit"
                disabled={loading}
                className="flex h-10 items-center gap-2 rounded-xl bg-[var(--mq-primary)] px-5 text-sm font-semibold text-white transition hover:bg-[var(--mq-primary-hover)] disabled:opacity-60"
              >
                {loading ? "Saving..." : "Save Changes"}
              </button>
              <button
                type="button"
                onClick={() => setEditing(false)}
                className="flex h-10 items-center rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-5 text-sm font-medium text-[var(--mq-text-secondary)] transition hover:bg-[var(--mq-bg)]"
              >
                Cancel
              </button>
            </div>
          </form>
        ) : (
          <div className="space-y-3">
            <div>
              <span className="text-xs text-[var(--mq-text-secondary)]">Full Name</span>
              <p className="text-sm font-medium text-[var(--mq-text)]">
                {user?.fullName}
              </p>
            </div>
            <div>
              <span className="text-xs text-[var(--mq-text-secondary)]">Email</span>
              <p className="text-sm font-medium text-[var(--mq-text)]">
                {user?.email}
              </p>
            </div>
            <div>
              <span className="text-xs text-[var(--mq-text-secondary)]">Role</span>
              <p className="text-sm font-medium text-[var(--mq-text)]">
                {user?.role?.replace("ROLE_", "") ?? "User"}
              </p>
            </div>
            <div>
              <span className="text-xs text-[var(--mq-text-secondary)]">Member Since</span>
              <p className="text-sm font-medium text-[var(--mq-text)]">
                {createdDate}
              </p>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}
