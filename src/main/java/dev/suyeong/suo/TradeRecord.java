package dev.suyeong.suo;

public record TradeRecord(
        String time,
        String result,
        String requester,
        String partner,
        String requesterItems,
        String partnerItems,
        String reason
) {
    public String encode() {
        return String.join("\t",
                safe(time),
                safe(result),
                safe(requester),
                safe(partner),
                safe(requesterItems),
                safe(partnerItems),
                safe(reason)
        );
    }

    public static TradeRecord decode(String line) {
        if (line == null || line.isBlank() || line.startsWith("#")) {
            return null;
        }
        String[] parts = line.split("\t", -1);
        if (parts.length < 7) {
            return null;
        }
        return new TradeRecord(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]);
    }

    private static String safe(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }
}
