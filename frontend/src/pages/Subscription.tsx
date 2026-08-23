import { useState, useEffect, useRef } from "react";
import { CreditCard, Check, Loader2, AlertCircle, RotateCcw } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../components/ui";
import { getAllPlans, getCurrentSubscription, formatStorage, getStorageInfo } from "../services/subscriptionService";
import { createCheckout, getPaymentHistory, loadRazorpayScript, openRazorpayCheckout, verifyPayment, formatCurrency, formatPaymentStatus } from "../services/paymentService";
import type { Plan, Subscription } from "../types/subscription";
import type { PaymentHistory } from "../types/payment";

export default function SubscriptionPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [plans, setPlans] = useState<Plan[]>([]);
  const [subscription, setSubscription] = useState<Subscription | null>(null);
  const [paymentHistory, setPaymentHistory] = useState<PaymentHistory[]>([]);
  const [paymentHistoryLoading, setPaymentHistoryLoading] = useState(false);
  const [paymentHistoryError, setPaymentHistoryError] = useState(false);
  const [storageInfo, setStorageInfo] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [upgrading, setUpgrading] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [failedPlan, setFailedPlan] = useState<string | null>(null);
  const errorTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => { loadData(); }, []);

  // Auto-dismiss error after 10 seconds
  useEffect(() => {
    if (error) {
      if (errorTimerRef.current) clearTimeout(errorTimerRef.current);
      errorTimerRef.current = setTimeout(() => {
        setError(null);
        setFailedPlan(null);
      }, 10000);
    }
    return () => {
      if (errorTimerRef.current) clearTimeout(errorTimerRef.current);
    };
  }, [error]);

  async function loadData() {
    try {
      setLoading(true);
      const [plansData, subData, storageData] = await Promise.all([
        getAllPlans(),
        getCurrentSubscription(),
        getStorageInfo(),
      ]);
      setPlans(plansData);
      setSubscription(subData);
      setStorageInfo(storageData);
      // Load payment history separately for better UX
      loadPaymentHistory();
    } catch (err) {
      setError("Failed to load subscription data");
    } finally {
      setLoading(false);
    }
  }

  async function loadPaymentHistory() {
    try {
      setPaymentHistoryLoading(true);
      setPaymentHistoryError(false);
      const history = await getPaymentHistory();
      setPaymentHistory(history);
    } catch {
      setPaymentHistoryError(true);
    } finally {
      setPaymentHistoryLoading(false);
    }
  }

  async function handleUpgrade(planCode: string) {
    try {
      setUpgrading(planCode);
      setError(null);
      
      // Load Razorpay script
      const scriptLoaded = await loadRazorpayScript();
      if (!scriptLoaded) {
        setError("Could not load payment gateway. Please try again.");
        setUpgrading(null);
        return;
      }

      // Create order via backend (backend validates plan and amount)
      const checkout = await createCheckout(planCode, "MONTHLY");

      // Dev mode: detect mock orders when Razorpay is disabled in backend
      if (checkout.razorpayKey === "rzp_test_placeholder" || checkout.razorpayKey === "placeholder_key") {
        try {
          const apiMod = await import("../services/api");
          const apiClient = apiMod.default;
          await apiClient.post("/payment/test-confirm?orderId=" + checkout.orderId);
          setUpgrading("processing");
          await new Promise(r => setTimeout(r, 1000));
          await loadData();
          toast.success("Upgrade successful! Your new plan is now active.");
        } catch {
          setError("Test payment simulation failed");
          setFailedPlan(planCode);
          setUpgrading(null);
        }
        return;
      }

      // Real Razorpay flow
      openRazorpayCheckout({
        orderId: checkout.orderId,
        amount: checkout.amountPaise,
        currency: checkout.currency,
        razorpayKey: checkout.razorpayKey,
        userName: user?.fullName,
        userEmail: user?.email,
        onSuccess: async (pid: string, oid: string, sig: string) => {
          setUpgrading("processing");
          try {
            // Verify signature on backend
            await verifyPayment(oid, pid, sig);
            // Wait for subscription to propagate
            await new Promise(r => setTimeout(r, 1000));
            await loadData();
            toast.success("Upgrade successful! Your new plan is now active.");
          } catch (err) {
            setError("Payment verification failed. Please contact support or try again.");
            setFailedPlan(planCode);
            setUpgrading(null);
          }
        },
        onFailure: (e) => {
          setError(e.description || "Payment could not be completed. Your account has not been charged.");
          setFailedPlan(planCode);
          setUpgrading(null);
        },
        onDismiss: () => {
          setError("Payment cancelled. You can try again whenever you're ready.");
          setFailedPlan(planCode);
          setUpgrading(null);
        },
      });
    } catch (err: any) {
      // Handle backend errors (e.g., payment disabled, invalid plan)
      const message = err.response?.data?.message || err.message || "Payment failed. Please try again.";
      setError(message);
      setFailedPlan(planCode);
      setUpgrading(null);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
      </div>
    );
  }

  const currentPlan = subscription?.plan || plans.find(p => p.code === "FREE");
  const usedStorage = storageInfo ? formatStorage(storageInfo.usedBytes) : "0 MB";
  const limitStorage = storageInfo ? formatStorage(storageInfo.limitBytes) : "500 MB";

  return (
    <div className="max-w-5xl mx-auto space-y-8 pb-12">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Subscription & Billing</h1>
        <p className="text-slate-500 dark:text-slate-400 mt-1">Manage your plan and billing</p>
      </div>

      {error && (
        <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
          <div className="flex items-center gap-3">
            <AlertCircle className="h-5 w-5 text-red-600 dark:text-red-400 flex-shrink-0" />
            <span className="text-red-700 dark:text-red-300 flex-1">{error}</span>
          </div>
          {failedPlan && (
            <div className="mt-3 flex gap-2">
              <button
                onClick={() => { setError(null); setFailedPlan(null); handleUpgrade(failedPlan); }}
                className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-sm font-medium rounded-lg transition-colors flex items-center gap-2"
              >
                <RotateCcw className="h-4 w-4" />
                Try Again
              </button>
              <button
                onClick={() => { setError(null); setFailedPlan(null); }}
                className="px-4 py-2 bg-white dark:bg-slate-700 hover:bg-slate-50 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-300 text-sm font-medium rounded-lg border border-slate-300 dark:border-slate-600 transition-colors"
              >
                Dismiss
              </button>
            </div>
          )}
        </div>
      )}

      {currentPlan && (
        <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 p-6">
          <h2 className="text-lg font-semibold text-slate-900 dark:text-white mb-4">Current Plan</h2>
          <div className="flex items-start justify-between">
            <div>
              <div className="flex items-center gap-3">
                <span className="text-2xl font-bold text-blue-600">{currentPlan.displayName}</span>
                <span className="px-2 py-1 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400 text-xs font-medium rounded-full">Active</span>
              </div>
              <p className="text-slate-500 dark:text-slate-400 mt-1">{currentPlan.description}</p>
            </div>
          </div>
          <div className="mt-6 grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="text-center p-3 bg-slate-50 dark:bg-slate-700/50 rounded-lg">
              <div className="text-2xl font-bold text-slate-900 dark:text-white">{usedStorage}</div>
              <div className="text-xs text-slate-500 dark:text-slate-400">of {limitStorage} Storage</div>
            </div>
            <div className="text-center p-3 bg-slate-50 dark:bg-slate-700/50 rounded-lg">
              <div className="text-2xl font-bold text-slate-900 dark:text-white">{currentPlan.dailyAiGenerations}</div>
              <div className="text-xs text-slate-500 dark:text-slate-400">AI Generations/Day</div>
            </div>
            <div className="text-center p-3 bg-slate-50 dark:bg-slate-700/50 rounded-lg">
              <div className="text-2xl font-bold text-slate-900 dark:text-white">{currentPlan.maxQuestionsPerGeneration}</div>
              <div className="text-xs text-slate-500 dark:text-slate-400">Questions/Generation</div>
            </div>
            <div className="text-center p-3 bg-slate-50 dark:bg-slate-700/50 rounded-lg">
              <div className="text-2xl font-bold text-slate-900 dark:text-white">{currentPlan.advancedModels ? "Yes" : "No"}</div>
              <div className="text-xs text-slate-500 dark:text-slate-400">Advanced Models</div>
            </div>
          </div>
        </div>
      )}

      <div>
        <h2 className="text-lg font-semibold text-slate-900 dark:text-white mb-4">Available Plans</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {plans.map((plan) => {
            const isCurrent = plan.code === currentPlan?.code;
            const isFree = plan.code === "FREE";
            return (
              <div key={plan.id} className={"bg-white dark:bg-slate-800 rounded-xl border p-6 flex flex-col " + (isCurrent ? "border-blue-500 ring-1 ring-blue-500/20" : "border-slate-200 dark:border-slate-700")}>
                <div className="flex items-center justify-between">
                  <h3 className="text-xl font-bold text-slate-900 dark:text-white">{plan.displayName}</h3>
                  {isCurrent && <span className="px-2 py-1 bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400 text-xs font-medium rounded-full">Current</span>}
                </div>
                <p className="text-slate-500 dark:text-slate-400 mt-2 flex-1">{plan.description}</p>
                <div className="mt-6 space-y-3">
                  <div className="text-sm text-slate-600 dark:text-slate-300">
                    <Check className="h-4 w-4 text-green-500 inline mr-2" />
                    {formatStorage(plan.storageLimitBytes)} Storage
                  </div>
                  <div className="text-sm text-slate-600 dark:text-slate-300">
                    <Check className="h-4 w-4 text-green-500 inline mr-2" />
                    {plan.dailyAiGenerations} AI Generations/Day
                  </div>
                  <div className="text-sm text-slate-600 dark:text-slate-300">
                    <Check className="h-4 w-4 text-green-500 inline mr-2" />
                    {plan.maxQuestionsPerGeneration} Questions/Generation
                  </div>
                  {plan.advancedModels && (
                    <div className="text-sm text-slate-600 dark:text-slate-300">
                      <Check className="h-4 w-4 text-green-500 inline mr-2" />Advanced AI Models
                    </div>
                  )}
                </div>
                <div className="mt-6">
                  {isCurrent ? (
                    <div className="w-full py-2 px-4 bg-slate-100 dark:bg-slate-700 text-slate-500 dark:text-slate-400 rounded-lg text-center font-medium">Current Plan</div>
                  ) : isFree ? (
                    <div className="w-full py-2 px-4 bg-slate-100 dark:bg-slate-700 text-slate-500 dark:text-slate-400 rounded-lg text-center font-medium">Included</div>
                  ) : (
                    <button onClick={() => handleUpgrade(plan.code)} disabled={upgrading !== null} className="w-full py-2 px-4 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white rounded-lg font-medium transition-colors flex items-center justify-center gap-2">
                      {upgrading === plan.code ? (<><Loader2 className="h-4 w-4 animate-spin" />Creating order...</>)
                      : upgrading === "processing" ? (<><Loader2 className="h-4 w-4 animate-spin" />Processing...</>)
                      : ("Upgrade to " + plan.displayName)}
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 p-6">
        <h2 className="text-lg font-semibold text-slate-900 dark:text-white mb-4">Billing History</h2>
        {paymentHistoryLoading ? (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="h-6 w-6 animate-spin text-slate-400" />
          </div>
        ) : paymentHistoryError ? (
          <div className="text-center py-8">
            <AlertCircle className="h-12 w-12 mx-auto mb-3 text-red-400" />
            <p className="text-red-600 dark:text-red-400">We couldn't load your billing history.</p>
            <button
              onClick={loadPaymentHistory}
              className="mt-3 px-4 py-2 bg-slate-100 dark:bg-slate-700 hover:bg-slate-200 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-300 text-sm font-medium rounded-lg transition-colors"
            >
              Try Again
            </button>
          </div>
        ) : paymentHistory.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead><tr className="text-left text-slate-500 dark:text-slate-400 border-b border-slate-200 dark:border-slate-700">
                <th className="pb-3 font-medium">Date</th>
                <th className="pb-3 font-medium">Plan</th>
                <th className="pb-3 font-medium">Amount</th>
                <th className="pb-3 font-medium">Status</th>
                <th className="pb-3 font-medium">Payment ID</th>
              </tr></thead>
              <tbody>
                {paymentHistory.map((p) => {
                  const si = formatPaymentStatus(p.status);
                  return (
                    <tr key={p.id} className="border-b border-slate-100 dark:border-slate-700/50">
                      <td className="py-3 text-slate-900 dark:text-white">{new Date(p.createdAt).toLocaleDateString()}</td>
                      <td className="py-3 text-slate-600 dark:text-slate-300">{p.planCode}</td>
                      <td className="py-3 text-slate-900 dark:text-white">{formatCurrency(p.amountPaise)}</td>
                      <td className="py-3"><span className={si.color + " font-medium"}>{si.label}</span></td>
                      <td className="py-3 text-slate-500 dark:text-slate-400 font-mono text-xs">{p.razorpayPaymentId || "-"}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="text-center py-8 text-slate-500 dark:text-slate-400">
            <CreditCard className="h-12 w-12 mx-auto mb-3 opacity-50" />
            <p>No billing activity yet.</p>
          </div>
        )}
      </div>
    </div>
  );
}
