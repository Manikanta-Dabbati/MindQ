import { CheckCircle2, Loader2, Mail } from "lucide-react";
import { useEffect, useState, useCallback } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import OtpInput from "../../components/auth/OtpInput";
import * as authService from "../../services/authService";
import AuthMobileHeader from "../../components/auth/AuthMobileHeader";

export default function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const email = searchParams.get("email") ?? "";

  const [otp, setOtp] = useState("");
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const [cooldown, setCooldown] = useState(0);

  const maskedEmail = email ? email.replace(/(.{2})(.*)(@.*)/, "$1***$3") : "***";

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setTimeout(() => setCooldown((c) => c - 1), 1000);
    return () => clearTimeout(timer);
  }, [cooldown]);

  const handleVerify = useCallback(async () => {
    if (otp.length !== 6) return;
    setError("");
    setLoading(true);
    try {
      await authService.verifyEmailOtp(email, otp);
      setSuccess(true);
      setTimeout(() => navigate("/login"), 2000);
    } catch (err: unknown) {
      if (typeof err === "object" && err !== null && "response" in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        setError(axiosErr.response?.data?.message ?? "Verification failed");
      } else {
        setError("Verification failed. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  }, [otp, email, navigate]);

  const handleResend = useCallback(async () => {
    if (cooldown > 0) return;
    setResending(true);
    setError("");
    try {
      await authService.resendOtp(email, "REGISTRATION");
      setCooldown(60);
    } catch (err: unknown) {
      if (typeof err === "object" && err !== null && "response" in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        setError(axiosErr.response?.data?.message ?? "Failed to resend code");
      } else {
        setError("Failed to resend code");
      }
    } finally {
      setResending(false);
    }
  }, [email, cooldown]);

  if (success) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[var(--mq-bg)] px-5">
        <div className="w-full max-w-md text-center">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-[var(--mq-success-light)]">
            <CheckCircle2 size={32} className="text-[var(--mq-success)]" />
          </div>
          <h1 className="mt-6 text-2xl font-bold text-[var(--mq-text)]">Email verified!</h1>
          <p className="mt-3 text-sm text-[var(--mq-text-secondary)]">Your account has been activated. Redirecting to sign in...</p>
          <Link to="/login" className="mt-6 inline-flex items-center gap-2 rounded-xl bg-[var(--mq-primary)] px-6 py-3 text-sm font-semibold text-white transition hover:bg-[var(--mq-primary-hover)]">Sign in now</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[var(--mq-bg)] lg:grid lg:grid-cols-2">
      <div className="relative hidden overflow-hidden bg-gradient-to-br from-[var(--mq-primary)] to-[var(--mq-ai)] p-12 text-white lg:flex lg:flex-col lg:justify-center xl:p-16">
        <div className="absolute -right-24 -top-24 h-72 w-72 rounded-full bg-[var(--mq-surface)]/10" />
        <div className="absolute -bottom-32 -left-24 h-96 w-96 rounded-full bg-[var(--mq-surface)]/10" />
        <div className="relative">
          <div className="text-3xl font-bold">Mind<span className="text-white/80">Q</span></div>
          <p className="mt-1 text-sm text-white/70">Sync Your Mind with AI</p>
          <h1 className="mt-10 text-4xl font-bold leading-tight xl:text-5xl">Verify your email to get started.</h1>
          <p className="mt-6 max-w-md text-base leading-7 text-white/75">We&apos;ve sent a verification code to your email address. Enter it below to activate your account.</p>
        </div>
      </div>

      <AuthMobileHeader title="Verify your email" subtitle="We sent a verification code to your email." />

      <div className="flex min-h-0 items-center justify-center px-5 py-10 sm:px-8 lg:min-h-screen">
        <div className="w-full max-w-md">
          <div className="mb-8 hidden lg:block">
            <div className="mb-5 flex h-11 w-11 items-center justify-center rounded-xl bg-[var(--mq-primary-light)] text-[var(--mq-primary)]"><Mail size={21} /></div>
            <h2 className="text-2xl font-bold tracking-tight text-[var(--mq-text)]">Verify your email</h2>
            <p className="mt-2 text-sm leading-6 text-[var(--mq-text-secondary)]">We sent a verification code to <span className="font-medium text-[var(--mq-text)]">{maskedEmail}</span></p>
          </div>

          {error && <div className="mb-5 rounded-xl border border-[var(--mq-error-border)] bg-[var(--mq-error-light)] px-4 py-3 text-sm text-[var(--mq-error)]">{error}</div>}

          <div className="space-y-6">
            <OtpInput value={otp} onChange={setOtp} disabled={loading} autoFocus />

            <button type="button" onClick={handleVerify} disabled={loading || otp.length !== 6}
              className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-[var(--mq-primary)] text-sm font-semibold text-white shadow-sm transition hover:bg-[var(--mq-primary-hover)] focus:outline-none focus:ring-4 focus:ring-[var(--mq-primary)]/20 disabled:cursor-not-allowed disabled:opacity-60">
              {loading ? <><Loader2 size={18} className="animate-spin" /> Verifying...</> : "Verify Email"}
            </button>

            <div className="text-center">
              <p className="text-sm text-[var(--mq-text-secondary)]">Didn&apos;t receive the code? </p>
              {cooldown > 0 ? (
                <p className="mt-1 text-xs text-[var(--mq-text-muted)]">Resend available in {cooldown}s</p>
              ) : (
                <button type="button" onClick={handleResend} disabled={resending}
                  className="mt-1 text-sm font-semibold text-[var(--mq-primary)] hover:text-[var(--mq-primary-hover)] disabled:opacity-60">
                  {resending ? "Sending..." : "Resend Code"}
                </button>
              )}
            </div>

            <div className="text-center">
              <Link to="/register" className="text-sm text-[var(--mq-text-secondary)] hover:text-[var(--mq-primary)]">← Back to registration</Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
