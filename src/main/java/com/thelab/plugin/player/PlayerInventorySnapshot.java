package com.thelab.plugin.player;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** A snapshot of a player's full state for later restoration. */
public class PlayerInventorySnapshot {

    private final ItemStack[] contents;
    private final ItemStack[] armorContents;
    private final ItemStack offHand;
    private final int heldItemSlot;
    private final GameMode gameMode;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final int xpLevel;
    private final float xpProgress;
    private final List<PotionEffect> potionEffects;
    private final boolean allowFlight;
    private final boolean flying;
    private final Location location;

    private PlayerInventorySnapshot(Player player) {
        this.contents = player.getInventory().getContents().clone();
        this.armorContents = player.getInventory().getArmorContents().clone();
        ItemStack oh = player.getInventory().getItemInOffHand();
        this.offHand = oh.clone();
        this.heldItemSlot = player.getInventory().getHeldItemSlot();
        this.gameMode = player.getGameMode();
        double maxHealth = 20.0;
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        }
        this.health = Math.min(player.getHealth(), maxHealth);
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.xpLevel = player.getLevel();
        this.xpProgress = player.getExp();
        this.potionEffects = new ArrayList<>(player.getActivePotionEffects());
        this.allowFlight = player.getAllowFlight();
        this.flying = player.isFlying();
        this.location = player.getLocation().clone();
    }

    /** Captures the current state of a player. */
    public static PlayerInventorySnapshot capture(Player player) {
        return new PlayerInventorySnapshot(player);
    }

    /** Restores this snapshot to the given player. */
    public void restore(Player player) {
        // Clear existing effects and inventory
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Restore everything
        player.getInventory().setContents(contents);
        player.getInventory().setArmorContents(armorContents);
        player.getInventory().setItemInOffHand(offHand);
        player.getInventory().setHeldItemSlot(heldItemSlot);
        player.setGameMode(gameMode);
        double maxHealth = 20.0;
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        }
        player.setHealth(Math.min(health, maxHealth));
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setLevel(xpLevel);
        player.setExp(xpProgress);
        player.addPotionEffects(potionEffects);
        player.setAllowFlight(allowFlight);
        if (allowFlight) player.setFlying(flying);
        // Teleport back to location if world still available
        if (location.getWorld() != null) {
            player.teleport(location);
        }
    }

    public Location getLocation() { return location; }
}
