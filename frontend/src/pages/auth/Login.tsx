import { ArrowRight, Brain, Eye, EyeOff, KeyRound, LockKeyhole, Mail, Loader2 } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import * as authService from "../../services/authService";
import { getDevConfig, devAutoVerify } from "../../services/authService";
import OtpInput from "../../components/auth/OtpInput";
import AuthMobileHeader from "../../components/auth/AuthMobileHeader";

type LoginMode = "password" | "otp";

export default function Login() {
  const [searchParams] = useSearchParams();
  const resetSuccess = searchParams.get("reset") === "success";
  const [mode, setMode] = useState<LoginMode>("password");
  const [bypassActive, setBypassActive] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [otp, setOtp] = useState("");
  const [otpSent, setOtpSent] = useState(false);
  const [otpLoading, setOtpLoading] = useState(false);
  const [otpCooldown, setOtpCooldown] = useState(0);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const { login, loginWithOtp } = useAuth();
  const navigate = useNavigate();

  // Check dev OTP bypass config on mount
  useEffect(() => {
    getDevConfig()
      .then((config) => setBypassActive(config.bypassEnabled))
      .catch(() => {});
  }, []);

  const handlePasswordLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await login(email, password);
      navigate("/dashboard");
    } catch (err: unknown) {
      if (typeof err === "object" && err !== null && "response" in err) {
        const axiosErr = err as { response?: { status?: number; data?: { message?: string } } };
        setError(axiosErr.response?.data?.message ?? "Login failed. Please try again.");
      } else {
        setError("Login failed. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  };

  const startCooldownTimer = () => {
    setOtpCooldown(60);
    const timer = setInterval(() => {
      setOtpCooldown((prev) => {
        if (prev <= 1) { clearInterval(timer); return 0; }
        return prev - 1;
      });
    }, 1000);
  };

  const handleRequestOtp = async () => {
    if (!email || otpCooldown > 0) return;
    setError("");
    setOtpLoading(true);
    try {
      // Dev bypass: auto-verify without sending OTP
      if (bypassActive) {
        const result = await devAutoVerify(email, "LOGIN");
        // The response contains auth tokens
        const data = result.data as { token: string; refreshToken: string; user: Record<string, unknown> } | undefined;
        if (data?.token) {
          localStorage.setItem("mindq_token", data.token);
          localStorage.setItem("mindq_refresh_token", data.refreshToken);
          navigate("/dashboard");
          return;
        }
      }
      await authService.requestLoginOtp(email);
      setOtpSent(true);
      startCooldownTimer();
    } catch (err: unknown) {
      if (typeof err === "object" && err !== null && "response" in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        setError(axiosErr.response?.data?.message ?? "Failed to send code");
      } else {
        setError("Failed to send code");
      }
    } finally {
      setOtpLoading(false);
    }
  };

  const handleOtpLogin = async () => {
    if (otp.length !== 6) return;
    setError("");
    setLoading(true);
    try {
      await loginWithOtp(email, otp);
      navigate("/dashboard");
    } catch (err: unknown) {
      if (typeof err === "object" && err !== null && "response" in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        setError(axiosErr.response?.data?.message ?? "Verification failed");
      } else {
        setError("Verification failed");
      }
    } finally {
      setLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (otpCooldown > 0) return;
    setOtpLoading(true);
    setError("");
    try {
      await authService.requestLoginOtp(email);
      startCooldownTimer();
    } catch { /* */ } finally {
      setOtpLoading(false);
    }
  };

  return (
    <div className="bg-[var(--mq-bg)] lg:grid lg:grid-cols-2 lg:min-h-screen">
      <div className="relative hidden overflow-hidden bg-gradient-to-br from-[var(--mq-primary)] to-[var(--mq-ai)] p-12 text-white lg:flex lg:flex-col lg:justify-between xl:p-16">
        <div className="absolute -right-24 -top-24 h-72 w-72 rounded-full bg-[var(--mq-surface)]/10" />
        <div className="absolute -bottom-32 -left-24 h-96 w-96 rounded-full bg-[var(--mq-surface)]/10" />
        <div className="relative">
          <div className="text-3xl font-bold">Mind<span className="text-white/80">Q</span></div>
          <p className="mt-1 text-sm text-white/70">Sync Your Mind with AI</p>
        </div>
        <div className="relative max-w-lg">
          <h1 className="text-4xl font-bold leading-tight xl:text-5xl">Turn what you learn into what you know.</h1>
          <p className="mt-6 max-w-md text-base leading-7 text-white/75">Learn smarter with AI-powered practice, personalized quizzes, and a knowledge space built around you.</p>
        </div>
        <p className="relative text-sm text-white/60">Your learning journey, organized.</p>
      </div>

      <AuthMobileHeader title="Welcome back 👋" subtitle="Sign in to continue your learning journey." />

      <div className="flex min-h-0 items-center justify-center px-5 py-10 sm:px-8 lg:min-h-screen">
        <div className="w-full max-w-md">
          <div className="mb-8 hidden lg:block">
            <div className="mb-5 flex h-11 w-11 items-center justify-center rounded-xl bg-[var(--mq-primary-light)] text-[var(--mq-primary)]"><Brain size={21} /></div>
            <h2 className="text-2xl font-bold tracking-tight text-[var(--mq-text)]">Welcome back</h2>
            <p className="mt-2 text-sm leading-6 text-[var(--mq-text-secondary)]">Sign in to continue your learning journey.</p>
          </div>

          <div className="mb-6 grid grid-cols-2 gap-2 rounded-xl bg-[var(--mq-bg)] p-1">
            <button type="button" onClick={() => { setMode("password"); setError(""); setOtp(""); setOtpSent(false); }}
              className={`flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium transition ${mode === "password" ? "bg-[var(--mq-surface)] text-[var(--mq-primary)] shadow-sm" : "text-[var(--mq-text-secondary)] hover:text-[var(--mq-text)]"}`}>
              <KeyRound size={16} /> Password
            </button>
            <button type="button" onClick={() => { setMode("otp"); setError(""); setPassword(""); }}
              className={`flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium transition ${mode === "otp" ? "bg-[var(--mq-surface)] text-[var(--mq-primary)] shadow-sm" : "text-[var(--mq-text-secondary)] hover:text-[var(--mq-text)]"}`}>
              <Mail size={16} /> Email OTP
            </button>
          </div>

          {resetSuccess && (
            <div className="mb-5 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:border-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-400">
              Password reset successful! Please sign in with your new password.
            </div>
          )}
          {error && <div className="mb-5 rounded-xl border border-[var(--mq-error-border)] bg-[var(--mq-error-light)] px-4 py-3 text-sm text-[var(--mq-error)]">{error}</div>}

          {mode === "password" && (
            <form className="space-y-5" onSubmit={handlePasswordLogin}>
              <div>
                <label htmlFor="email" className="mb-2 block text-sm font-medium text-[var(--mq-text)]">Email address</label>
                <div className="relative">
                  <Mail size={18} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)]" />
                  <input id="email" type="email" placeholder="you@example.com" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email"
                    className="h-12 w-full rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] pl-11 pr-4 text-sm text-[var(--mq-text)] placeholder:text-[var(--mq-text-secondary)] outline-none transition focus:border-[var(--mq-primary)] focus:ring-4 focus:ring-[var(--mq-primary)]/10" />
                </div>
              </div>
              <div>
                <div className="mb-2 flex items-center justify-between">
                  <label htmlFor="password" className="block text-sm font-medium text-[var(--mq-text)]">Password</label>
                  <Link to="/forgot-password" className="text-xs font-medium text-[var(--mq-primary)] hover:text-[var(--mq-primary-hover)]">Forgot password?</Link>
                </div>
                <div className="relative">
                  <LockKeyhole size={18} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)]" />
                  <input id="password" type={showPassword ? "text" : "password"} placeholder="Enter your password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password"
                    className="h-12 w-full rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] pl-11 pr-12 text-sm text-[var(--mq-text)] placeholder:text-[var(--mq-text-secondary)] outline-none transition focus:border-[var(--mq-primary)] focus:ring-4 focus:ring-[var(--mq-primary)]/10" />
                  <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)] hover:text-[var(--mq-text-secondary)]">
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>
              <button type="submit" disabled={loading}
                className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-[var(--mq-primary)] text-sm font-semibold text-white shadow-sm transition hover:bg-[var(--mq-primary-hover)] disabled:cursor-not-allowed disabled:opacity-60">
                {loading ? "Signing in..." : "Sign in"}{!loading && <ArrowRight size={17} />}
              </button>
            </form>
          )}

          {mode === "otp" && (
            <div className="space-y-5">
              {bypassActive && (
                <div className="rounded-xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-700">
                  Development mode -- OTP will be bypassed automatically.
                </div>
              )}
              <div>
                <label htmlFor="otp-email" className="mb-2 block text-sm font-medium text-[var(--mq-text)]">Email address</label>
                <div className="relative">
                  <Mail size={18} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)]" />
                  <input id="otp-email" type="email" placeholder="you@example.com" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" disabled={otpSent}
                    className="h-12 w-full rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] pl-11 pr-4 text-sm text-[var(--mq-text)] placeholder:text-[var(--mq-text-secondary)] outline-none transition focus:border-[var(--mq-primary)] focus:ring-4 focus:ring-[var(--mq-primary)]/10 disabled:opacity-60" />
                </div>
              </div>
              {!otpSent ? (
                <button type="button" onClick={handleRequestOtp} disabled={otpLoading || !email}
                  className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-[var(--mq-primary)] text-sm font-semibold text-white shadow-sm transition hover:bg-[var(--mq-primary-hover)] disabled:cursor-not-allowed disabled:opacity-60">
                  {otpLoading ? <><Loader2 size={18} className="animate-spin" /> Sending...</> : "Send Verification Code"}
                </button>
              ) : (
                <>
                  <div>
                    <label className="mb-2 block text-sm font-medium text-[var(--mq-text)]">Enter verification code</label>
                    <OtpInput value={otp} onChange={setOtp} disabled={loading} autoFocus />
                  </div>
                  <button type="button" onClick={handleOtpLogin} disabled={loading || otp.length !== 6}
                    className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-[var(--mq-primary)] text-sm font-semibold text-white shadow-sm transition hover:bg-[var(--mq-primary-hover)] disabled:cursor-not-allowed disabled:opacity-60">
                    {loading ? <><Loader2 size={18} className="animate-spin" /> Verifying...</> : "Verify & Sign In"}
                  </button>
                  <div className="text-center">
                    {otpCooldown > 0 ? <p className="text-xs text-[var(--mq-text-muted)]">Resend available in {otpCooldown}s</p>
                      : <button type="button" onClick={handleResendOtp} disabled={otpLoading} className="text-sm font-semibold text-[var(--mq-primary)] hover:text-[var(--mq-primary-hover)]">Resend Code</button>}
                  </div>
                </>
              )}
            </div>
          )}

          <p className="mt-8 text-center text-sm text-[var(--mq-text-secondary)]">
            Don&apos;t have an account? <Link to="/register" className="font-semibold text-[var(--mq-primary)] hover:text-[var(--mq-primary-hover)]">Create one</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
