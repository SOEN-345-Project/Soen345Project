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

describe("Sign Up Page", () => {

    beforeEach(() => {
        jest.clearAllMocks();
        sessionStorage.clear();
    });

    describe("When someone opens the sign up page", () => {
        it("shows all the fields they need to fill in", () => {
            render(<SignUpPage />);
            expect(screen.getByPlaceholderText("Enter your first name")).toBeInTheDocument();
            expect(screen.getByPlaceholderText("Enter your last name")).toBeInTheDocument();
            expect(screen.getByPlaceholderText("Enter your email")).toBeInTheDocument();
            expect(screen.getByPlaceholderText("Enter your phone number")).toBeInTheDocument();
            expect(screen.getByPlaceholderText("Enter your password")).toBeInTheDocument();
        });

        it("does not show the verification panel yet", () => {
            render(<SignUpPage />);
            expect(screen.queryByText(/resend code/i)).not.toBeInTheDocument();
        });
    });

    describe("When someone picks how they want to verify their account", () => {
        it("marks Phone as active when they click the Phone button", async () => {
            render(<SignUpPage />);
            await userEvent.click(screen.getByRole("button", { name: /^phone$/i }));
            const phoneLabel = Array.from(document.querySelectorAll("label"))
                .find(l => /Phone Number/.test(l.textContent ?? ""));
            expect(phoneLabel?.querySelector("span")).toBeInTheDocument();
        });

        it("marks Email as active again if they change their mind and click Email", async () => {
            render(<SignUpPage />);
            await userEvent.click(screen.getByRole("button", { name: /^phone$/i }));
            await userEvent.click(screen.getByRole("button", { name: /^email$/i }));
            const emailLabel = Array.from(document.querySelectorAll("label"))
                .find(l => /^Email/.test(l.textContent ?? ""));
            expect(emailLabel?.querySelector("span")).toBeInTheDocument();
        });
    });

    describe("When someone tries to sign up without filling everything in", () => {
        it("reminds them to fill in their first name if they forgot it", async () => {
            render(<SignUpPage />);
            await fillForm({ lastName: "Johnson", email: "sarah.j@gmail.com", password: "mypassword" });
            await submitForm();
            expect(await screen.findByText(/please fill in all fields/i)).toBeInTheDocument();
        });

        it("reminds them to add a password if they left it blank", async () => {
            render(<SignUpPage />);
            await fillForm({ firstName: "Sarah", lastName: "Johnson", email: "sarah.j@gmail.com" });
            await submitForm();
            expect(await screen.findByText(/please fill in all fields/i)).toBeInTheDocument();
        });

        it("reminds them to add an email if they picked email verification but left it blank", async () => {
            render(<SignUpPage />);
            await fillForm({ firstName: "Mike", lastName: "Torres", password: "mypassword" });
            await submitForm();
            expect(await screen.findByText(/email is required for email verification/i)).toBeInTheDocument();
        });

        it("reminds them to add a phone number if they picked phone verification but left it blank", async () => {
            render(<SignUpPage />);
            await userEvent.click(screen.getByRole("button", { name: /^phone$/i }));
            await fillForm({ firstName: "Mike", lastName: "Torres", email: "mike@gmail.com", password: "mypassword" });
            await submitForm();
            expect(await screen.findByText(/phone number is required for phone verification/i)).toBeInTheDocument();
        });

        it("does not hit the server at all until the form is complete", async () => {
            render(<SignUpPage />);
            await submitForm();
            expect(mockSignup).not.toHaveBeenCalled();
        });
    });

    describe("When someone fills out the form correctly and hits Sign Up", () => {
        it("passes their name, email, and password to the server when they chose email verification", async () => {
            mockSignup.mockResolvedValueOnce({});
            render(<SignUpPage />);
            await fillForm({ firstName: "Emma", lastName: "Wilson", email: "emma.wilson@gmail.com", password: "mypassword" });
            await submitForm();

            expect(mockSignup).toHaveBeenCalledWith({
                firstName: "Emma",
                lastName: "Wilson",
                email: "emma.wilson@gmail.com",
                password: "mypassword",
                phoneNumber: "",
                verificationMethod: "EMAIL",
            });
        });

        it("passes their name, phone number, and password to the server when they chose phone verification", async () => {
            mockSignup.mockResolvedValueOnce({});
            render(<SignUpPage />);
            await userEvent.click(screen.getByRole("button", { name: /^phone$/i }));
            await fillForm({ firstName: "Carlos", lastName: "Rivera", email: "carlos.r@yahoo.com", phone: "5551234567", password: "mypassword" });
            await submitForm();

            expect(mockSignup).toHaveBeenCalledWith({
                firstName: "Carlos",
                lastName: "Rivera",
                email: "carlos.r@yahoo.com",
                password: "mypassword",
                phoneNumber: "5551234567",
                verificationMethod: "PHONE",
            });
        });

        it("swaps the form out for a verification code screen once the account is created", async () => {
            mockSignup.mockResolvedValueOnce({});
            render(<SignUpPage />);
            await fillForm({ firstName: "Emma", lastName: "Wilson", email: "emma.wilson@gmail.com", password: "mypassword" });
            await submitForm();

            await waitFor(() => {
                expect(screen.queryByPlaceholderText("Enter your first name")).not.toBeInTheDocument();
                expect(screen.getByPlaceholderText("Enter confirmation code")).toBeInTheDocument();
            });
        });

        it("lets them know if an account with that email already exists", async () => {
            mockSignup.mockRejectedValueOnce("Email already exists.");
            render(<SignUpPage />);
            await fillForm({ firstName: "Tom", lastName: "Baker", email: "tom.baker@gmail.com", password: "mypassword" });
            await submitForm();
            expect(await screen.findByText("Email already exists.")).toBeInTheDocument();
        });

        it("shows a generic error message if something goes wrong and the server doesn't say why", async () => {
            mockSignup.mockRejectedValueOnce(null);
            render(<SignUpPage />);
            await fillForm({ firstName: "Tom", lastName: "Baker", email: "tom.baker@gmail.com", password: "mypassword" });
            await submitForm();
            expect(await screen.findByText(/signup failed/i)).toBeInTheDocument();
        });

        it("greys out the Sign Up button so they can't click it twice while it's loading", async () => {
            mockSignup.mockImplementation(() => new Promise(() => {}));
            render(<SignUpPage />);
            await fillForm({ firstName: "Lucy", lastName: "Chen", email: "lucy.chen@gmail.com", password: "mypassword" });
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /sign up/i }));
            });
            expect(screen.getByRole("button", { name: /signing up\.\.\./i })).toBeDisabled();
        });
    });

    describe("When someone is on the verification code screen", () => {

        const goToVerification = async () => {
            mockSignup.mockResolvedValueOnce({});
            render(<SignUpPage />);
            await fillForm({ firstName: "John", lastName: "Doe", email: "john@example.com", password: "mypassword" });
            await submitForm();
            await waitFor(() => screen.getByPlaceholderText("Enter confirmation code"));
        };

        it("tells them to check their email for the code", async () => {
            await goToVerification();
            expect(screen.getByText(/we sent a code to your email/i)).toBeInTheDocument();
        });

        it("tells them to check their phone for the code if they signed up with a phone number", async () => {
            mockSignup.mockResolvedValueOnce({});
            render(<SignUpPage />);
            await userEvent.click(screen.getByRole("button", { name: /^phone$/i }));
            await fillForm({ firstName: "John", lastName: "Doe", phone: "5559876543", password: "mypassword" });
            await submitForm();
            await waitFor(() => screen.getByPlaceholderText("Enter confirmation code"));
            expect(screen.getByText(/we sent a code to your phone/i)).toBeInTheDocument();
        });

        it("reminds them to actually type in the code before hitting Confirm", async () => {
            await goToVerification();
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /confirm/i }));
            });
            expect(await screen.findByText(/please enter the code/i)).toBeInTheDocument();
        });

        it("sends their email address and the code they typed to the server", async () => {
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

        it("sends their phone number and the code they typed to the server when they used phone verification", async () => {
            mockSignup.mockResolvedValueOnce({});
            mockVerify.mockResolvedValueOnce({});
            render(<SignUpPage />);
            await userEvent.click(screen.getByRole("button", { name: /^phone$/i }));
            await fillForm({ firstName: "Maria", lastName: "Garcia", phone: "5551112222", password: "mypassword" });
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

        it("sends them to the sign in page once their account is verified", async () => {
            mockVerify.mockResolvedValueOnce({});
            await goToVerification();
            await userEvent.type(screen.getByPlaceholderText("Enter confirmation code"), "123456");
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /confirm/i }));
            });
            await waitFor(() => expect(mockPush).toHaveBeenCalledWith("/signin"));
        });

        it("lets them know if the code they entered is wrong or has expired", async () => {
            mockVerify.mockRejectedValueOnce("Invalid or expired code.");
            await goToVerification();
            await userEvent.type(screen.getByPlaceholderText("Enter confirmation code"), "000000");
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /confirm/i }));
            });
            expect(await screen.findByText("Invalid or expired code.")).toBeInTheDocument();
        });

        it("asks the server to send a fresh code when they click Resend", async () => {
            mockResendCode.mockResolvedValueOnce({});
            await goToVerification();
            await act(async () => {
                fireEvent.click(screen.getByText(/resend code/i));
            });
            expect(mockResendCode).toHaveBeenCalledWith("john@example.com", undefined);
        });

        it("takes them back to the sign up form if they click Cancel", async () => {
            await goToVerification();
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /cancel/i }));
            });
            expect(screen.queryByPlaceholderText("Enter confirmation code")).not.toBeInTheDocument();
            expect(screen.getByPlaceholderText("Enter your first name")).toBeInTheDocument();
        });

        it("greys out the Confirm button so they can't submit the code twice while it's verifying", async () => {
            mockSignup.mockResolvedValueOnce({});
            mockVerify.mockImplementation(() => new Promise(() => {}));
            render(<SignUpPage />);
            await fillForm({ firstName: "John", lastName: "Doe", email: "john@example.com", password: "mypassword" });
            await submitForm();
            await waitFor(() => screen.getByPlaceholderText("Enter confirmation code"));
            await userEvent.type(screen.getByPlaceholderText("Enter confirmation code"), "654321");
            await act(async () => {
                fireEvent.click(screen.getByRole("button", { name: /confirm/i }));
            });
            expect(screen.getByRole("button", { name: /confirming\.\.\./i })).toBeDisabled();
        });
    });
});
