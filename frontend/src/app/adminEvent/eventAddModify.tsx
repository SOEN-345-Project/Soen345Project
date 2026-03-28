"use client";

import { useEffect, useRef, useState } from "react";
import { EventDto,createEvent, updateEvent } from "@/lib/axios";

export interface AdminEventRequest {
    title: string;
    description?: string;
    eventDate: string;
    categoryId: number;
    locationId: number;
    totalTickets: number;
}

interface EventFormModalProps {
    event: EventDto | null;
    mode: "add" | "modify";
    onClose: () => void;
}

const CATEGORY_OPTIONS = [
    { id: 1, name: "Concert" },
    { id: 2, name: "Movie" },
    { id: 3, name: "Sports" },
    { id: 4, name: "Theatre" },
    { id: 5, name: "Festival" },
];

const LOCATION_OPTIONS = [
    { id: 1, name: "Bell Centre" },
    { id: 2, name: "Place des Arts" },
    { id: 3, name: "Olympic Stadium" },
    { id: 4, name: "MTELUS" },
    { id: 5, name: "Scotiabank Arena" },
];

const toDatetimeLocal = (iso: string) => {
    if (!iso) return "";
    return new Date(iso).toISOString().slice(0, 16);
};

const empty: AdminEventRequest = {
    title: "",
    description: "",
    eventDate: "",
    categoryId: 0,
    locationId: 0,
    totalTickets: 1,
};

export default function EventFormModal({ event, mode, onClose }: EventFormModalProps) {
    const overlayRef = useRef<HTMLDivElement>(null);

    const [form, setForm]         = useState<AdminEventRequest>(empty);
    const [status, setStatus]     = useState<"idle" | "loading" | "success" | "error">("idle");
    const [errorMsg, setErrorMsg] = useState("");

    useEffect(() => {
        if (mode === "modify" && event) {
            setForm({
                title:        event.title ?? "",
                description:  event.description ?? "",
                eventDate:    toDatetimeLocal(event.eventDate),
                categoryId:   CATEGORY_OPTIONS.find((c) => c.name === event.categoryName)?.id ?? 0,
                locationId:   LOCATION_OPTIONS.find((l) => l.name === event.locationName)?.id ?? 0,
                totalTickets: event.totalTickets ?? 1,
            });
        } else {
            setForm(empty);
        }
        setStatus("idle");
        setErrorMsg("");
    }, [event, mode]);

    useEffect(() => {
        const handler = (e: KeyboardEvent) => {
            if (e.key === "Escape" && status !== "loading") onClose();
        };
        window.addEventListener("keydown", handler);
        return () => window.removeEventListener("keydown", handler);
    }, [onClose, status]);

    useEffect(() => {
        document.body.style.overflow = "hidden";
        return () => { document.body.style.overflow = ""; };
    }, []);

    const set = (field: keyof AdminEventRequest, value: string | number) =>
        setForm((prev) => ({ ...prev, [field]: value }));

    const handleSubmit = async () => {
        if (!form.title.trim())    return setErrorMsg("Title is required.");
        if (!form.eventDate)       return setErrorMsg("Event date is required.");
        if (!form.categoryId)      return setErrorMsg("Please select a category.");
        if (!form.locationId)      return setErrorMsg("Please select a location.");
        if (form.totalTickets < 1) return setErrorMsg("Total tickets must be at least 1.");

        setStatus("loading");
        setErrorMsg("");
        try {
            const payload: AdminEventRequest = {
                ...form,
                eventDate: new Date(form.eventDate).toISOString(),
            };

            if (isModify && event?.id) {
                await updateEvent(event.id, payload);
            } else {
                await createEvent(payload);
            }
            setStatus("success");
        } catch (err: any) {
            setErrorMsg(err?.message ?? "Something went wrong. Please try again.");
            setStatus("error");
        }
    };

    const inputStyle: React.CSSProperties = {
        width: "100%",
        padding: "8px 10px",
        fontSize: 13,
        color: "#111827",
        border: "1px solid #d1d5db",
        borderRadius: 6,
        outline: "none",
        backgroundColor: "#fff",
        boxSizing: "border-box",
    };

    const labelStyle: React.CSSProperties = {
        display: "block",
        fontSize: 12,
        fontWeight: 600,
        color: "#374151",
        marginBottom: 4,
    };

    const fieldStyle: React.CSSProperties = { marginBottom: 12 };

    const isModify = mode === "modify";
    const title    = isModify ? "Modify Event" : "Add New Event";
    const btnLabel = isModify ? "Save Changes" : "Create Event";

    return (
        <div
            ref={overlayRef}
            onClick={(e) => { if (e.target === overlayRef.current && status !== "loading") onClose(); }}
            style={{
                position: "fixed", inset: 0, zIndex: 50,
                display: "flex", alignItems: "center", justifyContent: "center",
                padding: 16, backgroundColor: "rgba(0,0,0,0.5)",
            }}
        >
            <div style={{
                width: "100%", maxWidth: 480,
                backgroundColor: "#fff", borderRadius: 10,
                border: "1px solid #e5e7eb", padding: 24,
                maxHeight: "90vh", overflowY: "auto",
            }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
                    <div>
                        <p style={{ fontSize: 11, color: "#6b7280", marginBottom: 2 }}>
                            {isModify ? `Editing · ${event?.title}` : "New event"}
                        </p>
                        <h2 style={{ fontSize: 17, fontWeight: 700, color: "#111827", margin: 0 }}>{title}</h2>
                    </div>
                    <button
                        onClick={onClose}
                        disabled={status === "loading"}
                        style={{ background: "none", border: "none", cursor: "pointer", fontSize: 16, color: "#6b7280" }}
                    >
                        ✕
                    </button>
                </div>

                {status === "success" ? (
                    <div style={{ textAlign: "center", paddingBlock: 24 }}>
                        <p style={{ color: "#15803d", fontWeight: 600, fontSize: 15, marginBottom: 6 }}>
                            {isModify ? "Event updated!" : "Event created!"}
                        </p>
                        <p style={{ fontSize: 13, color: "#6b7280", marginBottom: 20 }}>
                            {isModify
                                ? "The event has been successfully updated."
                                : "The new event has been successfully added."}
                        </p>
                        <button
                            onClick={onClose}
                            style={{ width: "100%", padding: "9px 0", backgroundColor: "#15803d", color: "#fff", border: "none", borderRadius: 6, cursor: "pointer", fontSize: 14 }}
                        >
                            Done
                        </button>
                    </div>
                ) : (
                    <>
                        <div style={fieldStyle}>
                            <label style={labelStyle}>Title <span style={{ color: "#ef4444" }}>*</span></label>
                            <input
                                style={inputStyle}
                                type="text"
                                placeholder="Event title"
                                value={form.title}
                                onChange={(e) => set("title", e.target.value)}
                                disabled={status === "loading"}
                            />
                        </div>

                        <div style={fieldStyle}>
                            <label style={labelStyle}>Description</label>
                            <textarea
                                style={{ ...inputStyle, resize: "vertical", minHeight: 72 }}
                                placeholder="Optional description"
                                value={form.description}
                                onChange={(e) => set("description", e.target.value)}
                                disabled={status === "loading"}
                            />
                        </div>

                        <div style={fieldStyle}>
                            <label htmlFor="event-date" style={labelStyle}>Event Date <span style={{ color: "#ef4444" }}>*</span></label>
                            <input
                                style={inputStyle}
                                id="event-date"
                                type="datetime-local"
                                value={form.eventDate}
                                onChange={(e) => set("eventDate", e.target.value)}
                                disabled={status === "loading"}
                            />
                        </div>

                        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 12 }}>
                            <div>
                                <label style={labelStyle}>Category <span style={{ color: "#ef4444" }}>*</span></label>
                                <select
                                    style={inputStyle}
                                    value={form.categoryId}
                                    onChange={(e) => set("categoryId", Number(e.target.value))}
                                    disabled={status === "loading"}
                                >
                                    <option value={0} disabled>Select…</option>
                                    {CATEGORY_OPTIONS.map((c) => (
                                        <option key={c.id} value={c.id}>{c.name}</option>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label style={labelStyle}>Location <span style={{ color: "#ef4444" }}>*</span></label>
                                <select
                                    style={inputStyle}
                                    value={form.locationId}
                                    onChange={(e) => set("locationId", Number(e.target.value))}
                                    disabled={status === "loading"}
                                >
                                    <option value={0} disabled>Select…</option>
                                    {LOCATION_OPTIONS.map((l) => (
                                        <option key={l.id} value={l.id}>{l.name}</option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        <div style={fieldStyle}>
                            <label style={labelStyle}>Total Tickets <span style={{ color: "#ef4444" }}>*</span></label>
                            <input
                                style={inputStyle}
                                type="number"
                                min={1}
                                value={form.totalTickets}
                                onChange={(e) => set("totalTickets", Math.max(1, Number(e.target.value)))}
                                disabled={status === "loading"}
                            />
                        </div>

                        {errorMsg && (
                            <p style={{ fontSize: 13, color: "#dc2626", marginBottom: 12 }}>{errorMsg}</p>
                        )}

                        <div style={{ display: "flex", gap: 8, marginTop: 4 }}>
                            <button
                                onClick={onClose}
                                disabled={status === "loading"}
                                style={{
                                    flex: 1, padding: "9px 0",
                                    backgroundColor: "#fef2f2", color: "#dc2626",
                                    border: "1px solid #fecaca", borderRadius: 6,
                                    cursor: "pointer", fontSize: 14,
                                }}
                            >
                                Cancel
                            </button>
                            <button
                                onClick={handleSubmit}
                                disabled={status === "loading"}
                                style={{
                                    flex: 2, padding: "9px 0",
                                    backgroundColor: isModify ? "#1d4ed8" : "#15803d",
                                    color: "#fff", border: "none", borderRadius: 6,
                                    cursor: status === "loading" ? "not-allowed" : "pointer",
                                    fontSize: 14, opacity: status === "loading" ? 0.6 : 1,
                                }}
                            >
                                {status === "loading" ? "Saving…" : btnLabel}
                            </button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}
