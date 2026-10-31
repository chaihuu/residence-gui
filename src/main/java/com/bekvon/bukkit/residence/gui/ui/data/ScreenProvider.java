package com.bekvon.bukkit.residence.gui.ui.data;

import java.util.List;

import org.bukkit.entity.Player;

/**
 * Builds one screen of the menu.
 * <p>
 * The renderers only talk to a {@link ScreenProvider}: they ask for a
 * {@link MenuView} and for the {@link MenuAction} of every node, then draw it in
 * their own way. All business logic therefore lives in one place and the chest
 * menu and the Bedrock form stay structurally identical.
 */
public interface ScreenProvider {

    /**
     * Builds the immutable content of the screen.
     *
     * @param page 1 based page number
     */
    MenuView build(Player player, int page);

    /**
     * The action attached to an entry of the item list.
     *
     * @param index index inside {@link MenuView#getItems()}
     */
    MenuAction actionFor(int index);

    /**
     * The action attached to a footer button such as {@code back} or
     * {@code close}, or {@code null} when this screen has no such button.
     */
    MenuAction actionForFooter(String id);

    /**
     * The action of a shortcut button. Shortcuts are the fixed slot buttons,
     * e.g. the residence the player stands in.
     *
     * @param index index inside {@link MenuView#getShortcuts()}
     */
    MenuAction actionForShortcut(int index);

    /**
     * Description of the command wizard state, {@code null} outside a command
     * flow.
     */
    ScreenContext.FlowInfo flow();

    /**
     * Rendered path used in the title, e.g. {@code Flags}.
     */
    String path(Player player);

    /** Screen kind, see {@link Screens}. */
    String kind();

    /** Screen target: a chapter id, a residence name, an argument index ... */
    String target();

    /** Page the screen was built for. */
    int page();

    /** All item actions, in list order. */
    List<MenuAction> actions();
}
