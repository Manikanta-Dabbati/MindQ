export interface Plan {
  id: number;
  code: string;
  displayName: string;
  description: string;
  storageLimitBytes: number;
  dailyAiGenerations: number;
  maxQuestionsPerGeneration: number;
  advancedModels: boolean;
  aiTutor: boolean;
  exportFormats: boolean;
  prioritySupport: boolean;
  priceInPaise: number;
}

export interface Subscription {
  subscriptionId: number | null;
  plan: Plan;
  status: string;
  startDate: string | null;
  endDate: string | null;
  cancelledAt: string | null;
}
