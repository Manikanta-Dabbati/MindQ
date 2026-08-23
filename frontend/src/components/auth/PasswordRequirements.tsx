import { Check, X } from "lucide-react";

interface PasswordRequirementsProps {
  password: string;
}

interface Requirement {
  label: string;
  test: (pw: string) => boolean;
}

const requirements: Requirement[] = [
  { label: "At least 8 characters", test: (pw) => pw.length >= 8 },
  { label: "One uppercase letter", test: (pw) => /[A-Z]/.test(pw) },
  { label: "One lowercase letter", test: (pw) => /[a-z]/.test(pw) },
  { label: "One number", test: (pw) => /[0-9]/.test(pw) },
  { label: "One special character (!@#$%^&* etc.)", test: (pw) => /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?`]/.test(pw) },
];

export default function PasswordRequirements({
  password,
}: PasswordRequirementsProps) {
  if (!password) return null;

  return (
    <div className="mt-2 space-y-1">
      {requirements.map((req) => {
        const met = req.test(password);
        return (
          <div key={req.label} className="flex items-center gap-2 text-xs">
            {met ? (
              <Check size={13} className="shrink-0 text-[var(--mq-success)]" />
            ) : (
              <X size={13} className="shrink-0 text-[var(--mq-error)]" />
            )}
            <span
              className={
                met ? "text-[var(--mq-success)]" : "text-[var(--mq-text-secondary)]"
              }
            >
              {req.label}
            </span>
          </div>
        );
      })}
    </div>
  );
}

export function isPasswordValid(password: string): boolean {
  return requirements.every((req) => req.test(password));
}
