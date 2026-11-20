package dev.suyeong.suo;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

final class TradeListener implements Listener {
    private final TradeService trades;

    TradeListener(TradeService trades) {
        this.trades = trades;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof TradeView view) || !(event.getWhoClicked() instanceof Player player)) {
            scrubLeakedPane(event);
            return;
        }

        if (trades.isLocked(view) || event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            return;
        }

        if (clicked.getHolder() instanceof TradeView) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (GuiLayout.isInput(view.kind(), slot)) {
                event.setCancelled(false);
                return;
            }
            if (pressed(event.getClick()) && slot == GuiLayout.confirmSlot(view.kind())) {
                trades.confirm(player, view);
            } else if (pressed(event.getClick()) && slot == GuiLayout.cancelSlot(view.kind())) {
                trades.cancel(player, view);
            }
            return;
        }

        if (event.isShiftClick()) {
            if (view.kind() == MenuKind.OFFER) {
                return;
            }
            event.setCancelled(true);
            if (view.kind() == MenuKind.COUNTER && !trades.isUiItem(event.getCurrentItem())) {
                trades.moveCounterItem(player, view, event.getSlot());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof TradeView view)) {
            return;
        }
        if (trades.isLocked(view)) {
            event.setCancelled(true);
            return;
        }
        int size = top.getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < size && !GuiLayout.isInput(view.kind(), rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof TradeView view) || !(event.getPlayer() instanceof Player player)) {
            return;
        }
        trades.closed(player, view);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        trades.quit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        trades.death(event);
    }

    private void scrubLeakedPane(InventoryClickEvent event) {
        if (trades.isUiItem(event.getCurrentItem())) {
            event.setCancelled(true);
            event.setCurrentItem(null);
        }
        if (trades.isUiItem(event.getCursor())) {
            event.setCancelled(true);
            event.getWhoClicked().setItemOnCursor(null);
        }
    }

    private static boolean pressed(ClickType click) {
        return click == ClickType.LEFT
                || click == ClickType.RIGHT
                || click == ClickType.SHIFT_LEFT
                || click == ClickType.SHIFT_RIGHT;
    }
}
