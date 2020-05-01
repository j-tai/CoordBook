package ca.jtai.coordbook;

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
}
