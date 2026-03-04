import React from "react";
import { render, screen, fireEvent, waitFor, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import '@testing-library/jest-dom';
import SignUpPage from "./page";
import { signup, verify, resendCode } from "@/lib/axios";
import SignInPage from "@/app/signin/page";

// ── Mocks ──────────────────────────────────────────────────────────────────
const mockPush = jest.fn();

jest.mock("next/navigation", () => ({
    useRouter: () => ({ push: mockPush }),
}));

jest.mock("@/lib/axios", () => ({
    signup: jest.fn(),
    verify: jest.fn(),
    resendCode: jest.fn(),
}));

const mockSignup = signup as jest.MockedFunction<typeof signup>;
const mockVerify = verify as jest.MockedFunction<typeof verify>;
const mockResendCode = resendCode as jest.MockedFunction<typeof resendCode>;

// ── Helpers ────────────────────────────────────────────────────────────────
const fillForm = async ({
                            firstName = "",
                            lastName = "",
                            email = "",
                            phone = "",
                            password = "",
                        }: {
    firstName?: string;
    lastName?: string;
    email?: string;
    phone?: string;
    password?: string;
}) => {
    if (firstName) await userEvent.type(screen.getByPlaceholderText("Enter your first name"), firstName);
    if (lastName)  await userEvent.type(screen.getByPlaceholderText("Enter your last name"), lastName);
    if (email)     await userEvent.type(screen.getByPlaceholderText("Enter your email"), email);
    if (phone)     await userEvent.type(screen.getByPlaceholderText("Enter your phone number"), phone);
    if (password)  await userEvent.type(screen.getByPlaceholderText("Enter your password"), password);
};

const submitForm = async () => {
    await act(async () => {
        fireEvent.click(screen.getByRole("button", { name: /sign up/i }));
    });
};

describe("SignUpPage", () => {

    beforeEach(() => {
        jest.clearAllMocks();
        sessionStorage.clear();
    });

    describe("Rendering", () => {
        it("renders the Sign Up heading", () => {
            render(<SignUpPage />);
            expect(screen.getByRole("heading", { name: /sign up/i })).toBeInTheDocument();
        });

        it("renders all input fields", () => {
            render(<SignUpPage />);
            expect(screen.getByPlaceholderText("Enter your email")).toBeInTheDocument();
            expect(screen.getByPlaceholderText("Enter your password")).toBeInTheDocument();
            expect(screen.getByPlaceholderText("Enter your first name")).toBeInTheDocument();
            expect(screen.getByPlaceholderText("Enter your last name")).toBeInTheDocument();
            expect(screen.getByPlaceholderText("Enter your phone number")).toBeInTheDocument();
        });

        it("renders the Email and Phone toggle buttons", () => {
            render(<SignUpPage />);
            expect(screen.getByRole("button", { name: /email/i })).toBeInTheDocument();
            expect(screen.getByRole("button", { name: /phone/i })).toBeInTheDocument();
        });

        it("renders the Sign Up submit button", () => {
            render(<SignUpPage />);
            expect(screen.getByRole("button", { name: /sign up/i })).toBeInTheDocument();
        });

        it("renders the Sign In navigation link", () => {
            // TODO
        });

        it("does not show verification panel on initial render", () => {
            // TODO
        });
    });

    describe("Verification method toggle", () => {
        it("defaults to EMAIL verification", () => {
            // TODO - email * should be visible, phone * should not
        });

        it("switches to PHONE verification when Phone button is clicked", () => {
            // TODO - phone * should be visible, email * should not
        });

        it("switches back to EMAIL when Email button is clicked", () => {
            // TODO
        });
    });

    describe("Validation", () => {
        it("shows error when first name is missing", async () => {
            // TODO
        });

        it("shows error when last name is missing", async () => {
            // TODO
        });

        it("shows error when password is missing", async () => {
            // TODO
        });

        it("shows error when EMAIL method selected but email is empty", async () => {
            // TODO
        });

        it("shows error when PHONE method selected but phone is empty", async () => {
            // TODO
        });

        it("does not call signup() when validation fails", async () => {
            // TODO
        });
    });

    describe("Successful signup", () => {
        it("calls signup() with correct payload for EMAIL method", async () => {
            // TODO
            // mockSignup.mockResolvedValueOnce({});
        });

        it("calls signup() with correct payload for PHONE method", async () => {
            // TODO
            // mockSignup.mockResolvedValueOnce({});
        });

        it("shows verification panel after successful signup", async () => {
            // TODO
            // mockSignup.mockResolvedValueOnce({});
        });

        it("hides the form and shows verification panel", async () => {
            // TODO
        });
    });


    describe("Failed signup", () => {
        it("shows error when signup() throws a string", async () => {
            // TODO
            // mockSignup.mockRejectedValueOnce("Email already exists.");
        });

        it("shows fallback error when signup() throws with no message", async () => {
            // TODO
            // mockSignup.mockRejectedValueOnce({});
        });
    });


    describe("Verification panel", () => {

        const goToVerification = async () => {
            mockSignup.mockResolvedValueOnce({});
            render(<SignUpPage />);
            await fillForm({ firstName: "John", lastName: "Doe", email: "john@example.com", password: "pass123" });
            await submitForm();
            await waitFor(() => screen.getByPlaceholderText("Enter confirmation code"));
        };

        it("shows correct message for EMAIL verification", async () => {

        });

        it("shows correct message for PHONE verification", async () => {

        });

        it("shows error when confirming with empty code", async () => {

        });

        it("calls verify() with correct email payload", async () => {

        });

        it("calls verify() with correct phone payload when PHONE method", async () => {

        });

        it("redirects to /signin after successful verification", async () => {

        });

        it("shows error when verify() fails", async () => {

        });

        it("calls resendCode() when Resend code is clicked", async () => {

        });

        it("hides verification panel and shows form when Cancel is clicked", async () => {
        });
    });


    describe("Loading states", () => {
        it("shows 'Signing up...' while signup request is in flight", async () => {

        });

        it("disables Sign Up button while loading", async () => {

        });

        it("shows 'Confirming...' while verify request is in flight", async () => {

        });
    });


    describe("Navigation", () => {
        it("navigates to /signin when Sign In link is clicked", async () => {
            // TODO
            // expect(mockPush).toHaveBeenCalledWith("/signin");
        });
    });
});