import { useEffect, useRef, type ReactNode } from "react";

interface FocusTrapProps {
  children: ReactNode;
  enabled?: boolean;
  onEscape?: () => void;
}

export default function FocusTrap({
  children,
  enabled = true,
  onEscape,
}: FocusTrapProps) {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!enabled) return;

    const container = containerRef.current;
    if (!container) return;

    // Get all focusable elements
    const focusableSelectors =
      'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape" && onEscape) {
        onEscape();
        return;
      }

      if (e.key !== "Tab") return;

      // container is guaranteed non-null here due to early return above
      const focusable = container!.querySelectorAll(focusableSelectors);
      const first = focusable[0] as HTMLElement;
      const last = focusable[focusable.length - 1] as HTMLElement;

      if (!first || !last) return;

      if (e.shiftKey) {
        // Shift+Tab: if on first, go to last
        if (document.activeElement === first) {
          e.preventDefault();
          last.focus();
        }
      } else {
        // Tab: if on last, go to first
        if (document.activeElement === last) {
          e.preventDefault();
          first.focus();
        }
      }
    }

    // Focus the first focusable element on mount
    const firstFocusable = container!.querySelector(focusableSelectors) as HTMLElement;
    if (firstFocusable) {
      firstFocusable.focus();
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [enabled, onEscape]);

  return <div ref={containerRef}>{children}</div>;
}
