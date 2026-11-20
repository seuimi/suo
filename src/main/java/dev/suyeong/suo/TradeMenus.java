package dev.suyeong.suo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

final class TradeMenus {
    private final NamespacedKey uiKey;
    private final ItemStack filler;
    private final ItemStack confirm;
    private final ItemStack cancel;

    TradeMenus(Plugin plugin) {
        this.uiKey = new NamespacedKey(plugin, "ui");
        this.filler = filler();
        this.confirm = button(Material.GREEN_STAINED_GLASS_PANE, TradeTexts.CONFIRM, NamedTextColor.GREEN);
        this.cancel = button(Material.RED_STAINED_GLASS_PANE, TradeTexts.CANCEL, NamedTextColor.RED);
    }

    Inventory offer(TradeSession session) {
        TradeView view = new TradeView(session.id(), MenuKind.OFFER, Component.text(TradeTexts.TITLE_OFFER), GuiLayout.OFFER_SIZE);
        paintButtons(view.getInventory(), GuiLayout.OFFER_BUTTON_ROW);
        return view.getInventory();
    }

    Inventory counter(TradeSession session) {
        TradeView view = new TradeView(
                session.id(),
                MenuKind.COUNTER,
                Component.text(TradeTexts.TITLE_COUNTER),
                GuiLayout.TRADE_SIZE
        );
        Inventory inventory = view.getInventory();
        place(inventory, session.copyOffer(), 0);
        paintRow(inventory, GuiLayout.SEPARATOR_ROW);
        paintButtons(inventory, GuiLayout.TRADE_BUTTON_ROW);
        return inventory;
    }

    Inventory review(TradeSession session) {
        TradeView view = new TradeView(
                session.id(),
                MenuKind.FINAL,
                Component.text(TradeTexts.TITLE_FINAL),
                GuiLayout.TRADE_SIZE
        );
        Inventory inventory = view.getInventory();
        place(inventory, session.copyOffer(), 0);
        paintRow(inventory, GuiLayout.SEPARATOR_ROW);
        place(inventory, session.copyCounter(), GuiLayout.COUNTER_INPUT);
        paintButtons(inventory, GuiLayout.TRADE_BUTTON_ROW);
        return inventory;
    }

    boolean isUiItem(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return false;
        }
        Byte mark = stack.getItemMeta().getPersistentDataContainer().get(uiKey, PersistentDataType.BYTE);
        return mark != null;
    }

    private void paintButtons(Inventory inventory, int rowStart) {
        for (int offset = 0; offset < 9; offset++) {
            ItemStack pane = switch (offset) {
                case GuiLayout.CONFIRM_OFFSET -> confirm.clone();
                case GuiLayout.CANCEL_OFFSET -> cancel.clone();
                default -> filler.clone();
            };
            inventory.setItem(rowStart + offset, pane);
        }
    }

    private void paintRow(Inventory inventory, int rowStart) {
        for (int offset = 0; offset < 9; offset++) {
            inventory.setItem(rowStart + offset, filler.clone());
        }
    }

    private static void place(Inventory inventory, ItemStack[] items, int start) {
        for (int i = 0; i < items.length; i++) {
            ItemStack stack = items[i];
            if (stack != null && !stack.getType().isAir()) {
                inventory.setItem(start + i, stack.clone());
            }
        }
    }

    private ItemStack filler() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        meta.setHideTooltip(true);
        tag(meta);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack button(Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name)
                .color(color)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        tag(meta);
        item.setItemMeta(meta);
        return item;
    }

    private void tag(ItemMeta meta) {
        meta.getPersistentDataContainer().set(uiKey, PersistentDataType.BYTE, (byte) 1);
    }
}
