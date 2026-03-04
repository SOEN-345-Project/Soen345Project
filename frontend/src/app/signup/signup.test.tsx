import React from "react";
import { render, screen, fireEvent, waitFor, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import '@testing-library/jest-dom';
import SignUpPage from "./page";
import { signup, verify, resendCode } from "@/lib/axios";

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

const fillForm = async ({
                            firstName = "", lastName = "", email = "",
                            phone = "", password = "",
                        }: {
    firstName?: string; lastName?: string; email?: string;
    phone?: string; password?: string;
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

const getLabels = () => {
    const all = Array.from(document.querySelectorAll("label"));
    return {
        emailLabel: all.find(l => /^Email/.test(l.textContent ?? "")),
        phoneLabel: all.find(l => /Phone Number/.test(l.textContent ?? "")),
    };
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
            render(<SignUpPage />);
            expect(screen.getByText("Sign In")).toBeInTheDocument();
        });

        it("does not show verification panel on initial render", () => {
            render(<SignUpPage />);
            expect(screen.queryByText(/resend code/i)).not.toBeInTheDocument();
        });
    });

    describe("Verification method toggle", () => {
        it("defaults to EMAIL verification", () => {
            render(<SignUpPage />);
            const { emailLabel, phoneLabel } = getLabels();
            expect(emailLabel?.querySelector("span")).toBeInTheDocument();
            expect(phoneLabel?.querySelector("span")).toBeNull();
        });

        it("switches to PHONE verification when Phone button is clicked", async () => {
            render(<SignUpPage />);
            await userEvent.click(screen.getByRole("button", { name: /^phone$/i }));
            const { emailLabel, phoneLabel } = getLabels();
            expect(phoneLabel?.querySelector("span")).toBeInTheDocument();
            expect(emailLabel?.querySelector("span")).toBeNull();
        });

        it("switches back to EMAIL when Email button is clicked", async () => {
            render(<SignUpPage />);
            await userEvent.click(screen.getByRole("button", { name: /^phone$/i }));
            await userEvent.click(screen.getByRole("button", { name: /^email$/i }));
            const { emailLabel, phoneLabel } = getLabels();
            expect(emailLabel?.querySelector("span")).toBeInTheDocument();
            expect(phoneLabel?.querySelector("span")).toBeNull();
        });
    });

    describe("Validation", () => {
        it("shows error when first name is missing", async () => {
            render(<SignUpPage />);
            await fillForm({ lastName: "Johnson", email: "sarah.j@gmail.com", password: "hunter2" });
            await submitForm();
            expect(await screen.findByText(/please fill in all fields/i)).toBeInTheDocument();
        });

        it("shows error when last name is missing", async () => {
            render(<SignUpPage />);
            await fillForm({ firstName: "Sarah", email: "sarah.j@gmail.com", password: "hunter2" });
            await submitForm();
            expect(await screen.findByText(/please fill in all fields/i)).toBeInTheDocument();
        });

        it("shows error when password is missing", async () => {
            render(<SignUpPage />);
            await fillForm({ firstName: "Sarah", lastName: "Johnson", email: "sarah.j@gmail.com" });
            await submitForm();
            expect(await screen.findByText(/please fill in all fields/i)).toBeInTheDocument();
        });

        it("shows error when EMAIL method selected but email is empty", async () => {
            render(<SignUpPage />);
            await fillForm({ firstName: "Mike", lastName: "Torres", password: "password123" });
            await submitForm();
            expect(await screen.findByText(/email is required for email verification/i)).toBeInTheDocument();
        });

        it("shows error when PHONE method selected but phone is empty", async () => {
            render(<SignUpPage />);
            await userEvent.click(screen.getByRole("button", { name: /^phone$/i }));
            await fillForm({ firstName: "Mike", lastName: "Torres", email: "mike@gmail.com", password: "password123" });
            await submitForm();
            expect(await screen.findByText(/phone number is required for phone verification/i)).toBeInTheDocument();
        });

        it("does not call signup() when validation fails", async () => {
            render(<SignUpPage />);
            await submitForm();
            expect(mockSignup).not.toHaveBeenCalled();
        });
    });

    describe("Successful signup", () => {
        it("calls signup() with correct payload for EMAIL method", async () => {
            mockSignup.mockResolvedValueOnce({});
            render(<SignUpPage />);
            await fillForm({
                firstName: "Emma",
                lastName: "Wilson",
                email: "emma.wilson@gmail.com",
                password: "securePass99",
            });
            await submitForm();

            expect(mockSignup).toHaveBeenCalledWith({
                firstName: "Emma",
                lastName: "Wilson",
                email: "emma.wilson@gmail.com",
                password: "securePass99",
                phoneNumber: "",
                verificationMethod: "EMAIL",
            });
        });

        it("calls signup() with correct payload for PHONE method", async () => {
            mockSignup.mockResolvedValueOnce({});
            render(<SignUpPage />);
            await userEvent.click(screen.getByRole("button", { name: /^phone$/i }));
            await fillForm({
                firstName: "Carlos",
                lastName: "Rivera",
                email: "carlos.r@yahoo.com",
                phone: "5551234567",
                password: "mypassword!",
            });
            await submitForm();

            expect(mockSignup).toHaveBeenCalledWith({
                firstName: "Carlos",
                lastName: "Rivera",
                email: "carlos.r@yahoo.com",
                password: "mypassword!",
                phoneNumber: "5551234567",
                verificationMethod: "PHONE",
            });
        });

        it("shows verification panel after successful signup", async () => {
            mockSignup.mockResolvedValueOnce({});
            render(<SignUpPage />);
            await fillForm({
                firstName: "Emma", lastName: "Wilson",
                email: "emma.wilson@gmail.com", password: "securePass99",
            });
            await submitForm();

            await waitFor(() =>
                expect(screen.getByPlaceholderText("Enter confirmation code")).toBeInTheDocument()
            );
        });

        it("hides the form and shows verification panel", async () => {
            mockSignup.mockResolvedValueOnce({});
            render(<SignUpPage />);
            await fillForm({
                firstName: "Emma", lastName: "Wilson",
                email: "emma.wilson@gmail.com", password: "securePass99",
            });
            await submitForm();

            await waitFor(() => {
                expect(screen.queryByPlaceholderText("Enter your first name")).not.toBeInTheDocument();
                expect(screen.getByPlaceholderText("Enter confirmation code")).toBeInTheDocument();
            });
        });
    });

    describe("Failed signup", () => {
        it("shows error when signup() throws a string", async () => {
            mockSignup.mockRejectedValueOnce("Email already exists.");
            render(<SignUpPage />);
            await fillForm({
                firstName: "Tom", lastName: "Baker",
                email: "tom.baker@gmail.com", password: "password",
            });
            await submitForm();

            expect(await screen.findByText("Email already exists.")).toBeInTheDocument();
        });

        it("shows fallback error when signup() throws with no message", async () => {
            mockSignup.mockRejectedValueOnce(null);
            render(<SignUpPage />);
            await fillForm({
                firstName: "Tom", lastName: "Baker",
                email: "tom.baker@gmail.com", password: "password",
            });
            await submitForm();

            expect(await screen.findByText(/signup failed/i)).toBeInTheDocument();
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
            await goToVerification();
            expect(screen.getByText(/we sent a code to your email/i)).toBeInTheDocument();
        });

        it("shows correct message for PHONE verification", async () => {
            mockSignup.mockResolvedValueOnce({});
            render(<SignUpPage />);
            await userEvent.click(screen.getByRole("button", { name: /^phone$/i }));
            await fillForm({ firstName: "John", lastName: "Doe", phone: "5559876543", password: "pass123" });
            await submitForm();
            await waitFor(() => screen.getByPlaceholderText("Enter confirmation code"));

            expect(screen.getByText(/we sent a code to your phone/i)).toBeInTheDocument();
        });

        it("shows error when confirming with empty code", async () => {
            await goToVerification();
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /confirm/i }));
            });
            expect(await screen.findByText(/please enter the code/i)).toBeInTheDocument();
        });

        it("calls verify() with correct email payload", async () => {
            mockVerify.mockResolvedValueOnce({});
            await goToVerification();
            await userEvent.type(screen.getByPlaceholderText("Enter confirmation code"), "482910");
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /confirm/i }));
            });

            expect(mockVerify).toHaveBeenCalledWith({
                email: "john@example.com",
                phoneNumber: "",
                verificationCode: "482910",
            });
        });

        it("calls verify() with correct phone payload when PHONE method", async () => {
            mockSignup.mockResolvedValueOnce({});
            mockVerify.mockResolvedValueOnce({});
            render(<SignUpPage />);
            await userEvent.click(screen.getByRole("button", { name: /^phone$/i }));
            await fillForm({ firstName: "Maria", lastName: "Garcia", phone: "5551112222", password: "qwerty123" });
            await submitForm();
            await waitFor(() => screen.getByPlaceholderText("Enter confirmation code"));

            await userEvent.type(screen.getByPlaceholderText("Enter confirmation code"), "773421");
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /confirm/i }));
            });

            expect(mockVerify).toHaveBeenCalledWith({
                email: "",
                phoneNumber: "5551112222",
                verificationCode: "773421",
            });
        });

        it("redirects to /signin after successful verification", async () => {
            mockVerify.mockResolvedValueOnce({});
            await goToVerification();
            await userEvent.type(screen.getByPlaceholderText("Enter confirmation code"), "123456");
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /confirm/i }));
            });

            await waitFor(() => expect(mockPush).toHaveBeenCalledWith("/signin"));
        });

        it("shows error when verify() fails", async () => {
            mockVerify.mockRejectedValueOnce("Invalid or expired code.");
            await goToVerification();
            await userEvent.type(screen.getByPlaceholderText("Enter confirmation code"), "000000");
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /confirm/i }));
            });

            expect(await screen.findByText("Invalid or expired code.")).toBeInTheDocument();
        });

        it("calls resendCode() when Resend code is clicked", async () => {
            mockResendCode.mockResolvedValueOnce({});
            await goToVerification();
            await act(async () => {
                fireEvent.click(screen.getByText(/resend code/i));
            });

            expect(mockResendCode).toHaveBeenCalledWith("john@example.com", undefined);
        });

        it("hides verification panel and shows form when Cancel is clicked", async () => {
            await goToVerification();
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /cancel/i }));
            });

            expect(screen.queryByPlaceholderText("Enter confirmation code")).not.toBeInTheDocument();
            expect(screen.getByPlaceholderText("Enter your first name")).toBeInTheDocument();
        });
    });

    describe("Loading states", () => {
        it("shows 'Signing up...' while signup request is in flight", async () => {
            mockSignup.mockImplementation(() => new Promise(() => {}));
            render(<SignUpPage />);
            await fillForm({ firstName: "Lucy", lastName: "Chen", email: "lucy.chen@gmail.com", password: "abc12345" });
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /sign up/i }));
            });

            expect(screen.getByRole("button", { name: /signing up\.\.\./i })).toBeInTheDocument();
        });

        it("disables Sign Up button while loading", async () => {
            mockSignup.mockImplementation(() => new Promise(() => {}));
            render(<SignUpPage />);
            await fillForm({ firstName: "Lucy", lastName: "Chen", email: "lucy.chen@gmail.com", password: "abc12345" });
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /sign up/i }));
            });

            expect(screen.getByRole("button", { name: /signing up\.\.\./i })).toBeDisabled();
        });

        it("shows 'Confirming...' while verify request is in flight", async () => {
            mockSignup.mockResolvedValueOnce({});
            mockVerify.mockImplementation(() => new Promise(() => {}));
            render(<SignUpPage />);
            await fillForm({ firstName: "John", lastName: "Doe", email: "john@example.com", password: "pass123" });
            await submitForm();
            await waitFor(() => screen.getByPlaceholderText("Enter confirmation code"));

            await userEvent.type(screen.getByPlaceholderText("Enter confirmation code"), "654321");
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /confirm/i }));
            });

            expect(screen.getByRole("button", { name: /confirming\.\.\./i })).toBeInTheDocument();
        });
    });

    describe("Navigation", () => {
        it("navigates to /signin when Sign In link is clicked", async () => {
            render(<SignUpPage />);
            await act(async () => {
                fireEvent.click(screen.getByText("Sign In"));
            });
            expect(mockPush).toHaveBeenCalledWith("/signin");
        });
    });
});