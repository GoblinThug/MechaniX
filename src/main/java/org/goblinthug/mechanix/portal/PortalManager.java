package org.goblinthug.mechanix.portal;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.goblinthug.mechanix.MechaniX;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PortalManager implements Listener {

    private final MechaniX plugin;
    private final Map<UUID, Long> lastTeleport = new HashMap<>();
    private final Map<UUID, String> ignorePortal = new HashMap<>();

    /** Счётчик тиков анимации для эффектов частиц. */
    private long animTick = 0;

    public PortalManager(MechaniX plugin) {
        this.plugin = plugin;
    }

    // ============================================================
    //         Запуск фонового таска — вызывать из onEnable()
    // ============================================================

    public void start() {
        if (!plugin.getConfigManager().portalShowActiveBorders) return;

        int period = Math.max(2, plugin.getConfigManager().portalActiveBorderPeriod);

        Runnable task = () -> {
            for (String name : plugin.getPortalStorage().getPortalNames()) {
                PortalStorage.Portal portal = plugin.getPortalStorage().getPortal(name);
                if (portal == null) continue;
                renderBorders(portal);
            }
        };

        if (MechaniX.isFolia()) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(), 40L, period);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, task, 40L, period);
        }
    }

    // ============================================================
    //                    Телепортация
    // ============================================================

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        var cfg = plugin.getConfigManager();
        if (cfg.permissionsEnabled
                && !player.hasPermission(cfg.permPortalUse)
                && !player.hasPermission(cfg.permAdmin)) {
            return;
        }

        long now = System.currentTimeMillis();
        Long last = lastTeleport.get(uuid);
        if (last != null && now - last < cfg.portalCooldownMs) return;

        String ignore = ignorePortal.get(uuid);
        Location to = event.getTo();

        for (String name : plugin.getPortalStorage().getPortalNames()) {
            if (name.equals(ignore)) continue;
            PortalStorage.Portal portal = plugin.getPortalStorage().getPortal(name);
            if (portal == null) continue;

            if (portal.getFrameA().contains(to)) {
                teleport(player, portal.getFrameB(), name);
                return;
            }
            if (portal.getFrameB().contains(to)) {
                teleport(player, portal.getFrameA(), name);
                return;
            }
        }
    }

    private void teleport(Player player, PortalStorage.Zone destZone, String portalName) {
        Location dest = destZone.center();
        dest.setYaw(player.getLocation().getYaw());
        dest.setPitch(player.getLocation().getPitch());

        UUID uuid = player.getUniqueId();
        lastTeleport.put(uuid, System.currentTimeMillis());
        ignorePortal.put(uuid, portalName);

        plugin.runTaskAt(player.getLocation(), () ->
                player.teleportAsync(dest).thenAccept(ok ->
                        plugin.runTaskAtLater(dest, () -> ignorePortal.remove(uuid), 60L)
                )
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastTeleport.remove(uuid);
        ignorePortal.remove(uuid);
    }

    // ============================================================
    //           Постоянная подсветка активных порталов
    // ============================================================

    public void renderBorders(PortalStorage.Portal portal) {
        animTick++;

        var cfg = plugin.getConfigManager();
        Particle particle = cfg.borderParticle;
        Color color = parseColor(portal.getColorHex());

        // Рамка (обе стороны)
        drawBorder(portal.getFrameA(), particle, color, cfg.borderStep);
        drawBorder(portal.getFrameB(), particle, color, cfg.borderStep);

        // Внутренняя заливка (обе стороны)
        if (cfg.portalFillEnabled) {
            drawFill(portal.getFrameA(), particle, color);
            drawFill(portal.getFrameB(), particle, color);
        }
    }

    // ============================================================
    //                        Рамка
    // ============================================================

    private void drawBorder(PortalStorage.Zone zone, Particle particle,
                            Color color, double step) {
        World world = zone.getWorld();
        if (world == null) return;

        var cfg = plugin.getConfigManager();
        int density = Math.max(1, cfg.portalBorderDensity);
        double spread = Math.max(0.0, cfg.portalSpread);

        double x1 = zone.getMin().getBlockX();
        double y1 = zone.getMin().getBlockY();
        double z1 = zone.getMin().getBlockZ();
        double x2 = zone.getMax().getBlockX() + 1;
        double y2 = zone.getMax().getBlockY() + 1;
        double z2 = zone.getMax().getBlockZ() + 1;

        double[][][] edges = buildEdges(zone, x1, y1, z1, x2, y2, z2);

        Particle.DustOptions dust = particle == Particle.DUST
                ? new Particle.DustOptions(color, 1.5f)
                : null;

        for (double[][] edge : edges) {
            double[] a = edge[0];
            double[] b = edge[1];
            double dx = b[0] - a[0];
            double dy = b[1] - a[1];
            double dz = b[2] - a[2];
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            int points = Math.max(1, (int) (length / Math.max(step, 0.05)));

            for (int i = 0; i <= points; i++) {
                double t = i / (double) points;
                double bx = a[0] + dx * t;
                double by = a[1] + dy * t;
                double bz = a[2] + dz * t;

                for (int k = 0; k < density; k++) {
                    Location loc = new Location(world,
                            bx + rnd(spread),
                            by + rnd(spread),
                            bz + rnd(spread));
                    spawnAt(world, particle, loc, dust);
                }
            }
        }
    }

    // ============================================================
    //                    Внутренняя заливка
    // ============================================================

    private void drawFill(PortalStorage.Zone zone, Particle particle, Color color) {
        World world = zone.getWorld();
        if (world == null) return;

        var cfg = plugin.getConfigManager();
        double fillStep = Math.max(0.25, cfg.portalFillStep);
        int density = Math.max(1, cfg.portalFillDensity);
        double drift = cfg.portalDrift;
        double spread = Math.max(0.0, cfg.portalSpread);

        double minX = zone.getMin().getBlockX();
        double minY = zone.getMin().getBlockY();
        double minZ = zone.getMin().getBlockZ();
        double maxX = zone.getMax().getBlockX() + 1;
        double maxY = zone.getMax().getBlockY() + 1;
        double maxZ = zone.getMax().getBlockZ() + 1;

        Particle.DustOptions dust = particle == Particle.DUST
                ? new Particle.DustOptions(color, 1.0f)
                : null;

        double t = animTick * 0.15;

        switch (zone.getNormal()) {
            case X -> {
                double planeX = minX + 0.5;
                for (double y = minY + 0.5; y < maxY; y += fillStep) {
                    for (double z = minZ + 0.5; z < maxZ; z += fillStep) {
                        double offset = Math.sin(t + y * 0.4 + z * 0.4) * drift;
                        for (int k = 0; k < density; k++) {
                            Location loc = new Location(world,
                                    planeX + offset + rnd(spread),
                                    y + rnd(spread),
                                    z + rnd(spread));
                            spawnAt(world, particle, loc, dust);
                        }
                    }
                }
            }
            case Y -> {
                double planeY = minY + 0.5;
                for (double x = minX + 0.5; x < maxX; x += fillStep) {
                    for (double z = minZ + 0.5; z < maxZ; z += fillStep) {
                        double offset = Math.sin(t + x * 0.4 + z * 0.4) * drift;
                        for (int k = 0; k < density; k++) {
                            Location loc = new Location(world,
                                    x + rnd(spread),
                                    planeY + offset + rnd(spread),
                                    z + rnd(spread));
                            spawnAt(world, particle, loc, dust);
                        }
                    }
                }
            }
            case Z -> {
                double planeZ = minZ + 0.5;
                for (double x = minX + 0.5; x < maxX; x += fillStep) {
                    for (double y = minY + 0.5; y < maxY; y += fillStep) {
                        double offset = Math.sin(t + x * 0.4 + y * 0.4) * drift;
                        for (int k = 0; k < density; k++) {
                            Location loc = new Location(world,
                                    x + rnd(spread),
                                    y + rnd(spread),
                                    planeZ + offset + rnd(spread));
                            spawnAt(world, particle, loc, dust);
                        }
                    }
                }
            }
        }
    }

    // ============================================================
    //                        Геометрия
    // ============================================================

    private double[][][] buildEdges(PortalStorage.Zone zone,
                                    double x1, double y1, double z1,
                                    double x2, double y2, double z2) {
        switch (zone.getNormal()) {
            case X -> {
                return new double[][][]{
                        {{x1, y1, z1}, {x1, y1, z2}},
                        {{x1, y1, z2}, {x1, y2, z2}},
                        {{x1, y2, z2}, {x1, y2, z1}},
                        {{x1, y2, z1}, {x1, y1, z1}},
                        {{x2, y1, z1}, {x2, y1, z2}},
                        {{x2, y1, z2}, {x2, y2, z2}},
                        {{x2, y2, z2}, {x2, y2, z1}},
                        {{x2, y2, z1}, {x2, y1, z1}},
                };
            }
            case Y -> {
                return new double[][][]{
                        {{x1, y1, z1}, {x2, y1, z1}},
                        {{x2, y1, z1}, {x2, y1, z2}},
                        {{x2, y1, z2}, {x1, y1, z2}},
                        {{x1, y1, z2}, {x1, y1, z1}},
                        {{x1, y2, z1}, {x2, y2, z1}},
                        {{x2, y2, z1}, {x2, y2, z2}},
                        {{x2, y2, z2}, {x1, y2, z2}},
                        {{x1, y2, z2}, {x1, y2, z1}},
                };
            }
            case Z -> {
                return new double[][][]{
                        {{x1, y1, z1}, {x2, y1, z1}},
                        {{x2, y1, z1}, {x2, y2, z1}},
                        {{x2, y2, z1}, {x1, y2, z1}},
                        {{x1, y2, z1}, {x1, y1, z1}},
                        {{x1, y1, z2}, {x2, y1, z2}},
                        {{x2, y1, z2}, {x2, y2, z2}},
                        {{x2, y2, z2}, {x1, y2, z2}},
                        {{x1, y2, z2}, {x1, y1, z2}},
                };
            }
        }
        return new double[0][][];
    }

    // ============================================================
    //                        Утилиты
    // ============================================================

    private void spawnAt(World world, Particle particle, Location loc,
                         Particle.DustOptions dust) {
        if (dust != null) {
            world.spawnParticle(particle, loc, 1, 0, 0, 0, 0, dust);
        } else {
            world.spawnParticle(particle, loc, 1);
        }
    }

    private double rnd(double spread) {
        return spread <= 0 ? 0 : (Math.random() - 0.5) * spread;
    }

    private Color parseColor(String hex) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            int rgb = Integer.parseInt(h, 16);
            return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (Exception e) {
            return Color.PURPLE;
        }
    }
}