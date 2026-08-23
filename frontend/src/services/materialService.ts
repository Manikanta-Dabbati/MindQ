import api from "./api";
import type { ApiResponse } from "../types/auth";
import type { MaterialDetail, MaterialSummary, PaginatedResponse } from "../types/material";

export async function listMaterials(
  page = 0,
  size = 20,
  search?: string,
): Promise<PaginatedResponse<MaterialSummary>> {
  const params: Record<string, string | number> = { page, size };
  if (search) params.search = search;
  const response = await api.get<ApiResponse<PaginatedResponse<MaterialSummary>>>("/materials", { params });
  return response.data.data;
}

export async function getMaterial(id: number): Promise<MaterialDetail> {
  const response = await api.get<ApiResponse<MaterialDetail>>(
    `/materials/${id}`,
  );
  return response.data.data;
}

export async function createMaterial(
  title: string,
  content: string,
): Promise<MaterialDetail> {
  const response = await api.post<ApiResponse<MaterialDetail>>("/materials", {
    title,
    content,
  });
  return response.data.data;
}

export async function uploadMaterial(
  file: File,
  title?: string,
): Promise<MaterialDetail> {
  const formData = new FormData();
  formData.append("file", file);
  if (title) {
    formData.append("title", title);
  }

  const response = await api.post<ApiResponse<MaterialDetail>>(
    "/materials/upload",
    formData,
    {
      timeout: 120000,
    },
  );
  return response.data.data;
}

export async function deleteMaterial(id: number): Promise<void> {
  await api.delete(`/materials/${id}`);
}
