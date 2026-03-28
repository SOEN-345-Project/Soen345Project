import {fireEvent, render, screen} from "@testing-library/react";
import EventFormModal from "./eventAddModify";
import "@testing-library/jest-dom";
import {createEvent, EventDto, updateEvent} from "@/lib/axios";
import userEvent from "@testing-library/user-event";

jest.mock("@/lib/axios", () => ({
    createEvent: jest.fn(),
    updateEvent: jest.fn(),
}));
const fakeEvent: EventDto = {
    id: 42,
    title: "Jazz Night at Bell Centre",
    description: "An unforgettable evening of live jazz with world-class musicians.",
    eventDate: "2025-08-15T20:00:00",
    categoryName: "Concert",
    locationName: "Bell Centre",
    city: "Montreal",
    totalTickets: 120,
    status: "ACTIVE",
};

describe("EventFormModal", () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });
    it("calls onClose when Cancel button is clicked", () => {
        const mockOnClose = jest.fn();
        render(<EventFormModal event={null}
        mode="add"
        onClose={mockOnClose}/>);

        const cancelButton = screen.getByText(/cancel/i);
        expect(cancelButton).toBeInTheDocument();
        fireEvent.click(cancelButton);

        expect(mockOnClose).toHaveBeenCalledTimes(1);
    });
    it("renders add mode correctly when event is null", () => {
        render(
            <EventFormModal
                event={null}
                mode="add"
                onClose={jest.fn()}
            />
        );

        const cancelButton = screen.getByText(/cancel/i);
        expect(cancelButton).toBeInTheDocument();
        expect(screen.getByText("Add New Event")).toBeInTheDocument();

        expect(screen.getByPlaceholderText("Event title")).toBeInTheDocument();
        expect(screen.getByText("Event Date")).toBeInTheDocument();
        expect(screen.getByText("Category")).toBeInTheDocument();
        expect(screen.getByText("Location")).toBeInTheDocument();
        expect(screen.getByText("Total Tickets")).toBeInTheDocument();


        expect(screen.getByText("Create Event")).toBeInTheDocument();
    });

    it("renders add mode correctly when event is not null", () => {
        render(
            <EventFormModal
                event={fakeEvent}
                mode="add"
                onClose={jest.fn()}
            />
        );
        const input = screen.getByPlaceholderText("Event title");
        expect(screen.getByText("Add New Event")).toBeInTheDocument();
        expect(input).toHaveValue("");
        expect(screen.getByPlaceholderText("Event title")).toBeInTheDocument();
        expect(screen.getByText("Event Date")).toBeInTheDocument();
        expect(screen.getByText("Category")).toBeInTheDocument();
        expect(screen.getByText("Location")).toBeInTheDocument();
        expect(screen.getByText("Total Tickets")).toBeInTheDocument();


        expect(screen.getByText("Create Event")).toBeInTheDocument();
    });

    it("renders modify mode correctly when event is null", () => {
        render(
            <EventFormModal
                event={null}
                mode="modify"
                onClose={jest.fn()}
            />
        );

        expect(screen.getByText("Modify Event")).toBeInTheDocument();

        expect(screen.getByPlaceholderText("Event title")).toBeInTheDocument();
        expect(screen.getByText("Event Date")).toBeInTheDocument();
        expect(screen.getByText("Category")).toBeInTheDocument();
        expect(screen.getByText("Location")).toBeInTheDocument();
        expect(screen.getByText("Total Tickets")).toBeInTheDocument();


        expect(screen.getByText("Save Changes")).toBeInTheDocument();
    });

    it("renders modify mode correctly when event is not null", () => {
        render(
            <EventFormModal
                event={fakeEvent}
                mode="modify"
                onClose={jest.fn()}
            />
        );
        const input = screen.getByPlaceholderText("Event title");

        expect(screen.getByText("Modify Event")).toBeInTheDocument();
        expect(screen.getByPlaceholderText("Event title")).toBeInTheDocument();
        expect(input).toHaveValue("Jazz Night at Bell Centre");
        expect(screen.getByText("Event Date")).toBeInTheDocument();
        expect(screen.getByText("Category")).toBeInTheDocument();
        expect(screen.getByText("Location")).toBeInTheDocument();
        expect(screen.getByText("Total Tickets")).toBeInTheDocument();


        expect(screen.getByText("Save Changes")).toBeInTheDocument();
    });
});

describe("EventFormModal - Add Event", () => {
    beforeEach(() => {
        jest.clearAllMocks();
        (createEvent as jest.Mock).mockResolvedValue({});
        (updateEvent as jest.Mock).mockResolvedValue({});
    });
    it("submits form and calls createEvent", async () => {
        (createEvent as jest.Mock).mockResolvedValue({});

        render(
            <EventFormModal
                event={null}
                mode="add"
                onClose={jest.fn()}
            />
        );

        await userEvent.type(
            screen.getByPlaceholderText("Event title"),
            "Jazz Night"
        );

        await userEvent.type(
            screen.getByPlaceholderText("Optional description"),
            "Live jazz performance"
        );

        fireEvent.change(
            screen.getByLabelText(/event date/i),
            { target: { value: "2026-05-01T20:00" } }
        );

        const selects = screen.getAllByRole("combobox");

        await userEvent.selectOptions(selects[0], "1");
        await userEvent.selectOptions(selects[1], "1");

        const ticketsInput = screen.getByRole("spinbutton");
        await userEvent.clear(ticketsInput);
        await userEvent.type(ticketsInput, "00");

        await userEvent.click(
            screen.getByRole("button", { name: /create event/i })
        );

        expect(createEvent).toHaveBeenCalledTimes(1);

        expect(createEvent).toHaveBeenCalledWith(
            expect.objectContaining({
                title: "Jazz Night",
                description: "Live jazz performance",
                categoryId: 1,
                locationId: 1,
                totalTickets: 100,
                eventDate: expect.any(String),
            })
        );
    });

    it("submits form and calls createEvent which result in an error", async () => {
        (createEvent as jest.Mock).mockRejectedValueOnce(new Error('Network Error'));

        render(
            <EventFormModal
                event={null}
                mode="add"
                onClose={jest.fn()}
            />
        );

        await userEvent.type(
            screen.getByPlaceholderText("Event title"),
            "Jazz Night"
        );

        await userEvent.type(
            screen.getByPlaceholderText("Optional description"),
            "Live jazz performance"
        );

        fireEvent.change(
            screen.getByLabelText(/event date/i),
            { target: { value: "2026-05-01T20:00" } }
        );

        const selects = screen.getAllByRole("combobox");

        await userEvent.selectOptions(selects[0], "1");
        await userEvent.selectOptions(selects[1], "1");

        const ticketsInput = screen.getByRole("spinbutton");
        await userEvent.clear(ticketsInput);
        await userEvent.type(ticketsInput, "00");

        await userEvent.click(
            screen.getByRole("button", { name: /create event/i })
        );

        expect(createEvent).toHaveBeenCalledTimes(1);

        expect(await screen.findByText("Network Error"))
            .toBeInTheDocument();
    });
});

describe("EventFormModal - Modify Event", () => {
    beforeEach(() => {
        jest.clearAllMocks();
        (createEvent as jest.Mock).mockResolvedValue({});
        (updateEvent as jest.Mock).mockResolvedValue({});
    });
    it("submits form and calls updateEvent", async () => {
        (updateEvent as jest.Mock).mockResolvedValue({});

        render(
            <EventFormModal
                event={fakeEvent}
                mode="modify"
                onClose={jest.fn()}
            />
        );

        const titleInput = screen.getByPlaceholderText("Event title");
        await userEvent.clear(titleInput);
        await userEvent.type(titleInput, "Jazz Night");

        const descriptionInput = screen.getByPlaceholderText("Optional description");
        await userEvent.clear(descriptionInput);
        await userEvent.type(descriptionInput, "Live jazz performance");

        fireEvent.change(
            screen.getByLabelText(/event date/i),
            { target: { value: "2026-05-01T20:00" } }
        );

        const selects = screen.getAllByRole("combobox");
        await userEvent.selectOptions(selects[0], "1"); // category
        await userEvent.selectOptions(selects[1], "1"); // location

        const ticketsInput = screen.getByRole("spinbutton");
        await userEvent.clear(ticketsInput);
        await userEvent.type(ticketsInput, "00");

        await userEvent.click(
            screen.getByRole("button", { name: /save changes/i })
        );

        expect(updateEvent).toHaveBeenCalledTimes(1);

        expect(updateEvent).toHaveBeenCalledWith(
            42,
            expect.objectContaining({
                title: "Jazz Night",
                description: "Live jazz performance",
                categoryId: 1,
                locationId: 1,
                totalTickets: 100,
                eventDate: expect.any(String),
            })
        );
    });

    it("submits form and calls updateEvent which result in an error", async () => {
        (updateEvent as jest.Mock).mockRejectedValueOnce(new Error('Network Error'));

        render(
            <EventFormModal
                event={fakeEvent}
                mode="modify"
                onClose={jest.fn()}
            />
        );

        await userEvent.type(
            screen.getByPlaceholderText("Event title"),
            "Jazz Night"
        );

        await userEvent.type(
            screen.getByPlaceholderText("Optional description"),
            "Live jazz performance"
        );

        fireEvent.change(
            screen.getByLabelText(/event date/i),
            { target: { value: "2026-05-01T20:00" } }
        );

        const selects = screen.getAllByRole("combobox");

        await userEvent.selectOptions(selects[0], "1");
        await userEvent.selectOptions(selects[1], "1");

        const ticketsInput = screen.getByRole("spinbutton");
        await userEvent.clear(ticketsInput);
        await userEvent.type(ticketsInput, "00");

        await userEvent.click(
            screen.getByRole("button", { name: /save changes/i })
        );

        expect(updateEvent).toHaveBeenCalledTimes(1);

        expect(await screen.findByText("Network Error"))
            .toBeInTheDocument();
    });
});