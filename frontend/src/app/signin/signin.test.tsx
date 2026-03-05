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

describe("Sign In Page", () => {

    beforeEach(() => {
        jest.clearAllMocks();
        sessionStorage.clear();
    });

    describe("When the page first loads", () => {
        it("shows the email/phone and password fields", () => {
            render(<SignInPage />);
            expect(screen.getByPlaceholderText("Enter your email or phone")).toBeInTheDocument();
            expect(screen.getByPlaceholderText("Enter your password")).toBeInTheDocument();
        });

        it("has no error message visible yet", () => {
            render(<SignInPage />);
            expect(screen.queryByText(/please fill in all fields/i)).not.toBeInTheDocument();
        });
    });

    describe("When the user tries to submit without filling in the form", () => {
        it("warns the user to fill in all fields if both are empty", async () => {
            render(<SignInPage />);
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /sign in/i }));
            });
            expect(screen.getByText(/please fill in all fields/i)).toBeInTheDocument();
        });

        it("warns the user if they only typed a password but forgot their email or phone", async () => {
            render(<SignInPage />);
            await userEvent.type(screen.getByPlaceholderText("Enter your password"), "mypassword");
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /sign in/i }));
            });
            expect(screen.getByText(/please fill in all fields/i)).toBeInTheDocument();
        });

        it("never contacts the server if the form is incomplete", async () => {
            render(<SignInPage />);
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /sign in/i }));
            });
            expect(mockLogin).not.toHaveBeenCalled();
        });
    });

    describe("When the user signs in successfully", () => {
        it("sends the email and password to the server when the user types an email", async () => {
            mockLogin.mockResolvedValueOnce({ token: "abc123" });
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "mypassword");
            await waitFor(() =>
                expect(mockLogin).toHaveBeenCalledWith({
                    email: "user@example.com",
                    phoneNumber: "",
                    password: "mypassword",
                })
            );
        });

        it("sends the phone number and password to the server when the user types a phone number", async () => {
            mockLogin.mockResolvedValueOnce({ token: "abc123" });
            render(<SignInPage />);
            await fillAndSubmit("5141234567", "mypassword");
            await waitFor(() =>
                expect(mockLogin).toHaveBeenCalledWith({
                    email: "",
                    phoneNumber: "5141234567",
                    password: "mypassword",
                })
            );
        });

        it("saves the token and marks the user as logged in", async () => {
            mockLogin.mockResolvedValueOnce({ token: "abc123" });
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "mypassword");
            await waitFor(() => {
                expect(sessionStorage.getItem("token")).toBe("abc123");
                expect(sessionStorage.getItem("userLoggedIn")).toBe("true");
            });
        });

        it("also works if the token comes back nested inside response.data", async () => {
            mockLogin.mockResolvedValueOnce({ data: { token: "nested-token" } });
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "mypassword");
            await waitFor(() =>
                expect(sessionStorage.getItem("token")).toBe("nested-token")
            );
        });

        it("takes the user to the events page", async () => {
            mockLogin.mockResolvedValueOnce({ token: "abc123" });
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "mypassword");
            await waitFor(() => expect(mockPush).toHaveBeenCalledWith("/event"));
        });
    });

    describe("When the login attempt fails", () => {
        it("tells the user no token was received if the server responds but sends nothing back", async () => {
            mockLogin.mockResolvedValueOnce({});
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "mypassword");
            await waitFor(() =>
                expect(screen.getByText(/login failed: no token received/i)).toBeInTheDocument()
            );
        });

        it("shows the error message returned by the server", async () => {
            mockLogin.mockRejectedValueOnce(new Error("Server error"));
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "wrongpassword");
            await waitFor(() =>
                expect(screen.getByText(/server error/i)).toBeInTheDocument()
            );
        });

        it("falls back to a generic message if the server gives no explanation", async () => {
            mockLogin.mockRejectedValueOnce({});
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "wrongpassword");
            await waitFor(() =>
                expect(screen.getByText(/invalid credentials/i)).toBeInTheDocument()
            );
        });

        it("keeps the user on the sign in page instead of redirecting them", async () => {
            mockLogin.mockRejectedValueOnce("bad creds");
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "wrongpassword");
            await waitFor(() => expect(mockPush).not.toHaveBeenCalled());
        });
    });

    describe("While the sign in request is in progress", () => {
        it("disables the submit button so the user cannot click it twice", async () => {
            mockLogin.mockImplementation(() => new Promise(() => {}));
            render(<SignInPage />);
            await fillAndSubmit("user@example.com", "mypassword");
            expect(screen.getByRole("button", { name: /signing in/i })).toBeDisabled();
        });
    });
});