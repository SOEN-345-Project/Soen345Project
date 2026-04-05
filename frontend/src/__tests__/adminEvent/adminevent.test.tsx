import React from "react";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { getEveryEvents, cancelEvent, createEvent, updateEvent } from "@/lib/axios";
import AdminEventsPage from "../../app/adminEvent/page";

const mockPush = jest.fn();

jest.mock("next/navigation", () => ({
    useRouter: () => ({ push: mockPush }),
}));

jest.mock("@/lib/axios", () => ({
    getEveryEvents: jest.fn(),
    cancelEvent:    jest.fn(),
    createEvent:    jest.fn(),
    updateEvent:    jest.fn(),
}));

jest.mock("../../app/reservation/reservation", () => ({
    __esModule: true,
    default: () => null,
}));

jest.mock("@/app/adminEvent/eventAddModify", () => ({
    __esModule: true,
    default: ({ mode, event, onClose }: { mode: string; event: any; onClose: () => void }) => (
        <div data-testid="mock-event-form-modal">
            <span data-testid="modal-mode">{mode}</span>
            <span data-testid="modal-event-title">{event?.title ?? "no-event"}</span>
            <button onClick={onClose}>Close Modal</button>
        </div>
    ),
}));



const MOCK_EVENTS = [
    {
        id: 1,
        title: "Jazz Night",
        description: "Live jazz",
        categoryName: "Concert",
        locationName: "Bell Centre",
        city: "Montreal",
        eventDate: "2025-08-15T20:00:00",
        totalTickets: 200,
        status: "ACTIVE",
    },
    {
        id: 2,
        title: "Champions Cup",
        description: "Soccer final",
        categoryName: "Sports",
        locationName: "Olympic Stadium",
        city: "Montreal",
        eventDate: "2025-09-01T15:00:00",
        totalTickets: 1000,
        status: "ACTIVE",
    },
    {
        id: 3,
        title: "Old Show",
        description: "Cancelled event",
        categoryName: "Concert",
        locationName: "MTELUS",
        city: "Montreal",
        eventDate: "2025-07-01T18:00:00",
        totalTickets: 500,
        status: "CANCELLED",
    },
];

beforeEach(() => {
    jest.clearAllMocks();
    (getEveryEvents as jest.Mock).mockResolvedValue(MOCK_EVENTS);
    (cancelEvent as jest.Mock).mockResolvedValue({});
    (createEvent as jest.Mock).mockResolvedValue({});
    (updateEvent as jest.Mock).mockResolvedValue({});
    Storage.prototype.getItem = jest.fn((key: string) => {
        switch (key) {
            case "token":
                return "mock-token";
            case "isAdmin":
                return "true";
            default:
                return null;
        }
    });Storage.prototype.clear = jest.fn();
});

test("renders the overall page", async () => {
    render(<AdminEventsPage />);
    await waitFor(() => expect(screen.getByText("All Events")).toBeInTheDocument());

});
test("Shows the loader when the events are fetched", async () => {
    (getEveryEvents as jest.Mock).mockImplementation(() => new Promise(() => {}));
    render(<AdminEventsPage />);
    expect(screen.getByText("Loading...")).toBeInTheDocument();
    expect(screen.getByText("Loading events...")).toBeInTheDocument();
});

test("shows event cards once the fetch completes", async () => {
    render(<AdminEventsPage />);
    await screen.findByText("Jazz Night");
    expect(screen.getByText("Champions Cup")).toBeInTheDocument();
    expect(screen.getByText("Jazz Night")).toBeInTheDocument();
   expect(screen.getAllByRole("button", { name: /modify/i })[0]).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: /cancel/i })[0]).toBeInTheDocument();

});

test("shows no events message when fetch returns empty", async () => {
    (getEveryEvents as jest.Mock).mockResolvedValue([]);
    render(<AdminEventsPage />);
    const message = await screen.findByText(/no events found/i);
    expect(message).toBeInTheDocument();
});

test("redirects to /signin when no auth token is found", async () => {
    Storage.prototype.getItem = jest.fn(() => null);
    render(<AdminEventsPage />);
    await waitFor(() => expect(mockPush).toHaveBeenCalledWith("/signin"));
});

test("logout clears session and redirects to /signin", async () => {
    render(<AdminEventsPage />);
    await screen.findByText("Jazz Night");
    fireEvent.click(screen.getByRole("button", { name: /logout/i }));
    expect(sessionStorage.clear).toHaveBeenCalled();
    expect(mockPush).toHaveBeenCalledWith("/signin");
});

test("shows an error message when fetch fails", async () => {
    (getEveryEvents as jest.Mock).mockRejectedValue(new Error("Network error"));
    render(<AdminEventsPage />);
    await screen.findByText(/network error/i);
    expect(screen.queryByText("Jazz Night")).not.toBeInTheDocument();
});

test("clicking a category tab only shows events from that category", async () => {
    render(<AdminEventsPage />);
    await screen.findByText("Jazz Night");
    fireEvent.click(screen.getByRole("button", { name: "Sports" }));
    expect(screen.getByText("Champions Cup")).toBeInTheDocument();
    expect(screen.queryByText("Jazz Night")).not.toBeInTheDocument();
});

test("clicking All tab shows all events", async () => {
    render(<AdminEventsPage />);
    await screen.findByText("Jazz Night");
    fireEvent.click(screen.getByRole("button", { name: "Sports" }));
    fireEvent.click(screen.getByRole("button", { name: "All" }));
    expect(screen.getByText("Jazz Night")).toBeInTheDocument();
    expect(screen.getByText("Champions Cup")).toBeInTheDocument();
});

test("moving between different tabs", async () => {
    render(<AdminEventsPage />);
    await screen.findByText("Jazz Night");
    fireEvent.click(screen.getByRole("button", { name: "Sports" }));
    fireEvent.click(screen.getByRole("button", { name: "All" }));
    fireEvent.click(screen.getByRole("button", { name: "Concert" }));
    expect(screen.getByText("Old Show")).toBeInTheDocument();
    expect(screen.getByText("Jazz Night")).toBeInTheDocument();
});



test("clicking Modify button opens modal in modify mode with correct event", async () => {
    render(<AdminEventsPage />);
    await screen.findByText("Jazz Night");
    fireEvent.click(screen.getAllByRole("button", { name: /modify/i })[0]);
    expect(screen.getByTestId("mock-event-form-modal")).toBeInTheDocument();
    expect(screen.getByTestId("modal-mode").textContent).toBe("modify");
    expect(screen.getByTestId("modal-event-title").textContent).toBe("Jazz Night");
});

test("clicking Add Event button opens modal in add mode with no event", async () => {
    render(<AdminEventsPage />);
    await screen.findByText("Jazz Night");
    fireEvent.click(screen.getByRole("button", { name: /add event/i }));
    expect(screen.getByTestId("mock-event-form-modal")).toBeInTheDocument();
    expect(screen.getByTestId("modal-mode").textContent).toBe("add");
    expect(screen.getByTestId("modal-event-title").textContent).toBe("no-event");
});

test("closing the modal calls loadAll to refresh events", async () => {
    render(<AdminEventsPage />);
    await screen.findByText("Jazz Night");
    fireEvent.click(screen.getByRole("button", { name: /add event/i }));
    fireEvent.click(screen.getByRole("button", { name: /close modal/i }));
    await waitFor(() => expect(getEveryEvents).toHaveBeenCalled);
});

test("cancel event calls cancelEvent API and refreshes list", async () => {
    render(<AdminEventsPage />);
    await screen.findByText("Jazz Night");
    fireEvent.click(screen.getAllByRole("button", { name: /cancel/i })[0]);
    await waitFor(() => expect(cancelEvent).toHaveBeenCalled);
    await waitFor(() => expect(getEveryEvents).toHaveBeenCalled);
});

test("cancel event shows error when API fails", async () => {
    (cancelEvent as jest.Mock).mockRejectedValueOnce(new Error("Cancel failed"));
    render(<AdminEventsPage />);
    await screen.findByText("Jazz Night");
    fireEvent.click(screen.getAllByRole("button", { name: /cancel/i })[0]);
    await screen.findByText(/cancel failed/i);
});