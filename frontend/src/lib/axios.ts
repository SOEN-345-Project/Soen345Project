import axios from "axios";

const api = axios.create({
    baseURL: "http://localhost:8000",
    headers: {
        "Content-Type": "application/json",
    },
});

api.interceptors.response.use(
    (res) => res.data,
    (err) => Promise.reject(
        err.response?.data?.message ??
        err.response?.data ??
        err.message ??
        "Something went wrong"
    )
);

export interface EventDto {
    id: number;
    title: string;
    description: string;
    eventDate: string;
    categoryName: string;
    locationName: string;
    city: string;
    totalTickets: number;
}

export interface EventFilterParams {
    categoryId?: number;
    locationId?: number;
    startDate?: string;
    endDate?: string;
}

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

export interface ReservationRequest {
    eventId: number;
    quantity: number;
}

export const signup      = async (data: RegisterUserDto): Promise<any> => api.post("/auth/signup", data);
export const signupAdmin = async (data: RegisterUserDto): Promise<any> => api.post("/auth/signup/admin", data);
export const login       = async (data: LoginUserDto):   Promise<any> => api.post("/auth/login", data);
export const verify      = async (data: VerifyUserDto):  Promise<any> => api.post("/auth/verify", data);
export const resendCode  = async (email?: string, phoneNumber?: string): Promise<any> => api.post("/auth/resend", null, { params: { email, phoneNumber } });
export const checkEmail  = async (email: string):        Promise<any> => api.get("/auth/check-email", { params: { email } });
export const testAuth    = async ():                     Promise<any> => api.get("/auth/test");
export const getAllEvents = async (): Promise<EventDto[]> =>
    api.get("/api/events");

export const searchEvents = async (keyword: string): Promise<EventDto[]> =>
    api.get("/api/events/search", { params: { keyword } });

export const filterEvents = async (params: EventFilterParams): Promise<EventDto[]> =>
    api.get("/api/events/filter", { params });

export async function createReservation(accessToken: string, data: ReservationRequest): Promise<any> {
    const response = await api.post("/api/reservations", data, {
        headers: { Authorization: `Bearer ${accessToken}` }
    });
    console.log("createReservation response:", response);
    return response;
}

export async function getAllReservations(accessToken: string): Promise<any> {
    const response = await api.get("/api/reservations/my", {
        headers: { Authorization: `Bearer ${accessToken}` }
    });
    return response;
}

export async function cancelReservation(accessToken: string, reservationId: number): Promise<any> {
    const response = await api.delete(`/api/reservations/${reservationId}`, {
        headers: { Authorization: `Bearer ${accessToken}` }
    });
    return response;
}

export default api;