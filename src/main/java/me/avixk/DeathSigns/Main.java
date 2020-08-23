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
            }if (args[0].equalsIgnoreCase("recover") && sender.hasPermission("deathsigns.admin")){
                int recovered = recoverArea(((Player) sender).getLocation());
                if(recovered == 0){
                    sender.sendMessage("§cNo signs found in a 10 block radius.");
                }else{
                    sender.sendMessage("§aRecovered §6" + recovered + "§a sign(s).");
                }
                return true;
            }/*if (args[0].equalsIgnoreCase("test") && sender.hasPermission("deathsigns.admin")){
                Thread thread = new Thread(){
                    @Override
                    public void run() {
                        int maxDistance = 5;
                        Block block = ((Player) sender).getLocation().getBlock();
                        for (int currentDistance = 0; currentDistance <= maxDistance; currentDistance++) {
                            for (int z = -(currentDistance); z <= currentDistance; z++) {// scan for a valid sign placement
                                for (int x = -(currentDistance); x <= currentDistance; x++) {
                                    for (int y = 0; y <= (currentDistance * 2); y++) {
                                        Block nearbyBlock = block.getRelative(x, y, z);
                                        if ((nearbyBlock.getType().equals(Material.AIR)
                                                || nearbyBlock.getType().equals(Material.LAVA)
                                                || nearbyBlock.getType().equals(Material.WATER))
                                                && (nearbyBlock.getRelative(0, 1, 0).getType().equals(Material.AIR)
                                                || nearbyBlock.getRelative(0, 1, 0).getType().equals(Material.LAVA)
                                                || nearbyBlock.getRelative(0, 1, 0).getType().equals(Material.WATER))) {

                                        }
                                        sender.sendMessage(nearbyBlock.getType().name());
                                        if(nearbyBlock.getType().equals(Material.AIR)){
                                            ((Player) sender).sendBlockChange(nearbyBlock.getLocation(), Material.GLASS, (byte) 0);
                                        }
                                        try {
                                            Thread.sleep(10);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    }
                };
                thread.start();
                return true;
            }*/
        }
        if(sender.hasPermission("deathsigns.admin")){
            sender.sendMessage("§cUsage: /deathsigns <enable | disable | §4recover§c>");
        }else{
            sender.sendMessage("§cUsage: /deathsigns <enable | disable>");
        }
        return true;
    }

    public static void spawnDeathSign(Block block, Player player, ItemStack[] items) {
        Main.getPlugin().getLogger().info("§c" + player.getName() + "'s grave was spawned at " + block.getX() + ", " + (block.getY() + 1) + ", " + block.getZ() + " in " + block.getWorld().getName() + ".");
        player.sendMessage(Main.getPlugin().getConfig().getString("deathPrivateMessage")
                .replace("{x}",block.getX()+"")
                .replace("{y}",(block.getY()+1)+"")
                .replace("{z}",block.getZ()+"")
                .replace("{world}",block.getWorld().getName())
        );

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
    public int recoverArea(Location loc){
        int recovered = 0;
        File file = new File(getPlugin().getDataFolder() + "/Inventories/");
        for(File f : file.listFiles()){
            Location locFromFile = locationFromString(f.getName().replace(".yml",""));
            if(loc.getWorld().equals(locFromFile.getWorld())){
                if(loc.distance(locFromFile) < 10){
                    recovered++;
                    ItemStack[] items = recallItems(locFromFile);
                    for (ItemStack i : items){
                        if(i!=null) locFromFile.getWorld().dropItem(locFromFile, i);
                    }
                    if(locFromFile.getBlock().getType().equals(Material.OAK_SIGN)) locFromFile.getBlock().setType(Material.AIR);
                    if(locFromFile.getBlock().getRelative(0,-1,0).getType().equals(Material.CRYING_OBSIDIAN))locFromFile.getBlock().getRelative(0,-1,0).setType(Material.AIR);
                }
            }
        }
        return recovered;
    }
    public Location locationFromString(String locString) {
        String[] s = locString.split("\\.");
        return new Location(Bukkit.getWorld(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2]),Integer.parseInt(s[3]));
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
