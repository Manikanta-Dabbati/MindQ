import api from "./api";
import type { ApiResponse } from "../types/auth";

export interface StorageInfo {
  usedBytes: number;
  limitBytes: number;
  remainingBytes: number;
  usedPercentage: number;
  maxFileSizeBytes: number;
}

export async function getStorageInfo(): Promise<StorageInfo> {
  const response = await api.get<ApiResponse<StorageInfo>>("/materials/storage");
  return response.data.data;
}
