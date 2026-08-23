import api from './api';
import type { MaterialSummary } from '../types/material';
import type { QuizHistoryItem, DashboardStats } from '../types/dashboard';

function formatRelativeTime(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays === 1) return 'Yesterday';
  if (diffDays < 7) return `${diffDays} days ago`;
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

export async function getDashboardStats(): Promise<DashboardStats> {
  const [materialsRes, historyRes] = await Promise.all([
    api.get<{ data: { content: MaterialSummary[]; totalElements: number } }>('/materials', { params: { page: 0, size: 100 } }),
    api.get<{ data: QuizHistoryItem[] }>('/mcq/history'),
  ]);

  const materials = materialsRes.data.data.content;
  const history = historyRes.data.data;

  // Stats
  const totalMaterials = materials.length;
  const completedQuizzes = history.filter((h) => h.status === 'COMPLETED');
  const totalQuizzes = completedQuizzes.length;
  const totalQuestionsAnswered = completedQuizzes.reduce((sum, h) => sum + h.totalQuestions, 0);
  const averageScore =
    totalQuizzes > 0
      ? Math.round(
          completedQuizzes.reduce((sum, h) => sum + h.percentage, 0) / totalQuizzes,
        )
      : 0;

  // Continue Learning: most recent quiz with a material
  const continueLearning = (() => {
    const recentQuizWithMaterial = completedQuizzes.find((h) => h.materialTitle);
    if (recentQuizWithMaterial) {
      return {
        materialId: recentQuizWithMaterial.id,
        materialTitle: recentQuizWithMaterial.materialTitle!,
        materialType: 'Study Material',
        quizTitle: recentQuizWithMaterial.mcqSetTitle,
        lastScore: Math.round(recentQuizWithMaterial.percentage),
      };
    }
    return null;
  })();

  // Weak Areas: quizzes with score below 70%
  const weakAreas = completedQuizzes
    .filter((h) => h.percentage < 70)
    .slice(0, 3)
    .map((h) => ({
      title: h.mcqSetTitle,
      score: Math.round(h.percentage),
      quizId: h.mcqSetId,
    }));

  // Recent materials (top 3)
  const recentMaterials = materials.slice(0, 3).map((m) => ({
    id: m.id,
    title: m.title,
    materialType: m.materialType,
    updatedAt: m.updatedAt,
  }));

  // Recent activity (top 5, mix of quizzes and materials)
  const activity: DashboardStats['recentActivity'] = [];

  // Add recent quizzes
  for (const h of history.slice(0, 3)) {
    activity.push({
      id: h.id,
      title: `Completed ${h.mcqSetTitle}`,
      time: formatRelativeTime(h.completedAt ?? h.startedAt),
      score: Math.round(h.percentage),
      type: 'quiz',
    });
  }

  // Add recent materials
  for (const m of materials.slice(0, 2)) {
    // Don't duplicate if already in activity
    if (!activity.some((a) => a.title.includes(m.title))) {
      activity.push({
        id: m.id,
        title: `Added ${m.title}`,
        time: formatRelativeTime(m.updatedAt),
        score: null,
        type: 'material',
      });
    }
  }

  // Sort by time (most recent first) and limit to 5
  activity.sort((a, b) => {
    // Simple sort by checking "ago" patterns
    const getPriority = (t: string) => {
      if (t === 'Just now') return 0;
      if (t.includes('m ago')) return 1;
      if (t.includes('h ago')) return 2;
      if (t === 'Yesterday') return 3;
      return 4;
    };
    return getPriority(a.time) - getPriority(b.time);
  });

  return {
    totalMaterials,
    totalQuizzes,
    averageScore,
    totalQuestionsAnswered,
    recentMaterials,
    recentActivity: activity.slice(0, 5),
    continueLearning,
    weakAreas,
  };
}
