export interface CheckoutRequest {
  planCode: string;
  billingPeriod: string;
}

export interface CheckoutResponse {
  orderId: string;
  amountPaise: number;
  currency: string;
  razorpayKey: string;
  billingPeriod: string;
}

export interface PaymentHistory {
  id: number;
  planCode: string;
  amountPaise: number;
  currency: string;
  status: string;
  billingPeriod: string;
  createdAt: string;
  razorpayPaymentId?: string;
}

declare global {
  interface Window {
    Razorpay: new (options: any) => { open: () => void };
  }
}
