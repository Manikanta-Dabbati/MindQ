import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect } from 'vitest';
import Landing from '../Landing';

describe('Landing Page', () => {
  it('renders the hero section with brand name', () => {
    render(
      <MemoryRouter>
        <Landing />
      </MemoryRouter>
    );

    // MindQ appears in nav and footer
    const mindqElements = screen.getAllByText(/MindQ/);
    expect(mindqElements.length).toBeGreaterThan(0);

    // Hero section text
    expect(screen.getByText(/Start Learning Free/)).toBeInTheDocument();
    expect(screen.getByText(/AI-Powered Learning Platform/)).toBeInTheDocument();
  });

  it('renders the "How It Works" section', () => {
    render(
      <MemoryRouter>
        <Landing />
      </MemoryRouter>
    );

    expect(screen.getByText(/How It Works/)).toBeInTheDocument();
  });

  it('renders login and register links', () => {
    render(
      <MemoryRouter>
        <Landing />
      </MemoryRouter>
    );

    const signInLinks = screen.getAllByText(/Sign In/i);
    expect(signInLinks.length).toBeGreaterThan(0);

    const getStartedLinks = screen.getAllByText(/Get Started/i);
    expect(getStartedLinks.length).toBeGreaterThan(0);
  });

  it('renders the features section', () => {
    render(
      <MemoryRouter>
        <Landing />
      </MemoryRouter>
    );

    expect(screen.getByText(/Knowledge Vault/)).toBeInTheDocument();
    expect(screen.getByText(/AI-Powered MCQs/)).toBeInTheDocument();
  });
});
