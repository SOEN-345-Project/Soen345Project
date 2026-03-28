// ReservationModal.test.tsx
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import ReservationModal from "./reservation";
import * as axiosLib from "@/lib/axios";
import "@testing-library/jest-dom";
jest.mock("@/lib/axios", () => ({
    createReservation: jest.fn(),
}));

const mockEvent = {
    id: 1,
    title: "Jazz Night",
    description: "A great jazz event",
    eventDate: "2026-04-05T20:00:00",
    categoryName: "Concert",
    locationName: "Théâtre Rialto",
    city: "Montreal",
    totalTickets: 10,
    status:  "ACTIVE",
};

const mockOnClose = jest.fn();
beforeEach(() => {
    jest.clearAllMocks();
    sessionStorage.setItem("token", "mock-token");
});

describe("Reservation Modal testing", () => {

    it("renders nothing when event is null", () => {
        const { queryByText } = render(
            <ReservationModal event={null} onClose={mockOnClose}  />
        );
        expect(screen.queryByText("Available")).not.toBeInTheDocument();
        expect(screen.queryByText("Date:")).not.toBeInTheDocument();
    });

    it("renders event details correctly (Happy path)", () => {
        render(<ReservationModal event={mockEvent} onClose={mockOnClose}  />);
        expect(screen.getByText("Jazz Night")).toBeInTheDocument();
        expect(screen.getByText("Concert")).toBeInTheDocument();
        expect(screen.getByText("A great jazz event")).toBeInTheDocument();
        expect(screen.getByText(/Théâtre Rialto/)).toBeInTheDocument();
        expect(screen.getByText(/10 tickets/)).toBeInTheDocument();
        expect(screen.getByText("1")).toBeInTheDocument();
    });

    it("increments and decrements ticket count", () => {
        render(<ReservationModal event={mockEvent} onClose={mockOnClose}  />);
        const increment = screen.getByText("+");
        const decrement = screen.getByText("−");

        fireEvent.click(increment);
        expect(screen.getByText("2")).toBeInTheDocument();

        fireEvent.click(decrement);
        expect(screen.getByText("1")).toBeInTheDocument();
    });

    it("does not decrement below the bare minimum of 1 ticket", () => {
        render(<ReservationModal event={mockEvent} onClose={mockOnClose}/>);
        const decrement = screen.getByText("−");
        fireEvent.click(decrement);
        expect(screen.getByText("1")).toBeInTheDocument();
    });

    it("does not increment above the maximum amount of tickets", () => {
        const limitedEvent = { ...mockEvent, totalTickets: 2 };
        render(<ReservationModal event={limitedEvent} onClose={mockOnClose} />);
        const increment = screen.getByText("+");
        fireEvent.click(increment);
        fireEvent.click(increment);
        expect(screen.getByText("2")).toBeInTheDocument();
    });

    it("calls onClose when the close button is clicked", () => {
        render(<ReservationModal event={mockEvent} onClose={mockOnClose} />);
        fireEvent.click(screen.getByText("✕"));
        expect(mockOnClose).toHaveBeenCalled();
        expect(screen.queryByText("Available")).not.toBeInTheDocument();
    });


    it("shows success state after the reservation is successfully processed (Happy Path)", async () => {
        (axiosLib.createReservation as jest.Mock).mockResolvedValueOnce({ reservationId:1, eventId:1,
            eventTitle: "Event Details",
            eventDate:"Localdatatime",
            eventLocation:"Location",
            quantity:2,
            status:'RESERVED',
            createdAt:'12/12/2026',});

        render(<ReservationModal event={mockEvent} onClose={mockOnClose}  />);
        fireEvent.click(screen.getByText("Reserve 1 ticket"));

        await waitFor(() => {
            expect(screen.getByText("Reservation confirmed!")).toBeInTheDocument();
        });
    });

    it("shows error state when reservation fails", async () => {
        (axiosLib.createReservation as jest.Mock).mockRejectedValueOnce(new Error("Lack of Tickets"));

        const {getByText} = render(<ReservationModal event={mockEvent} onClose={mockOnClose} />);
        fireEvent.click(screen.getByText("+"));
        expect(screen.getByText("2")).toBeInTheDocument();
        fireEvent.click(screen.getByText("Reserve 2 tickets"));

        await waitFor(() => {
            expect(screen.getByText("Lack of Tickets")).toBeInTheDocument();
        });
    });

    it("shows error state when bad request", async () => {
        (axiosLib.createReservation as jest.Mock).mockRejectedValueOnce({
            response: {
                status: 400,
                data: "Bad request",
            },
            message: "Request failed with status code 400",
        });

        render(<ReservationModal event={mockEvent} onClose={mockOnClose} />);
        fireEvent.click(screen.getByText("Reserve 1 ticket"));

        await waitFor(() => {
            expect(screen.getByText("Request failed with status code 400")).toBeInTheDocument();
        });
    });

    it("calls onClose when Done button is clicked after success (Happy path with extra steps)", async () => {
        (axiosLib.createReservation as jest.Mock).mockResolvedValueOnce({ reservationId:1, eventId:1,
            eventTitle: "Event Details",
            eventDate:"Localdatatime",
            eventLocation:"Location",
            quantity:2,
            status:'RESERVED',
            createdAt:'12/12/2026',});

        render(<ReservationModal event={mockEvent} onClose={mockOnClose}  />);
        fireEvent.click(screen.getByText("Reserve 1 ticket"));

        await waitFor(() => screen.getByText("Done"));
        fireEvent.click(screen.getByText("Done"));
        expect(mockOnClose).toHaveBeenCalled();
    });

});