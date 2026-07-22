package com.example.roulette;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RoulettePlugin extends JavaPlugin {

    private final Map<UUID, Integer> playerChips = new HashMap<>();
    private final Map<UUID, Material> playerBets = new HashMap<>();
    private final Map<UUID, Integer> playerBetAmounts = new HashMap<>();
    private final Map<UUID, Integer> playerDefaultBets = new HashMap<>();

    public static final int[] BET_STEPS = {50, 100, 500, 1000, 5000, 10000, 50000, 100000, 1000000};
    public static final int DEFAULT_BET_INDEX = 1;

    private File chipsFile;
    private FileConfiguration chipsConfig;

    @Override
    public void onEnable() {
        loadChips();
        getCommand("roulette").setExecutor(new RouletteCommand(this));
        getCommand("chips").setExecutor(new ChipsCommand(this));
        getServer().getPluginManager().registerEvents(new RouletteListener(this), this);
        getLogger().info("RoulettePlugin abilitato con successo!");
    }

    @Override
    public void onDisable() {
        saveChips();
        getLogger().info("RoulettePlugin disabilitato. Chips salvate.");
    }

    private void loadChips() {
        chipsFile = new File(getDataFolder(), "chips.yml");
        if (!chipsFile.exists()) {
            saveResource("chips.yml", false);
        }
        chipsConfig = YamlConfiguration.loadConfiguration(chipsFile);
        for (String key : chipsConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int amount = chipsConfig.getInt(key + ".chips", 0);
                int defaultBet = chipsConfig.getInt(key + ".defaultBet", BET_STEPS[DEFAULT_BET_INDEX]);
                playerChips.put(uuid, amount);
                playerDefaultBets.put(uuid, defaultBet);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void saveChips() {
        if (chipsConfig == null) return;
        chipsConfig.getKeys(false).forEach(k -> chipsConfig.set(k, null));
        for (Map.Entry<UUID, Integer> entry : playerChips.entrySet()) {
            UUID uuid = entry.getKey();
            chipsConfig.set(uuid.toString() + ".chips", entry.getValue());
            chipsConfig.set(uuid.toString() + ".defaultBet", playerDefaultBets.getOrDefault(uuid, BET_STEPS[DEFAULT_BET_INDEX]));
        }
        try {
            chipsConfig.save(chipsFile);
        } catch (IOException e) {
            getLogger().severe("Impossibile salvare chips.yml: " + e.getMessage());
        }
    }

    public int getChips(UUID uuid) {
        return playerChips.getOrDefault(uuid, 0);
    }

    public void setChips(UUID uuid, int amount) {
        if (amount < 0) amount = 0;
        playerChips.put(uuid, amount);
        saveChips();
    }

    public void addChips(UUID uuid, int amount) {
        setChips(uuid, getChips(uuid) + amount);
    }

    public void subtractChips(UUID uuid, int amount) {
        setChips(uuid, getChips(uuid) - amount);
    }

    public Material getPlayerBet(UUID uuid) {
        return playerBets.get(uuid);
    }

    public void setPlayerBet(UUID uuid, Material bet) {
        if (bet == null) {
            playerBets.remove(uuid);
            playerBetAmounts.remove(uuid);
        } else {
            playerBets.put(uuid, bet);
        }
    }

    public int getPlayerBetAmount(UUID uuid) {
        return playerBetAmounts.getOrDefault(uuid, 0);
    }

    public void setPlayerBetAmount(UUID uuid, int amount) {
        if (amount < 0) amount = 0;
        playerBetAmounts.put(uuid, amount);
    }

    public void addPlayerBetAmount(UUID uuid, int amount) {
        setPlayerBetAmount(uuid, getPlayerBetAmount(uuid) + amount);
    }

    public void subtractPlayerBetAmount(UUID uuid, int amount) {
        setPlayerBetAmount(uuid, getPlayerBetAmount(uuid) - amount);
    }

    public int getPlayerDefaultBet(UUID uuid) {
        return playerDefaultBets.getOrDefault(uuid, BET_STEPS[DEFAULT_BET_INDEX]);
    }

    public void setPlayerDefaultBet(UUID uuid, int bet) {
        playerDefaultBets.put(uuid, bet);
        saveChips();
    }

    public static String formatBet(int bet) {
        return String.valueOf(bet);
    }
}
