package me.aegis.hud;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class AegisHud extends JavaPlugin {

    private StatsTracker stats;
    private CurrencyStore currency;
    private HudManager hud;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        stats = new StatsTracker();
        currency = new CurrencyStore(getDataFolder(), getLogger());
        currency.load();

        Placeholders placeholders = new Placeholders(this, stats, currency);
        hud = new HudManager(this, placeholders);

        getServer().getPluginManager().registerEvents(stats, this);
        getServer().getPluginManager().registerEvents(hud, this);

        HudCommand hudCommand = new HudCommand(this);
        PluginCommand hudPc = getCommand("aegishud");
        if (hudPc != null) {
            hudPc.setExecutor(hudCommand);
            hudPc.setTabCompleter(hudCommand);
        }

        AmethystCommand amethystCommand = new AmethystCommand(this);
        PluginCommand amePc = getCommand("amethyst");
        if (amePc != null) {
            amePc.setExecutor(amethystCommand);
            amePc.setTabCompleter(amethystCommand);
        }

        // автосохранение балансов раз в 30 секунд (только если что-то менялось)
        getServer().getScheduler().runTaskTimer(this, currency::save, 20L * 30, 20L * 30);

        hud.start();
    }

    @Override
    public void onDisable() {
        if (hud != null) {
            hud.stop();
        }
        if (currency != null) {
            currency.save();
        }
    }

    public HudManager hud() {
        return hud;
    }

    public StatsTracker stats() {
        return stats;
    }

    public CurrencyStore currency() {
        return currency;
    }
}
