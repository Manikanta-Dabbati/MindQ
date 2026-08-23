import { useEffect, useRef, useState } from "react";

interface OtpInputProps {
  length?: number;
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  autoFocus?: boolean;
}

export default function OtpInput({
  length = 6,
  value,
  onChange,
  disabled = false,
  autoFocus = true,
}: OtpInputProps) {
  const [digits, setDigits] = useState<string[]>(
    Array(length).fill("")
  );
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  // Sync external value to internal state
  useEffect(() => {
    const newDigits = Array(length).fill("");
    for (let i = 0; i < Math.min(value.length, length); i++) {
      newDigits[i] = value[i];
    }
    setDigits(newDigits);
  }, [value, length]);

  // Auto-focus first input
  useEffect(() => {
    if (autoFocus && inputRefs.current[0]) {
      inputRefs.current[0].focus();
    }
  }, [autoFocus]);

  const handleChange = (index: number, inputValue: string) => {
    // Only allow digits
    const digit = inputValue.replace(/[^0-9]/g, "").slice(-1);

    const newDigits = [...digits];
    newDigits[index] = digit;
    setDigits(newDigits);

    // Notify parent
    const newValue = newDigits.join("");
    onChange(newValue);

    // Auto-advance to next input
    if (digit && index < length - 1) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (
    index: number,
    e: React.KeyboardEvent<HTMLInputElement>
  ) => {
    if (e.key === "Backspace" && !digits[index] && index > 0) {
      // Move to previous input on backspace if current is empty
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (e: React.ClipboardEvent) => {
    e.preventDefault();
    const pastedData = e.clipboardData
      .getData("text")
      .replace(/[^0-9]/g, "")
      .slice(0, length);

    const newDigits = Array(length).fill("");
    for (let i = 0; i < pastedData.length; i++) {
      newDigits[i] = pastedData[i];
    }
    setDigits(newDigits);
    onChange(newDigits.join(""));

    // Focus last filled input or next empty one
    const focusIndex = Math.min(pastedData.length, length - 1);
    inputRefs.current[focusIndex]?.focus();
  };

  return (
    <div className="flex justify-center gap-3">
      {digits.map((digit, index) => (
        <input
          key={index}
          ref={(el) => {
            inputRefs.current[index] = el;
          }}
          type="text"
          inputMode="numeric"
          maxLength={1}
          value={digit}
          onChange={(e) => handleChange(index, e.target.value)}
          onKeyDown={(e) => handleKeyDown(index, e)}
          onPaste={handlePaste}
          disabled={disabled}
          className="h-14 w-12 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] text-center text-xl font-semibold text-[var(--mq-text)] outline-none transition focus:border-[var(--mq-primary)] focus:ring-4 focus:ring-[var(--mq-primary)]/10 disabled:opacity-50"
          aria-label={`Digit ${index + 1}`}
        />
      ))}
    </div>
  );
}
