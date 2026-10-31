package com.bekvon.bukkit.residence.gui.ui.data;

import org.bukkit.entity.Player;

/**
 * Everything a renderer needs to draw and drive one screen.
 */
public final class ScreenContext {

    /**
     * Description of the command wizard state, shown in the Bedrock form header
     * so the form does not have to re-implement the wizard.
     */
    public interface FlowInfo {
        /** Internal command name, e.g. {@code padd}. */
        String getCommandName();

        /** Translation key of the command name. */
        String getCommandLabelKey();

        /** Current screen kind, e.g. {@code argument}. */
        String getKind();

        /** 1 based step number. */
        int getStep();

        int getTotalSteps();

        /** Translation key of the current argument label, may be {@code null}. */
        String getArgumentLabelKey();

        /** The command line that will run, may be {@code null}. */
        String getCommandLine();
    }

    private final Player player;
    private final MenuView view;
    private final ScreenProvider provider;
    private final int page;
    private final int perPage;

    public ScreenContext(Player player, MenuView view, ScreenProvider provider, int page, int perPage) {
        this.player = player;
        this.view = view;
        this.provider = provider;
        this.page = Math.max(1, page);
        this.perPage = Math.max(1, perPage);
    }

    public Player getPlayer() {
        return player;
    }

    public MenuView getView() {
        return view;
    }

    public ScreenProvider getProvider() {
        return provider;
    }

    public FlowInfo getFlow() {
        return provider == null ? null : provider.flow();
    }

    public int getPage() {
        return page;
    }

    public int getPerPage() {
        return perPage;
    }

    public int getTotalPages() {
        return view.getTotalPages(perPage);
    }

    /**
     * Action of a content item, index relative to the full list.
     */
    public MenuAction actionForIndex(int index) {
        return provider == null ? MenuAction.none() : provider.actionFor(index);
    }

    /**
     * Action of a footer button, never {@code null}.
     */
    public MenuAction actionForFooter(String id) {
        MenuAction action = provider == null ? null : provider.actionForFooter(id);
        return action == null ? MenuAction.none() : action;
    }
}
