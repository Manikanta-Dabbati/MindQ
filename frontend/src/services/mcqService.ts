import api from "./api";
import type { ApiResponse } from "../types/auth";
import type { GenerateMcqRequest, McqSetResponse } from "../types/mcq";

export async function generateMcqs(
  request: GenerateMcqRequest,
): Promise<McqSetResponse> {
  const response = await api.post<ApiResponse<McqSetResponse>>(
    "/mcq/generate",
    request,
    {
      timeout: 120000,
    },
  );
  return response.data.data;
}

export async function getQuizHistory() {
  const response = await api.get<ApiResponse<any[]>>("/mcq/history");
  return response.data.data;
}

export async function downloadQuiz(mcqSetId: number): Promise<Blob> {
  const response = await api.get(`/mcq/${mcqSetId}/download`, {
    responseType: "blob",
  });
  return response.data;
}

export async function saveQuizToVault(mcqSetId: number) {
  const response = await api.post<ApiResponse<any>>(`/mcq/${mcqSetId}/save-to-vault`);
  return response.data.data;
}

export async function getMcqSet(id: number): Promise<McqSetResponse> {
  const response = await api.get<ApiResponse<McqSetResponse>>(`/mcq/${id}`);
  return response.data.data;
}

export interface QuizAnswerData {
  questionId: number;
  selectedOptionId: number | null;
  selectedOptionText: string | null;
  isCorrect: boolean;
  correctOptionId: number | null;
  correctOptionText: string | null;
  timeTakenSeconds: number;
}

export async function getAttemptAnswers(attemptId: number): Promise<QuizAnswerData[]> {
  const response = await api.get<ApiResponse<QuizAnswerData[]>>(`/mcq/attempt/${attemptId}/answers`);
  return response.data.data;
}
