package org.goblinthug.mechanix.portal;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.goblinthug.mechanix.MechaniX;
import org.goblinthug.mechanix.menu.MainMenu;
import org.goblinthug.mechanix.menu.MechaniXHolder;
import org.goblinthug.mechanix.menu.MenuType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PortalMenuListener implements Listener {

    private final MechaniX plugin;

    // 1 = frame A, 2 = frame B
    private final Map<UUID, Integer> awaitingFrame = new HashMap<>();
    private final Map<UUID, PortalStorage.Zone> frameA = new HashMap<>();
    private final Map<UUID, PortalStorage.Zone> frameB = new HashMap<>();
    private final Map<UUID, String> openPortal = new HashMap<>();

    // Состояния чат-ввода при создании портала
    private final Map<UUID, String> pendingName = new HashMap<>();
    private final Map<UUID, String> awaitingColorFor = new HashMap<>();

    public PortalMenuListener(MechaniX plugin) {
        this.plugin = plugin;
    }

    // ============================================================
    //                    Обработка клика в меню
    // ============================================================

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MechaniXHolder holder)) return;

        MenuType type = holder.getType();
        switch (type) {
            case PORTAL_MAIN -> {
                event.setCancelled(true);
                handleMain(player, event.getRawSlot());
            }
            case PORTAL_CREATE -> {
                event.setCancelled(true);
                handleCreate(player, event.getRawSlot());
            }
            case PORTAL_LIST -> {
                event.setCancelled(true);
                handleList(player, event);
            }
            case PORTAL_MANAGE -> {
                event.setCancelled(true);
                handlePortal(player, event.getRawSlot());
            }
            default -> { }
        }
    }

    private void handleMain(Player player, int slot) {
        var cfg = plugin.getConfigManager();
        UUID uuid = player.getUniqueId();

        if (slot == PortalMenu.SLOT_CREATE) {
            if (noPerm(player, cfg.permPortalCreate)) return;
            PortalMenu.openCreate(plugin, player, frameA.get(uuid), frameB.get(uuid));
        } else if (slot == PortalMenu.SLOT_LIST) {
            if (noPerm(player, cfg.permPortalUse)) return;
            List<String> names = plugin.getPortalStorage().getPortalsByOwner(uuid);
            PortalMenu.openList(plugin, player, names);
        } else if (slot == PortalMenu.SLOT_CLOSE) {
            player.closeInventory();
        }
    }

    private void handleCreate(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        var lang = plugin.getLangManager();

        if (slot == PortalMenu.SLOT_SELECT_A) {
            awaitingFrame.put(uuid, 1);
            player.closeInventory();
            feedback(player, lang.get("portal-click-frame"));
        } else if (slot == PortalMenu.SLOT_SELECT_B) {
            awaitingFrame.put(uuid, 2);
            player.closeInventory();
            feedback(player, lang.get("portal-click-frame"));
        } else if (slot == PortalMenu.SLOT_CONFIRM) {
            if (frameA.get(uuid) == null || frameB.get(uuid) == null) {
                feedback(player, lang.get("portal-frames-need-two"));
                return;
            }
            player.closeInventory();
            feedback(player, lang.get("portal-enter-name"));
            awaitingColorFor.put(uuid, "NAME");
        } else if (slot == PortalMenu.SLOT_BACK_CREATE) {
            MainMenu.open(plugin, player);
        }
    }

    private void handleList(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        if (event.getRawSlot() == 49) {
            MainMenu.open(plugin, player);
            return;
        }

        Component dn = clicked.getItemMeta().displayName();
        if (dn == null) return;
        String name = PlainTextComponentSerializer.plainText().serialize(dn);

        PortalStorage.Portal portal = plugin.getPortalStorage().getPortal(name);
        if (portal == null) return;
        if (!portal.getOwner().equals(player.getUniqueId()) && !hasAdmin(player)) {
            feedback(player, plugin.getLangManager().get("not-your-zone"));
            return;
        }

        openPortal.put(player.getUniqueId(), name);
        PortalMenu.openPortal(plugin, player);
    }

    private void handlePortal(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        String name = openPortal.get(uuid);
        if (name == null) return;

        PortalStorage.Portal portal = plugin.getPortalStorage().getPortal(name);
        if (portal == null) return;

        var lang = plugin.getLangManager();

        if (slot == PortalMenu.SLOT_SHOW) {
            player.closeInventory();
            plugin.getPortalManager().renderBorders(portal);
        } else if (slot == PortalMenu.SLOT_DELETE) {
            if (noPerm(player, plugin.getConfigManager().permPortalDelete)) return;
            plugin.getPortalStorage().deletePortal(name);
            player.closeInventory();
            feedback(player, lang.get("portal-deleted", "portal", name));
            openPortal.remove(uuid);
        } else if (slot == PortalMenu.SLOT_BACK_PORTAL) {
            List<String> names = plugin.getPortalStorage().getPortalsByOwner(uuid);
            PortalMenu.openList(plugin, player, names);
        }
    }

    // ============================================================
    //             Клик по блоку рамки (рукой)
    // ============================================================

    @EventHandler
    public void onFrameClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!awaitingFrame.containsKey(uuid)) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        event.setCancelled(true);

        Block clicked = event.getClickedBlock();
        var lang = plugin.getLangManager();

        int which = awaitingFrame.remove(uuid);

        plugin.runTaskAt(clicked.getLocation(), () -> {
            PortalStorage.Zone zone = PortalStorage.detectFrame(plugin, clicked.getLocation());
            if (zone == null) {
                player.sendMessage(lang.get("portal-frame-not-found"));
                return;
            }

            if (which == 1) {
                frameA.put(uuid, zone);
            } else {
                frameB.put(uuid, zone);
            }

            player.sendMessage(lang.get("portal-frame-found",
                    "size", zone.width() + "x" + zone.height()));

            PortalMenu.openCreate(plugin, player, frameA.get(uuid), frameB.get(uuid));
        });
    }

    // ============================================================
    //                  Ввод имени и HEX-цвета в чат
    // ============================================================

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        String state = awaitingColorFor.get(uuid);
        if (state == null) return;

        event.setCancelled(true);

        String message = PlainTextComponentSerializer.plainText()
                .serialize(event.message())
                .trim();
        var lang = plugin.getLangManager();

        if (state.equals("NAME")) {
            if (message.isEmpty()) {
                feedback(player, lang.get("zone-name-empty"));
                return;
            }
            if (plugin.getPortalStorage().getPortal(message) != null) {
                feedback(player, lang.get("portal-name-taken", "portal", message));
                return;
            }
            pendingName.put(uuid, message);
            awaitingColorFor.put(uuid, "COLOR");
            feedback(player, lang.get("portal-enter-color"));
            return;
        }

        if (state.equals("COLOR")) {
            if (!message.matches("#?[0-9a-fA-F]{6}")) {
                feedback(player, lang.get("portal-color-invalid"));
                return;
            }
            String hex = message.startsWith("#") ? message : "#" + message;

            String name = pendingName.remove(uuid);
            awaitingColorFor.remove(uuid);
            if (name == null) return;

            PortalStorage.Zone a = frameA.get(uuid);
            PortalStorage.Zone b = frameB.get(uuid);
            if (a == null || b == null) {
                feedback(player, lang.get("portal-frames-need-two"));
                return;
            }

            if (!a.getWorld().equals(b.getWorld())) {
                feedback(player, lang.get("portal-world-mismatch"));
                return;
            }

            if (a.overlaps(b)) {
                feedback(player, lang.get("portal-overlap"));
                return;
            }

            plugin.runTaskAt(a.getMin(), () -> {
                plugin.getPortalStorage().createPortal(name, a, b, uuid, hex);
                player.sendMessage(lang.get("portal-created", "portal", name));
                frameA.remove(uuid);
                frameB.remove(uuid);
            });
        }
    }

    // ============================================================
    //                          Утилиты
    // ============================================================

    private void feedback(Player player, String msg) {
        if (plugin.getConfigManager().useActionbarForFeedback) {
            player.sendActionBar(Component.text(msg));
        } else {
            player.sendMessage(msg);
        }
    }

    private boolean noPerm(Player player, String perm) {
        var cfg = plugin.getConfigManager();
        if (!cfg.permissionsEnabled) return false;
        if (player.hasPermission(perm) || player.hasPermission(cfg.permAdmin)) return false;
        feedback(player, plugin.getLangManager().get("no-permission"));
        return true;
    }

    private boolean hasAdmin(Player player) {
        var cfg = plugin.getConfigManager();
        return !cfg.permissionsEnabled || player.hasPermission(cfg.permAdmin);
    }
}