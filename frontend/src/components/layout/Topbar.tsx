import {
  Bell,
  Clock,
  FileText,
  Loader2,
  Menu,
  Monitor,
  Moon,
  Search,
  Sun,
  Trash2,
  X,
  XCircle,
} from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { useTheme } from "../../context/ThemeContext";
import {
  globalSearch,
  type SearchResult,
} from "../../services/searchService";

interface TopbarProps {
  onMenuClick: () => void;
}

const THEME_ORDER: Array<"system" | "light" | "dark"> = ["system", "light", "dark"];

const THEME_CONFIG = {
  system: { icon: Monitor, label: "System", ariaLabel: "Theme: System (click to switch to Light)" },
  light: { icon: Sun, label: "Light", ariaLabel: "Theme: Light (click to switch to Dark)" },
  dark: { icon: Moon, label: "Dark", ariaLabel: "Theme: Dark (click to switch to System)" },
} as const;

// Recent searches persistence
const RECENT_SEARCHES_KEY = "mindq_recent_searches";
const MAX_RECENT = 8;

function loadRecentSearches(): string[] {
  try {
    const raw = localStorage.getItem(RECENT_SEARCHES_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function saveRecentSearches(searches: string[]) {
  localStorage.setItem(RECENT_SEARCHES_KEY, JSON.stringify(searches));
}

export default function Topbar({ onMenuClick }: TopbarProps) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { theme, setTheme } = useTheme();

  // Search state
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchError, setSearchError] = useState("");
  const [open, setOpen] = useState(false);
  const [highlightIndex, setHighlightIndex] = useState(-1);
  const [recentSearches, setRecentSearches] = useState<string[]>(loadRecentSearches);
  const inputRef = useRef<HTMLInputElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout>>();

  const initials = user?.fullName
    ?.split(" ")
    .map((n) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2) ?? "M";

  const cycleTheme = () => {
    const currentIndex = THEME_ORDER.indexOf(theme);
    const nextIndex = (currentIndex + 1) % THEME_ORDER.length;
    setTheme(THEME_ORDER[nextIndex]);
  };

  // Save a search term to history
  const addRecentSearch = useCallback((term: string) => {
    const trimmed = term.trim();
    if (!trimmed) return;
    setRecentSearches((prev) => {
      const updated = [trimmed, ...prev.filter((s) => s !== trimmed)].slice(0, MAX_RECENT);
      saveRecentSearches(updated);
      return updated;
    });
  }, []);

  // Clear all recent searches
  const clearRecentSearches = useCallback(() => {
    setRecentSearches([]);
    saveRecentSearches([]);
  }, []);

  // Remove a single recent search
  const removeRecentSearch = useCallback((term: string) => {
    setRecentSearches((prev) => {
      const updated = prev.filter((s) => s !== term);
      saveRecentSearches(updated);
      return updated;
    });
  }, []);

  // Debounced search
  const doSearch = useCallback(async (q: string) => {
    if (!q.trim()) {
      setResults([]);
      setSearchError("");
      setLoading(false);
      return;
    }
    setLoading(true);
    setSearchError("");
    try {
      const data = await globalSearch(q);
      setResults(data);
    } catch (err: unknown) {
      setResults([]);
      let msg = "Search failed. Please try again.";
      if (typeof err === "object" && err !== null && "response" in err) {
        const axiosErr = err as { response?: { status?: number; data?: { message?: string } } };
        if (axiosErr.response?.status === 500) {
          msg = "Search is temporarily unavailable. Please try again in a moment.";
        } else if (axiosErr.response?.data?.message) {
          msg = axiosErr.response.data.message;
        } else if (axiosErr.response?.status === 401) {
          msg = "Session expired. Please log in again.";
        }
      } else if (err instanceof Error) {
        msg = err.message || msg;
      }
      setSearchError(msg);
    } finally {
      setLoading(false);
    }
  }, []);

  const handleQueryChange = (value: string) => {
    setQuery(value);
    setHighlightIndex(-1);
    setSearchError("");
    clearTimeout(debounceRef.current);
    if (!value.trim()) {
      setResults([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    debounceRef.current = setTimeout(() => doSearch(value), 300);
  };

  // Select a recent search term
  const selectRecentSearch = (term: string) => {
    setQuery(term);
    setHighlightIndex(-1);
    addRecentSearch(term);
    doSearch(term);
    inputRef.current?.focus();
  };

  // Close dropdown on outside click
  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(e.target as Node) &&
        inputRef.current &&
        !inputRef.current.contains(e.target as Node)
      ) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, []);

  // Keyboard navigation
  const handleKeyDown = (e: React.KeyboardEvent) => {
    const isRecentMode = open && !query.trim() && recentSearches.length > 0 && !loading;
    const isResultsMode = open && (results.length > 0 || loading);
    if (!isRecentMode && !isResultsMode) return;

    const listLength = isRecentMode ? recentSearches.length : results.length;

    switch (e.key) {
      case "ArrowDown":
        e.preventDefault();
        setHighlightIndex((prev) =>
          prev < listLength - 1 ? prev + 1 : 0,
        );
        break;
      case "ArrowUp":
        e.preventDefault();
        setHighlightIndex((prev) =>
          prev > 0 ? prev - 1 : listLength - 1,
        );
        break;
      case "Enter":
        e.preventDefault();
        if (isRecentMode && highlightIndex >= 0 && highlightIndex < recentSearches.length) {
          selectRecentSearch(recentSearches[highlightIndex]);
        } else if (isResultsMode && highlightIndex >= 0 && highlightIndex < results.length) {
          addRecentSearch(query);
          navigateToResult(results[highlightIndex]);
        } else if (isResultsMode && query.trim()) {
          // If nothing highlighted but there are results, just save the search
          addRecentSearch(query);
        }
        break;
      case "Escape":
        setOpen(false);
        setHighlightIndex(-1);
        inputRef.current?.blur();
        break;
    }
  };

  const navigateToResult = (result: SearchResult) => {
    setOpen(false);
    setQuery("");
    setResults([]);
    navigate(result.link);
  };

  const themeConfig = THEME_CONFIG[theme];
  const ThemeIcon = themeConfig.icon;

  const hasResults = results.length > 0;
  const showRecent = open && !query.trim() && recentSearches.length > 0 && !loading;
  const showDropdown = open && (hasResults || loading || query.trim().length > 0 || showRecent);

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-[var(--mq-border)] bg-[var(--mq-surface)]/95 px-4 backdrop-blur-sm sm:px-6 lg:px-8">
      {/* Mobile menu */}
      <button
        onClick={onMenuClick}
        className="rounded-lg p-2 text-[var(--mq-text-secondary)] hover:bg-[var(--mq-surface-hover)] lg:hidden"
        aria-label="Open navigation"
      >
        <Menu size={21} />
      </button>

      {/* Mobile brand */}
      <div className="flex items-center gap-2 lg:hidden">
        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-[var(--mq-primary)]">
          <span className="text-xs font-bold text-white">Q</span>
        </div>
        <span className="text-base font-bold text-[var(--mq-text)]">MindQ</span>
      </div>

      {/* Search */}
      <div className="relative hidden w-full max-w-sm sm:block" ref={dropdownRef}>
        <Search
          size={18}
          className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)] pointer-events-none"
        />

        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={(e) => handleQueryChange(e.target.value)}
          onFocus={() => setOpen(true)}
          onKeyDown={handleKeyDown}
          placeholder="Search materials, quizzes..."
          className="h-10 w-full rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface-hover)] pl-10 pr-9 text-sm text-[var(--mq-text)] outline-none transition placeholder:text-[var(--mq-text-muted)] focus:border-[var(--mq-primary)] focus:bg-[var(--mq-surface)] focus:ring-2 focus:ring-[var(--mq-primary)]/10"
          autoComplete="off"
        />

        {/* Clear button */}
        {query && (
          <button
            onClick={() => {
              setQuery("");
              setResults([]);
              setHighlightIndex(-1);
              inputRef.current?.focus();
            }}
            className="absolute right-3 top-1/2 -translate-y-1/2 rounded-md p-0.5 text-[var(--mq-text-muted)] hover:text-[var(--mq-text-secondary)]"
            aria-label="Clear search"
          >
            <X size={14} />
          </button>
        )}

        {/* Dropdown */}
        {showDropdown && (
          <div className="absolute top-full left-0 right-0 mt-2 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] shadow-xl overflow-hidden z-50">

            {/* Recent searches */}
            {showRecent && (
              <div>
                <div className="flex items-center justify-between px-4 py-2 bg-[var(--mq-bg)]">
                  <span className="text-[10px] font-semibold uppercase tracking-wider text-[var(--mq-text-muted)]">
                    Recent Searches
                  </span>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      clearRecentSearches();
                    }}
                    className="flex items-center gap-1 text-[10px] text-[var(--mq-text-muted)] hover:text-[var(--mq-error)] transition"
                  >
                    <Trash2 size={10} />
                    Clear
                  </button>
                </div>
                {recentSearches.map((term, idx) => (
                  <div key={`recent-${term}-${idx}`} className="flex items-center">
                    <button
                      onClick={() => selectRecentSearch(term)}
                      onMouseEnter={() => setHighlightIndex(idx)}
                      className={`flex flex-1 items-center gap-3 px-4 py-2.5 text-left transition ${
                        highlightIndex === idx
                          ? "bg-[var(--mq-primary-light)]"
                          : "hover:bg-[var(--mq-bg)]"
                      }`}
                    >
                      <Clock size={14} className="shrink-0 text-[var(--mq-text-muted)]" />
                      <span className="truncate text-sm text-[var(--mq-text)]">
                        {term}
                      </span>
                    </button>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        removeRecentSearch(term);
                      }}
                      className="mr-2 shrink-0 rounded-md p-1 text-[var(--mq-text-muted)] hover:text-[var(--mq-error)] transition"
                      aria-label={`Remove "${term}" from history`}
                    >
                      <X size={12} />
                    </button>
                  </div>
                ))}
              </div>
            )}

            {/* Loading */}
            {loading && (
              <div className="flex items-center gap-2 px-4 py-3 text-sm text-[var(--mq-text-secondary)]">
                <Loader2 size={14} className="animate-spin" />
                Searching...
              </div>
            )}

            {/* Search results */}
            {!loading && hasResults && (
              <>
                {/* Result count header */}
                <div className="flex items-center justify-between border-b border-[var(--mq-border)] px-4 py-2 bg-[var(--mq-bg)]">
                  <span className="text-[10px] font-semibold uppercase tracking-wider text-[var(--mq-text-muted)]">
                    {results.length} result{results.length !== 1 ? "s" : ""}
                  </span>
                  <div className="flex items-center gap-2">
                    {results.filter((r) => r.type === "MATERIAL").length > 0 && (
                      <span className="rounded-full bg-[var(--mq-primary-light)] px-2 py-0.5 text-[9px] font-medium text-[var(--mq-primary)]">
                        {results.filter((r) => r.type === "MATERIAL").length} materials
                      </span>
                    )}
                    {results.filter((r) => r.type === "QUIZ").length > 0 && (
                      <span className="rounded-full bg-[var(--mq-ai-light)] px-2 py-0.5 text-[9px] font-medium text-[var(--mq-ai)]">
                        {results.filter((r) => r.type === "QUIZ").length} quizzes
                      </span>
                    )}
                    {results.filter((r) => r.type === "ATTEMPT").length > 0 && (
                      <span className="rounded-full bg-[var(--mq-success-light)] px-2 py-0.5 text-[9px] font-medium text-[var(--mq-success)]">
                        {results.filter((r) => r.type === "ATTEMPT").length} attempts
                      </span>
                    )}
                    {results.filter((r) => r.type === "GENERATION").length > 0 && (
                      <span className="rounded-full bg-[var(--mq-ai-light)] px-2 py-0.5 text-[9px] font-medium text-[var(--mq-ai)]">
                        {results.filter((r) => r.type === "GENERATION").length} generations
                      </span>
                    )}
                  </div>
                </div>
                <div className="max-h-80 overflow-y-auto">
                {/* Materials */}
                {results.some((r) => r.type === "MATERIAL") && (
                  <div>
                    <div className="px-4 py-2 text-[10px] font-semibold uppercase tracking-wider text-[var(--mq-text-muted)] bg-[var(--mq-bg)]">
                      Study Materials
                    </div>
                    {results
                      .filter((r) => r.type === "MATERIAL")
                      .map((result) => {
                        const globalIdx = results.indexOf(result);
                        return (
                          <button
                            key={`mat-${result.id}`}
                            onClick={() => {
                              addRecentSearch(query);
                              navigateToResult(result);
                            }}
                            onMouseEnter={() => setHighlightIndex(globalIdx)}
                            className={`flex w-full items-center gap-3 px-4 py-2.5 text-left transition ${
                              highlightIndex === globalIdx
                                ? "bg-[var(--mq-primary-light)]"
                                : "hover:bg-[var(--mq-bg)]"
                            }`}
                          >
                            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[var(--mq-primary-light)] text-[var(--mq-primary)]">
                              <FileText size={14} />
                            </div>
                            <div className="min-w-0 flex-1">
                              <p className="truncate text-sm font-medium text-[var(--mq-text)]">
                                {result.title}
                              </p>
                              <p className="truncate text-xs text-[var(--mq-text-muted)]">
                                {result.subtitle}
                              </p>
                            </div>
                            <span className="shrink-0 rounded-md bg-[var(--mq-bg)] px-2 py-0.5 text-[10px] font-medium text-[var(--mq-text-muted)]">
                              Material
                            </span>
                          </button>
                        );
                      })}
                  </div>
                )}

                {/* Quizzes */}
                {results.some((r) => r.type === "QUIZ") && (
                  <div>
                    <div className="px-4 py-2 text-[10px] font-semibold uppercase tracking-wider text-[var(--mq-text-muted)] bg-[var(--mq-bg)]">
                      Quizzes
                    </div>
                    {results
                      .filter((r) => r.type === "QUIZ")
                      .map((result) => {
                        const globalIdx = results.indexOf(result);
                        return (
                          <button
                            key={`quiz-${result.id}`}
                            onClick={() => {
                              addRecentSearch(query);
                              navigateToResult(result);
                            }}
                            onMouseEnter={() => setHighlightIndex(globalIdx)}
                            className={`flex w-full items-center gap-3 px-4 py-2.5 text-left transition ${
                              highlightIndex === globalIdx
                                ? "bg-[var(--mq-primary-light)]"
                                : "hover:bg-[var(--mq-bg)]"
                            }`}
                          >
                            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[var(--mq-ai-light)] text-[var(--mq-ai)]">
                              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M9 11l3 3L22 4" />
                                <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11" />
                              </svg>
                            </div>
                            <div className="min-w-0 flex-1">
                              <p className="truncate text-sm font-medium text-[var(--mq-text)]">
                                {result.title}
                              </p>
                              <p className="truncate text-xs text-[var(--mq-text-muted)]">
                                {result.subtitle}
                              </p>
                            </div>
                            <span className="shrink-0 rounded-md bg-[var(--mq-bg)] px-2 py-0.5 text-[10px] font-medium text-[var(--mq-text-muted)]">
                              Quiz
                            </span>
                          </button>
                        );
                      })}
                  </div>
                )}

                {/* Quiz Attempts */}
                {results.some((r) => r.type === "ATTEMPT") && (
                  <div>
                    <div className="px-4 py-2 text-[10px] font-semibold uppercase tracking-wider text-[var(--mq-text-muted)] bg-[var(--mq-bg)]">
                      Quiz History
                    </div>
                    {results
                      .filter((r) => r.type === "ATTEMPT")
                      .map((result) => {
                        const globalIdx = results.indexOf(result);
                        return (
                          <button
                            key={`attempt-${result.id}`}
                            onClick={() => {
                              addRecentSearch(query);
                              navigateToResult(result);
                            }}
                            onMouseEnter={() => setHighlightIndex(globalIdx)}
                            className={`flex w-full items-center gap-3 px-4 py-2.5 text-left transition ${
                              highlightIndex === globalIdx
                                ? "bg-[var(--mq-primary-light)]"
                                : "hover:bg-[var(--mq-bg)]"
                            }`}
                          >
                            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[var(--mq-success-light)] text-[var(--mq-success)]">
                              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M12 20V10" /><path d="M18 20V4" /><path d="M6 20v-4" />
                              </svg>
                            </div>
                            <div className="min-w-0 flex-1">
                              <p className="truncate text-sm font-medium text-[var(--mq-text)]">
                                {result.title}
                              </p>
                              <p className="truncate text-xs text-[var(--mq-text-muted)]">
                                {result.subtitle}
                              </p>
                            </div>
                            <span className="shrink-0 rounded-md bg-[var(--mq-bg)] px-2 py-0.5 text-[10px] font-medium text-[var(--mq-text-muted)]">
                              Attempt
                            </span>
                          </button>
                        );
                      })}
                  </div>
                )}

                {/* AI Generations */}
                {results.some((r) => r.type === "GENERATION") && (
                  <div>
                    <div className="px-4 py-2 text-[10px] font-semibold uppercase tracking-wider text-[var(--mq-text-muted)] bg-[var(--mq-bg)]">
                      AI Generations
                    </div>
                    {results
                      .filter((r) => r.type === "GENERATION")
                      .map((result) => {
                        const globalIdx = results.indexOf(result);
                        return (
                          <button
                            key={`gen-${result.id}`}
                            onClick={() => {
                              addRecentSearch(query);
                              navigateToResult(result);
                            }}
                            onMouseEnter={() => setHighlightIndex(globalIdx)}
                            className={`flex w-full items-center gap-3 px-4 py-2.5 text-left transition ${
                              highlightIndex === globalIdx
                                ? "bg-[var(--mq-primary-light)]"
                                : "hover:bg-[var(--mq-bg)]"
                            }`}
                          >
                            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[var(--mq-ai-light)] text-[var(--mq-ai)]">
                              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
                              </svg>
                            </div>
                            <div className="min-w-0 flex-1">
                              <p className="truncate text-sm font-medium text-[var(--mq-text)]">
                                {result.title}
                              </p>
                              <p className="truncate text-xs text-[var(--mq-text-muted)]">
                                {result.subtitle}
                              </p>
                            </div>
                            <span className="shrink-0 rounded-md bg-[var(--mq-bg)] px-2 py-0.5 text-[10px] font-medium text-[var(--mq-text-muted)]">
                              Generation
                            </span>
                          </button>
                        );
                      })}
                  </div>
                )}
              </div>
              </>
            )}

            {/* Search error */}
            {!loading && searchError && query.trim().length > 0 && (
              <div className="px-4 py-4">
                <div className="flex items-center gap-3 rounded-lg border border-[var(--mq-error-border)] bg-[var(--mq-error-light)] px-3 py-2.5">
                  <XCircle size={14} className="shrink-0 text-[var(--mq-error)]" />
                  <span className="text-xs text-[var(--mq-error)]">{searchError}</span>
                </div>
              </div>
            )}

            {/* Empty state */}
            {!loading && !hasResults && !searchError && query.trim().length > 0 && (
              <div className="px-4 py-6 text-center">
                <Search size={20} className="mx-auto mb-2 text-[var(--mq-text-muted)]" />
                <p className="text-sm text-[var(--mq-text-secondary)]">
                  No results for "{query}"
                </p>
                <p className="mt-1 text-xs text-[var(--mq-text-muted)]">
                  Try searching for a material title or quiz topic
                </p>
              </div>
            )}

            {/* Footer hint */}
            {!loading && (hasResults || showRecent) && (
              <div className="border-t border-[var(--mq-border)] bg-[var(--mq-bg)] px-4 py-2 text-[10px] text-[var(--mq-text-muted)]">
                {showRecent ? "↑↓ navigate · ↵ search · esc close" : "↑↓ navigate · ↵ select · esc close"}
              </div>
            )}
          </div>
        )}
      </div>

      <div className="ml-auto flex items-center gap-2">
        {/* Notifications */}
        <button
          disabled
          className="relative rounded-xl p-2.5 text-[var(--mq-text-muted)] cursor-not-allowed"
          aria-label="Notifications (coming soon)"
          title="Notifications coming soon"
        >
          <Bell size={20} />
        </button>

        {/* Theme toggle */}
        <button
          onClick={cycleTheme}
          className="rounded-xl p-2.5 text-[var(--mq-text-secondary)] transition hover:bg-[var(--mq-surface-hover)] hover:text-[var(--mq-text)]"
          aria-label={themeConfig.ariaLabel}
          title={`Theme: ${themeConfig.label}`}
        >
          <ThemeIcon size={20} />
        </button>

        {/* Profile */}
        <button id="topbar-profile" onClick={() => navigate("/profile")} className="flex items-center gap-2 rounded-xl p-1.5 transition hover:bg-[var(--mq-surface-hover)]">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-[var(--mq-primary-light)] text-xs font-semibold text-[var(--mq-primary)]">
            {initials}
          </div>
          <span className="hidden text-sm font-medium text-[var(--mq-text)] md:block">
            {user?.fullName?.split(" ")[0] ?? "User"}
          </span>
        </button>
      </div>
    </header>
  );
}
