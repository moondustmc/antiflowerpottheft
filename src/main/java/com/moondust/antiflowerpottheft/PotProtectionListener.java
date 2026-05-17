package com.moondust.antiflowerpottheft;

import java.util.Iterator;
import java.util.Optional;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class PotProtectionListener implements Listener {
    private static final String BYPASS_PERMISSION = "antiflowerpottheft.bypass";
    private static final String DEFAULT_DENY_BREAK_MESSAGE = "&cFlower pots are protected in this region.";
    private static final String DEFAULT_DENY_REMOVE_MESSAGE = "&cYou cannot remove plants from flower pots in this region.";
    private static final String DEFAULT_DENY_FRAME_ROTATE_MESSAGE = "&cItem frames cannot be rotated in this region.";
    private static final String DEFAULT_DENY_HANGING_DESTROY_MESSAGE = "&cItem frames and paintings are protected in this region.";

    private final Antiflowerpottheft plugin;
    private final RegionManager regionManager;

    public PotProtectionListener(Antiflowerpottheft plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Optional<ProtectedRegion> region = regionAt(block);
        if (!isFlowerPot(block.getType()) || region.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return;
        }

        event.setCancelled(true);
        sendDenyMessage(player, "messages.deny-break", DEFAULT_DENY_BREAK_MESSAGE, region.get());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Block block = event.getClickedBlock();
        Optional<ProtectedRegion> region = regionAt(block);
        if (!isPottedPlant(block.getType()) || region.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return;
        }

        event.setCancelled(true);
        sendDenyMessage(player, "messages.deny-remove", DEFAULT_DENY_REMOVE_MESSAGE, region.get());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        removeProtectedPotsFromExplosion(event.blockList().iterator());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        removeProtectedPotsFromExplosion(event.blockList().iterator());
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemFrameInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Optional<ProtectedRegion> region = protectedHangingRegion(entity);
        if (!(entity instanceof ItemFrame) || region.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return;
        }

        event.setCancelled(true);
        sendDenyMessage(player, "messages.deny-frame-rotate", DEFAULT_DENY_FRAME_ROTATE_MESSAGE, region.get());
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingDamage(EntityDamageByEntityEvent event) {
        Optional<ProtectedRegion> region = protectedHangingRegion(event.getEntity());
        if (region.isEmpty()) {
            return;
        }

        Player player = getResponsiblePlayer(event.getDamager());
        if (player != null && player.hasPermission(BYPASS_PERMISSION)) {
            return;
        }

        event.setCancelled(true);
        if (player != null) {
            sendDenyMessage(player, "messages.deny-hanging-destroy", DEFAULT_DENY_HANGING_DESTROY_MESSAGE, region.get());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        Optional<ProtectedRegion> region = protectedHangingRegion(event.getEntity());
        if (region.isEmpty()) {
            return;
        }

        Player player = null;
        if (event instanceof HangingBreakByEntityEvent entityEvent) {
            player = getResponsiblePlayer(entityEvent.getRemover());
            if (player != null && player.hasPermission(BYPASS_PERMISSION)) {
                return;
            }
        }

        event.setCancelled(true);
        if (player != null) {
            sendDenyMessage(player, "messages.deny-hanging-destroy", DEFAULT_DENY_HANGING_DESTROY_MESSAGE, region.get());
        }
    }

    private void removeProtectedPotsFromExplosion(Iterator<Block> blocks) {
        while (blocks.hasNext()) {
            Block block = blocks.next();
            if (isFlowerPot(block.getType()) && isProtected(block)) {
                blocks.remove();
            }
        }
    }

    private boolean isProtected(Block block) {
        return regionAt(block).isPresent();
    }

    private Optional<ProtectedRegion> regionAt(Block block) {
        return regionManager.getRegionAt(block.getLocation());
    }

    private Optional<ProtectedRegion> regionAt(Entity entity) {
        return regionManager.getRegionAt(entity.getLocation());
    }

    private Optional<ProtectedRegion> protectedHangingRegion(Entity entity) {
        if (!isProtectedHanging(entity)) {
            return Optional.empty();
        }

        return regionAt(entity);
    }

    private Player getResponsiblePlayer(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }

        if (entity instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }

        return null;
    }

    private void sendDenyMessage(Player player, String path, String fallback, ProtectedRegion region) {
        String message = plugin.getConfig().getString(path, fallback);
        if (message == null || message.isBlank()) {
            return;
        }

        message = message.replace("{region}", region.getName());
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    private boolean isFlowerPot(Material material) {
        return material == Material.FLOWER_POT || isPottedPlant(material);
    }

    private boolean isPottedPlant(Material material) {
        return material.name().startsWith("POTTED_");
    }

    private boolean isProtectedHanging(Entity entity) {
        return entity instanceof ItemFrame || entity instanceof Painting;
    }
}
