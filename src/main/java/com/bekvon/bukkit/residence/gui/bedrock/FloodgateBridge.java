package com.bekvon.bukkit.residence.gui.bedrock;

import com.bekvon.bukkit.residence.gui.ui.text.MenuLog;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.bekvon.bukkit.residence.Residence;

/**
 * Sends Bedrock forms through Floodgate and Cumulus.
 * <p>
 * Floodgate is an optional dependency, so every Cumulus call is made through
 * reflection. The classes {@code org.geysermc.cumulus.form.*} are resolved once
 * and cached; when anything is missing the caller falls back to the chest menu
 * and the player still gets a working interface.
 * <p>
 * The bridge supports both Cumulus generations that Geyser ships:
 * <ul>
 * <li>1.1+ ({@code SimpleForm.builder()}, {@code ModalForm.builder()}),</li>
 * <li>1.0 ({@code SimpleForm.builder()} as well, but a differently shaped
 * response: {@code getClickedButtonId()} instead of
 * {@code clickedButtonId()}).</li>
 * </ul>
 * Both are handled by probing the method by name and accepting either return
 * name, which keeps the plugin compatible with older Geyser builds.
 */
public final class FloodgateBridge {

    /** Button definition passed to {@link #sendSimpleForm}. */
    public static final class Button {
        private final String text;
        private final String iconPath;
        private final String iconUrl;

        public Button(String text, String iconPath, String iconUrl) {
            this.text = text == null ? "" : text;
            this.iconPath = iconPath;
            this.iconUrl = iconUrl;
        }

        public String getText() {
            return text;
        }
    }

    private final Residence plugin;

    private boolean checked = false;
    private boolean available = false;
    private Object api = null;

    private Method apiGetInstance;
    private Method apiSendForm;
    private Method apiIsFloodgatePlayer;

    private Method simpleBuilder;
    private Method modalBuilder;
    private Method builderTitle;
    private Method builderContent;
    private Method builderButtonWithImage;
    private Method builderButton;
    private Method builderValidHandler;
    private Method builderClosedOrInvalidHandler;
    private Method builderBuild;

    private Method modalContent;
    private Method modalButton1;
    private Method modalButton2;

    private Method imageOf;
    private Object imageTypePath;
    private Object imageTypeUrl;

    private Class<?> simpleResponseType;
    private Method clickedButtonId;
    private Method modalResponseTypeClicked;
    private Class<?> modalResponseType;
    private Method modalResponseClicked;

    public FloodgateBridge(Residence plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // availability
    // ------------------------------------------------------------------

    /**
     * Resolves the API once. A failed resolution is not retried on every call,
     * only after {@link #reset()}, so a missing Floodgate does not cost
     * reflection lookups on every menu open.
     */
    public boolean isAvailable() {
        if (checked)
            return available;
        checked = true;
        available = resolve();
        if (!available && BedrockSupport.isFloodgatePresent())
            MenuLog.warning(plugin.getLogger(), "Floodgate is installed but the form API could not be resolved, the chest menu is used instead.");
        return available;
    }

    /**
     * Allows a retry after a plugin reload, e.g. when Floodgate was installed
     * while the server was running.
     */
    public void reset() {
        checked = false;
        available = false;
        api = null;
    }

    private boolean resolve() {
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            apiGetInstance = apiClass.getMethod("getInstance");
            api = apiGetInstance.invoke(null);
            if (api == null)
                return false;
            apiIsFloodgatePlayer = apiClass.getMethod("isFloodgatePlayer", UUID.class);
            apiSendForm = findMethod(apiClass, "sendForm", UUID.class, Class.forName("org.geysermc.cumulus.form.Form"));
            if (apiSendForm == null)
                return false;

            Class<?> simpleForm = Class.forName("org.geysermc.cumulus.form.SimpleForm");
            Class<?> simpleFormBuilder = Class.forName("org.geysermc.cumulus.form.SimpleForm$Builder");
            Class<?> modalForm = Class.forName("org.geysermc.cumulus.form.ModalForm");
            Class<?> modalFormBuilder = Class.forName("org.geysermc.cumulus.form.ModalForm$Builder");
            Class<?> formBuilder = Class.forName("org.geysermc.cumulus.form.util.FormBuilder");

            simpleBuilder = simpleForm.getMethod("builder");
            builderTitle = formBuilder.getMethod("title", String.class);
            builderValidHandler = findMethod(formBuilder, "validResultHandler", java.util.function.Consumer.class);
            builderClosedOrInvalidHandler = findMethod(formBuilder, "closedOrInvalidResultHandler", Runnable.class);
            if (builderValidHandler == null || builderClosedOrInvalidHandler == null)
                return false;
            builderBuild = formBuilder.getMethod("build");

            builderContent = findMethod(simpleFormBuilder, "content", String.class);

            modalBuilder = modalForm.getMethod("builder");
            modalContent = findMethod(modalFormBuilder, "content", String.class);
            modalButton1 = findMethod(modalFormBuilder, "button1", String.class);
            modalButton2 = findMethod(modalFormBuilder, "button2", String.class);

            // Images are optional; a Geyser build without them still works.
            try {
                Class<?> formImage = Class.forName("org.geysermc.cumulus.util.FormImage");
                Class<?> formImageType = Class.forName("org.geysermc.cumulus.util.FormImage$Type");
                imageOf = formImage.getMethod("of", formImageType, String.class);
                imageTypePath = formImageType.getField("PATH").get(null);
                imageTypeUrl = formImageType.getField("URL").get(null);
                builderButtonWithImage = findMethod(simpleFormBuilder, "button", String.class, formImageType, String.class);
            } catch (Throwable ignored) {
                imageOf = null;
                builderButtonWithImage = null;
            }
            builderButton = findMethod(simpleFormBuilder, "button", String.class);

            simpleResponseType = Class.forName("org.geysermc.cumulus.response.SimpleFormResponse");
            clickedButtonId = findAnyMethod(simpleResponseType, "clickedButtonId", "getClickedButtonId");

            modalResponseType = Class.forName("org.geysermc.cumulus.response.ModalFormResponse");
            modalResponseClicked = findAnyMethod(modalResponseType, "clickedButtonText", "getClickedButtonText");
            modalResponseTypeClicked = findAnyMethod(modalResponseType, "isPositive", "getClickedButton");

            // The builder interface requires a title, the header and the content
            // are validated when a form is built.
            if (builderTitle == null)
                return false;
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Finds a method by name and parameter types, tolerating the use of a
     * superclass parameter type (the result handler types changed between
     * Cumulus versions).
     */
    private static Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        for (Class<?> current = owner; current != null; current = current.getSuperclass()) {
            for (Method method : current.getMethods()) {
                if (!method.getName().equals(name))
                    continue;
                if (!compatible(method.getParameterTypes(), parameterTypes))
                    continue;
                method.setAccessible(true);
                return method;
            }
            for (Class<?> iface : current.getInterfaces()) {
                Method found = findMethod(iface, name, parameterTypes);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    private static boolean compatible(Class<?>[] actual, Class<?>[] expected) {
        if (actual.length != expected.length)
            return false;
        for (int i = 0; i < actual.length; i++)
            if (!expected[i].isAssignableFrom(actual[i]) && !actual[i].isAssignableFrom(expected[i]))
                return false;
        return true;
    }

    /**
     * Finds the first existing method of several names, used where the Cumulus
     * 1.0 getter names differ from 1.1.
     */
    private static Method findAnyMethod(Class<?> owner, String... names) {
        for (String name : names) {
            try {
                for (Method method : owner.getMethods()) {
                    if (method.getName().equals(name) && method.getParameterCount() == 0) {
                        method.setAccessible(true);
                        return method;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    /**
     * Whether a player is a Bedrock player according to Floodgate.
     */
    public boolean isFloodgatePlayer(Player player) {
        if (player == null || !isAvailable())
            return false;
        try {
            Object result = api.getClass().getMethod("isFloodgatePlayer", UUID.class).invoke(api, player.getUniqueId());
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // sending a simple form
    // ------------------------------------------------------------------

    /**
     * Sends a simple form and calls {@code onClick} with the index of the
     * pressed button.
     * <p>
     * The callback always runs on the main thread, even when Floodgate invokes
     * it from its own executor.
     *
     * @param player  receiver
     * @param title   form title
     * @param content form description, plain text
     * @param buttons buttons in display order
     * @param onClick button index handler, {@code null} to ignore the result
     * @param onClose called when the player closes the form
     * @return {@code false} when the form could not be sent and the caller has to
     *         fall back to the chest menu
     */
    public boolean sendSimpleForm(final Player player, String title, String content, List<Button> buttons, final Consumer<Integer> onClick,
            final Runnable onClose) {
        if (!isAvailable() || player == null || !player.isOnline())
            return false;
        try {
            Object builder = simpleBuilder.invoke(null);
            builderTitle.invoke(builder, safe(title));

            if (builderContent != null)
                builderContent.invoke(builder, safe(content));

            for (Button button : buttons) {
                if (hasText(button.iconPath) && imageOf != null && builderButtonWithImage != null) {
                    Object image = imageOf.invoke(null, imageTypePath, button.iconPath);
                    builderButtonWithImage.invoke(builder, button.text, image);
                } else if (hasText(button.iconUrl) && imageOf != null && builderButtonWithImage != null) {
                    Object image = imageOf.invoke(null, imageTypeUrl, button.iconUrl);
                    builderButtonWithImage.invoke(builder, button.text, image);
                } else if (builderButton != null) {
                    builderButton.invoke(builder, button.text);
                }
            }

            final Consumer<Object> handler = new Consumer<Object>() {
                @Override
                public void accept(final Object response) {
                    runMain(new Runnable() {
                        @Override
                        public void run() {
                            if (!player.isOnline() || onClick == null)
                                return;
                            int index = clickIndex(response);
                            if (index >= 0)
                                onClick.accept(index);
                        }
                    });
                }
            };
            builderValidHandler.invoke(builder, handler);

            final Runnable close = new Runnable() {
                @Override
                public void run() {
                    if (onClose == null)
                        return;
                    runMain(onClose);
                }
            };
            builderClosedOrInvalidHandler.invoke(builder, close);

            Object form = builderBuild.invoke(builder);
            return send(player, form);
        } catch (Throwable e) {
            MenuLog.warning(plugin.getLogger(), "Unable to send a Bedrock form: " + e.getClass().getSimpleName() + " " + e.getMessage());
            return false;
        }
    }

    /**
     * The index of the pressed button.
     */
    private int clickIndex(Object response) {
        if (response == null || clickedButtonId == null)
            return -1;
        try {
            Object value = clickedButtonId.invoke(response);
            return value instanceof Integer ? (Integer) value : -1;
        } catch (Throwable e) {
            return -1;
        }
    }

    /**
     * Sends a two button form.
     *
     * @param onPositive called when the first button is pressed
     * @param onNegative called when the second button is pressed
     */
    public boolean sendModalForm(Player player, String title, String content, String positiveText, String negativeText, final Runnable onPositive,
            final Runnable onNegative, final Runnable onClose) {
        if (!isAvailable() || player == null || !player.isOnline() || modalBuilder == null)
            return false;
        try {
            Object builder = modalBuilder.invoke(null);
            builderTitle.invoke(builder, safe(title));
            if (modalContent != null)
                modalContent.invoke(builder, safe(content));
            if (modalButton1 != null)
                modalButton1.invoke(builder, safe(positiveText));
            if (modalButton2 != null)
                modalButton2.invoke(builder, safe(negativeText));

            final Consumer<Object> handler = new Consumer<Object>() {
                @Override
                public void accept(final Object response) {
                    final boolean positive = isPositive(response);
                    runMain(new Runnable() {
                        @Override
                        public void run() {
                            if (!player.isOnline())
                                return;
                            if (positive && onPositive != null)
                                onPositive.run();
                            else if (!positive && onNegative != null)
                                onNegative.run();
                        }
                    });
                }
            };
            builderValidHandler.invoke(builder, handler);
            if (onClose != null) {
                builderClosedOrInvalidHandler.invoke(builder, new Runnable() {
                    @Override
                    public void run() {
                        runMain(onClose);
                    }
                });
            }
            return send(player, builderBuild.invoke(builder));
        } catch (Throwable e) {
            MenuLog.warning(plugin.getLogger(), "Unable to send a Bedrock modal form: " + e.getClass().getSimpleName() + " " + e.getMessage());
            return false;
        }
    }

    private boolean isPositive(Object response) {
        if (response == null)
            return false;
        try {
            if (modalResponseTypeClicked != null) {
                Object value = modalResponseTypeClicked.invoke(response);
                if (value instanceof Boolean)
                    return (Boolean) value;
                if (value instanceof String)
                    return "true".equalsIgnoreCase((String) value) || "1".equals(value);
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private boolean send(Player player, Object form) {
        try {
            Object result = apiSendForm.invoke(api, player.getUniqueId(), form);
            return result instanceof Boolean ? (Boolean) result : true;
        } catch (Throwable e) {
            MenuLog.warning(plugin.getLogger(), "Unable to send a Bedrock form: " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void runMain(final Runnable runnable) {
        if (runnable == null)
            return;
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Trims a list of buttons to the configured maximum; the caller paginates
     * before calling so this is only a last line of defence against a form the
     * Bedrock client cannot scroll.
     */
    public static List<Button> limit(List<Button> buttons, int max) {
        if (buttons == null)
            return new ArrayList<Button>();
        if (max <= 0 || buttons.size() <= max)
            return buttons;
        return new ArrayList<Button>(buttons.subList(0, max));
    }

    /**
     * Debug helper listing the resolved Cumulus entry points.
     */
    public List<String> debugInfo() {
        if (!isAvailable())
            return Arrays.asList("Floodgate form API not available");
        return Arrays.asList("SimpleForm builder: " + (simpleBuilder != null), "ModalForm builder: " + (modalBuilder != null),
                "Images: " + (imageOf != null), "clickedButtonId: " + (clickedButtonId != null));
    }
}
