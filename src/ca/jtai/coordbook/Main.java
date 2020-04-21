package ca.jtai.coordbook;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;

public class Main extends JavaPlugin {
    private Book book;

    @Override
    public void onEnable() {
        ConfigurationSerialization.registerClass(Entry.class);

        book = new Book();
        int loaded = book.load(getBookFolder());
        getLogger().info("Loaded " + loaded + " entries.");

        getCommand("coordbook").setExecutor(new CoordBookCmd(book));

        // Save every 5 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                int saved = book.save(getBookFolder());
                if (saved != 0)
                    getLogger().info("Saved " + saved + " entries.");
            }
        }.runTaskTimer(this, 5 * 60 * 20, 5 * 60 * 20);
    }

    public void onDisable() {
        int saved = book.save(getBookFolder());
        if (saved != 0)
            getLogger().info("Saved " + saved + " entries.");
    }

    private File getBookFolder() {
        return new File(getDataFolder(), "coords");
    }
}
