package com.bekvon.bukkit.residence.gui.ui.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.gui.ui.data.AbstractScreen;
import com.bekvon.bukkit.residence.gui.ui.data.Chapter;
import com.bekvon.bukkit.residence.gui.ui.data.MenuAction;
import com.bekvon.bukkit.residence.gui.ui.data.MenuItem;
import com.bekvon.bukkit.residence.gui.ui.data.MenuScreens;
import com.bekvon.bukkit.residence.gui.ui.data.MenuView;
import com.bekvon.bukkit.residence.gui.ui.data.Screens;
import com.bekvon.bukkit.residence.gui.ui.text.MenuText;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;

/**
 * Root screen: one entry per chapter plus the shortcuts for the residence the
 * player stands in.
 * <p>
 * The chapter entries form the paged content area; the shortcuts are placed on
 * their configured slots. On Bedrock the same entries become the form buttons.
 */
public class MainScreen extends AbstractScreen {

    public MainScreen(Residence plugin, MenuScreens screens, int page) {
        super(plugin, screens, null, page);
    }

    @Override
    public String kind() {
        return Screens.MAIN;
    }

    @Override
    protected String titleKey() {
        // The root screen has no path, the brand is the whole title.
        return "";
    }

    @Override
    protected List<String> itemHintKeys() {
        return Collections.singletonList("menu.hint.left_click");
    }

    @Override
    protected String emptyKind() {
        return "section";
    }

    @Override
    protected void collect(Player player) {
        for (Chapter chapter : screens.chaptersFor(player)) {
            int count = screens.specsOf(chapter).size();
            MenuText.Args args = MenuText.Args.of().put("count", count).put("chapter", chapter.getId());

            List<String> lore = new ArrayList<String>();
            lore.add("menu.chapter." + chapter.getId() + ".lore");
            lore.add("menu.chapter.count");

            add(new MenuItem(chapter.getId(), lang.get(player, "menu.chapter." + chapter.getId() + ".name", args), lore, args, chapter.getIcon(),
                    chapter.getIconPath(), chapter.getIconUrl()), MenuAction.open("chapter:" + chapter.getId()));
        }
    }

    @Override
    protected void decorate(MenuView.Builder builder, Player player) {
        ClaimedResidence current = data.currentResidence(player);

        if (current != null) {
            MenuText.Args args = MenuText.Args.of().put("residence", current.getName()).put("owner", current.getOwner());
            shortcut(Screens.shortcut(plugin, player, "current_residence", args, null), MenuAction.open("residence:" + current.getName()));
            builder.content(lang.get(player, "menu.form.standing-in", args));
        } else {
            shortcut(Screens.shortcut(plugin, player, "current_residence_empty", null, null), MenuAction.none());
            builder.content(lang.get(player, "menu.form.not-in-residence", null));
        }

        shortcut(Screens.shortcut(plugin, player, "quick_flags", null, null), MenuAction.command("set"));
        shortcut(Screens.shortcut(plugin, player, "residence_list", null, null), MenuAction.command("list"));

        addStandardFooter(builder, player);
    }

    @Override
    protected boolean showsPagingWhenSinglePage() {
        // The root screen always shows the page counter so the footer never
        // jumps between screens.
        return true;
    }

    @Override
    protected List<Component> formHeader(Player player) {
        List<Component> out = new ArrayList<Component>();
        out.add(lang.get(player, "menu.form.main", null));
        return out;
    }
}
