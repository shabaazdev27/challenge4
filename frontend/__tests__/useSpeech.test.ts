import { renderHook, act } from '@testing-library/react-hooks';
import { useSpeech } from '../hooks/useSpeech';

describe('useSpeech', () => {
  it('initializes with default values', () => {
    const { result } = renderHook(() => useSpeech());
    expect(result.current.isListening).toBe(false);
    expect(result.current.isSpeaking).toBe(false);
  });
});
