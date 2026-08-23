import api from "./api";
import type { ApiResponse } from "../types/auth";

export interface QuizAnswerSubmission {
  questionId: number;
  selectedOptionId: number;
}

export interface AnswerResult {
  questionId: number;
  questionText: string;
  selectedOptionId: number | null;
  selectedOptionText: string | null;
  correctOptionId: number;
  correctOptionText: string;
  isCorrect: boolean;
  explanation: string;
}

export interface QuizResultResponse {
  attemptId: number;
  score: number;
  totalQuestions: number;
  percentage: number;
  status: string;
  answers: AnswerResult[];
}

export interface SubmitQuizOptions {
  quizMode?: string;
  timeLimitMinutes?: number;
  timeSpentSeconds?: number;
}

export async function submitQuiz(
  mcqSetId: number,
  answers: QuizAnswerSubmission[],
  options?: SubmitQuizOptions,
): Promise<QuizResultResponse> {
  const response = await api.post<ApiResponse<QuizResultResponse>>(
    `/mcq/${mcqSetId}/submit`,
    {
      answers,
      ...options,
    },
    {
      timeout: 30000,
    },
  );
  return response.data.data;
}
