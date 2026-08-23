import { describe, it, expect, vi, beforeEach } from 'vitest';
import * as authService from '../authService';

// Mock axios
vi.mock('../api', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

import api from '../api';

describe('authService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('login', () => {
    it('calls POST /auth/login with credentials', async () => {
      const mockResponse = {
        data: {
          data: {
            token: 'token123',
            refreshToken: 'refresh123',
            tokenType: 'Bearer',
            expiresInSeconds: 86400,
            refreshExpiresInSeconds: 604800,
            user: { id: 1, email: 'test@example.com', fullName: 'Test User', role: 'ROLE_USER', createdAt: '2026-01-01' },
          },
        },
      };
      vi.mocked(api.post).mockResolvedValue(mockResponse);

      const result = await authService.login({
        email: 'test@example.com',
        password: 'password123',
      });

      expect(api.post).toHaveBeenCalledWith('/auth/login', {
        email: 'test@example.com',
        password: 'password123',
      });
      expect(result.token).toBe('token123');
    });
  });

  describe('register', () => {
    it('calls POST /auth/register with user data', async () => {
      const mockResponse = {
        data: {
          data: { id: 1, email: 'new@example.com', fullName: 'New User', role: 'ROLE_USER', createdAt: '2026-01-01' },
        },
      };
      vi.mocked(api.post).mockResolvedValue(mockResponse);

      const result = await authService.register({
        fullName: 'New User',
        email: 'new@example.com',
        password: 'password123',
        consentAccepted: true,
      });

      expect(api.post).toHaveBeenCalledWith('/auth/register', {
        fullName: 'New User',
        email: 'new@example.com',
        password: 'password123',
        consentAccepted: true,
      });
      expect(result.email).toBe('new@example.com');
    });
  });

  describe('getCurrentUser', () => {
    it('calls GET /auth/me', async () => {
      const mockResponse = {
        data: {
          data: { id: 1, email: 'test@example.com', fullName: 'Test User' },
        },
      };
      vi.mocked(api.get).mockResolvedValue(mockResponse);

      const result = await authService.getCurrentUser();

      expect(api.get).toHaveBeenCalledWith('/auth/me');
      expect(result.email).toBe('test@example.com');
    });
  });
});
