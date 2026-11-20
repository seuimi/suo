package dev.suyeong.suo;

/**
 * Chest slots are counted left to right, top to bottom.
 * Each button row follows the requested 1-based positions:
 * 1, 2, 3 gray / 4 confirm / 5 gray / 6 cancel / 7, 8, 9 gray.
 */
public final class GuiLayout {
    public static final int OFFER_SIZE = 18;
    public static final int TRADE_SIZE = 36;

    public static final int OFFER_INPUT = 0;
    public static final int COUNTER_INPUT = 18;
    public static final int SEPARATOR_ROW = 9;
    public static final int OFFER_BUTTON_ROW = 9;
    public static final int TRADE_BUTTON_ROW = 27;

    public static final int CONFIRM_OFFSET = 3;
    public static final int CANCEL_OFFSET = 5;

    private GuiLayout() {
    }

    public static int buttonRow(MenuKind kind) {
        return kind == MenuKind.OFFER ? OFFER_BUTTON_ROW : TRADE_BUTTON_ROW;
    }

    public static int confirmSlot(MenuKind kind) {
        return buttonRow(kind) + CONFIRM_OFFSET;
    }

    public static int cancelSlot(MenuKind kind) {
        return buttonRow(kind) + CANCEL_OFFSET;
    }

    public static int inputStart(MenuKind kind) {
        return switch (kind) {
            case OFFER -> OFFER_INPUT;
            case COUNTER -> COUNTER_INPUT;
            case FINAL -> -1;
        };
    }

    public static boolean isInput(MenuKind kind, int rawSlot) {
        int start = inputStart(kind);
        return start >= 0 && rawSlot >= start && rawSlot < start + 9;
    }

    public static boolean isFillerOffset(int offset) {
        return offset >= 0 && offset < 9 && offset != CONFIRM_OFFSET && offset != CANCEL_OFFSET;
    }
}
