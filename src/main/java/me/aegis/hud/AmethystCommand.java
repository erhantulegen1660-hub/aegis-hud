package me.aegis.hud;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class AmethystCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of("balance", "give", "take", "set");
    private static final String ADMIN = "aegishud.admin";

    private final AegisHud plugin;

    public AmethystCommand(AegisHud plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "balance" : args[0].toLowerCase(Locale.ROOT);
        String currencyName = plugin.getConfig().getString("currency.name", "Аметисты");
        CurrencyStore store = plugin.currency();

        switch (sub) {
            case "balance", "bal" -> showBalance(sender, args, currencyName, store);
            case "give", "take", "set" -> changeBalance(sender, sub, args, currencyName, store);
            default -> send(sender, "/amethyst [balance | give | take | set] ...", NamedTextColor.GRAY);
        }
        return true;
    }

    private void showBalance(CommandSender sender, String[] args, String name, CurrencyStore store) {
        boolean self = args.length < 2;
        OfflinePlayer target;

        if (!self) {
            if (!sender.hasPermission(ADMIN)) {
                send(sender, "Нет прав.", NamedTextColor.RED);
                return;
            }
            target = find(args[1]);
            if (target == null) {
                send(sender, "Игрок не найден.", NamedTextColor.RED);
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            send(sender, "Укажи ник игрока.", NamedTextColor.RED);
            return;
        }

        String amount = CurrencyStore.format(store.balance(target.getUniqueId()));
        String text = self
                ? name + ": ♦ " + amount
                : name + " у " + (target.getName() != null ? target.getName() : args[1]) + ": ♦ " + amount;
        send(sender, text, NamedTextColor.LIGHT_PURPLE);
    }

    private void changeBalance(CommandSender sender, String sub, String[] args, String name, CurrencyStore store) {
        if (!sender.hasPermission(ADMIN)) {
            send(sender, "Нет прав.", NamedTextColor.RED);
            return;
        }
        if (args.length < 3) {
            send(sender, "/amethyst " + sub + " <ник> <количество>", NamedTextColor.GRAY);
            return;
        }

        OfflinePlayer target = find(args[1]);
        if (target == null) {
            send(sender, "Игрок не найден.", NamedTextColor.RED);
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException ex) {
            send(sender, "Количество должно быть числом.", NamedTextColor.RED);
            return;
        }
        if (amount < 0) {
            send(sender, "Количество не может быть отрицательным.", NamedTextColor.RED);
            return;
        }

        UUID id = target.getUniqueId();
        switch (sub) {
            case "give" -> store.give(id, amount);
            case "take" -> {
                if (!store.take(id, amount)) {
                    send(sender, "У игрока недостаточно: " + CurrencyStore.format(store.balance(id)), NamedTextColor.RED);
                    return;
                }
            }
            default -> store.set(id, amount);
        }

        send(sender, name + " " + args[1] + ": ♦ " + CurrencyStore.format(store.balance(id)), NamedTextColor.GREEN);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String s : SUBS) {
                boolean allowed = s.equals("balance") || sender.hasPermission(ADMIN);
                if (allowed && s.startsWith(prefix)) {
                    out.add(s);
                }
            }
        } else if (args.length == 2 && sender.hasPermission(ADMIN)) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }

    private static OfflinePlayer find(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    private static void send(CommandSender sender, String text, NamedTextColor color) {
        sender.sendMessage(Component.text(text, color));
    }
}
