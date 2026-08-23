export interface TopicPerformance {
  topic: string;
  totalQuestions: number;
  correctAnswers: number;
  accuracy: number;
  quizCount: number;
}

export interface DailyActivity {
  date: string;
  quizzesTaken: number;
  questionsAnswered: number;
  correctAnswers: number;
  averageScore: number;
}

export interface AnalyticsOverview {
  totalQuizzes: number;
  totalQuestions: number;
  correctAnswers: number;
  incorrectAnswers: number;
  averageScore: number;
  totalMaterials: number;
  totalFlashcards: number;
  studyStreak: number;
  favoriteTopic: string;
  topicPerformance: TopicPerformance[];
  recentActivity: DailyActivity[];
}
