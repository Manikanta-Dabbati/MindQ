import {
  ArrowRight,
  BookOpen,
  Brain,
  Download,
  Lock,
  Sparkles,
  Trophy,
  Zap,
} from "lucide-react";
import { Link } from "react-router-dom";

const features = [
  {
    icon: Brain,
    title: "AI-Powered MCQs",
    description:
      "Generate intelligent multiple-choice questions from any topic, pasted text, or uploaded document using advanced AI models.",
  },
  {
    icon: BookOpen,
    title: "Knowledge Vault",
    description:
      "Upload PDFs, DOCX files, or paste text. Your personal knowledge repository with 500 MB free storage.",
  },
  {
    icon: Zap,
    title: "Instant Generation",
    description:
      "Get quiz-ready questions in seconds. Choose difficulty, question count, and AI model.",
  },
  {
    icon: Trophy,
    title: "Track Progress",
    description:
      "Backend-scored quizzes with detailed explanations. See your strengths and weak areas.",
  },
  {
    icon: Download,
    title: "Export & Save",
    description:
      "Download quizzes as PDF, save to your vault, or generate another set from the same source.",
  },
  {
    icon: Lock,
    title: "Secure & Private",
    description:
      "Your data stays yours. JWT authentication, encrypted storage, and user isolation.",
  },
];

const steps = [
  {
    step: "1",
    title: "Upload or Type",
    description: "Add your study material or simply type a topic.",
  },
  {
    step: "2",
    title: "Generate",
    description: "Choose difficulty and question count. Let AI do the work.",
  },
  {
    step: "3",
    title: "Practice",
    description: "Take the quiz, get instant feedback with explanations.",
  },
  {
    step: "4",
    title: "Improve",
    description: "Review mistakes, track progress, and learn smarter.",
  },
];

export default function Landing() {
  return (
    <div className="min-h-screen bg-[var(--mq-bg)]">
      {/* Navigation */}
      <nav className="fixed top-0 z-50 w-full border-b border-[var(--mq-border)] bg-[var(--mq-surface)]/80 backdrop-blur-lg">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-5">
          <Link to="/" className="text-xl font-bold text-[var(--mq-text)]">
            Mind<span className="text-[var(--mq-primary)]">Q</span>
          </Link>

          <div className="flex items-center gap-3">
            <Link
              to="/login"
              className="rounded-xl px-4 py-2 text-sm font-medium text-[var(--mq-text-secondary)] transition hover:text-[var(--mq-text)]"
            >
              Sign in
            </Link>
            <Link
              to="/register"
              className="flex items-center gap-1.5 rounded-xl bg-[var(--mq-primary)] px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-[var(--mq-primary-hover)]"
            >
              Get Started
              <ArrowRight size={15} />
            </Link>
          </div>
        </div>
      </nav>

      {/* Hero */}
      <section className="relative overflow-hidden pt-32 pb-20 lg:pt-40 lg:pb-28">
        <div className="absolute -right-40 -top-40 h-[500px] w-[500px] rounded-full bg-[var(--mq-primary)]/[0.08] dark:bg-[var(--mq-primary)]/[0.15]" />
        <div className="absolute -bottom-40 -left-40 h-[400px] w-[400px] rounded-full bg-[var(--mq-ai)]/[0.08] dark:bg-[var(--mq-ai)]/[0.15]" />

        <div className="relative mx-auto max-w-6xl px-5 text-center">
          <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-[var(--mq-border)] bg-[var(--mq-surface)] px-4 py-1.5 text-xs font-medium text-[var(--mq-text-secondary)]">
            <Sparkles size={14} className="text-[var(--mq-ai)]" />
            AI-Powered Learning Platform
          </div>

          <h1 className="mx-auto max-w-3xl text-4xl font-bold leading-tight tracking-tight text-[var(--mq-text)] sm:text-5xl lg:text-6xl">
            Sync Your Mind
            <br />
            <span className="bg-gradient-to-r from-[var(--mq-primary)] to-[var(--mq-ai)] bg-clip-text text-transparent">
              with AI
            </span>
          </h1>

          <p className="mx-auto mt-6 max-w-xl text-base leading-7 text-[var(--mq-text-secondary)] sm:text-lg">
            Turn your study materials into intelligent practice quizzes.
            Upload documents, generate MCQs, track your progress, and learn
            smarter — not harder.
          </p>

          <div className="mt-10 flex flex-col items-center justify-center gap-4 sm:flex-row">
            <Link
              to="/register"
              className="flex h-12 items-center gap-2 rounded-xl bg-[var(--mq-primary)] px-7 text-sm font-semibold text-white shadow-sm transition hover:bg-[var(--mq-primary-hover)] hover:shadow-md"
            >
              Start Learning Free
              <ArrowRight size={16} />
            </Link>
            <Link
              to="/login"
              className="flex h-12 items-center gap-2 rounded-xl border border-[var(--mq-border)] bg-[var(--mq-surface)] px-7 text-sm font-semibold text-[var(--mq-text)] transition hover:bg-[var(--mq-bg)]"
            >
              Sign In
            </Link>
          </div>
        </div>
      </section>

      {/* How It Works */}
      <section className="py-20">
        <div className="mx-auto max-w-6xl px-5">
          <div className="mb-14 text-center">
            <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-[var(--mq-primary)]">
              How It Works
            </p>
            <h2 className="text-2xl font-bold text-[var(--mq-text)] sm:text-3xl">
              Four steps to smarter learning
            </h2>
          </div>

          <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-4">
            {steps.map((s) => (
              <div key={s.step} className="text-center">
                <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-[var(--mq-primary)] text-sm font-bold text-white">
                  {s.step}
                </div>
                <h3 className="mb-1 text-sm font-semibold text-[var(--mq-text)]">
                  {s.title}
                </h3>
                <p className="text-xs leading-6 text-[var(--mq-text-secondary)]">
                  {s.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="border-t border-[var(--mq-border)] bg-[var(--mq-surface)] py-20">
        <div className="mx-auto max-w-6xl px-5">
          <div className="mb-14 text-center">
            <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-[var(--mq-ai)]">
              Features
            </p>
            <h2 className="text-2xl font-bold text-[var(--mq-text)] sm:text-3xl">
              Everything you need to learn effectively
            </h2>
          </div>

          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {features.map((f) => {
              const Icon = f.icon;
              return (
                <div
                  key={f.title}
                  className="rounded-2xl border border-[var(--mq-border)] p-6 transition hover:shadow-md"
                >
                  <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl bg-[var(--mq-primary-light)] text-[var(--mq-primary)]">
                    <Icon size={20} />
                  </div>
                  <h3 className="mb-1.5 text-sm font-semibold text-[var(--mq-text)]">
                    {f.title}
                  </h3>
                  <p className="text-xs leading-6 text-[var(--mq-text-secondary)]">
                    {f.description}
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-20">
        <div className="mx-auto max-w-3xl px-5 text-center">
          <div className="rounded-3xl bg-gradient-to-br from-[var(--mq-primary)] to-[var(--mq-ai)] p-10 sm:p-14">
            <h2 className="text-2xl font-bold text-white sm:text-3xl">
              Ready to learn smarter?
            </h2>
            <p className="mx-auto mt-3 max-w-md text-sm text-white/75">
              Join MindQ and start turning your knowledge into mastery.
              Free to get started — no credit card required.
            </p>
            <Link
              to="/register"
              className="mt-8 inline-flex h-12 items-center gap-2 rounded-xl bg-[var(--mq-surface)] px-7 text-sm font-semibold text-[var(--mq-primary)] shadow-sm transition hover:bg-[var(--mq-surface)]/90"
            >
              Create Free Account
              <ArrowRight size={16} />
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-[var(--mq-border)] py-8">
        <div className="mx-auto max-w-6xl px-5 text-center">
          <p className="text-xs text-[var(--mq-text-muted)]">
            © {new Date().getFullYear()} MindQ. Built with AI for smarter
            learning.
          </p>
        </div>
      </footer>
    </div>
  );
}
