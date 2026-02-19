import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import App from './App';

describe('App', () => {
  it('renders the sidebar brand name', () => {
    render(<App />);
    expect(screen.getByText('Perf Tester')).toBeInTheDocument();
  });

  it('renders the Dashboard nav link', () => {
    render(<App />);
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
  });
});
