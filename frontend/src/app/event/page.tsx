"use client";
import ReservationModal from "@/app/reservation/reservation";
import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { getAllEvents, searchEvents, filterEvents, EventDto, EventFilterParams } from "@/lib/axios";

const toJavaDateTime = (date: string): string =>
    new Date(date).toISOString().slice(0, 19);

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

const EventsPage = () => {
    const router = useRouter();

    const [allEvents, setAllEvents]           = useState<EventDto[]>([]);
    const [displayEvents, setDisplayEvents]   = useState<EventDto[]>([]);
    const [categories, setCategories]         = useState<string[]>(["All"]);
    const [keyword, setKeyword]               = useState("");
    const [startDate, setStartDate]           = useState("");
    const [endDate, setEndDate]               = useState("");
    const [categoryId, setCategoryId]         = useState<number | undefined>(undefined);
    const [locationId, setLocationId]         = useState<number | undefined>(undefined);
    const [activeCategory, setActiveCategory] = useState("All");
    const [loading, setLoading]               = useState(false);
    const [error, setError]                   = useState<string | null>(null);
    const [selectedEvent, setSelectedEvent] = useState<EventDto | null>(null);
    const handleReserve = async (eventId: number, ticketCount: number) => {
       // await createReservation({ eventId, ticketCount }); // replace with your real axios call
    };
    const loadAll = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const events = await getAllEvents();
            setAllEvents(events);
            setDisplayEvents(events);
            const cats = ["All", ...Array.from(new Set(events.map((e) => e.categoryName).filter(Boolean)))];
            setCategories(cats);
        } catch (err: any) {
            setError(err?.toString() ?? "Failed to load events.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        if (!sessionStorage.getItem("token") ) {
            router.push("/signin");
            return;
        }
        if (!sessionStorage.getItem("isAdmin") ) {
            router.push("/adminEvent");
            return;
        }


        loadAll();
    }, []);

    const handleSearch = async () => {
        if (!keyword.trim()) {
            setDisplayEvents(
                activeCategory === "All"
                    ? allEvents
                    : allEvents.filter((e) => e.categoryName === activeCategory)
            );
            return;
        }
        setLoading(true);
        setError(null);
        try {
            const events = await searchEvents(keyword.trim());
            setDisplayEvents(
                activeCategory === "All"
                    ? events
                    : events.filter((e) => e.categoryName === activeCategory)
            );
        } catch (err: any) {
            setError(err?.toString() ?? "Search failed.");
        } finally {
            setLoading(false);
        }
    };

    const handleFilter = async () => {
        setLoading(true);
        setError(null);
        try {
            const params: EventFilterParams = {
                ...(categoryId && { categoryId }),
                ...(locationId && { locationId }),
                ...(startDate  && { startDate: toJavaDateTime(startDate) }),
                ...(endDate    && { endDate:   toJavaDateTime(endDate)   }),
            };
            const events = await filterEvents(params);
            setDisplayEvents(events);
        } catch (err: any) {
            setError(err?.toString() ?? "Filter failed.");
        } finally {
            setLoading(false);
        }
    };

    const handleCategoryClick = (cat: string) => {
        setActiveCategory(cat);
        setDisplayEvents(
            cat === "All"
                ? allEvents
                : allEvents.filter((e) => e.categoryName === cat)
        );
    };

    const handleReset = () => {
        setKeyword("");
        setStartDate("");
        setEndDate("");
        setCategoryId(undefined);
        setLocationId(undefined);
        setActiveCategory("All");
        setDisplayEvents(allEvents);
    };

    const inputClass =
        "bg-white border border-stone-200 rounded-lg px-3 py-2 text-sm text-stone-800 placeholder-stone-300 outline-none focus:border-stone-400 transition-colors";

    return (
        <div className="min-h-screen bg-stone-50 pb-20">
            <div className="max-w-5xl mx-auto px-6">

                <header className="py-14 border-b border-stone-200 mb-10">
                    <div className="flex items-center justify-between">
                        <div className="flex-1 text-center">
                            <h1 className="text-4xl font-bold text-stone-900 tracking-tight ">Available Events</h1>
                            <p className="mt-2 text-sm text-stone-400 font-light">
                                {loading
                                    ? "Loading..."
                                    : `${displayEvents.length} event${displayEvents.length !== 1 ? "s" : ""} found`}
                            </p>
                        </div>
                        <div className="flex items-center gap-2">
                        <button
                            onClick={() => { router.push("/reservation"); }}
                            className="px-4 py-2 bg-blue-700 text-white text-sm font-medium rounded-lg hover:bg-blue-800 active:scale-95 transition-all"
                        >
                            Reservation
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

                {error && (
                    <div className="mb-6 px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
                        {error}
                    </div>
                )}

                <div className="flex gap-2 mb-3 max-w-lg mx-auto">
                    <input
                        className={`${inputClass} w-full`}
                        type="text"
                        placeholder="Search by keyword..."
                        value={keyword}
                        onChange={(e) => setKeyword(e.target.value)}
                        onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                    />
                    <button
                        onClick={handleSearch}
                        disabled={loading}
                        className="bg-stone-900 text-white text-sm font-medium px-5 py-2 rounded-lg hover:bg-stone-700 active:scale-95 transition-all disabled:opacity-100"
                    >
                        Search
                    </button>
                </div>

                <div className="flex flex-wrap gap-2 mb-5 justify-center">
                    <select
                        className={`${inputClass} w-40`}
                        value={categoryId ?? ""}
                        onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : undefined)}
                    >
                        <option value="">All Categories</option>
                        {CATEGORY_OPTIONS.map((c) => (
                            <option key={c.id} value={c.id}>{c.name}</option>
                        ))}
                    </select>
                    <select
                        className={`${inputClass} w-44`}
                        value={locationId ?? ""}
                        onChange={(e) => setLocationId(e.target.value ? Number(e.target.value) : undefined)}
                    >
                        <option value="">All Locations</option>
                        {LOCATION_OPTIONS.map((l) => (
                            <option key={l.id} value={l.id}>{l.name}</option>
                        ))}
                    </select>
                    <input
                        className={`${inputClass} w-40`}
                        type="date"
                        title="Start date"
                        value={startDate}
                        onChange={(e) => setStartDate(e.target.value)}
                    />
                    <input
                        className={`${inputClass} w-40`}
                        type="date"
                        title="End date"
                        value={endDate}
                        onChange={(e) => setEndDate(e.target.value)}
                    />
                    <button
                        onClick={handleFilter}
                        disabled={loading}
                        className="bg-stone-700 text-white text-sm font-medium px-5 py-2 rounded-lg hover:bg-stone-600 active:scale-95 transition-all disabled:opacity-100">
                        Filter
                    </button>
                    <button
                        onClick={handleReset}
                        className="text-red-500 text-sm px-3 py-2 rounded-lg hover:text-red-700 transition-colors"
                    >
                        Reset
                    </button>
                </div>

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
                        <p className="text-sm font-light">No events found. Try adjusting your filters.</p>
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
                                        Status : {ev.status}
                                    </p>

                                    <div className="mt-5 pt-4 border-t border-stone-100 flex items-center justify-between">
                                        <span className="text-xs text-stone-300">{dt.full} · {dt.time}</span>
                                    <div className="text-stone-900"> Reserve</div>
                                        <button
                                            onClick={(e) => { e.stopPropagation(); setSelectedEvent(ev); }}
                                            className="w-6 h-6 rounded-full bg-stone-100 flex items-center justify-center text-xs text-stone-400 group-hover:bg-stone-900 group-hover:text-white transition-all"
                                            aria-label="Reserve tickets"
                                        >
                                            →
                                        </button>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}

            </div>
            <ReservationModal
                event={selectedEvent}
                onClose={() => setSelectedEvent(null)}
            />
        </div>
    );
};

export default EventsPage;