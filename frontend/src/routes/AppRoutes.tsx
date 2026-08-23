import { lazy, Suspense } from "react";
import { Route, Routes } from "react-router-dom";

import AppShell from "../components/layout/AppShell";
import ProtectedRoute from "../components/ProtectedRoute";
import AdminRoute from "../components/AdminRoute";
import { FullPageSpinner } from "../components/ui/Spinner";

// Lazy-loaded pages — each gets its own chunk
const Landing = lazy(() => import("../pages/Landing"));
const Login = lazy(() => import("../pages/auth/Login"));
const Register = lazy(() => import("../pages/auth/Register"));
const VerifyEmail = lazy(() => import("../pages/auth/VerifyEmail"));
const ForgotPassword = lazy(() => import("../pages/auth/ForgotPassword"));
const Dashboard = lazy(() => import("../pages/Dashboard"));
const KnowledgeVault = lazy(() => import("../pages/KnowledgeVault"));
const MaterialDetails = lazy(() => import("../pages/MaterialDetails"));
const AIStudio = lazy(() => import("../pages/AIStudio"));
const MCQQuiz = lazy(() => import("../pages/MCQQuiz"));
const QuizHistory = lazy(() => import("../pages/QuizHistory"));
const Analytics = lazy(() => import("../pages/Analytics"));
const Settings = lazy(() => import("../pages/Settings"));
const Profile = lazy(() => import("../pages/Profile"));
const Subscription = lazy(() => import("../pages/Subscription"));
const AdminDashboard = lazy(() => import("../pages/admin/AdminDashboard"));
const AdminUsers = lazy(() => import("../pages/admin/AdminUsers"));
const NotFound = lazy(() => import("../pages/errors/NotFound"));

function LazyWrapper({ children }: { children: React.ReactNode }) {
  return <Suspense fallback={<FullPageSpinner />}>{children}</Suspense>;
}

export default function AppRoutes() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/" element={<LazyWrapper><Landing /></LazyWrapper>} />
      <Route path="/login" element={<LazyWrapper><Login /></LazyWrapper>} />
      <Route path="/register" element={<LazyWrapper><Register /></LazyWrapper>} />
      <Route path="/verify-email" element={<LazyWrapper><VerifyEmail /></LazyWrapper>} />
      <Route path="/forgot-password" element={<LazyWrapper><ForgotPassword /></LazyWrapper>} />

      {/* Protected routes */}
      <Route path="/dashboard" element={<ProtectedRoute><AppShell><LazyWrapper><Dashboard /></LazyWrapper></AppShell></ProtectedRoute>} />
      <Route path="/vault/:id" element={<ProtectedRoute><AppShell><LazyWrapper><MaterialDetails /></LazyWrapper></AppShell></ProtectedRoute>} />
      <Route path="/vault" element={<ProtectedRoute><AppShell><LazyWrapper><KnowledgeVault /></LazyWrapper></AppShell></ProtectedRoute>} />
      <Route path="/ai-studio" element={<ProtectedRoute><AppShell><LazyWrapper><AIStudio /></LazyWrapper></AppShell></ProtectedRoute>} />
      <Route path="/quiz" element={<ProtectedRoute><AppShell><LazyWrapper><MCQQuiz /></LazyWrapper></AppShell></ProtectedRoute>} />
      <Route path="/history" element={<ProtectedRoute><AppShell><LazyWrapper><QuizHistory /></LazyWrapper></AppShell></ProtectedRoute>} />
      <Route path="/analytics" element={<ProtectedRoute><AppShell><LazyWrapper><Analytics /></LazyWrapper></AppShell></ProtectedRoute>} />
      <Route path="/settings" element={<ProtectedRoute><AppShell><LazyWrapper><Settings /></LazyWrapper></AppShell></ProtectedRoute>} />
      <Route path="/profile" element={<ProtectedRoute><AppShell><LazyWrapper><Profile /></LazyWrapper></AppShell></ProtectedRoute>} />
      <Route path="/subscription" element={<ProtectedRoute><AppShell><LazyWrapper><Subscription /></LazyWrapper></AppShell></ProtectedRoute>} />

      {/* Admin routes — require ROLE_ADMIN */}
      <Route path="/admin" element={<AdminRoute><AppShell><LazyWrapper><AdminDashboard /></LazyWrapper></AppShell></AdminRoute>} />
      <Route path="/admin/users" element={<AdminRoute><AppShell><LazyWrapper><AdminUsers /></LazyWrapper></AppShell></AdminRoute>} />

      {/* 404 — catch-all for unknown routes */}
      <Route path="*" element={<LazyWrapper><NotFound /></LazyWrapper>} />
    </Routes>
  );
}