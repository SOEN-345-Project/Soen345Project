"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { signup, verify, resendCode } from "@/lib/axios";

type VerificationMethod = "EMAIL" | "PHONE";

const SignUpPage = () => {
    const router = useRouter();

    const [email, setEmail] = useState("");
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [phonenumber, setPhoneNumber] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [verificationMethod, setVerificationMethod] = useState<VerificationMethod>("EMAIL");

    const [showVerification, setShowVerification] = useState(false);
    const [verificationCode, setVerificationCode] = useState("");
    const [verifyError, setVerifyError] = useState("");
    const [verifyLoading, setVerifyLoading] = useState(false);

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError("");

        if (!firstName || !lastName || !password) {
            setError("Please fill in all fields.");
            return;
        }
        if (verificationMethod === "EMAIL" && !email) {
            setError("Email is required for email verification.");
            return;
        }
        if (verificationMethod === "PHONE" && !phonenumber) {
            setError("Phone number is required for phone verification.");
            return;
        }

        setLoading(true);
        try {
            await signup({ firstName, lastName, email, password, phoneNumber: phonenumber, verificationMethod });
            setShowVerification(true);
        } catch (err: any) {
            setError(err ?? "Signup failed.");
        } finally {
            setLoading(false);
        }
    };

    const handleVerify = async () => {
        setVerifyError("");
        if (!verificationCode.trim()) {
            setVerifyError("Please enter the code.");
            return;
        }
        setVerifyLoading(true);
        try {
            await verify({
                email: verificationMethod === "EMAIL" ? email : "",
                phoneNumber: verificationMethod === "PHONE" ? phonenumber : "",
                verificationCode,
            });
            router.push("/signin");
        } catch (err: any) {
            setVerifyError(err ?? "Invalid code.");
        } finally {
            setVerifyLoading(false);
        }
    };

    const handleResend = async () => {
        try {
            await resendCode(
                verificationMethod === "EMAIL" ? email : undefined,
                verificationMethod === "PHONE" ? phonenumber : undefined,
            );
        } catch (err: any) {
            setVerifyError(err ?? "Failed to resend code.");
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-400 px-4">
            <div className="w-full max-w-md bg-white p-8 rounded-2xl shadow-xl">

                <h1 className="text-3xl font-bold text-center mb-6 text-gray-900">Sign Up</h1>

                {error && <div className="mb-4 text-red-600 text-sm text-center">{error}</div>}

                {!showVerification && (
                    <form onSubmit={handleSubmit} className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium mb-2 text-gray-700">First Name</label>
                            <input type="text" className="w-full px-4 py-2 border border-gray-300 rounded-lg text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="Enter your first name" value={firstName} onChange={(e) => setFirstName(e.target.value)} />
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-2 text-gray-700">Last Name</label>
                            <input type="text" className="w-full px-4 py-2 border border-gray-300 rounded-lg text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="Enter your last name" value={lastName} onChange={(e) => setLastName(e.target.value)} />
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-2 text-gray-700">
                                Email {verificationMethod === "EMAIL" && <span className="text-red-500">*</span>}
                            </label>
                            <input type="email" className="w-full px-4 py-2 border border-gray-300 rounded-lg text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="Enter your email" value={email} onChange={(e) => setEmail(e.target.value)} />
                        </div>

                        {/* ✅ FIXED: Verify via toggle — both Email and Phone buttons now rendered correctly */}
                        <div>
                            <label className="block text-sm font-medium mb-2 text-gray-700">Verify via</label>
                            <div className="grid grid-cols-2 gap-2">
                                <button
                                    type="button"
                                    onClick={() => setVerificationMethod("EMAIL")}
                                    className={`py-2 px-4 rounded-lg text-sm font-medium border transition-all duration-150 ${
                                        verificationMethod === "EMAIL"
                                            ? "bg-blue-600 text-white border-blue-600 shadow-sm"
                                            : "bg-white text-gray-600 border-gray-300 hover:bg-gray-50"
                                    }`}
                                >
                                     Email
                                </button>
                                <button
                                    type="button"
                                    onClick={() => setVerificationMethod("PHONE")}
                                    className={`py-2 px-4 rounded-lg text-sm font-medium border transition-all duration-150 ${
                                        verificationMethod === "PHONE"
                                            ? "bg-blue-600 text-white border-blue-600 shadow-sm"
                                            : "bg-white text-gray-600 border-gray-300 hover:bg-gray-50"
                                    }`}
                                >
                                     Phone
                                </button>
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-2 text-gray-700">
                                Phone Number {verificationMethod === "PHONE" && <span className="text-red-500">*</span>}
                            </label>
                            <input type="tel" className="w-full px-4 py-2 border border-gray-300 rounded-lg text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="Enter your phone number" value={phonenumber} onChange={(e) => setPhoneNumber(e.target.value)} />
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-2 text-gray-700">Password</label>
                            <input type="password" className="w-full px-4 py-2 border border-gray-300 rounded-lg text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="Enter your password" value={password} onChange={(e) => setPassword(e.target.value)} />
                        </div>

                        <button type="submit" disabled={loading} className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition duration-200 font-medium disabled:opacity-50">
                            {loading ? "Signing up..." : "Sign Up"}
                        </button>
                    </form>
                )}

                {showVerification && (
                    <div className="mt-5 border border-blue-200 bg-blue-50 rounded-xl p-4">
                        <p className="text-sm text-blue-800 font-medium mb-3">
                            We sent a code to your {verificationMethod === "EMAIL" ? "email" : "phone"} — enter it below to confirm your account.
                        </p>
                        <div className="flex gap-2">
                            <input
                                type="text"
                                className="flex-1 px-3 py-2 border border-blue-300 rounded-lg text-gray-900 placeholder-gray-400 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                                placeholder="Enter confirmation code"
                                value={verificationCode}
                                onChange={(e) => setVerificationCode(e.target.value)}
                                autoFocus
                            />
                            <button
                                onClick={handleVerify}
                                disabled={verifyLoading}
                                className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition duration-200 whitespace-nowrap disabled:opacity-50"
                            >
                                {verifyLoading ? "Confirming..." : "Confirm"}
                            </button>
                        </div>
                        {verifyError && <p className="mt-2 text-xs text-red-600">{verifyError}</p>}
                        <p className="mt-2 text-xs text-blue-600 cursor-pointer hover:underline w-fit" onClick={handleResend}>
                            Resend code
                        </p>
                    </div>
                )}

                {showVerification && (
                    <div className="flex justify-center mt-4">
                        <button
                            onClick={() => setShowVerification(false)}
                            className="px-4 py-2 bg-red-600 text-white text-sm font-medium rounded-lg hover:bg-red-700 transition duration-200 whitespace-nowrap"
                        >
                            Cancel
                        </button>
                    </div>
                )}

                <p className="mt-6 text-center text-sm text-gray-600">
                    Already have an account?{" "}
                    <span onClick={() => router.push("/signin")} className="text-blue-600 cursor-pointer hover:underline font-medium">
                        Sign In
                    </span>
                </p>
            </div>
        </div>
    );
};

export default SignUpPage;