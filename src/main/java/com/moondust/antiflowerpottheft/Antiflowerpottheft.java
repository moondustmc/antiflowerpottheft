package com.moondust.antiflowerpottheft;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Antiflowerpottheft extends JavaPlugin {

    private RegionManager regionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        regionManager = new RegionManager(this);
        regionManager.load();

        AntiFlowerPotTheftCommand commandHandler = new AntiFlowerPotTheftCommand(this, regionManager);
        PluginCommand command = getCommand("flowerpot");
        if (command != null) {
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);
        }

        getServer().getPluginManager().registerEvents(new PotProtectionListener(this, regionManager), this);
        getLogger().info("Loaded " + regionManager.getRegions().size() + " protected flower pot region(s).");
    }

    @Override
    public void onDisable() {
        if (regionManager != null) {
            regionManager.save();
        }
    }
}
