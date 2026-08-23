import {
  ArrowLeft,
  BookOpen,
  Brain,
  Clock3,
  FileText,
  Loader2,
  Trash2,
  Sparkles,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import type { MaterialDetail } from "../types/material";
import { useToast } from "../components/ui";
import * as materialService from "../services/materialService";

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const days = Math.floor(diff / 86400000);
  if (days === 0) return "Today";
  if (days === 1) return "Yesterday";
  return `${days} days ago`;
}

function deriveCategory(title: string): string {
  const lower = title.toLowerCase();
  if (lower.includes("java") && !lower.includes("javascript")) return "Java";
  if (lower.includes("spring")) return "Spring Boot";
  if (lower.includes("react")) return "React";
  if (lower.includes("mysql") || lower.includes("database") || lower.includes("sql")) return "Database";
  if (lower.includes("javascript") || lower.includes("js")) return "JavaScript";
  return "General";
}

export default function MaterialDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const [material, setMaterial] = useState<MaterialDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    materialService
      .getMaterial(Number(id))
      .then(setMaterial)
      .catch(() => setError("Material not found."))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <Loader2 size={24} className="animate-spin text-[var(--mq-primary)]" />
      </div>
    );
  }

  if (error || !material) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <Link to="/vault" className="inline-flex items-center gap-2 text-sm font-medium text-[var(--mq-text-secondary)] transition hover:text-[var(--mq-primary)]">
          <ArrowLeft size={17} /> Back to Knowledge Vault
        </Link>
        <div className="rounded-2xl border border-dashed border-[var(--mq-error-border)] bg-[var(--mq-surface)] px-6 py-16 text-center">
          <p className="text-sm text-[var(--mq-error)]">{error || "Material not found."}</p>
        </div>
      </div>
    );
  }

  const typeLabel = material.materialType === "PDF_UPLOAD" ? "PDF" : "NOTE";
  const category = deriveCategory(material.title);
  const updated = formatDate(material.updatedAt);
  const pages = Math.max(1, Math.round(material.wordCount / 250));

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      {/* Back */}
      <Link
        to="/vault"
        className="inline-flex items-center gap-2 text-sm font-medium text-[var(--mq-text-secondary)] transition hover:text-[var(--mq-primary)]"
      >
        <ArrowLeft size={17} />
        Back to Knowledge Vault
      </Link>

      {/* Main material header */}
      <section className="overflow-hidden rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] shadow-[var(--mq-shadow-sm)]">
        <div className="border-b border-[var(--mq-border)] p-6 sm:p-8">
          <div className="flex flex-col justify-between gap-6 sm:flex-row sm:items-start">
            <div className="flex gap-4">
              <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-[var(--mq-primary-light)] text-[var(--mq-primary)]">
                <FileText size={26} />
              </div>

              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <span className="rounded-lg bg-[var(--mq-primary-light)] px-2.5 py-1 text-xs font-semibold text-[var(--mq-primary)]">
                    {typeLabel}
                  </span>

                  <span className="rounded-lg bg-[var(--mq-bg)] px-2.5 py-1 text-xs font-medium text-[var(--mq-text-secondary)]">
                    {category}
                  </span>
                </div>

                <h1 className="mt-3 text-2xl font-bold tracking-tight text-[var(--mq-text)] sm:text-3xl">
                  {material.title}
                </h1>

                <p className="mt-2 text-sm text-[var(--mq-text-secondary)]">
                  Added to your Knowledge Vault
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={() => setShowDeleteConfirm(true)}
              className="self-start rounded-xl border border-[var(--mq-error-border)] p-2.5 text-[var(--mq-error)] transition hover:bg-[var(--mq-error-light)] hover:text-[var(--mq-error)]"
              aria-label="Delete material"
            >
              <Trash2 size={19} />
            </button>
          </div>



          {/* Metadata */}
          <div className="mt-6 flex flex-wrap gap-5 border-t border-[var(--mq-border)] pt-5">
            <div className="flex items-center gap-2 text-sm text-[var(--mq-text-secondary)]">
              <FileText size={16} />
              {pages} pages · {material.wordCount.toLocaleString()} words
            </div>

            {material.fileName && (
              <div className="flex items-center gap-2 text-sm text-[var(--mq-text-secondary)]">
                <FileText size={16} />
                {material.fileName} {material.fileSizeBytes ? `· ${formatFileSize(material.fileSizeBytes)}` : ""}
              </div>
            )}

            <div className="flex items-center gap-2 text-sm text-[var(--mq-text-secondary)]">
              <Clock3 size={16} />
              Updated {updated.toLowerCase()}
            </div>
          </div>
        </div>

        {/* Actions */}
        <div className="flex flex-col gap-3 bg-[var(--mq-bg)] p-5 sm:flex-row sm:items-center sm:justify-between sm:px-8">
          <div>
            <p className="text-sm font-semibold text-[var(--mq-text)]">
              Ready to practice?
            </p>

            <p className="mt-1 text-xs text-[var(--mq-text-secondary)]">
              Generate questions from this material using AI.
            </p>
          </div>

          <Link
            to={`/ai-studio?materialId=${material.id}`}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-[var(--mq-primary)] to-[var(--mq-ai)] px-5 py-3 text-sm font-semibold text-white transition hover:opacity-95"
          >
            <Sparkles size={17} />
            Generate MCQs
          </Link>
        </div>
      </section>

      {/* Content overview */}
      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        {/* Preview */}
        <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-6 shadow-[var(--mq-shadow-sm)]">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[var(--mq-bg)] text-[var(--mq-primary)]">
              <BookOpen size={19} />
            </div>

            <div>
              <h2 className="font-semibold text-[var(--mq-text)]">
                Material Overview
              </h2>

              <p className="mt-1 text-xs text-[var(--mq-text-secondary)]">
                Preview of your learning resource
              </p>
            </div>
          </div>

          <div className="mt-6 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-bg)] p-5">
            <div className="flex items-center gap-3">
              <FileText
                size={19}
                className="text-[var(--mq-primary)]"
              />

              <span className="text-sm font-medium text-[var(--mq-text)]">
                {material.title}
              </span>

              {material.content && (
                <p className="mt-4 text-xs leading-5 text-[var(--mq-text-secondary)] line-clamp-6 whitespace-pre-wrap">
                  {material.content}
                </p>
              )}
            </div>

            <div className="mt-5 max-h-64 overflow-y-auto">
              <p className="whitespace-pre-wrap text-sm leading-6 text-[var(--mq-text-secondary)]">
                {material.content}
              </p>
            </div>
          </div>
        </section>

        {/* AI Actions */}
        <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-6 shadow-[var(--mq-shadow-sm)]">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[var(--mq-ai-light)] text-[var(--mq-ai)]">
              <Brain size={19} />
            </div>

            <div>
              <h2 className="font-semibold text-[var(--mq-text)]">
                AI Studio
              </h2>

              <p className="mt-1 text-xs text-[var(--mq-text-secondary)]">
                Learn from this material
              </p>
            </div>
          </div>

          <div className="mt-5 space-y-3">
            <Link
              to={`/ai-studio?materialId=${material.id}`}
              className="flex items-center gap-3 rounded-xl border border-[var(--mq-border)] p-3 transition hover:border-[var(--mq-primary-border)] hover:bg-[var(--mq-bg)]"
            >
              <Sparkles
                size={17}
                className="text-[var(--mq-ai)]"
              />

              <div>
                <p className="text-sm font-medium text-[var(--mq-text)]">
                  Generate MCQs
                </p>

                <p className="mt-0.5 text-xs text-[var(--mq-text-secondary)]">
                  Create a practice quiz
                </p>
              </div>
            </Link>
          </div>
        </section>
      </div>

      {/* Delete Confirmation Dialog */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/30 px-4 backdrop-blur-[2px]">
          <div className="w-full max-w-md overflow-hidden rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] shadow-[0_20px_60px_rgba(0, 0, 0, 0.15)]">
            <div className="p-6 text-center">
              <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-[var(--mq-error-light)] text-[var(--mq-error)]">
                <Trash2 size={28} />
              </div>
              <h3 className="mt-4 text-lg font-bold text-[var(--mq-text)]">Delete Material?</h3>
              <p className="mt-2 text-sm text-[var(--mq-text-secondary)]">
                Are you sure you want to delete <span className="font-semibold text-[var(--mq-text)]">{material.title}</span>? This action cannot be undone.
              </p>
            </div>
            <div className="flex gap-3 border-t border-[var(--mq-border)] px-6 py-4">
              <button
                type="button"
                onClick={() => setShowDeleteConfirm(false)}
                disabled={deleting}
                className="flex-1 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 py-2.5 text-sm font-semibold text-[var(--mq-text)] transition hover:bg-[var(--mq-bg)] disabled:opacity-60"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={async () => {
                  setDeleting(true);
                  try {
                    await materialService.deleteMaterial(material.id);
                    toast.success(`"${material.title}" deleted`);
                    navigate("/vault");
                  } catch {
                    toast.error("Failed to delete material.");
                  } finally {
                    setDeleting(false);
                    setShowDeleteConfirm(false);
                  }
                }}
                disabled={deleting}
                className="flex-1 inline-flex items-center justify-center gap-2 rounded-xl bg-[var(--mq-error)] px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-[var(--mq-error)] disabled:opacity-60"
              >
                {deleting ? "Deleting..." : "Delete"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}