package ca.jtai.coordbook;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Manages the list of coordinates in the book.
 */
public class Book {
    private final HashMap<String, HashMap<String, Entry>> map = new HashMap<>();
    private boolean dirty = false;

    public Map<String, Entry> get(String world) {
        HashMap<String, Entry> w = map.get(world);
        if (w == null) {
            w = new HashMap<>();
            map.put(world, w);
        }
        return w;
    }

    public int load(File folder) {
        folder.mkdirs();
        int count = 0;
        for (String filename : Objects.requireNonNull(folder.list())) {
            if (!filename.endsWith(".yml"))
                continue;
            // Trim off ".yml"
            String worldName = filename.substring(0, filename.length() - 4);
            HashMap<String, Entry> world = new HashMap<>();
            map.put(worldName, world);
            // Read the file
            File file = new File(folder, filename);
            try {
                YamlConfiguration config = new YamlConfiguration();
                config.load(file);
                for (String key : config.getKeys(false)) {
                    world.put(key, config.getObject(key, Entry.class));
                    count++;
                }
            } catch (InvalidConfigurationException | IOException e) {
                e.printStackTrace();
            }
        }
        return count;
    }

    public int save(File folder) {
        if (!dirty)
            return 0;
        folder.mkdirs();
        int count = 0;
        for (String worldName : map.keySet()) {
            HashMap<String, Entry> world = map.get(worldName);
            YamlConfiguration config = new YamlConfiguration();
            for (String name : world.keySet()) {
                Entry entry = world.get(name);
                config.set(name, entry);
                count++;
            }
            config.options().indent(2);
            try {
                config.save(new File(folder, worldName + ".yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dirty = false;
        return count;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty() {
        this.dirty = true;
    }
}
