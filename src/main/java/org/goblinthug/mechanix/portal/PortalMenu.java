package org.goblinthug.mechanix.portal;

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

public class PortalMenu {

    public static final int SLOT_CREATE = 11;
    public static final int SLOT_LIST = 13;
    public static final int SLOT_CLOSE = 15;

    // Создание портала
    public static final int SLOT_SELECT_A = 11;
    public static final int SLOT_SELECT_B = 15;
    public static final int SLOT_CONFIRM = 13;
    public static final int SLOT_BACK_CREATE = 22;

    public static final int SLOT_SHOW = 11;
    public static final int SLOT_DELETE = 15;
    public static final int SLOT_BACK_PORTAL = 22;

    public static Component titleMain(MechaniX plugin) {
        return Component.text(plugin.getLangManager().raw("portal-title-main"), NamedTextColor.DARK_PURPLE);
    }

    public static Component titleCreate(MechaniX plugin) {
        return Component.text(plugin.getLangManager().raw("portal-title-create"), NamedTextColor.DARK_PURPLE);
    }

    public static Component titleList(MechaniX plugin) {
        return Component.text(plugin.getLangManager().raw("portal-title-list"), NamedTextColor.DARK_PURPLE);
    }

    public static Component titlePortal(MechaniX plugin) {
        return Component.text(plugin.getLangManager().raw("portal-title-manage"), NamedTextColor.DARK_PURPLE);
    }

    public static void openMain(MechaniX plugin, Player player) {
        var lang = plugin.getLangManager();
        MechaniXHolder holder = new MechaniXHolder(MenuType.PORTAL_MAIN);
        Inventory inv = Bukkit.createInventory(holder, 27, titleMain(plugin));
        holder.setInventory(inv);

        inv.setItem(SLOT_CREATE, makeItem(Material.ENDER_PEARL,
                Component.text(lang.raw("portal-menu-create"), NamedTextColor.LIGHT_PURPLE),
                Component.text(lang.raw("portal-menu-create-lore"), NamedTextColor.GRAY)));

        inv.setItem(SLOT_LIST, makeItem(Material.CHEST,
                Component.text(lang.raw("portal-menu-list"), NamedTextColor.LIGHT_PURPLE),
                Component.text(lang.raw("portal-menu-list-lore"), NamedTextColor.GRAY)));

        inv.setItem(SLOT_CLOSE, makeItem(Material.BARRIER,
                Component.text(lang.raw("menu-close"), NamedTextColor.RED)));

        player.openInventory(inv);
    }

    public static void openCreate(MechaniX plugin, Player player,
                                  PortalStorage.Zone a, PortalStorage.Zone b) {
        var lang = plugin.getLangManager();
        MechaniXHolder holder = new MechaniXHolder(MenuType.PORTAL_CREATE);
        Inventory inv = Bukkit.createInventory(holder, 27, titleCreate(plugin));
        holder.setInventory(inv);

        inv.setItem(SLOT_SELECT_A, makeItem(
                a == null ? Material.GRAY_DYE : Material.LIME_DYE,
                Component.text(lang.raw("portal-menu-select1"), NamedTextColor.GREEN),
                Component.text(a == null
                                ? lang.raw("portal-menu-status-unset")
                                : lang.raw("portal-menu-status-set")
                                + " (" + a.width() + "x" + a.height() + ")",
                        NamedTextColor.GRAY)));

        inv.setItem(SLOT_SELECT_B, makeItem(
                b == null ? Material.GRAY_DYE : Material.LIME_DYE,
                Component.text(lang.raw("portal-menu-select2"), NamedTextColor.GREEN),
                Component.text(b == null
                                ? lang.raw("portal-menu-status-unset")
                                : lang.raw("portal-menu-status-set")
                                + " (" + b.width() + "x" + b.height() + ")",
                        NamedTextColor.GRAY)));

        inv.setItem(SLOT_CONFIRM, makeItem(Material.EMERALD_BLOCK,
                Component.text(lang.raw("portal-menu-create"), NamedTextColor.GREEN)));

        inv.setItem(SLOT_BACK_CREATE, makeItem(Material.ARROW,
                Component.text(lang.raw("menu-back"), NamedTextColor.YELLOW)));

        player.openInventory(inv);
    }

    public static void openList(MechaniX plugin, Player player, List<String> names) {
        var lang = plugin.getLangManager();
        MechaniXHolder holder = new MechaniXHolder(MenuType.PORTAL_LIST);
        Inventory inv = Bukkit.createInventory(holder, 54, titleList(plugin));
        holder.setInventory(inv);

        int slot = 0;
        for (String name : names) {
            if (slot >= 45) break;
            inv.setItem(slot++, makeItem(Material.ENDER_PEARL,
                    Component.text(name, NamedTextColor.LIGHT_PURPLE),
                    Component.text(lang.raw("menu-zone-entry-lore"), NamedTextColor.GRAY)));
        }

        inv.setItem(49, makeItem(Material.ARROW,
                Component.text(lang.raw("menu-back"), NamedTextColor.YELLOW)));

        player.openInventory(inv);
    }

    public static void openPortal(MechaniX plugin, Player player) {
        var lang = plugin.getLangManager();
        MechaniXHolder holder = new MechaniXHolder(MenuType.PORTAL_MANAGE);
        Inventory inv = Bukkit.createInventory(holder, 27, titlePortal(plugin));
        holder.setInventory(inv);

        inv.setItem(SLOT_SHOW, makeItem(Material.ENDER_EYE,
                Component.text(lang.raw("menu-show"), NamedTextColor.GREEN),
                Component.text(lang.raw("menu-show-lore"), NamedTextColor.GRAY)));

        inv.setItem(SLOT_DELETE, makeItem(Material.REDSTONE_BLOCK,
                Component.text(lang.raw("menu-delete"), NamedTextColor.RED),
                Component.text(lang.raw("menu-delete-lore"), NamedTextColor.GRAY)));

        inv.setItem(SLOT_BACK_PORTAL, makeItem(Material.ARROW,
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