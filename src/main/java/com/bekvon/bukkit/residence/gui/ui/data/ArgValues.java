package com.bekvon.bukkit.residence.gui.ui.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for the argument aligned value list used by {@link CommandFlow}.
 * <p>
 * Position {@code i} always belongs to argument {@code i}. Skipped optional
 * arguments are represented by {@code null} so later positions stay stable.
 */
public final class ArgValues {

    private ArgValues() {
    }

    /**
     * Creates a list with one empty slot per argument.
     */
    public static List<String> create(CommandSpec spec) {
        int size = spec == null ? 0 : spec.getArgumentCount();
        List<String> list = new ArrayList<String>(size);
        for (int i = 0; i < size; i++)
            list.add(null);
        return list;
    }

    public static void set(List<String> list, int index, String value) {
        if (list == null || index < 0 || index >= list.size())
            return;
        list.set(index, value);
    }

    public static String get(List<String> list, int index) {
        if (list == null || index < 0 || index >= list.size())
            return null;
        return list.get(index);
    }

    /**
     * The tokens that are appended to the command, skipping holes.
     */
    public static List<String> values(CommandSpec spec, List<String> list) {
        List<String> out = new ArrayList<String>();
        if (spec == null || list == null)
            return out;
        for (int i = 0; i < spec.getArgumentCount() && i < list.size(); i++) {
            String value = list.get(i);
            if (value != null && !value.isEmpty())
                out.add(value);
        }
        return out;
    }

    /**
     * The command line that will run, e.g. {@code /res padd home Steve}.
     */
    public static String commandLine(CommandSpec spec, List<String> list) {
        StringBuilder sb = new StringBuilder("/res ");
        sb.append(spec == null ? "" : spec.getName());
        for (String token : values(spec, list))
            sb.append(' ').append(token);
        return sb.toString().trim();
    }
}
