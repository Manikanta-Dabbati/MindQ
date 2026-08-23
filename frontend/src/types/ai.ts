export interface AIModel {
  id: number;
  name: string;
  modelCode: string;
  provider: string;
  isActive: boolean;
  isDefault: boolean;
  maxTokens: number;
}
