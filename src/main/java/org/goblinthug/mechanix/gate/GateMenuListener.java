package org.goblinthug.mechanix.gate;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.goblinthug.mechanix.MechaniX;
import org.goblinthug.mechanix.menu.MechaniXHolder;
import org.goblinthug.mechanix.menu.MenuType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GateMenuListener implements Listener {

    private final MechaniX plugin;

    private final Map<UUID, Integer> awaitingPoint = new HashMap<>();
    private final Map<UUID, Boolean> awaitingName = new HashMap<>();
    private final Map<UUID, String> awaitingBind = new HashMap<>();
    private final Map<UUID, String> awaitingUnbind = new HashMap<>();
    private final Map<UUID, Location> pos1Map = new HashMap<>();
    private final Map<UUID, Location> pos2Map = new HashMap<>();
    private final Map<UUID, String> openZone = new HashMap<>();

    public GateMenuListener(MechaniX plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MechaniXHolder holder)) return;

        MenuType type = holder.getType();
        switch (type) {
            case GATE_MAIN -> {
                event.setCancelled(true);
                handleMain(player, event.getRawSlot());
            }
            case GATE_CREATE -> {
                event.setCancelled(true);
                handleCreate(player, event.getRawSlot());
            }
            case GATE_LIST -> {
                event.setCancelled(true);
                handleList(player, event);
            }
            case GATE_ZONE -> {
                event.setCancelled(true);
                handleZone(player, event.getRawSlot());
            }
            default -> { }
        }
    }

    private void handleMain(Player player, int slot) {
        if (slot == GateMenu.SLOT_CREATE) {
            if (noPerm(player, plugin.getConfigManager().permCreate)) return;
            GateMenu.openCreate(plugin, player);
        } else if (slot == GateMenu.SLOT_LIST) {
            if (noPerm(player, plugin.getConfigManager().permUse)) return;
            List<String> zones = plugin.getGateStorage().getZonesByOwner(player.getUniqueId());
            GateMenu.openZoneList(plugin, player, zones);
        } else if (slot == GateMenu.SLOT_CLOSE) {
            player.closeInventory();
        }
    }

    private void handleCreate(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        var lang = plugin.getLangManager();

        if (slot == GateMenu.SLOT_POS1) {
            awaitingPoint.put(uuid, 1);
            player.closeInventory();
            feedback(player, lang.get("click-pos1"));
        } else if (slot == GateMenu.SLOT_POS2) {
            awaitingPoint.put(uuid, 2);
            player.closeInventory();
            feedback(player, lang.get("click-pos2"));
        } else if (slot == GateMenu.SLOT_CONFIRM) {
            if (!pos1Map.containsKey(uuid) || !pos2Map.containsKey(uuid)) {
                feedback(player, lang.get("points-missing"));
                return;
            }
            awaitingName.put(uuid, true);
            player.closeInventory();
            feedback(player, lang.get("enter-name"));
        } else if (slot == GateMenu.SLOT_BACK_CREATE) {
            org.goblinthug.mechanix.menu.MainMenu.open(plugin, player);
        }
    }

    private void handleList(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        int slot = event.getRawSlot();
        if (slot == 49) {
            GateMenu.openMain(plugin, player);
            return;
        }

        Component displayName = clicked.getItemMeta().displayName();
        if (displayName == null) return;
        String name = PlainTextComponentSerializer.plainText().serialize(displayName);

        GateStorage.Zone zone = plugin.getGateStorage().getZone(name);
        if (zone == null) return;
        if (!zone.getOwner().equals(player.getUniqueId()) && !hasAdmin(player)) {
            feedback(player, plugin.getLangManager().get("not-your-zone"));
            return;
        }

        openZone.put(player.getUniqueId(), name);
        GateMenu.openZone(plugin, player);
    }

    private void handleZone(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        String zoneName = openZone.get(uuid);
        if (zoneName == null) return;

        GateStorage.Zone zone = plugin.getGateStorage().getZone(zoneName);
        if (zone == null) return;

        var lang = plugin.getLangManager();

        if (slot == GateMenu.SLOT_BIND) {
            if (noPerm(player, plugin.getConfigManager().permBind)) return;
            awaitingBind.put(uuid, zoneName);
            player.closeInventory();
            feedback(player, lang.get("bind-click"));
        } else if (slot == GateMenu.SLOT_UNBIND) {
            if (noPerm(player, plugin.getConfigManager().permBind)) return;
            awaitingUnbind.put(uuid, zoneName);
            player.closeInventory();
            feedback(player, lang.get("unbind-click"));
        } else if (slot == GateMenu.SLOT_SHOW) {
            player.closeInventory();
            showZone(player, zone);
        } else if (slot == GateMenu.SLOT_DELETE) {
            if (noPerm(player, plugin.getConfigManager().permDelete)) return;
            plugin.getGateManager().forceCloseByZone(zoneName);
            plugin.getGateStorage().deleteZone(zoneName);
            player.closeInventory();
            feedback(player, lang.get("zone-deleted", "zone", zoneName));
            openZone.remove(uuid);
        } else if (slot == GateMenu.SLOT_BACK_ZONE) {
            org.goblinthug.mechanix.menu.MainMenu.open(plugin, player);
        }
    }

    @EventHandler
    public void onPointClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!awaitingPoint.containsKey(uuid)) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        event.setCancelled(true);

        Location loc = event.getClickedBlock().getLocation();
        int which = awaitingPoint.remove(uuid);
        var lang = plugin.getLangManager();

        if (which == 1) {
            pos1Map.put(uuid, loc);
            feedback(player, lang.get("point1-set",
                    "x", loc.getBlockX(),
                    "y", loc.getBlockY(),
                    "z", loc.getBlockZ()));
        } else {
            pos2Map.put(uuid, loc);
            feedback(player, lang.get("point2-set",
                    "x", loc.getBlockX(),
                    "y", loc.getBlockY(),
                    "z", loc.getBlockZ()));
        }

        GateMenu.openCreate(plugin, player);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!awaitingName.containsKey(uuid)) return;

        event.setCancelled(true);

        String name = PlainTextComponentSerializer.plainText()
                .serialize(event.message())
                .trim();

        var lang = plugin.getLangManager();
        var cfg = plugin.getConfigManager();

        if (name.isEmpty()) {
            feedback(player, lang.get("zone-name-empty"));
            return;
        }

        Location p1 = pos1Map.get(uuid);
        Location p2 = pos2Map.get(uuid);

        if (p1 == null || p2 == null) {
            feedback(player, lang.get("points-lost"));
            awaitingName.remove(uuid);
            return;
        }

        if (plugin.getGateStorage().getZone(name) != null) {
            feedback(player, lang.get("zone-name-taken", "zone", name));
            return;
        }

        int owned = plugin.getGateStorage().getZonesByOwner(uuid).size();
        boolean bypass = cfg.adminBypassLimit && hasAdmin(player);
        if (!bypass && owned >= cfg.defaultMaxZones) {
            feedback(player, lang.get("zone-limit-reached", "limit", cfg.defaultMaxZones));
            return;
        }

        int sx = Math.abs(p1.getBlockX() - p2.getBlockX()) + 1;
        int sy = Math.abs(p1.getBlockY() - p2.getBlockY()) + 1;
        int sz = Math.abs(p1.getBlockZ() - p2.getBlockZ()) + 1;

        if (sx > cfg.maxZoneSide || sy > cfg.maxZoneSide || sz > cfg.maxZoneSide) {
            feedback(player, lang.get("zone-side-too-big", "max", cfg.maxZoneSide));
            return;
        }

        long volume = (long) sx * sy * sz;
        if (volume > cfg.maxZoneVolume) {
            feedback(player, lang.get("zone-too-big", "max", cfg.maxZoneVolume));
            return;
        }

        plugin.runTaskAt(p1, () -> {
            if (!isZoneFilled(p1, p2)) {
                player.sendMessage(lang.get("zone-has-air"));
                return;
            }

            plugin.getGateStorage().createZone(name, p1, p2, uuid);
            player.sendMessage(lang.get("zone-created", "zone", name));

            awaitingName.remove(uuid);
            pos1Map.remove(uuid);
            pos2Map.remove(uuid);
        });
    }

    private boolean isZoneFilled(Location p1, Location p2) {
        if (!p1.getWorld().equals(p2.getWorld())) return false;

        int x1 = Math.min(p1.getBlockX(), p2.getBlockX());
        int y1 = Math.min(p1.getBlockY(), p2.getBlockY());
        int z1 = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int x2 = Math.max(p1.getBlockX(), p2.getBlockX());
        int y2 = Math.max(p1.getBlockY(), p2.getBlockY());
        int z2 = Math.max(p1.getBlockZ(), p2.getBlockZ());

        var blacklist = plugin.getConfigManager().blacklistedBlocks;

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    Block b = p1.getWorld().getBlockAt(x, y, z);
                    if (b.isEmpty() || b.isLiquid()) return false;
                    if (blacklist.contains(b.getType())) return false;
                }
            }
        }
        return true;
    }

    @EventHandler
    public void onBindOrUnbindClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        boolean binding = awaitingBind.containsKey(uuid);
        boolean unbinding = awaitingUnbind.containsKey(uuid);
        if (!binding && !unbinding) return;

        boolean isRightClick = event.getAction() == Action.RIGHT_CLICK_BLOCK;
        boolean isPhysical = event.getAction() == Action.PHYSICAL;
        if (!isRightClick && !isPhysical) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Material type = block.getType();
        boolean valid = type.name().endsWith("_BUTTON")
                || type == Material.LEVER
                || type.name().endsWith("_PRESSURE_PLATE");

        var lang = plugin.getLangManager();

        if (!valid) {
            feedback(player, lang.get("bind-not-trigger"));
            return;
        }

        event.setCancelled(true);

        if (binding) {
            String zoneName = awaitingBind.remove(uuid);

            String existing = plugin.getGateStorage().getBoundZone(block.getLocation());
            if (existing != null) {
                feedback(player, lang.get("bind-already", "zone", existing));
                return;
            }

            plugin.getGateStorage().bindBlock(block.getLocation(), zoneName);
            feedback(player, lang.get("bind-success", "zone", zoneName));
        } else {
            awaitingUnbind.remove(uuid);

            String existing = plugin.getGateStorage().getBoundZone(block.getLocation());
            if (existing == null) {
                feedback(player, lang.get("unbind-not-bound"));
                return;
            }

            plugin.getGateStorage().unbindBlock(block.getLocation());
            feedback(player, lang.get("unbind-success", "zone", existing));
        }
    }

    private void showZone(Player player, GateStorage.Zone zone) {
        var cfg = plugin.getConfigManager();
        World world = zone.getWorld();

        if (!player.getWorld().equals(world)) {
            player.sendMessage(plugin.getLangManager().get("zone-not-found", "zone", "world"));
            return;
        }

        Location min = zone.getMin();
        Location max = zone.getMax();

        double x1 = min.getBlockX();
        double y1 = min.getBlockY();
        double z1 = min.getBlockZ();
        double x2 = max.getBlockX() + 1;
        double y2 = max.getBlockY() + 1;
        double z2 = max.getBlockZ() + 1;

        double[][][] edges = {
                {{x1, y1, z1}, {x2, y1, z1}},
                {{x2, y1, z1}, {x2, y1, z2}},
                {{x2, y1, z2}, {x1, y1, z2}},
                {{x1, y1, z2}, {x1, y1, z1}},
                {{x1, y2, z1}, {x2, y2, z1}},
                {{x2, y2, z1}, {x2, y2, z2}},
                {{x2, y2, z2}, {x1, y2, z2}},
                {{x1, y2, z2}, {x1, y2, z1}},
                {{x1, y1, z1}, {x1, y2, z1}},
                {{x2, y1, z1}, {x2, y2, z1}},
                {{x2, y1, z2}, {x2, y2, z2}},
                {{x1, y1, z2}, {x1, y2, z2}},
        };

        Object particleData = null;
        if (cfg.borderParticle == Particle.DUST) {
            try {
                String hex = cfg.borderColorHex.startsWith("#")
                        ? cfg.borderColorHex
                        : "#" + cfg.borderColorHex;
                java.awt.Color c = java.awt.Color.decode(hex);
                particleData = new Particle.DustOptions(
                        Color.fromRGB(c.getRed(), c.getGreen(), c.getBlue()), 1.5f);
            } catch (NumberFormatException ignored) {
                particleData = new Particle.DustOptions(Color.RED, 1.5f);
            }
        }

        final Object data = particleData;
        Location regionAnchor = min.clone();

        int duration = Math.max(cfg.borderDurationTicks, 1);
        int period = Math.max(cfg.borderPeriodTicks, 1);

        Runnable draw = () -> {
            for (double[][] edge : edges) {
                double[] a = edge[0];
                double[] b = edge[1];

                double dx = b[0] - a[0];
                double dy = b[1] - a[1];
                double dz = b[2] - a[2];
                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);

                int points = Math.max(1, (int) (length / cfg.borderStep));

                for (int i = 0; i <= points; i++) {
                    double t = i / (double) points;
                    double px = a[0] + dx * t;
                    double py = a[1] + dy * t;
                    double pz = a[2] + dz * t;

                    Location loc = new Location(world, px, py, pz);
                    if (data != null) {
                        world.spawnParticle(cfg.borderParticle, loc, 1, 0, 0, 0, 0, data);
                    } else {
                        world.spawnParticle(cfg.borderParticle, loc, 1);
                    }
                }
            }
        };

        for (int tick = 0; tick < duration; tick += period) {
            if (tick == 0) {
                plugin.runTaskAt(regionAnchor, draw);
            } else {
                final long delay = tick;
                plugin.runTaskAtLater(regionAnchor, draw, delay);
            }
        }
    }

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