import { useCallback, useRef, useState } from "react";
import { v4 as uuidv4 } from "uuid";
import {
  sendChatMessage,
  ChatResponse,
  RouteResult,
  CrowdState,
} from "@/lib/api";

export interface Message {
  id: string;
  role: "user" | "assistant";
  content: string;
  language?: string;
  route?: RouteResult;
  congestionWarning?: string;
  timestamp: Date;
}

/**
 * Chat state management hook.
 *
 * Maintains the message history, loading state, session ID, and the latest
 * route + crowd state for map rendering. Connects to the backend via the
 * `sendChatMessage` API client.
 */
export function useChat() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [currentRoute, setCurrentRoute] = useState<RouteResult | null>(null);
  const [crowdState, setCrowdState] = useState<CrowdState>({});
  const [currentLanguage, setCurrentLanguage] = useState<string>("en");

  // Session ID persisted in memory for the lifetime of the page
  const sessionIdRef = useRef<string>(uuidv4());

  // ─── Send message ─────────────────────────────────────────────────────

  const sendMessage = useCallback(
    async (userText: string, currentLocation: string = "GATE_A") => {
      if (!userText.trim() || isLoading) return;

      const userMessage: Message = {
        id: uuidv4(),
        role: "user",
        content: userText.trim(),
        timestamp: new Date(),
      };

      setMessages((prev) => [...prev, userMessage]);
      setIsLoading(true);

      try {
        const response: ChatResponse = await sendChatMessage({
          message: userText.trim(),
          sessionId: sessionIdRef.current,
          currentLocation,
        });

        const assistantMessage: Message = {
          id: uuidv4(),
          role: "assistant",
          content: response.narration ?? "I found your route — please check the map.",
          language: response.language,
          route: response.route ?? undefined,
          congestionWarning: response.congestionWarning ?? undefined,
          timestamp: new Date(),
        };

        setMessages((prev) => [...prev, assistantMessage]);

        // Update map state
        if (response.route) setCurrentRoute(response.route);
        if (response.crowdState) setCrowdState(response.crowdState);
        if (response.language) setCurrentLanguage(response.language);

        // Keep sessionId consistent
        if (response.sessionId) {
          sessionIdRef.current = response.sessionId;
        }

        return response;
      } catch (err) {
        const errMessage: Message = {
          id: uuidv4(),
          role: "assistant",
          content:
            "Sorry, I'm having trouble connecting right now. Please try again in a moment.",
          timestamp: new Date(),
        };
        setMessages((prev) => [...prev, errMessage]);
        console.error("Chat error:", err);
        return null;
      } finally {
        setIsLoading(false);
      }
    },
    [isLoading]
  );

  // ─── Clear history ────────────────────────────────────────────────────

  const clearChat = useCallback(() => {
    setMessages([]);
    setCurrentRoute(null);
    setCrowdState({});
  }, []);

  return {
    messages,
    isLoading,
    currentRoute,
    crowdState,
    currentLanguage,
    sendMessage,
    clearChat,
  };
}
