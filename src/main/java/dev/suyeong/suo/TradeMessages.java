package dev.suyeong.suo;

import java.time.Duration;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

final class TradeMessages {
    private static final ClickCallback.Options CLICKS = ClickCallback.Options.builder()
            .uses(5)
            .lifetime(Duration.ofMinutes(5))
            .build();

    private TradeMessages() {
    }

    static Component cancelled() {
        return Component.text(TradeTexts.CANCELLED, NamedTextColor.RED, TextDecoration.BOLD);
    }

    static Component approved() {
        return Component.text(TradeTexts.APPROVED, NamedTextColor.GREEN, TextDecoration.BOLD);
    }

    static void cancelledTo(Player player) {
        if (player != null) {
            player.sendMessage(cancelled());
        }
    }

    static void approvedTo(Player player) {
        if (player != null) {
            player.sendMessage(approved());
        }
    }

    static void request(Player partner, TradeSession session, TradeService trades, JavaPlugin plugin) {
        Component accept = button(plugin, TradeTexts.ACCEPT, NamedTextColor.GREEN, "거래를 수락합니다", player -> trades.accept(player, session.id()));
        Component deny = button(plugin, TradeTexts.DENY, NamedTextColor.RED, "거래를 거절합니다", player -> trades.reject(player, session.id()));
        partner.sendMessage(Component.text(TradeTexts.requestLine(session.requesterName()))
                .append(Component.newline())
                .append(accept)
                .append(Component.space())
                .append(deny));
    }

    static void review(Player requester, TradeSession session, TradeService trades, JavaPlugin plugin) {
        Component confirm = button(plugin, TradeTexts.REVIEW, NamedTextColor.GOLD, "거래 내역을 확인합니다", player -> trades.review(player, session.id()));
        requester.sendMessage(Component.text(TradeTexts.reviewLine(session.partnerName()))
                .append(Component.newline())
                .append(confirm));
    }

    private static Component button(JavaPlugin plugin, String label, NamedTextColor color, String hover, java.util.function.Consumer<Player> action) {
        ClickCallback<Audience> callback = audience -> {
            if (!(audience instanceof Player player)) {
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> action.accept(player));
        };
        return Component.text(label, color, TextDecoration.BOLD)
                .clickEvent(ClickEvent.callback(callback, CLICKS))
                .hoverEvent(HoverEvent.showText(Component.text(hover)));
    }
}
