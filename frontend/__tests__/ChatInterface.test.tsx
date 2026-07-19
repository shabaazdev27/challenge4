import { render, screen } from "@testing-library/react";
import ChatInterface from "../components/ChatInterface";

describe("ChatInterface", () => {
  it("renders correctly", () => {
    render(
      <ChatInterface
        messages={[]}
        isLoading={false}
        currentLocation="GATE_A"
        onLocationChange={() => {}}
        onSendMessage={() => {}}
        onSpeakMessage={() => {}}
        isSpeaking={false}
        isListening={false}
        onStartListening={() => {}}
        onStopListening={() => {}}
        voiceSupported={false}
        ttsSupported={false}
        transcript=""
      />,
    );
    expect(screen.getByText(/AI Navigator/i)).toBeInTheDocument();
  });
});
