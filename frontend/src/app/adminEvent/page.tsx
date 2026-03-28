"use client";
import ReservationModal from "@/app/reservation/reservation";
import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { cancelEvent, getEveryEvents, EventDto } from "@/lib/axios";
import EventFormModal from "@/app/adminEvent/eventAddModify";

const formatDate = (iso: string) => {
    const d = new Date(iso);
    return {
        day:   d.toLocaleDateString("en-US", { day: "2-digit" }),
        month: d.toLocaleDateString("en-US", { month: "short" }).toUpperCase(),
        full:  d.toLocaleDateString("en-US", { weekday: "short", month: "long", day: "numeric" }),
        time:  d.toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" }),
    };
};

const CATEGORY_COLORS: Record<string, { dot: string; text: string; bg: string; border: string }> = {
    "Concert": { dot: "bg-violet-400", text: "text-violet-600", bg: "bg-violet-50", border: "border-violet-200" },
    "Movie": { dot: "bg-rose-400",   text: "text-rose-600",   bg: "bg-rose-50",   border: "border-rose-200"   },
    "Sports": { dot: "bg-green-400",  text: "text-green-600",  bg: "bg-green-50",  border: "border-green-200"  },
    "Theatre": { dot: "bg-amber-400",  text: "text-amber-600",  bg: "bg-amber-50",  border: "border-amber-200"  },
    "Festival": { dot: "bg-orange-400", text: "text-orange-600", bg: "bg-orange-50", border: "border-orange-200" },
};

const fakeEvent: EventDto = {
    id: 42,
    title: "Jazz Night at Bell Centre",
    description: "An unforgettable evening of live jazz with world-class musicians.",
    eventDate: "2025-08-15T20:00:00",
    categoryName: "Concert",
    locationName: "Bell Centre",
    city: "Montreal",
    totalTickets: 120,
    status: "ACTIVE",
};
const AdminEventsPage = () => {
    const router = useRouter();
    const [modal, setModal] = useState<{ mode: "add" | "modify"; event: EventDto | null } | null>(null);
    const [allEvents, setAllEvents]           = useState<EventDto[]>([]);
    const [displayEvents, setDisplayEvents]   = useState<EventDto[]>([]);
    const [categories, setCategories]         = useState<string[]>(["All"]);
    const [activeCategory, setActiveCategory] = useState("All");
    const [loading, setLoading]               = useState(false);
    const [error, setError]                   = useState<string | null>(null);

    const loadAll = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const events = await getEveryEvents() as EventDto[];
            setAllEvents(events);
            setDisplayEvents(events);
            const cats = ["All", ...Array.from(new Set(events.map((e) => e.categoryName).filter((c): c is string => Boolean(c))))];
            setCategories(cats);
        } catch (err: any) {
            setError(err?.toString() ?? "Failed to load events.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        if (!sessionStorage.getItem("token")) {
            router.push("/signin");
            return;
        }
        if (!sessionStorage.getItem("isAdmin") ) {
            router.push("/event");
            return;
        }
        loadAll();
    }, []);

    const handleCategoryClick = (cat: string) => {
        setActiveCategory(cat);
        setDisplayEvents(
            cat === "All"
                ? allEvents
                : allEvents.filter((e) => e.categoryName === cat)
        );
    };
    const handleCancel = async (eventId: number) => {
        try {
            await cancelEvent(eventId);
            await loadAll();
        } catch (err: any) {
            setError(err?.toString() ?? "Failed to cancel event.");
        }
    };

    const inputClass =
        "bg-white border border-stone-200 rounded-lg px-3 py-2 text-sm text-stone-800 placeholder-stone-300 outline-none focus:border-stone-400 transition-colors";

    return (
        <div className="min-h-screen bg-stone-50 pb-20">
            <div className="max-w-5xl mx-auto px-6">

                <header className="py-14 border-b border-stone-200 mb-10">
                    <div className="flex items-center justify-between">
                        <div className="flex-1 text-center">
                            <h1 className="text-4xl font-bold text-stone-900 tracking-tight ">All Events</h1>
                            <p className="mt-2 text-sm text-stone-400 font-light">
                                {loading
                                    ? "Loading..."
                                    : `${displayEvents.length} event${displayEvents.length !== 1 ? "s" : ""} found`}
                            </p>
                        </div>
                        <div style={{ display: "flex", gap: 8 }}>
                            <button
                                onClick={() => setModal({ mode: "add", event: null })}
                                style={{ backgroundColor: "#059669", color: "#fff", padding: "8px 16px", fontSize: 14, fontWeight: 500, borderRadius: 8, border: "none", cursor: "pointer" }}
                            >
                                + Add Event
                            </button>
                            <button
                                onClick={() => { sessionStorage.clear(); router.push("/signin"); }}
                                style={{ backgroundColor: "#b91c1c", color: "#fff", padding: "8px 16px", fontSize: 14, fontWeight: 500, borderRadius: 8, border: "none", cursor: "pointer" }}
                            >
                                Logout
                            </button>
                        </div>
                    </div>
                </header>

                {error && (
                    <div className="mb-6 px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
                        {error}
                    </div>
                )}

                <div className="flex flex-wrap gap-2 mb-10 justify-center">
                    {categories.map((cat) => (
                        <button
                            key={cat}
                            onClick={() => handleCategoryClick(cat)}
                            className={`px-4 py-1.5 rounded-full text-xs font-medium border transition-all ${
                                activeCategory === cat
                                    ? "bg-stone-900 text-white border-stone-900"
                                    : "bg-white text-stone-500 border-stone-200 hover:border-stone-400 hover:text-stone-800"
                            }`}
                        >
                            {cat}
                        </button>
                    ))}
                </div>

                {loading ? (
                    <div className="text-center py-24 text-stone-300">
                        <p className="text-sm font-light">Loading events...</p>
                    </div>
                ) : displayEvents.length === 0 ? (
                    <div className="text-center py-24 text-stone-300">
                        <p className="text-sm font-light">No events found.</p>
                    </div>
                ) : (
                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                        {displayEvents.map((ev) => {
                            const dt = formatDate(ev.eventDate);
                            const cat = CATEGORY_COLORS[ev.categoryName] ?? { dot: "bg-stone-400", text: "text-stone-600", bg: "bg-stone-50", border: "border-stone-200" };
                            return (
                                <div
                                    key={ev.id}
                                    className="bg-white border border-stone-200 rounded-2xl p-6 flex flex-col hover:shadow-md hover:-translate-y-0.5 hover:border-stone-300 transition-all duration-200 group cursor-pointer"
                                >
                                    <div className="flex items-start justify-between mb-5">
                                        <div className="flex flex-col items-center bg-stone-50 border border-stone-200 rounded-lg px-3 py-2 leading-none gap-0.5">
                                            <span className="text-xl font-bold text-stone-900">{dt.day}</span>
                                            <span className="text-[9px] tracking-widest text-stone-400 uppercase font-medium">{dt.month}</span>
                                        </div>
                                        <span className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border ${cat.bg} ${cat.text} ${cat.border}`}>
                                            <span className={`w-1.5 h-1.5 rounded-full ${cat.dot}`} />
                                            {ev.categoryName}
                                        </span>
                                    </div>

                                    <h2 className="text-lg font-semibold text-stone-900 mb-1 leading-snug">{ev.title}</h2>

                                    {ev.description && (
                                        <p className="text-xs text-stone-400 font-light mb-3 line-clamp-2">{ev.description}</p>
                                    )}

                                    <p className="text-stone-400 text-xs font-light">
                                        {[ev.locationName, ev.city].filter(Boolean).join(", ")}
                                    </p>

                                    {ev.totalTickets != null && (
                                        <p className="text-stone-400 text-xs font-light mt-1">
                                            {ev.totalTickets} tickets available
                                        </p>
                                    )}
                                    <p className="text-stone-400 text-xs font-light mt-1">
                                        Status: {ev.status}
                                    </p>

                                    <div className="mt-5 pt-4 border-t border-stone-100 flex flex-col gap-2">
                                        <span className="text-xs text-stone-300">{dt.full} · {dt.time}</span>
                                        <div className="flex items-center gap-2">
                                            <button
                                                onClick={(e) => { e.stopPropagation(); setModal({ mode: "modify", event: ev }); }}
                                                className="flex-1 px-3 py-1.5 bg-blue-50 text-blue-600 text-xs font-medium rounded-lg hover:bg-blue-100 active:scale-95 transition-all"
                                            >
                                                Modify
                                            </button>
                                            <button
                                                onClick={(e) => { e.stopPropagation(); handleCancel(ev.id); }}
                                                className="flex-1 px-3 py-1.5 bg-red-50 text-red-600 text-xs font-medium rounded-lg hover:bg-red-100 active:scale-95 transition-all"
                                            >
                                                Cancel
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}

            </div>
            {modal && (
                <EventFormModal
                    event={modal.event}
                    mode={modal.mode}
                    onClose={() => {setModal(null); loadAll();}}
                />
            )}
        </div>
    );
};

export default AdminEventsPage;