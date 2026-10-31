package com.bekvon.bukkit.residence.gui.ui.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Immutable description of a single {@code /res} sub command as it should be
 * presented inside the menu.
 * <p>
 * This is deliberately kept independent from the command classes themselves so
 * that the menu can be written declaratively and stay readable. The command
 * names are still validated against the real command registry by
 * {@link CommandRegistry}, so a typo here can never create a menu entry that
 * executes something the plugin does not support.
 */
public class CommandSpec {

    private final String name;
    private final String category;
    private final boolean admin;
    private final boolean dangerous;
    private final boolean requiresPlayer;
    private final boolean inGameOnly;
    private final List<ArgDef> arguments = new ArrayList<ArgDef>();
    /** Optional hint shown in the lore when the command works better from chat. */
    private final boolean chatRecommended;

    private CommandSpec(String name, String category, boolean admin, boolean dangerous, boolean requiresPlayer, boolean inGameOnly,
            boolean chatRecommended) {
        this.name = name;
        this.category = category;
        this.admin = admin;
        this.dangerous = dangerous;
        this.requiresPlayer = requiresPlayer;
        this.inGameOnly = inGameOnly;
        this.chatRecommended = chatRecommended;
    }

    /**
     * A plain sub command of a chapter.
     *
     * @param name     internal command name, also the translation key suffix
     * @param category chapter id
     */
    public static CommandSpec of(String name, String category) {
        return new CommandSpec(name, category, false, false, true, false, false);
    }

    public CommandSpec admin() {
        return new CommandSpec(name, category, true, dangerous, requiresPlayer, inGameOnly, chatRecommended);
    }

    public CommandSpec dangerous() {
        return new CommandSpec(name, category, admin, true, requiresPlayer, inGameOnly, chatRecommended);
    }

    /**
     * Command also works from console, so the menu does not have to require a
     * player to run it.
     */
    public CommandSpec consoleAllowed() {
        return new CommandSpec(name, category, admin, dangerous, false, inGameOnly, chatRecommended);
    }

    /**
     * Command is driven entirely through chat input (multi word text).
     */
    public CommandSpec chatRecommended() {
        return new CommandSpec(name, category, admin, dangerous, requiresPlayer, inGameOnly, true);
    }

    public CommandSpec args(ArgDef... defs) {
        arguments.addAll(Arrays.asList(defs));
        return this;
    }

    /**
     * Appends a residence argument that defaults to the residence the player is
     * standing in when clicked without typing one.
     */
    public CommandSpec residenceArg() {
        arguments.add(ArgDef.residence());
        return this;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isDangerous() {
        return dangerous;
    }

    public boolean requiresPlayer() {
        return requiresPlayer;
    }

    public boolean isInGameOnly() {
        return inGameOnly;
    }

    public boolean isChatRecommended() {
        return chatRecommended;
    }

    public List<ArgDef> getArguments() {
        return Collections.unmodifiableList(arguments);
    }

    public int getArgumentCount() {
        return arguments.size();
    }

    /**
     * Number of arguments required before the command can run.
     */
    public int getRequiredArguments() {
        int required = 0;
        for (ArgDef def : arguments) {
            if (!def.isOptional() && !def.isSkipped())
                required++;
        }
        return required;
    }

    /**
     * Translator independent usage line, e.g.
     * {@code /res padd <residence> [player]}. The argument names are the
     * internal identifiers, which keeps the line readable in a log; the menu
     * shows the translated version from {@code menu.command.<name>.usage}.
     */
    public String getUsage() {
        StringBuilder sb = new StringBuilder("/res ").append(name);
        for (ArgDef def : arguments) {
            if (def.isSkipped())
                continue;
            sb.append(' ').append(def.isOptional() ? "[" : "<").append(def.getLabelKey());
            sb.append(def.isOptional() ? "]" : ">");
        }
        return sb.toString();
    }

    /**
     * Translation key of the short description.
     */
    public String getDescriptionKey() {
        return CommandRegistry.describeKey(name);
    }
}
