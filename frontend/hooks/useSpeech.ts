import { useCallback, useEffect, useRef, useState } from "react";

/**
 * Web Speech API hook for voice input (STT) and voice output (TTS).
 *
 * Wraps both `SpeechRecognition` (microphone input) and
 * `speechSynthesis` (text-to-speech output) with graceful degradation
 * when either API is unavailable in the browser.
 *
 * @returns Hook state and control functions for voice I/O.
 */
export function useSpeech() {
  const [isListening, setIsListening] = useState(false);
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [transcript, setTranscript] = useState("");
  const [voiceSupported, setVoiceSupported] = useState(false);
  const [ttsSupported, setTtsSupported] = useState(false);

  const recognitionRef = useRef<any>(null);
  const utteranceRef = useRef<SpeechSynthesisUtterance | null>(null);

  // ─── Feature detection ────────────────────────────────────────────────

  useEffect(() => {
    const SpeechRecognitionAPI: any =
      (window as any).SpeechRecognition ??
      (window as any).webkitSpeechRecognition;

    if (SpeechRecognitionAPI) {
      setVoiceSupported(true);
      const recognition = new SpeechRecognitionAPI();
      recognition.continuous = false;
      recognition.interimResults = false;
      // Do not fix the language — let the browser detect it
      recognition.lang = "";

      recognition.onresult = (event: any) => {
        const text = event.results[0]?.[0]?.transcript ?? "";
        setTranscript(text);
        setIsListening(false);
      };

      recognition.onerror = () => setIsListening(false);
      recognition.onend = () => setIsListening(false);

      recognitionRef.current = recognition;
    }

    if (typeof window !== "undefined" && "speechSynthesis" in window) {
      setTtsSupported(true);
    }

    return () => {
      recognitionRef.current?.abort();
      window.speechSynthesis?.cancel();
    };
  }, []);

  // ─── Voice input (STT) ────────────────────────────────────────────────

  const startListening = useCallback(() => {
    if (!recognitionRef.current || isListening) return;
    setTranscript("");
    setIsListening(true);
    try {
      recognitionRef.current.start();
    } catch {
      setIsListening(false);
    }
  }, [isListening]);

  const stopListening = useCallback(() => {
    recognitionRef.current?.stop();
    setIsListening(false);
  }, []);

  // ─── Voice output (TTS) ───────────────────────────────────────────────

  /**
   * Speaks the given text using the browser's speech synthesis engine.
   * Attempts to match a voice for the detected language.
   *
   * @param text     Text to speak
   * @param language ISO 639-1 language code (e.g. "es", "fr", "en")
   */
  const speak = useCallback(
    (text: string, language: string = "en") => {
      if (!ttsSupported || !text) return;

      window.speechSynthesis.cancel(); // Stop any ongoing speech

      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = language;
      utterance.rate = 0.95;
      utterance.pitch = 1.0;

      // Try to find a voice matching the language
      const voices = window.speechSynthesis.getVoices();
      const match = voices.find(
        (v) =>
          v.lang.startsWith(language) ||
          v.lang.toLowerCase() === language.toLowerCase()
      );
      if (match) utterance.voice = match;

      utterance.onstart = () => setIsSpeaking(true);
      utterance.onend = () => setIsSpeaking(false);
      utterance.onerror = () => setIsSpeaking(false);

      utteranceRef.current = utterance;
      window.speechSynthesis.speak(utterance);
    },
    [ttsSupported]
  );

  const stopSpeaking = useCallback(() => {
    window.speechSynthesis?.cancel();
    setIsSpeaking(false);
  }, []);

  return {
    isListening,
    isSpeaking,
    transcript,
    voiceSupported,
    ttsSupported,
    startListening,
    stopListening,
    speak,
    stopSpeaking,
  };
}
