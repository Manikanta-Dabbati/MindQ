import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi } from 'vitest';
import ProtectedRoute from '../ProtectedRoute';
import { useAuth } from '../../context/AuthContext';

vi.mock('../../context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

const mockUseAuth = vi.mocked(useAuth);

function TestComponent() {
  return <div>Protected Content</div>;
}

describe('ProtectedRoute', () => {
  it('renders children when user is authenticated', () => {
    mockUseAuth.mockReturnValue({
      user: { id: 1, email: 'test@example.com', fullName: 'Test User', role: 'ROLE_USER' },
      loading: false,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
    } as any);

    render(
      <MemoryRouter>
        <ProtectedRoute>
          <TestComponent />
        </ProtectedRoute>
      </MemoryRouter>
    );

    expect(screen.getByText('Protected Content')).toBeInTheDocument();
  });

  it('shows loading state when auth is loading', () => {
    mockUseAuth.mockReturnValue({
      user: null,
      loading: true,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
    } as any);

    render(
      <MemoryRouter>
        <ProtectedRoute>
          <TestComponent />
        </ProtectedRoute>
      </MemoryRouter>
    );

    expect(screen.getByText('Loading...')).toBeInTheDocument();
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });

  it('redirects to login when user is not authenticated', () => {
    mockUseAuth.mockReturnValue({
      user: null,
      loading: false,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
    } as any);

    render(
      <MemoryRouter>
        <ProtectedRoute>
          <TestComponent />
        </ProtectedRoute>
      </MemoryRouter>
    );

    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });
});
