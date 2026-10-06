package org.goblinthug.mechanix.portal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.goblinthug.mechanix.MechaniX;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PortalStorage {

    private final MechaniX plugin;
    private final Map<String, Portal> portals = new ConcurrentHashMap<>();
    private final File file;
    private FileConfiguration config;

    public PortalStorage(MechaniX plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "portals.yml");

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create portals.yml: " + e.getMessage());
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
        loadAll();
    }

    public void createPortal(String name, Zone frameA, Zone frameB, UUID owner, String colorHex) {
        Portal portal = new Portal(frameA, frameB, owner, colorHex);
        portals.put(name.toLowerCase(), portal);
        savePortal(name.toLowerCase(), portal);
    }

    public Portal getPortal(String name) {
        return portals.get(name.toLowerCase());
    }

    public List<String> getPortalNames() {
        return new ArrayList<>(portals.keySet());
    }

    public void deletePortal(String name) {
        String key = name.toLowerCase();
        portals.remove(key);
        config.set("portals." + key, null);
        save();
    }

    public List<String> getPortalsByOwner(UUID owner) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Portal> e : portals.entrySet()) {
            if (e.getValue().getOwner().equals(owner)) result.add(e.getKey());
        }
        return result;
    }

    private void loadAll() {
        ConfigurationSection sec = config.getConfigurationSection("portals");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            String worldName = config.getString("portals." + key + ".world");
            if (worldName == null) continue;
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            Zone frameA = readZone(world, "portals." + key + ".frameA");
            Zone frameB = readZone(world, "portals." + key + ".frameB");
            if (frameA == null || frameB == null) continue;

            String ownerStr = config.getString("portals." + key + ".owner");
            UUID owner;
            try {
                owner = ownerStr != null ? UUID.fromString(ownerStr) : new UUID(0, 0);
            } catch (IllegalArgumentException e) {
                owner = new UUID(0, 0);
            }

            String colorHex = config.getString("portals." + key + ".color", "#FF55FF");

            portals.put(key, new Portal(frameA, frameB, owner, colorHex));
        }

        plugin.getLogger().info("Loaded portals: " + portals.size());
    }

    private Zone readZone(World world, String path) {
        if (!config.contains(path + ".x1")) return null;
        int x1 = config.getInt(path + ".x1");
        int y1 = config.getInt(path + ".y1");
        int z1 = config.getInt(path + ".z1");
        int x2 = config.getInt(path + ".x2");
        int y2 = config.getInt(path + ".y2");
        int z2 = config.getInt(path + ".z2");
        return new Zone(new Location(world, x1, y1, z1), new Location(world, x2, y2, z2));
    }

    private void savePortal(String name, Portal portal) {
        String path = "portals." + name + ".";
        config.set(path + "world", portal.getFrameA().getWorld().getName());
        writeZone(path + "frameA.", portal.getFrameA());
        writeZone(path + "frameB.", portal.getFrameB());
        config.set(path + "owner", portal.getOwner().toString());
        config.set(path + "color", portal.getColorHex());
        save();
    }

    private void writeZone(String path, Zone zone) {
        config.set(path + "x1", zone.getMin().getBlockX());
        config.set(path + "y1", zone.getMin().getBlockY());
        config.set(path + "z1", zone.getMin().getBlockZ());
        config.set(path + "x2", zone.getMax().getBlockX());
        config.set(path + "y2", zone.getMax().getBlockY());
        config.set(path + "z2", zone.getMax().getBlockZ());
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save portals.yml: " + e.getMessage());
        }
    }

    // ============================================================
    //                    Zone (плоскость внутри рамки)
    // ============================================================

    public static class Zone {
        private final Location min;
        private final Location max;
        private final World world;
        private final Axis normal;

        public Zone(Location p1, Location p2) {
            this.world = p1.getWorld();
            int x1 = Math.min(p1.getBlockX(), p2.getBlockX());
            int y1 = Math.min(p1.getBlockY(), p2.getBlockY());
            int z1 = Math.min(p1.getBlockZ(), p2.getBlockZ());
            int x2 = Math.max(p1.getBlockX(), p2.getBlockX());
            int y2 = Math.max(p1.getBlockY(), p2.getBlockY());
            int z2 = Math.max(p1.getBlockZ(), p2.getBlockZ());

            int dx = x2 - x1;
            int dy = y2 - y1;
            int dz = z2 - z1;

            if (dx == 0) normal = Axis.X;
            else if (dy == 0) normal = Axis.Y;
            else if (dz == 0) normal = Axis.Z;
            else normal = Axis.Y;

            this.min = new Location(world, x1, y1, z1);
            this.max = new Location(world, x2, y2, z2);
        }

        public boolean contains(Location loc) {
            if (loc.getWorld() == null || !loc.getWorld().equals(world)) return false;
            return loc.getBlockX() >= min.getBlockX() && loc.getBlockX() <= max.getBlockX()
                    && loc.getBlockY() >= min.getBlockY() && loc.getBlockY() <= max.getBlockY()
                    && loc.getBlockZ() >= min.getBlockZ() && loc.getBlockZ() <= max.getBlockZ();
        }

        public boolean overlaps(Zone other) {
            if (!world.equals(other.world)) return false;
            return min.getBlockX() <= other.max.getBlockX() && max.getBlockX() >= other.min.getBlockX()
                    && min.getBlockY() <= other.max.getBlockY() && max.getBlockY() >= other.min.getBlockY()
                    && min.getBlockZ() <= other.max.getBlockZ() && max.getBlockZ() >= other.min.getBlockZ();
        }

        public Location center() {
            double cx = (min.getBlockX() + max.getBlockX()) / 2.0 + 0.5;
            double cy = min.getBlockY();
            double cz = (min.getBlockZ() + max.getBlockZ()) / 2.0 + 0.5;
            return new Location(world, cx, cy, cz);
        }

        public World getWorld() { return world; }
        public Location getMin() { return min; }
        public Location getMax() { return max; }
        public Axis getNormal() { return normal; }

        public int width() {
            return switch (normal) {
                case X -> max.getBlockZ() - min.getBlockZ() + 1;
                case Y, Z -> max.getBlockX() - min.getBlockX() + 1;
            };
        }

        public int height() {
            return switch (normal) {
                case X, Z -> max.getBlockY() - min.getBlockY() + 1;
                case Y -> max.getBlockZ() - min.getBlockZ() + 1;
            };
        }
    }

    public enum Axis { X, Y, Z }

    public static class Portal {
        private final Zone frameA;
        private final Zone frameB;
        private final UUID owner;
        private final String colorHex;

        public Portal(Zone frameA, Zone frameB, UUID owner, String colorHex) {
            this.frameA = frameA;
            this.frameB = frameB;
            this.owner = owner;
            this.colorHex = colorHex;
        }

        public Zone getFrameA() { return frameA; }
        public Zone getFrameB() { return frameB; }
        public UUID getOwner() { return owner; }
        public String getColorHex() { return colorHex; }
    }

    // ============================================================
    //                    Frame detection
    // ============================================================

    /**
     * Определяет рамку портала по кликнутому блоку.
     * Возвращает Zone (внутренняя воздушная область) либо null.
     *
     * Вертикальные оси (X, Z) проверяются раньше горизонтальной (Y),
     * чтобы при клике по вертикальной рамке не находилась "яма" под ней.
     * Горизонтальные порталы можно отключить через
     * config.yml: portal.allow-horizontal (по умолчанию false).
     */
    public static Zone detectFrame(MechaniX plugin, Location clicked) {
        World world = clicked.getWorld();
        if (world == null) return null;

        int cx = clicked.getBlockX();
        int cy = clicked.getBlockY();
        int cz = clicked.getBlockZ();

        if (!isFrameMaterial(world, cx, cy, cz)) return null;

        boolean allowHorizontal = plugin.getConfigManager().portalAllowHorizontal;

        Axis[] order = allowHorizontal
                ? new Axis[]{Axis.X, Axis.Z, Axis.Y}
                : new Axis[]{Axis.X, Axis.Z};

        for (Axis normal : order) {
            List<int[]> perpDirs = planeDirections(normal);
            for (int[] d : perpDirs) {
                int nx = cx + d[0];
                int ny = cy + d[1];
                int nz = cz + d[2];

                if (!world.getBlockAt(nx, ny, nz).getType().isAir()) continue;

                Zone zone = floodInterior(plugin, world, nx, ny, nz, normal);
                if (zone == null) continue;
                if (!clickedIsFrameOfZone(cx, cy, cz, zone, world)) continue;
                return zone;
            }
        }
        return null;
    }

    private static int[] normalVector(Axis axis) {
        return switch (axis) {
            case X -> new int[]{1, 0, 0};
            case Y -> new int[]{0, 1, 0};
            case Z -> new int[]{0, 0, 1};
        };
    }

    /**
     * BFS по воздуху в плоскости normal. Возвращает Zone, если область
     * замкнута рамкой и имеет допустимые размеры.
     */
    private static Zone floodInterior(MechaniX plugin, World world,
                                      int sx, int sy, int sz, Axis normal) {
        Set<Long> visited = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{sx, sy, sz});
        visited.add(pack(sx, sy, sz));

        int minX = sx, maxX = sx;
        int minY = sy, maxY = sy;
        int minZ = sz, maxZ = sz;

        List<int[]> dirs = planeDirections(normal);
        int maxSide = plugin.getConfigManager().portalMaxSide;
        int cap = (maxSide + 2) * (maxSide + 2);

        while (!queue.isEmpty()) {
            if (visited.size() > cap) return null;

            int[] cur = queue.poll();
            for (int[] d : dirs) {
                int nx = cur[0] + d[0];
                int ny = cur[1] + d[1];
                int nz = cur[2] + d[2];
                long key = pack(nx, ny, nz);
                if (visited.contains(key)) continue;
                if (!world.getBlockAt(nx, ny, nz).getType().isAir()) continue;

                visited.add(key);
                queue.add(new int[]{nx, ny, nz});

                if (nx < minX) minX = nx;
                if (nx > maxX) maxX = nx;
                if (ny < minY) minY = ny;
                if (ny > maxY) maxY = ny;
                if (nz < minZ) minZ = nz;
                if (nz > maxZ) maxZ = nz;
            }
        }

        switch (normal) {
            case X -> { if (minX != maxX) return null; }
            case Y -> { if (minY != maxY) return null; }
            case Z -> { if (minZ != maxZ) return null; }
        }

        int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume != visited.size()) return null;

        int w, h;
        switch (normal) {
            case X -> { w = maxZ - minZ + 1; h = maxY - minY + 1; }
            case Y -> { w = maxX - minX + 1; h = maxZ - minZ + 1; }
            case Z -> { w = maxX - minX + 1; h = maxY - minY + 1; }
            default -> { return null; }
        }

        int minSide = plugin.getConfigManager().portalMinSide;
        if (w < minSide || h < minSide) return null;
        if (w > maxSide || h > maxSide) return null;

        if (!interiorPerimeterIsFrame(world, normal, minX, minY, minZ, maxX, maxY, maxZ)) {
            return null;
        }

        return new Zone(
                new Location(world, minX, minY, minZ),
                new Location(world, maxX, maxY, maxZ));
    }

    /**
     * Проверяет, что кольцо вокруг внутренней области (расширенное на 1 блок
     * в плоскости) целиком состоит из материала рамки.
     */
    private static boolean interiorPerimeterIsFrame(World world, Axis normal,
                                                    int minX, int minY, int minZ,
                                                    int maxX, int maxY, int maxZ) {
        switch (normal) {
            case X -> {
                int x = minX;
                for (int y = minY - 1; y <= maxY + 1; y++) {
                    for (int z = minZ - 1; z <= maxZ + 1; z++) {
                        boolean edge = (y == minY - 1 || y == maxY + 1
                                || z == minZ - 1 || z == maxZ + 1);
                        if (edge && !isFrameMaterial(world, x, y, z)) return false;
                    }
                }
            }
            case Y -> {
                int y = minY;
                for (int x = minX - 1; x <= maxX + 1; x++) {
                    for (int z = minZ - 1; z <= maxZ + 1; z++) {
                        boolean edge = (x == minX - 1 || x == maxX + 1
                                || z == minZ - 1 || z == maxZ + 1);
                        if (edge && !isFrameMaterial(world, x, y, z)) return false;
                    }
                }
            }
            case Z -> {
                int z = minZ;
                for (int x = minX - 1; x <= maxX + 1; x++) {
                    for (int y = minY - 1; y <= maxY + 1; y++) {
                        boolean edge = (x == minX - 1 || x == maxX + 1
                                || y == minY - 1 || y == maxY + 1);
                        if (edge && !isFrameMaterial(world, x, y, z)) return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Проверяет, что кликнутый блок принадлежит рамке найденной зоны.
     * Учитывает возможную толщину рамки.
     */
    private static boolean clickedIsFrameOfZone(int cx, int cy, int cz,
                                                Zone zone, World world) {
        int zx1 = zone.getMin().getBlockX();
        int zy1 = zone.getMin().getBlockY();
        int zz1 = zone.getMin().getBlockZ();
        int zx2 = zone.getMax().getBlockX();
        int zy2 = zone.getMax().getBlockY();
        int zz2 = zone.getMax().getBlockZ();

        int planeCoord, clickCoord;
        switch (zone.getNormal()) {
            case X -> { planeCoord = zx1; clickCoord = cx; }
            case Y -> { planeCoord = zy1; clickCoord = cy; }
            case Z -> { planeCoord = zz1; clickCoord = cz; }
            default -> { return false; }
        }

        if (planeCoord != clickCoord) {
            int step = planeCoord > clickCoord ? 1 : -1;
            int p = clickCoord + step;
            int safety = 0;
            while (safety++ < 16) {
                int bx = cx, by = cy, bz = cz;
                switch (zone.getNormal()) {
                    case X -> bx = p;
                    case Y -> by = p;
                    case Z -> bz = p;
                }
                if (!isFrameMaterial(world, bx, by, bz)) return false;
                if (p == planeCoord) break;
                p += step;
            }
        }

        switch (zone.getNormal()) {
            case X -> {
                boolean inRange = cy >= zy1 - 1 && cy <= zy2 + 1
                        && cz >= zz1 - 1 && cz <= zz2 + 1;
                if (!inRange) return false;
                boolean inInterior = cy >= zy1 && cy <= zy2
                        && cz >= zz1 && cz <= zz2;
                return !inInterior;
            }
            case Y -> {
                boolean inRange = cx >= zx1 - 1 && cx <= zx2 + 1
                        && cz >= zz1 - 1 && cz <= zz2 + 1;
                if (!inRange) return false;
                boolean inInterior = cx >= zx1 && cx <= zx2
                        && cz >= zz1 && cz <= zz2;
                return !inInterior;
            }
            case Z -> {
                boolean inRange = cx >= zx1 - 1 && cx <= zx2 + 1
                        && cy >= zy1 - 1 && cy <= zy2 + 1;
                if (!inRange) return false;
                boolean inInterior = cx >= zx1 && cx <= zx2
                        && cy >= zy1 && cy <= zy2;
                return !inInterior;
            }
        }
        return false;
    }

    private static List<int[]> planeDirections(Axis normal) {
        List<int[]> dirs = new ArrayList<>();
        switch (normal) {
            case X -> {
                dirs.add(new int[]{0, 1, 0});
                dirs.add(new int[]{0, -1, 0});
                dirs.add(new int[]{0, 0, 1});
                dirs.add(new int[]{0, 0, -1});
            }
            case Y -> {
                dirs.add(new int[]{1, 0, 0});
                dirs.add(new int[]{-1, 0, 0});
                dirs.add(new int[]{0, 0, 1});
                dirs.add(new int[]{0, 0, -1});
            }
            case Z -> {
                dirs.add(new int[]{1, 0, 0});
                dirs.add(new int[]{-1, 0, 0});
                dirs.add(new int[]{0, 1, 0});
                dirs.add(new int[]{0, -1, 0});
            }
        }
        return dirs;
    }

    private static boolean isFrameMaterial(World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        var type = block.getType();
        if (type.isAir()) return false;
        if (!type.isSolid()) return false;
        if (!type.isOccluding()) return false;
        return true;
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38)
                | ((long) (y & 0xFFFF) << 22)
                | (z & 0x3FFFFF);
    }

    private static int[] unpack(long key) {
        int x = (int) (key >> 38);
        int y = (int) ((key >> 22) & 0xFFFF);
        int z = (int) (key & 0x3FFFFF);

        if (x > 0x1FFFFFF) x -= 0x4000000;
        if (y > 0x7FFF) y -= 0x10000;
        if (z > 0x1FFFFF) z -= 0x400000;

        return new int[]{x, y, z};
    }
}