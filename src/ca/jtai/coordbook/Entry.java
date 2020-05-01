package ca.jtai.coordbook;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * An entry in the coordinate book.
 */
public class Entry implements ConfigurationSerializable {
    private final int x, y, z;
    private final long dateAdded;
    private final UUID author;
    private boolean isPinned;

    private Entry(int x, int y, int z, long dateAdded, UUID author) {
        Objects.requireNonNull(author);
        this.x = x;
        this.y = y;
        this.z = z;
        this.dateAdded = dateAdded;
        this.author = author;
    }

    public Entry(int x, int y, int z, UUID author) {
        this(x, y, z, System.currentTimeMillis(), author);
    }

    public Entry(Location location, UUID author) {
        this(location.getBlockX(), location.getBlockY(), location.getBlockZ(), author);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public UUID getAuthor() {
        return author;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entry entry = (Entry) o;
        return x == entry.x &&
                y == entry.y &&
                z == entry.z &&
                dateAdded == entry.dateAdded &&
                author.equals(entry.author);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, dateAdded, author);
    }

    @Override
    public Map<String, Object> serialize() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        map.put("date", dateAdded);
        map.put("by", author.toString());
        return map;
    }

    public static Entry deserialize(Map<String, Object> map) {
        return new Entry(
                (Integer) map.get("x"),
                (Integer) map.get("y"),
                (Integer) map.get("z"),
                (Long) map.get("date"),
                UUID.fromString((String) map.get("by"))
        );
    }
}
