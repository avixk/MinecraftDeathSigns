package me.avixk.DeathSigns;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
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
        Conf.loadSignFile();
    }
    public static List<String> disableSignPlayers = new ArrayList<String>();
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length > 0){
            if (args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("on")){
                if(args.length == 1){
                    if(!disableSignPlayers.contains(sender.getName())){
                        sender.sendMessage("§aYour DeathSigns are already enabled.");
                    }else {
                        disableSignPlayers.remove(sender.getName());
                        sender.sendMessage("§aYour DeathSigns are now enabled.");
                    }
                    return true;
                }else if(args.length == 2 && sender.hasPermission("deathsigns.admin")){
                    Player p = Bukkit.getPlayer(args[1]);
                    if(p == null){
                        sender.sendMessage("§cPlayer not found.");
                    }else{
                        if(!disableSignPlayers.contains(p.getName())){
                            sender.sendMessage("§a" + p.getName() +"'s DeathSigns are already enabled.");
                        }else {
                            disableSignPlayers.remove(p.getName());
                            sender.sendMessage("§a" + p.getName() +"'s DeathSigns are now enabled.");
                            p.sendMessage("§a" + sender.getName() +" enabled your DeathSigns.");
                        }
                        return true;
                    }
                }
            }else if (args[0].equalsIgnoreCase("disable") || args[0].equalsIgnoreCase("off")){
                if(args.length == 1){
                    if(!disableSignPlayers.contains(sender.getName())){
                        sender.sendMessage("§aYour DeathSigns are already §cdisabled.");
                    }else {
                        disableSignPlayers.remove(sender.getName());
                        sender.sendMessage("§aYour DeathSigns are now §cdisabled.");
                    }
                    return true;
                }else if(args.length == 2 && sender.hasPermission("deathsigns.admin")){
                    Player p = Bukkit.getPlayer(args[1]);
                    if(p == null){
                        sender.sendMessage("§cPlayer not found.");
                    }else{
                        if(disableSignPlayers.contains(p.getName())){
                            sender.sendMessage("§a" + p.getName() +"'s DeathSigns are already §cdisabled.");
                        }else {
                            disableSignPlayers.add(p.getName());
                            sender.sendMessage("§a" + p.getName() +"'s DeathSigns are now §cdisabled.");
                            p.sendMessage("§a" + sender.getName() +" §cdisabled§a your DeathSigns.");
                        }
                        return true;
                    }
                }
            }else if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("ls")){
                if(args.length == 1){
                    if(!(sender instanceof Player)){
                        sender.sendMessage("§cThis command cannot be run from console.");
                        return true;
                    }
                    sender.sendMessage(Conf.getListText(((Player) sender).getUniqueId()));
                    return true;
                }else if(args.length == 2 && sender.hasPermission("deathsigns.admin")){
                    Player p = Bukkit.getPlayer(args[1]);
                    if(p == null){
                        sender.sendMessage("§cPlayer not found.");
                    }else{
                        sender.sendMessage(Conf.getListText(p.getUniqueId()));
                        return true;
                    }
                }
                return true;
            }else if (args[0].equalsIgnoreCase("recover")){
                if(!sender.hasPermission("deathsigns.admin")){
                    sender.sendMessage("§cYou do not have permission to recover graves. Ask an admin for help.");
                    return true;
                }
                int recovered = recoverArea(((Player) sender).getLocation());
                if(recovered == 0){
                    sender.sendMessage("§cNo signs found in a 10 block radius.");
                }else{
                    sender.sendMessage("§aRecovered §6" + recovered + "§a sign(s).");
                }
                return true;
            }else if (args[0].equalsIgnoreCase("test")){
                if(!sender.hasPermission("deathsigns.admin")){
                    sender.sendMessage("§cYou do not have permission to recover graves. Ask an admin for help.");
                    return true;
                }
                Player p = (Player) sender;
                Location l1 = p.getLocation().getBlock().getLocation().clone().add(.5,0,.5), l2 = l1.clone().add(0,1,0);
                p.getWorld().playSound(l1, Sound.BLOCK_WOOD_BREAK,1,1);
                p.getWorld().spawnParticle(Particle.BLOCK_DUST, l1, 50, .5,.5,.5, Material.OAK_PLANKS.createBlockData());
                p.getWorld().playSound(l2, Sound.BLOCK_STONE_BREAK,1,1);
                p.getWorld().spawnParticle(Particle.BLOCK_DUST, l2, 50, .5,.5,.5, Material.CRYING_OBSIDIAN.createBlockData());
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
            sender.sendMessage("§cAdmin Usage: /deathsigns <enable | disable | list | §4recover§c> §4[player]");
        }else{
            sender.sendMessage("§cUsage: /deathsigns <enable | disable | list>");
        }
        return true;
    }

    public static void spawnDeathSign(Block block, Player player, ItemStack[] items) {
        Bukkit.broadcast("§c" + player.getName() + "'s grave was spawned at " + block.getX() + ", " + (block.getY() + 1) + ", " + block.getZ() + " in " + block.getWorld().getName() + ".", "deathsigns.admin");
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
                    Conf.addSign(signBlock.getLocation(),player.getUniqueId());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        Location l1 = sign.getLocation().clone().add(.5,0,.5), l2 = block.getLocation().clone().add(.5,0,.5);
        player.getWorld().playSound(l1, Sound.BLOCK_WOOD_BREAK,1,1);
        player.getWorld().spawnParticle(Particle.BLOCK_DUST, l1, 50, .5,.5,.5, Material.OAK_PLANKS.createBlockData());
        player.getWorld().playSound(l2, Sound.BLOCK_STONE_BREAK,1,1);
        player.getWorld().spawnParticle(Particle.BLOCK_DUST, l2, 50, .5,.5,.5, Material.CRYING_OBSIDIAN.createBlockData());
        //player.getWorld().spawnParticle(Particle.BLOCK_DUST, sign.getLocation(), 100, new MaterialData(Material.OAK_PLANKS));
        //player.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation(), 100, new MaterialData(Material.CRYING_OBSIDIAN));
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
                    Conf.setStatus(locFromFile,"RECOVERED");
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

    public static void saveLastDeathLocation(){
        
    }


}
