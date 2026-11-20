package dev.suyeong.suo;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

final class Items {
    private static final Component OVERFLOW = Component.text(
            "인벤토리가 가득 차서 일부 아이템을 바닥에 떨어뜨렸습니다.",
            NamedTextColor.YELLOW
    );

    private Items() {
    }

    static ItemStack[] emptyNine() {
        return new ItemStack[9];
    }

    static ItemStack[] pull(Inventory inventory, int start) {
        ItemStack[] items = emptyNine();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(start + i);
            if (stack != null && !stack.getType().isAir() && stack.getAmount() > 0) {
                items[i] = stack.clone();
            }
            inventory.setItem(start + i, null);
        }
        return items;
    }

    static void strip(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, null);
        }
    }

    static void give(Player player, boolean notifyOverflow, ItemStack[]... groups) {
        if (player == null) {
            return;
        }
        boolean overflow = false;
        World world = player.getWorld();
        Location location = player.getLocation();
        for (ItemStack[] group : groups) {
            overflow |= place(player, world, location, group);
        }
        if (overflow && notifyOverflow) {
            player.sendMessage(OVERFLOW);
        }
    }

    static void addDrops(PlayerDeathEvent event, ItemStack[] items) {
        if (items == null) {
            return;
        }
        for (ItemStack stack : items) {
            if (present(stack)) {
                event.getDrops().add(stack.clone());
            }
        }
    }

    /**
     * Moves as much of {@code moving} as possible into the nine slots starting at {@code start}.
     * The source stack's amount is reduced. Empty result means the whole stack moved.
     */
    static ItemStack moveInto(Inventory inventory, int start, ItemStack moving) {
        if (!present(moving)) {
            return null;
        }
        int max = Math.max(1, moving.getMaxStackSize());
        for (int pass = 0; pass < 2 && moving.getAmount() > 0; pass++) {
            for (int i = 0; i < 9 && moving.getAmount() > 0; i++) {
                ItemStack slot = inventory.getItem(start + i);
                if (pass == 0) {
                    if (!present(slot) || !slot.isSimilar(moving)) {
                        continue;
                    }
                    int room = max - slot.getAmount();
                    if (room <= 0) {
                        continue;
                    }
                    int moved = Math.min(room, moving.getAmount());
                    slot.setAmount(slot.getAmount() + moved);
                    moving.setAmount(moving.getAmount() - moved);
                } else if (!present(slot)) {
                    int moved = Math.min(max, moving.getAmount());
                    ItemStack placed = moving.clone();
                    placed.setAmount(moved);
                    inventory.setItem(start + i, placed);
                    moving.setAmount(moving.getAmount() - moved);
                }
            }
        }
        return moving.getAmount() <= 0 ? null : moving;
    }

    private static boolean place(Player player, World world, Location location, ItemStack[] items) {
        if (items == null) {
            return false;
        }
        boolean overflow = false;
        for (ItemStack stack : items) {
            if (!present(stack)) {
                continue;
            }
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack.clone());
            for (ItemStack left : leftover.values()) {
                if (!present(left)) {
                    continue;
                }
                world.dropItemNaturally(location, left);
                overflow = true;
            }
        }
        return overflow;
    }

    private static boolean present(ItemStack stack) {
        return stack != null && !stack.getType().isAir() && stack.getAmount() > 0;
    }
}
