import api from './api';
import type { Plan, Subscription } from '../types/subscription';

export async function getAllPlans(): Promise<Plan[]> {
  const res = await api.get<{ data: Plan[] }>('/subscription/plans');
  return res.data.data;
}

export async function getCurrentSubscription(): Promise<Subscription> {
  const res = await api.get<{ data: Subscription }>('/subscription/current');
  return res.data.data;
}

export function formatStorage(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(0)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`;
}

import type { StorageInfo } from './storageService';

export async function getStorageInfo(): Promise<StorageInfo> {
  const res = await api.get<{ data: StorageInfo }>('/materials/storage');
  return res.data.data;
}
