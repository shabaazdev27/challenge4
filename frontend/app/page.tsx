"use client";

import React, { useState, useEffect, useCallback } from "react";
import StadiumMap from "@/components/StadiumMap";
import ChatInterface from "@/components/ChatInterface";
import CrowdIndicator from "@/components/CrowdIndicator";
import GameScoreboard from "@/components/GameScoreboard";
import LiveViewMap from "@/components/LiveViewMap";
import { useChat } from "@/hooks/useChat";
import { useSpeech } from "@/hooks/useSpeech";
import { fetchCrowdState } from "@/lib/api";

/**
 * StadiumMate — Main Application Page
 *
 * Split-panel layout:
 * - Left/top: SVG stadium map with animated route + crowd heat overlay
 * - Right/bottom: AI chat panel with multilingual messages + voice controls
 *
 * On mobile (<768px) the map collapses to the top and the chat fills the bottom.
 */
export default function HomePage() {
  const [currentLocation, setCurrentLocation] = useState("GATE_A");
  const [showDemoControls, setShowDemoControls] = useState(false);
  const [viewMode, setViewMode] = useState<"map" | "live">("map");

  const {
    messages,
    isLoading,
    currentRoute,
    crowdState,
    sendMessage,
  } = useChat();

  const {
    isListening,
    isSpeaking,
    transcript,
    voiceSupported,
    ttsSupported,
    startListening,
    stopListening,
    speak,
    stopSpeaking,
  } = useSpeech();

  // Poll crowd state every 15 seconds for map heat overlay updates
  useEffect(() => {
    let active = true;
    const poll = async () => {
      if (!active) return;
      // Crowd state is also returned per chat response — this is a background refresh
      await fetchCrowdState().catch(() => {});
    };
    const interval = setInterval(poll, 15000);
    return () => {
      active = false;
      clearInterval(interval);
    };
  }, []);

  const handleSendMessage = useCallback(
    async (text: string) => {
      const response = await sendMessage(text, currentLocation);
      // Auto-speak the assistant's response
      if (response?.narration && ttsSupported) {
        speak(response.narration, response.language ?? "en");
      }
    },
    [sendMessage, currentLocation, speak, ttsSupported]
  );

  const handleSpeakMessage = useCallback(
    (text: string, language: string) => {
      if (isSpeaking) {
        stopSpeaking();
      } else {
        speak(text, language);
      }
    },
    [isSpeaking, speak, stopSpeaking]
  );

  return (
    <div className="app-layout">
      {/* ── Header ─────────────────────────────────────────────── */}
      <header className="app-header" role="banner">
        <div className="app-logo">
          <div className="app-logo-icon" aria-hidden="true">🏟️</div>
          <span>
            Stadium<span className="app-logo-accent">Mate</span>
          </span>
          <span
            style={{
              fontSize: "0.65rem",
              color: "var(--text-muted)",
              fontWeight: 400,
              marginLeft: 4,
              marginTop: 2,
            }}
          >
            FIFA WC 2026
          </span>
        </div>

        <nav
          aria-label="Header actions"
          style={{ display: "flex", alignItems: "center", gap: 12 }}
        >
          {/* Demo controls toggle */}
          <button
            onClick={() => setShowDemoControls((v) => !v)}
            style={{
              background: showDemoControls
                ? "rgba(255,215,0,0.12)"
                : "transparent",
              border: "1px solid rgba(255,215,0,0.2)",
              borderRadius: 8,
              padding: "4px 10px",
              fontSize: "0.72rem",
              color: "var(--color-accent)",
              cursor: "pointer",
              fontWeight: 600,
            }}
            aria-label={showDemoControls ? "Hide demo controls" : "Show demo controls"}
            aria-pressed={showDemoControls}
          >
            ⚡ Demo
          </button>

          <span
            className="badge badge--live"
            role="status"
            aria-label="System live"
          >
            ● Live
          </span>
        </nav>
      </header>

      {/* ── Main split panel ───────────────────────────────────── */}
      <main className="main-panel" role="main">
        {/* Left: Stadium Map & Live View */}
        <div style={{ display: "flex", flexDirection: "column", overflow: "hidden", position: "relative" }}>
          <div style={{ position: "absolute", top: 16, right: 16, zIndex: 10, display: "flex", gap: 8 }}>
            <button
              onClick={() => setViewMode("map")}
              style={{
                background: viewMode === "map" ? "var(--color-accent)" : "rgba(10,14,26,0.8)",
                color: viewMode === "map" ? "#000" : "#fff",
                border: "1px solid var(--color-accent)",
                padding: "6px 12px", borderRadius: 8, fontSize: "0.8rem", cursor: "pointer", fontWeight: "bold"
              }}
            >
              2D Map
            </button>
            <button
              onClick={() => setViewMode("live")}
              style={{
                background: viewMode === "live" ? "var(--color-accent)" : "rgba(10,14,26,0.8)",
                color: viewMode === "live" ? "#000" : "#fff",
                border: "1px solid var(--color-accent)",
                padding: "6px 12px", borderRadius: 8, fontSize: "0.8rem", cursor: "pointer", fontWeight: "bold"
              }}
            >
              Live AR View
            </button>
          </div>
          
          {viewMode === "map" ? (
            <StadiumMap route={currentRoute} crowdState={crowdState} />
          ) : (
            <LiveViewMap apiKey={process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY || ""} />
          )}
        </div>

        {/* Right: Chat + Crowd panel */}
        <div
          style={{ display: "flex", flexDirection: "column", overflow: "hidden", height: "100%" }}
        >
          {/* Fan Gamification Scoreboard */}
          <GameScoreboard userId="demo_fan_123" />

          <ChatInterface
            messages={messages}
            isLoading={isLoading}
            currentLocation={currentLocation}
            onLocationChange={setCurrentLocation}
            onSendMessage={handleSendMessage}
            onSpeakMessage={handleSpeakMessage}
            isSpeaking={isSpeaking}
            isListening={isListening}
            onStartListening={startListening}
            onStopListening={stopListening}
            voiceSupported={voiceSupported}
            ttsSupported={ttsSupported}
            transcript={transcript}
          />

          {/* Crowd density + demo controls */}
          <CrowdIndicator
            crowdState={crowdState}
            showControls={showDemoControls}
          />
        </div>
      </main>
    </div>
  );
}
