package org.goblinthug.mechanix.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.goblinthug.mechanix.MechaniX;
import org.goblinthug.mechanix.gate.GateMenu;
import org.goblinthug.mechanix.portal.PortalMenu;

public class MainMenuListener implements Listener {

    private final MechaniX plugin;

    public MainMenuListener(MechaniX plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MechaniXHolder holder)) return;
        if (holder.getType() != MenuType.MAIN) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == MainMenu.SLOT_GATES) {
            GateMenu.openMain(plugin, player);
        } else if (slot == MainMenu.SLOT_PORTALS) {
            PortalMenu.openMain(plugin, player);
        } else if (slot == MainMenu.SLOT_CLOSE) {
            player.closeInventory();
        }
    }
}