import { Eye, EyeOff, KeyRound, Loader2, LockKeyhole, Mail, ShieldCheck } from "lucide-react";
import { useState, useEffect, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import * as authService from "../../services/authService";
import { getDevConfig, devAutoVerify } from "../../services/authService";
import OtpInput from "../../components/auth/OtpInput";
import PasswordRequirements from "../../components/auth/PasswordRequirements";
import AuthMobileHeader from "../../components/auth/AuthMobileHeader";

type Step = "email" | "otp" | "password";

const RESEND_COOLDOWN = 60;

export default function ForgotPassword() {
  const navigate = useNavigate();
  const [step, setStep] = useState<Step>("email");
  const [resendCooldown, setResendCooldown] = useState(0);
  const [bypassActive, setBypassActive] = useState(false);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const [email, setEmail] = useState("");

  // Check dev OTP bypass config on mount
  useEffect(() => {
    getDevConfig()
      .then((config) => setBypassActive(config.bypassEnabled))
      .catch(() => {});
  }, []);
  const [otp, setOtp] = useState("");
  const [resetToken, setResetToken] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // ── Cooldown timer ────────────────────────────────────
  useEffect(() => {
    if (resendCooldown <= 0) {
      if (timerRef.current) clearInterval(timerRef.current);
      return;
    }
    timerRef.current = setInterval(() => {
      setResendCooldown((prev) => {
        if (prev <= 1) {
          if (timerRef.current) clearInterval(timerRef.current);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => { if (timerRef.current) clearInterval(timerRef.current); };
  }, [resendCooldown]);

  // Start cooldown when entering OTP step
  useEffect(() => {
    if (step === "otp" && resendCooldown === 0) {
      setResendCooldown(RESEND_COOLDOWN);
    }
  }, [step]);

  const startResendCooldown = () => setResendCooldown(RESEND_COOLDOWN);

  // ── Step 1: Send OTP ────────────────────────────────────
  const handleSendOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      if (bypassActive) {
        // Dev bypass: skip OTP and get reset token directly
        const result = await devAutoVerify(email, "PASSWORD_RESET");
        const data = result.data as { resetToken?: string } | undefined;
        if (data?.resetToken) {
          setResetToken(data.resetToken);
          setStep("password");
          return;
        }
      }
      await authService.forgotPassword(email);
      setStep("otp");
    } catch (err: unknown) {
      const msg =
        typeof err === "object" && err !== null && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;
      setError(msg ?? "Failed to send verification code. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  // ── Step 2: Verify OTP ──────────────────────────────────
  const handleVerifyOtp = async () => {
    if (otp.length !== 6) return;
    setError("");
    setLoading(true);
    try {
      const token = await authService.verifyResetOtp(email, otp);
      setResetToken(token);
      setStep("password");
    } catch (err: unknown) {
      const msg =
        typeof err === "object" && err !== null && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;
      setError(msg ?? "Invalid verification code. Please try again.");
      setOtp("");
    } finally {
      setLoading(false);
    }
  };

  // ── Step 3: Reset Password ───────────────────────────────
  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    if (newPassword !== confirmPassword) {
      setError("Passwords do not match");
      return;
    }
    setLoading(true);
    try {
      await authService.resetPassword(resetToken, newPassword);
      // Show success, then redirect after a short delay
      setStep("email"); // reuse but will show success via a flag
      navigate("/login?reset=success");
    } catch (err: unknown) {
      const msg =
        typeof err === "object" && err !== null && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;
      setError(msg ?? "Failed to reset password. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  // ── Step indicator ───────────────────────────────────────
  const steps: { key: Step; label: string; num: number }[] = [
    { key: "email", label: "Email", num: 1 },
    { key: "otp", label: "Verify", num: 2 },
    { key: "password", label: "New Password", num: 3 },
  ];
  const currentIdx = steps.findIndex((s) => s.key === step);

  return (
    <div className="min-h-screen bg-[var(--mq-bg)] lg:grid lg:grid-cols-2">
      {/* Desktop hero */}
      <div className="relative hidden overflow-hidden bg-gradient-to-br from-[var(--mq-primary)] to-[var(--mq-ai)] p-12 text-white lg:flex lg:flex-col lg:justify-center xl:p-16">
        <div className="absolute -right-24 -top-24 h-72 w-72 rounded-full bg-[var(--mq-surface)]/10" />
        <div className="absolute -bottom-32 -left-24 h-96 w-96 rounded-full bg-[var(--mq-surface)]/10" />
        <div className="relative">
          <div className="text-3xl font-bold">Mind<span className="text-white/80">Q</span></div>
          <p className="mt-1 text-sm text-white/70">Sync Your Mind with AI</p>
          <h1 className="mt-10 text-4xl font-bold leading-tight xl:text-5xl">Reset your password.</h1>
          <p className="mt-6 max-w-md text-base leading-7 text-white/75">
            {step === "email" && "Enter your email and we'll send you a verification code."}
            {step === "otp" && "Enter the 6-digit code we sent to your email."}
            {step === "password" && "Choose a strong password to keep your account secure."}
          </p>
        </div>
      </div>

      {/* Mobile header */}
      <AuthMobileHeader
        title="Reset your password"
        subtitle={
          step === "email"
            ? "Enter your email to receive a verification code."
            : step === "otp"
              ? "Enter the 6-digit code we sent to your email."
              : "Choose a strong password to keep your account secure."
        }
      />

      {/* Form area */}
      <div className="flex min-h-0 items-center justify-center px-5 py-10 sm:px-8 lg:min-h-screen">
        <div className="w-full max-w-md">
          {/* Desktop header */}
          <div className="mb-8 hidden lg:block">
            <div className="mb-5 flex h-11 w-11 items-center justify-center rounded-xl bg-[var(--mq-primary-light)] text-[var(--mq-primary)]">
              <KeyRound size={21} />
            </div>
            <h2 className="text-2xl font-bold tracking-tight text-[var(--mq-text)]">
              {step === "email" && "Forgot your password?"}
              {step === "otp" && "Verify your identity"}
              {step === "password" && "Set your new password"}
            </h2>
            <p className="mt-2 text-sm leading-6 text-[var(--mq-text-secondary)]">
              {step === "email" && "Enter your email and we'll send you a verification code."}
              {step === "otp" && "Enter the 6-digit code we sent to your email."}
              {step === "password" && "Choose a strong password to keep your account secure."}
            </p>
          </div>

          {/* Step progress indicator */}
          <div className="mb-8 hidden lg:flex items-center gap-2">
            {steps.map((s, i) => (
              <div key={s.key} className="flex items-center gap-2">
                <div className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-semibold transition-colors ${
                  i < currentIdx
                    ? "bg-[var(--mq-primary)] text-white"
                    : i === currentIdx
                      ? "bg-[var(--mq-primary)] text-white"
                      : "bg-[var(--mq-border)] text-[var(--mq-text-secondary)]"
                }`}>
                  {i < currentIdx ? "✓" : s.num}
                </div>
                <span className={`text-xs font-medium ${
                  i <= currentIdx ? "text-[var(--mq-text)]" : "text-[var(--mq-text-secondary)]"
                }`}>{s.label}</span>
                {i < steps.length - 1 && (
                  <div className={`mx-1 h-px w-6 ${
                    i < currentIdx ? "bg-[var(--mq-primary)]" : "bg-[var(--mq-border)]"
                  }`} />
                )}
              </div>
            ))}
          </div>

          {/* Error */}
          {error && (
            <div className="mb-5 rounded-xl border border-[var(--mq-error-border)] bg-[var(--mq-error-light)] px-4 py-3 text-sm text-[var(--mq-error)]">
              {error}
            </div>
          )}

          {/* ── Step 1: Email ──────────────────────── */}
          {step === "email" && (
            <form className="space-y-5" onSubmit={handleSendOtp}>
              {bypassActive && (
                <div className="rounded-xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-700">
                  Development mode -- OTP will be bypassed automatically.
                </div>
              )}
              <div>
                <label htmlFor="forgot-email" className="mb-2 block text-sm font-medium text-[var(--mq-text)]">
                  Email address
                </label>
                <div className="relative">
                  <Mail size={18} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)]" />
                  <input
                    id="forgot-email"
                    type="email"
                    placeholder="you@example.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    autoComplete="email"
                    className="h-12 w-full rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] pl-11 pr-4 text-sm text-[var(--mq-text)] placeholder:text-[var(--mq-text-secondary)] outline-none transition focus:border-[var(--mq-primary)] focus:ring-4 focus:ring-[var(--mq-primary)]/10"
                  />
                </div>
              </div>
              <button
                type="submit"
                disabled={loading || !email}
                className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-[var(--mq-primary)] text-sm font-semibold text-white shadow-sm transition hover:bg-[var(--mq-primary-hover)] focus:outline-none focus:ring-4 focus:ring-[var(--mq-primary)]/20 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {loading ? (
                  <><Loader2 size={18} className="animate-spin" /> Sending code...</>
                ) : (
                  "Send Verification Code"
                )}
              </button>
            </form>
          )}

          {/* ── Step 2: OTP ────────────────────────── */}
          {step === "otp" && (
            <div className="space-y-5">
              <div>
                <label className="mb-2 block text-sm font-medium text-[var(--mq-text)]">
                  Enter verification code
                </label>
                <p className="mb-4 text-xs text-[var(--mq-text-secondary)]">
                  We sent a 6-digit code to <span className="font-medium text-[var(--mq-text)]">{email}</span>
                </p>
                <OtpInput value={otp} onChange={setOtp} autoFocus />
              </div>
              <button
                onClick={handleVerifyOtp}
                disabled={loading || otp.length !== 6}
                className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-[var(--mq-primary)] text-sm font-semibold text-white shadow-sm transition hover:bg-[var(--mq-primary-hover)] focus:outline-none focus:ring-4 focus:ring-[var(--mq-primary)]/20 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {loading ? (
                  <><Loader2 size={18} className="animate-spin" /> Verifying...</>
                ) : (
                  <><ShieldCheck size={18} /> Verify Code</>
                )}
              </button>
              <button
                type="button"
                onClick={async () => {
                  if (resendCooldown > 0) return;
                  setError("");
                  setLoading(true);
                  try {
                    await authService.resendOtp(email, "PASSWORD_RESET");
                    startResendCooldown();
                  } catch {
                    setError("Failed to resend code. Please try again.");
                  } finally {
                    setLoading(false);
                  }
                }}
                disabled={loading || resendCooldown > 0}
                className="w-full text-center text-sm font-medium text-[var(--mq-primary)] hover:text-[var(--mq-primary-hover)] disabled:opacity-50"
              >
                {resendCooldown > 0
                  ? `Resend code in ${resendCooldown}s`
                  : "Resend code"}
              </button>
              <button
                type="button"
                onClick={() => { setStep("email"); setOtp(""); setError(""); }}
                className="w-full text-center text-sm text-[var(--mq-text-secondary)] hover:text-[var(--mq-text)]"
              >
                ← Use a different email
              </button>
            </div>
          )}

          {/* ── Step 3: New Password ───────────────── */}
          {step === "password" && (
            <form className="space-y-5" onSubmit={handleResetPassword}>
              <div>
                <label htmlFor="new-password" className="mb-2 block text-sm font-medium text-[var(--mq-text)]">
                  New Password
                </label>
                <div className="relative">
                  <LockKeyhole size={18} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)]" />
                  <input
                    id="new-password"
                    type={showPassword ? "text" : "password"}
                    placeholder="Enter new password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    required
                    autoComplete="new-password"
                    className="h-12 w-full rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] pl-11 pr-12 text-sm text-[var(--mq-text)] placeholder:text-[var(--mq-text-secondary)] outline-none transition focus:border-[var(--mq-primary)] focus:ring-4 focus:ring-[var(--mq-primary)]/10"
                  />
                  <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)]">
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
                <PasswordRequirements password={newPassword} />
              </div>
              <div>
                <label htmlFor="confirm-password" className="mb-2 block text-sm font-medium text-[var(--mq-text)]">
                  Confirm New Password
                </label>
                <div className="relative">
                  <LockKeyhole size={18} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)]" />
                  <input
                    id="confirm-password"
                    type={showPassword ? "text" : "password"}
                    placeholder="Confirm new password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                    autoComplete="new-password"
                    className="h-12 w-full rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] pl-11 pr-4 text-sm text-[var(--mq-text)] placeholder:text-[var(--mq-text-secondary)] outline-none transition focus:border-[var(--mq-primary)] focus:ring-4 focus:ring-[var(--mq-primary)]/10"
                  />
                </div>
                {confirmPassword && newPassword !== confirmPassword && (
                  <p className="mt-1.5 text-xs text-[var(--mq-error)]">Passwords do not match</p>
                )}
              </div>
              <button
                type="submit"
                disabled={loading || !newPassword || newPassword !== confirmPassword}
                className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-[var(--mq-primary)] text-sm font-semibold text-white shadow-sm transition hover:bg-[var(--mq-primary-hover)] focus:outline-none focus:ring-4 focus:ring-[var(--mq-primary)]/20 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {loading ? (
                  <><Loader2 size={18} className="animate-spin" /> Resetting...</>
                ) : (
                  "Reset Password"
                )}
              </button>
            </form>
          )}

          <p className="mt-8 text-center text-sm text-[var(--mq-text-secondary)]">
            Remember your password?{" "}
            <Link to="/login" className="font-semibold text-[var(--mq-primary)] hover:text-[var(--mq-primary-hover)]">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
