import {
  BarChart3,
  BookOpen,
  Brain,
  CreditCard,
  Home,
  LogOut,
  Settings,
  Trophy,
  UserCircle,
  X,
} from "lucide-react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

interface SidebarProps {
  mobileOpen: boolean;
  onClose: () => void;
}

const navigationItems = [
  {
    label: "Dashboard",
    icon: Home,
    path: "/dashboard",
  },
  {
    label: "Knowledge Vault",
    icon: BookOpen,
    path: "/vault",
  },
  {
    label: "AI Studio",
    icon: Brain,
    path: "/ai-studio",
  },
  {
    label: "Quiz History",
    icon: Trophy,
    path: "/history",
  },
  {
    label: "Analytics",
    icon: BarChart3,
    path: "/analytics",
  },
  {
    label: "Profile",
    icon: UserCircle,
    path: "/profile",
  },
  {
    label: "Subscription",
    icon: CreditCard,
    path: "/subscription",
  },
  {
    label: "Settings",
    icon: Settings,
    path: "/settings",
  },
];

export default function Sidebar({
  mobileOpen,
  onClose,
}: SidebarProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  const initials = user?.fullName
    ?.split(" ")
    .map((n) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2) ?? "M";

  return (
    <>
      {/* Mobile overlay */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/20 lg:hidden"
          onClick={onClose}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`
          fixed inset-y-0 left-0 z-50 flex w-64 flex-col
          border-r border-[var(--mq-border)] bg-[var(--mq-surface)]
          transition-transform duration-200
          lg:static lg:translate-x-0
          ${mobileOpen ? "translate-x-0" : "-translate-x-full"}
        `}
      >
        {/* Brand */}
        <div className="flex h-20 items-center justify-between px-6">
          <Link
            to="/dashboard"
            onClick={onClose}
            className="block"
          >
            <div className="text-2xl font-bold tracking-tight text-[var(--mq-text)]">
              Mind<span className="text-[var(--mq-primary)]">Q</span>
            </div>

            <p className="mt-0.5 text-[11px] font-medium text-[var(--mq-text-secondary)]">
              Sync Your Mind with AI
            </p>
          </Link>

          {/* Mobile close button */}
          <button
            onClick={onClose}
            className="rounded-lg p-2 text-[var(--mq-text-secondary)] transition hover:bg-[var(--mq-surface-hover)] hover:text-[var(--mq-text)] lg:hidden"
            aria-label="Close navigation"
          >
            <X size={20} />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-5">
          <p className="mb-3 px-3 text-[11px] font-semibold uppercase tracking-wider text-[var(--mq-text-muted)]">
            Workspace
          </p>

          <div className="space-y-1">
            {navigationItems.map((item) => {
              const Icon = item.icon;

              const active = item.path === "/dashboard"
                ? location.pathname === item.path
                : location.pathname === item.path || location.pathname.startsWith(item.path + "/");

              return (
                <Link
                  key={item.label}
                  id={item.path === "/dashboard" ? "sidebar-dashboard" : item.path === "/vault" ? "sidebar-vault" : item.path === "/ai-studio" ? "sidebar-ai-studio" : item.path === "/history" ? "sidebar-history" : item.path === "/analytics" ? "sidebar-analytics" : undefined}
                  to={item.path}
                  onClick={onClose}
                  className={`
                    flex w-full items-center gap-3 rounded-xl px-3 py-2.5
                    text-sm font-medium transition-colors
                    ${
                      active
                        ? "bg-[var(--mq-primary-light)] text-[var(--mq-primary)]"
                        : "text-[var(--mq-text-secondary)] hover:bg-[var(--mq-surface-hover)] hover:text-[var(--mq-text)]"
                    }
                  `}
                >
                  <Icon
                    size={19}
                    strokeWidth={active ? 2.3 : 2}
                  />

                  <span>{item.label}</span>
                </Link>
              );
            })}
          </div>
        </nav>

        {/* Bottom profile + logout */}
        <div className="border-t border-[var(--mq-border)] p-4">
          <div className="flex items-center gap-3 rounded-xl p-2">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[var(--mq-primary-light)] text-sm font-semibold text-[var(--mq-primary)]">
              {initials}
            </div>

            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-semibold text-[var(--mq-text)]">
                {user?.fullName ?? "User"}
              </p>

              <p className="truncate text-xs text-[var(--mq-text-secondary)]">
                {user?.email ?? ""}
              </p>
            </div>

            <button
              onClick={handleLogout}
              className="shrink-0 rounded-lg p-2 text-[var(--mq-text-muted)] transition hover:bg-[var(--mq-error-light)] hover:text-[var(--mq-error)]"
              title="Logout"
              aria-label="Logout"
            >
              <LogOut size={18} />
            </button>
          </div>
        </div>
      </aside>
    </>
  );
}