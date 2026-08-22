package com.example.monitor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

public final class ItemSerializer {

    private ItemSerializer() {
    }

    public static String name(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        String displayName = item.getItemMeta() != null && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : null;
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }
        String mat = item.getType().name().toLowerCase().replace('_', ' ');
        return capitalize(mat);
    }

    public static List<String> inventory(Player player) {
        List<String> lines = new ArrayList<>();
        PlayerInventory inv = player.getInventory();

        lines.add("<b>Hotbar:</b>");
        for (int i = 0; i < 9; i++) {
            append(lines, inv.getItem(i));
        }

        lines.add("<b>Inventory:</b>");
        for (int i = 9; i < 36; i++) {
            append(lines, inv.getItem(i));
        }

        lines.add("<b>Armor:</b>");
        append(lines, inv.getHelmet());
        append(lines, inv.getChestplate());
        append(lines, inv.getLeggings());
        append(lines, inv.getBoots());

        lines.add("<b>Offhand:</b>");
        append(lines, inv.getItemInOffHand());

        return lines;
    }

    private static void append(List<String> lines, ItemStack item) {
        String n = name(item);
        if (n != null) {
            lines.add("- " + escape(n) + " x" + item.getAmount());
        }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String capitalize(String s) {
        String[] parts = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}