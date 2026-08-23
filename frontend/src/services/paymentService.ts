import api from './api';
import type { CheckoutResponse, PaymentHistory } from '../types/payment';

export async function createCheckout(planCode: string, billingPeriod: string = 'MONTHLY'): Promise<CheckoutResponse> {
  const res = await api.post<{ data: CheckoutResponse }>('/payment/checkout', { planCode, billingPeriod });
  return res.data.data;
}

export async function getPaymentHistory(): Promise<PaymentHistory[]> {
  const res = await api.get<{ data: PaymentHistory[] }>('/payment/history');
  return res.data.data;
}

export async function verifyPayment(
  razorpayOrderId: string,
  razorpayPaymentId: string,
  razorpaySignature: string
): Promise<{ verified: boolean; status: string }> {
  const res = await api.post<{ data: { verified: boolean; status: string } }>('/payment/verify', {
    razorpayOrderId,
    razorpayPaymentId,
    razorpaySignature,
  });
  return res.data.data;
}

export function loadRazorpayScript(): Promise<boolean> {
  return new Promise((resolve) => {
    if (document.querySelector('script[src="https://checkout.razorpay.com/v1/checkout.js"]')) {
      resolve(true);
      return;
    }
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

export function openRazorpayCheckout(opts: {
  orderId: string;
  amount: number;
  currency: string;
  razorpayKey: string;
  userName?: string;
  userEmail?: string;
  onSuccess: (pid: string, oid: string, sig: string) => void;
  onFailure: (e: { description?: string }) => void;
  onDismiss?: () => void;
}): void {
  const rzp = new window.Razorpay({
    key: opts.razorpayKey,
    amount: opts.amount,
    currency: opts.currency,
    name: 'MindQ',
    description: 'Plan Upgrade',
    order_id: opts.orderId,
    handler: (r: any) => opts.onSuccess(r.razorpay_payment_id, r.razorpay_order_id, r.razorpay_signature),
    prefill: { name: opts.userName || '', email: opts.userEmail || '' },
    theme: { color: '#2563EB' },
    modal: { ondismiss: () => opts.onDismiss?.() },
  });
  rzp.open();
}

export function formatCurrency(amountPaise: number, currency: string = 'INR'): string {
  return currency === 'INR' ? '₹' + (amountPaise / 100).toLocaleString('en-IN') : (amountPaise / 100).toFixed(2) + ' ' + currency;
}

export function formatPaymentStatus(status: string): { label: string; color: string } {
  switch (status.toUpperCase()) {
    case 'SUCCESS': case 'CAPTURED': return { label: 'Paid', color: 'text-[var(--mq-success)]' };
    case 'FAILED': return { label: 'Failed', color: 'text-[var(--mq-error)]' };
    case 'PENDING': return { label: 'Pending', color: 'text-[var(--mq-warning)]' };
    default: return { label: status, color: 'text-[var(--mq-text-secondary)]' };
  }
}
