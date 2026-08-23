import {
  BookOpen,
  Clock,
  Grid2X2,
  List,
  Search,
  SortAsc,
  Type,
  Upload,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import MaterialCard, {
  Material,
} from "../components/vault/MaterialCard";
import UploadMaterialModal from "../components/vault/UploadMaterialModal";
import StorageBar from "../components/vault/StorageBar";
import type { MaterialSummary } from "../types/material";
import { SkeletonVault } from "../components/ui/Skeleton";
import { useToast, FocusTrap } from "../components/ui";
import * as materialService from "../services/materialService";

function deriveCategory(title: string): string {
  const lower = title.toLowerCase();
  if (lower.includes("java") && !lower.includes("javascript")) return "Java";
  if (lower.includes("spring")) return "Spring Boot";
  if (lower.includes("react")) return "React";
  if (lower.includes("mysql") || lower.includes("database") || lower.includes("sql")) return "Database";
  if (lower.includes("javascript") || lower.includes("js")) return "JavaScript";
  if (lower.includes("rest") || lower.includes("api")) return "Backend";
  return "General";
}

function mapToMaterial(m: MaterialSummary): Material {
  const typeLabel = m.materialType === "PDF_UPLOAD" ? "PDF" : "NOTE";
  const diff = Date.now() - new Date(m.updatedAt).getTime();
  const days = Math.floor(diff / 86400000);
  const updated = days === 0 ? "Today" : days === 1 ? "Yesterday" : `${days} days ago`;
  return {
    id: m.id,
    title: m.title,
    type: typeLabel,
    category: deriveCategory(m.title),
    pages: Math.max(1, Math.round(m.wordCount / 250)),
    updated,
  };
}

const filters = ["All", "PDF", "Documents", "Notes"];

const sortOptions = [
  { label: "Newest first", value: "newest" as const, icon: Clock },
  { label: "Oldest first", value: "oldest" as const, icon: Clock },
  { label: "Name A-Z", value: "name-asc" as const, icon: Type },
  { label: "Name Z-A", value: "name-desc" as const, icon: Type },
];

type SortOption = "newest" | "oldest" | "name-asc" | "name-desc";

export default function KnowledgeVault() {
  const [activeFilter, setActiveFilter] = useState("All");
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [sortBy, setSortBy] = useState<SortOption>("newest");
  const [showSort, setShowSort] = useState(false);
  const [view, setView] = useState<"grid" | "list">("grid");
  const [uploadOpen, setUploadOpen] = useState(false);
  const [materials, setMaterials] = useState<Material[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [_totalCount, setTotalCount] = useState(0);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Material | null>(null);
  const toast = useToast();
  const sortRef = useRef<HTMLDivElement>(null);

  // Search debounce
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);

  // Close sort dropdown on outside click
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (sortRef.current && !sortRef.current.contains(e.target as Node)) {
        setShowSort(false);
      }
    }
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, []);

  const fetchMaterials = useCallback(async () => {
    try {
      setLoading(true);
      setError("");
      const response = await materialService.listMaterials(0, 100);
      setMaterials(response.content.map(mapToMaterial));
      setTotalCount(response.totalElements);
    } catch {
      setError("Failed to load materials. Please try again.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMaterials();
  }, [fetchMaterials]);

  const handleUploadSuccess = () => {
    fetchMaterials();
  };

  const handleDelete = async () => {
    if (!confirmDelete) return;
    setDeletingId(confirmDelete.id);
    try {
      await materialService.deleteMaterial(confirmDelete.id);
      toast.success(`"${confirmDelete.title}" deleted`);
      setConfirmDelete(null);
      fetchMaterials();
    } catch {
      toast.error("Failed to delete material. Please try again.");
    } finally {
      setDeletingId(null);
    }
  };

  const isEmpty = materials.length === 0 && !loading && !error;

  const filteredAndSorted = useMemo(() => {
    let result = materials.filter((material) => {
      const searchText = debouncedSearch.toLowerCase();
      const matchesSearch =
        material.title.toLowerCase().includes(searchText) ||
        material.category.toLowerCase().includes(searchText);
      const matchesFilter =
        activeFilter === "All" ||
        (activeFilter === "PDF" && material.type === "PDF") ||
        (activeFilter === "Documents" && material.type === "DOCUMENT") ||
        (activeFilter === "Notes" && material.type === "NOTE");
      return matchesSearch && matchesFilter;
    });

    result = [...result].sort((a, b) => {
      switch (sortBy) {
        case "newest":
          return materials.indexOf(a) - materials.indexOf(b);
        case "oldest":
          return materials.indexOf(b) - materials.indexOf(a);
        case "name-asc":
          return a.title.localeCompare(b.title);
        case "name-desc":
          return b.title.localeCompare(a.title);
        default:
          return 0;
      }
    });

    return result;
  }, [materials, debouncedSearch, activeFilter, sortBy]);

  const noResults = filteredAndSorted.length === 0 && materials.length > 0;

  return (
    <>
      <div className="mx-auto max-w-7xl space-y-6">
        {/* Header */}
        <section className="flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
          <div>
            <p className="mb-1 text-sm font-medium text-[var(--mq-primary)]">
              Your knowledge space
            </p>

            <h1 className="text-2xl font-bold tracking-tight text-[var(--mq-text)] sm:text-3xl">
              Knowledge Vault
            </h1>

            <p className="mt-2 text-sm text-[var(--mq-text-secondary)] sm:text-base">
              Organize and access everything you're learning in one place.
            </p>
          </div>

          <button
            onClick={() => setUploadOpen(true)}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-[var(--mq-primary)] px-5 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-[var(--mq-primary-hover)]"
          >
            <Upload size={18} />
            Upload Material
          </button>
        </section>

      {/* Storage */}
      <StorageBar />

      {/* Search and controls */}
      <section className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-4">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            {/* Search */}
            <div className="relative w-full lg:max-w-md">
              <Search
                size={18}
                className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)]"
              />

              <input
                type="search"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Search your materials..."
                className="h-11 w-full rounded-xl border border-[var(--mq-border)] bg-[var(--mq-bg)] pl-11 pr-4 text-sm text-[var(--mq-text)] outline-none transition placeholder:text-[var(--mq-text-secondary)] focus:border-[var(--mq-primary)] focus:bg-[var(--mq-surface)] focus:ring-4 focus:ring-[var(--mq-primary)]/10"
              />
            </div>            {/* Controls */}
            <div className="flex items-center justify-between gap-2 sm:gap-3">
              {/* Filters */}
              <div className="flex items-center gap-1 overflow-x-auto rounded-xl bg-[var(--mq-bg)] p-1">
                {filters.map((filter) => (
                  <button
                    key={filter}
                    onClick={() => setActiveFilter(filter)}
                    className={`whitespace-nowrap rounded-lg px-2.5 py-1.5 text-xs font-medium transition sm:px-3 sm:py-2 ${
                      activeFilter === filter
                        ? "bg-[var(--mq-surface)] text-[var(--mq-primary)] shadow-sm"
                        : "text-[var(--mq-text-secondary)] hover:text-[var(--mq-text)]"
                    }`}
                  >
                    {filter}
                  </button>
                ))}
              </div>

              {/* Sort dropdown */}
              <div className="relative" ref={sortRef}>
                <button
                  onClick={() => setShowSort(!showSort)}
                  className="flex items-center gap-1.5 rounded-xl border border-[var(--mq-border)] px-3 py-2.5 text-xs font-medium text-[var(--mq-text-secondary)] transition hover:bg-[var(--mq-bg)] hover:text-[var(--mq-text)]"
                  aria-label="Sort materials"
                >
                  <SortAsc size={15} />
                  <span className="hidden sm:inline">
                    {sortOptions.find((o) => o.value === sortBy)?.label}
                  </span>
                </button>
                {showSort && (
                  <div className="absolute right-0 top-full z-10 mt-1 w-44 overflow-hidden rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] shadow-lg">
                    {sortOptions.map((option) => {
                      const Icon = option.icon;
                      return (
                        <button
                          key={option.value}
                          onClick={() => {
                            setSortBy(option.value);
                            setShowSort(false);
                          }}
                          className={`flex w-full items-center gap-2 px-3 py-2.5 text-left text-sm transition ${
                            sortBy === option.value
                              ? "bg-[var(--mq-primary-light)] text-[var(--mq-primary)]"
                              : "text-[var(--mq-text-secondary)] hover:bg-[var(--mq-bg)] hover:text-[var(--mq-text)]"
                          }`}
                        >
                          <Icon size={14} />
                          {option.label}
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>

              {/* View toggle */}
              <div className="hidden rounded-xl border border-[var(--mq-border)] p-1 sm:flex">
                <button
                  onClick={() => setView("grid")}
                  className={`rounded-lg p-2 ${
                    view === "grid"
                      ? "bg-[var(--mq-primary-light)] text-[var(--mq-primary)]"
                      : "text-[var(--mq-text-muted)]"
                  }`}
                  aria-label="Grid view"
                >
                  <Grid2X2 size={17} />
                </button>

                <button
                  onClick={() => setView("list")}
                  className={`rounded-lg p-2 ${
                    view === "list"
                      ? "bg-[var(--mq-primary-light)] text-[var(--mq-primary)]"
                      : "text-[var(--mq-text-muted)]"
                  }`}
                  aria-label="List view"
                >
                  <List size={17} />
                </button>
              </div>
            </div>
          </div>
        </section>

        {/* Results heading */}
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-semibold text-[var(--mq-text)]">
              Your Materials
            </h2>

            <p className="mt-1 text-sm text-[var(--mq-text-secondary)]">
              {filteredAndSorted.length}{" "}
              {filteredAndSorted.length === 1 ? "resource" : "resources"}
              {debouncedSearch && ` matching "${debouncedSearch}"`}
            </p>
          </div>
        </div>

        {/* Materials */}
        {loading ? (
          <SkeletonVault />
        ) : error ? (
          <div className="rounded-2xl border border-dashed border-[var(--mq-error-border)] bg-[var(--mq-surface)] px-6 py-16 text-center">
            <p className="text-sm text-[var(--mq-error)]">{error}</p>
            <button onClick={fetchMaterials} className="mt-3 text-sm font-medium text-[var(--mq-primary)] hover:text-[var(--mq-primary-hover)]">
              Try again
            </button>
          </div>
        ) : isEmpty ? (
          <div className="rounded-2xl border border-dashed border-[var(--mq-text-muted)] bg-[var(--mq-surface)] px-6 py-16 text-center">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-[var(--mq-primary-light)] text-[var(--mq-primary)]">
              <BookOpen size={21} />
            </div>
            <h3 className="mt-4 font-semibold text-[var(--mq-text)]">Your Knowledge Vault is empty</h3>
            <p className="mx-auto mt-2 max-w-sm text-sm text-[var(--mq-text-secondary)]">
              Upload your first learning resource and MindQ will turn it into an interactive study tool.
            </p>
            <button
              onClick={() => setUploadOpen(true)}
              className="mt-5 inline-flex items-center gap-2 rounded-xl bg-[var(--mq-primary)] px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-[var(--mq-primary-hover)]"
            >
              <Upload size={16} /> Upload Material
            </button>
          </div>
        ) : filteredAndSorted.length > 0 ? (
          <div
            className={
              view === "grid"
                ? "grid gap-3 sm:gap-4 sm:grid-cols-2 xl:grid-cols-3"
                : "grid gap-3"
            }
          >
            {filteredAndSorted.map((material) => (
              <MaterialCard key={material.id} material={material} />
            ))}
          </div>
        ) : noResults ? (
          <div className="rounded-2xl border border-dashed border-[var(--mq-text-muted)] bg-[var(--mq-surface)] px-6 py-16 text-center">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-[var(--mq-primary-light)] text-[var(--mq-primary)]">
              <Search size={21} />
            </div>
            <h3 className="mt-4 font-semibold text-[var(--mq-text)]">No materials found</h3>
            <p className="mx-auto mt-2 max-w-sm text-sm text-[var(--mq-text-secondary)]">
              Try changing your search or selecting a different material type.
            </p>
          </div>
        ) : null}
      </div>

      {/* Upload Modal */}
      <UploadMaterialModal
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
        onSuccess={handleUploadSuccess}
      />

      {/* Delete Confirmation Dialog */}
      {confirmDelete && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/30 px-4 backdrop-blur-[2px]" role="dialog" aria-modal="true" aria-label="Delete material confirmation">
          <FocusTrap onEscape={() => setConfirmDelete(null)}>
          <div className="w-full max-w-md overflow-hidden rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] shadow-[0_20px_60px_rgba(0, 0, 0, 0.15)]">
            <div className="p-6 text-center">
              <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-[var(--mq-error-light)] text-[var(--mq-error)]">
                <svg className="h-7 w-7" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" /></svg>
              </div>
              <h3 className="mt-4 text-lg font-bold text-[var(--mq-text)]">Delete Material?</h3>
              <p className="mt-2 text-sm text-[var(--mq-text-secondary)]">
                Are you sure you want to delete <span className="font-semibold text-[var(--mq-text)]">{confirmDelete.title}</span>? This action cannot be undone.
              </p>
            </div>
            <div className="flex gap-3 border-t border-[var(--mq-border)] px-6 py-4">
              <button
                type="button"
                onClick={() => setConfirmDelete(null)}
                disabled={deletingId !== null}
                className="flex-1 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 py-2.5 text-sm font-semibold text-[var(--mq-text)] transition hover:bg-[var(--mq-bg)] disabled:opacity-60"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleDelete}
                disabled={deletingId !== null}
                className="flex-1 inline-flex items-center justify-center gap-2 rounded-xl bg-[var(--mq-error)] px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-[var(--mq-error)] disabled:opacity-60"
              >
                {deletingId !== null ? "Deleting..." : "Delete"}
              </button>
            </div>
          </div>
          </FocusTrap>
        </div>
      )}
    </>
  );
}