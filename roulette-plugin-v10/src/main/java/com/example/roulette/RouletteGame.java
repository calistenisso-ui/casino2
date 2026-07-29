package com.example.roulette;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RouletteGame {

    private final RoulettePlugin plugin;
    private final Inventory gui;
    private final HumanEntity player;
    private final Material chosenColor;
    private final List<Integer> ringSlots;
    private int currentStep = 0;
    private int totalSteps;
    private BukkitTask task;
    private final int betAmount;

    public RouletteGame(RoulettePlugin plugin, Inventory gui, HumanEntity player) {
        this.plugin = plugin;
        this.gui = gui;
        this.player = player;
        this.ringSlots = new ArrayList<>();
        for (int s : RouletteCommand.RING_SLOTS) {
            this.ringSlots.add(s);
        }
        this.chosenColor = ThreadLocalRandom.current().nextBoolean()
                ? Material.RED_WOOL : Material.BLACK_WOOL;
        this.betAmount = player instanceof Player p
                ? plugin.getPlayerBetAmount(p.getUniqueId()) : 0;
    }

    public void start() {
        if (!(gui.getHolder() instanceof RouletteHolder holder)) return;
        if (!(player instanceof Player p)) return;

        UUID uuid = p.getUniqueId();

        plugin.subtractChips(uuid, betAmount);

        holder.setSpinning(true);
        restorePattern();
        gui.setItem(RouletteCommand.CENTER_SLOT, null);

        List<Integer> matchingIndices = new ArrayList<>();
        for (int i = 0; i < ringSlots.size(); i++) {
            ItemStack item = gui.getItem(ringSlots.get(i));
            if (item != null && item.getType() == chosenColor) {
                matchingIndices.add(i);
            }
        }

        int targetIndex = matchingIndices.get(
                ThreadLocalRandom.current().nextInt(matchingIndices.size()));

        int stepsToTarget = (2 - targetIndex + ringSlots.size()) % ringSlots.size();
        this.totalSteps = (ringSlots.size() * 2) + stepsToTarget;

        scheduleNext();
    }

    private void restorePattern() {
        Material[][] pattern = {
                {Material.RED_WOOL,   Material.BLACK_WOOL, Material.RED_WOOL,   Material.BLACK_WOOL, Material.RED_WOOL},
                {Material.BLACK_WOOL, null,                null,                null,                Material.BLACK_WOOL},
                {Material.RED_WOOL,   null,                null,                null,                Material.RED_WOOL},
                {Material.BLACK_WOOL, null,                null,                null,                Material.BLACK_WOOL},
                {Material.RED_WOOL,   Material.BLACK_WOOL, Material.RED_WOOL,   Material.BLACK_WOOL, Material.RED_WOOL}
        };

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                Material mat = pattern[row][col];
                if (mat != null) {
                    int slot = row * 9 + col;
                    gui.setItem(slot, RouletteCommand.createRingItem(mat));
                }
            }
        }
    }

    private void scheduleNext() {
        if (currentStep >= totalSteps) {
            finish();
            return;
        }

        ItemStack lastItem = gui.getItem(ringSlots.get(ringSlots.size() - 1));
        for (int i = ringSlots.size() - 1; i > 0; i--) {
            gui.setItem(ringSlots.get(i), gui.getItem(ringSlots.get(i - 1)));
        }
        gui.setItem(ringSlots.get(0), lastItem);

        if (player instanceof Player p) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f,
                    1.0f + (float) currentStep / totalSteps);
        }

        currentStep++;
        double progress = (double) currentStep / totalSteps;
        long delay = 1 + Math.round(progress * progress * 7);

        task = Bukkit.getScheduler().runTaskLater(plugin, this::scheduleNext, delay);
    }

    private void finish() {
        if (!(gui.getHolder() instanceof RouletteHolder holder)) return;

        ItemStack sunflower = new ItemStack(Material.SUNFLOWER);
        ItemMeta sunflowerMeta = sunflower.getItemMeta();
        if (sunflowerMeta != null) {
            sunflowerMeta.displayName(Component.text("GIRA!").color(TextColor.color(0xFFD700)));
            sunflowerMeta.setCustomModelData(RouletteCommand.SUNFLOWER_INVISIBLE_CMD);
            sunflower.setItemMeta(sunflowerMeta);
        }
        gui.setItem(RouletteCommand.CENTER_SLOT, sunflower);

        if (player instanceof Player p) {
            UUID uuid = p.getUniqueId();

            // Azzera la scommessa PRIMA di aggiornare la GUI
            Material bet = plugin.getPlayerBet(uuid);

            if (bet != null && bet == chosenColor) {
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                plugin.addChips(uuid, betAmount * 2);
            } else {
                p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 0.6f);
            }

            // Reset della scommessa
            plugin.setPlayerBet(uuid, null);
            plugin.setPlayerBetAmount(uuid, 0);

            // Ora aggiorna la GUI con bet=null -> mostra la lana INVISIBILE
            RouletteCommand.updateBetDisplay(gui, null, 0, p);
            RouletteCommand.updateLevers(gui, p);
        }

        holder.setSpinning(false);
    }
}
