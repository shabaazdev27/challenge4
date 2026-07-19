/**
 * GameScoreboard — Fan Gamification Panel
 *
 * Displays the current fan's score alongside a mini-leaderboard of top fans.
 * Fetches from /api/game/score/:userId. Falls back to simulated data on error.
 * Uses only vanilla CSS classes from globals.css design system (no Tailwind).
 */

"use client";

import React, { useEffect, useState, useCallback } from "react";

interface ScoreEntry {
  userId: string;
  displayName: string;
  score: number;
  avatar: string;
  rank?: number;
}

interface GameScoreboardProps {
  userId: string;
}

// ── Simulated leaderboard fallback (demo-safe) ────────────────────────────────
const DEMO_LEADERBOARD: ScoreEntry[] = [
  { userId: "fan_alpha",   displayName: "⚡ AlphaFan",   score: 4850, avatar: "🦁" },
  { userId: "fan_beta",    displayName: "🔥 GoalKing",   score: 3920, avatar: "🐯" },
  { userId: "fan_gamma",   displayName: "🏆 SuperSupp",  score: 3110, avatar: "🦊" },
  { userId: "demo_fan_123", displayName: "🎯 You",        score: 1240, avatar: "⭐" },
  { userId: "fan_delta",   displayName: "🎺 StadiumPro", score: 980,  avatar: "🐺" },
];

const API_BASE =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/** Fetch a user's score; throws on network/parse error */
async function fetchUserScore(userId: string): Promise<number> {
  const res = await fetch(`${API_BASE}/api/game/score/${userId}`, {
    headers: { "Content-Type": "application/json" },
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const data = (await res.json()) as { score?: number };
  if (typeof data.score !== "number") throw new Error("Invalid payload");
  return data.score;
}

/** Merge the live score into the leaderboard and re-rank */
function buildLeaderboard(userId: string, liveScore: number): ScoreEntry[] {
  const board = DEMO_LEADERBOARD.map((e) =>
    e.userId === userId ? { ...e, score: liveScore } : { ...e }
  );
  board.sort((a, b) => b.score - a.score);
  return board.map((e, i) => ({ ...e, rank: i + 1 }));
}

export default function GameScoreboard({ userId }: GameScoreboardProps) {
  const [leaderboard, setLeaderboard] = useState<ScoreEntry[]>(
    buildLeaderboard(userId, 1240)
  );
  const [isExpanded, setIsExpanded] = useState(false);
  const [pulse, setPulse] = useState(false);

  const userEntry = leaderboard.find((e) => e.userId === userId) ?? leaderboard[0];

  const refresh = useCallback(async () => {
    try {
      const score = await fetchUserScore(userId);
      setLeaderboard(buildLeaderboard(userId, score));
      setPulse(true);
      setTimeout(() => setPulse(false), 600);
    } catch {
      // Keep demo data — no error surfaced to user
    }
  }, [userId]);

  // Refresh on mount and every 30 s
  useEffect(() => {
    refresh();
    const interval = setInterval(refresh, 30_000);
    return () => clearInterval(interval);
  }, [refresh]);

  const rankMedal = (rank: number) => {
    if (rank === 1) return "🥇";
    if (rank === 2) return "🥈";
    if (rank === 3) return "🥉";
    return `#${rank}`;
  };

  return (
    <div className="scoreboard-card" aria-label="Fan Gamification Scoreboard">
      {/* ── Header row ──────────────────────────────────────────────── */}
      <div className="scoreboard-header">
        <div className="scoreboard-title">
          <span aria-hidden="true">🏅</span>
          <span>Fan Arena</span>
        </div>

        <div
          className={`scoreboard-score-pill ${pulse ? "scoreboard-score-pulse" : ""}`}
          aria-label={`Your score: ${userEntry.score.toLocaleString()} points`}
        >
          <span className="scoreboard-score-number">
            {userEntry.score.toLocaleString()}
          </span>
          <span className="scoreboard-score-unit">pts</span>
        </div>
      </div>

      {/* ── User rank summary ──────────────────────────────────────── */}
      <div className="scoreboard-my-rank" aria-label={`Your rank: ${userEntry.rank}`}>
        <span className="scoreboard-rank-badge">{rankMedal(userEntry.rank ?? 99)}</span>
        <div className="scoreboard-rank-info">
          <span className="scoreboard-rank-name">{userEntry.displayName}</span>
          <span className="scoreboard-rank-label">
            Rank {userEntry.rank} of {leaderboard.length} · Catch mascots to climb!
          </span>
        </div>
        <button
          className="scoreboard-toggle-btn"
          onClick={() => setIsExpanded((v) => !v)}
          aria-label={isExpanded ? "Hide leaderboard" : "Show full leaderboard"}
          aria-expanded={isExpanded}
        >
          {isExpanded ? "▲" : "▼"}
        </button>
      </div>

      {/* ── Leaderboard ───────────────────────────────────────────── */}
      {isExpanded && (
        <div
          className="scoreboard-leaderboard"
          role="list"
          aria-label="Leaderboard top fans"
        >
          {leaderboard.slice(0, 5).map((entry) => {
            const isMe = entry.userId === userId;
            return (
              <div
                key={entry.userId}
                className={`scoreboard-entry ${isMe ? "scoreboard-entry--me" : ""}`}
                role="listitem"
                aria-label={`${rankMedal(entry.rank ?? 99)} ${entry.displayName}: ${entry.score.toLocaleString()} points`}
              >
                <span className="scoreboard-entry-rank" aria-hidden="true">
                  {rankMedal(entry.rank ?? 99)}
                </span>
                <span className="scoreboard-entry-avatar" aria-hidden="true">
                  {entry.avatar}
                </span>
                <span className="scoreboard-entry-name">{entry.displayName}</span>
                <span className="scoreboard-entry-score">
                  {entry.score.toLocaleString()}
                </span>
              </div>
            );
          })}
        </div>
      )}

      {/* ── Progress bar toward next rank ─────────────────────────── */}
      <div className="scoreboard-progress-area" aria-label="Progress to next rank">
        <div className="scoreboard-progress-label">
          <span>Next rank in {Math.max(0, (leaderboard[(userEntry.rank ?? 2) - 2]?.score ?? 9999) - userEntry.score).toLocaleString()} pts</span>
        </div>
        <div className="scoreboard-progress-track" role="progressbar" aria-valuenow={Math.min(100, (userEntry.score / Math.max(1, leaderboard[0]?.score ?? 1)) * 100)} aria-valuemin={0} aria-valuemax={100} aria-label="Score progress towards next rank">
          <div
            className="scoreboard-progress-fill"
            style={{
              width: `${Math.min(100, (userEntry.score / Math.max(1, leaderboard[0]?.score ?? 1)) * 100)}%`,
            }}
          />
        </div>
      </div>
    </div>
  );
}
