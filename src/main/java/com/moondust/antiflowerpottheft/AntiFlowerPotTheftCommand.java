package com.moondust.antiflowerpottheft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class AntiFlowerPotTheftCommand implements TabExecutor {
    private static final List<String> ROOT_SUBCOMMANDS = List.of("help", "region", "reload");
    private static final List<String> REGION_SUBCOMMANDS = List.of("pos1", "pos2", "create", "delete", "list", "info");

    private final Antiflowerpottheft plugin;
    private final RegionManager regionManager;

    public AntiFlowerPotTheftCommand(Antiflowerpottheft plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("antiflowerpottheft.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subcommand) {
            case "region" -> handleRegion(sender, label, args);
            case "reload" -> handleReload(sender);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " help.");
                yield true;
            }
        };
    }

    private boolean handleRegion(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " region <pos1|pos2|create|delete|list|info>");
            return true;
        }

        String subcommand = args[1].toLowerCase(Locale.ROOT);
        return switch (subcommand) {
            case "pos1" -> setPosition(sender, true);
            case "pos2" -> setPosition(sender, false);
            case "create" -> createRegion(sender, label, args);
            case "delete" -> deleteRegion(sender, label, args);
            case "list" -> listRegions(sender);
            case "info" -> showRegionInfo(sender, label, args);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown region subcommand. Use /" + label + " help.");
                yield true;
            }
        };
    }

    private boolean setPosition(CommandSender sender, boolean first) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can set region positions.");
            return true;
        }

        Location location = player.getLocation().getBlock().getLocation();
        RegionSelection selection = regionManager.getSelection(player.getUniqueId());
        if (first) {
            selection.setFirst(location);
        } else {
            selection.setSecond(location);
        }

        sender.sendMessage(ChatColor.GREEN + "Set position " + (first ? "1" : "2") + " to "
                + formatLocation(location) + ".");
        return true;
    }

    private boolean createRegion(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can create regions from selections.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " region create <name>");
            return true;
        }

        String name = args[2];
        if (!regionManager.isNameValid(name)) {
            sender.sendMessage(ChatColor.RED + "Region names must be 1-32 letters, numbers, dashes, or underscores.");
            return true;
        }

        RegionSelection selection = regionManager.getSelection(player.getUniqueId());
        if (!selection.isComplete()) {
            sender.sendMessage(ChatColor.RED + "Set both positions first with /" + label + " region pos1 and /" + label + " region pos2.");
            return true;
        }

        if (!selection.isSameWorld()) {
            sender.sendMessage(ChatColor.RED + "Both region positions must be in the same world.");
            return true;
        }

        World world = selection.getFirst().getWorld();
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "Could not determine the selected world.");
            return true;
        }

        ProtectedRegion region = regionManager.createRegion(name, world, selection.getFirst(), selection.getSecond());
        sender.sendMessage(ChatColor.GREEN + "Created flower pot protection region '" + region.getName() + "'.");
        return true;
    }

    private boolean deleteRegion(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " region delete <name>");
            return true;
        }

        String name = args[2];
        if (!regionManager.deleteRegion(name)) {
            sender.sendMessage(ChatColor.RED + "No protected region named '" + name + "' exists.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Deleted protected region '" + name + "'.");
        return true;
    }

    private boolean listRegions(CommandSender sender) {
        if (regionManager.getRegions().isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No flower pot protection regions are configured.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "Flower pot protection regions:");
        for (ProtectedRegion region : regionManager.getRegions()) {
            sender.sendMessage(ChatColor.YELLOW + "- " + region.getName()
                    + ChatColor.GRAY + " (" + region.getWorldName() + ")");
        }
        return true;
    }

    private boolean showRegionInfo(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " region info <name>");
            return true;
        }

        String name = args[2];
        regionManager.getRegion(name).ifPresentOrElse(region -> {
            sender.sendMessage(ChatColor.GOLD + "Region: " + ChatColor.YELLOW + region.getName());
            sender.sendMessage(ChatColor.GRAY + "World: " + region.getWorldName());
            sender.sendMessage(ChatColor.GRAY + "Min: " + region.getMinX() + ", " + region.getMinY() + ", " + region.getMinZ());
            sender.sendMessage(ChatColor.GRAY + "Max: " + region.getMaxX() + ", " + region.getMaxY() + ", " + region.getMaxZ());
        }, () -> sender.sendMessage(ChatColor.RED + "No protected region named '" + name + "' exists."));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfig();
        regionManager.load();
        sender.sendMessage(ChatColor.GREEN + "Reloaded " + regionManager.getRegions().size() + " protected region(s).");
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "AntiFlowerPotTheft commands:");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " region pos1" + ChatColor.GRAY + " - Set the first corner.");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " region pos2" + ChatColor.GRAY + " - Set the second corner.");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " region create <name>" + ChatColor.GRAY + " - Save the selected region.");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " region delete <name>" + ChatColor.GRAY + " - Delete a region.");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " region list" + ChatColor.GRAY + " - List regions.");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " region info <name>" + ChatColor.GRAY + " - Show region bounds.");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload" + ChatColor.GRAY + " - Reload regions from config.");
    }

    private String formatLocation(Location location) {
        String worldName = location.getWorld() == null ? "unknown" : location.getWorld().getName();
        return worldName + " " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("antiflowerpottheft.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return matches(ROOT_SUBCOMMANDS, args[0]);
        }

        if (!args[0].equalsIgnoreCase("region")) {
            return Collections.emptyList();
        }

        if (args.length == 2) {
            return matches(REGION_SUBCOMMANDS, args[1]);
        }

        if (args.length == 3 && Arrays.asList("delete", "info").contains(args[1].toLowerCase(Locale.ROOT))) {
            List<String> regionNames = regionManager.getRegions().stream()
                    .map(ProtectedRegion::getName)
                    .toList();
            return matches(regionNames, args[2]);
        }

        return Collections.emptyList();
    }

    private List<String> matches(List<String> options, String input) {
        String normalizedInput = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(normalizedInput)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
