package com.bekvon.bukkit.residence.gui.ui.data;

/**
 * Describes where the value for a command argument comes from.
 * <p>
 * Every argument of every {@code /res} sub command is modelled as a chain of
 * these types, which lets the chest menu walk the player through a command
 * without them having to remember the syntax.
 */
public enum ArgType {

    /** One of a fixed set of literal words, e.g. {@code allow}/{@code block}/{@code list}. */
    CHOICE,
    /** A residence the player can actually act on (own, admin flag or resadmin). */
    RESIDENCE,
    /** An online player. */
    PLAYER,
    /** A residence flag name. */
    FLAG,
    /** A permission group defined in groups.yml. */
    GROUP,
    /** A loaded world. */
    WORLD,
    /** true / false / remove. */
    FLAG_STATE,
    /** A whole number, entered through + / - buttons. */
    AMOUNT,
    /** Any text, entered through chat. */
    TEXT,
    /** Free text typed in chat that is sent verbatim (chat messages, command names). */
    RAW_TEXT

}
