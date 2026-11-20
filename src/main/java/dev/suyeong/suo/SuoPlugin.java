package dev.suyeong.suo;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SuoPlugin extends JavaPlugin {
    private TradeService trades;

    @Override
    public void onEnable() {
        this.trades = new TradeService(this);
        getServer().getPluginManager().registerEvents(new TradeListener(trades), this);
        PluginCommand command = getCommand("suo");
        if (command == null) {
            getLogger().severe("/suo 명령어를 등록하지 못했습니다.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        SuoCommand executor = new SuoCommand(trades);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        getLogger().info("suo 거래 플러그인을 켰습니다.");
    }

    @Override
    public void onDisable() {
        if (trades != null) {
            trades.shutdown();
        }
    }
}
