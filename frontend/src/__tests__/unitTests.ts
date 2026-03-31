import {formatDate,toDatetimeLocal,toJavaDateTime,formatDateReservation,isPhone} from '@/app/utils';

describe('formatDate', () => {

    it('return valid valuew upon inputting valid date', () => {
        const iso = "2024-06-15T12:00:00.000Z";
        const result = formatDate(iso);
        expect(result.day).toMatch(/^\d{2}$/);
        expect(result.month).toBe(result.month.toUpperCase());
        expect(result.month).toContain("JUN");
        expect(result.full).toContain("June");
        expect(result.full).toContain("15");
        expect(result.time).toMatch(/\d{2}:\d{2}/);
    });

    it('return invalid value upon inputting invalid date', () => {
        const iso = "12024-06-1115dswedwT12:00:00.000Z";
        const result = formatDate(iso);
        expect(result.day).toBe("Invalid Date");
        expect(result.month).toBe("INVALID DATE");
        expect(result.full).toBe("Invalid Date");
        expect(result.time).toBe("Invalid Date");
    });

    it('return invalid value upon inputting empty date', () => {
        const iso = "";
        const result = formatDate(iso);
        expect(result.day).toBe("Invalid Date");
        expect(result.month).toBe("INVALID DATE");
        expect(result.full).toBe("Invalid Date");
        expect(result.time).toBe("Invalid Date");
    });
});

describe('toDatetimeLocal', () => {
    it('returns correct output', () => {
        const result = toDatetimeLocal("2024-06-15T12:30:00.000Z");
        expect(result).toBe("2024-06-15T12:30");
    });
    it('throws error for invalid input', () => {
        expect(() => toDatetimeLocal("nv-drrgf-reg")).toThrow(RangeError);
    });
    it('returns empty string upon entering empty input', () => {
        expect(toDatetimeLocal("")).toBe("");
    });
});

describe('toJavaDateTime', () => {
    it('returns correct value for valid input', () => {
        expect(toJavaDateTime("2024-06-15T12:30:00.000Z")).toBe("2024-06-15T12:30:00");
    });

    it('throws upon invalid input', () => {
        expect(() => toJavaDateTime("nv-drrgf-reg")).toThrow(RangeError);
    });

    it('throws upon empty string', () => {
        expect(() => toJavaDateTime("")).toThrow(RangeError);
    });
});


describe('formatDateReservation', () => {
    it('returns correct values for valid input', () => {
        const result = formatDateReservation("2024-06-15T12:00:00.000Z");
        expect(result.full).toContain("June");
        expect(result.full).toContain("15");
        expect(result.full).toContain("2024");
        expect(result.time).toMatch(/\d{2}:\d{2}/);
    });

    it('returns Invalid value for invalid input', () => {
        const result = formatDateReservation("nv-drrgf-reg");
        expect(result.full).toBe("Invalid Date");
        expect(result.time).toBe("Invalid Date");
    });

    it('returns Invalid value for empty string', () => {
        const result = formatDateReservation("");
        expect(result.full).toBe("Invalid Date");
        expect(result.time).toBe("Invalid Date");
    });
});

describe('isPhone', () => {
    it('return true when valid value is inputted', () => {
        const result = isPhone("2024-06-15T14:30:00.000Z");
        expect(result).toBe(false);
    });
    it('return false when wrong value is inputted', () => {
        const result = isPhone("2024-06-15T14:30:00.000Z");
        expect(result).toBe(false);
    });
    it('return false when empty  value is inputted', () => {
        const result = isPhone("");
        expect(result).toBe(false);
    });
});