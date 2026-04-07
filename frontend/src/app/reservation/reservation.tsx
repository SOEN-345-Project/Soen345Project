"use client";

import { useEffect, useRef, useState } from "react";
import { EventDto, ReservationRequest, createReservation } from "@/lib/axios";
import {formatDateReservation} from "@/app/utils";

interface ReservationModalProps {
    event: EventDto | null;
    onClose: () => void;
}

export default function ReservationModal({ event, onClose}: ReservationModalProps) {
    const [ticketCount, setTicketCount] = useState(1);
    const [status, setStatus] = useState<"idle" | "loading" | "success" | "error">("idle");
    const [errorMsg, setErrorMsg] = useState("");
    const overlayRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (event) { setTicketCount(1); setStatus("idle"); setErrorMsg(""); }
    }, [event?.id]);

    useEffect(() => {
        const handler = (e: KeyboardEvent) => { if (e.key === "Escape" && status !== "loading") onClose(); };
        window.addEventListener("keydown", handler);
        return () => window.removeEventListener("keydown", handler);
    }, [onClose, status]);

    useEffect(() => {
        document.body.style.overflow = event ? "hidden" : "";
        return () => { document.body.style.overflow = ""; };
    }, [event]);

    if (!event) return null;

    const dt = formatDateReservation(event.eventDate);
    const maxTickets = event.totalTickets ?? 10;

    const handleConfirm = async () => {
        const request: ReservationRequest = {
            eventId: event.id,
            quantity: ticketCount,
        };
        setStatus("loading");
        setErrorMsg("");
        try {
            const response = await createReservation(String(sessionStorage.getItem('token')), request);
            console.log(response);
            setStatus("success");
        } catch (err: any) {
            setErrorMsg(err?.message ?? "Something went wrong. Please try again.");
            setStatus("error");
        }
    };

    return (
        <div
            ref={overlayRef}
            onClick={(e) => { if (e.target === overlayRef.current && status !== "loading") onClose(); }}
            style={{ position: "fixed", inset: 0, zIndex: 50, display: "flex", alignItems: "center", justifyContent: "center", padding: 16, backgroundColor: "rgba(0,0,0,0.5)" }}
        >
            <div style={{ width: "100%", maxWidth: 440, backgroundColor: "#fff", borderRadius: 8, border: "1px solid #e5e7eb", padding: 24 }}>

                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 16 }}>
                    <div>
                        <p style={{ fontSize: 11, color: "#6b7280", marginBottom: 4 }}>{event.categoryName}</p>
                        <h2 style={{ fontSize: 17, fontWeight: 600, color: "#111827", margin: 0 }}>{event.title}</h2>
                        {event.description && <p style={{ fontSize: 13, color: "#6b7280", marginTop: 4 }}>{event.description}</p>}
                    </div>
                    <button onClick={onClose} disabled={status === "loading"} style={{ background: "none", border: "none", cursor: "pointer", fontSize: 16, color: "#6b7280" }}>✕</button>
                </div>

                <div style={{ borderTop: "1px solid #e5e7eb", borderBottom: "1px solid #e5e7eb", paddingBlock: 12, marginBottom: 16, fontSize: 13, color: "#6b7280" }}>
                    <p style={{ margin: "0 0 4px" }}><strong>Date:</strong> {dt.full} · {dt.time}</p>
                    {(event.locationName || event.city) && (
                        <p style={{ margin: "0 0 4px" }}><strong>Location:</strong> {[event.locationName, event.city].filter(Boolean).join(", ")}</p>
                    )}
                    {event.totalTickets != null && (
                        <p style={{ margin: 0 }}><strong>Available:</strong> {event.totalTickets} tickets</p>
                    )}
                </div>

                {status === "success" ? (
                    <div style={{ textAlign: "center" }}>
                        <p style={{ color: "#15803d", fontWeight: 600, marginBottom: 4 }}>A confirmation has been sent to your email.</p>
                        <p style={{ fontSize: 13, color: "#6b7280", marginBottom: 16 }}>{ticketCount} ticket{ticketCount !== 1 ? "s" : ""} successfully reserved.</p>
                        <button onClick={onClose} style={{ width: "100%", padding: "8px 0", backgroundColor: "#15803d", color: "#fff", border: "none", borderRadius: 6, cursor: "pointer", fontSize: 14 }}>Done</button>
                    </div>
                ) : (
                    <div>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
                            <span style={{ fontSize: 14, color: "#374151" }}>Number of tickets</span>
                            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                                <button onClick={() => setTicketCount((n) => Math.max(1, n - 1))} disabled={ticketCount <= 1 || status === "loading"} style={{ width: 28, height: 28, border: "1px solid #111827", borderRadius: "50%", background: "#fff", cursor: "pointer", fontSize: 16, color: "#111827" }}>−</button>
                                <span style={{ fontSize: 16, fontWeight: 600, minWidth: 20, textAlign: "center", color: "#111827" }}>{ticketCount}</span>
                                <button onClick={() => setTicketCount((n) => Math.min(maxTickets, n + 1))} disabled={ticketCount >= maxTickets || status === "loading"} style={{ width: 28, height: 28, border: "1px solid #111827", borderRadius: "50%", background: "#fff", cursor: "pointer", fontSize: 16, color: "#111827" }}>+</button>
                            </div>
                        </div>

                        {status === "error" && errorMsg && (
                            <p style={{ fontSize: 13, color: "#dc2626", marginBottom: 12 }}>{errorMsg}</p>
                        )}

                        <button
                            onClick={handleConfirm}
                            disabled={status === "loading"}
                            style={{ width: "100%", padding: "9px 0", backgroundColor: "#111827", color: "#fff", border: "none", borderRadius: 6, cursor: status === "loading" ? "not-allowed" : "pointer", fontSize: 14, opacity: status === "loading" ? 0.6 : 1 }}
                        >
                            {status === "loading" ? "Confirming…" : `Reserve ${ticketCount} ticket${ticketCount !== 1 ? "s" : ""}`}
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}