package com.bekvon.bukkit.residence.gui.ui.screen;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.gui.ui.data.AbstractScreen;
import com.bekvon.bukkit.residence.gui.ui.data.ArgDef;
import com.bekvon.bukkit.residence.gui.ui.data.ArgType;
import com.bekvon.bukkit.residence.gui.ui.data.CommandRegistry;
import com.bekvon.bukkit.residence.gui.ui.data.CommandSpec;
import com.bekvon.bukkit.residence.gui.ui.data.MenuAction;
import com.bekvon.bukkit.residence.gui.ui.data.MenuItem;
import com.bekvon.bukkit.residence.gui.ui.data.MenuScreens;
import com.bekvon.bukkit.residence.gui.ui.data.MenuView;
import com.bekvon.bukkit.residence.gui.ui.data.Screens;
import com.bekvon.bukkit.residence.gui.ui.text.MenuText;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;

/**
 * Overview of one residence with the actions that make sense for it.
 * <p>
 * Reached from the shortcut of the root screen. Every action starts the normal
 * command wizard with the residence pre-filled, so the chest menu and the
 * Bedrock form behave exactly like {@code /res <command> <residence>}.
 */
public class ResidenceScreen extends AbstractScreen {

    public ResidenceScreen(Residence plugin, MenuScreens screens, String residenceName, int page) {
        super(plugin, screens, residenceName, page);
    }

    private ClaimedResidence residence() {
        return data.residence(target);
    }

    @Override
    public String kind() {
        return Screens.RESIDENCE;
    }

    @Override
    protected String titleKey() {
        return "menu.title.residence";
    }

    @Override
    protected String emptyKind() {
        return "action";
    }

    @Override
    protected List<String> itemHintKeys() {
        return Arrays.asList("menu.hint.left_click");
    }

    /**
     * Command name, icon and translation key of every shortcut of this screen.
     * The residence argument is pre-filled automatically.
     */
    private static final String[][] ACTIONS = {
            { "set", "COMPARATOR", "menu.residence.set" },
            { "pset", "WRITTEN_BOOK", "menu.residence.pset" },
            { "info", "BOOK", "menu.residence.info" },
            { "tp", "ENDER_PEARL", "menu.residence.tp" },
            { "tpset", "LODESTONE", "menu.residence.tpset" },
            { "show", "SPYGLASS", "menu.residence.show" },
            { "sublist", "GRASS_BLOCK", "menu.residence.sublist" },
            { "area", "FILLED_MAP", "menu.residence.area" },
            { "expand", "PISTON", "menu.residence.expand" },
            { "contract", "STICKY_PISTON", "menu.residence.contract" },
            { "padd", "PLAYER_HEAD", "menu.residence.padd" },
            { "pdel", "SKELETON_SKULL", "menu.residence.pdel" },
            { "command", "COMMAND_BLOCK", "menu.residence.command" },
            { "lset", "CHEST", "menu.residence.lset" },
            { "rename", "NAME_TAG", "menu.residence.rename" },
            { "reset", "RED_CONCRETE", "menu.residence.reset" },
            { "remove", "TNT", "menu.residence.remove" } };

    @Override
    protected void collect(Player player) {
        ClaimedResidence res = residence();
        if (res == null) {
            return;
        }

        for (String[] action : ACTIONS) {
            String command = action[0];
            CommandSpec spec = CommandRegistry.get(command);
            String key = action[2];

            MenuText.Args args = MenuText.Args.of().put("residence", target).put("command", command);
            List<String> lore = new ArrayList<String>();
            lore.add(key + ".lore");
            if (spec == null) {
                // The command is not part of the registry on this build; still
                // show the entry so the player can use the chat fallback.
                lore.add("menu.residence.unavailable");
            } else if (!data.canUse(player, spec)) {
                lore.add("menu.command.flag.no-permission");
            } else if (spec.isDangerous()) {
                lore.add("menu.command.flag.dangerous");
            }

            boolean enabled = spec != null && data.canUse(player, spec);
            add(new MenuItem(command, lang.get(player, key + ".name", args), lore, args, enabled ? action[1] : "BARRIER", null, null),
                    enabled ? MenuAction.command(command) : MenuAction.none());
        }
    }

    @Override
    protected void decorate(MenuView.Builder builder, Player player) {
        ClaimedResidence res = residence();
        if (res != null) {
            MenuText.Args args = MenuText.Args.of().put("residence", target).put("owner", res.getOwner()).put("world", res.getWorldName());
            builder.content(lang.get(player, "menu.form.residence", args));
        } else {
            builder.content(lang.get(player, "menu.form.residence-missing", null));
        }
        addStandardFooter(builder, player);
    }

    @Override
    protected List<Component> formHeader(Player player) {
        List<Component> out = new ArrayList<Component>();
        ClaimedResidence res = residence();
        if (res == null) {
            out.add(lang.get(player, "menu.form.residence-missing", null));
            return out;
        }
        MenuText.Args args = MenuText.Args.of().put("residence", target).put("owner", res.getOwner()).put("world", res.getWorldName())
                .put("subzones", res.getSubzonesAmount(true));
        out.add(lang.get(player, "menu.form.residence", args));
        return out;
    }

    @Override
    protected MenuText.Args pathArgs(Player player) {
        return MenuText.Args.of().put("target", target);
    }

    /**
     * The residence currently shown, used by the renderers to show the seal.
     */
    public ClaimedResidence residenceOf() {
        return residence();
    }

    /**
     * Pre-filled residence for a command of this screen.
     */
    public static String presetFor(CommandSpec spec, String residenceName) {
        if (spec == null || residenceName == null)
            return null;
        if (spec.getArgumentCount() == 0)
            return null;
        ArgDef first = spec.getArguments().get(0);
        return first.getType() == ArgType.RESIDENCE ? residenceName : null;
    }
}
