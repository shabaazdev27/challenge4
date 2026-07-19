import { render, screen, fireEvent } from "@testing-library/react";
import ChatInterface from "../components/ChatInterface";
import { Message } from "@/hooks/useChat";

beforeAll(() => {
  window.HTMLElement.prototype.scrollIntoView = jest.fn();
});

describe("ChatInterface", () => {
  let mockSendMessage: jest.Mock;
  let mockLocationChange: jest.Mock;
  let mockSpeakMessage: jest.Mock;
  let mockStartListening: jest.Mock;
  let mockStopListening: jest.Mock;

  beforeEach(() => {
    mockSendMessage = jest.fn();
    mockLocationChange = jest.fn();
    mockSpeakMessage = jest.fn();
    mockStartListening = jest.fn();
    mockStopListening = jest.fn();
  });

  it("renders welcome screen when message history is empty", () => {
    render(
      <ChatInterface
        messages={[]}
        isLoading={false}
        currentLocation="GATE_A"
        onLocationChange={mockLocationChange}
        onSendMessage={mockSendMessage}
        onSpeakMessage={mockSpeakMessage}
        isSpeaking={false}
        isListening={false}
        onStartListening={mockStartListening}
        onStopListening={mockStopListening}
        voiceSupported={true}
        ttsSupported={true}
        transcript=""
      />
    );

    expect(screen.getByText(/Welcome to StadiumMate/i)).toBeInTheDocument();
    
    // Test suggestion chip click
    const chip = screen.getByLabelText("Suggestion: Where's the nearest accessible restroom?");
    expect(chip).toBeInTheDocument();
    fireEvent.click(chip);

    // Verify text is filled in textarea (which has role textbox / name: Type your navigation question)
    const textarea = screen.getByPlaceholderText("Ask in any language — text or voice") as HTMLTextAreaElement;
    expect(textarea.value).toBe("Where's the nearest accessible restroom?");
  });

  it("renders messages, route summaries, congestion warnings, and turn-by-turn directions", () => {
    const mockMessages: Message[] = [
      {
        id: "msg1",
        role: "user",
        content: "Where is the restroom?",
        timestamp: new Date(),
      },
      {
        id: "msg2",
        role: "assistant",
        content: "Here is your route to REST_E.",
        language: "en",
        congestionWarning: "High traffic near GATE_A",
        route: {
          steps: [
            { nodeId: "GATE_A", nodeName: "Gate A", instruction: "Start at Gate A", edgeType: "walk", distanceFromPrevious: 0 },
            { nodeId: "REST_E", nodeName: "East Restroom", instruction: "Walk to East Restroom", edgeType: "walk", distanceFromPrevious: 45 },
          ],
          totalDistance: 100,
          estimatedMinutes: 1,
          nodePath: ["GATE_A", "REST_E"],
          rerouted: false,
        },
        timestamp: new Date(),
      }
    ];

    render(
      <ChatInterface
        messages={mockMessages}
        isLoading={false}
        currentLocation="GATE_A"
        onLocationChange={mockLocationChange}
        onSendMessage={mockSendMessage}
        onSpeakMessage={mockSpeakMessage}
        isSpeaking={false}
        isListening={false}
        onStartListening={mockStartListening}
        onStopListening={mockStopListening}
        voiceSupported={true}
        ttsSupported={true}
        transcript=""
      />
    );

    // Verify messages are rendered
    expect(screen.getByText("Where is the restroom?")).toBeInTheDocument();
    expect(screen.getByText("Here is your route to REST_E.")).toBeInTheDocument();
    
    // Verify route stats
    expect(screen.getByText("100m")).toBeInTheDocument();
    expect(screen.getByText("45m")).toBeInTheDocument();
    
    // Verify congestion warning
    expect(screen.getByText("High traffic near GATE_A")).toBeInTheDocument();

    // Verify turn-by-turn directions
    expect(screen.getByText("🧭 Turn-by-Turn Directions")).toBeInTheDocument();
    expect(screen.getByText("Start at Gate A")).toBeInTheDocument();
    expect(screen.getByText("Walk to East Restroom")).toBeInTheDocument();

    // Test speak button click
    const speakBtn = screen.getByRole("button", { name: "Read this message aloud" });
    fireEvent.click(speakBtn);
    expect(mockSpeakMessage).toHaveBeenCalledWith("Here is your route to REST_E.", "en");
  });

  it("handles input submit via button and Enter key", () => {
    render(
      <ChatInterface
        messages={[]}
        isLoading={false}
        currentLocation="GATE_A"
        onLocationChange={mockLocationChange}
        onSendMessage={mockSendMessage}
        onSpeakMessage={mockSpeakMessage}
        isSpeaking={false}
        isListening={false}
        onStartListening={mockStartListening}
        onStopListening={mockStopListening}
        voiceSupported={true}
        ttsSupported={true}
        transcript=""
      />
    );

    const textarea = screen.getByPlaceholderText("Ask in any language — text or voice") as HTMLTextAreaElement;
    
    // Type text
    fireEvent.change(textarea, { target: { value: "Gate B" } });
    
    // Press Enter to send
    fireEvent.keyDown(textarea, { key: "Enter", code: "Enter", charCode: 13 });
    expect(mockSendMessage).toHaveBeenCalledWith("Gate B");

    // Type text again
    fireEvent.change(textarea, { target: { value: "Gate C" } });
    
    // Click send button
    const sendBtn = screen.getByRole("button", { name: "Send message" });
    fireEvent.click(sendBtn);
    expect(mockSendMessage).toHaveBeenCalledWith("Gate C");
  });

  it("handles voice mic clicks and status displays", () => {
    const { rerender } = render(
      <ChatInterface
        messages={[]}
        isLoading={false}
        currentLocation="GATE_A"
        onLocationChange={mockLocationChange}
        onSendMessage={mockSendMessage}
        onSpeakMessage={mockSpeakMessage}
        isSpeaking={false}
        isListening={false}
        onStartListening={mockStartListening}
        onStopListening={mockStopListening}
        voiceSupported={true}
        ttsSupported={true}
        transcript=""
      />
    );

    // Mic click starts listening
    const micBtn = screen.getByRole("button", { name: "Start voice input" });
    fireEvent.click(micBtn);
    expect(mockStartListening).toHaveBeenCalled();

    // Rerender with isListening: true
    rerender(
      <ChatInterface
        messages={[]}
        isLoading={false}
        currentLocation="GATE_A"
        onLocationChange={mockLocationChange}
        onSendMessage={mockSendMessage}
        onSpeakMessage={mockSpeakMessage}
        isSpeaking={false}
        isListening={true}
        onStartListening={mockStartListening}
        onStopListening={mockStopListening}
        voiceSupported={true}
        ttsSupported={true}
        transcript=""
      />
    );

    // Status shows Listening...
    expect(screen.getByText("Listening…")).toBeInTheDocument();

    // Mic click stops listening
    const stopMicBtn = screen.getByRole("button", { name: "Stop voice input" });
    fireEvent.click(stopMicBtn);
    expect(mockStopListening).toHaveBeenCalled();
  });

  it("handles location dropdown change", () => {
    render(
      <ChatInterface
        messages={[]}
        isLoading={false}
        currentLocation="GATE_A"
        onLocationChange={mockLocationChange}
        onSendMessage={mockSendMessage}
        onSpeakMessage={mockSpeakMessage}
        isSpeaking={false}
        isListening={false}
        onStartListening={mockStartListening}
        onStopListening={mockStopListening}
        voiceSupported={true}
        ttsSupported={true}
        transcript=""
      />
    );

    const select = screen.getByLabelText("Your current location in the stadium");
    fireEvent.change(select, { target: { value: "GATE_B" } });
    expect(mockLocationChange).toHaveBeenCalledWith("GATE_B");
  });
});
