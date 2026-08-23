import {
  AlertTriangle,
  CheckCircle2,
  Eye,
  EyeOff,
  KeyRound,
  Laptop,
  Loader2,
  LogOut,
  Monitor,
  Moon,
  Shield,
  Smartphone,
  Sun,
  Trash2,
} from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../components/ui";
import { useTheme } from "../context/ThemeContext";

export default function Settings() {
  const { user, logoutAll, changePassword, deleteAccount } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const { theme, setTheme } = useTheme();

  // Change password state
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [passwordSuccess, setPasswordSuccess] = useState("");
  const [passwordError, setPasswordError] = useState("");

  // Logout all state
  const [logoutLoading, setLogoutLoading] = useState(false);
  const [logoutConfirm, setLogoutConfirm] = useState(false);

  // Delete account state
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState(false);
  const [deleteConfirmText, setDeleteConfirmText] = useState("");

  // Close dialogs on Escape key
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key !== "Escape") return;
      if (deleteLoading || logoutLoading) return; // don't close while action in progress
      if (deleteConfirm) {
        setDeleteConfirm(false);
        setDeleteConfirmText("");
      } else if (logoutConfirm) {
        setLogoutConfirm(false);
        setLogoutError("");
      }
    };
    document.addEventListener("keydown", handleEscape);
    return () => document.removeEventListener("keydown", handleEscape);
  }, [deleteConfirm, logoutConfirm, deleteLoading, logoutLoading]);

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordError("");
    setPasswordSuccess("");

    if (newPassword !== confirmPassword) {
      setPasswordError("New passwords do not match");
      return;
    }

    if (currentPassword === newPassword) {
      setPasswordError("New password must be different from current password");
      return;
    }

    setPasswordLoading(true);
    try {
      await changePassword(currentPassword, newPassword);
      toast.success("Password changed successfully");
      setPasswordSuccess("");
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err: unknown) {
      if (typeof err === "object" && err !== null && "response" in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        setPasswordError(
          axiosErr.response?.data?.message ?? "Failed to change password",
        );
      } else {
        setPasswordError("Failed to change password");
      }
    } finally {
      setPasswordLoading(false);
    }
  };

  const [logoutError, setLogoutError] = useState("");

  const handleLogoutAll = async () => {
    setLogoutLoading(true);
    setLogoutError("");
    try {
      await logoutAll();
      toast.success("All sessions invalidated. Please sign in again.");
      navigate("/login");
    } catch (err: unknown) {
      let msg = "Failed to invalidate sessions. Please try again.";
      if (typeof err === "object" && err !== null && "response" in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        msg = axiosErr.response?.data?.message ?? msg;
      }
      setLogoutError(msg);
      setLogoutLoading(false);
    }
  };

  const [deleteError, setDeleteError] = useState("");

  const handleDeleteAccount = async () => {
    if (deleteConfirmText !== "DELETE") return;
    setDeleteLoading(true);
    setDeleteError("");
    try {
      await deleteAccount();
      navigate("/login");
    } catch (err: unknown) {
      let msg = "Failed to delete account. Please try again.";
      if (typeof err === "object" && err !== null && "response" in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        msg = axiosErr.response?.data?.message ?? msg;
      }
      setDeleteError(msg);
      setDeleteLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-2xl space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-[var(--mq-text)]">Settings</h1>
        <p className="mt-1 text-sm text-[var(--mq-text-secondary)]">
          Manage your account security and preferences.
        </p>
      </div>

      {/* Appearance */}
      <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-6">
        <div className="mb-4 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[var(--mq-primary-light)] text-[var(--mq-primary)]">
            <Sun size={19} />
          </div>
          <div>
            <h2 className="text-lg font-semibold text-[var(--mq-text)]">
              Appearance
            </h2>
            <p className="text-xs text-[var(--mq-text-secondary)]">
              Choose your preferred theme.
            </p>
          </div>
        </div>

        <div className="rounded-xl border border-[var(--mq-border)] bg-[var(--mq-bg)] p-4">
          <p className="mb-3 text-sm font-medium text-[var(--mq-text)]">Theme</p>
          <div className="grid grid-cols-3 gap-3">
            {(
              [
                { value: "light" as const, label: "Light", icon: Sun },
                { value: "dark" as const, label: "Dark", icon: Moon },
                { value: "system" as const, label: "System", icon: Monitor },
              ] as const
            ).map(({ value, label, icon: Icon }) => (
              <button
                key={value}
                type="button"
                onClick={() => setTheme(value)}
                className={`flex flex-col items-center gap-2 rounded-xl border px-4 py-3 text-sm font-medium transition
                  ${
                    theme === value
                      ? "border-[var(--mq-primary)] bg-[var(--mq-primary-light)] text-[var(--mq-primary)]"
                      : "border-[var(--mq-border)] bg-[var(--mq-surface)] text-[var(--mq-text-secondary)] hover:border-[var(--mq-primary-border)] hover:text-[var(--mq-text)]"
                  }`}
                aria-label={`${label} theme`}
                aria-pressed={theme === value}
              >
                <Icon size={20} />
                <span>{label}</span>
              </button>
            ))}
          </div>
          <p className="mt-3 text-xs text-[var(--mq-text-muted)]">
            System mode follows your operating system preference.
          </p>
        </div>
      </section>

            {/* Account Info */}
      <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-6">
        <h2 className="mb-4 text-lg font-semibold text-[var(--mq-text)]">
          Account Information
        </h2>
        <div className="space-y-3">
          <div>
            <span className="text-sm text-[var(--mq-text-secondary)]">Name</span>
            <p className="text-sm font-medium text-[var(--mq-text)]">
              {user?.fullName}
            </p>
          </div>
          <div>
            <span className="text-sm text-[var(--mq-text-secondary)]">Email</span>
            <p className="text-sm font-medium text-[var(--mq-text)]">{user?.email}</p>
          </div>
          <div>
            <span className="text-sm text-[var(--mq-text-secondary)]">Role</span>
            <p className="text-sm font-medium text-[var(--mq-text)]">
              {user?.role?.replace("ROLE_", "") ?? "User"}
            </p>
          </div>
        </div>
      </section>

      {/* Change Password */}
      <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-6">
        <div className="mb-4 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[var(--mq-primary-light)] text-[var(--mq-primary)]">
            <KeyRound size={19} />
          </div>
          <div>
            <h2 className="text-lg font-semibold text-[var(--mq-text)]">
              Change Password
            </h2>
            <p className="text-xs text-[var(--mq-text-secondary)]">
              Password must be 8+ chars with uppercase, lowercase, digit, and
              special character.
            </p>
          </div>
        </div>

        {passwordSuccess && (
          <div className="mb-4 flex items-center gap-2 rounded-xl border border-[var(--mq-success-border)] bg-[var(--mq-success-light)] px-4 py-3 text-sm text-[var(--mq-success)]">
            <CheckCircle2 size={16} />
            {passwordSuccess}
          </div>
        )}

        {passwordError && (
          <div className="mb-4 rounded-xl border border-[var(--mq-error-border)] bg-[var(--mq-error-light)] px-4 py-3 text-sm text-[var(--mq-error)]">
            {passwordError}
          </div>
        )}

        <form onSubmit={handleChangePassword} className="space-y-4">
          {/* Current password */}
          <div>
            <label className="mb-1.5 block text-sm font-medium text-[var(--mq-text)]">
              Current Password
            </label>
            <div className="relative">
              <input
                type={showCurrent ? "text" : "password"}
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                required
                className="h-11 w-full rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 pr-11 text-sm text-[var(--mq-text)] outline-none transition focus:border-[var(--mq-primary)] focus:ring-4 focus:ring-[var(--mq-primary)]/10"
                placeholder="Enter current password"
              />
              <button
                type="button"
                onClick={() => setShowCurrent(!showCurrent)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)] hover:text-[var(--mq-text-secondary)]"
              >
                {showCurrent ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          {/* New password */}
          <div>
            <label className="mb-1.5 block text-sm font-medium text-[var(--mq-text)]">
              New Password
            </label>
            <div className="relative">
              <input
                type={showNew ? "text" : "password"}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                className="h-11 w-full rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 pr-11 text-sm text-[var(--mq-text)] outline-none transition focus:border-[var(--mq-primary)] focus:ring-4 focus:ring-[var(--mq-primary)]/10"
                placeholder="Enter new password"
              />
              <button
                type="button"
                onClick={() => setShowNew(!showNew)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)] hover:text-[var(--mq-text-secondary)]"
              >
                {showNew ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          {/* Confirm password */}
          <div>
            <label className="mb-1.5 block text-sm font-medium text-[var(--mq-text)]">
              Confirm New Password
            </label>
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
              className="h-11 w-full rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 text-sm text-[var(--mq-text)] outline-none transition focus:border-[var(--mq-primary)] focus:ring-4 focus:ring-[var(--mq-primary)]/10"
              placeholder="Confirm new password"
            />
          </div>

          <button
            type="submit"
            disabled={passwordLoading}
            className="flex h-11 items-center justify-center gap-2 rounded-xl bg-[var(--mq-primary)] px-6 text-sm font-semibold text-white transition hover:bg-[var(--mq-primary-hover)] disabled:cursor-not-allowed disabled:opacity-60"
          >
            {passwordLoading ? "Changing..." : "Change Password"}
          </button>
        </form>
      </section>

      {/* Security — Logout All Devices */}
      <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-6">
        <div className="mb-4 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[var(--mq-error-light)] text-[var(--mq-error)]">
            <Shield size={19} />
          </div>
          <div>
            <h2 className="text-lg font-semibold text-[var(--mq-text)]">
              Security
            </h2>
            <p className="text-xs text-[var(--mq-text-secondary)]">
              Manage active sessions and device access.
            </p>
          </div>
        </div>

        <div className="rounded-xl border border-[var(--mq-border)] bg-[var(--mq-bg)] p-4">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="text-sm font-medium text-[var(--mq-text)]">
                Logout from all devices
              </p>
              <p className="mt-1 text-xs text-[var(--mq-text-secondary)]">
                Invalidate all active sessions and refresh tokens across every
                device. You'll need to sign in again everywhere.
              </p>
            </div>
            <button
              onClick={() => setLogoutConfirm(true)}
              className="flex shrink-0 items-center gap-2 rounded-xl border border-[var(--mq-error-border)] bg-[var(--mq-surface)] px-4 py-2 text-sm font-medium text-[var(--mq-error)] transition hover:bg-[var(--mq-error-light)]"
            >
              <LogOut size={15} />
              Logout All
            </button>
          </div>
        </div>
      </section>

      {/* Logout All Confirmation Dialog */}
      {logoutConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="w-full max-w-md rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-6 shadow-2xl">
            <div className="flex items-center gap-3 mb-4">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[var(--mq-error-light)]">
                <AlertTriangle size={20} className="text-[var(--mq-error)]" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-[var(--mq-text)]">Logout from All Devices</h3>
                <p className="text-xs text-[var(--mq-text-secondary)]">This will sign you out everywhere.</p>
              </div>
            </div>

            <p className="mb-4 text-sm text-[var(--mq-text-secondary)]">
              This action will immediately invalidate all active sessions and refresh
              tokens for your account. Any other device currently signed in will be
              logged out and will need to sign in again.
            </p>

            {/* Device impact illustration */}
            <div className="mb-4 flex items-center gap-3 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-bg)] p-4">
              <div className="flex items-center gap-2">
                <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-[var(--mq-primary-light)]">
                  <Smartphone size={14} className="text-[var(--mq-primary)]" />
                </div>
                <div>
                  <p className="text-xs font-medium text-[var(--mq-text)]">Mobile</p>
                  <p className="text-[10px] text-[var(--mq-text-muted)]">Signed out</p>
                </div>
              </div>
              <div className="text-[var(--mq-text-muted)]">→</div>
              <div className="flex items-center gap-2">
                <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-[var(--mq-primary-light)]">
                  <Laptop size={14} className="text-[var(--mq-primary)]" />
                </div>
                <div>
                  <p className="text-xs font-medium text-[var(--mq-text)]">Desktop</p>
                  <p className="text-[10px] text-[var(--mq-text-muted)]">Signed out</p>
                </div>
              </div>
              <div className="text-[var(--mq-text-muted)]">→</div>
              <div className="flex items-center gap-2">
                <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-[var(--mq-primary-light)]">
                  <Monitor size={14} className="text-[var(--mq-primary)]" />
                </div>
                <div>
                  <p className="text-xs font-medium text-[var(--mq-text)]">Tablet</p>
                  <p className="text-[10px] text-[var(--mq-text-muted)]">Signed out</p>
                </div>
              </div>
            </div>

            <div className="rounded-xl border border-[var(--mq-border)] bg-[var(--mq-bg)] p-3 mb-4">
              <p className="text-xs text-[var(--mq-text-secondary)]">
                <strong className="text-[var(--mq-text)]">What happens next:</strong>
              </p>
              <ul className="mt-2 space-y-1 text-xs text-[var(--mq-text-secondary)]">
                <li>• All active sessions are immediately invalidated</li>
                <li>• Every device will be signed out</li>
                <li>• You'll need to sign in again on each device</li>
                <li>• This action is instant and cannot be undone</li>
              </ul>
            </div>

            {logoutError && (
              <div className="mb-4 rounded-lg border border-[var(--mq-error-border)] bg-[var(--mq-error-light)] px-4 py-3 text-sm text-[var(--mq-error)]">
                {logoutError}
              </div>
            )}

            <div className="flex justify-end gap-3">
              <button
                onClick={() => {
                  setLogoutConfirm(false);
                  setLogoutError("");
                }}
                disabled={logoutLoading}
                className="rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 py-2 text-sm font-medium text-[var(--mq-text)] transition hover:bg-[var(--mq-bg)] disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                onClick={handleLogoutAll}
                disabled={logoutLoading}
                className="inline-flex items-center gap-2 rounded-xl bg-[var(--mq-error)] px-4 py-2 text-sm font-medium text-white transition hover:bg-[var(--mq-error)]/90 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {logoutLoading ? (
                  <>
                    <Loader2 size={14} className="animate-spin" />
                    Logging out...
                  </>
                ) : (
                  <>
                    <LogOut size={14} />
                    Logout from All Devices
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Danger Zone — Delete Account */}
      <section className="rounded-2xl border border-[var(--mq-error-border)] bg-[var(--mq-surface)] p-6">
        <div className="mb-4 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[var(--mq-error-light)] text-[var(--mq-error)]">
            <Trash2 size={19} />
          </div>
          <div>
            <h2 className="text-lg font-semibold text-[var(--mq-text)]">
              Danger Zone
            </h2>
            <p className="text-xs text-[var(--mq-text-secondary)]">
              Permanent and irreversible actions.
            </p>
          </div>
        </div>

        <div className="rounded-xl border border-[var(--mq-error-border)] bg-[var(--mq-error-light)]/50 p-4">
          {deleteError && (
            <div className="mb-3 rounded-lg border border-[var(--mq-error-border)] bg-[var(--mq-error-light)] px-4 py-3 text-sm text-[var(--mq-error)]">
              {deleteError}
            </div>
          )}
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="text-sm font-medium text-[var(--mq-text)]">
                Delete account
              </p>
              <p className="mt-1 text-xs text-[var(--mq-text-secondary)]">
                Permanently delete your account, all materials, quizzes, and
                data. This cannot be undone.
              </p>
            </div>

            {!deleteConfirm ? (
              <button
                onClick={() => setDeleteConfirm(true)}
                className="flex shrink-0 items-center gap-2 rounded-xl border border-[var(--mq-error-border)] bg-[var(--mq-surface)] px-4 py-2 text-sm font-medium text-[var(--mq-error)] transition hover:bg-[var(--mq-error-light)]"
              >
                <Trash2 size={15} />
                Delete Account
              </button>
            ) : (
              <div className="flex shrink-0 flex-col items-end gap-2">
                <div className="flex items-center gap-1.5 rounded-lg bg-[var(--mq-error-light)] px-3 py-1.5 text-xs text-[var(--mq-error)]">
                  <AlertTriangle size={14} />
                  Type DELETE to confirm
                </div>
                <input
                  type="text"
                  value={deleteConfirmText}
                  onChange={(e) => setDeleteConfirmText(e.target.value)}
                  placeholder="Type DELETE"
                  className="h-8 w-full rounded-lg border border-[var(--mq-error-border)] bg-[var(--mq-surface)] px-3 text-xs outline-none focus:border-red-500"
                />
                <div className="flex gap-2">
                  <button
                    onClick={handleDeleteAccount}
                    disabled={deleteLoading || deleteConfirmText !== "DELETE"}
                    className="rounded-lg bg-[var(--mq-error)] px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-[var(--mq-error)] disabled:opacity-40"
                  >
                    {deleteLoading ? "Deleting..." : "Yes, delete"}
                  </button>
                  <button
                    onClick={() => {
                      setDeleteConfirm(false);
                      setDeleteConfirmText("");
                    }}
                    className="rounded-lg border border-[var(--mq-border)] bg-[var(--mq-surface)] px-3 py-1.5 text-xs font-medium text-[var(--mq-text-secondary)] transition hover:bg-[var(--mq-bg)]"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}
