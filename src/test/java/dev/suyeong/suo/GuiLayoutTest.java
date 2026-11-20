package dev.suyeong.suo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiLayoutTest {
    @Test
    void offerMenuIsTwoRowsAndCounterMenuIsFourRows() {
        assertEquals(18, GuiLayout.OFFER_SIZE);
        assertEquals(36, GuiLayout.TRADE_SIZE);
    }

    @Test
    void buttonRowMatchesRequestedPositions() {
        assertEquals("GGGCGXGGG", row(GuiLayout.OFFER_BUTTON_ROW, MenuKind.OFFER));
        assertEquals("GGGCGXGGG", row(GuiLayout.TRADE_BUTTON_ROW, MenuKind.COUNTER));
        assertEquals("GGGCGXGGG", row(GuiLayout.TRADE_BUTTON_ROW, MenuKind.FINAL));
        assertEquals(12, GuiLayout.confirmSlot(MenuKind.OFFER));
        assertEquals(14, GuiLayout.cancelSlot(MenuKind.OFFER));
        assertEquals(30, GuiLayout.confirmSlot(MenuKind.COUNTER));
        assertEquals(32, GuiLayout.cancelSlot(MenuKind.FINAL));
    }

    @Test
    void onlyTheEditingRowAcceptsItems() {
        for (int slot = 0; slot < GuiLayout.OFFER_SIZE; slot++) {
            assertEquals(slot < 9, GuiLayout.isInput(MenuKind.OFFER, slot));
        }
        for (int slot = 0; slot < GuiLayout.TRADE_SIZE; slot++) {
            assertEquals(slot >= 18 && slot < 27, GuiLayout.isInput(MenuKind.COUNTER, slot));
            assertFalse(GuiLayout.isInput(MenuKind.FINAL, slot));
        }
    }

    @Test
    void messagesUseTheRequestedWording() {
        assertEquals("보낼 물건", TradeTexts.TITLE_OFFER);
        assertEquals("받을 물건 및 보낼 물건", TradeTexts.TITLE_COUNTER);
        assertEquals("거래 최종 확인", TradeTexts.TITLE_FINAL);
        assertEquals("확인", TradeTexts.CONFIRM);
        assertEquals("취소", TradeTexts.CANCEL);
        assertEquals("!! 거래가 취소되었습니다 !!", TradeTexts.CANCELLED);
        assertEquals("!! 거래가 승인되었습니다 !!", TradeTexts.APPROVED);
        assertEquals("[ 수락 ]", TradeTexts.ACCEPT);
        assertEquals("[ 거절 ]", TradeTexts.DENY);
        assertEquals("[ 확인 ]", TradeTexts.REVIEW);
        assertEquals("Steve 님이 거래를 요청했습니다.", TradeTexts.requestLine("Steve"));
        assertEquals("Alex 님과의 거래 내역을 확인해주세요.", TradeTexts.reviewLine("Alex"));
    }

    private static String row(int start, MenuKind kind) {
        StringBuilder builder = new StringBuilder(9);
        for (int offset = 0; offset < 9; offset++) {
            int slot = start + offset;
            if (slot == GuiLayout.confirmSlot(kind)) {
                builder.append('C');
            } else if (slot == GuiLayout.cancelSlot(kind)) {
                builder.append('X');
            } else {
                assertTrue(GuiLayout.isFillerOffset(offset));
                builder.append('G');
            }
        }
        return builder.toString();
    }
}
