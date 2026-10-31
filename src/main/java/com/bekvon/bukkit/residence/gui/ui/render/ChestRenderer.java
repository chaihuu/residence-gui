package com.bekvon.bukkit.residence.gui.ui.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.gui.ui.config.MenuLayout;
import com.bekvon.bukkit.residence.gui.ui.data.DataProviders;
import com.bekvon.bukkit.residence.gui.ui.data.MenuAction;
import com.bekvon.bukkit.residence.gui.ui.data.MenuView;
import com.bekvon.bukkit.residence.gui.ui.data.ScreenContext;
import com.bekvon.bukkit.residence.gui.ui.data.ScreenProvider;
import com.bekvon.bukkit.residence.gui.ui.data.ViewNode;
import com.bekvon.bukkit.residence.gui.ui.text.Serializer;

/**
 * Draws a {@link MenuView} as a chest menu.
 * <p>
 * The responsibilities of this class are strictly visual:
 * <ul>
 * <li>turn every {@link ViewNode} into an {@link ItemStack} with an Adventure
 * display name and lore,</li>
 * <li>place the entries on the configured slots, fill the border with the
 * configured filler and pin the navigation buttons to the configured footer
 * slots,</li>
 * <li>report which slot carries which {@link MenuAction}.</li>
 * </ul>
 * Names and lore are already rendered by the data layer, so this class contains
 * no translatable string.
 */
public final class ChestRenderer {

    /**
     * The layout result: inventory content, click map and content indexes.
     */
    public static final class Rendered {
        private final String title;
        private final int size;
        private final Map<Integer, ItemStack> items = new HashMap<Integer, ItemStack>();
        private final Map<Integer, MenuAction> actions = new HashMap<Integer, MenuAction>();
        private final Map<Integer, Integer> indexes = new HashMap<Integer, Integer>();

        Rendered(String title, int size) {
            this.title = title;
            this.size = size;
        }

        public String getTitle() {
            return title;
        }

        public int getSize() {
            return size;
        }

        public Map<Integer, ItemStack> getItems() {
            return items;
        }

        public MenuAction getAction(int slot) {
            return actions.get(slot);
        }

        /**
         * Index of the clicked entry inside the full item list of the screen,
         * or {@code -1} for a slot that is not a list entry.
         */
        public int getIndex(int slot) {
            Integer index = indexes.get(slot);
            return index == null ? -1 : index;
        }
    }

    private final Residence plugin;
    private final DataProviders data;

    public ChestRenderer(Residence plugin) {
        this.plugin = plugin;
        this.data = plugin.getDataProviders();
    }

    /**
     * Builds the inventory content of one screen.
     *
     * @param provider the screen, used for the click actions
     * @param view     the rendered content
     * @param page     1 based page number
     */
    public Rendered render(Player player, ScreenProvider provider, MenuView view, int page) {
        MenuLayout layout = plugin.getMenuSettings().getLayout();
        Rendered out = new Rendered(Serializer.json(view.getTitle()), layout.getInventorySize());
        ScreenContext context = new ScreenContext(player, view, provider, page, layout.contentSlots());

        if (layout.isFillBorder())
            fillBorder(out, player, layout);

        int contentSlots = layout.contentSlots();
        List<ViewNode> pageItems = view.getPage(page, contentSlots);
        int start = (Math.max(1, Math.min(page, view.getTotalPages(contentSlots))) - 1) * contentSlots;
        for (int i = 0; i < pageItems.size(); i++) {
            ViewNode node = pageItems.get(i);
            int slot = node.getSlot();
            if (slot < 0 || slot >= out.size)
                continue;
            out.items.put(slot, item(node));
            out.indexes.put(slot, start + i);
            if (node.isClickable())
                out.actions.put(slot, context.actionForIndex(start + i));
        }

        for (int i = 0; i < view.getShortcuts().size(); i++) {
            ViewNode node = view.getShortcuts().get(i);
            int slot = node.getSlot();
            if (slot < 0 || slot >= out.size)
                continue;
            out.items.put(slot, item(node));
            if (node.isClickable())
                out.actions.put(slot, provider.actionForShortcut(i));
        }

        for (MenuView.Footer footer : view.getFooter()) {
            ViewNode node = footer.getNode();
            int slot = node.getSlot();
            if (slot < 0 || slot >= out.size)
                continue;
            out.items.put(slot, item(node));
            if (node.isClickable())
                out.actions.put(slot, context.actionForFooter(footer.getId()));
        }

        return out;
    }

    /**
     * Fills every slot with the configured filler. Buttons are placed afterwards,
     * so the filler only shows in the slots that stay empty, which is what makes
     * the border look uniform on every page.
     */
    private void fillBorder(Rendered out, Player player, MenuLayout layout) {
        ItemStack filler = filler(player, layout);
        for (int slot = 0; slot < out.size; slot++)
            out.items.put(slot, filler);
    }

    /**
     * The border item. Its name and lore come from the language file, so a
     * server can put a hint into the border instead of an anonymous pane.
     */
    public ItemStack filler(Player player, MenuLayout layout) {
        Material material = data.resolveMaterial(layout.getFillerFallbacks(), layout.getFillerMaterial());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getUiLanguage().get(player, "menu.style.filler-name", null));
            List<Component> lore = plugin.getUiLanguage().getList(player, "menu.style.filler-lore", null);
            if (!lore.isEmpty())
                meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Turns a view node into a display item.
     */
    public ItemStack item(ViewNode node) {
        Material material = data.resolveMaterial(new ArrayList<String>(), node.getMaterial());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(node.getName());
            if (!node.getLore().isEmpty())
                meta.lore(node.getLore());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }
}
