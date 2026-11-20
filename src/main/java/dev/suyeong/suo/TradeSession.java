package dev.suyeong.suo;

import java.util.Arrays;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

final class TradeSession {
    private final UUID id = UUID.randomUUID();
    private final UUID requesterId;
    private final UUID partnerId;
    private final String requesterName;
    private final String partnerName;
    private Stage stage = Stage.OFFER;
    private int generation;
    private boolean finished;
    private boolean locked;
    private final ItemStack[] offer = new ItemStack[9];
    private final ItemStack[] counter = new ItemStack[9];

    TradeSession(Player requester, Player partner) {
        this.requesterId = requester.getUniqueId();
        this.partnerId = partner.getUniqueId();
        this.requesterName = requester.getName();
        this.partnerName = partner.getName();
    }

    UUID id() {
        return id;
    }

    UUID requesterId() {
        return requesterId;
    }

    UUID partnerId() {
        return partnerId;
    }

    String requesterName() {
        return requesterName;
    }

    String partnerName() {
        return partnerName;
    }

    Stage stage() {
        return stage;
    }

    int generation() {
        return generation;
    }

    boolean finished() {
        return finished;
    }

    boolean locked() {
        return locked;
    }

    boolean lock() {
        if (locked || finished) {
            return false;
        }
        locked = true;
        return true;
    }

    void unlock() {
        locked = false;
    }

    boolean transition(Stage from, Stage to) {
        if (finished || stage != from) {
            return false;
        }
        stage = to;
        generation++;
        if (to == Stage.DONE) {
            finished = true;
        }
        return true;
    }

    boolean markFinished() {
        if (finished) {
            return false;
        }
        finished = true;
        stage = Stage.DONE;
        generation++;
        return true;
    }

    void setOffer(ItemStack[] items) {
        copyInto(offer, items);
    }

    void setCounter(ItemStack[] items) {
        copyInto(counter, items);
    }

    ItemStack[] copyOffer() {
        return cloneOf(offer);
    }

    ItemStack[] copyCounter() {
        return cloneOf(counter);
    }

    ItemStack[] takeOffer() {
        ItemStack[] taken = cloneOf(offer);
        Arrays.fill(offer, null);
        return taken;
    }

    ItemStack[] takeCounter() {
        ItemStack[] taken = cloneOf(counter);
        Arrays.fill(counter, null);
        return taken;
    }

    private static void copyInto(ItemStack[] target, ItemStack[] source) {
        Arrays.fill(target, null);
        if (source == null) {
            return;
        }
        for (int i = 0; i < target.length && i < source.length; i++) {
            ItemStack stack = source[i];
            if (stack != null && !stack.getType().isAir() && stack.getAmount() > 0) {
                target[i] = stack.clone();
            }
        }
    }

    private static ItemStack[] cloneOf(ItemStack[] source) {
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            ItemStack stack = source[i];
            if (stack != null && !stack.getType().isAir() && stack.getAmount() > 0) {
                copy[i] = stack.clone();
            }
        }
        return copy;
    }
}
