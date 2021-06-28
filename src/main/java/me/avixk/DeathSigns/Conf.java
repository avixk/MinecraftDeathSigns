package me.avixk.DeathSigns;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Conf {
    public static boolean deathsign_cost_enabled = false;
    public static boolean deathsign_reward_enabled = false;
    public static double deathsign_cost = 0;
    public static double deathsign_reward = 0;
    public static int free_deathsigns = 0;

    public static void loadConfigOptions(){
        deathsign_cost_enabled = Main.plugin.getConfig().getBoolean("econ.deathsign.cost_enabled");
        deathsign_reward_enabled = Main.plugin.getConfig().getBoolean("econ.deathsign.reward_enabled");
        deathsign_cost = Main.plugin.getConfig().getDouble("econ.deathsign.cost");
        deathsign_reward = Main.plugin.getConfig().getDouble("econ.deathsign.reward");
        free_deathsigns = Main.plugin.getConfig().getInt("econ.deathsign.free");

    }

    static YamlFile storage;
    public static void loadSignFile(){
        storage = new YamlFile("storage");
    }
    public static void saveSignFile(){
        storage.save();
    }

    public static int getLastID(UUID player){
        if(Conf.storage.getConfig().contains("player." + player + ".last_id")){
            return Conf.storage.getConfig().getInt("player." + player + ".last_id");
        }
        return 1;
    }

    public static double judgeInventoryWorth(Inventory inventory){
        double multuplier = Main.plugin.getConfig().getDouble("destroy_command.explosion.item_worth_multiplier");
        double worth = 0;
        for(ItemStack item : inventory.getContents()){
            if(item == null)continue;
            if(Main.plugin.getConfig().contains("destroy_command.explosion.item_worth." + item.getType().name())){
                worth += Main.plugin.getConfig().getDouble("destroy_command.explosion.item_worth." + item.getType().name()) * item.getAmount();
            }
        }
        return worth * multuplier;
    }

    public static String getListText(UUID player, int page){
        OfflinePlayer pl = Bukkit.getOfflinePlayer(player);
        String out = "";
        if(pl == null || !pl.hasPlayedBefore()){
            return "§cPlayer not found";
        }
        out = conf("list_top_text").replace("{version}",Main.getPlugin().getDescription().getVersion()).replace("&","§").replace("{player}",pl.getName());
        if(!Conf.storage.getConfig().contains("player." + player)){
            out += "\n" + Main.plugin.getConfig().getString("list_format_no_deathsigns").replace("&","§");
            return out;
        }

        DeathSignHandler.updateSigns(player);

        List<String> ids = new ArrayList<>(Conf.storage.getConfig().getConfigurationSection("player." + player.toString() + ".deaths").getKeys(false));
        Collections.reverse(ids);
        if(ids.size() == 0)out += "\n" + Main.plugin.getConfig().getString("list_format_no_deathsigns").replace("&","§");
        //int skip = 0;
        int max = Main.getPlugin().getConfig().getInt("list_page_size");
        int pages = (ids.size() / max) + 1;
        if(page > pages){
            return "§cThis page does not exist";
        }
        /*if(page == 1){
            if(o.size() > max){
                skip = o.size() - max;
            }
        }else {
            skip = max * (page - 1);
        }*/
        int first = (page - 1) * max;
        int last = (page) * max;
        if(first < 0) first = 0;
        if(last > ids.size())last = ids.size();
        List<String> sublist = ids.subList(first, last);
        for(String s : sublist){
            /*if(skip > 0){
                skip--;
                continue;
            }*/
            out += "\n";
            try{
                Location loc = Util.locationFromString(Conf.storage.getConfig().getString("player." + player + ".deaths." + s + ".loc"));
                DeathSign deathSign = new DeathSign(player,loc,null, Integer.parseInt(s));
                out += conf("list_format")
                        .replace("&","§")
                        .replace("{id}",s+"")
                        .replace("{world}",loc.getWorld().getName())
                        .replace("{X}",loc.getBlockX()+"")
                        .replace("{Y}",loc.getBlockY()+"")
                        .replace("{Z}",loc.getBlockZ()+"")
                        .replace("{status}",Main.getPlugin().getConfig().getString("list_status_strings." + deathSign.getStatus()).replace("&","§"))
                        .replace("{status_time}",Util.millisToHumanString(System.currentTimeMillis() - deathSign.getStatusTime(),true))
                        .replace("{time}",Util.millisToHumanString(System.currentTimeMillis() - deathSign.getCreationTime(),true));
            }catch (Exception e){
                e.printStackTrace();
                out += conf("list_format_error")
                        .replace("&","§")
                        .replace("{id}",s+"");

            }
        }
        out += "\n" + conf("list_bottom_text").replace("&","§").replace("{page}",page+"").replace("{pages}",pages+"");
        return out;
    }/*
    public static void addSign(DeathSign sign){
        DeathSign oldSign = DeathSignHandler.getSign(sign.location);
        if(oldSign != null) DeathSignHandler.removeSign(oldSign);
        String signpath = "signs." + sign.owner + "." + locToString(sign.location) + ".";
        long millis = System.currentTimeMillis();
        signConfig.set(signpath + "status","PROTECTED");
        signConfig.set(signpath + "status_time",millis);
        signConfig.set(signpath + "time",millis);
        signConfig.set(signpath + "id",sign.getId());
        saveSignFile();
    }
    public static void removeSign(DeathSign sign){
        String signpath = "signs." + sign.owner + "." + locToString(sign.location);
        if(signConfig.contains(signpath)){
            signConfig.set(signpath,null);
            saveSignFile();
        }
    }*/
    public static int getFreeSigns(UUID player){
        if(!storage.getConfig().contains("player." + player + ".free_signs")){
            return Conf.free_deathsigns;
        }
        return storage.getConfig().getInt("player." + player + ".free_signs");
    }
    public static int useFreeSign(UUID player){//return -1 if no free sign is available, return 0 if this is last free sign
        if(!storage.getConfig().contains("player." + player + ".free_signs")){
            int amount = Conf.free_deathsigns - 1;
            if(amount >= 0) {
                storage.getConfig().set("player." + player + ".free_signs", amount);
                saveSignFile();
            }
            return amount;
        }else {
            int amount = storage.getConfig().getInt("player." + player + ".free_signs") - 1;
            if(amount >= 0) {
                storage.getConfig().set("player." + player + ".free_signs", amount);
                saveSignFile();
            }
            return amount;
        }
    }/*
    public static int getSignID(UUID player, Location sign){
        String signloc = locToString(sign);
        if(!signConfig.contains("signs"))return -1;
        if(signConfig.contains("signs."+player+"."+signloc)){
            return signConfig.getInt("signs."+player+"."+signloc+".id");
        }
        return -1;
    }
    public static long getSignTime(UUID player, Location sign){
        String signloc = locToString(sign);
        if(!signConfig.contains("signs"))return -1;
        if(signConfig.contains("signs."+player+"."+signloc)){
            return signConfig.getLong("signs."+player+"."+signloc+".time");
        }
        return -1;
    }
    public static long getStatusTime(UUID player, Location sign){
        String signloc = locToString(sign);
        if(!signConfig.contains("signs"))return -1;
        if(signConfig.contains("signs."+player+"."+signloc)){
            return signConfig.getLong("signs."+player+"."+signloc+".status_time");
        }
        return -1;
    }
    public static String getStatus(UUID player, Location sign){//RECOVERED TAKEN PROTECTED UNPROTECTED
        String signloc = locToString(sign);
        if(!signConfig.contains("signs"))return null;
        if(signConfig.contains("signs."+player+"."+signloc)){
            return signConfig.getString("signs."+player+"."+signloc+".status");
        }
        return null;
    }
    public static void setStatus(UUID player, Location sign, String status){
        String signloc = locToString(sign);
        if(!signConfig.contains("signs"))return;
        if(signConfig.contains("signs."+player+"."+signloc)){
            signConfig.set("signs."+player+"."+signloc+".status",status);
            signConfig.set("signs."+player+"."+signloc+".status_time",System.currentTimeMillis());
            saveSignFile();
            return;
        }
    }

    public static boolean signBelongsTo(Location sign, UUID player){
        String signloc = locToString(sign);
        if(!signConfig.contains("signs"))return false;
        for(String s : signConfig.getConfigurationSection("signs").getKeys(false)){
            if(signConfig.contains("signs."+s+"."+signloc)){
                if(player.toString().equals(s)){
                    return true;
                }
                return false;
            }
        }
        return false;
    }
    public static UUID signBelongsTo(Location sign){
        String signloc = locToString(sign);
        if(!signConfig.contains("signs"))return null;
        for(String s : signConfig.getConfigurationSection("signs").getKeys(false)){
            if(signConfig.contains("signs."+s+"."+signloc)){
                return UUID.fromString(s);
            }
        }
        return null;
    }
    public static boolean signExistsInConfig(Location sign){
        String signloc = locToString(sign);
        if(!signConfig.contains("signs"))return false;
        for(String s : signConfig.getConfigurationSection("signs").getKeys(false)){
            if(signConfig.contains("signs."+s+"."+signloc)){
                return true;
            }
        }
        return false;
    }*/
    public static String conf(String entry){
        return translateHexColorCodes("§#","",Main.getPlugin().getConfig().getString(entry).replace("&","§"));
    }


    static final char separator = ',';
    public static String locToString(Location location){
        return location.getWorld().getName() + separator + location.getBlockX() + separator + location.getBlockY() + separator + location.getBlockZ();
    }
    public static Location locFromString(String locString){
        try{
            String[] s = locString.split(separator+"");
            return new Location(Bukkit.getWorld(s[0]),Integer.parseInt(s[1]),Integer.parseInt(s[2]),Integer.parseInt(s[3]));
        }catch (Exception ignored){}
        return null;
    }
    public static final char COLOR_CHAR = '\u00A7';
    public static String translateHexColorCodes(String startTag, String endTag, String message)
    {
        final Pattern hexPattern = Pattern.compile(startTag + "([A-Fa-f0-9]{6})" + endTag);
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find())
        {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5)
            );
        }
        return matcher.appendTail(buffer).toString();
    }
}
