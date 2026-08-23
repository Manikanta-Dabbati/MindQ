import { HardDrive } from "lucide-react";
import { useEffect, useState } from "react";
import * as storageService from "../../services/storageService";

function formatBytes(bytes: number): string {
  if (bytes < 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export default function StorageBar() {
  const [info, setInfo] = useState<storageService.StorageInfo | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    storageService.getStorageInfo().then(setInfo).finally(() => setLoading(false));
  }, []);

  if (loading) return null;
  if (!info) return null;

  const pct = Math.min(100, Math.round(info.usedPercentage));
  const isWarning = pct > 80;
  const isCritical = pct > 95;

  return (
    <div className="rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] p-4" role="status" aria-live="polite">
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[var(--mq-bg)] text-[var(--mq-primary)]">
          <HardDrive size={19} />
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-xs font-semibold text-[var(--mq-text-secondary)]">Knowledge Vault Storage</p>
          <p className="mt-0.5 text-sm font-semibold text-[var(--mq-text)]">
            {formatBytes(info.usedBytes)} / {formatBytes(info.limitBytes)}
          </p>
        </div>
        <p className={`text-xs font-semibold ${isCritical ? "text-[var(--mq-error)]" : isWarning ? "text-[var(--mq-warning)]" : "text-[var(--mq-text-secondary)]"}`}>
          {pct}% used
        </p>
      </div>
      <div className="mt-3">
        <div className="h-2 overflow-hidden rounded-full bg-[var(--mq-border)]">
          <div
            className={`h-full rounded-full transition-all duration-300 ${isCritical ? "bg-[var(--mq-error-light)]0" : isWarning ? "bg-orange-400" : "bg-[var(--mq-primary)]"}`}
            style={{ width: `${pct}%` }}
          />
        </div>
        <p className="mt-1.5 text-xs text-[var(--mq-text-muted)]">{formatBytes(info.remainingBytes)} remaining</p>
      </div>
    </div>
  );
}
