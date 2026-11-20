package dev.suyeong.suo;

public final class TradeTexts {
    public static final String TITLE_OFFER = "보낼 물건";
    public static final String TITLE_COUNTER = "받을 물건 및 보낼 물건";
    public static final String TITLE_FINAL = "거래 최종 확인";
    public static final String CONFIRM = "확인";
    public static final String CANCEL = "취소";
    public static final String CANCELLED = "!! 거래가 취소되었습니다 !!";
    public static final String APPROVED = "!! 거래가 승인되었습니다 !!";
    public static final String ACCEPT = "[ 수락 ]";
    public static final String DENY = "[ 거절 ]";
    public static final String REVIEW = "[ 확인 ]";

    private TradeTexts() {
    }

    public static String requestLine(String requesterName) {
        return requesterName + " 님이 거래를 요청했습니다.";
    }

    public static String reviewLine(String partnerName) {
        return partnerName + " 님과의 거래 내역을 확인해주세요.";
    }
}
