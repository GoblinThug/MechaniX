package org.goblinthug.mechanix.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.goblinthug.mechanix.MechaniX;
import org.goblinthug.mechanix.menu.MainMenu;
import org.jetbrains.annotations.NotNull;

public class MxCommand implements CommandExecutor {

    private final MechaniX plugin;

    public MxCommand(MechaniX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLangManager().get("only-players"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (plugin.getConfigManager().permissionsEnabled
                    && !player.hasPermission(plugin.getConfigManager().permAdmin)) {
                player.sendMessage(plugin.getLangManager().get("no-permission"));
                return true;
            }
            plugin.getConfigManager().reload();
            plugin.getLangManager().load();
            player.sendMessage(plugin.getLangManager().get("reload-success"));
            return true;
        }

        var cfg = plugin.getConfigManager();
        if (cfg.permissionsEnabled
                && !player.hasPermission(cfg.permMenu)
                && !player.hasPermission(cfg.permAdmin)) {
            player.sendMessage(plugin.getLangManager().get("no-permission"));
            return true;
        }

        MainMenu.open(plugin, player);
        return true;
    }
}