package dev.suyeong.suo;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

final class TradeView implements InventoryHolder {
    private final UUID sessionId;
    private final MenuKind kind;
    private final Inventory inventory;

    TradeView(UUID sessionId, MenuKind kind, Component title, int size) {
        this.sessionId = sessionId;
        this.kind = kind;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    UUID sessionId() {
        return sessionId;
    }

    MenuKind kind() {
        return kind;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
