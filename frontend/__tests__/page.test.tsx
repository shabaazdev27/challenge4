import { render, screen, fireEvent, act } from "@testing-library/react";
import HomePage from "../app/page";
import { useChat } from "@/hooks/useChat";
import { useSpeech } from "@/hooks/useSpeech";
import { fetchCrowdState } from "@/lib/api";

jest.mock("@/hooks/useChat");
jest.mock("@/hooks/useSpeech");
jest.mock("@/lib/api", () => ({
  fetchCrowdState: jest.fn(),
}));

// Mock child components to isolate HomePage test
jest.mock("@/components/StadiumMap", () => () => (
  <div data-testid="stadium-map-mock" />
));
jest.mock("@/components/ChatInterface", () => (props: any) => (
  <div data-testid="chat-interface-mock">
    <button onClick={() => props.onSendMessage("Where is the restroom?")}>Send Mock</button>
    <button onClick={() => props.onSpeakMessage("Go left", "en")}>Speak Mock</button>
    <select
      data-testid="loc-select"
      value={props.currentLocation}
      onChange={(e) => props.onLocationChange(e.target.value)}
    >
      <option value="GATE_B">Gate B</option>
    </select>
  </div>
));
jest.mock("@/components/CrowdIndicator", () => () => (
  <div data-testid="crowd-indicator-mock" />
));
jest.mock("@/components/GameScoreboard", () => () => (
  <div data-testid="game-scoreboard-mock" />
));
jest.mock("@/components/LiveViewMap", () => () => (
  <div data-testid="live-view-map-mock" />
));

describe("HomePage", () => {
  let mockSendMessage: jest.Mock;
  let mockSpeak: jest.Mock;
  let mockStopSpeaking: jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();

    mockSendMessage = jest.fn();
    mockSpeak = jest.fn();
    mockStopSpeaking = jest.fn();

    (useChat as jest.Mock).mockReturnValue({
      messages: [],
      isLoading: false,
      currentRoute: null,
      crowdState: {},
      sendMessage: mockSendMessage,
    });

    (useSpeech as jest.Mock).mockReturnValue({
      isListening: false,
      isSpeaking: false,
      transcript: "",
      voiceSupported: true,
      ttsSupported: true,
      startListening: jest.fn(),
      stopListening: jest.fn(),
      speak: mockSpeak,
      stopSpeaking: mockStopSpeaking,
    });

    (fetchCrowdState as jest.Mock).mockResolvedValue({});
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it("renders correctly and supports page view modes, location switching, and messages", async () => {
    const { container } = render(<HomePage />);
    expect(container).toBeInTheDocument();

    // Trigger crowd poll interval
    await act(async () => {
      jest.advanceTimersByTime(16000);
    });
    expect(fetchCrowdState).toHaveBeenCalled();

    // Toggle view mode to Live View
    const liveViewBtn = screen.getByText("Live AR View");
    act(() => {
      fireEvent.click(liveViewBtn);
    });
    expect(screen.getByTestId("live-view-map-mock")).toBeInTheDocument();

    // Toggle back to 2D Map
    const mapBtn = screen.getByText("2D Map");
    act(() => {
      fireEvent.click(mapBtn);
    });
    expect(screen.getByTestId("stadium-map-mock")).toBeInTheDocument();

    // Toggle demo controls
    const demoBtn = screen.getByRole("button", { name: "Show demo controls" });
    act(() => {
      fireEvent.click(demoBtn);
    });

    // Test location change
    const select = screen.getByTestId("loc-select");
    act(() => {
      fireEvent.change(select, { target: { value: "GATE_B" } });
    });

    // Test sending message
    mockSendMessage.mockResolvedValueOnce({
      narration: "Route to restroom found.",
      language: "en",
    });
    const sendBtn = screen.getByText("Send Mock");
    await act(async () => {
      fireEvent.click(sendBtn);
    });
    expect(mockSendMessage).toHaveBeenCalledWith("Where is the restroom?", "GATE_B");
    expect(mockSpeak).toHaveBeenCalledWith("Route to restroom found.", "en");

    // Test speaking / toggling TTS speak
    const speakBtn = screen.getByText("Speak Mock");
    act(() => {
      fireEvent.click(speakBtn);
    });
    expect(mockSpeak).toHaveBeenCalledWith("Go left", "en");

    // Now set isSpeaking to true to test stopSpeaking branch
    (useSpeech as jest.Mock).mockReturnValue({
      isListening: false,
      isSpeaking: true,
      transcript: "",
      voiceSupported: true,
      ttsSupported: true,
      startListening: jest.fn(),
      stopListening: jest.fn(),
      speak: mockSpeak,
      stopSpeaking: mockStopSpeaking,
    });
    render(<HomePage />);
    const speakBtn2 = screen.getAllByText("Speak Mock")[1];
    act(() => {
      fireEvent.click(speakBtn2);
    });
    expect(mockStopSpeaking).toHaveBeenCalled();
  });
});
