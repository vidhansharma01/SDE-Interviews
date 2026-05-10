package lld.urlShortener.core;

/**
 * Base62 Encoder — converts numeric Snowflake IDs to short, URL-safe strings.
 *
 * ── Why Base62? ───────────────────────────────────────────────────────
 * Characters: 0-9 (10) + a-z (26) + A-Z (26) = 62 total
 * - URL-safe: no +, /, = characters that need percent-encoding.
 * - Human-readable: distinguishable characters (unlike Base58 which removes 0, O, l, 1).
 * - Compact: 7 chars = 62^7 ≈ 3.5 TRILLION unique codes.
 *   At 100M URLs/day → codes last 96 years.
 *
 * ── Algorithm ────────────────────────────────────────────────────────
 * Encoding = converting a decimal number to base 62 (exactly like base conversion):
 *   ID 1000000000 → divide by 62 repeatedly, collect remainders in reverse:
 *   1000000000 % 62 = 8  → '8'
 *   16129032   % 62 = 8  → '8'
 *   ...
 *   Left-pad result to CODE_LENGTH=7 with '0' for consistent length.
 *
 * Decoding = reverse: for each character, multiply accumulator by 62 and add index.
 *
 * ── Singleton Pattern ─────────────────────────────────────────────────
 * Stateless — pure functions. No reason to instantiate multiple times.
 * Safe to share across all threads.
 */
public final class Base62Encoder {

    private static final String ALPHABET  = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int    BASE      = 62;
    public  static final int    CODE_LEN  = 7;  // 62^7 ≈ 3.5 trillion unique codes

    // O(1) decode lookup: char → index. Avoids String.indexOf() per character.
    private static final int[] DECODE_MAP = new int[128];

    static {
        java.util.Arrays.fill(DECODE_MAP, -1);
        for (int i = 0; i < ALPHABET.length(); i++) {
            DECODE_MAP[ALPHABET.charAt(i)] = i;
        }
    }

    private static final Base62Encoder INSTANCE = new Base62Encoder();
    private Base62Encoder() {}
    public static Base62Encoder getInstance() { return INSTANCE; }

    /**
     * Encodes a positive long to a 7-character Base62 string.
     * Left-pads with '0' for consistent length.
     *
     * @throws IllegalArgumentException if id <= 0
     */
    public String encode(long id) {
        if (id <= 0) throw new IllegalArgumentException("ID must be > 0, got: " + id);
        char[] buf = new char[CODE_LEN];
        int pos = CODE_LEN - 1;
        while (id > 0 && pos >= 0) {
            buf[pos--] = ALPHABET.charAt((int)(id % BASE));
            id /= BASE;
        }
        while (pos >= 0) buf[pos--] = '0'; // left-pad with '0'
        return new String(buf);
    }

    /**
     * Decodes a Base62 string back to the original long.
     *
     * @throws IllegalArgumentException if the string contains invalid characters
     */
    public long decode(String code) {
        if (code == null || code.isEmpty())
            throw new IllegalArgumentException("code must not be null or empty");
        long result = 0;
        for (char c : code.toCharArray()) {
            if (c >= 128 || DECODE_MAP[c] == -1)
                throw new IllegalArgumentException("Invalid Base62 character: '" + c + "'");
            result = result * BASE + DECODE_MAP[c];
        }
        return result;
    }
}
