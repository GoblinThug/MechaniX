package org.goblinthug.mechanix.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MechaniXHolder implements InventoryHolder {

    private final MenuType type;
    private Inventory inventory;

    public MechaniXHolder(MenuType type) {
        this.type = type;
    }

    public MenuType getType() {
        return type;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}