package org.goblinthug.mechanix.gate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.goblinthug.mechanix.MechaniX;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GateStorage {

    private final MechaniX plugin;
    private final Map<String, Zone> zones = new ConcurrentHashMap<>();
    private final Map<String, String> bindings = new ConcurrentHashMap<>();
    private final File file;
    private FileConfiguration config;

    public GateStorage(MechaniX plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "zones.yml");

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create zones.yml: " + e.getMessage());
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
        loadAll();
    }

    public void createZone(String name, Location p1, Location p2, UUID owner) {
        Zone zone = new Zone(p1, p2, owner);
        zones.put(name.toLowerCase(), zone);
        saveZone(name.toLowerCase(), zone);
    }

    public Zone getZone(String name) {
        return zones.get(name.toLowerCase());
    }

    public List<String> getZoneNames() {
        return new ArrayList<>(zones.keySet());
    }

    public void deleteZone(String name) {
        String key = name.toLowerCase();
        zones.remove(key);
        bindings.entrySet().removeIf(e -> e.getValue().equals(key));
        config.set("zones." + key, null);
        config.set("opened." + key, null);
        save();
    }

    public List<String> getZonesByOwner(UUID owner) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Zone> entry : zones.entrySet()) {
            if (entry.getValue().getOwner().equals(owner)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public void bindBlock(Location blockLoc, String zoneName) {
        String key = blockKey(blockLoc);
        bindings.put(key, zoneName.toLowerCase());
        config.set("bindings." + key, zoneName.toLowerCase());
        save();
    }

    public boolean unbindBlock(Location blockLoc) {
        String key = blockKey(blockLoc);
        if (!bindings.containsKey(key)) return false;
        bindings.remove(key);
        config.set("bindings." + key, null);
        save();
        return true;
    }

    public String getBoundZone(Location blockLoc) {
        return bindings.get(blockKey(blockLoc));
    }

    public void saveOpenedGate(String zoneName, List<String> serialized) {
        config.set("opened." + zoneName.toLowerCase(), serialized);
        save();
    }

    public List<String> loadOpenedGate(String zoneName) {
        return config.getStringList("opened." + zoneName.toLowerCase());
    }

    public void removeOpenedGate(String zoneName) {
        config.set("opened." + zoneName.toLowerCase(), null);
        save();
    }

    public Map<String, List<String>> getAllOpened() {
        Map<String, List<String>> result = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("opened");
        if (section == null) return result;
        for (String key : section.getKeys(false)) {
            result.put(key, config.getStringList("opened." + key));
        }
        return result;
    }

    private String blockKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private void loadAll() {
        ConfigurationSection zoneSection = config.getConfigurationSection("zones");
        if (zoneSection != null) {
            for (String key : zoneSection.getKeys(false)) {
                String worldName = config.getString("zones." + key + ".world");
                if (worldName == null) continue;

                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;

                int x1 = config.getInt("zones." + key + ".x1");
                int y1 = config.getInt("zones." + key + ".y1");
                int z1 = config.getInt("zones." + key + ".z1");
                int x2 = config.getInt("zones." + key + ".x2");
                int y2 = config.getInt("zones." + key + ".y2");
                int z2 = config.getInt("zones." + key + ".z2");

                String ownerStr = config.getString("zones." + key + ".owner");
                UUID owner;
                try {
                    owner = ownerStr != null ? UUID.fromString(ownerStr) : new UUID(0, 0);
                } catch (IllegalArgumentException e) {
                    owner = new UUID(0, 0);
                }

                Location p1 = new Location(world, x1, y1, z1);
                Location p2 = new Location(world, x2, y2, z2);

                zones.put(key, new Zone(p1, p2, owner));
            }
        }

        ConfigurationSection bindSection = config.getConfigurationSection("bindings");
        if (bindSection != null) {
            for (String key : bindSection.getKeys(false)) {
                String zoneName = config.getString("bindings." + key);
                if (zoneName != null) {
                    bindings.put(key, zoneName);
                }
            }
        }

        plugin.getLogger().info("Loaded zones: " + zones.size() + ", bindings: " + bindings.size());
    }

    private void saveZone(String name, Zone zone) {
        String path = "zones." + name + ".";
        config.set(path + "world", zone.getWorld().getName());
        config.set(path + "x1", zone.getMin().getBlockX());
        config.set(path + "y1", zone.getMin().getBlockY());
        config.set(path + "z1", zone.getMin().getBlockZ());
        config.set(path + "x2", zone.getMax().getBlockX());
        config.set(path + "y2", zone.getMax().getBlockY());
        config.set(path + "z2", zone.getMax().getBlockZ());
        config.set(path + "owner", zone.getOwner().toString());
        save();
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save zones.yml: " + e.getMessage());
        }
    }

    public static class Zone {
        private final Location min;
        private final Location max;
        private final World world;
        private final UUID owner;

        public Zone(Location p1, Location p2, UUID owner) {
            this.world = p1.getWorld();
            this.owner = owner;
            this.min = new Location(
                    world,
                    Math.min(p1.getBlockX(), p2.getBlockX()),
                    Math.min(p1.getBlockY(), p2.getBlockY()),
                    Math.min(p1.getBlockZ(), p2.getBlockZ())
            );
            this.max = new Location(
                    world,
                    Math.max(p1.getBlockX(), p2.getBlockX()),
                    Math.max(p1.getBlockY(), p2.getBlockY()),
                    Math.max(p1.getBlockZ(), p2.getBlockZ())
            );
        }

        public boolean contains(Location loc) {
            if (!loc.getWorld().equals(world)) return false;
            return loc.getBlockX() >= min.getBlockX() && loc.getBlockX() <= max.getBlockX()
                    && loc.getBlockY() >= min.getBlockY() && loc.getBlockY() <= max.getBlockY()
                    && loc.getBlockZ() >= min.getBlockZ() && loc.getBlockZ() <= max.getBlockZ();
        }

        public World getWorld() { return world; }
        public Location getMin() { return min; }
        public Location getMax() { return max; }
        public UUID getOwner() { return owner; }
    }
}