package org.goblinthug.mechanix;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.goblinthug.mechanix.command.MxCommand;
import org.goblinthug.mechanix.gate.GateManager;
import org.goblinthug.mechanix.gate.GateMenuListener;
import org.goblinthug.mechanix.gate.GateStorage;
import org.goblinthug.mechanix.menu.MainMenuListener;
import org.goblinthug.mechanix.portal.PortalCommand;
import org.goblinthug.mechanix.portal.PortalManager;
import org.goblinthug.mechanix.portal.PortalMenuListener;
import org.goblinthug.mechanix.portal.PortalStorage;

import java.util.List;
import java.util.Map;

public class MechaniX extends JavaPlugin {

    private ConfigManager configManager;
    private LangManager langManager;
    private GateStorage gateStorage;
    private GateManager gateManager;
    private PortalStorage portalStorage;
    private PortalManager portalManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("lang/ru.yml", false);
        saveResource("lang/en.yml", false);

        configManager = new ConfigManager(this);
        langManager = new LangManager(this);

        gateStorage = new GateStorage(this);
        gateManager = new GateManager(this);
        portalStorage = new PortalStorage(this);
        portalManager = new PortalManager(this);
        portalManager.start();

        var mxCommand = getCommand("mx");
        if (mxCommand != null) {
            mxCommand.setExecutor(new MxCommand(this));
        }

        var portalCommand = getCommand("portal");
        if (portalCommand != null) {
            portalCommand.setExecutor(new PortalCommand(this));
        }

        getServer().getPluginManager().registerEvents(new MainMenuListener(this), this);
        getServer().getPluginManager().registerEvents(gateManager, this);
        getServer().getPluginManager().registerEvents(new GateMenuListener(this), this);
        getServer().getPluginManager().registerEvents(portalManager, this);
        getServer().getPluginManager().registerEvents(new PortalMenuListener(this), this);

        if (configManager.restoreOpenedGates) {
            for (Map.Entry<String, List<String>> entry : gateStorage.getAllOpened().entrySet()) {
                gateManager.restoreOpened(entry.getKey(), entry.getValue());
            }
        }

        getLogger().info("MechaniX enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MechaniX disabled.");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public LangManager getLangManager() { return langManager; }
    public GateStorage getGateStorage() { return gateStorage; }
    public GateManager getGateManager() { return gateManager; }
    public PortalStorage getPortalStorage() { return portalStorage; }
    public PortalManager getPortalManager() { return portalManager; }

    public void runTaskAt(Location location, Runnable task) {
        if (isFolia()) {
            Bukkit.getServer().getRegionScheduler().run(this, location, scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTask(this, task);
        }
    }

    public void runTaskAtLater(Location location, Runnable task, long delayTicks) {
        if (isFolia()) {
            Bukkit.getServer().getRegionScheduler().runDelayed(
                    this, location, scheduledTask -> task.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(this, task, delayTicks);
        }
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}