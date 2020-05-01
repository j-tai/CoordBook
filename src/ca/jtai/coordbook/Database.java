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
            book.load(new File(coordFolder, filename));
            map.put(worldName, book);
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
            book.save(new File(coordFolder, worldName + ".yml"));
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
