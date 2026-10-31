package com.bekvon.bukkit.residence.gui.ui.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.kyori.adventure.text.Component;

/**
 * A complete, renderer independent description of one menu screen.
 * <p>
 * The data layer builds a {@link MenuView} from the Residence model and the
 * translations, then hands the very same object to the chest renderer and to the
 * Bedrock form renderer. That is what guarantees both user interfaces show the
 * same entries, in the same order, with the same texts.
 */
public final class MenuView {

    /** A footer button with a fixed slot, e.g. back, close, language. */
    public static final class Footer {
        private final String id;
        private final ViewNode node;

        public Footer(String id, ViewNode node) {
            this.id = id;
            this.node = node;
        }

        public String getId() {
            return id;
        }

        public ViewNode getNode() {
            return node;
        }
    }

    private final Component title;
    private final List<Component> formContent;
    private final List<ViewNode> items;
    private final List<ViewNode> shortcuts;
    private final List<Footer> footer;
    private final boolean paginated;

    private MenuView(Builder builder) {
        this.title = builder.title == null ? Component.empty() : builder.title;
        this.formContent = Collections.unmodifiableList(builder.formContent);
        this.items = Collections.unmodifiableList(builder.items);
        this.shortcuts = Collections.unmodifiableList(builder.shortcuts);
        this.footer = Collections.unmodifiableList(builder.footer);
        this.paginated = builder.paginated;
    }

    public static Builder builder(Component title) {
        return new Builder(title);
    }

    public Component getTitle() {
        return title;
    }

    /**
     * Header lines of a Bedrock form: short sentences without decorations, so
     * the form stays readable on a phone screen.
     */
    public List<Component> getFormContent() {
        return formContent;
    }

    public List<ViewNode> getItems() {
        return items;
    }

    public List<ViewNode> getShortcuts() {
        return shortcuts;
    }

    public List<Footer> getFooter() {
        return footer;
    }

    public ViewNode footer(String id) {
        for (Footer one : footer)
            if (one.id.equals(id))
                return one.node;
        return null;
    }

    public boolean isPaginated() {
        return paginated;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getTotalPages(int perPage) {
        if (perPage <= 0)
            return 1;
        return Math.max(1, (int) Math.ceil(items.size() / (double) perPage));
    }

    /**
     * The items of one page.
     */
    public List<ViewNode> getPage(int page, int perPage) {
        if (perPage <= 0 || items.size() <= perPage)
            return new ArrayList<ViewNode>(items);
        int total = getTotalPages(perPage);
        int current = Math.max(1, Math.min(page, total));
        int start = (current - 1) * perPage;
        int end = Math.min(items.size(), start + perPage);
        return new ArrayList<ViewNode>(items.subList(start, end));
    }

    public static final class Builder {
        private final Component title;
        private List<Component> formContent = new ArrayList<Component>();
        private List<ViewNode> items = new ArrayList<ViewNode>();
        private List<ViewNode> shortcuts = new ArrayList<ViewNode>();
        private List<Footer> footer = new ArrayList<Footer>();
        private boolean paginated = true;

        private Builder(Component title) {
            this.title = title;
        }

        public Builder formContent(List<Component> value) {
            this.formContent = value == null ? new ArrayList<Component>() : value;
            return this;
        }

        public Builder content(Component value) {
            if (value != null)
                this.formContent.add(value);
            return this;
        }

        public Builder items(List<ViewNode> value) {
            this.items = value == null ? new ArrayList<ViewNode>() : value;
            return this;
        }

        public Builder item(ViewNode value) {
            if (value != null)
                this.items.add(value);
            return this;
        }

        public Builder shortcuts(List<ViewNode> value) {
            this.shortcuts = value == null ? new ArrayList<ViewNode>() : value;
            return this;
        }

        public Builder shortcut(ViewNode value) {
            if (value != null)
                this.shortcuts.add(value);
            return this;
        }

        public Builder footer(String id, ViewNode node) {
            if (node != null)
                this.footer.add(new Footer(id, node));
            return this;
        }

        public Builder paginated(boolean value) {
            this.paginated = value;
            return this;
        }

        public MenuView build() {
            return new MenuView(this);
        }
    }
}
