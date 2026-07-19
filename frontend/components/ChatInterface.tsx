/**
 * ChatInterface — AI Navigation Assistant Chat Panel
 *
 * Right-panel chat UI for StadiumMate. Features:
 * - Animated message bubbles with role-specific styling
 * - Language detection badge on assistant replies
 * - Turn-by-turn route steps with distance annotations
 * - Congestion warnings with prominent alert styling
 * - Voice input (STT) and text-to-speech controls
 * - Multilingual suggestion chips on welcome screen
 * - Accessible: ARIA roles, labels, keyboard navigation, WCAG 2.1 AA
 *
 * Uses only vanilla CSS via globals.css design-system tokens.
 */

"use client";

import React, { useEffect, useRef, useState, KeyboardEvent } from "react";
import { Message } from "@/hooks/useChat";
import { RouteResult } from "@/lib/api";

// ── Suggestion chips shown on the welcome screen ──────────────────────────────
const SUGGESTIONS = [
  { text: "Where's the nearest accessible restroom?", icon: "♿" },
  { text: "How do I get to Section 214?",             icon: "🗺️" },
  { text: "¿Dónde está la comida?",                   icon: "🍕" },
  { text: "Je cherche les ascenseurs",                 icon: "🛗" },
  { text: "Where is the merchandise store?",           icon: "🛍️" },
  { text: "Which gate is least crowded right now?",    icon: "📊" },
];

const STEP_ICONS: Record<string, string> = {
  walk:     "👟",
  elevator: "🛗",
  ramp:     "↗",
};

const LANG_LABELS: Record<string, string> = {
  en: "🇬🇧 English", es: "🇪🇸 Español", fr: "🇫🇷 Français",
  pt: "🇧🇷 Português", de: "🇩🇪 Deutsch", ar: "🇸🇦 العربية",
  zh: "🇨🇳 中文",     ja: "🇯🇵 日本語",   ko: "🇰🇷 한국어",
  it: "🇮🇹 Italiano", nl: "🇳🇱 Nederlands", ru: "🇷🇺 Русский",
};

// ── Location options ──────────────────────────────────────────────────────────
const LOCATIONS = [
  { id: "GATE_A",  label: "Gate A (East)" },
  { id: "GATE_B",  label: "Gate B (North)" },
  { id: "GATE_C",  label: "Gate C (West)" },
  { id: "GATE_D",  label: "Gate D (South)" },
  { id: "SEC_114", label: "Section 114" },
  { id: "SEC_120", label: "Section 120" },
  { id: "SEC_214", label: "Section 214" },
  { id: "SEC_320", label: "Section 320" },
];

// ── Props interface ───────────────────────────────────────────────────────────
interface ChatInterfaceProps {
  messages: Message[];
  isLoading: boolean;
  currentLocation: string;
  onLocationChange: (loc: string) => void;
  onSendMessage: (text: string) => void;
  onSpeakMessage: (text: string, language: string) => void;
  isSpeaking: boolean;
  isListening: boolean;
  onStartListening: () => void;
  onStopListening: () => void;
  voiceSupported: boolean;
  ttsSupported: boolean;
  transcript: string;
}

// ── Route Steps sub-component ─────────────────────────────────────────────────
function RouteSteps({ route }: { route: RouteResult }) {
  if (!route.steps || route.steps.length === 0) return null;

  return (
    <div className="route-panel" aria-label="Turn-by-turn navigation steps">
      <div className="route-panel-title" aria-hidden="true">
        🧭 Turn-by-Turn Directions
      </div>
      {route.steps.map((step, i) => {
        const isFirst = i === 0;
        const isLast  = i === route.steps.length - 1;
        const icon    = isFirst ? "📍" : isLast ? "🏁" : (STEP_ICONS[step.edgeType] ?? "👟");

        return (
          <React.Fragment key={step.nodeId}>
            <div
              className={`route-step ${isLast ? "route-step--destination" : ""}`}
              aria-label={`Step ${i + 1}: ${step.instruction}${step.distanceFromPrevious > 0 ? `, ${Math.round(step.distanceFromPrevious)} metres` : ""}`}
            >
              <span className="step-icon" aria-hidden="true">{icon}</span>
              <span style={{ flex: 1 }}>{step.instruction}</span>
              {step.distanceFromPrevious > 0 && (
                <span className="step-distance" aria-hidden="true">
                  {Math.round(step.distanceFromPrevious)}m
                </span>
              )}
            </div>
            {!isLast && <div className="step-connector" aria-hidden="true" />}
          </React.Fragment>
        );
      })}
    </div>
  );
}

// ── Main ChatInterface component ──────────────────────────────────────────────
export default function ChatInterface({
  messages,
  isLoading,
  currentLocation,
  onLocationChange,
  onSendMessage,
  onSpeakMessage,
  isSpeaking,
  isListening,
  onStartListening,
  onStopListening,
  voiceSupported,
  ttsSupported,
  transcript,
}: ChatInterfaceProps) {
  const [inputText, setInputText]     = useState("");
  const bottomRef                     = useRef<HTMLDivElement>(null);
  const textareaRef                   = useRef<HTMLTextAreaElement>(null);

  // ── Auto-scroll to latest message ────────────────────────────────────────
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isLoading]);

  // ── Fill input from speech transcript ────────────────────────────────────
  useEffect(() => {
    if (transcript) setInputText(transcript);
  }, [transcript]);

  // ── Auto-resize textarea ──────────────────────────────────────────────────
  useEffect(() => {
    const ta = textareaRef.current;
    if (!ta) return;
    ta.style.height = "auto";
    ta.style.height = `${Math.min(ta.scrollHeight, 120)}px`;
  }, [inputText]);

  const handleSend = () => {
    if (!inputText.trim() || isLoading) return;
    onSendMessage(inputText.trim());
    setInputText("");
    textareaRef.current?.focus();
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleMicClick = () => {
    if (isListening) onStopListening();
    else onStartListening();
  };

  const handleSuggestion = (text: string) => {
    setInputText(text);
    textareaRef.current?.focus();
  };

  return (
    <div className="chat-panel">
      {/* ── Chat Panel header ──────────────────────────────────────── */}
      <div className="chat-panel-header" aria-label="Chat panel header">
        <div className="chat-panel-header-title">
          <span aria-hidden="true">💬</span>
          <span>AI Navigator</span>
        </div>
        <div className="chat-panel-header-meta" aria-live="polite">
          {isListening && (
            <span className="chat-status chat-status--listening" role="status" aria-label="Microphone active, listening">
              <span className="chat-status-dot" aria-hidden="true" />
              Listening…
            </span>
          )}
          {isSpeaking && (
            <span className="chat-status chat-status--speaking" role="status" aria-label="Speaking response">
              <span className="chat-status-dot" aria-hidden="true" />
              Speaking…
            </span>
          )}
          {isLoading && !isListening && !isSpeaking && (
            <span className="chat-status chat-status--thinking" role="status" aria-label="AI is thinking">
              <span className="chat-status-dot" aria-hidden="true" />
              Thinking…
            </span>
          )}
        </div>
      </div>

      {/* ── Message list ──────────────────────────────────────────── */}
      <div
        className="chat-messages"
        role="log"
        aria-live="polite"
        aria-label="Conversation history"
        aria-atomic="false"
      >
        {messages.length === 0 ? (
          /* Welcome / empty state */
          <div className="chat-welcome" aria-label="Welcome screen">
            <div className="chat-welcome-icon" aria-hidden="true">🏟️</div>
            <h2>Welcome to StadiumMate</h2>
            <p>
              Ask me anything in your language — I&apos;ll guide you with
              real-time crowd-aware navigation.
            </p>
            <div
              className="chat-suggestions"
              role="list"
              aria-label="Suggested questions"
            >
              {SUGGESTIONS.map((s) => (
                <button
                  key={s.text}
                  className="suggestion-chip"
                  role="listitem"
                  onClick={() => handleSuggestion(s.text)}
                  aria-label={`Suggestion: ${s.text}`}
                >
                  <span aria-hidden="true">{s.icon}</span>
                  {s.text}
                </button>
              ))}
            </div>
          </div>
        ) : (
          /* Messages */
          messages.map((msg) => (
            <div
              key={msg.id}
              className={`message-row message-row--${msg.role}`}
              aria-label={`${msg.role === "user" ? "You" : "StadiumMate"}: ${msg.content}`}
            >
              {/* Avatar */}
              <div
                className={`message-avatar message-avatar--${msg.role}`}
                aria-hidden="true"
              >
                {msg.role === "user" ? "👤" : "🤖"}
              </div>

              {/* Bubble + extras */}
              <div className="message-content-col">
                {/* Language badge (assistant only) */}
                {msg.role === "assistant" && msg.language && (
                  <div
                    className="message-lang-badge"
                    aria-label={`Responded in: ${LANG_LABELS[msg.language] ?? msg.language}`}
                  >
                    {LANG_LABELS[msg.language] ?? msg.language}
                  </div>
                )}

                {/* Message bubble */}
                <div className={`message-bubble message-bubble--${msg.role}`}>
                  {msg.content}
                </div>

                {/* Route stats card */}
                {msg.route && (
                  <div
                    className="message-route-summary"
                    aria-label={`Route: ${Math.round(msg.route.totalDistance)} metres, approximately ${msg.route.estimatedMinutes} minutes`}
                  >
                    <span className="route-stat">
                      <span aria-hidden="true">📍</span>
                      <span>{Math.round(msg.route.totalDistance)}m</span>
                      <span className="route-stat-label">distance</span>
                    </span>
                    <span className="route-stat">
                      <span aria-hidden="true">⏱</span>
                      <span>{msg.route.estimatedMinutes} min</span>
                      <span className="route-stat-label">walk</span>
                    </span>
                    <span className="route-stat">
                      <span aria-hidden="true">🔢</span>
                      <span>{msg.route.steps.length}</span>
                      <span className="route-stat-label">steps</span>
                    </span>
                  </div>
                )}

                {/* Congestion alert */}
                {msg.congestionWarning && (
                  <div
                    className="crowd-warning"
                    role="alert"
                    aria-live="assertive"
                    aria-label={`Congestion warning: ${msg.congestionWarning}`}
                  >
                    <span aria-hidden="true">⚠️</span>
                    <span>{msg.congestionWarning}</span>
                  </div>
                )}

                {/* Turn-by-turn route */}
                {msg.route && <RouteSteps route={msg.route} />}

                {/* TTS speak button */}
                {msg.role === "assistant" && ttsSupported && (
                  <button
                    className={`chat-btn chat-btn--speak ${isSpeaking ? "speaking" : ""}`}
                    style={{ marginTop: 8, width: 34, height: 34, fontSize: "0.85rem" }}
                    onClick={() => onSpeakMessage(msg.content, msg.language ?? "en")}
                    aria-label={isSpeaking ? "Stop speaking" : "Read this message aloud"}
                    title={isSpeaking ? "Stop" : "Listen"}
                  >
                    {isSpeaking ? "⏹" : "🔊"}
                  </button>
                )}
              </div>
            </div>
          ))
        )}

        {/* Typing / thinking indicator */}
        {isLoading && (
          <div
            className="message-row"
            aria-live="polite"
            aria-label="StadiumMate is thinking"
          >
            <div className="message-avatar message-avatar--assistant" aria-hidden="true">
              🤖
            </div>
            <div className="message-bubble message-bubble--assistant">
              <div className="typing-indicator" aria-hidden="true">
                <div className="typing-dot" />
                <div className="typing-dot" />
                <div className="typing-dot" />
              </div>
            </div>
          </div>
        )}

        <div ref={bottomRef} aria-hidden="true" />
      </div>

      {/* ── Input area ────────────────────────────────────────────── */}
      <div className="chat-input-area">
        {/* Location selector */}
        <div className="location-selector">
          <label htmlFor="location-select" style={{ flexShrink: 0 }}>
            📍 You are at:
          </label>
          <select
            id="location-select"
            className="location-select"
            value={currentLocation}
            onChange={(e) => onLocationChange(e.target.value)}
            aria-label="Your current location in the stadium"
          >
            {LOCATIONS.map((loc) => (
              <option key={loc.id} value={loc.id}>
                {loc.label}
              </option>
            ))}
          </select>
        </div>

        {/* Input row */}
        <div className="chat-input-row" role="search" aria-label="Message input area">
          {/* Voice input */}
          {voiceSupported && (
            <button
              className={`chat-btn chat-btn--mic ${isListening ? "listening" : ""}`}
              onClick={handleMicClick}
              aria-label={isListening ? "Stop voice input" : "Start voice input"}
              title={isListening ? "Stop listening" : "Speak your question"}
            >
              {isListening ? "⏹" : "🎤"}
            </button>
          )}

          {/* Text textarea */}
          <textarea
            ref={textareaRef}
            className="chat-input"
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={
              isListening
                ? "🎤 Listening — speak your question…"
                : "Ask in any language — text or voice"
            }
            aria-label="Type your navigation question"
            aria-multiline="true"
            rows={1}
            disabled={isLoading}
          />

          {/* Send button */}
          <button
            className="chat-btn chat-btn--send"
            onClick={handleSend}
            disabled={!inputText.trim() || isLoading}
            aria-label="Send message"
            title="Send"
          >
            ➤
          </button>
        </div>

        {/* Hint text */}
        <p className="chat-input-hint" aria-hidden="true">
          Press <kbd>Enter</kbd> to send · <kbd>Shift+Enter</kbd> for new line
        </p>
      </div>
    </div>
  );
}
