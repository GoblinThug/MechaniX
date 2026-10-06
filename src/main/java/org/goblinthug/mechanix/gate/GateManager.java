package org.goblinthug.mechanix.gate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.goblinthug.mechanix.MechaniX;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GateManager implements Listener {

    private final MechaniX plugin;

    private final Map<UUID, GateData> openedGates = new HashMap<>();
    private final Map<String, Long> lastClick = new HashMap<>();

    public GateManager(MechaniX plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Material type = block.getType();
        boolean isButton = type.name().endsWith("_BUTTON");
        boolean isLever = type == Material.LEVER;

        if (!isButton && !isLever) return;

        String zoneName = plugin.getGateStorage().getBoundZone(block.getLocation());
        if (zoneName == null) return;

        String key = blockKey(block.getLocation());
        long now = System.currentTimeMillis();
        Long last = lastClick.get(key);
        if (last != null && now - last < plugin.getConfigManager().clickCooldownMs) return;
        lastClick.put(key, now);

        Player player = event.getPlayer();
        Location blockLoc = block.getLocation();

        plugin.runTaskAt(blockLoc, () -> toggleGate(player, zoneName));
    }

    @EventHandler
    public void onRedstone(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();
        if (!type.name().endsWith("_PRESSURE_PLATE")) return;

        if (event.getOldCurrent() > 0 || event.getNewCurrent() == 0) return;

        String zoneName = plugin.getGateStorage().getBoundZone(block.getLocation());
        if (zoneName == null) return;

        String key = blockKey(block.getLocation());
        long now = System.currentTimeMillis();
        Long last = lastClick.get(key);
        if (last != null && now - last < plugin.getConfigManager().clickCooldownMs) return;
        lastClick.put(key, now);

        Location blockLoc = block.getLocation();

        plugin.runTaskAt(blockLoc, () -> {
            Player nearest = findNearestPlayer(blockLoc, 16);
            toggleGate(nearest, zoneName);
        });
    }

    private Player findNearestPlayer(Location loc, double radius) {
        Player best = null;
        double bestDist = radius * radius;
        for (Player p : loc.getWorld().getPlayers()) {
            if (!p.getWorld().equals(loc.getWorld())) continue;
            double d = p.getLocation().distanceSquared(loc);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    private void toggleGate(Player player, String zoneName) {
        GateStorage.Zone zone = plugin.getGateStorage().getZone(zoneName);
        if (zone == null) {
            safeMessage(player, plugin.getLangManager().get("zone-not-found", "zone", zoneName));
            return;
        }

        UUID uuid = player != null ? player.getUniqueId() : new UUID(0, 0);
        GateData data = openedGates.get(uuid);

        if (data != null && data.zone != zone) {
            openedGates.remove(uuid);
            data = null;
        }

        if (data != null && (data.mode == Mode.OPENING || data.mode == Mode.CLOSING)) {
            safeMessage(player, plugin.getLangManager().get("gate-busy"));
            return;
        }

        if (data == null) {
            data = prepareGateData(player, zone, zoneName);
            if (data == null) {
                return;
            }
            openedGates.put(uuid, data);
            data.mode = Mode.OPENING;
            runStep(player, uuid, data);
            return;
        }

        if (!isZoneInOriginalState(data)) {
            Block broken = findBrokenBlock(data);
            if (broken != null) {
                safeMessage(player, plugin.getLangManager().get("gate-broken",
                        "x", broken.getX(),
                        "y", broken.getY(),
                        "z", broken.getZ()));
                return;
            }
        }

        data.generation++;
        if (data.mode == Mode.OPEN) {
            data.mode = Mode.CLOSING;
        } else {
            data.mode = Mode.OPENING;
        }
        runStep(player, uuid, data);
    }

    private void runStep(Player player, UUID uuid, GateData data) {
        if (data.mode == Mode.OPENING) {
            stepOpen(player, uuid, data);
        } else if (data.mode == Mode.CLOSING) {
            stepClose(player, uuid, data);
        }
    }

    private GateData prepareGateData(Player player, GateStorage.Zone zone, String zoneName) {
        var cfg = plugin.getConfigManager();
        List<SavedBlock> blocks = new ArrayList<>();
        Location min = zone.getMin();
        Location max = zone.getMax();
        List<String> serialized = new ArrayList<>();

        boolean whitelistEnabled = !cfg.allowedBlocks.isEmpty();

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Block block = zone.getWorld().getBlockAt(x, y, z);
                    if (block.getType() == Material.AIR) continue;
                    if (cfg.blacklistedBlocks.contains(block.getType())) continue;

                    if (whitelistEnabled && !cfg.allowedBlocks.contains(block.getType())) {
                        safeMessage(player, plugin.getLangManager().get("gate-disallowed-block",
                                "block", block.getType().name()));
                        return null;
                    }

                    blocks.add(new SavedBlock(block.getLocation(), block.getBlockData()));
                    serialized.add(serialize(block));
                }
            }
        }

        if (blocks.isEmpty()) {
            safeMessage(player, plugin.getLangManager().get("zone-no-blocks"));
            return null;
        }

        sortByDirection(blocks);

        plugin.getGateStorage().saveOpenedGate(zoneName, serialized);

        return new GateData(zone, blocks);
    }

    private void sortByDirection(List<SavedBlock> blocks) {
        switch (plugin.getConfigManager().openDirection) {
            case BOTTOM_UP -> blocks.sort(Comparator.comparingInt(b -> b.original.getBlockY()));
            case TOP_DOWN -> blocks.sort((a, b) -> Integer.compare(b.original.getBlockY(), a.original.getBlockY()));
            case X_AXIS -> blocks.sort(Comparator.comparingInt(b -> b.original.getBlockX()));
            case Z_AXIS -> blocks.sort(Comparator.comparingInt(b -> b.original.getBlockZ()));
        }
    }

    private int layerKey(SavedBlock block) {
        return switch (plugin.getConfigManager().openDirection) {
            case BOTTOM_UP, TOP_DOWN -> block.original.getBlockY();
            case X_AXIS -> block.original.getBlockX();
            case Z_AXIS -> block.original.getBlockZ();
        };
    }

    private boolean isZoneInOriginalState(GateData data) {
        for (SavedBlock saved : data.blocks) {
            Block current = saved.original.getBlock();
            if (current.getType() != saved.data.getMaterial()) {
                return false;
            }
        }
        return true;
    }

    private Block findBrokenBlock(GateData data) {
        for (SavedBlock saved : data.blocks) {
            Block current = saved.original.getBlock();

            if (saved.removed) {
                if (!current.getType().isAir()) {
                    return current;
                }
            } else {
                if (current.getType().isAir()) {
                    return current;
                }
                if (current.getType() != saved.data.getMaterial()) {
                    return current;
                }
            }
        }
        return null;
    }

    private void stepOpen(Player player, UUID uuid, GateData data) {
        if (data.mode != Mode.OPENING) return;

        int currentGen = data.generation;

        int firstIndex = -1;
        for (int i = 0; i < data.blocks.size(); i++) {
            if (!data.blocks.get(i).removed) {
                firstIndex = i;
                break;
            }
        }

        if (firstIndex == -1) {
            data.mode = Mode.OPEN;
            sendSound(data.zone.getMin(), plugin.getConfigManager().soundOpening);
            return;
        }

        int layer = layerKey(data.blocks.get(firstIndex));

        for (SavedBlock saved : data.blocks) {
            if (!saved.removed && layerKey(saved) == layer) {
                Block current = saved.original.getBlock();
                if (!current.getType().isAir()) {
                    current.setType(Material.AIR);
                }
                saved.removed = true;
            }
        }

        plugin.runTaskAtLater(data.zone.getMin(), () -> {
            if (data.generation == currentGen && data.mode == Mode.OPENING) {
                stepOpen(player, uuid, data);
            }
        }, Math.max(1, plugin.getConfigManager().stepDelayTicks));
    }

    private void stepClose(Player player, UUID uuid, GateData data) {
        if (data.mode != Mode.CLOSING) return;

        int currentGen = data.generation;

        int lastIndex = -1;
        for (int i = data.blocks.size() - 1; i >= 0; i--) {
            if (data.blocks.get(i).removed) {
                lastIndex = i;
                break;
            }
        }

        if (lastIndex == -1) {
            String zoneName = getZoneNameByData(data);
            if (zoneName != null) {
                plugin.getGateStorage().removeOpenedGate(zoneName);
            }
            data.mode = Mode.CLOSED;
            sendSound(data.zone.getMin(), plugin.getConfigManager().soundClosing);
            return;
        }

        int layer = layerKey(data.blocks.get(lastIndex));

        for (SavedBlock saved : data.blocks) {
            if (saved.removed && layerKey(saved) == layer) {
                Block current = saved.original.getBlock();
                if (current.getType().isAir()) {
                    current.setBlockData(saved.data);
                }
                saved.removed = false;
            }
        }

        plugin.runTaskAtLater(data.zone.getMin(), () -> {
            if (data.generation == currentGen && data.mode == Mode.CLOSING) {
                stepClose(player, uuid, data);
            }
        }, Math.max(1, plugin.getConfigManager().stepDelayTicks));
    }

    public void restoreOpened(String zoneName, List<String> serialized) {
        GateStorage.Zone zone = plugin.getGateStorage().getZone(zoneName);
        if (zone == null) return;

        List<SavedBlock> blocks = new ArrayList<>();
        for (String line : serialized) {
            SavedBlock saved = deserialize(line);
            if (saved != null) {
                saved.removed = true;
                blocks.add(saved);
            }
        }

        if (blocks.isEmpty()) return;

        GateData data = new GateData(zone, blocks);
        data.mode = Mode.OPEN;
        openedGates.put(zone.getOwner(), data);
    }

    public void forceCloseByZone(String zoneName) {
        GateStorage.Zone zone = plugin.getGateStorage().getZone(zoneName);
        if (zone == null) return;

        for (Map.Entry<UUID, GateData> entry : openedGates.entrySet()) {
            if (entry.getValue().zone == zone) {
                for (SavedBlock saved : entry.getValue().blocks) {
                    if (saved.removed) {
                        Block current = saved.original.getBlock();
                        if (current.getType().isAir()) {
                            current.setBlockData(saved.data);
                        }
                        saved.removed = false;
                    }
                }
                openedGates.remove(entry.getKey());
                break;
            }
        }

        plugin.getGateStorage().removeOpenedGate(zoneName);
    }

    private String getZoneNameByData(GateData data) {
        for (String name : plugin.getGateStorage().getZoneNames()) {
            if (plugin.getGateStorage().getZone(name) == data.zone) return name;
        }
        return null;
    }

    private void sendSound(Location loc, Sound sound) {
        if (sound == null) return;
        var cfg = plugin.getConfigManager();
        loc.getWorld().playSound(loc, sound, cfg.soundVolume, cfg.soundPitch);
    }

    private void safeMessage(Player player, String msg) {
        if (player != null) player.sendMessage(msg);
    }

    private String blockKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private String serialize(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ()
                + ":" + block.getBlockData().getAsString();
    }

    private SavedBlock deserialize(String line) {
        String[] parts = line.split(":", 5);
        if (parts.length < 5) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            BlockData data = Bukkit.createBlockData(parts[4]);
            return new SavedBlock(new Location(world, x, y, z), data);
        } catch (Exception e) {
            return null;
        }
    }

    private static class SavedBlock {
        final Location original;
        final BlockData data;
        boolean removed = false;

        SavedBlock(Location original, BlockData data) {
            this.original = original;
            this.data = data;
        }
    }

    private enum Mode {
        OPENING, CLOSING, OPEN, CLOSED
    }

    private static class GateData {
        final GateStorage.Zone zone;
        final List<SavedBlock> blocks;
        Mode mode = Mode.CLOSED;
        int generation = 0;

        GateData(GateStorage.Zone zone, List<SavedBlock> blocks) {
            this.zone = zone;
            this.blocks = blocks;
        }
    }
}