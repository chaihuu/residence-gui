package com.bekvon.bukkit.residence.gui.ui.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.gui.ui.data.AbstractScreen;
import com.bekvon.bukkit.residence.gui.ui.data.MenuAction;
import com.bekvon.bukkit.residence.gui.ui.data.MenuItem;
import com.bekvon.bukkit.residence.gui.ui.data.MenuScreens;
import com.bekvon.bukkit.residence.gui.ui.data.MenuView;
import com.bekvon.bukkit.residence.gui.ui.data.Screens;
import com.bekvon.bukkit.residence.gui.ui.text.MenuText;
import com.bekvon.bukkit.residence.gui.ui.text.UiLanguageManager;

/**
 * Language picker.
 * <p>
 * Java clients are matched from their own locale, so this screen is mainly for
 * Bedrock players, whose console language is read through Floodgate. Choosing a
 * language here overrides the detection for the rest of the session, and both
 * user interfaces follow immediately because they share this translation
 * source.
 */
public class LanguageScreen extends AbstractScreen {

    public LanguageScreen(Residence plugin, MenuScreens screens, int page) {
        super(plugin, screens, null, page);
    }

    @Override
    public String kind() {
        return Screens.LANGUAGE;
    }

    @Override
    protected String titleKey() {
        return "menu.title.language";
    }

    @Override
    protected String emptyKind() {
        return "action";
    }

    @Override
    protected List<String> itemHintKeys() {
        return Collections.singletonList("menu.hint.left_click");
    }

    @Override
    protected void collect(Player player) {
        String active = lang.languageFor(player);
        for (String code : lang.getAvailable()) {
            boolean selected = UiLanguageManager.normalize(code).equals(UiLanguageManager.normalize(active));
            MenuText.Args args = MenuText.Args.of().put("language", code);
            List<String> lore = new ArrayList<String>();
            lore.add("menu.language." + UiLanguageManager.normalize(code) + ".lore");
            if (selected)
                lore.add("menu.language.selected");

            Component name = lang.get(player, "menu.language." + UiLanguageManager.normalize(code) + ".name", args);
            add(new MenuItem(code, name, lore, args, selected ? "LIME_DYE" : "BOOK", null, null), MenuAction.language(code));
        }
    }

    @Override
    protected void decorate(MenuView.Builder builder, Player player) {
        addStandardFooter(builder, player);
    }

    @Override
    protected List<Component> formHeader(Player player) {
        List<Component> out = new ArrayList<Component>();
        out.add(lang.get(player, "menu.form.language", null));
        return out;
    }
}
