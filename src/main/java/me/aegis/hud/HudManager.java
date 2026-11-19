package me.aegis.hud;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Рисует боковой скорборд без мерцания: каждая строка это команда (Team),
 * а её текст лежит в prefix, так что лимита в 16 символов нет и числа справа скрыты.
 */
public final class HudManager implements Listener {

    private static final int MAX_LINES = 15;

    private final AegisHud plugin;
    private final Placeholders placeholders;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final Map<UUID, PlayerHud> huds = new HashMap<>();
    private final Set<UUID> disabled = new HashSet<>();

    private BukkitTask task;
    private long runs;
    private long frame;

    private List<String> lines = new ArrayList<>();
    private String titleFormat;
    private boolean titleAnimated;
    private double titleStep;
    private int titleTicks;
    private int linesEveryRuns;
    private Set<String> worlds = new HashSet<>();

    public HudManager(AegisHud plugin, Placeholders placeholders) {
        this.plugin = plugin;
        this.placeholders = placeholders;
    }

    // ---------- жизненный цикл ----------

    public void start() {
        loadConfig();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, titleTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            removeHud(p);
        }
        huds.clear();
    }

    public void reload() {
        stop();
        plugin.reloadConfig();
        start();
    }

    private void loadConfig() {
        FileConfiguration cfg = plugin.getConfig();

        lines = new ArrayList<>(cfg.getStringList("lines"));
        if (lines.size() > MAX_LINES) {
            lines = new ArrayList<>(lines.subList(0, MAX_LINES));
        }

        titleFormat = cfg.getString("title.format", "<bold>AEGIS</bold>");
        titleAnimated = cfg.getBoolean("title.animated", true);
        titleStep = Math.max(0.01, cfg.getDouble("title.step", 0.05));
        titleTicks = Math.max(1, cfg.getInt("title.speed-ticks", 2));

        int linesTicks = Math.max(1, cfg.getInt("lines-update-ticks", 10));
        linesEveryRuns = Math.max(1, linesTicks / titleTicks);

        worlds = new HashSet<>(cfg.getStringList("enabled-worlds"));
    }

    // ---------- основной цикл ----------

    private void tick() {
        runs++;
        frame++;
        boolean updateLines = runs % linesEveryRuns == 0;

        String titleStr = titleString();
        Component title = mm.deserialize(titleStr);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isActive(p)) {
                removeHud(p);
                continue;
            }

            PlayerHud hud = huds.get(p.getUniqueId());
            boolean fresh = false;
            if (hud == null) {
                hud = createHud(p);
                fresh = true;
            }

            if (!titleStr.equals(hud.lastTitle)) {
                hud.objective.displayName(title);
                hud.lastTitle = titleStr;
            }

            if (fresh || updateLines) {
                renderLines(p, hud);
            }
        }
    }

    private String titleString() {
        double phase = titleAnimated ? -1.0 + ((frame * titleStep) % 2.0) : 0.0;
        return titleFormat.replace("{phase}", String.format(Locale.US, "%.2f", phase));
    }

    private void renderLines(Player p, PlayerHud hud) {
        for (int i = 0; i < lines.size(); i++) {
            String resolved = placeholders.apply(p, lines.get(i));
            if (resolved.equals(hud.lastLines[i])) {
                continue; // ничего не изменилось, пакет не шлём
            }
            hud.lastLines[i] = resolved;
            hud.teams[i].prefix(resolved.isEmpty() ? Component.empty() : mm.deserialize(resolved));
        }
    }

    // ---------- создание и удаление ----------

    private PlayerHud createHud(Player p) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective objective = board.registerNewObjective("aegis_hud", Criteria.DUMMY, Component.empty());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank()); // прячем красные цифры справа

        Team[] teams = new Team[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            // уникальная невидимая запись для каждой строки
            String entry = "\u00a7" + Integer.toHexString(i) + "\u00a7r";
            Team team = board.registerNewTeam("l" + i);
            team.addEntry(entry);
            objective.getScore(entry).setScore(lines.size() - i);
            teams[i] = team;
        }

        p.setScoreboard(board);

        PlayerHud hud = new PlayerHud(objective, teams);
        huds.put(p.getUniqueId(), hud);
        return hud;
    }

    private void removeHud(Player p) {
        PlayerHud hud = huds.remove(p.getUniqueId());
        if (hud != null && p.isOnline()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private boolean isActive(Player p) {
        if (disabled.contains(p.getUniqueId())) {
            return false;
        }
        return worlds.isEmpty() || worlds.contains(p.getWorld().getName());
    }

    /** Возвращает true, если скорборд теперь включён. */
    public boolean toggle(Player p) {
        if (disabled.remove(p.getUniqueId())) {
            return true;
        }
        disabled.add(p.getUniqueId());
        removeHud(p);
        return false;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        huds.remove(event.getPlayer().getUniqueId());
    }

    private static final class PlayerHud {
        final Objective objective;
        final Team[] teams;
        final String[] lastLines;
        String lastTitle;

        PlayerHud(Objective objective, Team[] teams) {
            this.objective = objective;
            this.teams = teams;
            this.lastLines = new String[teams.length];
        }
    }
}
