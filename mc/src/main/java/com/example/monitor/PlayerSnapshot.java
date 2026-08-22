package com.example.monitor;

import org.bukkit.entity.Player;

import java.util.List;

public final class PlayerSnapshot {

    private PlayerSnapshot() {
    }

    public static String render(Player p) {
        double health = p.getHealth();
        double maxHealth = p.getMaxHealth();
        int food = p.getFoodLevel();
        float saturation = p.getSaturation();
        int xpLevel = p.getLevel();
        float xpProgress = p.getExp() * 100f;
        int xpTotal = p.getTotalExperience();

        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(escape(p.getName())).append("</b> — <i>online</i>\n");
        sb.append("\u2764\ufe0f Health: ").append(String.format("%.1f/%.1f", health, maxHealth)).append('\n');
        sb.append("\ud83c\udf56 Hunger: ").append(food).append(" (sat ").append(String.format("%.1f", saturation)).append(")\n");
        sb.append("\u2728 XP: level ").append(xpLevel)
                .append(" (").append(String.format("%.1f", xpProgress)).append("% ke level berikutnya, total ").append(xpTotal).append(")\n");
        sb.append("\ud83d\udccd Lokasi: ")
                .append(escape(p.getWorld().getName())).append(" ")
                .append(p.getLocation().getBlockX()).append(", ")
                .append(p.getLocation().getBlockY()).append(", ")
                .append(p.getLocation().getBlockZ()).append('\n');

        sb.append('\n');
        List<String> inv = ItemSerializer.inventory(p);
        for (String line : inv) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}