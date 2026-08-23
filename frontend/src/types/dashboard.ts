export interface QuizHistoryItem {
  id: number;
  mcqSetId: number;
  mcqSetTitle: string;
  materialTitle: string | null;
  score: number;
  totalQuestions: number;
  percentage: number;
  timeSpentSeconds: number;
  status: string;
  startedAt: string;
  completedAt: string | null;
}

export interface DashboardStats {
  totalMaterials: number;
  totalQuizzes: number;
  averageScore: number;
  totalQuestionsAnswered: number;
  recentMaterials: Array<{
    id: number;
    title: string;
    materialType: string;
    updatedAt: string;
  }>;
  recentActivity: Array<{
    id: number;
    title: string;
    time: string;
    score: number | null;
    type: 'quiz' | 'material';
  }>;
  continueLearning: {
    materialId: number;
    materialTitle: string;
    materialType: string;
    quizTitle: string;
    lastScore: number;
  } | null;
  weakAreas: Array<{
    title: string;
    score: number;
    quizId: number;
  }>;
}
