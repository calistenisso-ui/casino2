package com.example.roulette;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChipsCommand implements CommandExecutor {

    private final RoulettePlugin plugin;

    public ChipsCommand(RoulettePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Solo i giocatori possono vedere le proprie chips!");
                return true;
            }
            int chips = plugin.getChips(player.getUniqueId());
            player.sendMessage(Component.text("Hai ")
                    .color(TextColor.color(0xFFD700))
                    .append(Component.text(RoulettePlugin.formatBet(chips) + " chips").color(TextColor.color(0x00FF00)))
                    .append(Component.text("!").color(TextColor.color(0xFFD700))));
            return true;
        }

        if (args.length == 3) {
            if (!sender.hasPermission("roulette.chips.admin")) {
                sender.sendMessage(Component.text("Non hai permesso!").color(TextColor.color(0xFF0000)));
                return true;
            }

            String action = args[0].toLowerCase();
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Giocatore non trovato!").color(TextColor.color(0xFF0000)));
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Numero non valido!").color(TextColor.color(0xFF0000)));
                return true;
            }

            if (amount < 0) {
                sender.sendMessage(Component.text("La quantita' deve essere positiva!").color(TextColor.color(0xFF0000)));
                return true;
            }

            switch (action) {
                case "add" -> {
                    plugin.addChips(target.getUniqueId(), amount);
                    sender.sendMessage(Component.text("Aggiunte ")
                            .color(TextColor.color(0x00FF00))
                            .append(Component.text(RoulettePlugin.formatBet(amount) + " chips").color(TextColor.color(0xFFD700)))
                            .append(Component.text(" a ").color(TextColor.color(0x00FF00)))
                            .append(Component.text(target.getName()).color(TextColor.color(0x00AAFF))));
                }
                case "subtract" -> {
                    plugin.subtractChips(target.getUniqueId(), amount);
                    int remaining = plugin.getChips(target.getUniqueId());
                    sender.sendMessage(Component.text("Rimosse ")
                            .color(TextColor.color(0xFF0000))
                            .append(Component.text(RoulettePlugin.formatBet(amount) + " chips").color(TextColor.color(0xFFD700)))
                            .append(Component.text(" a ").color(TextColor.color(0xFF0000)))
                            .append(Component.text(target.getName()).color(TextColor.color(0x00AAFF)))
                            .append(Component.text(" (rimaste: " + RoulettePlugin.formatBet(remaining) + ")").color(TextColor.color(0xAAAAAA))));
                }
                default -> {
                    sender.sendMessage(Component.text("Uso: /chips [add|subtract] <player> <amount>").color(TextColor.color(0xFF0000)));
                }
            }
            return true;
        }

        sender.sendMessage(Component.text("Uso: /chips o /chips add/subtract <player> <amount>").color(TextColor.color(0xFF0000)));
        return true;
    }
}
