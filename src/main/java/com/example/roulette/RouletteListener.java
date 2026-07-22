package com.example.roulette;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RouletteListener implements Listener {

    private final RoulettePlugin plugin;
    private final Map<UUID, Integer> errorSlot = new HashMap<>();

    public RouletteListener(RoulettePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Gestisce il tasto destro sull'NPC della roulette: è l'UNICO modo per aprire
     * il menu (non è più possibile aprirlo scrivendo /roulette in chat).
     */
    @EventHandler
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        // Evita che l'evento venga gestito due volte (mano principale + mano secondaria)
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, RouletteCommand.NPC_KEY_NAME);
        if (!event.getRightClicked().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
            return;
        }

        event.setCancelled(true);
        RouletteCommand.openGui(plugin, event.getPlayer());
    }

    /**
     * Impedisce ai giocatori di colpire/uccidere l'NPC della roulette.
     */
    @EventHandler
    public void onNpcDamage(EntityDamageByEntityEvent event) {
        NamespacedKey key = new NamespacedKey(plugin, RouletteCommand.NPC_KEY_NAME);
        if (event.getEntity().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof RouletteHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (holder.isSpinning()) {
            return;
        }

        int slot = event.getRawSlot();
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        UUID uuid = player.getUniqueId();

        if (errorSlot.containsKey(uuid) && errorSlot.get(uuid) != slot) {
            restoreSlot(top, errorSlot.get(uuid), player);
            errorSlot.remove(uuid);
        }

        if (slot == RouletteCommand.CENTER_SLOT) {
            Material bet = plugin.getPlayerBet(uuid);
            if (bet == null) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            new RouletteGame(plugin, top, player).start();

        } else if (slot == RouletteCommand.BLACK_BET_SLOT) {
            // Slot 16: cliccando sulla lana nera invisibile o sul bottone warped
            handleBetClick(player, top, Material.BLACK_WOOL, slot);

        } else if (slot == RouletteCommand.RED_BET_SLOT) {
            // Slot 34: cliccando sulla lana rossa invisibile o sul bottone warped
            handleBetClick(player, top, Material.RED_WOOL, slot);

        } else if (slot == RouletteCommand.LEVER_UP_SLOT) {
            handleLeverUp(player, top);

        } else if (slot == RouletteCommand.LEVER_DOWN_SLOT) {
            handleLeverDown(player, top);

        } else {
            if (errorSlot.containsKey(uuid)) {
                restoreSlot(top, errorSlot.get(uuid), player);
                errorSlot.remove(uuid);
            }
        }
    }

    private void handleBetClick(Player player, Inventory gui, Material color, int slot) {
        UUID uuid = player.getUniqueId();
        int chips = plugin.getChips(uuid);
        int currentBet = plugin.getPlayerBetAmount(uuid);
        int defaultBet = plugin.getPlayerDefaultBet(uuid);
        Material currentBetColor = plugin.getPlayerBet(uuid);

        if (errorSlot.containsKey(uuid) && errorSlot.get(uuid) == slot) {
            restoreSlot(gui, slot, player);
            errorSlot.remove(uuid);
        }

        if (currentBetColor == null) {
            // Nessuna scommessa attiva: inizia una nuova scommessa su questo colore
            if (defaultBet > chips) {
                RouletteCommand.showError(gui, slot);
                errorSlot.put(uuid, slot);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            plugin.setPlayerBet(uuid, color);
            plugin.setPlayerBetAmount(uuid, defaultBet);
            RouletteCommand.updateBetDisplay(gui, color, defaultBet, player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

        } else if (currentBetColor == color) {
            // Stesso colore: aggiunge chips alla scommessa esistente
            int newTotalBet = currentBet + defaultBet;
            if (newTotalBet > chips) {
                // NON azzerare la scommessa! Aggiungi solo la lore rossa al bottone esistente
                RouletteCommand.showErrorOnButton(gui, slot, currentBet);
                errorSlot.put(uuid, slot);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            plugin.addPlayerBetAmount(uuid, defaultBet);
            int newBet = plugin.getPlayerBetAmount(uuid);
            RouletteCommand.updateBetDisplay(gui, color, newBet, player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

        } else {
            // Colore diverso: azzera la vecchia scommessa e inizia una nuova su questo colore
            if (defaultBet > chips) {
                RouletteCommand.showError(gui, slot);
                errorSlot.put(uuid, slot);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            // Azzera la vecchia scommessa
            plugin.setPlayerBet(uuid, null);
            plugin.setPlayerBetAmount(uuid, 0);
            // Aggiorna la GUI per mostrare la lana invisibile sull'altro slot
            Material otherColor = (color == Material.BLACK_WOOL) ? Material.RED_WOOL : Material.BLACK_WOOL;
            RouletteCommand.updateBetDisplay(gui, otherColor, 0, player);

            // Inizia nuova scommessa su questo colore
            plugin.setPlayerBet(uuid, color);
            plugin.setPlayerBetAmount(uuid, defaultBet);
            RouletteCommand.updateBetDisplay(gui, color, defaultBet, player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }

    private void handleLeverUp(Player player, Inventory gui) {
        UUID uuid = player.getUniqueId();
        int currentBet = plugin.getPlayerDefaultBet(uuid);
        int[] steps = RoulettePlugin.BET_STEPS;

        int currentIndex = -1;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == currentBet) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex < steps.length - 1) {
            plugin.setPlayerDefaultBet(uuid, steps[currentIndex + 1]);
            RouletteCommand.updateLevers(gui, player);
            player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 1.2f);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private void handleLeverDown(Player player, Inventory gui) {
        UUID uuid = player.getUniqueId();
        int currentBet = plugin.getPlayerDefaultBet(uuid);
        int[] steps = RoulettePlugin.BET_STEPS;

        int currentIndex = -1;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == currentBet) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex > 0) {
            plugin.setPlayerDefaultBet(uuid, steps[currentIndex - 1]);
            RouletteCommand.updateLevers(gui, player);
            player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 0.8f);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private void restoreSlot(Inventory gui, int slot, Player player) {
        Material bet = plugin.getPlayerBet(player.getUniqueId());
        int betAmount = plugin.getPlayerBetAmount(player.getUniqueId());

        if (slot == RouletteCommand.BLACK_BET_SLOT) {
            if (bet == Material.BLACK_WOOL) {
                // Se c'e' ancora una scommessa attiva sul nero, ripristina il bottone warped
                ItemStack btn = new ItemStack(Material.WARPED_BUTTON);
                ItemMeta btnMeta = btn.getItemMeta();
                if (btnMeta != null) {
                    btnMeta.displayName(Component.text("Chips").color(TextColor.color(0x00AAAA)));
                    btnMeta.setCustomModelData(RouletteCommand.getChipsCustomModelData(betAmount));
                    java.util.List<Component> lore = new java.util.ArrayList<>();
                    lore.add(Component.text("Scommessa: NERO").color(TextColor.color(0x555555)));
                    lore.add(Component.text(RoulettePlugin.formatBet(betAmount) + " chips scommesse").color(TextColor.color(0xFFD700)));
                    btnMeta.lore(lore);
                    btn.setItemMeta(btnMeta);
                }
                gui.setItem(slot, btn);
            } else {
                // Nessuna scommessa, ripristina il blocco di concrete nera INVISIBILE
                ItemStack blackBlock = new ItemStack(Material.BLACK_CONCRETE);
                ItemMeta blackMeta = blackBlock.getItemMeta();
                if (blackMeta != null) {
                    blackMeta.displayName(Component.text("Scommetti NERO").color(TextColor.color(0x555555)));
                    blackMeta.setCustomModelData(RouletteCommand.INVISIBLE_BLACK_CONCRETE_CMD);
                    blackBlock.setItemMeta(blackMeta);
                }
                gui.setItem(slot, blackBlock);
            }
        } else if (slot == RouletteCommand.RED_BET_SLOT) {
            if (bet == Material.RED_WOOL) {
                // Se c'e' ancora una scommessa attiva sul rosso, ripristina il bottone warped
                ItemStack btn = new ItemStack(Material.WARPED_BUTTON);
                ItemMeta btnMeta = btn.getItemMeta();
                if (btnMeta != null) {
                    btnMeta.displayName(Component.text("Chips").color(TextColor.color(0x00AAAA)));
                    btnMeta.setCustomModelData(RouletteCommand.getChipsCustomModelData(betAmount));
                    java.util.List<Component> lore = new java.util.ArrayList<>();
                    lore.add(Component.text("Scommessa: ROSSO").color(TextColor.color(0xFF0000)));
                    lore.add(Component.text(RoulettePlugin.formatBet(betAmount) + " chips scommesse").color(TextColor.color(0xFFD700)));
                    btnMeta.lore(lore);
                    btn.setItemMeta(btnMeta);
                }
                gui.setItem(slot, btn);
            } else {
                // Nessuna scommessa, ripristina il blocco di concrete rossa INVISIBILE
                ItemStack redBlock = new ItemStack(Material.RED_CONCRETE);
                ItemMeta redMeta = redBlock.getItemMeta();
                if (redMeta != null) {
                    redMeta.displayName(Component.text("Scommetti ROSSO").color(TextColor.color(0xFF0000)));
                    redMeta.setCustomModelData(RouletteCommand.INVISIBLE_RED_CONCRETE_CMD);
                    redBlock.setItemMeta(redMeta);
                }
                gui.setItem(slot, redBlock);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof RouletteHolder holder) {
            if (holder.isSpinning() && event.getPlayer() instanceof Player player) {
                Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(event.getView().getTopInventory()));
                return;
            }
            UUID uuid = event.getPlayer().getUniqueId();
            plugin.setPlayerBet(uuid, null);
            plugin.setPlayerBetAmount(uuid, 0);
            errorSlot.remove(uuid);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof RouletteHolder) {
            event.setCancelled(true);
        }
    }
}
