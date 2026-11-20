package dev.suyeong.suo;

import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

final class SuoCommand implements CommandExecutor, TabCompleter {
    private static final Component PLAYERS_ONLY = Component.text("플레이어만 사용할 수 있습니다.", NamedTextColor.RED);
    private static final Component USAGE = Component.text("사용법: /suo <닉네임>", NamedTextColor.GRAY);
    private static final Component NO_PERMISSION = Component.text("거래를 사용할 권한이 없습니다.", NamedTextColor.RED);
    private static final Component SELF = Component.text("자기 자신에게는 거래를 요청할 수 없습니다.", NamedTextColor.RED);
    private static final Component MISSING = Component.text("플레이어를 찾을 수 없습니다.", NamedTextColor.RED);
    private static final Component DEAD = Component.text("상대방이 죽은 상태입니다.", NamedTextColor.RED);

    private final TradeService trades;

    SuoCommand(TradeService trades) {
        this.trades = trades;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PLAYERS_ONLY);
            return true;
        }
        if (!player.hasPermission("suo.use")) {
            player.sendMessage(NO_PERMISSION);
            return true;
        }
        if (args.length != 1 || args[0].isBlank()) {
            player.sendMessage(USAGE);
            return true;
        }
        Player partner = player.getServer().getPlayerExact(args[0]);
        if (partner == null) {
            player.sendMessage(MISSING);
            return true;
        }
        if (partner.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(SELF);
            return true;
        }
        if (partner.isDead()) {
            player.sendMessage(DEAD);
            return true;
        }
        trades.request(player, partner);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length > 1 || !(sender instanceof Player player)) {
            return List.of();
        }
        String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        return player.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> !name.equalsIgnoreCase(player.getName()))
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
