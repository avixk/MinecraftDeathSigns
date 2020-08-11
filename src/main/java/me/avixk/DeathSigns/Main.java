package me.avixk.DeathSigns;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Main extends JavaPlugin {
    private static Main plugin;
    public static Main getPlugin(){
        return plugin;
    }
    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        plugin = this;
        Bukkit.getPluginManager().registerEvents(new Events(),this);
    }
    public static List<String> disableSignPlayers = new ArrayList<String>();
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)){
            sender.sendMessage("§cThis command cannot be run from console.");
            return true;
        }
        if(args.length == 1){
            if (args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("on")){
                if(!disableSignPlayers.contains(sender.getName())){
                    sender.sendMessage("§aYour deathsigns are already enabled.");
                }else {
                    disableSignPlayers.remove(sender.getName());
                    sender.sendMessage("§aYour deathsigns are now enabled.");
                }
                return true;
            }else if (args[0].equalsIgnoreCase("disable") || args[0].equalsIgnoreCase("off")){
                if(disableSignPlayers.contains(sender.getName())){
                    sender.sendMessage("§aYour deathsigns are already §cdisabled§a.");
                }else {
                    disableSignPlayers.add(sender.getName());
                    sender.sendMessage("§aYour deathsigns are now §cdisabled§a.");
                }
                return true;
            }if (args[0].equalsIgnoreCase("recover")){
                sender.sendMessage("§cSorry, the recover command has not been implemented yet.");

                return true;
            }
        }
        if(sender.hasPermission("deathsigns.admin")){
            sender.sendMessage("§cUsage: /deathsigns <enable | disable | §4recover§c>");
        }else{
            sender.sendMessage("§cUsage: /deathsigns <enable | disable>");
        }
        return true;
    }

    public static void spawnDeathSign(Block block, Player player, ItemStack[] items) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm MMM/dd/yy");
        LocalDateTime now = LocalDateTime.now();
        String dateTimeString = dtf.format(now);

        Block signBlock = block.getRelative(0,1,0);
        block.setType(Material.CRYING_OBSIDIAN);
        signBlock.setType(Material.OAK_SIGN);
        Sign sign = (Sign) signBlock.getState();
        sign.setLine(0,"§lR.I.P.");
        sign.setLine(1,player.getName());
        sign.setLine(3,dateTimeString);
        byte signRotation = (byte) Math.round((player.getLocation().getYaw() + 180) / 24);
        if(signRotation < 0 || signRotation > 15) signRotation = 0;
        sign.setRawData((byte) signRotation);
        sign.update();
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), new Runnable(){
            @Override
            public void run() {
                try {
                    saveItems(signBlock.getLocation(), items);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static String locationToString(Location loc) {//just turns a location into a string
        return loc.getWorld().getName() + "." + loc.getBlockX() + "." +loc.getBlockY() + "." +loc.getBlockZ();
    }

    public static void saveItems(Location loc, ItemStack[] items) throws IOException {//saves items to file based on location
        YamlConfiguration c = new YamlConfiguration();
        c.set("inventory.content", items);
        c.save(new File(getPlugin().getDataFolder() + "/Inventories/", locationToString(loc) + ".yml"));
    }

    public static ItemStack[] recallItems(Location loc){//recalls items from file based on location, then deletes the file
        File file = new File(getPlugin().getDataFolder() + "/Inventories/", locationToString(loc) + ".yml");
        if(!file.exists()) return null;
        YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
        ItemStack[] items = ((List<ItemStack>) c.get("inventory.content")).toArray(new ItemStack[0]);
        try {
            file.delete();
        }catch (Exception e){}
        return items;
    }
}
