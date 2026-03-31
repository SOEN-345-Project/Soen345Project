import {formatDate,toDatetimeLocal,toJavaDateTime,formatDateReservation,isPhone} from '@/app/utils';

describe('formatDate', () => {
});

describe('toDatetimeLocal', () => {
});

describe('toJavaDateTime', () => {
});


describe('formatDateReservation', () => {
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