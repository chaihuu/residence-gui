package com.bekvon.bukkit.residence.gui.ui.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.bekvon.bukkit.residence.Residence;

/**
 * Declarative catalogue of every {@code /res} sub command together with the
 * arguments the menu has to collect before running it.
 * <p>
 * The registry is the single source of truth for the menu and contains no
 * display text at all: the name of a command and the label of an argument are
 * translation keys resolved from the language files
 * ({@code menu.command.<name>.name} and {@code menu.arg.<label>}). The internal
 * label of an argument is only used to build that key and the usage line, so
 * translating the menu can never change the command that is executed.
 * <p>
 * The registry is validated against
 * {@link com.bekvon.bukkit.residence.CommandFiller} at startup: a command the
 * plugin knows but the menu does not is reported in the log.
 */
public final class CommandRegistry {

    private static final Map<String, CommandSpec> SPECS = new LinkedHashMap<String, CommandSpec>();
    private static final Set<String> MISSING = new LinkedHashSet<String>();

    static {
        general();
        flags();
        ownership();
        areas();
        teleport();
        economy();
        raid();
        chat();
        admin();
        danger();
    }

    private CommandRegistry() {
    }

    private static void add(CommandSpec spec) {
        SPECS.put(spec.getName().toLowerCase(Locale.ENGLISH), spec);
    }

    private static void general() {
        String c = "general";
        add(CommandSpec.of("info", c).args(ArgDef.residence().optional(true)));
        add(CommandSpec.of("current", c));
        add(CommandSpec.of("list", c).args(ArgDef.player().optional(true)));
        add(CommandSpec.of("listall", c).args(ArgDef.amount("page", 1, 999).optional(true)));
        add(CommandSpec.of("limits", c).args(ArgDef.player().optional(true)));
        add(CommandSpec.of("show", c).args(ArgDef.residence().optional(true)));
        add(CommandSpec.of("tool", c));
        add(CommandSpec.of("create", c).args(ArgDef.text("residence_name")));
        add(CommandSpec.of("auto", c).args(ArgDef.text("residence_name").optional(true), ArgDef.amount("radius", 1, 200).optional(true)));
        add(CommandSpec.of("check", c).args(ArgDef.residence().optional(true), ArgDef.flag(), ArgDef.player().optional(true)));
        add(CommandSpec.of("material", c).chatRecommended().args(ArgDef.text("material")));
        add(CommandSpec.of("placeholders", c));
        add(CommandSpec.of("confirm", c).dangerous());
    }

    private static void flags() {
        String c = "flags";

        // /res set and /res pset open the dedicated flag editors when no flag is
        // given, so the menu links straight to those instead of asking for
        // arguments.
        add(CommandSpec.of("set", c).args(ArgDef.residence().optional(true)));
        add(CommandSpec.of("pset", c).args(ArgDef.residence().optional(true), ArgDef.player()));
        add(CommandSpec.of("flags", c));
        add(CommandSpec.of("gset", c).args(ArgDef.residence(), ArgDef.group(), ArgDef.flag(), ArgDef.flagState()));
        add(CommandSpec.of("setall", c).args(ArgDef.flag(), ArgDef.flagState()));
        add(CommandSpec.of("setallfor", c).args(ArgDef.player(), ArgDef.flag(), ArgDef.flagState()));
        add(CommandSpec.of("mirror", c).args(ArgDef.residence(), ArgDef.residence()));
        add(CommandSpec.of("pmirror", c).args(ArgDef.residence(), ArgDef.player(), ArgDef.player()));
        add(CommandSpec.of("reset", c).dangerous().args(ArgDef.residence()));
        add(CommandSpec.of("lists", c).args(ArgDef.choice("action", "list", "add", "remove", "apply", "set", "pset", "gset", "view")));
        add(CommandSpec.of("command", c).chatRecommended()
                .args(ArgDef.residence(), ArgDef.choice("action", "list", "allow", "block"), ArgDef.rawText("command").optional(true)));
    }

    private static void ownership() {
        String c = "ownership";
        add(CommandSpec.of("padd", c).args(ArgDef.residence().optional(true), ArgDef.player()));
        add(CommandSpec.of("pdel", c).args(ArgDef.residence().optional(true), ArgDef.player()));
        add(CommandSpec.of("give", c).dangerous()
                .args(ArgDef.residence(), ArgDef.player(), ArgDef.choice("include_subzones", "true", "false").optional(true)));
        add(CommandSpec.of("rename", c).args(ArgDef.residence(), ArgDef.text("new_name")));
        add(CommandSpec.of("renamearea", c).args(ArgDef.residence(), ArgDef.text("old_area_name"), ArgDef.text("new_area_name")));
        add(CommandSpec.of("setmain", c).args(ArgDef.residence().optional(true)));
        add(CommandSpec.of("kick", c).args(ArgDef.player()));
    }

    private static void areas() {
        String c = "areas";
        add(CommandSpec.of("select", c).args(ArgDef.choice("action", "coords", "size", "cost", "vert", "sky", "bedrock", "expand", "shift", "chunk",
                "residence", "worldedit", "clear", "options", "outward")));
        add(CommandSpec.of("expand", c).args(ArgDef.residence().optional(true), ArgDef.amount("amount", 1, 256)));
        add(CommandSpec.of("contract", c).args(ArgDef.residence().optional(true), ArgDef.amount("amount", 1, 256)));
        add(CommandSpec.of("subzone", c).args(ArgDef.residence().optional(true), ArgDef.text("subzone_name")));
        add(CommandSpec.of("sublist", c).args(ArgDef.residence().optional(true)));
        add(CommandSpec.of("area", c).args(ArgDef.choice("action", "list", "listall", "add", "remove", "replace"), ArgDef.residence(),
                ArgDef.text("area_name").optional(true)));
        add(CommandSpec.of("lset", c).args(ArgDef.residence(), ArgDef.choice("type", "info", "blacklist", "ignorelist"),
                ArgDef.text("material").optional(true)));
        add(CommandSpec.of("tpset", c));
    }

    private static void teleport() {
        String c = "teleport";
        add(CommandSpec.of("tp", c).args(ArgDef.residence().optional(true)));
        add(CommandSpec.of("tpconfirm", c));
        add(CommandSpec.of("compass", c).args(ArgDef.residence().optional(true)));
        add(CommandSpec.of("rt", c).args(ArgDef.world().optional(true)));
        add(CommandSpec.of("unstuck", c));
    }

    private static void economy() {
        String c = "economy";
        add(CommandSpec.of("market", c).args(ArgDef.choice("action", "list", "sell", "unsell", "buy", "rent", "rentable", "autopay", "payrent", "release",
                "unrent", "sign", "confirm")));
        add(CommandSpec.of("bank", c).args(ArgDef.choice("action", "balance", "deposit", "withdraw"), ArgDef.residence().optional(true),
                ArgDef.amount("amount", 1, 1000000).optional(true)));
        add(CommandSpec.of("shop", c).args(ArgDef.choice("action", "list", "vote", "like", "votes", "likes", "setdesc", "createboard", "deleteboard"),
                ArgDef.residence().optional(true)));
        add(CommandSpec.of("lease", c).args(ArgDef.choice("action", "cost", "renew", "list", "expires"), ArgDef.residence().optional(true)));
    }

    private static void raid() {
        String c = "raid";
        add(CommandSpec.of("attack", c).args(ArgDef.residence().optional(true)));
        add(CommandSpec.of("defend", c).args(ArgDef.residence().optional(true), ArgDef.player().optional(true)));
        add(CommandSpec.of("raidstatus", c).args(ArgDef.residence().optional(true)));
        add(CommandSpec.of("leaveraid", c));
        add(CommandSpec.of("raid", c).admin().args(ArgDef.choice("action", "start", "stop", "kick", "immunity"), ArgDef.residence().optional(true),
                ArgDef.choice("immunity_action", "add", "take", "set", "clear").optional(true)));
    }

    private static void chat() {
        String c = "chat";
        add(CommandSpec.of("rc", c).args(ArgDef.choice("action", "join", "leave", "setcolor", "setprefix", "kick"), ArgDef.residence().optional(true),
                ArgDef.player().optional(true)));
        add(CommandSpec.of("message", c).chatRecommended()
                .args(ArgDef.residence(), ArgDef.choice("type", "enter", "leave", "remove"), ArgDef.rawText("message").optional(true)));
    }

    private static void admin() {
        String c = "admin";
        add(CommandSpec.of("resadmin", c).admin().args(ArgDef.choice("state", "on", "off")));
        add(CommandSpec.of("server", c).admin().args(ArgDef.residence()));
        add(CommandSpec.of("setowner", c).admin().args(ArgDef.residence(), ArgDef.player(), ArgDef.choice("keep_flags", "true", "false").optional(true)));
        add(CommandSpec.of("setallowner", c).admin().dangerous().args(ArgDef.player(), ArgDef.player()));
        add(CommandSpec.of("clearflags", c).admin().dangerous().args(ArgDef.residence()));
        add(CommandSpec.of("save", c).admin().args(ArgDef.residence()));
        add(CommandSpec.of("restore", c).admin().dangerous().args(ArgDef.residence()));
        add(CommandSpec.of("listhidden", c).admin().args(ArgDef.player().optional(true)));
        add(CommandSpec.of("listallhidden", c).admin());
        add(CommandSpec.of("reload", c).admin().args(ArgDef.choice("target", "config", "lang", "groups", "flags")));
        add(CommandSpec.of("signupdate", c).admin());
        add(CommandSpec.of("signconvert", c).admin());
        add(CommandSpec.of("version", c));
    }

    private static void danger() {
        String c = "danger";
        add(CommandSpec.of("remove", c).dangerous().args(ArgDef.residence()));
        add(CommandSpec.of("removeall", c).admin().dangerous().args(ArgDef.player()));
        add(CommandSpec.of("removeworld", c).admin().dangerous().args(ArgDef.world()));
    }

    // ------------------------------------------------------------------
    // lookups
    // ------------------------------------------------------------------

    public static Map<String, CommandSpec> getAll() {
        return Collections.unmodifiableMap(SPECS);
    }

    public static CommandSpec get(String name) {
        if (name == null)
            return null;
        return SPECS.get(name.toLowerCase(Locale.ENGLISH));
    }

    /**
     * All specs of one chapter, in declaration order.
     *
     * @param chapterId chapter id such as {@code flags}
     */
    public static List<CommandSpec> getCategory(String chapterId) {
        List<CommandSpec> list = new ArrayList<CommandSpec>();
        if (chapterId == null)
            return list;
        for (CommandSpec spec : SPECS.values())
            if (spec.getCategory().equalsIgnoreCase(chapterId))
                list.add(spec);
        return list;
    }

    /**
     * Specs of a chapter. When the chapter lists explicit command names in the
     * config only those are returned, in the configured order. Unknown names are
     * ignored so a typo in the config never breaks the menu.
     */
    public static List<CommandSpec> getCategory(Chapter chapter) {
        if (chapter == null)
            return new ArrayList<CommandSpec>();
        if (chapter.getCommandNames() == null || chapter.getCommandNames().isEmpty())
            return getCategory(chapter.getId());

        List<CommandSpec> list = new ArrayList<CommandSpec>();
        for (String name : chapter.getCommandNames()) {
            CommandSpec spec = get(name);
            if (spec != null && !list.contains(spec))
                list.add(spec);
        }
        return list;
    }

    /**
     * Translation key of the display name of a command.
     */
    public static String nameKey(String commandName) {
        return "menu.command." + key(commandName) + ".name";
    }

    /**
     * Translation key of the short description of a command.
     */
    public static String describeKey(String commandName) {
        return "menu.command." + key(commandName) + ".desc";
    }

    /**
     * Translation key of the label of an argument, e.g.
     * {@code menu.arg.residence}.
     */
    public static String argLabelKey(String commandName, ArgDef def) {
        if (def == null)
            return "menu.arg.value";
        return "menu.arg." + def.getLabelKey();
    }

    /**
     * Translation key of a literal choice value.
     */
    public static String choiceKey(String value) {
        return "menu.choice." + key(value);
    }

    private static String key(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ENGLISH);
    }

    // ------------------------------------------------------------------
    // diagnostics
    // ------------------------------------------------------------------

    /**
     * Commands that the plugin exposes but the menu does not describe. Only
     * populated after {@link #validate()}.
     */
    public static Set<String> getMissing() {
        return Collections.unmodifiableSet(MISSING);
    }

    /**
     * Cross checks the registry against the commands the plugin actually loaded.
     * Any command the plugin knows about but the menu does not is collected so it
     * is visible in the log.
     */
    public static void validate() {
        MISSING.clear();
        try {
            Residence plugin = Residence.getInstance();
            if (plugin == null || plugin.getCommandFiller() == null)
                return;
            Set<String> actual = plugin.getCommandFiller().getCommandMap().keySet();
            for (String name : actual) {
                String key = name.toLowerCase(Locale.ENGLISH);
                if (!SPECS.containsKey(key) && !key.equals("menu"))
                    MISSING.add(key);
            }
        } catch (Throwable ignored) {
            // The registry works standalone even if the plugin is not loaded.
        }
    }
}
