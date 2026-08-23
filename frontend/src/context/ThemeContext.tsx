import { createContext, useContext, useEffect, useState, useCallback, useMemo, type ReactNode } from "react";

type ThemePreference = "light" | "dark" | "system";
type ResolvedTheme = "light" | "dark";

interface ThemeContextType {
  /** User's stored preference */
  theme: ThemePreference;
  /** Actually applied theme */
  resolvedTheme: ResolvedTheme;
  setTheme: (theme: ThemePreference) => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

const STORAGE_KEY = "mindq_theme";

function getSystemPreference(): ResolvedTheme {
  if (typeof window === "undefined") return "light";
  return window.matchMedia("(prefers-color-scheme: dark)").matches
    ? "dark"
    : "light";
}

function resolveTheme(pref: ThemePreference): ResolvedTheme {
  return pref === "system" ? getSystemPreference() : pref;
}

/** Read saved preference, defaulting to "system" */
function getSavedPreference(): ThemePreference {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === "light" || saved === "dark" || saved === "system") return saved;
  } catch { /* localStorage unavailable */ }
  return "system";
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [preference, setPreference] = useState<ThemePreference>(getSavedPreference);
  const [resolved, setResolved] = useState<ResolvedTheme>(() => resolveTheme(getSavedPreference()));

  // Apply theme class to <html>
  useEffect(() => {
    const root = document.documentElement;
    if (resolved === "dark") {
      root.classList.add("dark");
    } else {
      root.classList.remove("dark");
    }
  }, [resolved]);

  // Listen for system preference changes when in "system" mode
  useEffect(() => {
    if (preference !== "system") return;

    const mq = window.matchMedia("(prefers-color-scheme: dark)");
    const handler = (e: MediaQueryListEvent) => {
      setResolved(e.matches ? "dark" : "light");
    };

    mq.addEventListener("change", handler);
    return () => mq.removeEventListener("change", handler);
  }, [preference]);

  // Re-resolve when preference changes
  useEffect(() => {
    setResolved(resolveTheme(preference));
  }, [preference]);

  const setTheme = useCallback((theme: ThemePreference) => {
    setPreference(theme);
    try {
      localStorage.setItem(STORAGE_KEY, theme);
    } catch { /* localStorage unavailable */ }
  }, []);

  const value = useMemo(
    () => ({ theme: preference, resolvedTheme: resolved, setTheme }),
    [preference, resolved, setTheme],
  );

  return (
    <ThemeContext.Provider value={value}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme(): ThemeContextType {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error("useTheme must be used within a ThemeProvider");
  return ctx;
}
