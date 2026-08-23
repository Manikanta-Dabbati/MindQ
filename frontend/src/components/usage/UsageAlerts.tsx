import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { HardDrive, Zap, ArrowRight } from "lucide-react";
import { getStorageInfo, type StorageInfo } from "../../services/storageService";
import { getCurrentSubscription, formatStorage } from "../../services/subscriptionService";
import type { Plan } from "../../types/subscription";

export function StorageUsageAlert() {
  const [storage, setStorage] = useState<StorageInfo | null>(null);
  const [plan, setPlan] = useState<Plan | null>(null);

  useEffect(() => {
    Promise.all([getStorageInfo(), getCurrentSubscription().catch(() => null)])
      .then(([s, sub]) => { setStorage(s); setPlan(sub?.plan ?? null); })
      .catch(() => {});
  }, []);

  if (!storage || !plan) return null;
  const pct = storage.usedPercentage;
  const isHigh = pct >= 80;
  const isCritical = pct >= 95;
  const isFree = plan.code === "FREE";
  if (!isHigh) return null;

  return (
    <div className={"rounded-xl border p-4 " + (isCritical ? "border-red-200 bg-red-50 dark:border-red-800 dark:bg-red-900/20" : "border-amber-200 bg-amber-50 dark:border-amber-800 dark:bg-amber-900/20")}>
      <div className="flex items-start gap-3">
        <div className={"flex h-10 w-10 shrink-0 items-center justify-center rounded-xl " + (isCritical ? "bg-red-100 text-red-600 dark:bg-red-900/40 dark:text-red-400" : "bg-amber-100 text-amber-600 dark:bg-amber-900/40 dark:text-amber-400")}>
          <HardDrive size={20} />
        </div>
        <div className="flex-1">
          <p className="text-sm font-semibold text-slate-900 dark:text-white">{isCritical ? "Storage almost full" : "Storage running low"}</p>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">You have used {formatStorage(storage.usedBytes)} of {formatStorage(storage.limitBytes)} ({Math.round(pct)}%){isFree ? ". Upgrade your plan for more storage." : ""}</p>
          <div className="mt-3 h-2 overflow-hidden rounded-full bg-slate-200 dark:bg-slate-700">
            <div className={"h-full rounded-full transition-all " + (isCritical ? "bg-red-500" : "bg-amber-500")} style={{ width: pct + "%" }} />
          </div>
          {isFree && (<Link to="/subscription" className="mt-3 inline-flex items-center gap-1.5 text-sm font-medium text-blue-600 hover:text-blue-700 dark:text-blue-400">Upgrade Plan <ArrowRight size={14} /></Link>)}
        </div>
      </div>
    </div>
  );
}

export function GenerationLimitAlert({ className }: { className?: string }) {
  const [plan, setPlan] = useState<Plan | null>(null);

  useEffect(() => {
    getCurrentSubscription().then((sub: any) => setPlan(sub?.plan ?? null)).catch(() => {});
  }, []);

  if (!plan) return null;
  const isFree = plan.code === "FREE";
  const isLowLimit = plan.dailyAiGenerations <= 20;

  if (!isFree || !isLowLimit) return null;

  return (
    <div className={"rounded-xl border border-blue-200 bg-blue-50 p-3 dark:border-blue-800 dark:bg-blue-900/20 " + (className ?? "")}>
      <div className="flex items-center gap-3">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-blue-100 text-blue-600 dark:bg-blue-900/40 dark:text-blue-400">
          <Zap size={16} />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-slate-900 dark:text-white">{plan.dailyAiGenerations} AI generations per day</p>
          <p className="text-xs text-slate-500 dark:text-slate-400">Upgrade for more daily generations and advanced models.</p>
        </div>
        <Link to="/subscription" className="shrink-0 inline-flex items-center gap-1 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-blue-700">
          Upgrade
        </Link>
      </div>
    </div>
  );
}
