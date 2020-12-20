package ca.jtai.coordbook;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Main extends JavaPlugin {
    private Database database;

    @Override
    public void onEnable() {
        ConfigurationSerialization.registerClass(Entry.class);

        database = new Database();
        database.load(getDataFolder());
        getLogger().info("Data loaded successfully.");

        getCommand("coordbook").setExecutor(new CoordBookCmd(database));

        // Save every 5 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                database.save(getDataFolder());
            }
        }.runTaskTimer(this, 5 * 60 * 20, 5 * 60 * 20);
    }

    public void onDisable() {
        database.save(getDataFolder());
        getLogger().info("Data saved successfully.");
    }
}
