package ca.jtai.coordbook;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Main extends JavaPlugin {
    private Book book;

    @Override
    public void onEnable() {
        ConfigurationSerialization.registerClass(Entry.class);

        book = new Book();
        book.load(getDataFolder());
        getLogger().info("Data loaded successfully.");

        getCommand("coordbook").setExecutor(new CoordBookCmd(book));

        // Save every 5 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                book.save(getDataFolder());
            }
        }.runTaskTimer(this, 5 * 60 * 20, 5 * 60 * 20);
    }

    public void onDisable() {
        book.save(getDataFolder());
        getLogger().info("Data saved successfully.");
    }
}
