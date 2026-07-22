package com.example.roulette;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class RouletteCommand implements CommandExecutor {

    //  renderizza la texture di sfondo dalla resource pack
    public static final Component GUI_TITLE = Component.text("")
            .color(TextColor.color(0xFFFFFF));

    public static final int[] RING_SLOTS = {0, 1, 2, 3, 4, 13, 22, 31, 40, 39, 38, 37, 36, 27, 18, 9};
    public static final int CENTER_SLOT = 20;
    public static final int BLACK_BET_SLOT = 16;
    public static final int RED_BET_SLOT = 34;
    public static final int LEVER_UP_SLOT = 14;
    public static final int LEVER_DOWN_SLOT = 32;

    // Custom Model Data per le lane dell'anello (già esistenti)
    public static final int RING_BLACK_CUSTOM_MODEL_DATA = 1001;
    public static final int RING_RED_CUSTOM_MODEL_DATA = 1002;

    // Custom Model Data per i blocchi invisibili nei slot scommessa (concrete)
    public static final int INVISIBLE_BLACK_CONCRETE_CMD = 2001;
    public static final int INVISIBLE_RED_CONCRETE_CMD = 2002;

    // Custom Model Data per chips (3 livelli)
    public static final int CHIPS_LOW_CMD = 3001;      // fino a 5000
    public static final int CHIPS_MEDIUM_CMD = 3002;   // 5001-50000
    public static final int CHIPS_HIGH_CMD = 3003;     // 50001+

    // Custom Model Data per leve (2 stati)
    public static final int LEVER_UP_CMD = 4001;
    public static final int LEVER_DOWN_CMD = 4002;

    // Custom Model Data per il girasole (bottone GIRA) invisibile
    public static final int SUNFLOWER_INVISIBLE_CMD = 5001;

    // Chiave usata per marcare l'entità come NPC della roulette
    public static final String NPC_KEY_NAME = "roulette_npc";

    private final RoulettePlugin plugin;

    public RouletteCommand(RoulettePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo i giocatori possono eseguire questo comando!");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("spawn")) {
            if (!player.hasPermission("roulette.admin")) {
                player.sendMessage(Component.text("Non hai il permesso per usare questo comando!").color(TextColor.color(0xFF0000)));
                return true;
            }
            spawnNpc(player);
            player.sendMessage(Component.text("NPC roulette spawnato! Interagisci con tasto destro per aprire il menu.").color(TextColor.color(0x00FF00)));
            return true;
        }

        // Il menu della roulette non si apre più direttamente dalla chat:
        // è accessibile solo tramite tasto destro sull'NPC dedicato.
        player.sendMessage(Component.text("Questo comando non apre più il menu direttamente.").color(TextColor.color(0xFF5555)));
        player.sendMessage(Component.text("Interagisci (tasto destro) con l'NPC della roulette per aprire il menu.").color(TextColor.color(0xFFAA00)));
        if (player.hasPermission("roulette.admin")) {
            player.sendMessage(Component.text("Admin: usa /roulette spawn per creare l'NPC nella tua posizione.").color(TextColor.color(0x55AAFF)));
        }
        return true;
    }

    /**
     * Spawna un NPC invisibile (immobile, invulnerabile, silenzioso) nella posizione
     * del giocatore che ha eseguito il comando. L'NPC viene marcato tramite
     * PersistentDataContainer così il listener può riconoscerlo al tasto destro.
     */
    private void spawnNpc(Player player) {
        Location loc = player.getLocation();
        LivingEntity npc = (LivingEntity) player.getWorld().spawnEntity(loc, EntityType.VILLAGER);

        npc.setInvisible(true);
        npc.setInvulnerable(true);
        npc.setSilent(true);
        npc.setPersistent(true);
        npc.setRemoveWhenFarAway(false);
        npc.setCollidable(false);
        npc.setCustomNameVisible(false);
        npc.setAI(false);
        npc.setGravity(false);

        if (npc instanceof Villager villager) {
            villager.setVillagerType(Villager.Type.PLAINS);
            villager.setAgeLock(true);
            villager.setAdult();
        }

        NamespacedKey key = new NamespacedKey(plugin, NPC_KEY_NAME);
        npc.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    }

    /**
     * Apre la GUI della roulette per il giocatore. Chiamato dal listener quando
     * il giocatore fa tasto destro sull'NPC della roulette (mai dalla chat).
     */
    public static void openGui(RoulettePlugin plugin, Player player) {
        Inventory gui = Bukkit.createInventory(new RouletteHolder(), 45, GUI_TITLE);

        // Anello 5x5
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
                    gui.setItem(slot, createRingItem(mat));
                }
            }
        }

        // Girasole al centro
        ItemStack sunflower = new ItemStack(Material.SUNFLOWER);
        ItemMeta sunflowerMeta = sunflower.getItemMeta();
        if (sunflowerMeta != null) {
            sunflowerMeta.displayName(Component.text("GIRA!").color(TextColor.color(0xFFD700)));
            sunflowerMeta.setCustomModelData(SUNFLOWER_INVISIBLE_CMD);
            sunflower.setItemMeta(sunflowerMeta);
        }
        gui.setItem(CENTER_SLOT, sunflower);

        // Leve bet
        updateLevers(gui, player);

        // Blocchi scommessa (inizialmente mostra lana INVISIBILE per scommettere)
        updateBetDisplay(gui, null, 0, player);

        player.openInventory(gui);
    }

    static void updateLevers(Inventory gui, Player player) {
        int defaultBet = ((RoulettePlugin) player.getServer().getPluginManager().getPlugin("RoulettePlugin")).getPlayerDefaultBet(player.getUniqueId());
        String formatted = RoulettePlugin.formatBet(defaultBet);

        // Leva su (aumenta) - con texture custom
        ItemStack leverUp = new ItemStack(Material.LEVER);
        ItemMeta upMeta = leverUp.getItemMeta();
        if (upMeta != null) {
            upMeta.displayName(Component.text("AUMENTA BET").color(TextColor.color(0x00FF00)));
            upMeta.setCustomModelData(LEVER_UP_CMD);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Bet attuale: " + formatted).color(TextColor.color(0xFFD700)));
            upMeta.lore(lore);
            leverUp.setItemMeta(upMeta);
        }
        gui.setItem(LEVER_UP_SLOT, leverUp);

        // Leva giu (diminuisce) - con texture custom
        ItemStack leverDown = new ItemStack(Material.LEVER);
        ItemMeta downMeta = leverDown.getItemMeta();
        if (downMeta != null) {
            downMeta.displayName(Component.text("DIMINUISCI BET").color(TextColor.color(0xFF0000)));
            downMeta.setCustomModelData(LEVER_DOWN_CMD);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Bet attuale: " + formatted).color(TextColor.color(0xFFD700)));
            downMeta.lore(lore);
            leverDown.setItemMeta(downMeta);
        }
        gui.setItem(LEVER_DOWN_SLOT, leverDown);
    }

    /**
     * Crea un blocco di lana per l'anello della roulette.
     */
    static ItemStack createRingItem(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            if (mat == Material.BLACK_WOOL) {
                meta.setCustomModelData(RING_BLACK_CUSTOM_MODEL_DATA);
            } else if (mat == Material.RED_WOOL) {
                meta.setCustomModelData(RING_RED_CUSTOM_MODEL_DATA);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Restituisce il Custom Model Data corretto per le chips in base all'importo.
     */
    static int getChipsCustomModelData(int betAmount) {
        if (betAmount <= 5000) {
            return CHIPS_LOW_CMD;
        } else if (betAmount <= 50000) {
            return CHIPS_MEDIUM_CMD;
        } else {
            return CHIPS_HIGH_CMD;
        }
    }

    static void updateBetDisplay(Inventory gui, Material bet, int betAmount, Player player) {
        // NERO - slot 16
        if (bet == Material.BLACK_WOOL) {
            // Se c'e' una scommessa attiva sul nero, mostra il bottone warped con le chips
            ItemStack btn = new ItemStack(Material.WARPED_BUTTON);
            ItemMeta btnMeta = btn.getItemMeta();
            if (btnMeta != null) {
                btnMeta.displayName(Component.text("Chips").color(TextColor.color(0x00AAAA)));
                btnMeta.setCustomModelData(getChipsCustomModelData(betAmount));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Scommessa: NERO").color(TextColor.color(0x555555)));
                lore.add(Component.text(RoulettePlugin.formatBet(betAmount) + " chips scommesse").color(TextColor.color(0xFFD700)));
                btnMeta.lore(lore);
                btn.setItemMeta(btnMeta);
            }
            gui.setItem(BLACK_BET_SLOT, btn);
        } else {
            // Nessuna scommessa sul nero, mostra il blocco di concrete nera INVISIBILE
            ItemStack blackBlock = new ItemStack(Material.BLACK_CONCRETE);
            ItemMeta blackMeta = blackBlock.getItemMeta();
            if (blackMeta != null) {
                blackMeta.displayName(Component.text("Scommetti NERO").color(TextColor.color(0x555555)));
                blackMeta.setCustomModelData(INVISIBLE_BLACK_CONCRETE_CMD);
                blackBlock.setItemMeta(blackMeta);
            }
            gui.setItem(BLACK_BET_SLOT, blackBlock);
        }

        // ROSSO - slot 34
        if (bet == Material.RED_WOOL) {
            // Se c'e' una scommessa attiva sul rosso, mostra il bottone warped con le chips
            ItemStack btn = new ItemStack(Material.WARPED_BUTTON);
            ItemMeta btnMeta = btn.getItemMeta();
            if (btnMeta != null) {
                btnMeta.displayName(Component.text("Chips").color(TextColor.color(0x00AAAA)));
                btnMeta.setCustomModelData(getChipsCustomModelData(betAmount));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Scommessa: ROSSO").color(TextColor.color(0xFF0000)));
                lore.add(Component.text(RoulettePlugin.formatBet(betAmount) + " chips scommesse").color(TextColor.color(0xFFD700)));
                btnMeta.lore(lore);
                btn.setItemMeta(btnMeta);
            }
            gui.setItem(RED_BET_SLOT, btn);
        } else {
            // Nessuna scommessa sul rosso, mostra il blocco di concrete rossa INVISIBILE
            ItemStack redBlock = new ItemStack(Material.RED_CONCRETE);
            ItemMeta redMeta = redBlock.getItemMeta();
            if (redMeta != null) {
                redMeta.displayName(Component.text("Scommetti ROSSO").color(TextColor.color(0xFF0000)));
                redMeta.setCustomModelData(INVISIBLE_RED_CONCRETE_CMD);
                redBlock.setItemMeta(redMeta);
            }
            gui.setItem(RED_BET_SLOT, redBlock);
        }
    }

    /**
     * Mostra un errore sul bottone warped esistente aggiungendo una terza riga
     * alla lore in rosso "NON ABBASTANZA CHIPS". Non sostituisce il bottone.
     */
    static void showErrorOnButton(Inventory gui, int slot, int betAmount) {
        ItemStack item = gui.getItem(slot);
        if (item == null || item.getType() != Material.WARPED_BUTTON) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();

        // Rimuove eventuali messaggi di errore precedenti (terza riga)
        if (lore.size() >= 3) {
            lore.remove(2);
        }

        // Aggiunge la riga di errore in rosso
        lore.add(Component.text("NON ABBASTANZA CHIPS").color(TextColor.color(0xFF0000)));
        meta.lore(lore);
        item.setItemMeta(meta);
        gui.setItem(slot, item);
    }

    static void showError(Inventory gui, int slot) {
        Material baseMat = slot == BLACK_BET_SLOT ? Material.BLACK_CONCRETE : Material.RED_CONCRETE;
        ItemStack item = new ItemStack(baseMat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Non hai abbastanza chips!").color(TextColor.color(0xFF0000)));
            item.setItemMeta(meta);
        }
        gui.setItem(slot, item);
    }
}
