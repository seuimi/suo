package dev.suyeong.suo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

final class TradeLog {
    static final int PAGE_SIZE = 5;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String HEADER = "# suo 거래 내역. 게임 안에서는 OP가 /suo check 로 확인합니다.";

    private final JavaPlugin plugin;
    private final Path file;
    private final List<TradeRecord> entries = new ArrayList<>();

    TradeLog(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = plugin.getDataFolder().toPath().resolve("trades.log");
        load();
    }

    void approved(String requester, String partner, ItemStack[] offer, ItemStack[] counter) {
        append(new TradeRecord(now(), "승인", requester, partner, Items.summarize(offer), Items.summarize(counter), ""));
    }

    void cancelled(String requester, String partner, ItemStack[] offer, ItemStack[] counter, String reason) {
        append(new TradeRecord(now(), "취소", requester, partner, Items.summarize(offer), Items.summarize(counter), reason == null ? "" : reason));
    }

    void show(CommandSender sender, int page) {
        int pages = pages();
        if (entries.isEmpty() || pages == 0) {
            sender.sendMessage(Component.text("기록된 거래 내역이 없습니다.", NamedTextColor.GRAY));
            return;
        }
        if (page < 1 || page > pages) {
            sender.sendMessage(Component.text("없는 페이지입니다. 1부터 " + pages + "까지 볼 수 있습니다.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("거래 내역 " + page + "/" + pages, NamedTextColor.GOLD));
        int end = entries.size() - (page - 1) * PAGE_SIZE;
        int start = Math.max(0, end - PAGE_SIZE);
        for (int index = end - 1; index >= start; index--) {
            sender.sendMessage(toChat(entries.get(index)));
        }
        sender.sendMessage(Component.text("파일: plugins/suo/trades.log", NamedTextColor.DARK_GRAY));
        if (page < pages) {
            sender.sendMessage(Component.text("다음 페이지: /suo check " + (page + 1), NamedTextColor.GRAY));
        }
    }

    int pages() {
        if (entries.isEmpty()) {
            return 0;
        }
        return (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    private void load() {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                TradeRecord entry = TradeRecord.decode(line);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        } catch (IOException exception) {
            plugin.getLogger().severe("거래 내역을 읽지 못했습니다: " + exception.getMessage());
        }
    }

    private void append(TradeRecord entry) {
        entries.add(entry);
        try {
            if (plugin.getDataFolder().mkdirs() || plugin.getDataFolder().isDirectory()) {
                if (!Files.exists(file)) {
                    Files.writeString(file, HEADER + System.lineSeparator(), StandardCharsets.UTF_8);
                }
                Files.writeString(
                        file,
                        entry.encode() + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            }
        } catch (IOException exception) {
            plugin.getLogger().severe("거래 내역을 저장하지 못했습니다: " + exception.getMessage());
        }
    }

    private static String now() {
        return TIME.format(ZonedDateTime.now(ZoneId.systemDefault()));
    }

    private static Component toChat(TradeRecord entry) {
        boolean approved = "승인".equals(entry.result());
        NamedTextColor resultColor = approved ? NamedTextColor.GREEN : NamedTextColor.RED;
        String headline = "[" + shortTime(entry.time()) + "] " + entry.result();
        if (entry.reason() != null && !entry.reason().isBlank()) {
            headline += " · " + entry.reason();
        }
        String first = approved
                ? entry.requester() + " → " + entry.partner() + ": " + entry.requesterItems()
                : entry.requester() + " 이(가) 올린 것: " + entry.requesterItems();
        String second = approved
                ? entry.partner() + " → " + entry.requester() + ": " + entry.partnerItems()
                : entry.partner() + " 이(가) 올린 것: " + entry.partnerItems();
        return Component.text(headline, resultColor)
                .append(Component.newline())
                .append(Component.text(first, NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text(second, NamedTextColor.WHITE));
    }

    private static String shortTime(String time) {
        if (time != null && time.length() >= 16) {
            return time.substring(5, 16);
        }
        return time == null ? "" : time;
    }
}
