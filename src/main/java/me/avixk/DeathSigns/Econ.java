package me.avixk.DeathSigns;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

public class Econ {
    public static Economy econ = null;
    public static boolean set_up = false;
    public static boolean setupEconomy() {
        if (Main.plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Main.plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        if(econ != null)set_up = true;
        return set_up;
    }
    public static boolean takePlayerMoney(UUID player, double amount){
        OfflinePlayer pto = Bukkit.getOfflinePlayer(player);
        EconomyResponse resultdep = econ.withdrawPlayer(pto, amount);
        return resultdep.transactionSuccess();
    }
    public static boolean givePlayerMoney(UUID player, double amount){
        OfflinePlayer pto = Bukkit.getOfflinePlayer(player);
        EconomyResponse resultdep = econ.depositPlayer(pto, amount);
        return resultdep.transactionSuccess();
    }
    public static double getPlayerMoney(UUID player){
        return econ.getBalance(Bukkit.getOfflinePlayer(player));
    }
}