import { AlertTriangle, CheckCircle2, Loader2, Shield, Trash2, XCircle } from "lucide-react";
import { useEffect, useState } from "react";
import * as adminService from "../../services/adminService";
import type { AdminUser } from "../../types/admin";

export default function AdminUsers() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [deleteTarget, setDeleteTarget] = useState<AdminUser | null>(null);
  const [deleteConfirmText, setDeleteConfirmText] = useState("");
  const [deleting, setDeleting] = useState(false);
  const [toast, setToast] = useState<{ type: "success" | "error"; message: string } | null>(null);

  useEffect(() => {
    adminService
      .getAllUsers()
      .then(setUsers)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (toast) {
      const t = setTimeout(() => setToast(null), 4000);
      return () => clearTimeout(t);
    }
  }, [toast]);

  const filtered = users.filter(
    (u) =>
      u.fullName.toLowerCase().includes(search.toLowerCase()) ||
      u.email.toLowerCase().includes(search.toLowerCase())
  );

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await adminService.deleteUser(deleteTarget.id);
      setUsers((prev) => prev.filter((u) => u.id !== deleteTarget.id));
      setToast({ type: "success", message: `User ${deleteTarget.email} deleted successfully.` });
      setDeleteTarget(null);
      setDeleteConfirmText("");
    } catch (err: any) {
      const msg = err?.response?.data?.message || "Failed to delete user. Please try again.";
      setToast({ type: "error", message: msg });
    } finally {
      setDeleting(false);
    }
  };

  const openDeleteDialog = (user: AdminUser) => {
    setDeleteTarget(user);
    setDeleteConfirmText("");
  };

  const closeDeleteDialog = () => {
    setDeleteTarget(null);
    setDeleteConfirmText("");
    setDeleting(false);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <Loader2 size={28} className="animate-spin text-[var(--mq-primary)]" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      {/* Toast */}
      {toast && (
        <div
          className={`fixed right-4 top-4 z-50 flex items-center gap-2 rounded-xl border px-4 py-3 text-sm font-medium shadow-lg transition ${
            toast.type === "success"
              ? "border-[var(--mq-success)]/20 bg-[var(--mq-success-light)] text-[var(--mq-success)]"
              : "border-[var(--mq-error)]/20 bg-[var(--mq-error-light)] text-[var(--mq-error)]"
          }`}
        >
          {toast.type === "success" ? <CheckCircle2 size={16} /> : <XCircle size={16} />}
          {toast.message}
        </div>
      )}

      <div>
        <h1 className="text-2xl font-bold text-[var(--mq-text)]">User Management</h1>
        <p className="mt-1 text-sm text-[var(--mq-text-secondary)]">
          View and manage all registered users.
        </p>
      </div>

      {/* Search */}
      <div className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-4">
        <input
          type="text"
          placeholder="Search by name or email..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="h-10 w-full rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 text-sm text-[var(--mq-text)] outline-none transition focus:border-[var(--mq-primary)] focus:ring-4 focus:ring-[var(--mq-primary)]/10"
        />
      </div>

      {/* Users table */}
      <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] overflow-hidden">
        <div className="overflow-x-auto">
          <div className="flex items-center gap-2 border-b border-[var(--mq-border)] bg-[var(--mq-bg)] px-4 py-2 sm:hidden">
            <span className="text-xs text-[var(--mq-text-secondary)]">← Scroll horizontally to see all columns →</span>
          </div>
          <table className="w-full min-w-[900px] text-sm">
            <thead>
              <tr className="border-b border-[var(--mq-border)] bg-[var(--mq-bg)]">
                <th className="px-4 py-3 text-left font-semibold text-[var(--mq-text-secondary)]">User</th>
                <th className="px-4 py-3 text-left font-semibold text-[var(--mq-text-secondary)]">Role</th>
                <th className="px-4 py-3 text-left font-semibold text-[var(--mq-text-secondary)]">Status</th>
                <th className="px-4 py-3 text-left font-semibold text-[var(--mq-text-secondary)]">Plan</th>
                <th className="px-4 py-3 text-left font-semibold text-[var(--mq-text-secondary)]">Materials</th>
                <th className="px-4 py-3 text-left font-semibold text-[var(--mq-text-secondary)]">Quizzes</th>
                <th className="px-4 py-3 text-left font-semibold text-[var(--mq-text-secondary)]">Joined</th>
                <th className="px-4 py-3 text-left font-semibold text-[var(--mq-text-secondary)]">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--mq-border-light)]">
              {filtered.map((user) => (
                <tr key={user.id} className="hover:bg-[var(--mq-bg)] transition">
                  <td className="px-4 py-3">
                    <div>
                      <p className="font-medium text-[var(--mq-text)]">{user.fullName}</p>
                      <p className="text-xs text-[var(--mq-text-muted)]">{user.email}</p>
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                      user.role === "ROLE_ADMIN"
                        ? "bg-[var(--mq-error-light)] text-[var(--mq-error)]"
                        : "bg-[var(--mq-bg)] text-[var(--mq-text-secondary)]"
                    }`}>
                      {user.role === "ROLE_ADMIN" && <Shield size={11} />}
                      {user.role.replace("ROLE_", "")}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                      user.status === "ACTIVE"
                        ? "bg-[var(--mq-success-light)] text-[var(--mq-success)]"
                        : "bg-[var(--mq-error-light)] text-[var(--mq-error)]"
                    }`}>
                      {user.status === "ACTIVE" ? <CheckCircle2 size={11} /> : <XCircle size={11} />}
                      {user.status}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                      user.planCode === "PREMIUM"
                        ? "bg-[var(--mq-ai-light)] text-[var(--mq-ai)]"
                        : user.planCode === "PRO"
                          ? "bg-[var(--mq-primary-light)] text-[var(--mq-primary)]"
                          : "bg-[var(--mq-bg)] text-[var(--mq-text-secondary)]"
                    }`}>
                      {user.planCode}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-[var(--mq-text)]">{user.materialCount}</td>
                  <td className="px-4 py-3 text-[var(--mq-text)]">{user.quizCount}</td>
                  <td className="px-4 py-3 text-[var(--mq-text-muted)]">
                    {new Date(user.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <select
                        value={user.status}
                        onChange={async (e) => {
                          try {
                            await adminService.updateUserStatus(user.id, e.target.value);
                            setUsers((prev) =>
                              prev.map((u) =>
                                u.id === user.id ? { ...u, status: e.target.value } : u
                              )
                            );
                            setToast({ type: "success", message: "User status updated." });
                          } catch {
                            setToast({ type: "error", message: "Failed to update status." });
                          }
                        }}
                        className="rounded-lg border border-[var(--mq-border)] bg-[var(--mq-surface)] px-2 py-1 text-xs text-[var(--mq-text)]"
                      >
                        <option value="ACTIVE">Active</option>
                        <option value="INACTIVE">Inactive</option>
                        <option value="BANNED">Banned</option>
                      </select>
                      <button
                        onClick={() => openDeleteDialog(user)}
                        title="Delete user"
                        className="inline-flex items-center justify-center rounded-lg border border-[var(--mq-error)]/20 bg-[var(--mq-error-light)] p-1.5 text-[var(--mq-error)] transition hover:bg-[var(--mq-error)] hover:text-white"
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={8} className="px-4 py-12 text-center text-sm text-[var(--mq-text-secondary)]">
                    No users found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>

      {/* Delete Confirmation Dialog */}
      {deleteTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="w-full max-w-md rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-6 shadow-2xl">
            <div className="flex items-center gap-3 mb-4">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[var(--mq-error-light)]">
                <AlertTriangle size={20} className="text-[var(--mq-error)]" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-[var(--mq-text)]">Delete User</h3>
                <p className="text-xs text-[var(--mq-text-secondary)]">This action cannot be undone.</p>
              </div>
            </div>

            <div className="mb-4 rounded-xl border border-[var(--mq-error)]/20 bg-[var(--mq-error-light)] p-4">
              <p className="text-sm text-[var(--mq-text)]">
                This will permanently delete <strong>{deleteTarget.email}</strong> and all associated data:
              </p>
              <ul className="mt-2 space-y-1 text-xs text-[var(--mq-text-secondary)]">
                <li>• Study materials and uploaded files</li>
                <li>• MCQ sets, questions, and quiz history</li>
                <li>• Saved questions and generation history</li>
                <li>• Subscription and payment records</li>
                <li>• All authentication sessions</li>
              </ul>
            </div>

            <div className="mb-4">
              <label className="mb-1 block text-xs font-medium text-[var(--mq-text-secondary)]">
                Type <span className="font-bold text-[var(--mq-error)]">DELETE</span> to confirm:
              </label>
              <input
                type="text"
                value={deleteConfirmText}
                onChange={(e) => setDeleteConfirmText(e.target.value)}
                placeholder="DELETE"
                className="h-10 w-full rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 text-sm text-[var(--mq-text)] outline-none transition focus:border-[var(--mq-error)] focus:ring-4 focus:ring-[var(--mq-error)]/10"
              />
            </div>

            <div className="flex justify-end gap-3">
              <button
                onClick={closeDeleteDialog}
                disabled={deleting}
                className="rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 py-2 text-sm font-medium text-[var(--mq-text)] transition hover:bg-[var(--mq-bg)] disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                onClick={handleDelete}
                disabled={deleteConfirmText !== "DELETE" || deleting}
                className="inline-flex items-center gap-2 rounded-xl bg-[var(--mq-error)] px-4 py-2 text-sm font-medium text-white transition hover:bg-[var(--mq-error)]/90 disabled:cursor-not-allowed disabled:opacity-40"
              >
                {deleting ? (
                  <>
                    <Loader2 size={14} className="animate-spin" />
                    Deleting...
                  </>
                ) : (
                  <>
                    <Trash2 size={14} />
                    Delete User
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
