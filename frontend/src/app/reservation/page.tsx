"use client";
import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
interface Reservation {
    id: number;
    event: string;
    date: string;
    seat: string;
    tickets: number;
    price: string;
    status: "CONFIRMED" | "PENDING" | "CANCELLED";
}

const BASE_URL = "/api/reservations";
const getToken = () => sessionStorage.getItem("token") ?? "";

async function fetchMyReservations(): Promise<Reservation[]> {
    const res = await fetch(`${BASE_URL}/my`, {
        headers: { Authorization: `Bearer ${getToken()}` },
    });
    if (!res.ok) throw new Error("Failed to load reservations");
    return res.json();
}

async function cancelReservation(id: number): Promise<void> {
    const res = await fetch(`${BASE_URL}/${id}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${getToken()}` },
    });
    if (!res.ok) {
        const err = await res.json().catch(() => ({ message: "Cancel failed" }));
        throw new Error(err.message);
    }
}

export default function ReservationsList() {
    const [reservations, setReservations] = useState<Reservation[]>([]);
    const [loading, setLoading] = useState(true);
    const [cancellingIds, setCancellingIds] = useState<Set<number>>(new Set());
    const [error, setError] = useState<string | null>(null);
    const router = useRouter();
    const load = useCallback(async () => {
        setError(null);
        try {
            const data: Reservation[] = [
                { id: 1, event: "Jazz Night at Théâtre Rialto", date: "Apr 5, 2026", seat: "Row C, Seat 12", tickets: 2, price: "$64.00", status: "CONFIRMED" },
                { id: 2, event: "Montreal Comedy Festival", date: "Apr 18, 2026", seat: "Row A, Seat 7", tickets: 1, price: "$38.00", status: "PENDING" },
                { id: 3, event: "OSM — Beethoven Symphony No. 9", date: "May 2, 2026", seat: "Balcony, Seat 3", tickets: 4, price: "$128.00", status: "CONFIRMED" },
            ];
            setReservations(data);
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        if (!sessionStorage.getItem("token")) {
            router.push("/signin");
        }
    }, []);

    useEffect(() => {
        load();
    }, [load]);



    const handleCancel = async (id: number) => {
        setCancellingIds((prev) => new Set(prev).add(id));
        try {
            await cancelReservation(id);
            setReservations((prev) =>
                prev.map((r) => (r.id === id ? { ...r, status: "CANCELLED" } : r))
            );
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setCancellingIds((prev) => {
                const next = new Set(prev);
                next.delete(id);
                return next;
            });
        }
    };

    if (loading) return (
        <div style={{ minHeight: "100vh", backgroundColor: "#ffffff", colorScheme: "light", display: "flex", justifyContent: "center", alignItems: "center" }}>
            <p style={{ color: "#6b7280", fontSize: 14 }}>Loading...</p>
        </div>
    );

    return (
        <div style={{ minHeight: "100vh", overflowY: "auto", backgroundColor: "#ffffff", colorScheme: "light" }}>
            <div style={{ maxWidth: 600, margin: "0 auto", padding: "2rem 1rem" }}>
                <header className="py-14 border-b border-stone-200 mb-10">
                    <div className="flex items-center justify-between">
                        <div>
                            <h1 className="text-4xl font-bold text-stone-900 tracking-tight">Reservations</h1>

                        </div>
                        <div className="flex items-center gap-2">
                            <button
                                onClick={() => {  router.push("/event"); }}
                                className="px-4 py-2 bg-blue-700 text-white text-sm font-medium rounded-lg hover:bg-blue-800 active:scale-95 transition-all"
                            >
                                Event
                            </button>
                           <button
                            onClick={() => { sessionStorage.clear(); router.push("/signin"); }}
                            className="px-4 py-2 bg-red-700 text-white text-sm font-medium rounded-lg hover:bg-red-800 active:scale-95 transition-all"
                        >
                            Logout
                        </button>
                        </div>
                    </div>
                </header>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
          <span style={{ fontSize: 14, color: "#6b7280" }}>
            {reservations.length} reservation{reservations.length !== 1 ? "s" : ""}
          </span>
                </div>

                {error && <p style={{ color: "#dc2626", fontSize: 14 }}>{error}</p>}

                {reservations.length === 0 ? (
                    <p style={{ color: "#6b7280", fontSize: 14 }}>No reservations found.</p>
                ) : (
                    reservations.map((r) => (
                        <div
                            key={r.id}
                            style={{
                                border: "1px solid #e5e7eb",
                                borderRadius: 8,
                                padding: "14px 16px",
                                marginBottom: 10,
                                backgroundColor: "#ffffff",
                            }}
                        >
                            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 6 }}>
                                <strong style={{ fontSize: 15, color: "#111827" }}>{r.event}</strong>
                                <span style={{ fontSize: 12, color: "#6b7280" }}>{r.status}</span>
                            </div>
                            <div style={{ fontSize: 13, color: "#6b7280", marginBottom: 12 }}>
                                <span>{r.date}</span> &middot; <span>{r.seat}</span> &middot;{" "}
                                <span>{r.tickets} ticket{r.tickets !== 1 ? "s" : ""}</span> &middot;{" "}
                                <span>{r.price}</span>
                            </div>
                            {r.status !== "CANCELLED" && (
                                <button
                                    onClick={() => handleCancel(r.id)}
                                    disabled={cancellingIds.has(r.id)}
                                    style={{
                                        width: "100%",
                                        padding: "8px 0",
                                        fontSize: 13,
                                        cursor: cancellingIds.has(r.id) ? "not-allowed" : "pointer",
                                        backgroundColor: "#ffffff",
                                        color: "#374151",
                                        border: "1px solid #e5e7eb",
                                        borderRadius: 6,
                                    }}
                                >
                                    {cancellingIds.has(r.id) ? "Cancelling..." : "Cancel reservation"}
                                </button>
                            )}
                        </div>
                    ))
                )}
            </div>
        </div>
    );
}