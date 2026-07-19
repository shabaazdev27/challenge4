import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "StadiumMate — FIFA World Cup 2026 Navigation",
  description:
    "AI-powered multilingual navigation and crowd-aware concierge for MetLife Stadium. Get real-time route guidance in your language.",
  manifest: "/manifest.json",
  keywords: [
    "FIFA World Cup 2026",
    "MetLife Stadium",
    "stadium navigation",
    "multilingual",
    "AI assistant",
  ],
  authors: [{ name: "StadiumMate" }],
  openGraph: {
    title: "StadiumMate — FIFA World Cup 2026",
    description:
      "Find your way around MetLife Stadium with AI-powered multilingual navigation.",
    type: "website",
  },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#003087",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" dir="ltr">
      <head>
        <link rel="apple-touch-icon" href="/icons/icon-192.png" />
        <meta name="apple-mobile-web-app-capable" content="yes" />
        <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
      </head>
      <body>
        {children}
      </body>
    </html>
  );
}
