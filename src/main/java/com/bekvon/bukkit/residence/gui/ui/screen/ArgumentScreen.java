package com.bekvon.bukkit.residence.gui.ui.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags.FlagMode;
import com.bekvon.bukkit.residence.gui.ui.data.AbstractScreen;
import com.bekvon.bukkit.residence.gui.ui.data.ArgChatInput;
import com.bekvon.bukkit.residence.gui.ui.data.ArgDef;
import com.bekvon.bukkit.residence.gui.ui.data.ArgSources;
import com.bekvon.bukkit.residence.gui.ui.data.ArgType;
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
import com.bekvon.bukkit.residence.protection.ClaimedResidence;

/**
 * Lets the player pick the value of one command argument.
 * <p>
 * The screen adapts to the {@link ArgType}: residences, players, flags, groups
 * and worlds become paged lists, literal choices and flag states become short
 * lists, numbers get a +/- picker and free text hands control back to chat. The
 * same widgets are rendered as chest items or as form buttons.
 */
public class ArgumentScreen extends AbstractScreen {

    private final CommandSpec spec;
    private final List<String> picked;
    private final int index;
    /** Rolling value of the numeric picker. */
    private int numeric = Integer.MIN_VALUE;

    public ArgumentScreen(Residence plugin, MenuScreens screens, CommandSpec spec, List<String> picked, int index, int page) {
        super(plugin, screens, String.valueOf(index), page);
        this.spec = spec;
        this.picked = picked == null ? new ArrayList<String>() : picked;
        this.index = Math.max(0, index);
    }

    /**
     * Rebuilds a wizard step after a page change or a back navigation. The
     * state lives in the screens session, so the collected values never have to
     * be parsed back out of a screen.
     */
    public static ScreenProvider restore(Residence plugin, MenuScreens screens, String target, int page) {
        return screens.wizardStep(page);
    }

    private ArgDef def() {
        return spec != null && index < spec.getArgumentCount() ? spec.getArguments().get(index) : null;
    }

    private String command() {
        return spec == null ? "" : spec.getName();
    }

    @Override
    public String kind() {
        return Screens.ARGUMENT;
    }

    @Override
    protected String titleKey() {
        return "menu.title.argument";
    }

    @Override
    public String path(Player player) {
        return lang.getPlain(player, "menu.title.argument", pathArgs(player));
    }

    @Override
    protected MenuText.Args pathArgs(Player player) {
        return MenuText.Args.of().put("command", lang.getPlain(player, "menu.command." + command() + ".name", null)).put("step", index + 1)
                .put("steps", spec == null ? 0 : spec.getArgumentCount());
    }

    @Override
    protected String emptyKind() {
        return "choice";
    }

    @Override
    protected List<String> itemHintKeys() {
        return Collections.singletonList("menu.hint.left_click");
    }

    @Override
    protected void collect(Player player) {
        ArgDef def = def();
        if (def == null)
            return;

        ClaimedResidence context = CommandFlow.contextResidence(plugin, player, spec, picked);
        FlagMode mode = CommandFlow.modeFor(command());
        boolean admin = data.isAdmin(player);

        switch (def.getType()) {
        case CHOICE:
            fill(ArgSources.choices(player, def));
            break;
        case FLAG_STATE:
            fill(ArgSources.flagStates(player));
            break;
        case RESIDENCE:
            fill(ArgSources.residences(player, admin));
            break;
        case PLAYER:
            fill(ArgSources.players(player));
            break;
        case FLAG:
            fill(ArgSources.flags(player, mode, admin, context));
            break;
        case GROUP:
            fill(ArgSources.groups());
            break;
        case WORLD:
            fill(ArgSources.worlds());
            break;
        case AMOUNT:
            collectNumbers(player, def);
            break;
        case TEXT:
        case RAW_TEXT:
        default:
            // Free text is typed in chat through the footer button.
            break;
        }

        if (def.isOptional()) {
            MenuText.Args args = MenuText.Args.of().put("label", lang.getPlain(player, CommandFlow.argLabelKey(spec, index), null));
            add(new MenuItem(null, lang.get(player, "menu.argument.skip", args), Collections.singletonList("menu.argument.skip-lore"), args, "GRAY_DYE",
                    null, null), MenuAction.command(command()));
        }
    }

    /**
     * Adds the pickable values. Every entry starts the same wizard action; the
     * picked value is resolved from the clicked index.
     */
    private void fill(List<MenuItem> values) {
        for (MenuItem item : values)
            add(item, MenuAction.command(command()));
    }

    private void collectNumbers(Player player, ArgDef def) {
        if (numeric == Integer.MIN_VALUE)
            numeric = def.getMin();
        for (int preset : CommandFlow.presetsFor(command())) {
            if (preset < def.getMin() || preset > def.getMax())
                continue;
            MenuText.Args args = MenuText.Args.of().put("value", preset);
            add(new MenuItem(String.valueOf(preset), lang.get(player, "menu.argument.number-value", args),
                    Collections.singletonList("menu.argument.number-value-lore"), args, "PAPER", null, null), MenuAction.command(command()));
        }
    }

    @Override
    protected void addPagingFooter(MenuView.Builder builder, Player player) {
        ArgDef def = def();
        if (def != null && def.isNumeric()) {
            int min = def.getMin();
            int max = def.getMax();
            int step = def.getStep();
            if (numeric == Integer.MIN_VALUE)
                numeric = min;

            MenuText.Args args = MenuText.Args.of().put("value", numeric).put("step", step).put("min", min).put("max", max);
            builder.footer("value_minus", Screens.footer(plugin, player, "value_minus", args, null));
            builder.footer("value_current", Screens.footer(plugin, player, "value_current", args, null));
            builder.footer("value_plus", Screens.footer(plugin, player, "value_plus", args, null));
            builder.footer("value_use", Screens.footer(plugin, player, "value_use", args, null));
            return;
        }
        super.addPagingFooter(builder, player);
    }

    @Override
    protected void decorate(MenuView.Builder builder, Player player) {
        ArgDef def = def();
        if (def != null && (def.getType() == ArgType.TEXT || def.getType() == ArgType.RAW_TEXT)) {
            MenuText.Args args = MenuText.Args.of().put("command", command()).put("label", lang.getPlain(player, CommandFlow.argLabelKey(spec, index), null));
            builder.footer("type_in_chat", Screens.footer(plugin, player, "type_in_chat", args, null));
        }
        if (def == null || !def.isNumeric()) {
            MenuText.Args manual = MenuText.Args.of().put("line", MenuText.plain("/res " + command() + " ", null));
            builder.footer("type_manually", Screens.footer(plugin, player, "type_manually", manual, null));
        }
        addNavigationFooter(builder, player);
    }

    @Override
    public MenuAction actionForFooter(String id) {
        if ("value_minus".equals(id))
            return MenuAction.step(-step());
        if ("value_plus".equals(id))
            return MenuAction.step(step());
        if ("value_use".equals(id))
            return MenuAction.valueConfirm(currentValue());
        if ("type_in_chat".equals(id))
            return MenuAction.chatInput();
        if ("type_manually".equals(id))
            return MenuAction.suggest("/res " + command() + " ");
        return super.actionForFooter(id);
    }

    private int step() {
        ArgDef def = def();
        return def == null ? 1 : def.getStep();
    }

    private int currentValue() {
        ArgDef def = def();
        if (def == null)
            return 0;
        if (numeric == Integer.MIN_VALUE)
            numeric = def.getMin();
        return Math.max(def.getMin(), Math.min(def.getMax(), numeric));
    }

    /**
     * Increases or decreases the number shown by the picker.
     */
    public void adjust(int delta) {
        ArgDef def = def();
        if (def == null)
            return;
        numeric = Math.max(def.getMin(), Math.min(def.getMax(), currentValue() + delta));
    }

    /**
     * Accepts the value of the numeric picker and continues the wizard.
     */
    public void acceptNumber(int value) {
        choose(String.valueOf(value));
    }

    /**
     * Continues the wizard with a value for this argument. A {@code null} value
     * skips an optional argument.
     */
    public void choose(String value) {
        List<String> next = CommandFlow.withValue(picked, index, value);
        CommandFlow.next(plugin, screens, spec, next, index + 1);
    }

    /**
     * Value of a list entry.
     */
    public String valueOf(int itemIndex) {
        if (itemIndex < 0 || itemIndex >= items.size())
            return null;
        return items.get(itemIndex).getValue();
    }

    @Override
    protected List<Component> formHeader(Player player) {
        List<Component> out = new ArrayList<Component>();
        ArgDef def = def();
        MenuText.Args args = MenuText.Args.of().put("command", lang.getPlain(player, "menu.command." + command() + ".name", null)).put("step", index + 1)
                .put("steps", spec == null ? 0 : spec.getArgumentCount());
        if (def != null) {
            args.put("label", lang.getPlain(player, CommandFlow.argLabelKey(spec, index), null));
            args.put("line", ArgValues.commandLine(spec, picked));
        }
        out.add(lang.get(player, "menu.form.argument", args));
        if (spec != null)
            out.add(lang.get(player, spec.getDescriptionKey(), args));
        return out;
    }

    @Override
    public ScreenContext.FlowInfo flow() {
        return new ScreenContext.FlowInfo() {
            @Override
            public String getCommandName() {
                return command();
            }

            @Override
            public String getCommandLabelKey() {
                return "menu.command." + command() + ".name";
            }

            @Override
            public String getKind() {
                return Screens.ARGUMENT;
            }

            @Override
            public int getStep() {
                return index + 1;
            }

            @Override
            public int getTotalSteps() {
                return spec == null ? 0 : spec.getArgumentCount();
            }

            @Override
            public String getArgumentLabelKey() {
                return CommandFlow.argLabelKey(spec, index);
            }

            @Override
            public String getCommandLine() {
                return ArgValues.commandLine(spec, picked);
            }
        };
    }

    /**
     * Asks for the free text value of this argument in chat. The chat listener
     * continues the wizard when the player answers.
     */
    public void requestChatInput() {
        ArgChatInput.request(plugin, screens.getPlayer(), spec, spec.getArguments(), picked, index, def(), screens);
    }

    public CommandSpec getSpec() {
        return spec;
    }

    public List<String> getPicked() {
        return picked;
    }

    public int getIndex() {
        return index;
    }
}
