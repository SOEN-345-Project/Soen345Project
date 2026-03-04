"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

const SignInPage = () => {
    const router = useRouter();
    type VerificationMethod = "EMAIL" | "PHONE";

    const [identifier, setIdentifier] = useState<string>("");
    const [password, setPassword] = useState<string>("");
    const [error, setError] = useState<string>("");
    const [showVerification, setShowVerification] = useState(false);
    const [verificationCode, setVerificationCode] = useState("");
    const [verifyError, setVerifyError] = useState("");
    const [verificationMethod, setVerificationMethod] = useState<VerificationMethod>("EMAIL");

    const isPhone = (value: string) => /^[\d\s\+\-\(\)]{7,}$/.test(value.trim());

    const detectMethod = (value: string): VerificationMethod => {
        return isPhone(value) ? "PHONE" : "EMAIL";
    };

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError("");

        if (!identifier || !password) {
            setError("Please fill in all fields.");
            return;
        }

        const method = detectMethod(identifier);
        setVerificationMethod(method);

        try {
            // Replace with your real API call
            // const response = await axios.post("/api/auth/login", {
            //     [method === "EMAIL" ? "email" : "phone"]: identifier,
            //     password
            // });

            console.log("Logging in with:", { identifier, password, method });
            setShowVerification(true);

        } catch (err) {
            setError("Invalid credentials.");
        }
    };

    const handleVerify = () => {
        setVerifyError("");
        if (!verificationCode.trim()) {
            setVerifyError("Please enter the code.");
            return;
        }
        console.log("Verifying with code:", verificationCode);
        router.push("/dashboard");
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-400 px-4">
            <div className="w-full max-w-md bg-white p-8 rounded-2xl shadow-xl">

                <h1 className="text-3xl font-bold text-center mb-6 text-gray-900">
                    Sign In
                </h1>

                {error && (
                    <div className="mb-4 text-red-600 text-sm text-center">
                        {error}
                    </div>
                )}
                {!showVerification && (
                <form onSubmit={handleSubmit} className="space-y-4">

                    <div className="mb-6">
                        <label className="block text-sm font-medium mb-2 text-gray-700">
                            Email or Phone Number
                        </label>
                        <div className="relative">
                            <input
                                type="text"
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder="Enter your email or phone"
                                value={identifier}
                                onChange={(e) => setIdentifier(e.target.value)}
                            />
                            {identifier.length > 0 && (
                                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-medium px-2 py-0.5 rounded-full bg-blue-100 text-blue-700">
                                    {isPhone(identifier) ? "📱 Phone" : "✉️ Email"}
                                </span>
                            )}
                        </div>
                        {identifier.length > 0 && (
                            <p className="mt-1 text-xs text-gray-400">
                                {isPhone(identifier)
                                    ? "We'll send a confirmation code via SMS."
                                    : "We'll send a confirmation code to your email."}
                            </p>
                        )}
                    </div>

                    <div className="mb-8">
                        <label className="block text-sm font-medium mb-2 text-gray-700">
                            Password
                        </label>
                        <input
                            type="password"
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
                            placeholder="Enter your password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                        />
                    </div>

                    <button
                        type="submit"
                        className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition duration-200 font-medium">
                        Sign In
                    </button>
                </form>)}

                {showVerification && (
                    <div className="mt-5 border border-blue-200 bg-blue-50 rounded-xl p-5">
                        <p className="text-sm text-blue-800 font-medium mb-4 text-center">
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
                                className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition duration-200 whitespace-nowrap"
                            >
                                Confirm
                            </button>
                        </div>
                        {verifyError && <p className="mt-2 text-xs text-red-600 text-center">{verifyError}</p>}
                        <div className="flex items-center justify-between mt-3">
                            <p className="text-xs text-blue-600 cursor-pointer hover:underline" onClick={() => console.log("Resend")}>
                                Resend code
                            </p>
                            <button
                                onClick={() => setShowVerification(false)}
                                className="text-xs text-red-500 border border-red-300 px-3 py-1 rounded-lg hover:bg-red-50 transition duration-150 font-medium"
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                )}
                <p className="mt-6 text-center text-sm text-gray-600">
                    Don't have an account?{" "}
                    <span
                        onClick={() => router.push("/signup")}
                        className="text-blue-600 cursor-pointer hover:underline font-medium"
                    >
                        Sign Up
                    </span>
                </p>
            </div>
        </div>
    );
};

export default SignInPage;