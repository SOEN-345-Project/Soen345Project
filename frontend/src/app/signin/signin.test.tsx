// SignInPage.unit.test.tsx
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import SignInPage from "./page";
import "@testing-library/jest-dom";
// Mock Next.js router
const mockPush = jest.fn();
jest.mock("next/navigation", () => ({
    useRouter: () => ({ push: mockPush }),
}));

describe("SignInPage Unit Tests", () => {

    beforeEach(() => {
        mockPush.mockClear(); // Clear previous calls
    });

    it("renders the form inputs and button", () => {
        render(<SignInPage />);
        expect(screen.getByPlaceholderText("Enter your email")).toBeInTheDocument();
        expect(screen.getByPlaceholderText("Enter your password")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: /sign in/i })).toBeInTheDocument();
    });

    it("shows error if submitted with empty fields", async () => {
        render(<SignInPage />);
        const button = screen.getByRole("button", { name: /sign in/i });
        await userEvent.click(button);
        expect(screen.getByText("Please fill in all fields.")).toBeInTheDocument();
    });

    it("updates input values when typing", async () => {
        render(<SignInPage />);
        const emailInput = screen.getByPlaceholderText("Enter your email");
        const passwordInput = screen.getByPlaceholderText("Enter your password");

        await userEvent.type(emailInput, "test@example.com");
        await userEvent.type(passwordInput, "password123");

        expect(emailInput).toHaveValue("test@example.com");
        expect(passwordInput).toHaveValue("password123");
    });

    it("redirects when form is submitted with values", async () => {
        render(<SignInPage/>);
        const emailInput = screen.getByPlaceholderText("Enter your email");
        const passwordInput = screen.getByPlaceholderText("Enter your password");
        const button = screen.getByRole("button", { name: /sign in/i });

        await userEvent.type(emailInput, "user@example.com");
        await userEvent.type(passwordInput, "password123");
        await userEvent.click(button);

        expect(mockPush).toHaveBeenCalledWith("/dashboard");
    });
});