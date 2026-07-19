import { renderHook, act } from "@testing-library/react";
import { useSpeech } from "../hooks/useSpeech";

describe("useSpeech", () => {
  let mockRecognition: any;
  let mockSpeechSynthesis: any;
  let mockUtteranceConstructor: any;

  beforeEach(() => {
    mockRecognition = {
      start: jest.fn(),
      stop: jest.fn(),
      abort: jest.fn(),
      onresult: null,
      onerror: null,
      onend: null,
    };

    mockSpeechSynthesis = {
      speak: jest.fn().mockImplementation((utterance) => {
        if (utterance.onstart) utterance.onstart();
        if (utterance.onend) utterance.onend();
      }),
      cancel: jest.fn(),
      getVoices: jest.fn().mockReturnValue([
        { lang: "en-US", name: "Voice En" },
        { lang: "es-ES", name: "Voice Es" },
      ]),
    };

    mockUtteranceConstructor = jest.fn().mockImplementation((text) => ({
      text,
      lang: "",
      rate: 1.0,
      pitch: 1.0,
      voice: null,
      onstart: null,
      onend: null,
      onerror: null,
    }));

    // Mock window properties
    (window as any).SpeechRecognition = jest.fn().mockImplementation(() => mockRecognition);
    (window as any).speechSynthesis = mockSpeechSynthesis;
    (window as any).SpeechSynthesisUtterance = mockUtteranceConstructor;
  });

  afterEach(() => {
    delete (window as any).SpeechRecognition;
    delete (window as any).speechSynthesis;
    delete (window as any).SpeechSynthesisUtterance;
  });

  it("detects features and starts/stops speech recognition correctly", () => {
    const { result } = renderHook(() => useSpeech());
    expect(result.current.voiceSupported).toBe(true);
    expect(result.current.ttsSupported).toBe(true);

    // Test start listening
    act(() => {
      result.current.startListening();
    });
    expect(result.current.isListening).toBe(true);
    expect(mockRecognition.start).toHaveBeenCalled();

    // Trigger onresult
    act(() => {
      mockRecognition.onresult({
        results: [[{ transcript: "hello there" }]],
      });
    });
    expect(result.current.isListening).toBe(false);
    expect(result.current.transcript).toBe("hello there");

    // Test stop listening
    act(() => {
      result.current.stopListening();
    });
    expect(mockRecognition.stop).toHaveBeenCalled();
  });

  it("handles speech synthesis successfully", () => {
    const { result } = renderHook(() => useSpeech());

    act(() => {
      result.current.speak("Welcome to StadiumMate", "es");
    });

    expect(mockSpeechSynthesis.cancel).toHaveBeenCalled();
    expect(mockSpeechSynthesis.speak).toHaveBeenCalled();

    // Test stop speaking
    act(() => {
      result.current.stopSpeaking();
    });
    expect(mockSpeechSynthesis.cancel).toHaveBeenCalledTimes(2);
  });
});
