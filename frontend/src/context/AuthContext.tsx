import { createContext, useContext, useEffect, useState } from "react";
import type { User } from "../types/auth";
import { TOKEN_KEY, REFRESH_TOKEN_KEY } from "../services/api";
import * as authService from "../services/authService";

interface AuthContextType {
  user: User | null;
  token: string | null;
  refreshToken: string | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  loginWithOtp: (email: string, otpCode: string) => Promise<void>;
  register: (fullName: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  logoutAll: () => Promise<void>;
  changePassword: (currentPassword: string, newPassword: string) => Promise<void>;
  updateProfile: (fullName: string) => Promise<void>;
  deleteAccount: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(
    localStorage.getItem(TOKEN_KEY),
  );
  const [refreshTokenValue, setRefreshTokenValue] = useState<string | null>(
    localStorage.getItem(REFRESH_TOKEN_KEY),
  );
  const [loading, setLoading] = useState(true);

  // Restore user on mount if token exists
  useEffect(() => {
    if (!token) {
      setLoading(false);
      return;
    }

    authService
      .getCurrentUser()
      .then((u) => {
        setUser(u);
      })
      .catch(() => {
        // Token invalid/expired — clear everything
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(REFRESH_TOKEN_KEY);
        setToken(null);
        setRefreshTokenValue(null);
        setUser(null);
      })
      .finally(() => {
        setLoading(false);
      });
  }, [token]);

  const login = async (email: string, password: string) => {
    const response = await authService.login({ email, password });
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
    setToken(response.token);
    setRefreshTokenValue(response.refreshToken);
    setUser(response.user);
  };

  const register = async (fullName: string, email: string, password: string) => {
    await authService.register({ fullName, email, password, consentAccepted: true });
    // After registration, redirect to OTP verification (handled in Register.tsx)
  };

  const logout = async () => {
    // Best-effort notify backend
    await authService.logout(refreshTokenValue);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    setToken(null);
    setRefreshTokenValue(null);
    setUser(null);
  };

  const logoutAll = async () => {
    await authService.logoutAll();
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    setToken(null);
    setRefreshTokenValue(null);
    setUser(null);
  };

  const changePassword = async (currentPassword: string, newPassword: string) => {
    await authService.changePassword(currentPassword, newPassword);
  };

  const updateProfile = async (fullName: string) => {
    const updatedUser = await authService.updateProfile(fullName);
    setUser(updatedUser);
  };

  const deleteAccount = async () => {
    await authService.deleteAccount();
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    setToken(null);
    setRefreshTokenValue(null);
    setUser(null);
  };

  const loginWithOtp = async (email: string, otpCode: string) => {
    const response = await authService.verifyLoginOtp(email, otpCode);
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
    setToken(response.token);
    setRefreshTokenValue(response.refreshToken);
    setUser(response.user);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        refreshToken: refreshTokenValue,
        loading,
        login,
        loginWithOtp,
        register,
        logout,
        logoutAll,
        changePassword,
        updateProfile,
        deleteAccount,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
