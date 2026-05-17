package com.moondust.antiflowerpottheft;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public final class RegionManager {
    private final Antiflowerpottheft plugin;
    private final Map<String, ProtectedRegion> regions = new LinkedHashMap<>();
    private final Map<UUID, RegionSelection> selections = new LinkedHashMap<>();

    public RegionManager(Antiflowerpottheft plugin) {
        this.plugin = plugin;
    }

    public void load() {
        regions.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("regions");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection regionSection = section.getConfigurationSection(key);
            if (regionSection == null) {
                continue;
            }

            String name = regionSection.getString("name", key);
            String worldName = regionSection.getString("world");
            if (worldName == null || worldName.isBlank()) {
                plugin.getLogger().warning("Skipping region '" + key + "' because it has no world.");
                continue;
            }

            ProtectedRegion region = new ProtectedRegion(
                    name,
                    worldName,
                    regionSection.getInt("min.x"),
                    regionSection.getInt("min.y"),
                    regionSection.getInt("min.z"),
                    regionSection.getInt("max.x"),
                    regionSection.getInt("max.y"),
                    regionSection.getInt("max.z")
            );
            regions.put(normalizeName(name), region);
        }
    }

    public void save() {
        plugin.getConfig().set("regions", null);

        for (ProtectedRegion region : regions.values()) {
            String path = "regions." + normalizeName(region.getName());
            plugin.getConfig().set(path + ".name", region.getName());
            plugin.getConfig().set(path + ".world", region.getWorldName());
            plugin.getConfig().set(path + ".min.x", region.getMinX());
            plugin.getConfig().set(path + ".min.y", region.getMinY());
            plugin.getConfig().set(path + ".min.z", region.getMinZ());
            plugin.getConfig().set(path + ".max.x", region.getMaxX());
            plugin.getConfig().set(path + ".max.y", region.getMaxY());
            plugin.getConfig().set(path + ".max.z", region.getMaxZ());
        }

        plugin.saveConfig();
    }

    public RegionSelection getSelection(UUID playerId) {
        return selections.computeIfAbsent(playerId, ignored -> new RegionSelection());
    }

    public ProtectedRegion createRegion(String name, World world, Location first, Location second) {
        ProtectedRegion region = new ProtectedRegion(name, world, first, second);
        regions.put(normalizeName(name), region);
        save();
        return region;
    }

    public boolean deleteRegion(String name) {
        ProtectedRegion removed = regions.remove(normalizeName(name));
        if (removed == null) {
            return false;
        }

        save();
        return true;
    }

    public Optional<ProtectedRegion> getRegion(String name) {
        return Optional.ofNullable(regions.get(normalizeName(name)));
    }

    public Collection<ProtectedRegion> getRegions() {
        return Collections.unmodifiableCollection(regions.values());
    }

    public Optional<ProtectedRegion> getRegionAt(Location location) {
        return regions.values().stream()
                .filter(region -> region.contains(location))
                .findFirst();
    }

    public boolean isNameValid(String name) {
        return name != null && name.matches("[A-Za-z0-9_-]{1,32}");
    }

    private String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
