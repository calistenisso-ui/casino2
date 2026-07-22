package com.example.roulette;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class RouletteHolder implements InventoryHolder {

    private boolean spinning = false;

    public boolean isSpinning() {
        return spinning;
    }

    public void setSpinning(boolean spinning) {
        this.spinning = spinning;
    }

    @Override
    public @NotNull Inventory getInventory() {
        throw new UnsupportedOperationException();
    }
}
