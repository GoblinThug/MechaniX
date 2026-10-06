package org.goblinthug.mechanix.portal;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.goblinthug.mechanix.MechaniX;
import org.jetbrains.annotations.NotNull;

public class PortalCommand implements CommandExecutor {

    private final MechaniX plugin;

    public PortalCommand(MechaniX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLangManager().get("only-players"));
            return true;
        }

        PortalMenu.openMain(plugin, player);
        return true;
    }
}