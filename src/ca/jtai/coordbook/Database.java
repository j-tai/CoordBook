package ca.jtai.coordbook;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

/**
 * Manages the list of coordinates in the book.
 */
public class Database {
    private final HashMap<String, Book> map = new HashMap<>();
    private boolean dirty = false;

    public Book get(String world) {
        return map.computeIfAbsent(world, k -> new Book());
    }

    public void load(File folder) {
        folder.mkdirs();
        // Load coordinates
        File coordFolder = new File(folder, "coords");
        coordFolder.mkdirs();
        for (String filename : Objects.requireNonNull(coordFolder.list())) {
            if (!filename.endsWith(".yml"))
                continue;
            // Trim off ".yml"
            String worldName = filename.substring(0, filename.length() - 4);
            Book book = new Book();
            map.put(worldName, book);
            // Read the file
            File file = new File(coordFolder, filename);
            try {
                YamlConfiguration config = new YamlConfiguration();
                config.load(file);
                for (String key : config.getKeys(false)) {
                    book.put(key, config.getObject(key, Entry.class));
                }
            } catch (InvalidConfigurationException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void save(File folder) {
        if (!dirty)
            return;
        folder.mkdirs();
        // Save coordinates
        File coordFolder = new File(folder, "coords");
        coordFolder.mkdirs();
        for (String worldName : map.keySet()) {
            Book book = map.get(worldName);
            YamlConfiguration config = new YamlConfiguration();
            for (String name : book.getNames()) {
                Entry entry = book.get(name);
                config.set(name, entry);
            }
            config.options().indent(2);
            try {
                config.save(new File(coordFolder, worldName + ".yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dirty = false;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty() {
        this.dirty = true;
    }
}
