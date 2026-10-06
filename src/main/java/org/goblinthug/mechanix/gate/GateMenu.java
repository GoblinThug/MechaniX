package org.goblinthug.mechanix.gate;

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
import org.goblinthug.mechanix.menu.MechaniXHolder;
import org.goblinthug.mechanix.menu.MenuType;

import java.util.ArrayList;
import java.util.List;

public class GateMenu {

    public static final int SLOT_CREATE = 11;
    public static final int SLOT_LIST = 13;
    public static final int SLOT_CLOSE = 15;

    public static final int SLOT_POS1 = 10;
    public static final int SLOT_POS2 = 12;
    public static final int SLOT_CONFIRM = 14;
    public static final int SLOT_BACK_CREATE = 16;

    public static final int SLOT_BIND = 10;
    public static final int SLOT_UNBIND = 12;
    public static final int SLOT_SHOW = 14;
    public static final int SLOT_DELETE = 16;
    public static final int SLOT_BACK_ZONE = 22;

    public static Component titleMain(MechaniX plugin) {
        return Component.text(plugin.getLangManager().raw("menu-title-main"), NamedTextColor.DARK_GREEN);
    }

    public static Component titleCreate(MechaniX plugin) {
        return Component.text(plugin.getLangManager().raw("menu-title-create"), NamedTextColor.DARK_GREEN);
    }

    public static Component titleList(MechaniX plugin) {
        return Component.text(plugin.getLangManager().raw("menu-title-list"), NamedTextColor.DARK_GREEN);
    }

    public static Component titleZone(MechaniX plugin) {
        return Component.text(plugin.getLangManager().raw("menu-title-zone"), NamedTextColor.DARK_GREEN);
    }

    public static void openMain(MechaniX plugin, Player player) {
        var lang = plugin.getLangManager();
        MechaniXHolder holder = new MechaniXHolder(MenuType.GATE_MAIN);
        Inventory inv = Bukkit.createInventory(holder, 27, titleMain(plugin));
        holder.setInventory(inv);

        inv.setItem(SLOT_CREATE, makeItem(Material.EMERALD_BLOCK,
                Component.text(lang.raw("menu-create"), NamedTextColor.GREEN),
                Component.text(lang.raw("menu-create-lore"), NamedTextColor.GRAY)));

        inv.setItem(SLOT_LIST, makeItem(Material.CHEST,
                Component.text(lang.raw("menu-list"), NamedTextColor.GREEN),
                Component.text(lang.raw("menu-list-lore"), NamedTextColor.GRAY)));

        inv.setItem(SLOT_CLOSE, makeItem(Material.BARRIER,
                Component.text(lang.raw("menu-close"), NamedTextColor.RED)));

        player.openInventory(inv);
    }

    public static void openCreate(MechaniX plugin, Player player) {
        var lang = plugin.getLangManager();
        MechaniXHolder holder = new MechaniXHolder(MenuType.GATE_CREATE);
        Inventory inv = Bukkit.createInventory(holder, 27, titleCreate(plugin));
        holder.setInventory(inv);

        inv.setItem(SLOT_POS1, makeItem(Material.LIME_WOOL,
                Component.text(lang.raw("menu-pos1"), NamedTextColor.GREEN),
                Component.text(lang.raw("menu-pos1-lore"), NamedTextColor.GRAY)));

        inv.setItem(SLOT_POS2, makeItem(Material.LIME_WOOL,
                Component.text(lang.raw("menu-pos2"), NamedTextColor.GREEN),
                Component.text(lang.raw("menu-pos2-lore"), NamedTextColor.GRAY)));

        inv.setItem(SLOT_CONFIRM, makeItem(Material.EMERALD_BLOCK,
                Component.text(lang.raw("menu-confirm"), NamedTextColor.GREEN),
                Component.text(lang.raw("menu-confirm-lore"), NamedTextColor.GRAY)));

        inv.setItem(SLOT_BACK_CREATE, makeItem(Material.ARROW,
                Component.text(lang.raw("menu-back"), NamedTextColor.YELLOW)));

        player.openInventory(inv);
    }

    public static void openZoneList(MechaniX plugin, Player player, List<String> zoneNames) {
        var lang = plugin.getLangManager();
        MechaniXHolder holder = new MechaniXHolder(MenuType.GATE_LIST);
        Inventory inv = Bukkit.createInventory(holder, 54, titleList(plugin));
        holder.setInventory(inv);

        int slot = 0;
        for (String name : zoneNames) {
            if (slot >= 45) break;
            inv.setItem(slot++, makeItem(Material.PAPER,
                    Component.text(name, NamedTextColor.YELLOW),
                    Component.text(lang.raw("menu-zone-entry-lore"), NamedTextColor.GRAY)));
        }

        inv.setItem(49, makeItem(Material.ARROW,
                Component.text(lang.raw("menu-back"), NamedTextColor.YELLOW)));

        player.openInventory(inv);
    }

    public static void openZone(MechaniX plugin, Player player) {
        var lang = plugin.getLangManager();
        MechaniXHolder holder = new MechaniXHolder(MenuType.GATE_ZONE);
        Inventory inv = Bukkit.createInventory(holder, 27, titleZone(plugin));
        holder.setInventory(inv);

        inv.setItem(SLOT_BIND, makeItem(Material.TRIPWIRE_HOOK,
                Component.text(lang.raw("menu-bind"), NamedTextColor.GREEN),
                Component.text(lang.raw("menu-bind-lore"), NamedTextColor.GRAY)));

        inv.setItem(SLOT_UNBIND, makeItem(Material.SHEARS,
                Component.text(lang.raw("menu-unbind"), NamedTextColor.RED),
                Component.text(lang.raw("menu-unbind-lore"), NamedTextColor.GRAY)));

        inv.setItem(SLOT_SHOW, makeItem(Material.ENDER_EYE,
                Component.text(lang.raw("menu-show"), NamedTextColor.GREEN),
                Component.text(lang.raw("menu-show-lore"), NamedTextColor.GRAY)));

        inv.setItem(SLOT_DELETE, makeItem(Material.REDSTONE_BLOCK,
                Component.text(lang.raw("menu-delete"), NamedTextColor.RED),
                Component.text(lang.raw("menu-delete-lore"), NamedTextColor.GRAY)));

        inv.setItem(SLOT_BACK_ZONE, makeItem(Material.ARROW,
                Component.text(lang.raw("menu-back"), NamedTextColor.YELLOW)));

        player.openInventory(inv);
    }

    private static ItemStack makeItem(Material material, Component name, Component... lore) {
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