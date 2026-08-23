import { useEffect, useState, type ReactNode } from "react";

interface PageTransitionProps {
  children: ReactNode;
}

export default function PageTransition({ children }: PageTransitionProps) {
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    // Trigger animation on mount
    requestAnimationFrame(() => setIsVisible(true));
  }, []);

  return (
    <div
      className={`transition-opacity duration-300 ease-out ${
        isVisible ? "opacity-100" : "opacity-0"
      }`}
      style={{ transform: isVisible ? "translateY(0)" : "translateY(8px)" }}
    >
      {children}
    </div>
  );
}
