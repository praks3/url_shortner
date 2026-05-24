package com.url_shortener.util;

public final class Base62 {

    private static final char[] ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int BASE = ALPHABET.length;

    private static final int[] INVERSE = new int[128];
    static {
        for (int i = 0; i < INVERSE.length; i++) {
            INVERSE[i] = -1;
        }
        for (int i = 0; i < ALPHABET.length; i++) {
            INVERSE[ALPHABET[i]] = i;
        }
    }

    private Base62() {
        // utility class
    }

    public static String encode(long n) {
        if (n < 0) {
            throw new IllegalArgumentException("Base62.encode requires a non-negative value, got " + n);
        }
        if (n == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            sb.append(ALPHABET[(int) (n % BASE)]);
            n /= BASE;
        }
        return sb.reverse().toString();
    }

    public static long decode(String s) {
        if (s == null || s.isEmpty()) {
            throw new IllegalArgumentException("Base62.decode requires a non-empty string");
        }
        long n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int v = (c < INVERSE.length) ? INVERSE[c] : -1;
            if (v < 0) {
                throw new IllegalArgumentException("Invalid Base62 character: '" + c + "'");
            }
            n = n * BASE + v;
        }
        return n;
    }
}
