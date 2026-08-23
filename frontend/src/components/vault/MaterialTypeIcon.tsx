import {
  FileText,
  FileType,
  FolderOpen,
  StickyNote,
} from "lucide-react";

interface MaterialTypeIconProps {
  type: "PDF" | "DOCUMENT" | "NOTE" | "FOLDER";
}

export default function MaterialTypeIcon({
  type,
}: MaterialTypeIconProps) {
  const config = {
    PDF: {
      icon: FileType,
      background: "bg-[var(--mq-error-light)]",
      color: "text-[var(--mq-error)]",
    },
    DOCUMENT: {
      icon: FileText,
      background: "bg-[var(--mq-primary-light)]",
      color: "text-[var(--mq-primary)]",
    },
    NOTE: {
      icon: StickyNote,
      background: "bg-[var(--mq-warning-light)]",
      color: "text-[var(--mq-warning)]",
    },
    FOLDER: {
      icon: FolderOpen,
      background: "bg-indigo-50",
      color: "text-indigo-500",
    },
  };

  const selected = config[type];
  const Icon = selected.icon;

  return (
    <div
      className={`flex h-11 w-11 items-center justify-center rounded-xl ${selected.background} ${selected.color}`}
    >
      <Icon size={20} />
    </div>
  );
}