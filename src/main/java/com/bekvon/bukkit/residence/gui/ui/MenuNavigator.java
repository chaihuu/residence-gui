package com.bekvon.bukkit.residence.gui.ui;

import java.util.ArrayList;
import java.util.List;

import com.bekvon.bukkit.residence.Residence;

/**
 * Navigation history and breadcrumb path of one menu session.
 * <p>
 * A screen is described by its kind, its target (a chapter id, a residence name,
 * an argument index ...) and its page, so a history entry can rebuild itself on
 * demand. That keeps the history independent from the inventory instance - a new
 * inventory is created for every page change - and lets the same history drive
 * the chest menu and the Bedrock form.
 */
public final class MenuNavigator {

    /** Maximum history depth, which also bounds the breadcrumb length. */
    private static final int MAX_HISTORY = 24;

    /** One visited screen. */
    public static final class Step {
        private final String kind;
        private final String target;
        private final String label;
        private final int page;

        public Step(String kind, String target, String label, int page) {
            this.kind = kind;
            this.target = target;
            this.label = label == null ? "" : label;
            this.page = Math.max(1, page);
        }

        public String getKind() {
            return kind;
        }

        public String getTarget() {
            return target;
        }

        public String getLabel() {
            return label;
        }

        public int getPage() {
            return page;
        }
    }

    private final List<Step> history = new ArrayList<Step>();

    public MenuNavigator(Residence plugin) {
        // The plugin reference is kept for symmetry with the other managers and
        // future extensions; the history itself is pure data.
    }

    /**
     * Pushes the screen that is about to be replaced onto the history.
     */
    public void push(Step step) {
        if (step == null)
            return;
        history.add(step);
        if (history.size() > MAX_HISTORY)
            history.remove(0);
    }

    /**
     * Removes and returns the previous screen, or {@code null} at the root.
     */
    public Step pop() {
        if (history.isEmpty())
            return null;
        return history.remove(history.size() - 1);
    }

    /**
     * Breadcrumb labels of the visited screens, oldest first.
     */
    public List<String> labels() {
        List<String> out = new ArrayList<String>();
        for (Step step : history)
            if (step.getLabel() != null && !step.getLabel().isEmpty())
                out.add(step.getLabel());
        return out;
    }

    public boolean canGoBack() {
        return !history.isEmpty();
    }

    public void reset() {
        history.clear();
    }

    public int depth() {
        return history.size();
    }
}
