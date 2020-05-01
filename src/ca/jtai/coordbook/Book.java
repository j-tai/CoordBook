package ca.jtai.coordbook;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A world-local coordinate book.
 */
public class Book {
    private final HashMap<String, Entry> map = new HashMap<>();

    public Entry get(String name) {
        return map.get(name);
    }

    public void put(String name, Entry entry) {
        map.put(name, entry);
    }

    public boolean remove(String name) {
        return map.remove(name) != null;
    }

    public void clear() {
        map.clear();
    }

    public boolean has(String name) {
        return map.containsKey(name);
    }

    public Set<String> getNames() {
        return map.keySet();
    }

    public Set<Map.Entry<String, Entry>> getEntries() {
        return map.entrySet();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public void load(File file) {
        clear();
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.load(file);
            for (String key : config.getKeys(false)) {
                put(key, config.getObject(key, Entry.class));
            }
        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace();
        }
    }

    public void save(File file) {
        YamlConfiguration config = new YamlConfiguration();
        for (String name : getNames()) {
            Entry entry = get(name);
            config.set(name, entry);
        }
        config.options().indent(2);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
