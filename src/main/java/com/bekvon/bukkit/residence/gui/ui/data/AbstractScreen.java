package com.bekvon.bukkit.residence.gui.ui.data;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.gui.ui.MenuFormat;
import com.bekvon.bukkit.residence.gui.ui.MenuNavigator;
import com.bekvon.bukkit.residence.gui.ui.config.MenuLayout;
import com.bekvon.bukkit.residence.gui.ui.text.MenuText;
import com.bekvon.bukkit.residence.gui.ui.text.UiLanguageManager;

/**
 * Shared skeleton of every screen.
 * <p>
 * A concrete screen fills {@link #collect(Player)} and
 * {@link #decorate(MenuView.Builder, Player)}. The base class guarantees that the
 * title, the breadcrumb, the footer, the page buttons and the empty state are
 * produced the same way on every screen, which is what makes the chest menu and
 * the Bedrock form look uniform.
 */
public abstract class AbstractScreen implements ScreenProvider {

    protected final Residence plugin;
    protected final MenuScreens screens;
    protected final DataProviders data;
    /** Item source of the argument lists. */
    protected final ArgSources args;
    protected final MenuFormat format;
    protected final UiLanguageManager lang;
    protected final MenuLayout layout;
    protected final MenuNavigator navigator;
    /** Screen target, e.g. a chapter id or a residence name. */
    protected final String target;
    protected final int page;

    protected final List<MenuItem> items = new ArrayList<MenuItem>();
    protected final List<MenuAction> itemActions = new ArrayList<MenuAction>();
    protected final List<ViewNode> shortcuts = new ArrayList<ViewNode>();
    /** Actions of the shortcuts, by the index in {@link #shortcuts}. */
    protected final List<MenuAction> shortcutActions = new ArrayList<MenuAction>();
    protected final List<Component> formContent = new ArrayList<Component>();

    protected AbstractScreen(Residence plugin, MenuScreens screens, String target, int page) {
        this.plugin = plugin;
        this.screens = screens;
        this.data = plugin.getDataProviders();
        this.args = plugin.createArgSources();
        this.format = plugin.getMenuFormat();
        this.lang = plugin.getUiLanguage();
        this.layout = plugin.getMenuSettings().getLayout();
        this.navigator = screens == null ? null : screens.getNavigator();
        this.target = target;
        this.page = Math.max(1, page);
    }

    // ------------------------------------------------------------------
    // screen contract
    // ------------------------------------------------------------------

    @Override
    public abstract String kind();

    /** Translation key of the title segment. */
    protected abstract String titleKey();

    /**
     * Fills {@link #items} and {@link #itemActions}.
     */
    protected abstract void collect(Player player);

    /**
     * Adds shortcuts and footer buttons.
     */
    protected abstract void decorate(MenuView.Builder builder, Player player);

    /**
     * Placeholder kind used when the list is empty, see
     * {@link Screens#emptyState}.
     */
    protected String emptyKind() {
        return "action";
    }

    /**
     * Hints appended to every list item.
     */
    protected abstract List<String> itemHintKeys();

    @Override
    public String target() {
        return target;
    }

    @Override
    public int page() {
        return page;
    }

    /**
     * Rendered title segment, e.g. {@code Flags}. Returns an empty string for a
     * screen that is the root, in which case the title is just the brand.
     */
    @Override
    public String path(Player player) {
        String key = titleKey();
        if (key == null || key.isEmpty())
            return "";
        return lang.getPlain(player, key, pathArgs(player));
    }

    /**
     * Placeholders of the title segment.
     */
    protected MenuText.Args pathArgs(Player player) {
        MenuText.Args args = MenuText.Args.of();
        if (target != null)
            args.put("target", target);
        return args;
    }

    @Override
    public final MenuView build(Player player, int page) {
        items.clear();
        itemActions.clear();
        shortcuts.clear();
        shortcutActions.clear();
        formContent.clear();
        collect(player);

        MenuView.Builder builder = MenuView.builder(format.title(player, path(player)));
        for (Component line : formHeader(player))
            builder.content(line);

        if (items.isEmpty()) {
            builder.item(Screens.emptyState(plugin, player, emptyKind(), emptyArgs(player)));
        } else {
            builder.items(itemNodes(player));
        }

        decorate(builder, player);
        builder.shortcuts(shortcuts);
        return builder.build();
    }

    /**
     * Header lines of the Bedrock form.
     */
    protected List<Component> formHeader(Player player) {
        List<Component> out = new ArrayList<Component>();
        out.add(lang.get(player, "menu.form." + kind(), formArgs(player)));
        return out;
    }

    protected MenuText.Args formArgs(Player player) {
        MenuText.Args args = MenuText.Args.of().put("total", items.size()).put("page", page);
        if (target != null)
            args.put("target", target);
        return args;
    }

    protected MenuText.Args emptyArgs(Player player) {
        MenuText.Args args = MenuText.Args.of();
        if (target != null)
            args.put("target", target);
        return args;
    }

    // ------------------------------------------------------------------
    // items
    // ------------------------------------------------------------------

    /**
     * Converts the collected items into view nodes.
     * <p>
     * A list entry is placed on the next free slot of the content area; the
     * renderer moves it to the page position when it draws the page.
     */
    protected List<ViewNode> itemNodes(Player player) {
        List<ViewNode> out = new ArrayList<ViewNode>();
        List<String> hints = itemHintKeys();
        int index = 0;
        for (MenuItem item : items) {
            List<Component> lore = item.renderLore(format, player, hints);
            out.add(ViewNode.builder(index, item.getMaterial())
                    .name(item.getName())
                    .lore(lore)
                    .paged(true)
                    .index(index)
                    .icon(item.getIconPath(), item.getIconUrl())
                    .build());
            index++;
        }
        return out;
    }

    /**
     * Registers an item together with its click action.
     */
    protected void add(MenuItem item, MenuAction action) {
        if (item == null)
            return;
        items.add(item);
        itemActions.add(action == null ? MenuAction.none() : action);
    }

    // ------------------------------------------------------------------
    // footer
    // ------------------------------------------------------------------

    /**
     * Adds the previous / next page buttons when the list needs them.
     */
    protected void addPagingFooter(MenuView.Builder builder, Player player) {
        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) contentSlots()));
        if (totalPages <= 1 && !showsPagingWhenSinglePage())
            return;
        int current = Math.max(1, Math.min(page, totalPages));
        builder.footer("previous", Screens.pageButton(plugin, player, "previous", current > 1 && totalPages > 1, Math.max(1, current - 1), totalPages));
        builder.footer("next", Screens.pageButton(plugin, player, "next", current < totalPages, Math.min(totalPages, current + 1), totalPages));
        builder.footer("page_indicator", Screens.pageIndicator(plugin, player, current, totalPages, pageArgs(player)));
    }

    protected MenuText.Args pageArgs(Player player) {
        return MenuText.Args.of();
    }

    /**
     * Whether the footer shows a page counter even when everything fits on one
     * page.
     */
    protected boolean showsPagingWhenSinglePage() {
        return false;
    }

    /**
     * Number of slots of the paged content area.
     */
    public static int contentSlots() {
        return MenuLayout.CONTENT_SLOTS;
    }

    /**
     * Adds the back and close buttons.
     */
    protected void addNavigationFooter(MenuView.Builder builder, Player player) {
        if (navigator != null && navigator.canGoBack())
            builder.footer("back", Screens.footer(plugin, player, "back", null, null));
        builder.footer("close", Screens.footer(plugin, player, "close", null, null));
    }

    /**
     * Adds every standard footer button: paging, back, close and - for Bedrock
     * clients - the language picker.
     */
    protected void addStandardFooter(MenuView.Builder builder, Player player) {
        addPagingFooter(builder, player);
        addNavigationFooter(builder, player);
        addLanguageFooter(builder, player);
    }

    /**
     * The language picker is only shown to Bedrock clients because a Java
     * client is matched from its own locale automatically.
     */
    protected void addLanguageFooter(MenuView.Builder builder, Player player) {
        if (!plugin.getMenuSettings().isShowLanguageButton())
            return;
        if (!com.bekvon.bukkit.residence.gui.bedrock.BedrockSupport.isBedrockViewer(player))
            return;
        builder.footer("language", Screens.footer(plugin, player, "language", languageArgs(player), null));
    }

    protected MenuText.Args languageArgs(Player player) {
        String code = lang.languageFor(player);
        MenuText.Args args = MenuText.Args.of().put("language", code);
        args.put("languageName", MenuText.plain(lang.getPlain(lang.getDefaultLanguage(), "menu.language." + UiLanguageManager.normalize(code) + ".name", null), null));
        return args;
    }

    // ------------------------------------------------------------------
    // ScreenProvider
    // ------------------------------------------------------------------

    @Override
    public MenuAction actionFor(int index) {
        if (index < 0 || index >= itemActions.size())
            return MenuAction.none();
        MenuAction action = itemActions.get(index);
        return action == null ? MenuAction.none() : action;
    }

    /**
     * The action of a shortcut button, used by both renderers.
     */
    @Override
    public MenuAction actionForShortcut(int index) {
        if (index < 0 || index >= shortcutActions.size())
            return MenuAction.none();
        MenuAction action = shortcutActions.get(index);
        return action == null ? MenuAction.none() : action;
    }

    /**
     * The action of a footer button. Unlike {@link #actionForFooter(String)} this
     * also resolves the footer buttons that are not navigation, e.g. the
     * confirmation buttons, so the Bedrock renderer can map them to a button
     * index.
     */
    public MenuAction actionForFooterNode(String id) {
        MenuAction action = actionForFooter(id);
        return action == null ? MenuAction.none() : action;
    }

    /**
     * Adds a shortcut together with its action.
     */
    protected void shortcut(ViewNode node, MenuAction action) {
        if (node == null)
            return;
        shortcuts.add(node);
        shortcutActions.add(action == null ? MenuAction.none() : action);
    }

    @Override
    public MenuAction actionForFooter(String id) {
        if ("back".equals(id))
            return MenuAction.back();
        if ("close".equals(id))
            return MenuAction.close();
        if ("language".equals(id))
            return MenuAction.languagePicker();
        if ("previous".equals(id))
            return MenuAction.page(Math.max(1, page - 1));
        if ("next".equals(id))
            return MenuAction.page(page + 1);
        return null;
    }

    @Override
    public ScreenContext.FlowInfo flow() {
        return null;
    }

    @Override
    public List<MenuAction> actions() {
        return itemActions;
    }
}
