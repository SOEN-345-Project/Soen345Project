"use client";

import { useEffect, useRef, useState } from "react";
import { EventDto } from "@/lib/axios";

const formatDate = (iso: string) => {
    const d = new Date(iso);
    return {
        full: d.toLocaleDateString("en-US", { weekday: "long", year: "numeric", month: "long", day: "numeric" }),
        time: d.toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" }),
        day: d.toLocaleDateString("en-US", { day: "2-digit" }),
        month: d.toLocaleDateString("en-US", { month: "short" }).toUpperCase(),
    };
};

const CATEGORY_COLORS: Record<string, { dot: string; text: string; bg: string; border: string }> = {
    Concert:  { dot: "bg-violet-400", text: "text-violet-600", bg: "bg-violet-50",  border: "border-violet-200" },
    Movie:    { dot: "bg-rose-400",   text: "text-rose-600",   bg: "bg-rose-50",    border: "border-rose-200"   },
    Sports:   { dot: "bg-green-400",  text: "text-green-600",  bg: "bg-green-50",   border: "border-green-200"  },
    Theatre:  { dot: "bg-amber-400",  text: "text-amber-600",  bg: "bg-amber-50",   border: "border-amber-200"  },
    Festival: { dot: "bg-orange-400", text: "text-orange-600", bg: "bg-orange-50",  border: "border-orange-200" },
};

interface ReservationModalProps {
    event: EventDto | null;
    onClose: () => void;
    onConfirm: (eventId: number, ticketCount: number) => Promise<void>;
}

export default function ReservationModal({ event, onClose, onConfirm }: ReservationModalProps) {
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

    const dt = formatDate(event.eventDate);
    const cat = CATEGORY_COLORS[event.categoryName] ?? { dot: "bg-stone-400", text: "text-stone-600", bg: "bg-stone-50", border: "border-stone-200" };
    const maxTickets = event.totalTickets ?? 10;

    const handleConfirm = async () => {

        setStatus("loading");
        setErrorMsg("");
        try {
            await onConfirm(event.id, ticketCount);
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
            className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50"
        >
            <div className="w-full max-w-md bg-white rounded-2xl shadow-xl overflow-hidden">

                <div className={`h-1 w-full ${cat.dot}`} />

                <div className="px-6 pt-6 pb-4 border-b border-stone-100 flex items-start justify-between gap-4">
                    <div className="flex flex-col items-center bg-stone-50 border border-stone-200 rounded-xl px-3 py-2 shrink-0">
                        <span className="text-2xl font-bold text-stone-900">{dt.day}</span>
                        <span className="text-[9px] tracking-widest text-stone-400 uppercase font-semibold">{dt.month}</span>
                    </div>

                    <div className="flex-1 min-w-0">
                        <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-medium border mb-1 ${cat.bg} ${cat.text} ${cat.border}`}>
                            <span className={`w-1.5 h-1.5 rounded-full ${cat.dot}`} />
                            {event.categoryName}
                        </span>
                        <h2 className="text-lg font-semibold text-stone-900 truncate">{event.title}</h2>
                        {event.description && (
                            <p className="text-xs text-stone-400 mt-1 line-clamp-2">{event.description}</p>
                        )}
                    </div>

                    <button
                        onClick={onClose}
                        disabled={status === "loading"}
                        className="w-8 h-8 rounded-full bg-stone-100 flex items-center justify-center text-stone-400 hover:bg-stone-200 transition-colors disabled:opacity-40"
                    >
                        ✕
                    </button>
                </div>

                <div className="px-6 py-4 space-y-2 border-b border-stone-100">
                    <InfoRow label="Date & Time" value={`${dt.full} · ${dt.time}`} />
                    {(event.locationName || event.city) && (
                        <InfoRow label="Location" value={[event.locationName, event.city].filter(Boolean).join(", ")} />
                    )}
                    {event.totalTickets != null && (
                        <InfoRow label="Available tickets" value={`${event.totalTickets}`} />
                    )}
                </div>

                <div className="px-6 py-5">
                    {status === "success" ? (
                        <div className="bg-green-50 border border-green-200 rounded-xl p-5 text-center">
                            <p className="text-sm font-semibold text-green-800 mb-1">Reservation confirmed!</p>
                            <p className="text-xs text-green-600 mb-4">
                                {ticketCount} ticket{ticketCount !== 1 ? "s" : ""} successfully reserved.
                            </p>
                            <button onClick={onClose} className="w-full py-2.5 rounded-lg bg-green-700 text-white text-sm font-medium hover:bg-green-800 transition-colors">
                                Done
                            </button>
                        </div>
                    ) : (
                        <div className="space-y-4">
                            <div className="flex items-center justify-between">
                                <span className="text-sm text-stone-700 font-medium">Number of tickets</span>
                                <div className="flex items-center gap-3">
                                    <button
                                        onClick={() => setTicketCount((n) => Math.max(1, n - 1))}
                                        disabled={ticketCount <= 1 || status === "loading"}
                                        className="w-8 h-8 rounded-full border border-stone-200 bg-white flex items-center justify-center text-stone-600 hover:border-stone-400 disabled:opacity-30 transition-colors"
                                    >
                                        −
                                    </button>
                                    <span className="w-5 text-center text-lg font-semibold text-stone-900 tabular-nums">{ticketCount}</span>
                                    <button
                                        onClick={() => setTicketCount((n) => Math.min(maxTickets, n + 1))}
                                        disabled={ticketCount >= maxTickets || status === "loading"}
                                        className="w-8 h-8 rounded-full border border-stone-200 bg-white flex items-center justify-center text-stone-600 hover:border-stone-400 disabled:opacity-30 transition-colors"
                                    >
                                        +
                                    </button>
                                </div>
                            </div>

                            {status === "error" && errorMsg && (
                                <p className="text-xs text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
                                    {errorMsg}
                                </p>
                            )}

                            <button
                                onClick={handleConfirm}
                                disabled={status === "loading"}
                                className="w-full py-3 rounded-xl bg-stone-900 text-white text-sm font-medium hover:bg-stone-700 transition-colors disabled:opacity-60 flex items-center justify-center gap-2"
                            >
                                {status === "loading" ? (
                                    <>
                                        Confirming…
                                    </>
                                ) : (
                                    `Reserve ${ticketCount} ticket${ticketCount !== 1 ? "s" : ""}`
                                )}
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

const InfoRow = ({ label, value }: { label: string; value: string }) => (
    <div>
        <p className="text-[10px] font-semibold uppercase tracking-widest text-stone-500 mb-0.5">{label}</p>
        <p className="text-sm text-stone-600">{value}</p>
    </div>
);
