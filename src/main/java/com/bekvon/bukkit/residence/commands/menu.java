package com.bekvon.bukkit.residence.commands;

import java.util.Arrays;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.Zrips.CMILib.FileHandler.ConfigReader;

import com.bekvon.bukkit.residence.LocaleManager;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.CommandAnnotation;
import com.bekvon.bukkit.residence.containers.cmd;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.gui.ui.data.CommandRegistry;
import com.bekvon.bukkit.residence.gui.ui.text.MenuText;

public class menu implements cmd {

    @Override
    @CommandAnnotation(simple = true, priority = 50, regVar = { 0, 1, 2 }, consoleVar = { 666 })
    public Boolean perform(Residence plugin, CommandSender sender, String[] args, boolean resadmin) {

        if (!(sender instanceof Player)) {
            lm.Invalid_Ingame.sendMessage(sender);
            return false;
        }

        Player player = (Player) sender;

        if (!plugin.getConfigManager().isMenuEnabled()) {
            player.sendMessage(plugin.getUiLanguage().get(player, "menu.message.disabled", null));
            return true;
        }

        // /res menu check [language] prints a diagnostic of the language files,
        // which is how an administrator verifies that no key is missing.
        if (args.length >= 1 && "check".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission("residence.command.menu.check") && !resadmin) {
                lm.General_NoPermission.sendMessage(sender);
                return true;
            }
            MenuCheck.run(plugin, player, args.length >= 2 ? args[1] : null);
            return true;
        }

        if (args.length >= 1) {
            String target = args[0];
            // Allow jumping straight into a command, but only if we know it.
            if (CommandRegistry.get(target) != null) {
                plugin.getMenuManager().openCommand(player, target);
                return true;
            }
            player.sendMessage(plugin.getUiLanguage().get(player, "menu.message.unknown-command",
                    MenuText.Args.of().put("command", target)));
            return true;
        }

        plugin.getMenuManager().openMainMenu(player);
        return true;
    }

    @Override
    public void getLocale() {
        ConfigReader c = Residence.getInstance().getLocaleManager().getLocaleConfig();
        c.get("Description", "Open the Residence menu");
        c.get("Info", Arrays.asList("&eUsage: &6/res menu (command)", "Opens the Residence menu.",
                "Java players get a chest menu, Bedrock players a form.",
                "Every argument can be picked with clicks, no typing needed.",
                "&e/res menu check [language] &6verifies the menu language files."));
        LocaleManager.addTabCompleteMain(this, "[command]%%check");
    }
}
