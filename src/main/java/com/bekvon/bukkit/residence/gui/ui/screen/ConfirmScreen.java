package com.bekvon.bukkit.residence.gui.ui.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.gui.ui.data.AbstractScreen;
import com.bekvon.bukkit.residence.gui.ui.data.ArgValues;
import com.bekvon.bukkit.residence.gui.ui.data.CommandFlow;
import com.bekvon.bukkit.residence.gui.ui.data.CommandSpec;
import com.bekvon.bukkit.residence.gui.ui.data.MenuAction;
import com.bekvon.bukkit.residence.gui.ui.data.MenuItem;
import com.bekvon.bukkit.residence.gui.ui.data.MenuScreens;
import com.bekvon.bukkit.residence.gui.ui.data.MenuView;
import com.bekvon.bukkit.residence.gui.ui.data.ScreenContext;
import com.bekvon.bukkit.residence.gui.ui.data.ScreenProvider;
import com.bekvon.bukkit.residence.gui.ui.data.Screens;
import com.bekvon.bukkit.residence.gui.ui.text.MenuText;

/**
 * Final step of a command flow: shows exactly what will run and runs it on
 * confirmation.
 * <p>
 * Unsafe commands get a red confirm button so they are never run by a stray
 * click, which matters even more on Bedrock where taps are large.
 */
public class ConfirmScreen extends AbstractScreen {

    private final CommandSpec spec;
    private final List<String> picked;

    public ConfirmScreen(Residence plugin, MenuScreens screens, CommandSpec spec, List<String> picked, int page) {
        super(plugin, screens, spec == null ? null : spec.getName(), page);
        this.spec = spec;
        this.picked = picked == null ? new ArrayList<String>() : picked;
    }

    /**
     * Rebuilds the confirmation screen after a page change or a back navigation,
     * using the state stored in the screens session.
     */
    public static ScreenProvider restore(Residence plugin, MenuScreens screens, String target, int page) {
        return screens.wizardStep(page);
    }

    private String commandLine() {
        return ArgValues.commandLine(spec, picked);
    }

    @Override
    public String kind() {
        return Screens.CONFIRM;
    }

    @Override
    protected String titleKey() {
        return "menu.title.confirm";
    }

    @Override
    public String path(Player player) {
        return lang.getPlain(player, "menu.title.confirm", pathArgs(player));
    }

    @Override
    protected MenuText.Args pathArgs(Player player) {
        return MenuText.Args.of().put("command", lang.getPlain(player, "menu.command." + (spec == null ? "" : spec.getName()) + ".name", null));
    }

    @Override
    protected String emptyKind() {
        return "action";
    }

    @Override
    protected List<String> itemHintKeys() {
        return Collections.emptyList();
    }

    @Override
    protected void collect(Player player) {
        MenuText.Args args = CommandFlow.baseArgs(spec, picked);
        add(new MenuItem("preview", lang.get(player, "menu.confirm.preview", args), Collections.singletonList("menu.confirm.preview-lore"), args,
                "WRITABLE_BOOK", null, null), MenuAction.none());

        List<String> values = ArgValues.values(spec, picked);
        if (values.isEmpty()) {
            add(new MenuItem("none", lang.get(player, "menu.confirm.no-arguments", args), Collections.<String>emptyList(), args, "GRAY_STAINED_GLASS_PANE",
                    null, null), MenuAction.none());
            return;
        }
        for (int i = 0; i < values.size(); i++) {
            MenuText.Args valueArgs = MenuText.Args.of().put("index", i + 1).put("value", values.get(i));
            add(new MenuItem(values.get(i), lang.get(player, "menu.confirm.argument", valueArgs), Collections.<String>emptyList(), valueArgs, "PAPER", null,
                    null), MenuAction.none());
        }
    }

    @Override
    protected void decorate(MenuView.Builder builder, Player player) {
        boolean unsafe = spec != null && spec.isDangerous();
        MenuText.Args args = CommandFlow.baseArgs(spec, picked);
        builder.footer("confirm", Screens.footer(plugin, player, unsafe ? "confirm_danger" : "confirm", args, null));
        builder.footer("cancel", Screens.footer(plugin, player, "cancel", null, null));
        addNavigationFooter(builder, player);
    }

    @Override
    public MenuAction actionForFooter(String id) {
        if ("confirm".equals(id))
            return MenuAction.execute(spec != null && spec.isDangerous());
        if ("cancel".equals(id))
            return MenuAction.close();
        return super.actionForFooter(id);
    }

    @Override
    protected List<Component> formHeader(Player player) {
        List<Component> out = new ArrayList<Component>();
        MenuText.Args args = CommandFlow.baseArgs(spec, picked);
        out.add(lang.get(player, "menu.form.confirm", args));
        if (spec != null)
            out.add(lang.get(player, spec.getDescriptionKey(), args));
        if (spec != null && spec.isDangerous())
            out.add(lang.get(player, "menu.form.confirm-danger", args));
        return out;
    }

    @Override
    public ScreenContext.FlowInfo flow() {
        return new ScreenContext.FlowInfo() {
            @Override
            public String getCommandName() {
                return spec == null ? "" : spec.getName();
            }

            @Override
            public String getCommandLabelKey() {
                return "menu.command." + getCommandName() + ".name";
            }

            @Override
            public String getKind() {
                return Screens.CONFIRM;
            }

            @Override
            public int getStep() {
                return spec == null ? 0 : spec.getArgumentCount();
            }

            @Override
            public int getTotalSteps() {
                return spec == null ? 0 : spec.getArgumentCount();
            }

            @Override
            public String getArgumentLabelKey() {
                return null;
            }

            @Override
            public String getCommandLine() {
                return commandLine();
            }
        };
    }

    /**
     * Runs the assembled command on behalf of the player.
     */
    public void run(Player player) {
        if (spec == null || player == null)
            return;
        player.closeInventory();
        plugin.getMenuManager().dispatch(player, commandLine());
    }

    public CommandSpec getSpec() {
        return spec;
    }

    public List<String> getPicked() {
        return picked;
    }
}
