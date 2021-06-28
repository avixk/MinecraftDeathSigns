package me.avixk.DeathSigns;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main extends JavaPlugin {
    public static Main plugin;
    public static Main getPlugin(){
        return plugin;
    }
    @Override
    public void onEnable() {

        this.saveDefaultConfig();
        plugin = this;
        DeathSignHandler.key = new NamespacedKey(Main.getPlugin(), "deathsigns.compass");
        //Events.registerTab();
        Bukkit.getPluginManager().registerEvents(new Events(),this);
        Conf.loadSignFile();
        Events.compass = getConfig().getBoolean("death_compass.enable");

        if(Econ.setupEconomy())
            Bukkit.getLogger().info("Vault hooked. Economy support enabled.");
        else
            Bukkit.getLogger().info("Vault not found. Economy support disabled.");

        if(BountyHook.init())
            Bukkit.getLogger().info("HeadHunting hooked. Bounty support enabled.");
        else
            Bukkit.getLogger().info("HeadHunting not found. Bounty support disabled.");

        Conf.loadConfigOptions();
    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().info("Disabling DeathSigns, saving inventories...");
        long timenow = System.currentTimeMillis();
        //int failed = 0;
        int saved = 0;
        for(DeathSign e : DeathSignHandler.openSigns){
            e.saveItems();
            saved++;
        }
        DeathSignHandler.openSigns.clear();
        Bukkit.getLogger().info("Successfully saved " + saved + " inventories in " + (System.currentTimeMillis() - timenow) + " ms.");
        //if(failed != 0)Bukkit.getLogger().info(failed + " inventories failed to save.");
    }

    public static List<String> disableSignPlayers = new ArrayList<String>();
    public static HashMap<String,DeathSign> destroyConfirmPlayers = new HashMap<>();
    public static HashMap<String,Integer> destroyConfirmPlayerTasks = new HashMap<>();
    //public static HashMap<Location, Inventory> openSigns = new HashMap<Location, Inventory>();
    //public static List<DeathSign> openSigns = new ArrayList<>();
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
                int page = 1;
                UUID player = null;
                if(args.length == 1){
                    if(!(sender instanceof Player)){
                        sender.sendMessage("§cThis command cannot be run from console.");
                        return true;
                    }
                    player = ((Player) sender).getUniqueId();
                }else if(args.length == 2){
                    OfflinePlayer p = Bukkit.getOfflinePlayer(args[1]);
                    if(p == null || !p.hasPlayedBefore()){
                        try {
                            page = Integer.parseInt(args[1]);
                            if(!(sender instanceof Player)){
                                sender.sendMessage("§cThis command cannot be run from console.");
                                return true;
                            }
                            player = ((Player) sender).getUniqueId();
                        }catch (Exception e){
                            sender.sendMessage("§cPlayer not found.");
                            return true;
                        }
                    }else{
                        player = p.getUniqueId();
                    }
                }else if(args.length == 3){
                    OfflinePlayer p = Bukkit.getOfflinePlayer(args[1]);
                    if(p == null || !p.hasPlayedBefore()){
                        sender.sendMessage("§cPlayer not found.");
                        return true;
                    }
                    player = p.getUniqueId();
                    try {
                        page = Integer.parseInt(args[2]);
                    }catch (Exception e){
                        sender.sendMessage("§7Usage: /deathsigns list [player] [page #]");
                        return true;
                    }
                }
                if(sender instanceof Player && player != ((Player) sender).getUniqueId() && !sender.hasPermission("deathsigns.admin")){
                    sender.sendMessage("§cYou do not have permission to see other people's DeathSigns.");
                    return true;
                }
                sender.sendMessage(Conf.getListText(player, page));
                return true;
            }else if (args[0].equalsIgnoreCase("compass")){
                //ds compass 1
                if(!(sender instanceof Player)){
                    sender.sendMessage("§cThis command cannot be run from console.");
                    return true;
                }
                if(Main.plugin.getConfig().getBoolean("death_compass.require_permission") && !(sender.hasPermission("deathsigns.compass"))){
                    sender.sendMessage("§cYou do not have permission to run this command.");
                    return true;
                }
                int sign = -1;
                UUID target = ((Player) sender).getUniqueId();
                if(args.length == 2){
                    try {
                        sign = Integer.parseInt(args[1]);
                    }catch (Exception e){
                        sender.sendMessage("§7Usage: /deathsigns compass §c[sign number]");
                        return true;
                    }
                }else if(args.length == 3){
                    try {
                        sign = Integer.parseInt(args[2]);
                    }catch (Exception e){
                        sender.sendMessage("§7Usage: /deathsigns compass [player] §c[sign number]");
                        return true;
                    }
                    try {
                        target = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
                    }catch (Exception e){
                        sender.sendMessage("§7Usage: /deathsigns compass §c[player] §7[sign number]");
                        return true;//TODO FIX THIS SHIT
                    }
                }
                if(sign == -1) sign = Conf.getLastID(target);
                if(sign <= 0){
                    sender.sendMessage("§cDeathSign not found, sign " + sign + " does not exist.");
                    return true;
                }
                DeathSign deathSign = DeathSignHandler.getDeathSignFromIdentifier(target + "." + sign);
                if(deathSign == null){
                    sender.sendMessage("§cDeathSign not found. Check the sign ID and try again.");
                    return true;
                }
                String status = deathSign.getStatus();
                if(status == null){
                    sender.sendMessage("§cDeathSign not found.");
                    return true;
                }
                if(status.equals("RECOVERED")){
                    sender.sendMessage("§cYou already broke this deathsign.");
                    return true;
                }
                if(status.equals("TAKEN")){
                    sender.sendMessage("§cSomeone already broke this deathsign.");
                    return true;
                }
                //sender.sendMessage();
                //ItemStack comp = getDeathsignCompass(Bukkit.getOfflinePlayer(dsign.owner).getName(),targetLocation,dsign.deathMessage, sign);
                ((Player) sender).getInventory().addItem(deathSign.getDeathsignCompass());
                sender.sendMessage("§aEnjoy your compass.");
                return true;
            }else if (args[0].equalsIgnoreCase("destroy")){
                //ds compass 1
                if(!(sender instanceof Player)){
                    sender.sendMessage("§cThis command cannot be run from console.");
                    return true;
                }
                OfflinePlayer target = (OfflinePlayer) sender;
                if(Main.plugin.getConfig().getBoolean("command_destroy.require_permission") && !(sender.hasPermission("deathsigns.destroy"))){
                    sender.sendMessage("§cYou do not have permission to run this command.");
                    return true;
                }
                int sign = Conf.getLastID(((Player) sender).getUniqueId());
                if(args.length == 1){
                    sender.sendMessage("§7Usage: /deathsigns destroy §c[sign number]");
                    return true;
                }else if(args.length == 2){
                    try {
                        sign = Integer.parseInt(args[1]);
                    }catch (Exception e){
                        sender.sendMessage("§7Usage: /deathsigns destroy §c[sign number]");
                        return true;
                    }
                }else if(args.length == 3 && sender.hasPermission("deathsigns.destroy.other")){
                    try {
                        target = Bukkit.getOfflinePlayer(args[1]);
                        if(!target.hasPlayedBefore()){
                            sender.sendMessage("§7Usage: /deathsigns destroy §c[player] §7[sign number]");
                            return true;
                        }
                        sign = Integer.parseInt(args[2]);
                    }catch (Exception e){
                        sender.sendMessage("§7Usage: /deathsigns destroy [player] §c[sign number]");
                        return true;
                    }
                }
                if(sign <= 0){
                    sender.sendMessage("§cDeathSign not found, sign " + sign + " does not exist.");
                    return true;
                }
                DeathSign deathSign = DeathSignHandler.getDeathSignFromIdentifier(target.getUniqueId() + "." + sign);
                if(deathSign == null){
                    sender.sendMessage("§cDeathSign not found. Check the sign ID and try again.");
                    return true;
                }
                String status = deathSign.getStatus();
                if(status == null){
                    sender.sendMessage("§cDeathSign not found.");
                    return true;
                }
                if(status.equals("RECOVERED")){
                    sender.sendMessage("§cYou already broke this deathsign.");
                    return true;
                }
                if(status.equals("TAKEN")){
                    sender.sendMessage("§cSomeone already broke this deathsign.");
                    return true;
                }
                if(status.equals("DESTROYED")){
                    sender.sendMessage("§cYou already destroyed this deathsign.");
                    return true;
                }
                destroyConfirmPlayers.put(sender.getName(),deathSign);
                sender.sendMessage(Main.plugin.getConfig().getString("destroy_command.confirm_message")
                        .replace("{id}",sign+"").replace("&","§"));
                if(destroyConfirmPlayerTasks.containsKey(sender.getName())){
                    Bukkit.getScheduler().cancelTask(destroyConfirmPlayerTasks.get(sender.getName()));
                }
                int task = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        destroyConfirmPlayers.remove(sender.getName());
                        destroyConfirmPlayerTasks.remove(sender.getName());
                    }
                },3600);//3min delay
                destroyConfirmPlayerTasks.put(sender.getName(),task);
                return true;
            }else if (args[0].equalsIgnoreCase("recover")){
                if(!sender.hasPermission("deathsigns.admin")){
                    sender.sendMessage("§cYou do not have permission to recover graves. Ask an admin for help.");
                    return true;
                }
                Player p = (Player) sender;
                Location loc = p.getLocation();
                int ramrecovered = 0;
                int hdrecovered = 0;
                File file = new File(getPlugin().getDataFolder() + "/Inventories/");
                for(DeathSign deathSign : DeathSignHandler.openSigns){
                    if(loc.getWorld().equals(deathSign.location.getWorld())){
                        if(loc.distance(deathSign.location) <= 10){
                            for (ItemStack i :deathSign.getInventory().getContents()){
                                if(i!=null) loc.getWorld().dropItem(deathSign.getLocation(), i);
                            }
                            if(deathSign.getLocation().getBlock().getType().equals(Material.OAK_SIGN)) deathSign.getLocation().getBlock().setType(Material.AIR);
                            if(deathSign.getLocation().getBlock().getRelative(0,-1,0).getType().equals(Material.CRYING_OBSIDIAN))deathSign.getLocation().getBlock().getRelative(0,-1,0).setType(Material.AIR);
                            deathSign.setStatus("RECOVERED");
                            DeathSignHandler.removeSign(deathSign);
                            ramrecovered++;
                        }
                    }
                }
                for(File f : file.listFiles()){
                    Location locFromFile = Util.locationFromString(f.getName().replace(".yml",""));
                    if(loc.getWorld().equals(locFromFile.getWorld())){
                        if(loc.distance(locFromFile) <= 10){
                            //Map.Entry<ItemStack[], String> items = recallItems(locFromFile,true);
                            DeathSign deathSign = DeathSignHandler.getDeathSignAtLoc(locFromFile);
                            /*for (ItemStack i : deathSign.getInventory().getContents()){
                                if(i!=null) locFromFile.getWorld().dropItem(locFromFile, i);
                            }
                            if(locFromFile.getBlock().getType().equals(Material.OAK_SIGN)) locFromFile.getBlock().setType(Material.AIR);
                            if(locFromFile.getBlock().getRelative(0,-1,0).getType().equals(Material.CRYING_OBSIDIAN))locFromFile.getBlock().getRelative(0,-1,0).setType(Material.AIR);
                            deathSign.setStatus("RECOVERED");*/
                            if(deathSign != null){
                                deathSign.breakSign(p);
                                hdrecovered++;
                            }
                        }
                    }
                }
                if(ramrecovered == 0 && hdrecovered == 0){
                    sender.sendMessage("§cNo signs found in a 10 block radius.");
                }else{
                    sender.sendMessage("§aRecovered §6" + ramrecovered + "§a sign(s) from RAM, and §6" + hdrecovered + "§a sign(s) from disk.");
                }
                return true;
            }else if (args[0].equalsIgnoreCase("confirm")){
                if(destroyConfirmPlayers.containsKey(((Player)sender).getName())){
                    DeathSign sign = destroyConfirmPlayers.get(sender.getName());

                    String status = sign.getStatus();
                    if(status == null){
                        sender.sendMessage("§cDeathSign not found.");
                        return true;
                    }
                    if(status.equals("RECOVERED")){
                        sender.sendMessage("§cYou already broke this deathsign.");
                        return true;
                    }
                    if(status.equals("TAKEN")){
                        sender.sendMessage("§cSomeone already broke this deathsign.");
                        return true;
                    }
                    if(status.equals("DESTROYED")){
                        sender.sendMessage("§cThis deathsign was already destroyed.");
                        return true;
                    }

                    sign.selfDestruct();
                    destroyConfirmPlayers.remove(sender.getName());
                    destroyConfirmPlayerTasks.remove(sender.getName());
                    sender.sendMessage(Main.plugin.getConfig().getString("destroy_command.confirmed_message").replace("{id}",sign.id+"").replace("&","§"));
                }else{
                    sender.sendMessage("§cThere is no pending confirmation request.");
                }
                return true;
            }else if (args[0].equalsIgnoreCase("retroupgrade")){
                if(!sender.hasPermission("deathsigns.admin")){
                    sender.sendMessage("§cYou do not have permission to upgrade the config.");
                    return true;
                }

                long timeBefore = System.currentTimeMillis();
                int upgraded = 0;
                int errors = 0;

                File inventoryFolder = new File(Main.getPlugin().getDataFolder() + "/Inventories");
                for(File f : inventoryFolder.listFiles()){
                    if(f.getName().endsWith(".yml"))f.renameTo(new File(inventoryFolder + "/" + f.getName().replace(".yml","").replace(".",",") + ".yml"));
                }

                File signFile = new File(Main.getPlugin().getDataFolder() + "/sign_locations.yml");
                YamlConfiguration conf = new YamlConfiguration();
                try {
                    conf.load(signFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                for(String uuid : conf.getConfigurationSection("signs").getKeys(false)){
                    UUID uid = UUID.fromString(uuid);
                    int id = 1;
                    if(Conf.storage.getConfig().contains("player." + uuid + ".last_id")){
                        id = Conf.storage.getConfig().getInt("player." + uuid + ".last_id") + 1;
                    }
                    Conf.storage.getConfig().set("player." + uuid + ".last_id", id);

                    for(String locstr : conf.getConfigurationSection("signs." + uuid).getKeys(false)){
                        try {
                            Location loc = Util.locationFromString(locstr.replace(".",","));
                            Conf.storage.getConfig().set("player." + uid + ".deaths." + id + ".status",
                                    conf.getString("signs." + uuid + "." + locstr.replace(".",",") + ".status"));

                            Conf.storage.getConfig().set("player." + uid + ".deaths." + id + ".status_time",
                                    conf.getLong("signs." + uuid + "." + locstr.replace(".",",") + ".status_time"));

                            Conf.storage.getConfig().set("player." + uid + ".deaths." + id + ".time",
                                    conf.getLong("signs." + uuid + "." + locstr.replace(".",",") + ".time"));

                            Conf.storage.getConfig().set("player." + uid + ".deaths." + id + ".loc", locstr);

                            String deathMessage = "Unknown DeathSign";

                            File f = new File(Main.plugin.getDataFolder() + "/Inventories/" + locstr + ".yml");
                            /*if(!f.exists()){
                                f = new File(Main.plugin.getDataFolder() + "/Inventories/" + locstr.replace(",","."));
                            }*/
                            if(f.exists()){
                                try {
                                    YamlConfiguration con = new YamlConfiguration();
                                    con.load(f);
                                    if(con.contains("inventory.title")){
                                        deathMessage = con.getString("inventory.title");
                                    }
                                }catch (Exception e){

                                }
                            }

                            Conf.storage.getConfig().set("player." + uid + ".deaths." + id + ".death_message", deathMessage);
                            upgraded++;
                        }catch (Exception e){
                            errors++;
                        }
                        Conf.storage.getConfig().set("player." + uuid + ".last_id", id);
                        id++;
                    }
                }
                Conf.storage.save();
                sender.sendMessage("Upgraded " + upgraded + " signs in " + (System.currentTimeMillis() - timeBefore) + " ms with " + errors + " errors.");
                return true;
            }else if (args[0].equalsIgnoreCase("reload")){
                if(!sender.hasPermission("deathsigns.admin")){
                    sender.sendMessage("§cYou do not have permission to reload this plugin.");
                    return true;
                }
                reloadConfig();
                sender.sendMessage("§7The config was reloaded.");
                return true;
            }else if (args[0].equalsIgnoreCase("demosign")){
                if(!sender.hasPermission("deathsigns.admin")){
                    sender.sendMessage("§cYou do not have permission to create demo signs.");
                    return true;
                }
                Player player = ((Player)sender);

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(Main.plugin.getConfig().getString("time_format"));
                LocalDateTime now = LocalDateTime.now();
                String dateTimeString = dtf.format(now);

                Block baseBlock = player.getLocation().getBlock();
                Block signBlock = baseBlock.getRelative(BlockFace.UP);
                baseBlock.setType(Material.CRYING_OBSIDIAN);
                signBlock.setType(Material.OAK_SIGN);
                Sign sign = (Sign) signBlock.getState();
                sign.setLine(0,"§0§lR.I.P.");
                sign.setLine(1,player.getName());
                sign.setLine(3,dateTimeString);
                double rotation = 0;
                try {
                    rotation = (player.getLocation().getYaw() - 90) % 360;//taken from worldedit or something
                }catch (Exception e){

                }
                if (rotation < 0) {
                    rotation += 360.0;
                }
                byte signRotation = (byte) Math.round(rotation / 22.5);//gets 0-15 from 0-360
                if(signRotation < 0) signRotation = 0;
                signRotation+=4;//rotate sign by 90 degrees
                if(signRotation >= 16)signRotation-=16;
                sign.setRawData((byte) signRotation);//is there a better way to rotate signs??
                sign.update();

                return true;
            }else if (args[0].equalsIgnoreCase("testthedupeglitch")){
                if(!sender.hasPermission("deathsigns.admin")){
                    sender.sendMessage("§cYou do not have permission to test dupe glitches!");
                    return true;
                }
                if(Bukkit.getOnlinePlayers().size() < 2){
                    sender.sendMessage("There are not enough players online to test the dupe glitch.");
                    return true;
                }
                Block block = ((Player)sender).getTargetBlockExact(10,FluidCollisionMode.NEVER);
                List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
                Player p1 = onlinePlayers.get(0);
                Player p2 = onlinePlayers.get(1);
                PlayerInteractEvent event1 = new PlayerInteractEvent(p1, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP, EquipmentSlot.HAND);
                Bukkit.getPluginManager().callEvent(event1);
                PlayerInteractEvent event2 = new PlayerInteractEvent(p2, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP, EquipmentSlot.HAND);
                Bukkit.getPluginManager().callEvent(event2);
                sender.sendMessage("Called test events for " + p1.getName() + " and " + p2.getName());
                return true;
            }else return false;
        }
        if(sender.hasPermission("deathsigns.admin")){
            sender.sendMessage("§cAdmin Usage: /deathsigns <§4reload | recover§c | compass | destroy | enable | disable | list> §4[player] §c[page]");
        }else{
            sender.sendMessage("§cUsage: /deathsigns <compass | enable | disable | destroy | list>");
        }
        return true;
    }
    public static void log(String message){
        if(getPlugin().getConfig().getBoolean("broadcastSignsToAdmins")){
            Bukkit.broadcast(message, "deathsigns.admin");
        }else{
            Bukkit.getLogger().info(message);
        }
    }

    /*
    */




}
