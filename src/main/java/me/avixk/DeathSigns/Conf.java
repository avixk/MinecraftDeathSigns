package me.avixk.DeathSigns;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class Conf {
    static File signFile = new File(Main.getPlugin().getDataFolder() + "/sign_locations.yml");
    static YamlConfiguration signConfig = new YamlConfiguration();
    public static void loadSignFile(){
        try {
            signFile.mkdirs();
            signFile.createNewFile();
            signConfig.load(signFile);
        } catch (IOException | InvalidConfigurationException e) {
            Bukkit.getLogger().severe("Sign file failed to load.");
            e.printStackTrace();
        }
    }
    public static void saveSignFile(){
        try {
            signConfig.save(signFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String getListText(UUID player){
        String out = conf("list_top_text").replace("&","§").replace("{player}",Bukkit.getPlayer(player).getName());
        if(!signConfig.contains("signs." + player.toString())){
            out += "\n§cNo DeathSigns Found.";
            return out;
        }
        Set<String> o = signConfig.getConfigurationSection("signs." + player.toString()).getKeys(false);
        if(o.size() == 0)out += "\n§cNo DeathSigns Found.";
        int skip = 0;
        if(o.size() > 6){
            skip = o.size() - 6;
        }
        for(String s : o){
            if(skip > 0){
                skip--;
                continue;
            }
            Location loc = locFromString(s);
            out += "\n";
            out += conf("list_format")
                    .replace("&","§")
                    .replace("{world}",loc.getWorld().getName())
                    .replace("{X}",loc.getBlockX()+"")
                    .replace("{Y}",loc.getBlockY()+"")
                    .replace("{Z}",loc.getBlockZ()+"")
                    .replace("{status}",Main.getPlugin().getConfig().getString("list_status_strings." + getStatus(loc)).replace("&","§"))
                    .replace("{status_time}",millisToHumanString(System.currentTimeMillis() - getStatusTime(loc)))
                    .replace("{time}",millisToHumanString(System.currentTimeMillis() - getSignTime(loc)));
        }
        return out;
    }
    public static void addSign(Location sign, UUID player){
        String signpath = "signs." + player + "." + locToString(sign) + ".";
        long millis = System.currentTimeMillis();
        signConfig.set(signpath + "status","PROTECTED");
        signConfig.set(signpath + "status_time",millis);
        signConfig.set(signpath + "time",millis);
        saveSignFile();
    }
    public static void removeSign(Location sign, UUID player){
        String signpath = "signs." + player + "." + locToString(sign);
        if(signConfig.contains(signpath)){
            signConfig.set(signpath,null);
            saveSignFile();
        }
    }
    public static long getSignTime(Location sign){
        String signloc = locToString(sign);
        for(String s : signConfig.getConfigurationSection("signs").getKeys(false)){
            if(signConfig.contains("signs."+s+"."+signloc)){
                return signConfig.getLong("signs."+s+"."+signloc+".time");
            }
        }
        return -1;
    }
    public static long getStatusTime(Location sign){
        String signloc = locToString(sign);
        for(String s : signConfig.getConfigurationSection("signs").getKeys(false)){
            if(signConfig.contains("signs."+s+"."+signloc)){
                return signConfig.getLong("signs."+s+"."+signloc+".status_time");
            }
        }
        return -1;
    }
    public static String getStatus(Location sign){//RECOVERED TAKEN PROTECTED UNPROTECTED

        String signloc = locToString(sign);
        for(String s : signConfig.getConfigurationSection("signs").getKeys(false)){
            if(signConfig.contains("signs."+s+"."+signloc)){
                return signConfig.getString("signs."+s+"."+signloc+".status");
            }
        }
        return null;
    }
    public static void setStatus(Location sign, String status){
        String signloc = locToString(sign);
        for(String s : signConfig.getConfigurationSection("signs").getKeys(false)){
            if(signConfig.contains("signs."+s+"."+signloc)){
                signConfig.set("signs."+s+"."+signloc+".status",status);
                signConfig.set("signs."+s+"."+signloc+".status_time",System.currentTimeMillis());
                saveSignFile();
                return;
            }
        }
    }
    public static void updateSign(Location sign){
        String signloc = locToString(sign);
        for(String s : signConfig.getConfigurationSection("signs").getKeys(false)){
            if(signConfig.contains("signs."+s+"."+signloc)){
                int timeout = Main.getPlugin().getConfig().getInt("signTimeoutSeconds") * 1000;
                long signtime = signConfig.getLong("signs."+s+"."+signloc+".time");
                if(System.currentTimeMillis() > (signtime + timeout)){
                    signConfig.set("signs."+s+"."+signloc+".status","UNPROTECTED");
                    signConfig.set("signs."+s+"."+signloc+".status_time",System.currentTimeMillis());
                    saveSignFile();
                }
                return;
            }
        }
    }
    public static boolean signBelongsTo(Location sign, UUID player){
        String signloc = locToString(sign);
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
    public static boolean signExistsInConfig(Location sign){
        String signloc = locToString(sign);
        for(String s : signConfig.getConfigurationSection("signs").getKeys(false)){
            if(signConfig.contains("signs."+s+"."+signloc)){
                return true;
            }
        }
        return false;
    }
    public static String conf(String entry){
        return Main.getPlugin().getConfig().getString(entry);
    }
    public static String millisToHumanString(long millis){
        if(millis < 0)return "null";
        long seconds = millis/1000;
        if (seconds <= 60){//1 minute
            //seconds only
            return seconds + "s";
        }else{
            if (seconds < 3600){//1 hour
                //minutes and seconds
                long mins = seconds / 60;
                long remainder = seconds - (mins * 60);
                return mins + "m" + ((remainder == 0)? "" : (" " + remainder + "s"));
            }else{
                if (seconds < 86400){//1 day
                    //hours and minutes
                    long mins = seconds / 60;
                    long hours = mins / 60;
                    long remainder = mins - (hours * 60);
                    return hours + "h" + ((remainder == 0)? "" : (" " + remainder + "m"));
                }else{
                    if (seconds < 604800){//7 days
                        //days and hours
                        long hours = seconds / 3600;
                        long days = hours / 24;
                        long remainder = hours - (days * 24);
                        return days + "d" + ((remainder == 0)? "" : (" " + remainder + "h"));
                    }else{
                        //days only
                        long hours = seconds / 3600;
                        long days = hours / 24;
                        return days + "d";
                    }
                }
            }
        }
    }

    static final char separator = '_';
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
}
