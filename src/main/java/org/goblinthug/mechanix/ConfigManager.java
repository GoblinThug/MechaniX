package org.goblinthug.mechanix;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    private final MechaniX plugin;

    public String locale;
    public boolean restoreOpenedGates;
    public boolean useActionbarForFeedback;

    public long stepDelayTicks;
    public OpenDirection openDirection;
    public long clickCooldownMs;
    public int maxZoneVolume;
    public int maxZoneSide;
    public Set<Material> blacklistedBlocks;
    public Set<Material> protectedBlocks;
    public Set<Material> allowedBlocks;
    public Sound soundOpening;
    public Sound soundClosing;
    public float soundVolume;
    public float soundPitch;

    public Particle borderParticle;
    public String borderColorHex;
    public double borderStep;
    public int borderDurationTicks;
    public int borderPeriodTicks;

    public boolean permissionsEnabled;
    public String permUse, permCreate, permBind, permDelete, permAdmin, permMenu;
    public String permPortalUse, permPortalCreate, permPortalDelete;

    public int defaultMaxZones;
    public boolean adminBypassLimit;

    // Portals
    public long portalCooldownMs;
    public int portalMinSide;
    public int portalMaxSide;
    // Allow horizontal portals (XZ plane). See portals.allow-horizontal in config.yml.
    public boolean portalAllowHorizontal = false;
    public boolean portalShowActiveBorders;
    public int portalActiveBorderPeriod;

    // Portal particle visuals
    public boolean portalFillEnabled = true;
    public double portalFillStep = 1.0;
    public int portalBorderDensity = 1;
    public int portalFillDensity = 1;
    public double portalDrift = 0.25;
    public double portalSpread = 0.1;

    public ConfigManager(MechaniX plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();
        cfg.options().copyDefaults(true);
        plugin.saveConfig();

        locale = cfg.getString("general.locale", "ru").toLowerCase();
        restoreOpenedGates = cfg.getBoolean("general.restore-opened-gates", true);
        useActionbarForFeedback = cfg.getBoolean("general.use-actionbar-for-feedback", false);

        stepDelayTicks = cfg.getLong("gates.step-delay-ticks", 4L);
        clickCooldownMs = cfg.getLong("gates.click-cooldown-ms", 500L);
        maxZoneVolume = cfg.getInt("gates.max-zone-volume", 20000);
        maxZoneSide = cfg.getInt("gates.max-zone-side", 64);

        try {
            openDirection = OpenDirection.valueOf(
                    cfg.getString("gates.open-direction", "BOTTOM_UP").toUpperCase());
        } catch (IllegalArgumentException e) {
            openDirection = OpenDirection.BOTTOM_UP;
            plugin.getLogger().warning("Invalid gates.open-direction, using BOTTOM_UP.");
        }

        blacklistedBlocks = parseMaterials(cfg.getStringList("gates.blacklisted-blocks"));
        protectedBlocks = parseMaterials(cfg.getStringList("gates.protected-blocks"));
        allowedBlocks = parseMaterials(cfg.getStringList("gates.allowed-blocks"));

        soundOpening = parseSound(cfg.getString("gates.sounds.opening"));
        soundClosing = parseSound(cfg.getString("gates.sounds.closing"));
        soundVolume = (float) cfg.getDouble("gates.sounds.volume", 1.0);
        soundPitch = (float) cfg.getDouble("gates.sounds.pitch", 1.0);

        borderParticle = parseParticle(cfg.getString("gates.border.particle", "DUST"));
        borderColorHex = cfg.getString("gates.border.color", "#FF0000");
        borderStep = cfg.getDouble("gates.border.step", 0.25);
        borderDurationTicks = cfg.getInt("gates.border.duration-ticks", 100);
        borderPeriodTicks = cfg.getInt("gates.border.period-ticks", 10);

        permissionsEnabled = cfg.getBoolean("permissions.enabled", true);
        permUse    = cfg.getString("permissions.use", "mechanix.use");
        permCreate = cfg.getString("permissions.create", "mechanix.create");
        permBind   = cfg.getString("permissions.bind", "mechanix.bind");
        permDelete = cfg.getString("permissions.delete", "mechanix.delete");
        permAdmin  = cfg.getString("permissions.admin", "mechanix.admin");
        permMenu   = cfg.getString("permissions.menu", "mechanix.menu");
        permPortalUse    = cfg.getString("permissions.portal-use", "mechanix.portal.use");
        permPortalCreate = cfg.getString("permissions.portal-create", "mechanix.portal.create");
        permPortalDelete = cfg.getString("permissions.portal-delete", "mechanix.portal.delete");

        defaultMaxZones = cfg.getInt("limits.default-max-zones", 5);
        adminBypassLimit = cfg.getBoolean("limits.admin-bypass", true);

        portalCooldownMs = cfg.getLong("portals.cooldown-ms", 2000L);
        portalMinSide = cfg.getInt("portals.min-side", 2);
        portalMaxSide = cfg.getInt("portals.max-side", 21);
        portalShowActiveBorders = cfg.getBoolean("portals.show-active-borders", true);
        portalActiveBorderPeriod = cfg.getInt("portals.active-border-period", 2);
        portalAllowHorizontal = cfg.getBoolean("portals.allow-horizontal", false);

        portalFillEnabled = cfg.getBoolean("portals.particles.fill-enabled", true);
        portalFillStep = cfg.getDouble("portals.particles.fill-step", 1.0);
        portalBorderDensity = cfg.getInt("portals.particles.border-density", 1);
        portalFillDensity = cfg.getInt("portals.particles.fill-density", 1);
        portalDrift = cfg.getDouble("portals.particles.drift", 0.25);
        portalSpread = cfg.getDouble("portals.particles.spread", 0.1);
    }

    private Set<Material> parseMaterials(List<String> list) {
        Set<Material> out = new HashSet<>();
        for (String name : list) {
            try {
                out.add(Material.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown material in config: " + name);
            }
        }
        return out;
    }

    /**
     * Парсит звук из конфига. Работает и на Paper 1.20.6, и на 1.21+.
     *
     * Paper 1.21 сделал org.bukkit.Sound интерфейсом, поэтому valueOf() больше
     * не существует. Используем Registry.SOUNDS + NamespacedKey.
     */
    private Sound parseSound(String name) {
        if (name == null || name.isBlank()) return null;

        // Пытаемся получить через реестр (Paper 1.21+).
        // Legacy-имя BLOCK_PISTON_EXTEND -> minecraft:block.piston.extend
        String key = name.toLowerCase().replace('_', '.');
        NamespacedKey namespacedKey = NamespacedKey.minecraft(key);

        Sound sound = Registry.SOUNDS.get(namespacedKey);
        if (sound != null) return sound;

        // Fallback для нестандартных ключей (если в конфиге уже написан
        // ключ формата "minecraft:block.piston.extend").
        try {
            NamespacedKey parsed = NamespacedKey.fromString(name.toLowerCase());
            if (parsed != null) {
                sound = Registry.SOUNDS.get(parsed);
                if (sound != null) return sound;
            }
        } catch (Exception ignored) {
        }

        plugin.getLogger().warning("Unknown sound in config: " + name);
        return null;
    }

    /**
     * Парсит частицу из конфига. Аналогично Sound, Particle в Paper 1.21+
     * тоже стал интерфейсом, поэтому valueOf() недоступен.
     */
    private Particle parseParticle(String name) {
        if (name == null || name.isBlank()) return Particle.DUST;

        String key = name.toLowerCase();
        NamespacedKey namespacedKey = NamespacedKey.minecraft(key);

        Particle particle = Registry.PARTICLE_TYPE.get(namespacedKey);
        if (particle != null) return particle;

        plugin.getLogger().warning("Unknown particle in config: " + name + ", using DUST.");
        return Particle.DUST;
    }

    private Material parseMaterial(String name, Material def) {
        if (name == null || name.isBlank()) return def;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown material in config: " + name);
            return def;
        }
    }

    public enum OpenDirection {
        BOTTOM_UP, TOP_DOWN, X_AXIS, Z_AXIS
    }
}