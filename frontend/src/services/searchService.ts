import api from "./api";

export interface SearchResult {
  id: number;
  type: "MATERIAL" | "QUIZ" | "ATTEMPT" | "GENERATION";
  title: string;
  subtitle: string;
  link: string;
  icon: "file" | "quiz" | "attempt" | "generation";
}

export async function globalSearch(query: string): Promise<SearchResult[]> {
  if (!query.trim()) return [];
  const res = await api.get<{ data: SearchResult[] }>("/search", {
    params: { q: query.trim() },
  });
  return res.data.data;
}
