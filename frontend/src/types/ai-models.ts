import type { LucideIcon } from "lucide-react";
import { Zap, Brain, Microscope, Sparkles } from "lucide-react";

/**
 * User-facing model option with role, description, and badge.
 * The modelCode must match exactly what the backend expects.
 */
export interface AIModelOption {
  modelCode: string | null; // null = Auto mode
  name: string;
  role: string;
  description: string;
  icon: LucideIcon;
  badge: string;
  reasoningEffort?: "low" | "medium" | "high" | "none" | "default";
}

/**
 * Reasoning effort levels for UI presentation.
 */
export interface ReasoningOption {
  value: "auto" | "standard" | "deep";
  label: string;
  description: string;
}

/**
 * Centralized model configuration.
 * Technical model codes remain unchanged internally.
 */
export const MODEL_OPTIONS: AIModelOption[] = [
  {
    modelCode: null,
    name: "Auto",
    role: "Auto — Recommended",
    description: "MindQ chooses the best available model for your request.",
    icon: Sparkles,
    badge: "RECOMMENDED",
  },
  {
    modelCode: "openai/gpt-oss-20b",
    name: "GPT-OSS 20B",
    role: "Fast",
    description: "Fast generation for everyday quizzes and quick practice.",
    icon: Zap,
    badge: "FAST",
  },
  {
    modelCode: "qwen/qwen3.6-27b",
    name: "Qwen 3.6 27B",
    role: "Balanced",
    description: "A balanced choice for quality, reasoning, and speed.",
    icon: Brain,
    badge: "BALANCED",
  },
  {
    modelCode: "openai/gpt-oss-120b",
    name: "GPT-OSS 120B",
    role: "Deep Think",
    description: "Designed for more demanding reasoning tasks.",
    icon: Microscope,
    badge: "DEEP THINK",
  },
];

/**
 * Reasoning effort options for the UI.
 */
export const REASONING_OPTIONS: ReasoningOption[] = [
  {
    value: "auto",
    label: "Automatic",
    description: "Let the model decide based on the task",
  },
  {
    value: "standard",
    label: "Standard",
    description: "Normal reasoning effort",
  },
  {
    value: "deep",
    label: "Deep",
    description: "Extended reasoning for complex questions",
  },
];

/**
 * Map UI reasoning option to backend reasoning effort.
 */
export function mapReasoningToBackend(
  uiValue: "auto" | "standard" | "deep",
  modelCode: string | null,
): string {
  // Auto mode: let backend decide
  if (uiValue === "auto") return "auto";

  // GPT-OSS models support low/medium/high
  if (modelCode?.includes("gpt-oss")) {
    return uiValue === "deep" ? "high" : "medium";
  }

  // Qwen 3.6 27B supports none/default
  if (modelCode?.includes("qwen")) {
    return uiValue === "deep" ? "default" : "none";
  }

  // Unknown model: use auto
  return "auto";
}

/**
 * Get model recommendation based on difficulty.
 */
export function getModelRecommendation(
  difficulty: "EASY" | "MEDIUM" | "HARD",
): string | null {
  switch (difficulty) {
    case "EASY":
      return "openai/gpt-oss-20b"; // Fast
    case "MEDIUM":
      return "qwen/qwen3.6-27b"; // Balanced
    case "HARD":
      return "openai/gpt-oss-120b"; // Deep Think
    default:
      return null;
  }
}

/**
 * Get model option by model code (null for Auto).
 */
export function getModelOption(modelCode: string | null): AIModelOption {
  return MODEL_OPTIONS.find((m) => m.modelCode === modelCode) ?? MODEL_OPTIONS[0];
}
