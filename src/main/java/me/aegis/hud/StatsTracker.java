package me.aegis.hud;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Считает убийства и смерти в памяти. Сбрасывается командой /aegishud resetstats. */
public final class StatsTracker implements Listener {

    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        deaths.merge(dead.getUniqueId(), 1, Integer::sum);

        Player killer = dead.getKiller();
        if (killer != null && !killer.equals(dead)) {
            kills.merge(killer.getUniqueId(), 1, Integer::sum);
        }
    }

    public int kills(UUID id) {
        return kills.getOrDefault(id, 0);
    }

    public int deaths(UUID id) {
        return deaths.getOrDefault(id, 0);
    }

    public void reset() {
        kills.clear();
        deaths.clear();
    }
}
