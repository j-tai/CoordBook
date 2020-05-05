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
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CoordBookCmd implements CommandExecutor, TabCompleter {
    private static final int ENTRIES_PER_PAGE = 10;

    private final Database database;

    public CoordBookCmd(Database database) {
        this.database = database;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only in-game players may use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            list(player, label, "list", args, false);
        } else {
            String subcommand = args[0];
            String[] subargs = Arrays.copyOfRange(args, 1, args.length);
            if ("list".equals(subcommand))
                list(player, label, subcommand, subargs, false);
            else if ("edit".equals(subcommand))
                list(player, label, subcommand, subargs, true);
            else if ("add".equals(subcommand))
                add(player, label, subargs);
            else if ("rename".equals(subcommand))
                rename(player, label, subargs);
            else if ("remove".equals(subcommand))
                remove(player, label, subargs);
            else if ("toggle-pin".equals(subcommand))
                togglePin(player, label, subargs);
            else if ("swap-pin".equals(subcommand))
                swapPin(player, label, subargs);
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
        player.sendMessage(ChatColor.RED + "    /" + label + " edit");
        player.sendMessage(ChatColor.RED + "    /" + label + " add NAME");
        player.sendMessage(ChatColor.RED + "    /" + label + " remove NAME");
    }

    private static TextComponent colored(ChatColor color, String text) {
        TextComponent component = new TextComponent(text);
        component.setColor(color);
        return component;
    }

    private static String formatDistance(int meters) {
        if (meters >= 10000) {
            // Use %.0f instead of %d to round to nearest km instead of truncating
            return String.format("%.0fkm", meters / 1000.0);
        } else if (meters >= 1000) {
            return String.format("%.1fkm", meters / 1000.0);
        } else {
            return meters + "m";
        }
    }

    private void list(Player player, String label, String sublabel, String[] args, boolean showEditButtons) {
        Book book = database.get(player.getWorld().getName());
        if (book.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "This world's coordinate book is empty.");
            return;
        }

        int totalPages = (book.size() + (ENTRIES_PER_PAGE - 1)) / ENTRIES_PER_PAGE;
        int page = 0;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]) - 1;
            } catch (NumberFormatException ignored) {
            }
            if (page < 0 || page >= totalPages) {
                page = 0;
            }
        }

        // Send the Coordinate Book heading
        {
            BaseComponent heading = new TextComponent("----- Coordinate Book ");
            heading.setColor(ChatColor.GOLD);
            BaseComponent leftArrow = new TextComponent("←");
            if (page != 0) {
                leftArrow.setColor(ChatColor.AQUA);
                String prevPageCommand = "/" + label + " " + sublabel + " " + page;
                leftArrow.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, prevPageCommand));
                leftArrow.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new TextComponent[]{new TextComponent("Go to the previous page")}));
            } else {
                leftArrow.setColor(ChatColor.GRAY);
            }
            heading.addExtra(leftArrow);
            heading.addExtra(" " + (page + 1) + "/" + totalPages + " ");
            BaseComponent rightArrow = new TextComponent("→");
            if (page != totalPages - 1) {
                rightArrow.setColor(ChatColor.AQUA);
                String nextPageCommand = "/" + label + " " + sublabel + " " + (page + 2);
                rightArrow.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, nextPageCommand));
                rightArrow.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new TextComponent[]{new TextComponent("Go to the next page")}));
            } else {
                rightArrow.setColor(ChatColor.GRAY);
            }
            heading.addExtra(rightArrow);
            heading.addExtra(" -----");
            player.spigot().sendMessage(heading);
        }

        Location location = player.getLocation();

        List<String> entries = Stream.concat(
                book.getPinned().stream(),
                book.getEntries()
                        .stream()
                        .filter(ent -> !ent.getValue().isPinned())
                        .sorted((left, right) -> {
                            Location leftLoc = left.getValue().toLocation(location.getWorld());
                            Location rightLoc = right.getValue().toLocation(location.getWorld());
                            return Double.compare(location.distanceSquared(leftLoc), location.distanceSquared(rightLoc));
                        })
                        .map(Map.Entry::getKey)
        ).collect(Collectors.toList());
        for (int i = page * ENTRIES_PER_PAGE; i < Math.min((page + 1) * ENTRIES_PER_PAGE, entries.size()); i++) {
            String name = entries.get(i);
            Entry entry = book.get(name);
            BaseComponent msg = new TextComponent();
            if (showEditButtons) {
                // Delete button
                BaseComponent deleteButton = colored(ChatColor.RED, "x");
                deleteButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new TextComponent[]{new TextComponent("Delete entry")}));
                deleteButton.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/" + label + " remove " + name));
                msg.addExtra(deleteButton);
                // Pin button
                BaseComponent pinButton = colored(entry.isPinned() ? ChatColor.GREEN : ChatColor.DARK_GRAY, "+");
                pinButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new TextComponent[]{new TextComponent(entry.isPinned() ? "Unpin entry" : "Pin entry")}));
                pinButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/" + label + " toggle-pin " + (page + 1) + " " + name));
                msg.addExtra(pinButton);
                // Up/down arrows to change order of pinned
                if (entry.isPinned()) {
                    BaseComponent upButton = colored(i == 0 ? ChatColor.DARK_GRAY : ChatColor.AQUA, "↑");
                    if (i != 0) {
                        upButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/" + label + " swap-pin " + i + " " + (i - 1) + " " + String.join(" ", args)));
                    }
                    msg.addExtra(upButton);
                    boolean isLastPinned = i == book.getPinned().size() - 1;
                    BaseComponent downButton = colored(isLastPinned ? ChatColor.DARK_GRAY : ChatColor.AQUA, "↓");
                    if (!isLastPinned) {
                        downButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/" + label + " swap-pin " + i + " " + (i + 1) + " " + String.join(" ", args)));
                    }
                    msg.addExtra(downButton);
                }
                // Space
                msg.addExtra(" ");
            }
            // Name of the entry
            BaseComponent msgName = colored(entry.isPinned() ? ChatColor.GREEN : ChatColor.YELLOW, name);
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
            msg.addExtra(colored(ChatColor.RED, formatDistance(dist)));
            msg.addExtra(colored(ChatColor.DARK_RED, ")"));
            player.spigot().sendMessage(msg);
        }
    }

    private void add(Player player, String label, String[] args) {
        if (args.length == 0) {
            help(player, label);
            return;
        }
        if (!checkPermission(player, "coordbook.add"))
            return;
        String name = String.join(" ", args);
        Book book = database.get(player.getWorld().getName());
        if (book.has(name)) {
            player.sendMessage(ChatColor.RED + "An entry with the name '" + name + "' already exists.");
            return;
        }
        book.put(name, new Entry(player.getLocation(), player.getUniqueId()));
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
        Book book = database.get(player.getWorld().getName());
        Entry entry = book.get(name);
        if (entry == null) {
            player.sendMessage(ChatColor.RED + "The entry '" + name + "' does not exist.");
            return;
        }
        if (!player.getUniqueId().equals(entry.getAuthor())) {
            if (!checkPermission(player, "coordbook.remove.other"))
                return;
        }
        book.remove(name);
        player.sendMessage(ChatColor.GREEN + "Removed the entry '" + name + "'.");
    }

    private void rename(Player player, String label, String[] args) {
        int arrowIndex = Arrays.asList(args).indexOf("→");
        if (arrowIndex == -1 || arrowIndex == 0 || arrowIndex == args.length - 1) {
            help(player, label);
            return;
        }
        if (!checkPermission(player, "coordbook.rename"))
            return;
        String from = String.join(" ", Arrays.copyOfRange(args, 0, arrowIndex));
        String to = String.join(" ", Arrays.copyOfRange(args, arrowIndex + 1, args.length));
        Book book = database.get(player.getWorld().getName());
        Entry entry = book.get(from);
        if (entry == null) {
            player.sendMessage(ChatColor.RED + "The entry '" + from + "' does not exist.");
            return;
        }
        if (book.has(to)) {
            player.sendMessage(ChatColor.RED + "The entry '" + to + "' already exists.");
            return;
        }
        if (!entry.getAuthor().equals(player.getUniqueId())) {
            if (!checkPermission(player, "coordbook.rename.other"))
                return;
        }
        book.rename(from, to);
        player.sendMessage(ChatColor.GREEN + "Renamed the entry '" + from + "' to '" + to + "'.");
    }

    private void togglePin(Player player, String label, String[] args) {
        if (args.length < 2) {
            help(player, label);
            return;
        }
        if (!checkPermission(player, "coordbook.pin"))
            return;
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Book book = database.get(player.getWorld().getName());
        if (!book.has(name)) {
            player.sendMessage(ChatColor.RED + "The entry '" + name + "' does not exist.");
            return;
        }
        book.togglePinned(name);
        list(player, label, "edit", new String[]{args[0]}, true);
    }

    private void swapPin(Player player, String label, String[] args) {
        if (args.length < 2) {
            help(player, label);
            return;
        }
        if (!checkPermission(player, "coordbook.pin"))
            return;
        int pos1, pos2;
        try {
            pos1 = Integer.parseInt(args[0]);
            pos2 = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            help(player, label);
            return;
        }
        Book book = database.get(player.getWorld().getName());
        List<String> pinned = book.getPinned();
        if (pos1 < 0 || pos1 >= pinned.size() || pos2 < 0 || pos2 >= pinned.size()) {
            help(player, label);
            return;
        }
        Collections.swap(pinned, pos1, pos2);
        list(player, label, "edit", Arrays.copyOfRange(args, 2, args.length), true);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return Collections.emptyList();
        switch (args.length) {
            case 0:
                throw new AssertionError("Unreachable code");
            case 1: {
                String partial = args[0].toLowerCase();
                return Stream.of("list", "edit", "add", "rename", "remove")
                        .filter(s -> s.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
            }
            default:
                if ("list".equalsIgnoreCase(args[0])) {
                    return Collections.emptyList();
                } else if ("edit".equalsIgnoreCase(args[0])) {
                    return Collections.emptyList();
                } else if ("add".equalsIgnoreCase(args[0])) {
                    return Collections.emptyList();
                } else if ("rename".equalsIgnoreCase(args[0])) {
                    int arrowIndex = Arrays.asList(args).indexOf("→");
                    if (arrowIndex == -1) {
                        // Complete the first argument or the arrow
                        String partial = String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                                .toLowerCase();
                        return database.get(((Player) sender).getWorld().getName())
                                .getNames()
                                .stream()
                                .map(name -> name + " →")
                                .filter(arg -> arg.toLowerCase().startsWith(partial))
                                .collect(Collectors.toList());
                    } else {
                        // The arrow is already there -- just let the user type the new name
                        return Collections.emptyList();
                    }
                } else if ("remove".equalsIgnoreCase(args[0])) {
                    String world = ((Player) sender).getWorld().getName();
                    String partial = String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                            .toLowerCase();
                    return database.get(world)
                            .getNames()
                            .stream()
                            .filter(arg -> arg.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList());
                } else {
                    return Collections.emptyList();
                }
        }
    }
}
