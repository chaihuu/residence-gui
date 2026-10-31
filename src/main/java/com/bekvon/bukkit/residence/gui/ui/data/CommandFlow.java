package com.bekvon.bukkit.residence.gui.ui.data;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags.FlagMode;
import com.bekvon.bukkit.residence.gui.ui.text.MenuText;

/**
 * Walks through the arguments of a command one at a time.
 * <p>
 * Values are stored in an argument aligned list: index {@code i} always belongs
 * to argument {@code i}. A skipped optional argument is stored as {@code null}
 * so positions never shift. The flow only decides which screen comes next and
 * the screens decide how values are shown, which keeps the logic shared between
 * the chest menu and the Bedrock form.
 */
public final class CommandFlow {

    private CommandFlow() {
    }

    /**
     * Starts a flow, optionally pre-filled with a residence for the first
     * argument.
     */
    public static void start(Residence plugin, MenuScreens screens, CommandSpec spec, String presetResidence) {
        if (spec == null)
            return;
        List<String> picked = ArgValues.create(spec);
        if (presetResidence != null && spec.getArgumentCount() > 0 && spec.getArguments().get(0).getType() == ArgType.RESIDENCE)
            ArgValues.set(picked, 0, presetResidence);
        next(plugin, screens, spec, picked, 0);
    }

    /**
     * Moves to the first argument that still needs a value, then either asks
     * for it or shows the confirmation screen.
     *
     * @param picked argument aligned values collected so far
     * @param index  first argument that may still need a value
     */
    public static void next(Residence plugin, MenuScreens screens, CommandSpec spec, List<String> picked, int index) {
        Player player = screens == null ? null : screens.getPlayer();
        if (player == null || !player.isOnline() || spec == null)
            return;

        int next = Math.max(0, index);
        while (next < spec.getArgumentCount() && (spec.getArguments().get(next).isSkipped() || ArgValues.get(picked, next) != null))
            next++;

        if (next >= spec.getArgumentCount()) {
            screens.rememberWizard(spec, picked, Math.max(0, spec.getArgumentCount() - 1), true);
            screens.show(screens.wizardConfirm(1));
            return;
        }
        screens.rememberWizard(spec, picked, next, false);
        screens.show(screens.wizardArgument(1));
    }

    /**
     * Flag mode implied by a command, used to filter the flag list.
     */
    public static FlagMode modeFor(String command) {
        if (command == null)
            return FlagMode.Both;
        switch (command) {
        case "pset":
        case "pmirror":
        case "setallfor":
            return FlagMode.Player;
        case "set":
        case "setall":
        case "reset":
        case "clearflags":
            return FlagMode.Residence;
        default:
            return FlagMode.Both;
        }
    }

    /**
     * Preset numbers offered by the numeric picker, so a player does not have to
     * click "+" a hundred times.
     */
    public static int[] presetsFor(String command) {
        if (command == null)
            return new int[] { 1, 2, 5, 10, 25, 50, 100 };
        switch (command) {
        case "auto":
            return new int[] { 5, 10, 20, 30, 50, 100 };
        case "bank":
            return new int[] { 10, 100, 1000, 10000 };
        case "list":
        case "listall":
            return new int[] { 1, 2, 3, 5 };
        default:
            return new int[] { 1, 2, 5, 10, 25, 50, 100 };
        }
    }

    /**
     * Residence that provides the context for a flag list: the value already
     * picked for a residence argument, or the residence the player stands in.
     */
    public static com.bekvon.bukkit.residence.protection.ClaimedResidence contextResidence(Residence plugin, Player player, CommandSpec spec,
            List<String> picked) {
        try {
            if (spec != null && picked != null) {
                for (int i = 0; i < spec.getArgumentCount() && i < picked.size(); i++) {
                    if (spec.getArguments().get(i).getType() == ArgType.RESIDENCE) {
                        String name = picked.get(i);
                        if (name != null)
                            return plugin.getResidenceManager().getByName(name);
                        break;
                    }
                }
            }
            return plugin.getResidenceManager().getByLoc(player.getLocation());
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Translation key of the label of an argument.
     */
    public static String argLabelKey(CommandSpec spec, int index) {
        if (spec == null || index < 0 || index >= spec.getArgumentCount())
            return "menu.arg.value";
        return CommandRegistry.argLabelKey(spec.getName(), spec.getArguments().get(index));
    }

    /**
     * A copy of the value list that covers the given index, used when a value is
     * picked for an argument that was previously skipped.
     */
    public static List<String> withValue(List<String> picked, int index, String value) {
        List<String> next = new ArrayList<String>();
        if (picked != null)
            next.addAll(picked);
        while (next.size() <= index)
            next.add(null);
        next.set(index, value);
        return next;
    }

    /**
     * Placeholders used by every wizard screen.
     */
    public static MenuText.Args baseArgs(CommandSpec spec, List<String> picked) {
        MenuText.Args args = MenuText.Args.of();
        if (spec != null)
            args.put("command", spec.getName());
        args.put("line", ArgValues.commandLine(spec, picked));
        return args;
    }
}
