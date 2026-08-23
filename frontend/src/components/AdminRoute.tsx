import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

interface AdminRouteProps {
  children: React.ReactNode;
}

export default function AdminRoute({ children }: AdminRouteProps) {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[var(--mq-bg)]">
        <div className="text-sm text-[var(--mq-text-secondary)]">Loading...</div>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (user.role !== "ROLE_ADMIN") {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
}
