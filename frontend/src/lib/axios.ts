// src/lib/axios.ts
import axios from "axios";

const api = axios.create({
    baseURL: "http://localhost:8000",
    headers: {
        "Content-Type": "application/json",
    },
});


api.interceptors.response.use(
    (res) => res.data,
    (err) => Promise.reject(err.response?.data ?? err.message)
);

// ── Types ─────────────────────────────────────────────────────────────────────

export interface RegisterUserDto {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    phoneNumber: string;
    verificationMethod: string;
}

export interface LoginUserDto {
    email: string;
    phoneNumber: string;
    password: string;
}

export interface VerifyUserDto {
    email: string;
    phoneNumber: string;
    verificationCode: string;
}

export const signup      = async (data: RegisterUserDto): Promise<any> => api.post("/auth/signup", data);
export const signupAdmin = async (data: RegisterUserDto): Promise<any> => api.post("/auth/signup/admin", data);
export const login       = async (data: LoginUserDto):   Promise<any> => api.post("/auth/login", data);
export const verify      = async (data: VerifyUserDto):  Promise<any> => api.post("/auth/verify", data);
export const resendCode  = async (email?: string, phoneNumber?: string): Promise<any> => api.post("/auth/resend", null, { params: { email, phoneNumber } });
export const checkEmail  = async (email: string):        Promise<any> => api.get("/auth/check-email", { params: { email } });
export const testAuth    = async ():                     Promise<any> => api.get("/auth/test");

export default api;