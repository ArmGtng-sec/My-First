package com.example.monitor;

import org.bukkit.plugin.java.JavaPlugin;

public final class MonitorPlugin extends JavaPlugin {

    private TelegramBot telegramBot;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String token = getConfig().getString("bot-token", "");
        String password = getConfig().getString("password", "");

        if (token.isEmpty() || token.equals("ISI_TOKEN_DARI_BOTFATHER")) {
            getLogger().warning("bot-token belum diisi di config.yml. Telegram bot tidak dijalankan.");
            return;
        }

        telegramBot = new TelegramBot(this, token, password);
        telegramBot.start();
        getLogger().info("PlayerMonitor aktif, bot Telegram berjalan.");
    }

    @Override
    public void onDisable() {
        if (telegramBot != null) {
            telegramBot.stop();
        }
        getLogger().info("PlayerMonitor mati.");
    }
}