import {
  AlertCircle,
  CheckCircle2,
  FileText,
  Loader2,
  Upload,
  X,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { uploadMaterial } from "../../services/materialService";
import * as storageService from "../../services/storageService";
import { useToast, FocusTrap } from "../ui";

interface UploadMaterialModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

const ALLOWED_EXTENSIONS = [".pdf", ".docx"];
const ALLOWED_MIME_TYPES = [
  "application/pdf",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
];

type UploadState = "idle" | "uploading" | "success";

function formatBytes(bytes: number): string {
  if (bytes === 0) return "0 B";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function validateFile(
  file: File,
  maxFileSize: number,
  remainingStorage: number,
): string | null {
  const lowerName = file.name.toLowerCase();
  const hasValidExtension = ALLOWED_EXTENSIONS.some((ext) => lowerName.endsWith(ext));
  if (!hasValidExtension) {
    return "Supported formats: PDF and DOCX only.";
  }
  if (file.type && !ALLOWED_MIME_TYPES.includes(file.type) && file.type !== "application/octet-stream") {
    return "Supported formats: PDF and DOCX only.";
  }
  if (file.size > maxFileSize) {
    return `This file is larger than the maximum allowed size of ${formatBytes(maxFileSize)}.`;
  }
  if (file.size > remainingStorage) {
    return `You have only ${formatBytes(remainingStorage)} of storage remaining. Free up space or delete existing materials.`;
  }
  return null;
}

export default function UploadMaterialModal({ open, onClose, onSuccess }: UploadMaterialModalProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const toast = useToast();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [title, setTitle] = useState("");
  const [isDragging, setIsDragging] = useState(false);
  const [uploadState, setUploadState] = useState<UploadState>("idle");
  const [uploadError, setUploadError] = useState("");
  const [maxFileSize, setMaxFileSize] = useState(10 * 1024 * 1024);
  const [remainingStorage, setRemainingStorage] = useState(500 * 1024 * 1024);
  const [storageLoading, setStorageLoading] = useState(true);

  useEffect(() => {
    if (!open) return;
    setStorageLoading(true);
    storageService.getStorageInfo().then((info) => {
      setMaxFileSize(info.maxFileSizeBytes);
      setRemainingStorage(info.remainingBytes);
    }).catch(() => {}).finally(() => setStorageLoading(false));
  }, [open]);

  if (!open) return null;

  const handleFile = (file: File | undefined) => {
    if (!file) return;
    const error = validateFile(file, maxFileSize, remainingStorage);
    if (error) { setUploadError(error); return; }
    setUploadError("");
    setSelectedFile(file);
  };

  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    handleFile(event.target.files?.[0]);
  };

  const handleDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setIsDragging(false);
    handleFile(event.dataTransfer.files?.[0]);
  };

  const handleRemoveFile = () => {
    setSelectedFile(null);
    setUploadError("");
    if (inputRef.current) inputRef.current.value = "";
  };

  const handleClose = () => {
    if (uploadState === "uploading") return;
    setSelectedFile(null); setTitle(""); setIsDragging(false);
    setUploadError(""); setUploadState("idle");
    if (inputRef.current) inputRef.current.value = "";
    onClose();
  };

  const handleUpload = async () => {
    if (!selectedFile) return;
    setUploadState("uploading");
    setUploadError("");
    try {
      await uploadMaterial(selectedFile, title || undefined);
      setUploadState("success");
      toast.success("Material uploaded successfully");
      setTimeout(() => { handleClose(); onSuccess?.(); }, 1200);
    } catch (err: unknown) {
      setUploadState("idle");
      if (typeof err === "object" && err !== null && "response" in err) {
        const axiosErr = err as { response?: { status?: number; data?: { message?: string } } };
        const status = axiosErr.response?.status;
        const message = axiosErr.response?.data?.message;
        if (status === 413) setUploadError(`File is too large. Maximum size is ${formatBytes(maxFileSize)}.`);
        else if (status === 400 && message?.includes("Storage limit")) setUploadError("You've reached your storage limit. Delete some materials to free up space.");
        else if (status === 400 && message?.includes("Only PDF")) setUploadError("Supported formats: PDF and DOCX only.");
        else if (status === 400 && message?.includes("No text")) setUploadError("We couldn't extract text from this file. Please try a different document.");
        else if (status === 429) setUploadError("Too many upload requests. Please wait a moment and try again.");
        else if (status === 401 || status === 403) setUploadError("Your session has expired. Please log in again.");
        else setUploadError(message || "We couldn't process this file. Please try again.");
      } else {
        setUploadError("We couldn't connect to the server. Please check your connection and try again.");
      }
    }
  };

  const isUploading = uploadState === "uploading";
  const isSuccess = uploadState === "success";

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/30 px-4 backdrop-blur-[2px]" role="dialog" aria-modal="true" aria-label="Upload material" onMouseDown={(e) => { if (e.target === e.currentTarget) handleClose(); }}>
      <FocusTrap onEscape={handleClose}>
      <div className="w-full max-w-lg overflow-hidden rounded-2xl border border-[var(--mq-border)] bg-[var(--mq-surface)] shadow-[0_20px_60px_rgba(0, 0, 0, 0.15)]">
        <div className="flex items-start justify-between border-b border-[var(--mq-border)] px-6 py-5">
          <div>
            <h2 className="text-lg font-semibold text-[var(--mq-text)]">Upload Material</h2>
            <p className="mt-1 text-sm text-[var(--mq-text-secondary)]">Add a learning resource to your Knowledge Vault.</p>
          </div>
          <button onClick={handleClose} disabled={isUploading} className="rounded-lg p-2 text-[var(--mq-text-muted)] transition hover:bg-[var(--mq-bg)] hover:text-[var(--mq-text)] disabled:opacity-40" aria-label="Close upload modal"><X size={19} /></button>
        </div>

        <div className="p-6">
          {!storageLoading && (
            <div className="mb-4 flex flex-wrap gap-x-4 gap-y-1 rounded-xl bg-[var(--mq-bg)] px-4 py-2.5 text-xs text-[var(--mq-text-secondary)]">
              <span><span className="font-medium text-[var(--mq-text)]">Formats:</span> PDF, DOCX</span>
              <span><span className="font-medium text-[var(--mq-text)]">Max size:</span> {formatBytes(maxFileSize)}</span>
              <span><span className="font-medium text-[var(--mq-text)]">Storage:</span> {formatBytes(remainingStorage)} remaining</span>
            </div>
          )}

          {isSuccess ? (
            <div className="flex flex-col items-center py-8">
              <div className="flex h-14 w-14 items-center justify-center rounded-full bg-emerald-50 text-emerald-500"><CheckCircle2 size={28} /></div>
              <p className="mt-4 text-sm font-semibold text-[var(--mq-text)]">Upload complete</p>
              <p className="mt-1 text-xs text-[var(--mq-text-secondary)]">Ready for AI learning</p>
            </div>
          ) : !selectedFile ? (
            <div onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }} onDragLeave={() => setIsDragging(false)} onDrop={handleDrop} onClick={() => inputRef.current?.click()} className={`cursor-pointer rounded-2xl border-2 border-dashed p-10 text-center transition ${isDragging ? "border-[var(--mq-primary)] bg-[var(--mq-primary-light)]" : "border-[var(--mq-text-muted)] bg-[var(--mq-bg)] hover:border-[var(--mq-primary-border)] hover:bg-[var(--mq-primary-light)]/50"}`}>
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-[var(--mq-primary-light)] text-[var(--mq-primary)]"><Upload size={21} /></div>
              <h3 className="mt-4 text-sm font-semibold text-[var(--mq-text)]">Drop your file here</h3>
              <p className="mt-1 text-sm text-[var(--mq-text-secondary)]">or <span className="font-medium text-[var(--mq-primary)]">browse from your computer</span></p>
              <p className="mt-4 text-xs text-[var(--mq-text-muted)]">PDF or DOCX — up to {formatBytes(maxFileSize)}</p>
              <input ref={inputRef} type="file" accept=".pdf,.docx" onChange={handleInputChange} className="hidden" />
            </div>
          ) : (
            <div className="rounded-2xl border border-[var(--mq-primary-border)] bg-[var(--mq-primary-light)]/50 p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[var(--mq-surface)] text-[var(--mq-primary)] shadow-sm"><FileText size={20} /></div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold text-[var(--mq-text)]">{selectedFile.name}</p>
                  <p className="mt-1 text-xs text-[var(--mq-text-secondary)]">{formatBytes(selectedFile.size)}</p>
                </div>
                {!isUploading && <button onClick={handleRemoveFile} className="rounded-lg p-2 text-[var(--mq-text-muted)] hover:bg-[var(--mq-surface)] hover:text-[var(--mq-error)]" aria-label="Remove file"><X size={17} /></button>}
              </div>
              {isUploading ? (
                <div className="mt-3 flex items-center gap-2 text-xs font-medium text-[var(--mq-primary)]"><Loader2 size={14} className="animate-spin" />Uploading and processing...</div>
              ) : (
                <div className="mt-3 flex items-center gap-2 text-xs font-medium text-emerald-600"><CheckCircle2 size={15} />File ready to upload</div>
              )}
            </div>
          )}

          {selectedFile && !isSuccess && (
            <div className="mt-4">
              <label className="mb-2 block text-sm font-medium text-[var(--mq-text)]">Title <span className="text-[var(--mq-text-muted)]">(optional)</span></label>
              <input type="text" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Enter a title for this material" disabled={isUploading} className="h-11 w-full rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 text-sm text-[var(--mq-text)] outline-none transition placeholder:text-[var(--mq-text-secondary)] focus:border-[var(--mq-primary)] focus:ring-4 focus:ring-[var(--mq-primary)]/10 disabled:opacity-60" />
            </div>
          )}

          {uploadError && (
            <div className="mt-3 flex items-start gap-2 rounded-xl border border-[var(--mq-error-border)] bg-[var(--mq-error-light)] px-4 py-3 text-sm text-[var(--mq-error)]">
              <AlertCircle size={16} className="mt-0.5 shrink-0" />
              <span>{uploadError}</span>
            </div>
          )}

          {!selectedFile && !isSuccess && (
            <div className="mt-5 rounded-xl bg-[var(--mq-bg)] px-4 py-3">
              <p className="text-xs leading-5 text-[var(--mq-text-secondary)]">Your material will be stored in your Knowledge Vault and can later be used with MindQ AI for generating practice questions.</p>
            </div>
          )}
        </div>

        {!isSuccess && (
          <div className="flex items-center justify-end gap-3 border-t border-[var(--mq-border)] px-6 py-4">
            <button onClick={handleClose} disabled={isUploading} className="rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 py-2.5 text-sm font-medium text-[var(--mq-text)] transition hover:bg-[var(--mq-bg)] disabled:opacity-60">Cancel</button>
            <button disabled={!selectedFile || isUploading} onClick={handleUpload} className={`inline-flex items-center gap-2 rounded-xl px-5 py-2.5 text-sm font-semibold text-white transition ${selectedFile && !isUploading ? "bg-[var(--mq-primary)] hover:bg-[var(--mq-primary-hover)]" : "cursor-not-allowed bg-[var(--mq-text-muted)]"}`}>
              {isUploading ? <><Loader2 size={16} className="animate-spin" />Uploading...</> : "Upload Material"}
            </button>
          </div>
        )}
      </div>
      </FocusTrap>
    </div>
  );
}
