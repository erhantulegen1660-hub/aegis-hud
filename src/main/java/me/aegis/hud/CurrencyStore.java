package me.aegis.hud;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Встроенный баланс аметистов. Хранится в plugins/AegisHud/balances.yml. */
public final class CurrencyStore {

    private final File file;
    private final Logger log;
    private final Map<UUID, Long> balances = new HashMap<>();
    private boolean dirty;

    public CurrencyStore(File dataFolder, Logger log) {
        this.file = new File(dataFolder, "balances.yml");
        this.log = log;
    }

    public void load() {
        balances.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(false)) {
            try {
                balances.put(UUID.fromString(key), yml.getLong(key));
            } catch (IllegalArgumentException ignored) {
                // кривой ключ в файле, пропускаем
            }
        }
    }

    public void save() {
        if (!dirty) {
            return;
        }
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, Long> e : balances.entrySet()) {
            yml.set(e.getKey().toString(), e.getValue());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            yml.save(file);
            dirty = false;
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Не удалось сохранить balances.yml", ex);
        }
    }

    public long balance(UUID id) {
        return balances.getOrDefault(id, 0L);
    }

    public void set(UUID id, long amount) {
        balances.put(id, Math.max(0L, amount));
        dirty = true;
    }

    public void give(UUID id, long amount) {
        long current = balance(id);
        long next = current + amount;
        if (next < current) {
            next = Long.MAX_VALUE; // защита от переполнения
        }
        set(id, next);
    }

    /** Списывает аметисты. Возвращает false, если не хватает (тогда ничего не списывается). */
    public boolean take(UUID id, long amount) {
        long current = balance(id);
        if (current < amount) {
            return false;
        }
        set(id, current - amount);
        return true;
    }

    /** 1250 -> "1 250" */
    public static String format(long value) {
        return String.format(Locale.US, "%,d", value).replace(',', ' ');
    }
}
