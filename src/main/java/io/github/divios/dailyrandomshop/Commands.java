package io.github.divios.dailyrandomshop;

import io.github.divios.dailyrandomshop.GUIs.sellGuiIH;
import io.github.divios.dailyrandomshop.GUIs.settings.addDailyItemGuiIH;
import io.github.divios.dailyrandomshop.GUIs.settings.sellGuiSettings;
import io.github.divios.dailyrandomshop.GUIs.settings.settingsGuiIH;
import io.github.divios.dailyrandomshop.Utils.ConfigUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;


public class Commands implements CommandExecutor {

    private final DailyRandomShop main;

    public Commands(DailyRandomShop main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {

        if (args.length == 0) {

            if (!(sender instanceof Player) ) return true;

            if(!(sender.hasPermission("DailyRandomShop.open" ))) {
                sender.sendMessage(main.config.PREFIX + main.config.MSG_NOT_PERMS);
                return true;
            }
            sender.sendMessage(main.config.PREFIX + main.config.MSG_OPEN_SHOP);
            Player p = (Player) sender;
            p.openInventory(main.BuyGui.getGui());
        } else {
            if (args[0].equalsIgnoreCase("renovate")) {

                if (!sender.hasPermission("DailyRandomShop.renovate")) {
                    sender.sendMessage(main.config.PREFIX + main.config.MSG_NOT_PERMS);
                    return true;
                }

                main.BuyGui.inicializeGui(true);
                ConfigUtils.resetTime(main);
            } else if (args[0].equalsIgnoreCase("reload")) {

                if (!sender.hasPermission("DailyRandomShop.reload")) {
                    sender.sendMessage(main.config.PREFIX + main.config.MSG_NOT_PERMS);
                    return true;
                }
                try {
                    //ConfigUtils.CloseAllInventories(main);
                    main.reloadConfig();
                    sender.sendMessage(main.config.PREFIX + main.config.MSG_RELOAD);
                    ConfigUtils.reloadConfig(main, true);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (args[0].equalsIgnoreCase("sell") && (sender instanceof Player) &&
                        main.getConfig().getBoolean("enable-sell-gui")){

                if (!sender.hasPermission("DailyRandomShop.sell") ){
                    sender.sendMessage(main.config.PREFIX + main.config.MSG_NOT_PERMS);
                    return true;
                }
                Player p = (Player) sender;
                new sellGuiIH(main, p);

            }  else if (args[0].equalsIgnoreCase("addsellitem") && sender instanceof Player) {

                if (!sender.hasPermission("DailyRandomShop.addSellItem")) {
                    sender.sendMessage(main.config.PREFIX + main.config.MSG_NOT_PERMS);
                    return true;
                }

                Player p = (Player) sender;
                ItemStack item = p.getInventory().getItemInHand().clone();
                item.setAmount(1);

                if (item == null || item.getType() == Material.AIR) {
                    p.sendMessage(main.config.PREFIX + main.config.MSG_ADD_DAILY_ITEM_ERROR_ITEM);
                    return true;
                }

                if(main.listSellItems.containsKey(item)) {
                    p.sendMessage(main.config.PREFIX + ChatColor.GRAY + "That item is already on sale");
                    return true;
                }

                if (args.length == 1) {
                    p.sendMessage(main.config.PREFIX + main.config.MSG_ADD_DAILY_ITEM_ERROR_PRICE);
                    return true;
                }

                main.listSellItems.put(item, Double.parseDouble(args[1]));
                p.sendMessage(main.config.PREFIX + main.config.MSG_ADD_DAILY_ITEM_SUCCESS);
                main.SellGuiSettings = new sellGuiSettings(main);

                main.dbManager.addSellItem(item, Double.parseDouble(args[1]));


            } else if (args[0].equalsIgnoreCase("settings") && sender instanceof Player) {

                if(!sender.hasPermission("DailyRandomShop.settings")) {
                    sender.sendMessage(main.config.PREFIX + main.config.MSG_NOT_PERMS);
                    return true;
                }
                Player p = (Player) sender;

                new settingsGuiIH(main, p);
            } else if (args[0].equalsIgnoreCase("addDailyItem") && sender instanceof Player) {

                if(!sender.hasPermission("DailyRandomShop.addDailyItem")) {
                    sender.sendMessage(main.config.PREFIX + main.config.MSG_NOT_PERMS);
                    return true;
                }

                new addDailyItemGuiIH(main, (Player) sender, null);
            }

        }


        return true;
    }

}
