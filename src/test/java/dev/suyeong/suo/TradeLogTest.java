package dev.suyeong.suo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TradeLogTest {
    @Test
    void logLineRoundTrips() {
        TradeRecord entry = new TradeRecord(
                "2026-11-21 11:42:05",
                "승인",
                "Steve",
                "Alex",
                "diamond x64",
                "emerald x32",
                ""
        );
        assertEquals(entry, TradeRecord.decode(entry.encode()));
    }

    @Test
    void commentsAndBrokenLinesAreSkipped() {
        assertNull(TradeRecord.decode("# header"));
        assertNull(TradeRecord.decode(""));
        assertNull(TradeRecord.decode("too\tshort"));
    }
}
