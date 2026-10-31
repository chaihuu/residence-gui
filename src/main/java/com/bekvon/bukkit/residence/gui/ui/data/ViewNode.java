package com.bekvon.bukkit.residence.gui.ui.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.kyori.adventure.text.Component;

/**
 * One rendered entry of a menu screen: either a pageable list item or a
 * shortcut placed on a fixed slot.
 */
public final class ViewNode {

    private final int slot;
    private final String material;
    private final Component name;
    private final List<Component> lore;
    private final boolean paged;
    private final int index;
    private final boolean clickable;
    private final boolean selected;
    private final String iconPath;
    private final String iconUrl;

    private ViewNode(Builder builder) {
        this.slot = builder.slot;
        this.material = builder.material;
        this.name = builder.name == null ? Component.empty() : builder.name;
        this.lore = Collections.unmodifiableList(builder.lore);
        this.paged = builder.paged;
        this.index = builder.index;
        this.clickable = builder.clickable;
        this.selected = builder.selected;
        this.iconPath = builder.iconPath;
        this.iconUrl = builder.iconUrl;
    }

    public static Builder builder(int slot, String material) {
        return new Builder(slot, material);
    }

    public int getSlot() {
        return slot;
    }

    public String getMaterial() {
        return material;
    }

    public Component getName() {
        return name;
    }

    public List<Component> getLore() {
        return lore;
    }

    /**
     * Whether the node is part of the paged content area; shortcuts are placed
     * on their configured slot instead.
     */
    public boolean isPaged() {
        return paged;
    }

    public int getIndex() {
        return index;
    }

    public boolean isClickable() {
        return clickable;
    }

    public boolean isSelected() {
        return selected;
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public static final class Builder {
        private final int slot;
        private final String material;
        private Component name;
        private List<Component> lore = new ArrayList<Component>();
        private boolean paged;
        private int index = -1;
        private boolean clickable = true;
        private boolean selected;
        private String iconPath;
        private String iconUrl;

        private Builder(int slot, String material) {
            this.slot = slot;
            this.material = material;
        }

        public Builder name(Component value) {
            this.name = value;
            return this;
        }

        public Builder lore(List<Component> value) {
            this.lore = value == null ? new ArrayList<Component>() : new ArrayList<Component>(value);
            return this;
        }

        public Builder paged(boolean value) {
            this.paged = value;
            return this;
        }

        public Builder index(int value) {
            this.index = value;
            return this;
        }

        public Builder clickable(boolean value) {
            this.clickable = value;
            return this;
        }

        public Builder selected(boolean value) {
            this.selected = value;
            return this;
        }

        public Builder icon(String path, String url) {
            this.iconPath = path;
            this.iconUrl = url;
            return this;
        }

        public ViewNode build() {
            return new ViewNode(this);
        }
    }
}
