package com.bekvon.bukkit.residence.gui.ui.data;

import java.util.List;
import java.util.Locale;

import org.bukkit.entity.Player;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.gui.ui.MenuNavigator;
import com.bekvon.bukkit.residence.gui.ui.screen.ArgumentScreen;
import com.bekvon.bukkit.residence.gui.ui.screen.ChapterScreen;
import com.bekvon.bukkit.residence.gui.ui.screen.ConfirmScreen;
import com.bekvon.bukkit.residence.gui.ui.screen.LanguageScreen;
import com.bekvon.bukkit.residence.gui.ui.screen.MainScreen;
import com.bekvon.bukkit.residence.gui.ui.screen.ResidenceScreen;

/**
 * Factory for every screen of the menu, for one player session.
 * <p>
 * Both user interfaces use this factory, so a screen is described exactly once
 * whether it is drawn as an inventory or as a Bedrock form.
 */
public final class MenuScreens {

    private final Residence plugin;
    private final Player player;
    private final MenuNavigator navigator;

    /** Current command wizard state. */
    private CommandSpec wizardSpec = null;
    private java.util.List<String> wizardPicked = null;
    private int wizardIndex = 0;
    private boolean wizardConfirm = false;

    public MenuScreens(Residence plugin, Player player, MenuNavigator navigator) {
        this.plugin = plugin;
        this.player = player;
        this.navigator = navigator;
    }

    public Residence getPlugin() {
        return plugin;
    }

    public Player getPlayer() {
        return player;
    }

    public MenuNavigator getNavigator() {
        return navigator;
    }

    /**
     * Builds a screen.
     *
     * @param kind   see {@link Screens}
     * @param target screen target, meaning depends on the kind
     * @param page   1 based page
     */
    public ScreenProvider provider(String kind, String target, int page) {
        String key = kind == null ? Screens.MAIN : kind.toLowerCase(Locale.ENGLISH);
        int current = Math.max(1, page);
        switch (key) {
        case Screens.CHAPTER:
            return new ChapterScreen(plugin, this, target, current);
        case Screens.RESIDENCE:
            return new ResidenceScreen(plugin, this, target, current);
        case Screens.ARGUMENT:
            return ArgumentScreen.restore(plugin, this, target, current);
        case Screens.CONFIRM:
            return ConfirmScreen.restore(plugin, this, target, current);
        case Screens.LANGUAGE:
            return new LanguageScreen(plugin, this, current);
        case Screens.MAIN:
        default:
            return new MainScreen(plugin, this, current);
        }
    }

    /**
     * Starts the command wizard of a sub command, optionally with a preset
     * residence for the first argument.
     */
    public void openCommand(String command, String presetResidence) {
        CommandSpec spec = CommandRegistry.get(command);
        if (spec == null) {
            openMain();
            return;
        }
        CommandFlow.start(plugin, this, spec, presetResidence);
    }

    /**
     * Opens a screen and remembers the current one for the back button.
     */
    public void open(String kind, String target, int page) {
        plugin.getMenuManager().navigate(player, kind, target, page);
    }

    public void openMain() {
        open(Screens.MAIN, null, 1);
    }

    /**
     * Re-renders the current screen on another page, without touching history.
     */
    public void openPage(int page) {
        plugin.getMenuManager().rerender(player, page);
    }

    /**
     * Goes back one screen, or opens the root screen when the history is empty.
     */
    public void back() {
        plugin.getMenuManager().back(player);
    }

    /**
     * Closes the menu.
     */
    public void close() {
        plugin.getMenuManager().close(player);
    }

    /**
     * Renders an already built screen provider and shows it, without touching
     * the history. Used by the command wizard, whose steps are produced by
     * {@link CommandFlow} rather than by a navigation action.
     */
    public void show(ScreenProvider provider) {
        plugin.getMenuManager().show(player, provider);
    }

    // ------------------------------------------------------------------
    // command wizard state
    // ------------------------------------------------------------------

    /**
     * Remembers the wizard state so a page change or a back navigation can
     * rebuild the current step, and so the chat listener can continue a flow
     * that was interrupted by typing.
     */
    public void rememberWizard(CommandSpec spec, java.util.List<String> picked, int index, boolean confirm) {
        this.wizardSpec = spec;
        this.wizardPicked = picked == null ? new java.util.ArrayList<String>() : new java.util.ArrayList<String>(picked);
        this.wizardIndex = Math.max(0, index);
        this.wizardConfirm = confirm;
    }

    /**
     * The argument step the wizard is currently showing.
     */
    public ScreenProvider wizardArgument(int page) {
        if (wizardSpec == null)
            return provider(Screens.MAIN, null, page);
        return new ArgumentScreen(plugin, this, wizardSpec, wizardPicked, wizardIndex, page);
    }

    /**
     * The confirmation step of the wizard.
     */
    public ScreenProvider wizardConfirm(int page) {
        if (wizardSpec == null)
            return provider(Screens.MAIN, null, page);
        return new ConfirmScreen(plugin, this, wizardSpec, wizardPicked, page);
    }

    /**
     * Current wizard step; used when a renderer has to rebuild the screen it is
     * already showing.
     */
    public ScreenProvider wizardStep(int page) {
        return wizardConfirm ? wizardConfirm(page) : wizardArgument(page);
    }

    public boolean hasWizard() {
        return wizardSpec != null;
    }

    public void clearWizard() {
        this.wizardSpec = null;
        this.wizardPicked = null;
        this.wizardIndex = 0;
        this.wizardConfirm = false;
    }

    /**
     * Specs of a chapter, honouring the config override of its command list.
     */
    public List<CommandSpec> specsOf(Chapter chapter) {
        return CommandRegistry.getCategory(chapter);
    }

    /**
     * Chapters visible to this player, in display order.
     */
    public List<Chapter> chaptersFor(Player viewer) {
        List<Chapter> out = new java.util.ArrayList<Chapter>();
        DataProviders data = plugin.getDataProviders();
        for (Chapter chapter : plugin.getMenuSettings().getChapters()) {
            if (chapter.isAdmin() && !data.isAdmin(viewer))
                continue;
            out.add(chapter);
        }
        return out;
    }
}
