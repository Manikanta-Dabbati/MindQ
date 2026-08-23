import axios from "axios";

const TOKEN_KEY = "mindq_token";
const REFRESH_TOKEN_KEY = "mindq_refresh_token";

const api = axios.create({
  baseURL:
    import.meta.env.VITE_API_BASE_URL || "/api/v1",
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 15000,
});

// Attach JWT to every request if present
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Track if we're currently refreshing to avoid multiple concurrent refreshes
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

function processQueue(error: unknown) {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(undefined);
    }
  });
  failedQueue = [];
}

// Central 401 handling: attempt refresh before redirecting to login
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      const url = originalRequest.url ?? "";
      const isAuthCall =
        url.includes("/auth/login") ||
        url.includes("/auth/register") ||
        url.includes("/auth/refresh");

      // Don't attempt refresh for auth calls themselves
      if (isAuthCall) {
        return Promise.reject(error);
      }

      // Don't attempt refresh if we don't have a refresh token
      const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
      if (!refreshToken) {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(REFRESH_TOKEN_KEY);
        if (!window.location.pathname.includes("/login")) {
          window.location.href = "/login";
        }
        return Promise.reject(error);
      }

      // If already refreshing, queue this request
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(() => api(originalRequest));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const response = await axios.post(
          `${
            import.meta.env.VITE_API_BASE_URL || "/api/v1"
          }/auth/refresh`,
          { refreshToken },
          { headers: { "Content-Type": "application/json" } },
        );

        const { token: newToken, refreshToken: newRefreshToken } =
          response.data.data;

        localStorage.setItem(TOKEN_KEY, newToken);
        localStorage.setItem(REFRESH_TOKEN_KEY, newRefreshToken);

        // Retry original request with new token
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        processQueue(null);
        return api(originalRequest);
      } catch (refreshError) {
        // Refresh failed — clear everything and redirect to login
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(REFRESH_TOKEN_KEY);
        processQueue(refreshError);
        if (!window.location.pathname.includes("/login")) {
          window.location.href = "/login";
        }
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  },
);

export { TOKEN_KEY, REFRESH_TOKEN_KEY };
export default api;
