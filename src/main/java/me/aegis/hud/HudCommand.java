package me.aegis.hud;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HudCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of("reload", "toggle", "resetstats");

    private final AegisHud plugin;

    public HudCommand(AegisHud plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("aegishud.admin")) {
                    return deny(sender);
                }
                plugin.hud().reload();
                send(sender, "Конфиг перезагружен.", NamedTextColor.GREEN);
            }
            case "toggle" -> {
                if (!(sender instanceof Player player)) {
                    send(sender, "Эта команда только для игроков.", NamedTextColor.RED);
                    return true;
                }
                if (!player.hasPermission("aegishud.toggle")) {
                    return deny(sender);
                }
                boolean on = plugin.hud().toggle(player);
                send(sender, on ? "Скорборд включён." : "Скорборд выключен.", NamedTextColor.GREEN);
            }
            case "resetstats" -> {
                if (!sender.hasPermission("aegishud.admin")) {
                    return deny(sender);
                }
                plugin.stats().reset();
                send(sender, "Статистика убийств и смертей сброшена.", NamedTextColor.GREEN);
            }
            default -> send(sender, "/aegishud reload | toggle | resetstats", NamedTextColor.GRAY);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String s : SUBS) {
            if (s.startsWith(prefix)) {
                out.add(s);
            }
        }
        return out;
    }

    private static boolean deny(CommandSender sender) {
        send(sender, "Нет прав.", NamedTextColor.RED);
        return true;
    }

    private static void send(CommandSender sender, String text, NamedTextColor color) {
        sender.sendMessage(Component.text(text, color));
    }
}
