"use client";

import {useEffect, useEffectEvent, useState} from "react";
import { useRouter } from "next/navigation";
import { login,isAdmin } from "@/lib/axios";

const SignInPage = () => {

    const router = useRouter();

    const [identifier, setIdentifier] = useState<string>("");
    const [password, setPassword] = useState<string>("");
    const [error, setError] = useState<string>("");
    const [loading, setLoading] = useState(false);

    const isPhone = (value: string) => /^[\d\s\+\-\(\)]{7,}$/.test(value.trim());
    useEffect(() => {
        sessionStorage.clear();
    }, []);

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError("");

        if (!identifier || !password) {
            setError("Please fill in all fields.");
            return;
        }

        setLoading(true);
        try {
            const response = await login({
                email: isPhone(identifier) ? "" : identifier,
                phoneNumber: isPhone(identifier) ? identifier : "",
                password,
            });


            const token = response?.token ?? response?.data?.token;

            if (!token) {
                setError("Login failed: no token received.");
                return;
            }
            const isadmin = await isAdmin();
            sessionStorage.clear();
            sessionStorage.setItem("userLoggedIn", "true");
            sessionStorage.setItem("token", token);
            if (isadmin) {sessionStorage.setItem("isAdmin", "true");}

            if(isadmin){
            router.push("/adminEvent");}
            else {router.push("/event");}
        } catch (err: any) {

            const message =
                typeof err === "string"
                    ? err
                    : err?.message ?? err?.error ?? "Invalid credentials.";
            setError(message);
        } finally {
            setLoading(false);
        }
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
                        disabled={loading}
                        className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition duration-200 font-medium disabled:opacity-50">
                        {loading ? "Signing in..." : "Sign In"}
                    </button>
                </form>

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