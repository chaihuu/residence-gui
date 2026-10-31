package com.bekvon.bukkit.residence.gui.ui.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.Flags.FlagMode;
import com.bekvon.bukkit.residence.containers.ResidencePlayer;
import com.bekvon.bukkit.residence.gui.ui.text.MenuText;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;

/**
 * Supplies the pickable values of every {@link ArgType}.
 * <p>
 * The lists are built on demand so they always reflect the live server state
 * (new residences, joined players, enabled flags ...). Every visible string is
 * produced here through the translation system, which is why both user
 * interfaces show identical translated entries.
 */
public final class ArgSources {

    /** Icons of the list items, kept together so they are easy to change. */
    private static final String ICON_RESIDENCE = "GRASS_BLOCK";
    private static final String ICON_PLAYER = "PLAYER_HEAD";
    private static final String ICON_WORLD = "GRASS_BLOCK";
    private static final String ICON_GROUP = "NAME_TAG";
    private static final String ICON_FLAG = "PAPER";

    /** Active instance, set by the menu manager, so the items can be built without a plugin lookup. */
    private static ArgSources instance;

    private final Residence plugin;

    /**
     * Creates the source used for every screen. The items are repository
     * queries, so a single instance is enough.
     */
    public static synchronized ArgSources create(Residence plugin) {
        instance = new ArgSources(plugin);
        return instance;
    }

    private ArgSources(Residence plugin) {
        this.plugin = plugin;
    }

    /**
     * The configured source, created on demand when no menu has been opened
     * yet.
     */
    private static ArgSources get() {
        if (instance == null)
            instance = new ArgSources(Residence.getInstance());
        return instance;
    }

    private static MenuText.Args args() {
        return MenuText.Args.of();
    }

    private static String safe(String value) {
        return value == null ? "?" : value;
    }

    // ------------------------------------------------------------------
    // list items
    // ------------------------------------------------------------------

    /**
     * The residences the player can act on. The residence the player stands in
     * is sorted first and marked as selected.
     */
    public static List<MenuItem> residences(Player player, boolean admin) {
        List<MenuItem> out = new ArrayList<MenuItem>();
        Residence plugin = get().plugin;
        if (plugin == null || player == null)
            return out;

        ClaimedResidence current = plugin.getResidenceManager().getByLoc(player.getLocation());
        String currentName = current == null ? null : current.getName();

        List<ClaimedResidence> candidates = new ArrayList<ClaimedResidence>();
        ResidencePlayer rplayer = plugin.getPlayerManager().getResidencePlayer(player);
        if (rplayer != null && rplayer.getResList() != null)
            candidates.addAll(rplayer.getResList());

        if (admin) {
            for (ClaimedResidence one : plugin.getResidenceManager().getFromAllResidences(true, false, null))
                if (!candidates.contains(one))
                    candidates.add(one);
        } else if (current != null && !candidates.contains(current)) {
            candidates.add(current);
        }

        for (ClaimedResidence res : candidates) {
            if (res == null || res.getName() == null)
                continue;
            boolean here = res.getName().equalsIgnoreCase(currentName);
            int subzones = res.getSubzonesAmount(true);

            MenuText.Args args = args().put("residence", res.getName()).put("owner", safe(res.getOwner()))
                    .put("world", safe(res.getWorldName())).put("subzones", subzones);

            List<String> lore = new ArrayList<String>();
            lore.add("menu.item.residence.owner");
            lore.add("menu.item.residence.world");
            if (subzones > 0)
                lore.add("menu.item.residence.subzones");
            if (res.isForRent())
                lore.add("menu.item.residence.for-rent");
            if (res.isForSell())
                lore.add("menu.item.residence.for-sale");
            if (here)
                lore.add("menu.item.residence.here");

            out.add(new MenuItem(res.getName(), Component.text(res.getName()), lore, args, ICON_RESIDENCE, null, null));
        }

        out.sort((a, b) -> a.getValue().compareToIgnoreCase(b.getValue()));
        if (out.size() > 1 && currentName != null) {
            // Keep the residence the player stands in on top, alphabetically
            // sorted otherwise.
            List<MenuItem> sorted = new ArrayList<MenuItem>();
            for (MenuItem item : out)
                if (item.getValue().equalsIgnoreCase(currentName))
                    sorted.add(0, item);
                else
                    sorted.add(item);
            return sorted;
        }
        return out;
    }

    /**
     * Residence names from the ownership of a specific residence.
     */
    public static List<MenuItem> residences(Player player, boolean admin, ClaimedResidence residence) {
        return residences(player, admin);
    }

    /**
     * Online players, the viewer excluded.
     */
    public static List<MenuItem> players(Player viewer) {
        List<MenuItem> out = new ArrayList<MenuItem>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (viewer != null && online.getUniqueId().equals(viewer.getUniqueId()))
                continue;
            if (viewer != null && !viewer.canSee(online))
                continue;
            MenuText.Args args = args().put("player", online.getName()).put("world", safe(online.getWorld().getName()));
            out.add(new MenuItem(online.getName(), Component.text(online.getName()), java.util.Collections.singletonList("menu.item.player.world"), args,
                    ICON_PLAYER, null, null));
        }
        out.sort((a, b) -> a.getValue().compareToIgnoreCase(b.getValue()));
        return out;
    }

    /**
     * Flags the viewer may change in the given context.
     */
    public static List<MenuItem> flags(Player player, FlagMode mode, boolean admin, ClaimedResidence residence) {
        List<MenuItem> out = new ArrayList<MenuItem>();
        Residence plugin = get().plugin;
        if (plugin == null || player == null)
            return out;

        FlagPermissions global = plugin.getPermissionManager().getAllFlags();
        if (global == null)
            return out;

        Set<String> allowed = null;
        if (residence != null) {
            try {
                boolean perPlayer = mode == FlagMode.Player;
                allowed = new HashSet<String>(residence.getPermissions().getPossibleFlags(player, !perPlayer, admin));
            } catch (Throwable ignored) {
                allowed = null;
            }
        }

        for (String raw : FlagPermissions.getAllPossibleFlags()) {
            Flags flag = Flags.getFlag(raw);
            if (flag != null && !flag.isGlobalyEnabled())
                continue;
            if (flag != null && mode != null && flag.getFlagMode() != FlagMode.Both && flag.getFlagMode() != mode)
                continue;
            if (allowed != null && !allowed.contains(raw))
                continue;

            String name = flag == null ? raw : flag.getName();
            MenuText.Args args = args().put("flag", name).put("key", raw);
            if (flag != null)
                args.put("mode", plugin.getUiLanguage().getPlain(player, "menu.flagmode." + flag.getFlagMode().name().toLowerCase(Locale.ENGLISH), null));
            if (flag == null || flag.getDesc() == null)
                continue;
            args.put("desc", flag.getDesc());
            out.add(new MenuItem(raw.toLowerCase(Locale.ENGLISH), Component.text(name), Arrays.asList("menu.item.flag.desc", "menu.item.flag.mode"), args,
                    ICON_FLAG, null, null));
        }

        out.sort((a, b) -> a.getValue().compareToIgnoreCase(b.getValue()));
        return out;
    }

    public static List<MenuItem> worlds() {
        List<MenuItem> out = new ArrayList<MenuItem>();
        for (World world : Bukkit.getWorlds()) {
            MenuText.Args args = args().put("world", world.getName()).put("players", world.getPlayers().size());
            out.add(new MenuItem(world.getName(), Component.text(world.getName()), java.util.Collections.singletonList("menu.item.world.players"), args,
                    ICON_WORLD, null, null));
        }
        return out;
    }

    public static List<MenuItem> groups() {
        List<MenuItem> out = new ArrayList<MenuItem>();
        Residence plugin = get().plugin;
        if (plugin == null)
            return out;
        try {
            for (String group : plugin.getPermissionManager().getGroups().keySet()) {
                MenuText.Args args = args().put("group", group);
                out.add(new MenuItem(group, Component.text(group), new ArrayList<String>(), args, ICON_GROUP, null, null));
            }
        } catch (Throwable ignored) {
        }
        out.sort((a, b) -> a.getValue().compareToIgnoreCase(b.getValue()));
        return out;
    }

    /**
     * Literal choices of a {@link ArgType#CHOICE} argument. The display text is
     * the translated value, the command receives the raw value.
     */
    public static List<MenuItem> choices(Player player, ArgDef def) {
        List<MenuItem> out = new ArrayList<MenuItem>();
        if (def == null)
            return out;
        Residence plugin = get().plugin;
        for (String choice : def.getChoices()) {
            if (choice == null)
                continue;
            MenuText.Args args = args().put("value", choice);
            String key = "menu.choice." + choice.toLowerCase(Locale.ENGLISH);
            Component name = plugin != null && plugin.getUiLanguage().has(key) ? plugin.getUiLanguage().get(player, key, args) : Component.text(choice);
            out.add(new MenuItem(choice, name, java.util.Collections.singletonList("menu.item.choice.value"), args, iconFor(choice), null, null));
        }
        return out;
    }

    /**
     * true / false / remove for a flag.
     */
    public static List<MenuItem> flagStates(Player player) {
        List<MenuItem> out = new ArrayList<MenuItem>();
        Residence plugin = get().plugin;
        for (String state : new String[] { "true", "false", "remove" }) {
            MenuText.Args args = args().put("value", state);
            Component name = plugin == null ? Component.text(state)
                    : plugin.getUiLanguage().get(player, "menu.state." + state, args);
            out.add(new MenuItem(state, name, java.util.Collections.singletonList("menu.item.state.desc"), args, stateMaterial(state), null, null));
        }
        return out;
    }

    private static String stateMaterial(String state) {
        if ("true".equalsIgnoreCase(state))
            return "LIME_DYE";
        if ("false".equalsIgnoreCase(state))
            return "RED_DYE";
        return "GRAY_DYE";
    }

    private static String iconFor(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ENGLISH);
        switch (lower) {
        case "true":
        case "on":
        case "allow":
        case "add":
        case "start":
        case "join":
            return "LIME_DYE";
        case "false":
        case "off":
        case "block":
        case "stop":
        case "leave":
            return "RED_DYE";
        case "remove":
        case "clear":
            return "GRAY_DYE";
        default:
            return ICON_FLAG;
        }
    }
}
