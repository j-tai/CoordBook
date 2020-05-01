package ca.jtai.coordbook;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A world-local coordinate book.
 */
public class Book {
    private final HashMap<String, Entry> map = new HashMap<>();
    private boolean dirty = true;

    public Entry get(String name) {
        return map.get(name);
    }

    public void put(String name, Entry entry) {
        dirty = true;
        map.put(name, entry);
    }

    public boolean remove(String name) {
        dirty = true;
        return map.remove(name) != null;
    }

    public void clear() {
        dirty = true;
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
            config.options().pathSeparator('\0');

            int version = config.getInt("version", 0);
            switch (version) {
                case 0:
                    for (String key : config.getKeys(false)) {
                        put(key, config.getObject(key, Entry.class));
                    }
                    break;
                case 1:
                    for (Map<?, ?> map : config.getMapList("entries")) {
                        put((String) map.get("name"), (Entry) map.get("entry"));
                    }
                    break;
                default:
                    throw new IOException("Unknown version number " + version);
            }
        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace();
        }
        dirty = false;
    }

    public void save(File file) {
        if (!dirty)
            return;
        YamlConfiguration config = new YamlConfiguration();
        config.options().pathSeparator('\0');

        config.set("version", 1);
        // Save entries
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Map.Entry<String, Entry> entry : getEntries()) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("name", entry.getKey());
            map.put("entry", entry.getValue());
            entries.add(map);
        }
        config.set("entries", entries);

        config.options().indent(2);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        dirty = false;
    }
}
