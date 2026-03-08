import React from "react";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";

jest.mock("next/navigation", () => ({
    useRouter: () => ({ push: mockPush }),
}));

jest.mock("@/lib/axios", () => ({
    getAllEvents:  jest.fn(),
    searchEvents: jest.fn(),
    filterEvents: jest.fn(),
}));

import { getAllEvents, searchEvents, filterEvents } from "@/lib/axios";
import EventsPage from "./page";

const mockPush = jest.fn();

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
    },
];

beforeEach(() => {
    jest.clearAllMocks();
    (getAllEvents as jest.Mock).mockResolvedValue(MOCK_EVENTS);
    (searchEvents as jest.Mock).mockResolvedValue([]);
    (filterEvents as jest.Mock).mockResolvedValue([]);

    Storage.prototype.getItem = jest.fn((key) => (key === "token" ? "mock-token" : null));
    Storage.prototype.clear = jest.fn();
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

test("shows event cards once the fetch completes (Happy path)", async () => {
    render(<EventsPage />);
    await screen.findByText("Jazz Night");
    expect(screen.getByText("Champions Cup")).toBeInTheDocument();
    expect(screen.getByText("Jazz Night")).toBeInTheDocument();
});

test("shows an error message when the fetch fails", async () => {
    (getAllEvents as jest.Mock).mockRejectedValue(new Error("Network error"));
    render(<EventsPage />);
    await screen.findByText(/network error/i);
});

test("Trims the keyword before calling the search API", async () => {
    (searchEvents as jest.Mock).mockResolvedValue([MOCK_EVENTS[0]]);
    render(<EventsPage />);
    await screen.findByText("Jazz Night");

    await userEvent.type(screen.getByPlaceholderText(/search by keyword/i), "  jazz  ");
    fireEvent.click(screen.getByRole("button", { name: /^search$/i }));

    await waitFor(() => expect(searchEvents).toHaveBeenCalledWith("jazz"));
});

test("Search for a particular Event using the search bar (Happy Path)", async() =>{
    (searchEvents as jest.Mock).mockResolvedValue([MOCK_EVENTS[1]]);
    render(<EventsPage />);
    await screen.findByText("Jazz Night");

    await userEvent.type(screen.getByPlaceholderText(/search by keyword/i), "champion");
    fireEvent.click(screen.getByRole("button", { name: /search$/i }));
    await waitFor(() => expect(searchEvents).toHaveBeenCalledWith("champion"));
    await waitFor(() =>expect(screen.queryByText(/^Jazz Night$/)).not.toBeInTheDocument());
    expect(screen.getByText("Champions Cup")).toBeInTheDocument();
})

test("Search for a particular Event using the search bar and fails due to wrong search", async() =>{
    (searchEvents as jest.Mock).mockResolvedValue([]);
    render(<EventsPage />);
    await screen.findByText("Jazz Night");

    await userEvent.type(screen.getByPlaceholderText(/search by keyword/i), "some");
    fireEvent.click(screen.getByRole("button", { name: /search$/i }));
    await waitFor(() => expect(searchEvents).toHaveBeenCalledWith("some"));
    await waitFor(() =>expect(screen.queryByText(/^Jazz Night$/)).not.toBeInTheDocument());
    await waitFor(() => expect(screen.queryByText(/^Champions Cup$/)).not.toBeInTheDocument());
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
    expect(filterEvents as jest.Mock).toHaveBeenCalled();
});

test("passes the selected category to the filter API (Happy path)", async () => {
    (filterEvents as jest.Mock).mockResolvedValue([MOCK_EVENTS[0]]);
    render(<EventsPage />);
    await screen.findByText("Jazz Night");

    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "1" } });
    fireEvent.click(screen.getByRole("button", { name: /^filter$/i }));

    await waitFor(() =>
        expect(filterEvents).toHaveBeenCalledWith(expect.objectContaining({ categoryId: 1 }))
    );
});
test("passes the selected category to the filter API which is invalid", async () => {
    (filterEvents as jest.Mock).mockResolvedValue([]);
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
    (filterEvents as jest.Mock).mockResolvedValue([MOCK_EVENTS[0]]);
    render(<EventsPage />);
    await screen.findByText("Jazz Night");

    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "1" } });
    fireEvent.click(screen.getByRole("button", { name: /^filter$/i }));
    await waitFor(() => expect(filterEvents).toHaveBeenCalled());

    fireEvent.click(screen.getByRole("button", { name: /reset/i }));

    expect(screen.getByText("Jazz Night")).toBeInTheDocument();
    expect(screen.getByText("Champions Cup")).toBeInTheDocument();
});