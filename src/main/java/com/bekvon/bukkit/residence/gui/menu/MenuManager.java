package com.bekvon.bukkit.residence.gui.menu;

import com.bekvon.bukkit.residence.gui.ui.text.MenuLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.gui.bedrock.BedrockSupport;
import com.bekvon.bukkit.residence.gui.bedrock.FloodgateBridge;
import com.bekvon.bukkit.residence.gui.ui.MenuNavigator;
import com.bekvon.bukkit.residence.gui.ui.data.ArgDef;
import com.bekvon.bukkit.residence.gui.ui.data.ArgType;
import com.bekvon.bukkit.residence.gui.ui.data.ArgValues;
import com.bekvon.bukkit.residence.gui.ui.data.ChatInputListener;
import com.bekvon.bukkit.residence.gui.ui.data.CommandRegistry;
import com.bekvon.bukkit.residence.gui.ui.data.CommandSpec;
import com.bekvon.bukkit.residence.gui.ui.data.MenuAction;
import com.bekvon.bukkit.residence.gui.ui.data.MenuScreens;
import com.bekvon.bukkit.residence.gui.ui.data.MenuView;
import com.bekvon.bukkit.residence.gui.ui.data.ScreenProvider;
import com.bekvon.bukkit.residence.gui.ui.data.Screens;
import com.bekvon.bukkit.residence.gui.ui.render.ChestRenderer;
import com.bekvon.bukkit.residence.gui.ui.render.FormRenderer;
import com.bekvon.bukkit.residence.gui.ui.screen.ArgumentScreen;
import com.bekvon.bukkit.residence.gui.ui.screen.ConfirmScreen;
import com.bekvon.bukkit.residence.gui.ui.screen.ResidenceScreen;
import com.bekvon.bukkit.residence.gui.ui.text.MenuText;

/**
 * Entry point of the Residence menu.
 * <p>
 * The manager owns the per player session (navigation history and wizard state)
 * and decides which user interface is used: the chest menu for Java clients and
 * a Bedrock form for Bedrock clients when Floodgate can send one. Both
 * interfaces render the very same {@link MenuView}, so they cannot drift apart.
 * <p>
 * Chest clicks are handled here rather than by CMILib: the menu pins its buttons
 * to configured slots, so it needs its own inventory and its own slot map.
 */
public class MenuManager implements Listener {

    private final Residence plugin;
    private final ChatInputListener chatListener = new ChatInputListener();
    private final FloodgateBridge bridge;
    private final ChestRenderer chest;
    private final FormRenderer forms;

    private final Map<UUID, MenuScreens> sessions = new HashMap<UUID, MenuScreens>();
    private final Map<UUID, MenuNavigator> navigators = new HashMap<UUID, MenuNavigator>();
    /** Inventory holder of the menu currently shown to a player. */
    private final Map<UUID, MenuHolder> holders = new HashMap<UUID, MenuHolder>();

    public MenuManager(Residence plugin) {
        this.plugin = plugin;
        this.bridge = new FloodgateBridge(plugin);
        this.chest = new ChestRenderer(plugin);
        this.forms = new FormRenderer(plugin, bridge);
    }

    /**
     * Marker that owns the menu inventories, so a click in any other inventory
     * is ignored by the listener.
     */
    public static final class MenuHolder implements org.bukkit.inventory.InventoryHolder {
        private final UUID owner;

        MenuHolder(UUID owner) {
            this.owner = owner;
        }

        public UUID getOwner() {
            return owner;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // lifecycle
    // ------------------------------------------------------------------

    /**
     * Registers the listeners. Called on enable and after a plugin reload.
     */
    public void load() {
        unregister();
        CommandRegistry.validate();
        try {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            plugin.getServer().getPluginManager().registerEvents(chatListener, plugin);
            plugin.getServer().getPluginManager().registerEvents(new MenuJoinListener(plugin), plugin);
        } catch (Throwable e) {
            MenuLog.warning(plugin.getLogger(), "Failed to register menu listeners: " + e.getMessage());
        }

        if (BedrockSupport.isBedrockBridgePresent()) {
            MenuLog.info(plugin.getLogger(), "Bedrock bridge detected, Residence menu will be offered as a form to Bedrock players.");
            if (!BedrockSupport.hasFormApi())
                MenuLog.info(plugin.getLogger(), "Floodgate form classes were not found, Bedrock players get the chest menu.");
        }
    }

    private void unregister() {
        HandlerList.unregisterAll(this);
        sessions.clear();
        navigators.clear();
        holders.clear();
        bridge.reset();
    }

    /**
     * Reloads the menu configuration and every language file. Called by
     * {@code /res reload config} and {@code /res reload lang}.
     */
    public void reload() {
        sessions.clear();
        navigators.clear();
        bridge.reset();
        plugin.getUiLanguage().load();
    }

    // ------------------------------------------------------------------
    // public API used by the command
    // ------------------------------------------------------------------

    /**
     * Opens the root screen for a player.
     */
    public void openMainMenu(Player player) {
        if (player == null || !player.isOnline())
            return;
        MenuScreens screens = session(player);
        screens.openMain();
    }

    /**
     * Opens the menu straight at a command.
     */
    public void openCommand(Player player, String command) {
        if (player == null || !player.isOnline())
            return;
        CommandSpec spec = CommandRegistry.get(command);
        if (spec == null) {
            openMainMenu(player);
            return;
        }
        MenuScreens screens = session(player);
        screens.openCommand(spec.getName(), null);
    }

    public boolean isBedrock(Player player) {
        return BedrockSupport.isBedrockViewer(player);
    }

    /**
     * The session of a player, created on first use. A session remembers the
     * navigation history and the command that is being built.
     */
    public MenuScreens session(Player player) {
        if (player == null)
            return null;
        UUID id = player.getUniqueId();
        MenuScreens screens = sessions.get(id);
        if (screens == null) {
            MenuNavigator navigator = new MenuNavigator(plugin);
            screens = new MenuScreens(plugin, player, navigator);
            sessions.put(id, screens);
            navigators.put(id, navigator);
        }
        return screens;
    }

    // ------------------------------------------------------------------
    // navigation used by the screens
    // ------------------------------------------------------------------

    /**
     * Navigates to a screen and renders it.
     */
    public void navigate(Player player, String kind, String target, int page) {
        ScreenProvider provider = session(player).provider(kind, target, page);
        pushHistory(player);
        show(player, provider);
    }

    /**
     * Re-renders the current screen on another page.
     */
    public void rerender(Player player, int page) {
        MenuScreens screens = session(player);
        ScreenProvider current = currentProvider(player);
        if (current == null) {
            screens.openMain();
            return;
        }
        String kind = current.kind();
        if (Screens.ARGUMENT.equals(kind) || Screens.CONFIRM.equals(kind)) {
            show(player, screens.wizardStep(page));
            return;
        }
        show(player, screens.provider(kind, current.target(), page));
    }

    /**
     * Shows an already built screen without touching the history.
     */
    public void show(Player player, ScreenProvider provider) {
        if (player == null || !player.isOnline() || provider == null)
            return;
        MenuView view = provider.build(player, provider.page());
        render(player, provider, view, provider.page());
    }

    /**
     * Goes back one screen.
     */
    public void back(Player player) {
        MenuNavigator navigator = navigators.get(player.getUniqueId());
        MenuNavigator.Step previous = navigator == null ? null : navigator.pop();
        if (previous == null) {
            session(player).clearWizard();
            session(player).openMain();
            return;
        }
        if (Screens.ARGUMENT.equals(previous.getKind()) || Screens.CONFIRM.equals(previous.getKind()))
            session(player).clearWizard();
        ScreenProvider provider = session(player).provider(previous.getKind(), previous.getTarget(), previous.getPage());
        show(player, provider);
    }

    /**
     * Closes the menu and forgets the running command.
     */
    public void close(Player player) {
        if (player == null)
            return;
        MenuScreens screens = sessions.get(player.getUniqueId());
        if (screens != null)
            screens.clearWizard();
        player.closeInventory();
    }

    /**
     * Runs an assembled command line on behalf of the player.
     */
    public void dispatch(Player player, String commandLine) {
        if (player == null || !player.isOnline() || commandLine == null)
            return;
        try {
            Bukkit.dispatchCommand(player, commandLine.startsWith("/") ? commandLine.substring(1) : commandLine);
        } catch (Throwable e) {
            plugin.getUiLanguage().get(player, "menu.message.dispatch-failed", MenuText.Args.of().put("command", commandLine));
        }
        MenuScreens screens = sessions.get(player.getUniqueId());
        if (screens != null)
            screens.clearWizard();
    }

    // ------------------------------------------------------------------
    // rendering
    // ------------------------------------------------------------------

    /**
     * Renders a screen in the user interface that fits the client.
     */
    private void render(final Player player, final ScreenProvider provider, final MenuView view, final int page) {
        currentViews.put(player.getUniqueId(), new Current(provider, view, page));

        if (forms.canShow(player)) {
            FormRenderer.Result result = forms.show(player, provider, view, page, new java.util.function.Consumer<MenuAction>() {
                @Override
                public void accept(final MenuAction action) {
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            handle(player, provider, action, page, -1);
                        }
                    });
                }
            }, null);
            if (result == FormRenderer.Result.SENT)
                return;
        }
        openInventory(player, provider, view, page);
    }

    /**
     * Renders a screen as a chest menu.
     */
    private void openInventory(Player player, ScreenProvider provider, MenuView view, int page) {
        ChestRenderer.Rendered rendered = chest.render(player, provider, view, page);
        MenuHolder holder = new MenuHolder(player.getUniqueId());
        holders.put(player.getUniqueId(), holder);

        Inventory inventory = Bukkit.createInventory(holder, rendered.getSize(), renderedTitle(rendered));
        for (Map.Entry<Integer, org.bukkit.inventory.ItemStack> entry : rendered.getItems().entrySet()) {
            Integer slot = entry.getKey();
            if (slot != null && slot >= 0 && slot < rendered.getSize())
                inventory.setItem(slot, entry.getValue());
        }
        player.openInventory(inventory);
    }

    private net.kyori.adventure.text.Component renderedTitle(ChestRenderer.Rendered rendered) {
        return com.bekvon.bukkit.residence.gui.ui.text.Serializer.parseJson(rendered.getTitle());
    }

    /**
     * Current screen of a player, used by {@link #rerender}.
     */
    private static final class Current {
        final ScreenProvider provider;
        final MenuView view;
        final int page;

        Current(ScreenProvider provider, MenuView view, int page) {
            this.provider = provider;
            this.view = view;
            this.page = page;
        }
    }

    private final Map<UUID, Current> currentViews = new HashMap<UUID, Current>();

    public ScreenProvider currentProvider(Player player) {
        Current current = player == null ? null : currentViews.get(player.getUniqueId());
        return current == null ? null : current.provider;
    }

    private void pushHistory(Player player) {
        MenuNavigator navigator = navigators.get(player.getUniqueId());
        Current current = currentViews.get(player.getUniqueId());
        if (navigator == null || current == null)
            return;
        navigator.push(new MenuNavigator.Step(current.provider.kind(), current.provider.target(), current.provider.path(player), current.page));
    }

    // ------------------------------------------------------------------
    // click handling
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        org.bukkit.inventory.InventoryView view = event.getView();
        if (!(view.getTopInventory().getHolder() instanceof MenuHolder))
            return;
        // Clicks in the player inventory part of the screen are cancelled too:
        // the menu is read only.
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof MenuHolder))
            return;

        Current current = currentViews.get(player.getUniqueId());
        if (current == null)
            return;

        ChestRenderer.Rendered rendered = chest.render(player, current.provider, current.view, current.page);
        MenuAction action = rendered.getAction(event.getSlot());
        if (action == null || action.getType() == MenuAction.Type.NONE)
            return;
        handle(player, current.provider, action, current.page, rendered.getIndex(event.getSlot()));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder))
            return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        sessions.remove(id);
        navigators.remove(id);
        holders.remove(id);
        currentViews.remove(id);
        plugin.getUiLanguage().forget(event.getPlayer());
    }

    /**
     * Turns an action into behaviour. This is the single place where a click of
     * either user interface is executed.
     *
     * @param itemIndex index of the clicked list entry, {@code -1} when the
     *                  click did not come from a list entry
     */
    private void handle(Player player, ScreenProvider provider, MenuAction action, int page, int itemIndex) {
        if (player == null || !player.isOnline() || action == null)
            return;
        switch (action.getType()) {
        case NONE:
            return;
        case OPEN:
            navigate(player, action.screenKind(), action.screenTarget(), 1);
            return;
        case COMMAND: {
            // Picking a value of the running wizard and starting a new command
            // differ only in the arguments the wizard has already collected.
            if (provider instanceof ArgumentScreen) {
                ArgumentScreen argument = (ArgumentScreen) provider;
                argument.choose(argument.valueOf(itemIndex));
                return;
            }
            CommandSpec spec = CommandRegistry.get(action.getValue());
            String preset = null;
            if (provider instanceof ResidenceScreen && spec != null && spec.getArgumentCount() > 0) {
                ArgDef first = spec.getArguments().get(0);
                if (first.getType() == ArgType.RESIDENCE)
                    preset = provider.target();
            }
            session(player).openCommand(action.getValue(), preset);
            return;
        }
        case PAGE:
            rerender(player, action.getAmount());
            return;
        case BACK:
            back(player);
            return;
        case CLOSE:
            close(player);
            return;
        case LANGUAGE:
            switchLanguage(player, action.getValue());
            return;
        case LANGUAGE_PICKER:
            navigate(player, Screens.LANGUAGE, null, 1);
            return;
        case VALUE_STEP:
            if (provider instanceof ArgumentScreen) {
                ArgumentScreen argument = (ArgumentScreen) provider;
                argument.adjust(action.getAmount());
                show(player, session(player).wizardStep(page));
            }
            return;
        case VALUE_CONFIRM:
            if (provider instanceof ArgumentScreen)
                ((ArgumentScreen) provider).acceptNumber(action.getAmount());
            return;
        case CHAT_INPUT:
            if (provider instanceof ArgumentScreen) {
                ArgumentScreen argument = (ArgumentScreen) provider;
                player.closeInventory();
                argument.requestChatInput();
            }
            return;
        case SUGGEST:
            player.closeInventory();
            com.bekvon.bukkit.residence.gui.bedrock.ClientCompat.requestChatInput(player,
                    plugin.getUiLanguage().getPlain(player, "menu.prompt.suggest", null), action.getValue() == null ? "" : action.getValue());
            return;
        case EXECUTE:
            if (provider instanceof ConfirmScreen)
                ((ConfirmScreen) provider).run(player);
            return;
        default:
            return;
        }
    }

    /**
     * Item index the player clicked, used to resolve the picked value of an
     * argument list. Kept for the Bedrock path, where the form reports an index
     * directly.
     */
    private void switchLanguage(Player player, String code) {
        if (!plugin.getUiLanguage().setLanguage(player, code))
            return;
        Current current = currentViews.get(player.getUniqueId());
        if (current == null) {
            session(player).openMain();
            return;
        }
        // Re-render in the new language, which shows the change immediately in
        // both user interfaces.
        show(player, session(player).provider(current.provider.kind(), current.provider.target(), current.page));
    }

    public Residence getPlugin() {
        return plugin;
    }

    public FloodgateBridge getBridge() {
        return bridge;
    }

    public List<CommandSpec> commandsOf(String chapter) {
        return CommandRegistry.getCategory(chapter);
    }

    /**
     * Collected values of the wizard of a player, used by the command preview.
     */
    public static String preview(CommandSpec spec, List<String> picked) {
        List<String> tokens = new ArrayList<String>();
        if (spec != null)
            tokens.add(spec.getName());
        tokens.addAll(ArgValues.values(spec, picked));
        StringBuilder sb = new StringBuilder("/res");
        for (String token : tokens)
            sb.append(' ').append(token);
        return sb.toString();
    }
}
