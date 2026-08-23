import api from './api';
import type { AdminDashboardStats, AdminUser } from '../types/admin';

export async function getDashboardStats(): Promise<AdminDashboardStats> {
  const res = await api.get<{ data: AdminDashboardStats }>('/admin/dashboard');
  return res.data.data;
}

export async function getAllUsers(): Promise<AdminUser[]> {
  const res = await api.get<{ data: AdminUser[] }>('/admin/users');
  return res.data.data;
}

export async function updateUserStatus(userId: number, status: string): Promise<void> {
  await api.put(`/admin/users/${userId}/status`, { status });
}

export async function updateUserRole(userId: number, role: string): Promise<void> {
  await api.put(`/admin/users/${userId}/role`, { role });
}

export function formatStorage(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(0)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`;
}


export async function deleteUser(userId: number): Promise<void> {
  await api.delete(`/admin/users/${userId}`);
}
