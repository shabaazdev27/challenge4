import { renderHook } from '@testing-library/react';
import { useChat } from '../hooks/useChat';

describe('useChat', () => {
  it('initializes with default values', () => {
    const { result } = renderHook(() => useChat());
    expect(result.current.isLoading).toBe(false);
    expect(result.current.messages).toEqual([]);
  });
});
