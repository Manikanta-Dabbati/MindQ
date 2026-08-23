import {
  ArrowRight,
  Brain,
  Check,
  Eye,
  EyeOff,
  LockKeyhole,
  Mail,
  User,
} from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import * as authService from "../../services/authService";
import PasswordRequirements from "../../components/auth/PasswordRequirements";
import AuthMobileHeader from "../../components/auth/AuthMobileHeader";

export default function Register() {
  const [showPassword, setShowPassword] = useState(false);
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [consentAccepted, setConsentAccepted] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setFieldErrors({});

    // Validate consent
    if (!consentAccepted) {
      setFieldErrors({ consent: "Please accept the Terms and Privacy Policy to continue" });
      setLoading(false);
      return;
    }

    setLoading(true);

    try {
      await authService.register({ fullName, email, password, consentAccepted });
      // Redirect to email verification
      navigate(`/verify-email?email=${encodeURIComponent(email)}`);
    } catch (err: unknown) {
      if (
        typeof err === "object" &&
        err !== null &&
        "response" in err
      ) {
        const axiosErr = err as { response?: { status?: number; data?: { message?: string; data?: Record<string, string> } } };
        // Handle field-level validation errors
        if (axiosErr.response?.data?.data) {
          setFieldErrors(axiosErr.response.data.data);
        }
        setError(axiosErr.response?.data?.message ?? "Registration failed. Please try again.");
      } else {
        setError("Registration failed. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-[var(--mq-bg)] lg:grid lg:grid-cols-2 lg:min-h-screen">
      {/* Branding */}
      <div className="relative hidden overflow-hidden bg-gradient-to-br from-[var(--mq-primary)] to-[var(--mq-ai)] p-12 text-white lg:flex lg:flex-col lg:justify-between xl:p-16">
        <div className="absolute -right-24 -top-24 h-72 w-72 rounded-full bg-[var(--mq-surface)]/10" />
        <div className="absolute -bottom-32 -left-24 h-96 w-96 rounded-full bg-[var(--mq-surface)]/10" />

        <div className="relative">
          <div className="text-3xl font-bold">
            Mind<span className="text-white/80">Q</span>
          </div>

          <p className="mt-1 text-sm text-white/70">
            Sync Your Mind with AI
          </p>
        </div>

        <div className="relative">
          <h1 className="max-w-lg text-4xl font-bold leading-tight xl:text-5xl">
            Build better learning habits with MindQ.
          </h1>

          <div className="mt-8 space-y-4">
            {[
              "Organize your learning resources",
              "Generate AI-powered practice",
              "Track your learning progress",
            ].map((item) => (
              <div
                key={item}
                className="flex items-center gap-3 text-sm text-white/80"
              >
                <span className="flex h-6 w-6 items-center justify-center rounded-full bg-[var(--mq-surface)]/15">
                  <Check size={14} />
                </span>

                {item}
              </div>
            ))}
          </div>
        </div>

        <p className="relative text-sm text-white/60">
          Start building your knowledge today.
        </p>
      </div>

      {/* Register */}
      <AuthMobileHeader title="Create your account" subtitle="Start your personalized learning journey with MindQ." />

      <div className="flex min-h-0 items-center justify-center px-5 py-10 sm:px-8 lg:min-h-screen">
        <div className="w-full max-w-md">
          
          {/* Desktop-only heading */}
          <div className="mb-8 hidden lg:block">
            <div className="mb-5 flex h-11 w-11 items-center justify-center rounded-xl bg-[var(--mq-primary-light)] text-[var(--mq-primary)]"><Brain size={21} /></div>
            <h2 className="text-2xl font-bold tracking-tight text-[var(--mq-text)]">Create your account</h2>
            <p className="mt-2 text-sm leading-6 text-[var(--mq-text-secondary)]">Start your personalized learning journey with MindQ.</p>
          </div>

          {error && <div className="mb-5 rounded-xl border border-[var(--mq-error-border)] bg-[var(--mq-error-light)] px-4 py-3 text-sm text-[var(--mq-error)]">{error}</div>}

          <form className="space-y-5" onSubmit={handleSubmit}>
            <div>
              <label htmlFor="name" className="mb-2 block text-sm font-medium text-[var(--mq-text)]">Full name</label>
              <div className="relative">
                <User size={18} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)]" />
                <input id="name" type="text" placeholder="Your name" value={fullName} onChange={(e) => setFullName(e.target.value)} required autoComplete="name"
                  className="h-12 w-full rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] pl-11 pr-4 text-sm text-[var(--mq-text)] placeholder:text-[var(--mq-text-secondary)] outline-none transition focus:border-[var(--mq-primary)] focus:ring-4 focus:ring-[var(--mq-primary)]/10" />
              </div>
            </div>

            {/* Email */}
          

            {/* Email */}
            <div>
              <label
                htmlFor="register-email"
                className="mb-2 block text-sm font-medium text-[var(--mq-text)]"
              >
                Email address
              </label>

              <div className="relative">
                <Mail
                  size={18}
                  className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)]"
                />

                <input
                  id="register-email"
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

            {/* Password */}
            <div>
              <label
                htmlFor="register-password"
                className="mb-2 block text-sm font-medium text-[var(--mq-text)]"
              >
                Password
              </label>

              <div className="relative">
                <LockKeyhole
                  size={18}
                  className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)]"
                />

                <input
                  id="register-password"
                  type={showPassword ? "text" : "password"}
                  placeholder="Create a password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  autoComplete="new-password"
                  className="h-12 w-full rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] pl-11 pr-12 text-sm text-[var(--mq-text)] placeholder:text-[var(--mq-text-secondary)] outline-none transition focus:border-[var(--mq-primary)] focus:ring-4 focus:ring-[var(--mq-primary)]/10"
                />

                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[var(--mq-text-muted)]"
                >
                  {showPassword ? (
                    <EyeOff size={18} />
                  ) : (
                    <Eye size={18} />
                  )}
                </button>
              </div>

              <PasswordRequirements password={password} />
            </div>

            <div>
              <label className="flex items-start gap-2.5 text-xs leading-5 text-[var(--mq-text-secondary)]">
                <input
                  type="checkbox"
                  checked={consentAccepted}
                  onChange={(e) => setConsentAccepted(e.target.checked)}
                  className="mt-1 h-4 w-4 shrink-0 accent-[var(--mq-primary)]"
                />
                <span>
                  I agree to the MindQ terms and privacy policy.
                </span>
              </label>
              {fieldErrors.consent && (
                <p className="mt-1.5 text-xs text-[var(--mq-error)]">{fieldErrors.consent}</p>
              )}
            </div>

            <button
              type="submit"
              disabled={loading}
              className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-[var(--mq-primary)] text-sm font-semibold text-white shadow-sm transition hover:bg-[var(--mq-primary-hover)] focus:outline-none focus:ring-4 focus:ring-[var(--mq-primary)]/20 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {loading ? "Creating account..." : "Create account"}
              {!loading && <ArrowRight size={17} />}
            </button>
          </form>

          <p className="mt-8 text-center text-sm text-[var(--mq-text-secondary)]">
            Already have an account?{" "}
            <Link
              to="/login"
              className="font-semibold text-[var(--mq-primary)] hover:text-[var(--mq-primary-hover)]"
            >
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}