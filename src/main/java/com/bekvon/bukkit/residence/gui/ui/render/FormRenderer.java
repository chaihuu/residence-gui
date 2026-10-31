package com.bekvon.bukkit.residence.gui.ui.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.gui.bedrock.FloodgateBridge;
import com.bekvon.bukkit.residence.gui.ui.config.MenuLayout;
import com.bekvon.bukkit.residence.gui.ui.config.MenuSettings;
import com.bekvon.bukkit.residence.gui.ui.data.MenuAction;
import com.bekvon.bukkit.residence.gui.ui.data.MenuView;
import com.bekvon.bukkit.residence.gui.ui.data.ScreenContext;
import com.bekvon.bukkit.residence.gui.ui.data.ScreenProvider;
import com.bekvon.bukkit.residence.gui.ui.data.ViewNode;
import com.bekvon.bukkit.residence.gui.ui.text.MenuText;

/**
 * Draws a {@link MenuView} as a Bedrock {@code SimpleForm}.
 * <p>
 * The form is built from exactly the same {@link MenuView} the chest menu uses,
 * so titles, entries and hints match. Bedrock specific rules are handled here:
 * <ul>
 * <li>a form has no tooltips, so a button shows its name and the first
 * description line, while the full description stays in the header,</li>
 * <li>long lists are paginated to {@code Layout.Bedrock.max-buttons} so the
 * form never becomes an endless scroll,</li>
 * <li>icons are optional and come from the config
 * ({@code bedrock.icon-path} / {@code bedrock.icon-url}),</li>
 * <li>when Floodgate cannot send a form the method reports it and the caller
 * falls back to the inventory, so a player never ends up with a dead menu.</li>
 * </ul>
 */
public final class FormRenderer {

    /** Result of an attempt to show a form. */
    public enum Result {
        /** The form was sent. */
        SENT,
        /** The form API is not usable, the caller has to open the chest menu. */
        UNAVAILABLE
    }

    private final Residence plugin;
    private final FloodgateBridge bridge;

    public FormRenderer(Residence plugin, FloodgateBridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    /**
     * Whether a form can be shown to this player right now.
     */
    public boolean canShow(Player player) {
        if (player == null || !player.isOnline())
            return false;
        MenuSettings settings = plugin.getMenuSettings();
        if (!settings.useFormFor(com.bekvon.bukkit.residence.gui.bedrock.BedrockSupport.isBedrockViewer(player), bridge.isAvailable()))
            return false;
        return true;
    }

    /**
     * Shows the form of a screen.
     * <p>
     * Every button is followed by the button index in {@link #handle}, so the
     * click handler never has to guess the mapping.
     *
     * @param onAction receives the action of the pressed button
     * @param onClose  called when the form is closed without a click
     */
    public Result show(Player player, ScreenProvider provider, MenuView view, int page, Consumer<MenuAction> onAction, Runnable onClose) {
        if (!canShow(player))
            return Result.UNAVAILABLE;

        MenuLayout layout = plugin.getMenuSettings().getLayout();
        int maxButtons = layout.getBedrockMaxButtons();
        ScreenContext context = new ScreenContext(player, view, provider, page, layout.contentSlots());

        // Bedrock forms keep the current page, so a page of the chest list maps
        // one to one; the visible amount is limited by the form height.
        List<ViewNode> pageItems = view.getPage(page, layout.contentSlots());

        List<FloodgateBridge.Button> buttons = new ArrayList<FloodgateBridge.Button>();
        List<MenuAction> actions = new ArrayList<MenuAction>();

        for (int i = 0; i < pageItems.size() && buttons.size() < maxButtons; i++) {
            ViewNode node = pageItems.get(i);
            if (!node.isClickable())
                continue;
            int start = (Math.max(1, page) - 1) * layout.contentSlots();
            add(buttons, actions, player, node, context.actionForIndex(start + i));
        }

        for (int i = 0; i < view.getShortcuts().size() && buttons.size() < maxButtons; i++) {
            ViewNode node = view.getShortcuts().get(i);
            if (!node.isClickable())
                continue;
            add(buttons, actions, player, node, provider.actionForShortcut(i));
        }

        for (MenuView.Footer footer : view.getFooter()) {
            if (buttons.size() >= maxButtons)
                break;
            ViewNode node = footer.getNode();
            if (!node.isClickable())
                continue;
            add(buttons, actions, player, node, context.actionForFooter(footer.getId()));
        }

        if (buttons.isEmpty())
            return Result.UNAVAILABLE;

        String title = MenuText.plain(view.getTitle());
        String content = content(player, view, provider, page, layout);

        boolean sent = bridge.sendSimpleForm(player, title, content, buttons, new Consumer<Integer>() {
            @Override
            public void accept(Integer index) {
                if (index == null || index < 0 || index >= actions.size())
                    return;
                MenuAction action = actions.get(index);
                if (action != null)
                    onAction.accept(action);
            }
        }, onClose);
        return sent ? Result.SENT : Result.UNAVAILABLE;
    }

    private void add(List<FloodgateBridge.Button> buttons, List<MenuAction> actions, Player player, ViewNode node, MenuAction action) {
        buttons.add(new FloodgateBridge.Button(buttonText(player, node), node.getIconPath(), node.getIconUrl()));
        actions.add(action == null ? MenuAction.none() : action);
    }

    /**
     * A form button label: the name, plus the most useful description line.
     * <p>
     * A Bedrock form has a fixed button height, so a long tooltip is not shown
     * on the button; the selected state is prefixed with the configured marker
     * instead.
     */
    private String buttonText(Player player, ViewNode node) {
        String name = MenuText.plain(node.getName());
        if (node.isSelected()) {
            String marker = plugin.getUiLanguage().getPlain(player, "menu.form.selected-marker", null);
            if (marker != null && !marker.isEmpty())
                name = marker + name;
        }
        return name;
    }

    /**
     * The form description: the header lines of the screen, the page counter
     * and the full description of every visible entry, so nothing is lost
     * compared to the tooltip of the chest menu.
     */
    private String content(Player player, MenuView view, ScreenProvider provider, int page, MenuLayout layout) {
        StringBuilder sb = new StringBuilder();
        for (Component line : view.getFormContent()) {
            String text = MenuText.plain(line);
            if (text == null || text.isEmpty())
                continue;
            if (sb.length() > 0)
                sb.append('\n');
            sb.append(text);
        }

        int totalPages = view.getTotalPages(layout.contentSlots());
        if (totalPages > 1) {
            MenuText.Args args = MenuText.Args.of().put("page", page).put("pages", totalPages);
            String paging = plugin.getUiLanguage().getPlain(player, "menu.form.page", args);
            if (paging != null && !paging.isEmpty())
                sb.append(sb.length() > 0 ? "\n" : "").append(paging);
        }
        return sb.toString();
    }
}
