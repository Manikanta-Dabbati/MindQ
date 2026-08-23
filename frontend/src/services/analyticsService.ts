import api from './api';
import type { AnalyticsOverview } from '../types/analytics';

export async function getAnalyticsOverview(): Promise<AnalyticsOverview> {
  const res = await api.get<{ data: AnalyticsOverview }>('/analytics/overview');
  return res.data.data;
}
