import React from "react";
import { render, screen, fireEvent, waitFor, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import SignInPage from "./page";
import { login } from "@/lib/axios";
import '@testing-library/jest-dom';

const mockPush = jest.fn();

jest.mock("next/navigation", () => ({
    useRouter: () => ({ push: mockPush }),
}));

jest.mock("@/lib/axios", () => ({
    login: jest.fn(),
}));

const mockLogin = login as jest.MockedFunction<typeof login>;

const fillAndSubmit = async (identifier: string, password: string) => {
    await userEvent.type(screen.getByPlaceholderText("Enter your email or phone"), identifier);
    await userEvent.type(screen.getByPlaceholderText("Enter your password"), password);
    await act(async () => {
        fireEvent.click(screen.getByRole("button", { name: /sign in/i }));
    });
};

describe("SignInPage", () => {

    beforeEach(() => {
        jest.clearAllMocks();
        sessionStorage.clear();
    });

    describe("Rendering", () => {
        it("renders the Sign In heading", () => {
            render(<SignInPage />);
            expect(screen.getByRole("heading", { name: /sign in/i })).toBeInTheDocument();
        });

        it("renders email/phone input and password input", () => {
            render(<SignInPage />);
            expect(screen.getByPlaceholderText("Enter your email or phone")).toBeInTheDocument();
            expect(screen.getByPlaceholderText("Enter your password")).toBeInTheDocument();
        });

        it("renders the Sign In submit button", () => {
            render(<SignInPage />);
            expect(screen.getByRole("button", { name: /sign in/i })).toBeInTheDocument();
        });

        it("renders the Sign Up navigation link", () => {
            render(<SignInPage />);
            expect(screen.getByText("Sign Up")).toBeInTheDocument();
        });

        it("does not show error message on initial render", () => {
            render(<SignInPage />);
            expect(screen.queryByText(/please fill in all fields/i)).not.toBeInTheDocument();
        });
    });

    describe("Identifier type detection badge", () => {
        it("shows no badge when input is empty", () => {
            render(<SignInPage />);
            expect(screen.queryByText("📱 Phone")).not.toBeInTheDocument();
            expect(screen.queryByText("✉️ Email")).not.toBeInTheDocument();
        });

        it("shows Email badge for a valid email address", async () => {
            render(<SignInPage />);
            await userEvent.type(screen.getByPlaceholderText("Enter your email or phone"), "user@example.com");
            expect(screen.getByText("✉️ Email")).toBeInTheDocument();
        });

        it("shows Phone badge for a numeric phone number", async () => {
            render(<SignInPage />);
            await userEvent.type(screen.getByPlaceholderText("Enter your email or phone"), "5141234567");
            expect(screen.getByText("📱 Phone")).toBeInTheDocument();
        });

        it("shows Phone badge for a phone with +, spaces, dashes", async () => {
            render(<SignInPage />);
            await userEvent.type(screen.getByPlaceholderText("Enter your email or phone"), "+1 (514) 123-4567");
            expect(screen.getByText("📱 Phone")).toBeInTheDocument();
        });
    });

    describe("Validation", () => {
        it("shows error when both fields are empty", async () => {
            render(<SignInPage />);
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /sign in/i }));
            });
            expect(screen.getByText(/please fill in all fields/i)).toBeInTheDocument();
        });

        it("shows error when identifier is empty but password is filled", async () => {
            render(<SignInPage />);
            await userEvent.type(screen.getByPlaceholderText("Enter your password"), "password123");
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /sign in/i }));
            });
            expect(screen.getByText(/please fill in all fields/i)).toBeInTheDocument();
        });

        it("shows error when password is empty but identifier is filled", async () => {
            render(<SignInPage />);
            await userEvent.type(screen.getByPlaceholderText("Enter your email or phone"), "user@example.com");
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /sign in/i }));
            });
            expect(screen.getByText(/please fill in all fields/i)).toBeInTheDocument();
        });

        it("does not call login() when validation fails", async () => {
            render(<SignInPage />);
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /sign in/i }));
            });
            expect(mockLogin).not.toHaveBeenCalled();
        });
    });

    describe("Successful login", () => {
        it("calls login() with email payload when identifier looks like an email", async () => {
            mockLogin.mockResolvedValueOnce({ token: "abc123" });
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "password123");
            await waitFor(() =>
                expect(mockLogin).toHaveBeenCalledWith({
                    email: "user@example.com",
                    phoneNumber: "",
                    password: "password123",
                })
            );
        });

        it("calls login() with phoneNumber payload when identifier looks like a phone", async () => {
            mockLogin.mockResolvedValueOnce({ token: "abc123" });
            render(<SignInPage />);
            await fillAndSubmit("5141234567", "password123");
            await waitFor(() =>
                expect(mockLogin).toHaveBeenCalledWith({
                    email: "",
                    phoneNumber: "5141234567",
                    password: "password123",
                })
            );
        });

        it("stores token and userLoggedIn flag in sessionStorage on success", async () => {
            mockLogin.mockResolvedValueOnce({ token: "abc123" });
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "password123");
            await waitFor(() => {
                expect(sessionStorage.getItem("token")).toBe("abc123");
                expect(sessionStorage.getItem("userLoggedIn")).toBe("true");
            });
        });

        it("also handles token nested under response.data.token", async () => {
            mockLogin.mockResolvedValueOnce({ data: { token: "nested-token" } });
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "password123");
            await waitFor(() =>
                expect(sessionStorage.getItem("token")).toBe("nested-token")
            );
        });

        it("redirects to /event after successful login", async () => {
            mockLogin.mockResolvedValueOnce({ token: "abc123" });
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "password123");
            await waitFor(() => expect(mockPush).toHaveBeenCalledWith("/event"));
        });
    });

    describe("Failed login", () => {
        it("shows error when login resolves but returns no token", async () => {
            mockLogin.mockResolvedValueOnce({});
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "password123");
            await waitFor(() =>
                expect(screen.getByText(/login failed: no token received/i)).toBeInTheDocument()
            );
        });

        it("shows string error thrown by login()", async () => {
            mockLogin.mockRejectedValueOnce("Invalid credentials.");
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "wrongpassword");
            await waitFor(() =>
                expect(screen.getByText(/invalid credentials/i)).toBeInTheDocument()
            );
        });

        it("shows error.message when login throws an Error object", async () => {
            mockLogin.mockRejectedValueOnce(new Error("Server error"));
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "wrongpassword");
            await waitFor(() =>
                expect(screen.getByText(/server error/i)).toBeInTheDocument()
            );
        });

        it("shows fallback error message when rejection has no message", async () => {
            mockLogin.mockRejectedValueOnce({});
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "wrongpassword");
            await waitFor(() =>
                expect(screen.getByText(/invalid credentials/i)).toBeInTheDocument()
            );
        });

        it("does not redirect on failed login", async () => {
            mockLogin.mockRejectedValueOnce("bad creds");
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "wrongpassword");
            await waitFor(() => expect(mockPush).not.toHaveBeenCalled());
        });
    });

    describe("Loading state", () => {
        it("shows 'Signing in...' while request is in flight", async () => {
            mockLogin.mockImplementation(() => new Promise(() => {})); // never resolves
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "password123");
            expect(screen.getByText(/signing in\.\.\./i)).toBeInTheDocument();
        });

        it("disables the submit button while loading", async () => {
            mockLogin.mockImplementation(() => new Promise(() => {}));
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "password123");
            expect(screen.getByRole("button", { name: /signing in/i })).toBeDisabled();
        });
    });

    describe("Navigation", () => {
        it("navigates to /signup when Sign Up link is clicked", async () => {
            render(<SignInPage />);
            await act(async () => {
                fireEvent.click(screen.getByText("Sign Up"));
            });
            expect(mockPush).toHaveBeenCalledWith("/signup");
        });
    });
});