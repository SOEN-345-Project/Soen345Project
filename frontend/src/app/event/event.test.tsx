import React from "react";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import type { MockedFunction } from "jest-mock";


jest.mock("next/navigation", () => ({
    useRouter: () => ({ push: mockPush }),
}));

jest.mock("@/lib/axios", () => ({
    getAllEvents:  jest.fn(),
    searchEvents: jest.fn(),
    filterEvents: jest.fn(),
}));
jest.mock("../reservation/reservation", () => ({
    __esModule: true,
    default: ({ event, onClose }: { event: any; onClose: () => void }) => {
        if (!event) return null;
        return <div data-testid="mock-reservation-modal"><button onClick={onClose}>Close</button></div>;
    },
}));

import { getAllEvents, searchEvents, filterEvents } from "@/lib/axios";
import EventsPage from "./page";

const mockPush = jest.fn();
const mockGetAllEvents = getAllEvents as MockedFunction<typeof getAllEvents>;
const mockSearchEvents = searchEvents as MockedFunction<typeof searchEvents>;
const mockFilterEvents = filterEvents as MockedFunction<typeof filterEvents>;

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
];

beforeEach(() => {
    jest.clearAllMocks();
    mockGetAllEvents.mockResolvedValue(MOCK_EVENTS);
    mockSearchEvents.mockResolvedValue([]);
    mockFilterEvents.mockResolvedValue([]);

    Storage.prototype.getItem = jest.fn((key: string) => {
        switch (key) {
            case "token":
                return "mock-token";
            case "isAdmin":
                return null;
            default:
                return null;
        }
    });
    Storage.prototype.clear = jest.fn();
});



test("Checking that the page loads correctly", async () => {
    render(<EventsPage />);
    await waitFor(() =>
    {expect(screen.getByText("Available Events")).toBeInTheDocument();
        expect(screen.getByText("Available Events")).toBeInTheDocument();
        expect(screen.getByTitle("Start date")).toBeInTheDocument();
        expect(screen.getByTitle("End date")).toBeInTheDocument();
    });
});

test("Shows no events message when empty", async () => {
    mockGetAllEvents.mockResolvedValue([]);
    render(<EventsPage />);

    const message = await screen.findByText(/no events found/i);

    expect(message).toBeInTheDocument();
});

test("redirects to /signin when there is no user that has sign in", async () => {
    Storage.prototype.getItem = jest.fn(() => null);
    render(<EventsPage />);
    await waitFor(() => expect(mockPush).toHaveBeenCalledWith("/signin"));
});

test("logout clears the session and sends the user to /signin", async () => {
    render(<EventsPage />);
    await screen.findByText("Jazz Night");
    fireEvent.click(screen.getByRole("button", { name: /logout/i }));
    expect(sessionStorage.clear).toHaveBeenCalled();
    expect(mockPush).toHaveBeenCalledWith("/signin");
});

test("Reservation sends the user to /reservation", async () => {
    render(<EventsPage />);
    await screen.findByText("Jazz Night");
    fireEvent.click(screen.getByRole("button", { name: /reservation/i }));
    expect(mockPush).toHaveBeenCalledWith("/reservation");
});

test("shows event cards once the fetch completes (Happy path)", async () => {
    render(<EventsPage />);
    await screen.findByText("Jazz Night");
    expect(screen.getByText("Champions Cup")).toBeInTheDocument();
    expect(screen.getByText("Jazz Night")).toBeInTheDocument();
});

test("shows an error message when the fetch fails", async () => {
    mockGetAllEvents.mockRejectedValue(new Error("Network error"));
    render(<EventsPage />);
    await screen.findByText(/network error/i);
    expect(screen.queryByText("Champions Cup")).not.toBeInTheDocument();
    expect(screen.queryByText("Jazz Night")).not.toBeInTheDocument();
});

test("Trims the keyword before calling the search API", async () => {
    mockSearchEvents.mockResolvedValue([MOCK_EVENTS[0]]);
    render(<EventsPage />);
    await screen.findByText("Jazz Night");

    await userEvent.type(screen.getByPlaceholderText(/search by keyword/i), "  jazz  ");
    fireEvent.click(screen.getByRole("button", { name: /^search$/i }));

    await waitFor(() => expect(searchEvents).toHaveBeenCalledWith("jazz"));
});

test("Search for a particular Event using the search bar (Happy Path)", async() =>{
    mockSearchEvents.mockResolvedValue([MOCK_EVENTS[1]]);
    render(<EventsPage />);
    await screen.findByText("Jazz Night");

    await userEvent.type(screen.getByPlaceholderText(/search by keyword/i), "champion");
    fireEvent.click(screen.getByRole("button", { name: /search$/i }));
    await waitFor(() => expect(searchEvents).toHaveBeenCalledWith("champion"));
    await waitFor(() =>expect(screen.queryByText(/^Jazz Night$/)).not.toBeInTheDocument());
    expect(screen.getByText("Champions Cup")).toBeInTheDocument();
})

test("Search for a particular Event using the search bar and fails due to wrong search", async() =>{
    mockSearchEvents.mockResolvedValue([]);
    render(<EventsPage />);
    await screen.findByText("Jazz Night");

    await userEvent.type(screen.getByPlaceholderText(/search by keyword/i), "some");
    fireEvent.click(screen.getByRole("button", { name: /search$/i }));
    await waitFor(() => expect(searchEvents).toHaveBeenCalledWith("some"));
    await waitFor(() =>expect(screen.queryByText(/^Jazz Night$/)).not.toBeInTheDocument());
    await waitFor(() => expect(screen.queryByText(/^Champions Cup$/)).not.toBeInTheDocument());
})

test("Search for a particular Event using the search bar and fails due to network error", async() =>{
    mockSearchEvents.mockRejectedValueOnce(new Error("Network error"));
    render(<EventsPage />);
    await screen.findByText("Jazz Night");

    await userEvent.type(screen.getByPlaceholderText(/search by keyword/i), "some");
    fireEvent.click(screen.getByRole("button", { name: /search$/i }));
    await waitFor(() => expect(searchEvents).toHaveBeenCalledWith("some"));
    await screen.findByText(/network error/i);
})

test("does not call the search API when the keyword is blank", async () => {
    render(<EventsPage />);
    await screen.findByText("Jazz Night");
    fireEvent.click(screen.getByRole("button", { name: /^search$/i }));
    expect(searchEvents).not.toHaveBeenCalled();
});

test("Call the filter API when the keyword is blank", async () => {
    render(<EventsPage />);
    await screen.findByText("Jazz Night");
    fireEvent.click(screen.getByRole("button", { name: /^filter$/i }));
    expect(mockFilterEvents).toHaveBeenCalled();
});

test("passes the selected category to the filter API (Happy path)", async () => {
    mockFilterEvents.mockResolvedValue([MOCK_EVENTS[0]]);
    render(<EventsPage />);
    await screen.findByText("Jazz Night");

    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "1" } });
    fireEvent.click(screen.getByRole("button", { name: /^filter$/i }));

    await waitFor(() =>
        expect(filterEvents).toHaveBeenCalledWith(expect.objectContaining({ categoryId: 1 }))
    );
});

test("passes the selected category to the filter API which is invalid", async () => {
    mockFilterEvents.mockResolvedValue([]);
    render(<EventsPage />);
    await screen.findByText("Jazz Night");

    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "1" } });
    fireEvent.click(screen.getByRole("button", { name: /^filter$/i }));

    await waitFor(() =>
        expect(filterEvents).not.toHaveBeenCalledWith(expect.not.objectContaining({ categoryId: 1 }))
    );
});

test("clicking a category tab only shows events from that category", async () => {
    render(<EventsPage />);
    await screen.findByText("Jazz Night");

    fireEvent.click(screen.getByRole("button", { name: "Sports" }));

    expect(screen.getByText("Champions Cup")).toBeInTheDocument();
    expect(screen.queryByText("Jazz Night")).not.toBeInTheDocument();
});

test("reset brings back all events after filtering", async () => {
    mockFilterEvents.mockResolvedValue([MOCK_EVENTS[0]]);
    render(<EventsPage />);
    await screen.findByText("Jazz Night");

    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "1" } });
    fireEvent.click(screen.getByRole("button", { name: /^filter$/i }));
    await waitFor(() => expect(filterEvents).toHaveBeenCalled());

    fireEvent.click(screen.getByRole("button", { name: /reset/i }));

    expect(screen.getByText("Jazz Night")).toBeInTheDocument();
    expect(screen.getByText("Champions Cup")).toBeInTheDocument();
});

test("reset brings back all events after filterixcedfdeng", async () => {
    mockFilterEvents.mockRejectedValueOnce(new Error("Network Error"))
    render(<EventsPage />);
    await screen.findByText("Jazz Night");

    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "1" } });
    fireEvent.click(screen.getByRole("button", { name: /^filter$/i }));
    await waitFor(() => expect(filterEvents).toHaveBeenCalled());
    await screen.findByText(/network error/i);
});

test("clicking the arrow button opens the reservation modal", async () => {
    mockGetAllEvents.mockResolvedValue(MOCK_EVENTS);
    render(<EventsPage />);
    await screen.findByText("Jazz Night");

    fireEvent.click(screen.getAllByLabelText("Reserve tickets")[0]);

    expect(screen.getByTestId("mock-reservation-modal")).toBeInTheDocument();
});

