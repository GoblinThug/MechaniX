package org.goblinthug.mechanix.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.goblinthug.mechanix.MechaniX;

import java.util.ArrayList;
import java.util.List;

public class MainMenu {

    public static final int SLOT_GATES = 11;
    public static final int SLOT_PORTALS = 13;
    public static final int SLOT_CLOSE = 15;

    public static Component title(MechaniX plugin) {
        return Component.text(plugin.getLangManager().raw("main-title"), NamedTextColor.DARK_AQUA);
    }

    public static void open(MechaniX plugin, Player player) {
        var lang = plugin.getLangManager();
        MechaniXHolder holder = new MechaniXHolder(MenuType.MAIN);
        Inventory inv = Bukkit.createInventory(holder, 27, title(plugin));
        holder.setInventory(inv);

        inv.setItem(SLOT_GATES, makeItem(Material.IRON_DOOR,
                Component.text(lang.raw("main-gates"), NamedTextColor.GREEN),
                Component.text(lang.raw("main-gates-lore"), NamedTextColor.GRAY)));

        inv.setItem(SLOT_PORTALS, makeItem(Material.ENDER_PEARL,
                Component.text(lang.raw("main-portals"), NamedTextColor.LIGHT_PURPLE),
                Component.text(lang.raw("main-portals-lore"), NamedTextColor.GRAY)));

        inv.setItem(SLOT_CLOSE, makeItem(Material.BARRIER,
                Component.text(lang.raw("menu-close"), NamedTextColor.RED)));

        player.openInventory(inv);
    }

    public static ItemStack makeItem(Material material, Component name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            List<Component> loreList = new ArrayList<>();
            for (Component line : lore) {
                loreList.add(line.decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }
}