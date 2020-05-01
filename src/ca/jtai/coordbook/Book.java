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
    private final ArrayList<String> pinned = new ArrayList<>();
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
        Entry entry = map.remove(name);
        if (entry == null) return false;
        if (entry.isPinned()) {
            pinned.remove(name);
        }
        return true;
    }

    public void clear() {
        dirty = true;
        pinned.clear();
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

    public List<String> getPinned() {
        return pinned;
    }

    public boolean togglePinned(String name) {
        Entry entry = get(name);
        if (entry == null)
            throw new IllegalArgumentException("No such entry '" + name + "'");
        dirty = true;
        if (entry.isPinned()) {
            entry.setPinned(false);
            pinned.remove(name);
            return false;
        } else {
            entry.setPinned(true);
            pinned.add(name);
            return true;
        }
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
                    pinned.addAll(config.getStringList("pinned"));
                    for (String name : pinned) {
                        get(name).setPinned(true);
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
        config.set("pinned", pinned);

        config.options().indent(2);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        dirty = false;
    }
}
