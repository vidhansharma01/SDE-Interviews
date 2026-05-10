package lld.urlShortener.strategy;

import lld.urlShortener.core.Base62Encoder;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Concrete Strategy: Random alphanumeric short code.
 *
 * Generates a random CODE_LEN-character string from the Base62 alphabet.
 * Simpler than Snowflake but requires a uniqueness-check loop:
 *   "generate → check DB → if collision → regenerate"
 *
 * ── When to use this strategy ────────────────────────────────────────
 * - Small-scale systems where collision probability is negligible.
 * - When you want codes that are NOT time-sortable (privacy: codes don't
 *   reveal creation order — competitors can't enumerate your URL count).
 *
 * ── Collision probability ─────────────────────────────────────────────
 * Birthday Problem: With N existing codes and M=62^7 ≈ 3.5 trillion:
 *   P(collision) ≈ N²/(2M)
 *   At N=1 billion: P ≈ 0.014% per generation → still requires collision retry.
 *
 * ── Trade-off vs Base62Snowflake ─────────────────────────────────────
 * Base62Snowflake: zero collisions, DB write without pre-check, time-sortable.
 * RandomStrategy:  non-sequential (privacy benefit), requires collision check.
 *
 * This class demonstrates the Strategy Pattern — the service simply calls
 * generateCode() and doesn't know it's random vs Snowflake-based.
 */
public class RandomCodeStrategy implements EncodingStrategy {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int    CODE_LEN = Base62Encoder.CODE_LEN; // same 7-char length

    @Override
    public String generateCode(String longUrl, String userId) {
        StringBuilder sb = new StringBuilder(CODE_LEN);
        for (int i = 0; i < CODE_LEN; i++) {
            int idx = ThreadLocalRandom.current().nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(idx));
        }
        String code = sb.toString();
        System.out.printf("[RandomStrategy] Generated code '%s'%n", code);
        return code;
    }

    @Override
    public String strategyName() { return "RANDOM_ALPHANUMERIC"; }
}
