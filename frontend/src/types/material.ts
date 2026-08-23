export type MaterialType = "TEXT_PASTE" | "PDF_UPLOAD";

export interface MaterialSummary {
  id: number;
  title: string;
  materialType: MaterialType;
  wordCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface MaterialDetail {
  id: number;
  title: string;
  materialType: MaterialType;
  content: string;
  wordCount: number;
  fileName: string | null;
  fileSizeBytes: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
