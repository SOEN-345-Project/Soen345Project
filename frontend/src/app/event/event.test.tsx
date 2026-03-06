import React from "react";
import { render, screen, fireEvent, waitFor, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import EventsPage from "./page";
import { login } from "@/lib/axios";
import '@testing-library/jest-dom';


const mockPush = jest.fn();

jest.mock("next/navigation", () => ({
    useRouter: () => ({ push: mockPush }),
}));

describe("EventPage", () => {

    beforeEach(() => {
        jest.clearAllMocks();
        sessionStorage.clear();
    });

    describe("Rendering", () => {
        it("renders the Events heading", () => {
            render(<EventsPage />);
            expect(screen.getByRole("heading", { name: /available events/i })).toBeInTheDocument();
        });


    });

});
