package dev.suyeong.suo;

enum Stage {
    OFFER,
    PENDING,
    COUNTER,
    REVIEW,
    FINAL,
    DONE;

    boolean shows(MenuKind kind) {
        return switch (this) {
            case OFFER -> kind == MenuKind.OFFER;
            case COUNTER -> kind == MenuKind.COUNTER;
            case FINAL -> kind == MenuKind.FINAL;
            default -> false;
        };
    }
}
