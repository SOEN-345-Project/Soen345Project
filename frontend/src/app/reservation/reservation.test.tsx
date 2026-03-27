import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import ReservationsList from "./page";
import * as axiosLib from "@/lib/axios";
import EventsPage from "@/app/event/page";

jest.mock("next/navigation", () => ({
    useRouter: () => ({ push: mockPush }),
}));

jest.mock("@/lib/axios", () => ({
    getAllReservations: jest.fn(),
    cancelReservation:  jest.fn(),
}));

const mockPush = jest.fn();

const MOCK_RESERVATIONS = [
    {
        reservationId: 1,
        eventId: 1,
        eventTitle: "Jazz Night",
        eventDate: "2026-04-05T20:00:00",
        eventLocation: "Théâtre Rialto",
        quantity: 2,
        status: "CONFIRMED",
        createdAt: "2026-03-01T10:00:00",
    },
    {
        reservationId: 2,
        eventId: 2,
        eventTitle: "Comedy Festival",
        eventDate: "2026-04-18T19:00:00",
        eventLocation: "Bell Centre",
        quantity: 1,
        status: "CANCELLED",
        createdAt: "2026-03-02T10:00:00",
    },
];

beforeEach(() => {
    jest.clearAllMocks();
    (getAllReservations as jest.Mock).mockResolvedValue(MOCK_RESERVATIONS);
    (cancelReservation as jest.Mock).mockResolvedValue({});
    Storage.prototype.getItem = jest.fn((key) => (key === "token" ? "mock-token" : null));
    Storage.prototype.clear  = jest.fn();
});

const { getAllReservations, cancelReservation } = axiosLib as any;

describe("Reservation List and cancelling them", () => {
    it("redirects to /signin when there is no user that has sign in", async () => {
        Storage.prototype.getItem = jest.fn(() => null);
        render(<ReservationsList />);
        await waitFor(() => expect(mockPush).toHaveBeenCalledWith("/signin"));
    });

    it("renders the reservations list page", async () => {
        render(<ReservationsList />);
        await screen.findByText("Jazz Night");
        expect(screen.getByText("Reservations")).toBeInTheDocument();
        expect(screen.getByText("Event")).toBeInTheDocument();
        expect(screen.getByText("Logout")).toBeInTheDocument();
    });

    it("shows reservations after load", async () => {
        render(<ReservationsList />);
        await screen.findByText("Jazz Night");
        expect(screen.getByText("Comedy Festival")).toBeInTheDocument();
    });

    it("cancels a reservation and updates status", async () => {
        render(<ReservationsList />);
        await screen.findByText("Jazz Night");

        fireEvent.click(screen.getByText("Cancel reservation"));

        await waitFor(() =>
            expect(screen.getByText("CANCELLED")).toBeInTheDocument()
        );
    });
    it("shows error when cancel fails", async () => {
        (cancelReservation as jest.Mock).mockRejectedValueOnce(new Error("Network error"));
        render(<ReservationsList />);
        await screen.findByText("Jazz Night");
        fireEvent.click(screen.getByText("Cancel reservation"));
        await screen.findByText("Network error");
    });

    it("shows error when load fails", async () => {
        (getAllReservations as jest.Mock).mockRejectedValueOnce(new Error("Network error"));
        render(<ReservationsList />);
        await screen.findByText("Network error");
    });

    it("logout clears the session and sends the user to /signin", async () => {
        render(<ReservationsList />);
        await screen.findByText("Jazz Night");
        fireEvent.click(screen.getByRole("button", { name: /logout/i }));
        expect(sessionStorage.clear).toHaveBeenCalled();
        expect(mockPush).toHaveBeenCalledWith("/signin");
    });

    it("Pressing Event button sends the user to /event", async () => {
        render(<ReservationsList />);
        await screen.findByText("Jazz Night");
        fireEvent.click(screen.getByRole("button", { name: /event/i }));
        expect(mockPush).toHaveBeenCalledWith("/event");
    });

});