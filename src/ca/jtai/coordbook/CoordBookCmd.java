package ca.jtai.coordbook;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Map;

public class CoordBookCmd implements CommandExecutor {
    private final Book book;

    public CoordBookCmd(Book book) {
        this.book = book;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only in-game players may use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            list(player, label, args);
        } else {
            String subcommand = args[0];
            String[] subargs = Arrays.copyOfRange(args, 1, args.length);
            if ("list".equals(subcommand))
                list(player, label, subargs);
            else if ("add".equals(subcommand))
                add(player, label, subargs);
            else if ("remove".equals(subcommand))
                remove(player, label, subargs);
            else
                help(player, label);
        }
        return true;
    }

    private boolean checkPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(ChatColor.RED + "Sorry, you don't have permission to do that.");
            return false;
        }
        return true;
    }

    private void help(Player player, String label) {
        player.sendMessage(ChatColor.RED + "Usage:");
        player.sendMessage(ChatColor.RED + "    /" + label + " [list]");
        player.sendMessage(ChatColor.RED + "    /" + label + " add NAME");
        player.sendMessage(ChatColor.RED + "    /" + label + " remove NAME");
    }

    private static TextComponent colored(ChatColor color, String text) {
        TextComponent component = new TextComponent(text);
        component.setColor(color);
        return component;
    }

    private void list(Player player, String label, String[] args) {
        Map<String, Entry> entries = book.get(player.getWorld().getName());
        if (entries.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "This world's coordinate book is empty.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "----- Coordinate Book -----");
        Location location = player.getLocation();

        entries.entrySet()
                .stream()
                .sorted((left, right) -> {
                    Location leftLoc = left.getValue().toLocation(location.getWorld());
                    Location rightLoc = right.getValue().toLocation(location.getWorld());
                    return Double.compare(location.distanceSquared(rightLoc), location.distanceSquared(leftLoc));
                })
                .forEach(ent -> {
                    String name = ent.getKey();
                    Entry entry = ent.getValue();
                    BaseComponent msg = new TextComponent();
                    // Delete button
                    BaseComponent msgDelete = colored(ChatColor.RED, "x");
                    msgDelete.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new TextComponent[]{new TextComponent("Delete entry")}));
                    msgDelete.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                            "/" + label + " remove " + name));
                    msg.addExtra(msgDelete);
                    // Space
                    msg.addExtra(" ");
                    // Name of the entry
                    BaseComponent msgName = colored(ChatColor.YELLOW, name);
                    String author = Bukkit.getServer().getOfflinePlayer(entry.getAuthor()).getName();
                    if (author == null)
                        author = entry.getAuthor().toString(); // Display the raw UUID
                    msgName.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new TextComponent[]{new TextComponent("Added by " + author)}));
                    msg.addExtra(msgName);
                    // Coordinates
                    msg.addExtra(": ");
                    msg.addExtra(entry.getX() + ", " + entry.getY() + ", " + entry.getZ());
                    // Distance
                    msg.addExtra(" ");
                    msg.addExtra(colored(ChatColor.DARK_RED, "("));
                    int dist = (int) Math.round(location.distance(entry.toLocation(location.getWorld())));
                    msg.addExtra(colored(ChatColor.RED, dist + "m"));
                    msg.addExtra(colored(ChatColor.DARK_RED, ")"));
                    player.spigot().sendMessage(msg);
                });
    }

    private void add(Player player, String label, String[] args) {
        if (args.length == 0) {
            help(player, label);
            return;
        }
        if (!checkPermission(player, "coordbook.add"))
            return;
        String name = String.join(" ", args);
        Map<String, Entry> entries = book.get(player.getWorld().getName());
        if (entries.containsKey(name)) {
            player.sendMessage(ChatColor.RED + "An entry with the name '" + name + "' already exists.");
            return;
        }
        entries.put(name, new Entry(player.getLocation(), player.getUniqueId()));
        book.setDirty();
        player.sendMessage(ChatColor.GREEN + "Added an entry '" + name + "' at your current location.");
    }

    private void remove(Player player, String label, String[] args) {
        if (args.length == 0) {
            help(player, label);
            return;
        }
        if (!checkPermission(player, "coordbook.remove"))
            return;
        String name = String.join(" ", args);
        Map<String, Entry> entries = book.get(player.getWorld().getName());
        Entry entry = entries.get(name);
        if (entry == null) {
            player.sendMessage(ChatColor.RED + "The entry '" + name + "' does not exist.");
            return;
        }
        if (!player.getUniqueId().equals(entry.getAuthor())) {
            if (!checkPermission(player, "coordbook.remove.other"))
                return;
        }
        entries.remove(name);
        book.setDirty();
        player.sendMessage(ChatColor.GREEN + "Removed the entry '" + name + "'.");
    }
}
