package com.bekvon.bukkit.residence.gui.ui.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;

/**
 * Read-only queries against the Residence model that both user interfaces use.
 * <p>
 * Kept apart from {@code CommandRegistry} on purpose: the registry describes
 * the commands, this class answers the questions the screens ask about them.
 * Nothing here changes any data.
 */
public final class DataProviders {

    private final Residence plugin;

    public DataProviders(Residence plugin) {
        this.plugin = plugin;
    }

    /**
     * The residence the player currently stands in, or {@code null}.
     */
    public ClaimedResidence currentResidence(Player player) {
        if (player == null)
            return null;
        try {
            return plugin.getResidenceManager().getByLoc(player.getLocation());
        } catch (Throwable e) {
            return null;
        }
    }

    public ClaimedResidence residence(String name) {
        if (name == null)
            return null;
        try {
            return plugin.getResidenceManager().getByName(name);
        } catch (Throwable e) {
            return null;
        }
    }

    public boolean isAdmin(Player player) {
        if (player == null)
            return false;
        try {
            return plugin.getPermissionManager().isResidenceAdmin(player);
        } catch (Throwable e) {
            return false;
        }
    }

    public boolean hasPermission(Player player, String node) {
        if (player == null || node == null)
            return false;
        try {
            return player.hasPermission(node);
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Whether the player may use a sub command.
     */
    public boolean canUse(Player player, CommandSpec spec) {
        if (player == null || spec == null)
            return false;
        if (spec.isAdmin() && !isAdmin(player))
            return false;
        return hasPermission(player, "residence.command." + spec.getName().toLowerCase(Locale.ENGLISH));
    }

    /**
     * Number of residences the player owns.
     */
    public int ownedResidences(Player player) {
        if (player == null)
            return 0;
        try {
            com.bekvon.bukkit.residence.containers.ResidencePlayer rplayer = plugin.getPlayerManager().getResidencePlayer(player);
            return rplayer == null || rplayer.getResList() == null ? 0 : rplayer.getResList().size();
        } catch (Throwable e) {
            return 0;
        }
    }

    /**
     * Looks up a material name, returning {@code null} when the server
     * version does not know it.
     */
    public Material material(String name) {
        if (name == null || name.isEmpty())
            return null;
        Material material = materialExact(name);
        if (material != null)
            return material;
        // Config authors usually write "gray_stained_glass_pane"; the upper
        // case form is the canonical Bukkit name.
        material = materialExact(name.toUpperCase(Locale.ENGLISH).replace(' ', '_'));
        if (material != null)
            return material;
        // Legacy names such as "STAINED_GLASS_PANE" or "SIGN" still map to a
        // modern material on servers that keep the aliases.
        return materialExact(name.toUpperCase(Locale.ENGLISH));
    }

    private Material materialExact(String name) {
        try {
            Material material = Material.matchMaterial(name);
            if (material == null) {
                try {
                    material = Material.valueOf(name);
                } catch (IllegalArgumentException ignored) {
                    material = null;
                }
            }
            return material;
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Tries every candidate in order and returns the first material the server
     * actually has. Falls back to paper as a last resort so a menu never breaks
     * because of one wrong material name in a config.
     */
    public Material resolveMaterial(List<String> candidates, String primary) {
        List<String> order = new ArrayList<String>();
        if (primary != null)
            order.add(primary);
        if (candidates != null)
            order.addAll(candidates);
        for (String candidate : order) {
            Material material = material(candidate);
            if (material != null && material.isItem())
                return material;
        }
        Material paper = material("PAPER");
        return paper == null ? Material.STONE : paper;
    }

    public Residence getPlugin() {
        return plugin;
    }
}
