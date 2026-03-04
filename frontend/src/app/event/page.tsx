"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
interface Event {
    id: string;
    name: string;
    date: string;
    location: string;
    category: string;
    isActive: boolean;
}

const categoryConfig: Record<string, { emoji: string; textColor: string; bgColor: string }> = {
    Music:      { emoji: "🎵", textColor: "text-amber-700",  bgColor: "bg-amber-50"  },
    Food:       { emoji: "🍽️", textColor: "text-red-700",    bgColor: "bg-red-50"    },
    Art:        { emoji: "🎨", textColor: "text-violet-700", bgColor: "bg-violet-50" },
    Technology: { emoji: "⚡", textColor: "text-sky-700",    bgColor: "bg-sky-50"    },
};
const fallback = { emoji: "📅", textColor: "text-slate-600", bgColor: "bg-slate-50" };

const formatDate = (iso: string) => {
    const d = new Date(iso);
    return {
        day:   d.toLocaleDateString("en-US", { day: "2-digit" }),
        month: d.toLocaleDateString("en-US", { month: "short" }).toUpperCase(),
        full:  d.toLocaleDateString("en-US", { weekday: "short", month: "long", day: "numeric" }),
    };
};

const EventsPage = () => {
    const router = useRouter();

    useEffect(() => {
        if (sessionStorage.getItem("token") === null) {
            router.push("/signin");
        }
    }, []);

    const allEvents: Event[] = [
        { id: "1", name: "Rock Concert",    date: "2026-03-20", location: "Montreal",  category: "Music",      isActive: true  },
        { id: "2", name: "Food Festival",   date: "2026-03-25", location: "Toronto",   category: "Food",       isActive: true  },
        { id: "3", name: "Art Expo",        date: "2026-04-01", location: "Montreal",  category: "Art",        isActive: false },
        { id: "4", name: "Tech Conference", date: "2026-04-10", location: "Vancouver", category: "Technology", isActive: true  },
    ];

    const [events, setEvents]                 = useState<Event[]>([]);
    const [search, setSearch]                 = useState("");
    const [filters, setFilters]               = useState({ date: "", location: "", category: "" });
    const [filteredEvents, setFilteredEvents] = useState<Event[]>([]);
    const [activeCategory, setActiveCategory] = useState("All");

    const categories = ["All", ...Array.from(new Set(allEvents.map((e) => e.category)))];

    useEffect(() => {
        const active = allEvents.filter((e) => e.isActive);
        setEvents(active);
        setFilteredEvents(active);
    }, []);

    const runFilter = (overrideCat?: string) => {
        let temp = events;
        const cat = overrideCat ?? activeCategory;

        if (search) {
            const kw = search.toLowerCase();
            temp = temp.filter(
                (e) =>
                    e.name.toLowerCase().includes(kw) ||
                    e.location.toLowerCase().includes(kw) ||
                    e.category.toLowerCase().includes(kw)
            );
        }
        if (filters.date)     temp = temp.filter((e) => e.date === filters.date);
        if (filters.location) temp = temp.filter((e) => e.location.toLowerCase().includes(filters.location.toLowerCase()));
        if (filters.category) temp = temp.filter((e) => e.category.toLowerCase().includes(filters.category.toLowerCase()));
        if (cat !== "All")    temp = temp.filter((e) => e.category === cat);

        setFilteredEvents(temp);
    };

    const handleCategoryClick = (cat: string) => {
        setActiveCategory(cat);
        runFilter(cat);
    };

    const inputClass =
        "bg-white border border-stone-200 rounded-lg px-3 py-2 text-sm text-stone-800 placeholder-stone-300 outline-none focus:border-stone-400 transition-colors";

    return (
        <div className="min-h-screen bg-stone-50 pb-20">
            <div className="max-w-5xl mx-auto px-6">

                <header className="py-14 border-b border-stone-200 mb-10">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-xs tracking-widest uppercase text-stone-400 font-medium mb-2">Discover &amp; Explore</p>
                            <h1 className="text-4xl font-bold text-stone-900 tracking-tight">Available Events</h1>
                            <p className="mt-2 text-sm text-stone-400 font-light">
                                {filteredEvents.length} event{filteredEvents.length !== 1 ? "s" : ""} found
                            </p>
                        </div>
                        <button
                            onClick={() => {
                                sessionStorage.clear();
                                router.push("/signin");
                            }}
                            className="px-4 py-2 bg-red-700 text-white text-sm font-medium rounded-lg hover:bg-stone-700 active:scale-95 transition-all"
                        >
                            Logout
                        </button>
                    </div>
                </header>

                {/* Search & Filters */}
                <div className="flex flex-wrap gap-2 mb-5">
                    <input
                        className={`${inputClass} flex-1 min-w-48`}
                        type="text"
                        placeholder="Search by keyword…"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        onKeyDown={(e) => e.key === "Enter" && runFilter()}
                    />
                    <input
                        className={`${inputClass} w-36`}
                        type="date"
                        value={filters.date}
                        onChange={(e) => setFilters({ ...filters, date: e.target.value })}
                    />
                    <input
                        className={`${inputClass} w-32`}
                        type="text"
                        placeholder="Location"
                        value={filters.location}
                        onChange={(e) => setFilters({ ...filters, location: e.target.value })}
                    />
                    <input
                        className={`${inputClass} w-32`}
                        type="text"
                        placeholder="Category"
                        value={filters.category}
                        onChange={(e) => setFilters({ ...filters, category: e.target.value })}
                    />
                    <button
                        onClick={() => runFilter()}
                        className="bg-stone-900 text-white text-sm font-medium px-5 py-2 rounded-lg hover:bg-stone-700 active:scale-95 transition-all"
                    >
                        Search
                    </button>
                </div>

                {/* Category Pills */}
                <div className="flex flex-wrap gap-2 mb-10">
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
                            {cat !== "All" && categoryConfig[cat]?.emoji ? `${categoryConfig[cat].emoji} ` : ""}
                            {cat}
                        </button>
                    ))}
                </div>

                {/* Grid */}
                {filteredEvents.length === 0 ? (
                    <div className="text-center py-24 text-stone-300">
                        <p className="text-4xl mb-3">🔍</p>
                        <p className="text-sm font-light">No events found. Try adjusting your filters.</p>
                    </div>
                ) : (
                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                        {filteredEvents.map((ev) => {
                            const cfg = categoryConfig[ev.category] ?? fallback;
                            const dt  = formatDate(ev.date);
                            return (
                                <div
                                    key={ev.id}
                                    className="bg-white border border-stone-200 rounded-2xl p-6 flex flex-col hover:shadow-md hover:-translate-y-0.5 hover:border-stone-300 transition-all duration-200 group"
                                >
                                    {/* Top row */}
                                    <div className="flex items-start justify-between mb-5">
                                        {/* Date badge */}
                                        <div className="flex flex-col items-center bg-stone-50 border border-stone-200 rounded-lg px-3 py-2 leading-none gap-0.5">
                                            <span className="text-xl font-bold text-stone-900">{dt.day}</span>
                                            <span className="text-[9px] tracking-widest text-stone-400 uppercase font-medium">{dt.month}</span>
                                        </div>
                                        {/* Category badge */}
                                        <span className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${cfg.bgColor} ${cfg.textColor}`}>
                                            {cfg.emoji} {ev.category}
                                        </span>
                                    </div>

                                    {/* Name */}
                                    <h2 className="text-lg font-semibold text-stone-900 mb-3 leading-snug">{ev.name}</h2>

                                    {/* Location */}
                                    <div className="flex items-center gap-2 text-stone-400 text-xs font-light">
                                        <span>📍</span>
                                        <span>{ev.location}</span>
                                    </div>

                                    {/* Footer */}
                                    <div className="mt-5 pt-4 border-t border-stone-100 flex items-center justify-between">
                                        <span className="text-xs text-stone-300">{dt.full}</span>
                                        <div className="w-6 h-6 rounded-full bg-stone-100 flex items-center justify-center text-xs text-stone-400 group-hover:bg-stone-900 group-hover:text-white transition-all">
                                            →
                                        </div>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}

            </div>
        </div>
    );
};

export default EventsPage;
