export interface AdminDashboardStats {
  totalUsers: number;
  activeUsers: number;
  totalMaterials: number;
  totalQuizzes: number;
  totalAiGenerations: number;
  totalRevenue: number;
  storageUsedBytes: number;
}

export interface AdminUser {
  id: number;
  email: string;
  fullName: string;
  role: string;
  status: string;
  planCode: string;
  materialCount: number;
  quizCount: number;
  createdAt: string;
}
