import { render, screen } from '@testing-library/react';
import CrowdIndicator from '../components/CrowdIndicator';

describe('CrowdIndicator', () => {
  it('renders correctly', () => {
    render(<CrowdIndicator congestionLevel={0.5} />);
    expect(screen.getByText(/Crowd Level/i)).toBeInTheDocument();
  });
});
