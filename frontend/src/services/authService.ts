import api from "./api";
import type {
  ApiResponse,
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  User,
} from "../types/auth";

export async function login(data: LoginRequest): Promise<AuthResponse> {
  const response = await api.post<ApiResponse<AuthResponse>>(
    "/auth/login",
    data,
  );
  return response.data.data;
}

export async function register(data: RegisterRequest): Promise<User> {
  const response = await api.post<ApiResponse<User>>("/auth/register", data);
  return response.data.data;
}

export async function getCurrentUser(): Promise<User> {
  const response = await api.get<ApiResponse<User>>("/auth/me");
  return response.data.data;
}

export async function logout(refreshToken: string | null): Promise<void> {
  try {
    await api.post("/auth/logout", { refreshToken });
  } catch {
    // Best-effort — even if backend fails, clear locally
  }
}

export async function logoutAll(): Promise<void> {
  await api.post("/auth/logout-all");
}

export async function changePassword(
  currentPassword: string,
  newPassword: string,
): Promise<void> {
  await api.post("/auth/change-password", { currentPassword, newPassword });
}

export async function forgotPassword(email: string): Promise<void> {
  await api.post("/auth/forgot-password", { email });
}

export async function updateProfile(fullName: string): Promise<User> {
  const response = await api.put<ApiResponse<User>>("/auth/me", { fullName });
  return response.data.data;
}

export async function deleteAccount(): Promise<void> {
  await api.delete("/auth/me");
}

export async function verifyEmailOtp(
  email: string,
  otpCode: string,
): Promise<void> {
  await api.post("/auth/verify-email-otp", { email, otpCode, purpose: "REGISTRATION" });
}

export async function requestLoginOtp(email: string): Promise<void> {
  await api.post("/auth/request-login-otp", { email, purpose: "LOGIN" });
}

export async function verifyLoginOtp(
  email: string,
  otpCode: string,
): Promise<import("../types/auth").AuthResponse> {
  const response = await api.post<import("../types/auth").ApiResponse<import("../types/auth").AuthResponse>>(
    "/auth/verify-login-otp",
    { email, otpCode, purpose: "LOGIN" },
  );
  return response.data.data;
}

export async function resendOtp(
  email: string,
  purpose: string,
): Promise<void> {
  await api.post("/auth/resend-otp", { email, purpose });
}

export async function resetPassword(
  token: string,
  newPassword: string,
): Promise<void> {
  await api.post("/auth/reset-password", { token, newPassword });
}

export async function verifyResetOtp(
  email: string,
  otpCode: string,
): Promise<string> {
  const response = await api.post<
    import("../types/auth").ApiResponse<{ resetToken: string }>
  >("/auth/verify-reset-otp", { email, otpCode, purpose: "PASSWORD_RESET" });
  return response.data.data.resetToken;
}
