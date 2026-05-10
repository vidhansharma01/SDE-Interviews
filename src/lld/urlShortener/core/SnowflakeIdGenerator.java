package lld.urlShortener.core;

/**
 * Snowflake ID Generator — globally unique, time-sortable 64-bit IDs.
 *
 * ── Bit Layout ────────────────────────────────────────────────────────
 * [1b: sign=0][41b: timestamp ms][10b: machineId][12b: sequence]
 *
 *  Sign     (1 bit)  : always 0 → IDs always positive longs
 *  Timestamp(41 bits): ms since custom epoch (2020-01-01) → ~69 years of uniqueness
 *  MachineId(10 bits): 0–1023 unique machines → no cross-server collisions
 *  Sequence (12 bits): 0–4095 IDs per millisecond per machine
 *
 * ── Properties ───────────────────────────────────────────────────────
 * - 4096 unique IDs per millisecond per machine (4M/sec per machine)
 * - Monotonically increasing → time-sortable → Base62 codes grow predictably
 * - Zero DB dependency — no auto-increment, no UUID collision risk
 *
 * ── Clock Backward Protection ────────────────────────────────────────
 * NTP can adjust the system clock backward. If detected:
 *  - Small drift (< 5s): spin-wait until clock catches up.
 *  - Large drift (≥ 5s): throw — indicates a serious infrastructure problem.
 *
 * ── Thread Safety ────────────────────────────────────────────────────
 * synchronized on nextId() — one ID generated at a time per instance.
 * Each server has exactly one instance → no cross-thread contention in practice.
 */
public final class SnowflakeIdGenerator {

    private static final long EPOCH             = 1577836800000L; // 2020-01-01 UTC
    private static final int  MACHINE_ID_BITS   = 10;
    private static final int  SEQUENCE_BITS     = 12;
    private static final long MAX_MACHINE_ID    = (1L << MACHINE_ID_BITS) - 1;  // 1023
    private static final long MAX_SEQUENCE      = (1L << SEQUENCE_BITS) - 1;    // 4095
    private static final int  MACHINE_ID_SHIFT  = SEQUENCE_BITS;                // 12
    private static final int  TIMESTAMP_SHIFT   = MACHINE_ID_BITS + SEQUENCE_BITS; // 22

    private final long machineId;
    private long lastTimestamp = -1L;
    private long sequence      = 0L;

    public SnowflakeIdGenerator(long machineId) {
        if (machineId < 0 || machineId > MAX_MACHINE_ID)
            throw new IllegalArgumentException("machineId out of range [0, " + MAX_MACHINE_ID + "]");
        this.machineId = machineId;
    }

    /** Generates the next unique 64-bit ID. Synchronized for thread safety. */
    public synchronized long nextId() {
        long now = currentMs();

        if (now < lastTimestamp) {
            long drift = lastTimestamp - now;
            if (drift > 5000) throw new IllegalStateException("Clock moved back " + drift + "ms");
            while (now < lastTimestamp) now = currentMs(); // spin-wait
        }

        if (now == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) now = waitNextMs(lastTimestamp); // sequence exhausted
        } else {
            sequence = 0;
        }

        lastTimestamp = now;
        return ((now - EPOCH) << TIMESTAMP_SHIFT) | (machineId << MACHINE_ID_SHIFT) | sequence;
    }

    /** Extract original creation timestamp from a Snowflake ID (for debugging). */
    public long extractTimestamp(long id) { return (id >> TIMESTAMP_SHIFT) + EPOCH; }

    private long currentMs() { return System.currentTimeMillis(); }

    private long waitNextMs(long lastTs) {
        long ts = currentMs();
        while (ts <= lastTs) ts = currentMs();
        return ts;
    }
}
