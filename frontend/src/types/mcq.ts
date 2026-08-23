export interface OptionResponse {
  id: number;
  optionText: string;
  isCorrect: boolean;
  optionOrder: number;
}

export interface QuestionResponse {
  id: number;
  questionText: string;
  explanation: string;
  questionType: string;
  difficulty: string;
  orderIndex: number;
  options: OptionResponse[];
}

export interface McqSetResponse {
  id: number;
  title: string;
  description: string;
  topic: string | null;
  difficulty: string;
  totalQuestions: number;
  materialId: number;
  questions: QuestionResponse[];
  createdAt: string;
}

export interface GenerateMcqRequest {
  reasoningEffort?: string;
  materialId?: number;
  prompt?: string;
  modelCode?: string;
  numberOfQuestions: number;
  difficulty: "EASY" | "MEDIUM" | "HARD" | "MIXED";
  topic?: string;
}

export type QuizMode = "PRACTICE" | "TIMED" | "EXAM";
