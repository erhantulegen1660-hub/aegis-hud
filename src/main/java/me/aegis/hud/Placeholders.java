package me.aegis.hud;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Supplier;

/** Подставляет встроенные плейсхолдеры, а затем (если установлен) PlaceholderAPI. */
public final class Placeholders {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final AegisHud plugin;
    private final StatsTracker stats;
    private final CurrencyStore currency;
    private final Method papiMethod;

    public Placeholders(AegisHud plugin, StatsTracker stats, CurrencyStore currency) {
        this.plugin = plugin;
        this.stats = stats;
        this.currency = currency;

        Method method = null;
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                method = Class.forName("me.clip.placeholderapi.PlaceholderAPI")
                        .getMethod("setPlaceholders", Player.class, String.class);
            } catch (ReflectiveOperationException ignored) {
                // PlaceholderAPI не нашёлся, работаем без него
            }
        }
        this.papiMethod = method;
    }

    public String apply(Player p, String text) {
        if (text.indexOf('%') < 0) {
            return text;
        }

        text = rep(text, "%player%", p::getName);
        text = rep(text, "%ping%", () -> String.valueOf(p.getPing()));
        text = rep(text, "%ping_color%", () -> pingColor(p.getPing()));
        text = rep(text, "%amethysts%", () -> amethysts(p));
        text = rep(text, "%online%", () -> String.valueOf(Bukkit.getOnlinePlayers().size()));
        text = rep(text, "%max%", () -> String.valueOf(Bukkit.getMaxPlayers()));
        text = rep(text, "%world%", () -> p.getWorld().getName());
        text = rep(text, "%time%", () -> LocalTime.now().format(TIME));
        text = rep(text, "%date%", () -> LocalDate.now().format(DATE));
        text = rep(text, "%tps%", () -> String.format(Locale.US, "%.1f", Math.min(20.0, Bukkit.getTPS()[0])));
        text = rep(text, "%kills%", () -> String.valueOf(stats.kills(p.getUniqueId())));
        text = rep(text, "%deaths%", () -> String.valueOf(stats.deaths(p.getUniqueId())));
        text = rep(text, "%alive%", () -> String.valueOf(alive(p)));
        text = rep(text, "%border%", () -> String.valueOf(Math.round(p.getWorld().getWorldBorder().getSize())));
        text = rep(text, "%border_dist%", () -> String.valueOf(borderDistance(p)));
        text = rep(text, "%coords%", () -> {
            Location l = p.getLocation();
            return l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ();
        });

        return papi(p, text);
    }

    private String papi(Player p, String text) {
        if (papiMethod == null || text.indexOf('%') < 0) {
            return text;
        }
        try {
            return (String) papiMethod.invoke(null, p, text);
        } catch (ReflectiveOperationException ignored) {
            return text;
        }
    }

    private String amethysts(Player p) {
        String source = plugin.getConfig().getString("currency.source", "internal");
        if (source.equalsIgnoreCase("internal")) {
            return CurrencyStore.format(currency.balance(p.getUniqueId()));
        }
        if (papiMethod == null) {
            return "-";
        }
        String value = papi(p, source);
        // если плейсхолдер не распознан, PlaceholderAPI оставляет его как есть
        return value.indexOf('%') >= 0 ? "-" : value;
    }

    private static String pingColor(int ping) {
        if (ping < 80) {
            return "<green>";
        }
        if (ping < 150) {
            return "<yellow>";
        }
        return "<red>";
    }

    private static String rep(String text, String key, Supplier<String> value) {
        return text.contains(key) ? text.replace(key, value.get()) : text;
    }

    private static int alive(Player p) {
        int count = 0;
        for (Player other : p.getWorld().getPlayers()) {
            if (other.getGameMode() == GameMode.SURVIVAL) {
                count++;
            }
        }
        return count;
    }

    private static int borderDistance(Player p) {
        WorldBorder border = p.getWorld().getWorldBorder();
        Location center = border.getCenter();
        Location loc = p.getLocation();
        double half = border.getSize() / 2.0;
        double dist = half - Math.max(Math.abs(loc.getX() - center.getX()), Math.abs(loc.getZ() - center.getZ()));
        return (int) Math.max(0, Math.floor(dist));
    }
}
