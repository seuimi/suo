package dev.suyeong.suo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

final class TradeService {
    static final long DECISION_TIMEOUT_TICKS = 20L * 60L * 3L;

    private static final Component ALREADY_TRADING = Component.text("이미 거래 중입니다. 잠시 후 다시 시도해 주세요.", NamedTextColor.RED);
    private static final Component PARTNER_BUSY = Component.text("상대방이 이미 거래 중입니다.", NamedTextColor.RED);
    private static final Component ENDED = Component.text("이미 종료된 거래입니다.", NamedTextColor.RED);
    private static final Component WAITING_FINAL = Component.text("상대방의 최종 확인을 기다리는 중입니다.", NamedTextColor.GRAY);

    private final JavaPlugin plugin;
    private final TradeMenus menus;
    private final TradeLog log;
    private final Map<UUID, TradeSession> byId = new ConcurrentHashMap<>();
    private final Map<UUID, TradeSession> byPlayer = new ConcurrentHashMap<>();

    TradeService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.menus = new TradeMenus(plugin);
        this.log = new TradeLog(plugin);
    }

    void showHistory(CommandSender sender, int page) {
        log.show(sender, page);
    }

    boolean isLocked(TradeView view) {
        TradeSession session = byId.get(view.sessionId());
        return session == null || session.locked() || session.finished();
    }

    boolean isUiItem(ItemStack stack) {
        return menus.isUiItem(stack);
    }

    void request(Player requester, Player partner) {
        if (byPlayer.containsKey(requester.getUniqueId())) {
            requester.sendMessage(ALREADY_TRADING);
            return;
        }
        if (byPlayer.containsKey(partner.getUniqueId())) {
            requester.sendMessage(PARTNER_BUSY);
            return;
        }
        TradeSession session = new TradeSession(requester, partner);
        link(session);
        requester.openInventory(menus.offer(session));
    }

    void confirm(Player player, TradeView view) {
        TradeSession session = byId.get(view.sessionId());
        if (!canPress(player, session, view) || !session.lock()) {
            return;
        }
        MenuKind kind = view.kind();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (session.finished() || !session.stage().shows(kind)) {
                session.unlock();
                return;
            }
            switch (kind) {
                case OFFER -> confirmOffer(player, session);
                case COUNTER -> confirmCounter(player, session);
                case FINAL -> confirmFinal(player, session);
            }
        });
    }

    void cancel(Player player, TradeView view) {
        TradeSession session = byId.get(view.sessionId());
        if (!canPress(player, session, view) || !session.lock()) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (session.finished() || !session.stage().shows(view.kind())) {
                session.unlock();
                return;
            }
            refund(session, player, false, "취소 버튼");
        });
    }

    void closed(Player player, TradeView view) {
        TradeSession session = byId.get(view.sessionId());
        if (session == null || session.finished() || !session.stage().shows(view.kind())) {
            return;
        }
        refund(session, player, true, "창을 닫음");
    }

    void accept(Player partner, UUID sessionId) {
        TradeSession session = byId.get(sessionId);
        if (session == null || session.finished()) {
            partner.sendMessage(ENDED);
            return;
        }
        if (!partner.getUniqueId().equals(session.partnerId()) || session.stage() != Stage.PENDING) {
            return;
        }
        if (!session.transition(Stage.PENDING, Stage.COUNTER)) {
            return;
        }
        session.unlock();
        partner.openInventory(menus.counter(session));
    }

    void reject(Player partner, UUID sessionId) {
        TradeSession session = byId.get(sessionId);
        if (session == null || session.finished()) {
            partner.sendMessage(ENDED);
            return;
        }
        if (!partner.getUniqueId().equals(session.partnerId()) || session.stage() != Stage.PENDING) {
            return;
        }
        refund(session, partner, false, "거절");
    }

    void review(Player requester, UUID sessionId) {
        TradeSession session = byId.get(sessionId);
        if (session == null || session.finished()) {
            requester.sendMessage(ENDED);
            return;
        }
        if (!requester.getUniqueId().equals(session.requesterId()) || session.stage() != Stage.REVIEW) {
            return;
        }
        if (!session.transition(Stage.REVIEW, Stage.FINAL)) {
            return;
        }
        session.unlock();
        requester.openInventory(menus.review(session));
    }

    void quit(Player player) {
        TradeSession session = byPlayer.get(player.getUniqueId());
        if (session == null || session.finished()) {
            return;
        }
        refund(session, player, false, "접속 종료");
    }

    void death(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        TradeSession session = byPlayer.get(dead.getUniqueId());
        if (session == null || session.finished()) {
            return;
        }
        Held held = take(session, dead);
        if (held == null) {
            return;
        }
        boolean deadIsRequester = dead.getUniqueId().equals(session.requesterId());
        ItemStack[] deadItems = deadIsRequester ? held.requesterItems() : held.partnerItems();
        ItemStack[] otherItems = deadIsRequester ? held.partnerItems() : held.requesterItems();
        Player other = deadIsRequester ? held.partner() : held.requester();
        if (event.getKeepInventory()) {
            Items.give(dead, true, deadItems);
        } else {
            Items.addDrops(event, deadItems);
        }
        Items.give(other, true, otherItems);
        TradeMessages.cancelledTo(held.requester());
        if (held.stage() != Stage.OFFER) {
            TradeMessages.cancelledTo(held.partner());
            log.cancelled(session.requesterName(), session.partnerName(), held.requesterItems(), held.partnerItems(), dead.getName() + " · 사망");
        }
        closeLater(other, session);
        plugin.getLogger().info(session.requesterName() + " 님과 " + session.partnerName() + " 님의 거래가 사망으로 취소되었습니다.");
    }

    void moveCounterItem(Player player, TradeView view, int playerSlot) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            TradeSession session = byId.get(view.sessionId());
            if (session == null || session.finished() || session.stage() != Stage.COUNTER) {
                return;
            }
            if (!player.isOnline()) {
                return;
            }
            Inventory top = player.getOpenInventory().getTopInventory();
            if (!(top.getHolder() instanceof TradeView open) || !open.sessionId().equals(view.sessionId())) {
                return;
            }
            ItemStack stack = player.getInventory().getItem(playerSlot);
            if (stack == null || stack.getType().isAir() || menus.isUiItem(stack)) {
                return;
            }
            ItemStack leftover = Items.moveInto(top, GuiLayout.COUNTER_INPUT, stack);
            if (leftover == null) {
                player.getInventory().setItem(playerSlot, null);
            }
        });
    }

    void shutdown() {
        for (TradeSession session : List.copyOf(byId.values())) {
            refund(session, null, false, "서버 종료");
            for (Player player : List.copyOf(plugin.getServer().getOnlinePlayers())) {
                Inventory top = player.getOpenInventory().getTopInventory();
                if (top.getHolder() instanceof TradeView view && view.sessionId().equals(session.id())) {
                    player.closeInventory();
                }
            }
        }
    }

    private void confirmOffer(Player player, TradeSession session) {
        if (!session.transition(Stage.OFFER, Stage.PENDING)) {
            session.unlock();
            return;
        }
        session.setOffer(takeInputs(player, session, MenuKind.OFFER));
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            closeNow(player, session);
            session.unlock();
            deliverRequest(session);
        });
    }

    private void confirmCounter(Player player, TradeSession session) {
        if (!session.transition(Stage.COUNTER, Stage.REVIEW)) {
            session.unlock();
            return;
        }
        session.setCounter(takeInputs(player, session, MenuKind.COUNTER));
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            closeNow(player, session);
            session.unlock();
            deliverReview(session);
        });
    }

    private void confirmFinal(Player player, TradeSession session) {
        if (!session.transition(Stage.FINAL, Stage.DONE)) {
            session.unlock();
            return;
        }
        stripOpen(player, session);
        ItemStack[] offer = session.takeOffer();
        ItemStack[] counter = session.takeCounter();
        unlink(session);
        Player partner = player(session.partnerId(), null);
        log.approved(session.requesterName(), session.partnerName(), offer, counter);
        Items.give(partner, true, offer);
        Items.give(player, true, counter);
        TradeMessages.approvedTo(player);
        TradeMessages.approvedTo(partner);
        plugin.getServer().getScheduler().runTask(plugin, () -> closeNow(player, session));
        plugin.getLogger().info(session.requesterName() + " 님과 " + session.partnerName() + " 님의 거래가 승인되었습니다.");
    }

    private void deliverRequest(TradeSession session) {
        if (session.finished() || session.stage() != Stage.PENDING) {
            return;
        }
        Player partner = player(session.partnerId(), null);
        Player requester = player(session.requesterId(), null);
        if (partner == null) {
            refund(session, requester, false, "상대방 오프라인");
            return;
        }
        TradeMessages.request(partner, session, this, plugin);
        if (requester != null) {
            requester.sendMessage(Component.text(partner.getName() + " 님에게 거래를 요청했습니다.", NamedTextColor.GRAY));
        }
        armTimeout(session, Stage.PENDING);
    }

    private void deliverReview(TradeSession session) {
        if (session.finished() || session.stage() != Stage.REVIEW) {
            return;
        }
        Player requester = player(session.requesterId(), null);
        Player partner = player(session.partnerId(), null);
        if (requester == null) {
            refund(session, partner, false, "요청자 오프라인");
            return;
        }
        TradeMessages.review(requester, session, this, plugin);
        if (partner != null) {
            partner.sendMessage(WAITING_FINAL);
        }
        armTimeout(session, Stage.REVIEW);
    }

    private void armTimeout(TradeSession session, Stage stage) {
        int generation = session.generation();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (session.finished() || session.stage() != stage || session.generation() != generation) {
                return;
            }
            refund(session, null, false, "시간 초과");
        }, DECISION_TIMEOUT_TICKS);
    }

    private void refund(TradeSession session, Player actor, boolean fromClose, String reason) {
        Held held = take(session, actor);
        if (held == null) {
            return;
        }
        Items.give(held.requester(), true, held.requesterItems());
        Items.give(held.partner(), true, held.partnerItems());
        TradeMessages.cancelledTo(held.requester());
        if (held.stage() != Stage.OFFER) {
            TradeMessages.cancelledTo(held.partner());
            String actorName = actor == null ? "" : actor.getName() + " · ";
            log.cancelled(session.requesterName(), session.partnerName(), held.requesterItems(), held.partnerItems(), actorName + reason);
        }
        if (!fromClose) {
            closeLater(held.requester(), session);
            closeLater(held.partner(), session);
        } else if (actor != null) {
            Player other = actor.getUniqueId().equals(session.requesterId()) ? held.partner() : held.requester();
            closeLater(other, session);
        }
    }

    private Held take(TradeSession session, Player actor) {
        if (session.finished()) {
            return null;
        }
        Player requester = player(session.requesterId(), actor);
        Player partner = player(session.partnerId(), actor);
        ItemStack[] requesterExtra = drain(requester, session);
        ItemStack[] partnerExtra = drain(partner, session);
        Stage stage = session.stage();
        if (!session.markFinished()) {
            return null;
        }
        unlink(session);
        return new Held(
                requester,
                partner,
                combine(session.takeOffer(), requesterExtra),
                combine(session.takeCounter(), partnerExtra),
                stage
        );
    }

    private ItemStack[] takeInputs(Player player, TradeSession session, MenuKind kind) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof TradeView view) || !view.sessionId().equals(session.id())) {
            return Items.emptyNine();
        }
        int start = GuiLayout.inputStart(kind);
        ItemStack[] inputs = start < 0 ? Items.emptyNine() : Items.pull(top, start);
        Items.strip(top);
        return inputs;
    }

    private ItemStack[] drain(Player player, TradeSession session) {
        if (player == null) {
            return Items.emptyNine();
        }
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof TradeView view) || !view.sessionId().equals(session.id())) {
            return Items.emptyNine();
        }
        int start = GuiLayout.inputStart(view.kind());
        ItemStack[] inputs = start < 0 ? Items.emptyNine() : Items.pull(top, start);
        Items.strip(top);
        return inputs;
    }

    private void stripOpen(Player player, TradeSession session) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top.getHolder() instanceof TradeView view && view.sessionId().equals(session.id())) {
            Items.strip(top);
        }
    }

    private void closeLater(Player player, TradeSession session) {
        if (player == null || !plugin.isEnabled()) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> closeNow(player, session));
    }

    private void closeNow(Player player, TradeSession session) {
        if (player == null || !player.isOnline()) {
            return;
        }
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top.getHolder() instanceof TradeView view && view.sessionId().equals(session.id())) {
            player.closeInventory();
        }
    }

    private void link(TradeSession session) {
        byId.put(session.id(), session);
        byPlayer.put(session.requesterId(), session);
        byPlayer.put(session.partnerId(), session);
    }

    private void unlink(TradeSession session) {
        byId.remove(session.id(), session);
        byPlayer.remove(session.requesterId(), session);
        byPlayer.remove(session.partnerId(), session);
    }

    private Player player(UUID id, Player actor) {
        if (actor != null && actor.getUniqueId().equals(id)) {
            return actor;
        }
        return plugin.getServer().getPlayer(id);
    }

    private static boolean canPress(Player player, TradeSession session, TradeView view) {
        return session != null && !session.finished() && session.stage().shows(view.kind()) && viewer(player, session, view.kind());
    }

    private static boolean viewer(Player player, TradeSession session, MenuKind kind) {
        UUID expected = switch (kind) {
            case OFFER, FINAL -> session.requesterId();
            case COUNTER -> session.partnerId();
        };
        return player.getUniqueId().equals(expected);
    }

    private static ItemStack[] combine(ItemStack[] committed, ItemStack[] extra) {
        List<ItemStack> items = new ArrayList<>(18);
        addAll(items, committed);
        addAll(items, extra);
        return items.toArray(ItemStack[]::new);
    }

    private static void addAll(List<ItemStack> target, ItemStack[] items) {
        if (items == null) {
            return;
        }
        for (ItemStack stack : items) {
            if (stack != null && !stack.getType().isAir() && stack.getAmount() > 0) {
                target.add(stack);
            }
        }
    }

    private record Held(Player requester, Player partner, ItemStack[] requesterItems, ItemStack[] partnerItems, Stage stage) {
    }
}
