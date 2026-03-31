export const formatDate = (iso: string) => {
    const d = new Date(iso);
    return {
        day:   d.toLocaleDateString("en-US", { day: "2-digit" }),
        month: d.toLocaleDateString("en-US", { month: "short" }).toUpperCase(),
        full:  d.toLocaleDateString("en-US", { weekday: "short", month: "long", day: "numeric" }),
        time:  d.toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" }),
    };
};

export const toDatetimeLocal = (iso: string) => {
    if (!iso) return "";
    return new Date(iso).toISOString().slice(0, 16);
};

export const toJavaDateTime = (date: string): string =>
    new Date(date).toISOString().slice(0, 19);


export const formatDateReservation = (iso: string) => {
    const d = new Date(iso);
    return {
        full: d.toLocaleDateString("en-US", { weekday: "long", year: "numeric", month: "long", day: "numeric" }),
        time: d.toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" }),
    };
};

export const isPhone = (value: string) => /^[\d\s\+\-\(\)]{7,}$/.test(value.trim());