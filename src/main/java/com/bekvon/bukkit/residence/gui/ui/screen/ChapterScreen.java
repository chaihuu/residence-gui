package com.bekvon.bukkit.residence.gui.ui.screen;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.gui.ui.data.AbstractScreen;
import com.bekvon.bukkit.residence.gui.ui.data.Chapter;
import com.bekvon.bukkit.residence.gui.ui.data.CommandSpec;
import com.bekvon.bukkit.residence.gui.ui.data.MenuAction;
import com.bekvon.bukkit.residence.gui.ui.data.MenuItem;
import com.bekvon.bukkit.residence.gui.ui.data.MenuScreens;
import com.bekvon.bukkit.residence.gui.ui.data.MenuView;
import com.bekvon.bukkit.residence.gui.ui.data.Screens;
import com.bekvon.bukkit.residence.gui.ui.text.MenuText;

/**
 * Lists the commands of one chapter.
 * <p>
 * Each entry is a {@link CommandSpec} translated through
 * {@code menu.command.<name>.desc} and carries the state hints (admin, unsafe,
 * missing permission) as its lore. Locked commands stay visible but cannot be
 * clicked, which is friendlier than hiding them.
 */
public class ChapterScreen extends AbstractScreen {

    private final Chapter chapter;

    public ChapterScreen(Residence plugin, MenuScreens screens, String chapterId, int page) {
        super(plugin, screens, chapterId, page);
        this.chapter = find(chapterId);
    }

    private Chapter find(String id) {
        if (id == null)
            return null;
        for (Chapter one : plugin.getMenuSettings().getChapters())
            if (one.getId().equalsIgnoreCase(id))
                return one;
        return null;
    }

    @Override
    public String kind() {
        return Screens.CHAPTER;
    }

    @Override
    protected String titleKey() {
        return "menu.chapter." + (chapter == null ? "general" : chapter.getId()) + ".name";
    }

    @Override
    protected String emptyKind() {
        return "action";
    }

    @Override
    protected List<String> itemHintKeys() {
        return Arrays.asList("menu.hint.left_click");
    }

    @Override
    protected void collect(Player player) {
        if (chapter == null)
            return;
        for (CommandSpec spec : screens.specsOf(chapter)) {
            boolean allowed = data.canUse(player, spec);
            MenuText.Args args = MenuText.Args.of().put("command", spec.getName()).put("label",
                    lang.getPlain(player, "menu.command." + spec.getName() + ".name", null));

            List<String> lore = new ArrayList<String>();
            lore.add(spec.getDescriptionKey());
            if (spec.isDangerous())
                lore.add("menu.command.flag.dangerous");
            if (spec.isAdmin())
                lore.add("menu.command.flag.admin");
            if (spec.isChatRecommended())
                lore.add("menu.command.flag.chat");
            if (!allowed)
                lore.add("menu.command.flag.no-permission");

            add(new MenuItem(spec.getName(), lang.get(player, "menu.command." + spec.getName() + ".name", args), lore, args, allowed ? iconFor(spec) : "BARRIER",
                    null, null), allowed ? MenuAction.command(spec.getName()) : MenuAction.none());
        }
    }

    private String iconFor(CommandSpec spec) {
        if (spec.isDangerous())
            return "RED_CONCRETE";
        if (spec.isAdmin())
            return "ORANGE_CONCRETE";
        return "PAPER";
    }

    @Override
    protected void decorate(MenuView.Builder builder, Player player) {
        addStandardFooter(builder, player);
    }

    @Override
    protected List<Component> formHeader(Player player) {
        List<Component> out = new ArrayList<Component>();
        MenuText.Args args = MenuText.Args.of().put("count", items.size()).put("total", items.size());
        out.add(lang.get(player, "menu.form.chapter", args));
        if (chapter != null)
            out.add(lang.get(player, "menu.chapter." + chapter.getId() + ".lore", args));
        return out;
    }

    public Chapter getChapter() {
        return chapter;
    }
}
