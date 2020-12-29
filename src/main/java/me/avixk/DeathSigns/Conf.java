package me.avixk.DeathSigns;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
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
        OfflinePlayer pl = Bukkit.getOfflinePlayer(player);
        String out = "";
        if(pl == null){
            return "§cPlayer not found";
        }
        out = conf("list_top_text").replace("&","§").replace("{player}",pl.getName());
        if(!signConfig.contains("signs." + player.toString())){
            out += "\n§cNo DeathSigns Found.";
            return out;
        }

        updateSigns(player);

        Set<String> o = signConfig.getConfigurationSection("signs." + player.toString()).getKeys(false);
        if(o.size() == 0)out += "\n§cNo DeathSigns Found.";
        int skip = 0;
        int max = Main.getPlugin().getConfig().getInt("recentDeathSignsMax");
        if(o.size() > max){
            skip = o.size() - max;
        }
        for(String s : o){
            if(skip > 0){
                skip--;
                continue;
            }
            Location loc = locFromString(s);
            out += "\n";
            try{
                out += conf("list_format")
                        .replace("&","§")
                        .replace("{world}",loc.getWorld().getName())
                        .replace("{X}",loc.getBlockX()+"")
                        .replace("{Y}",loc.getBlockY()+"")
                        .replace("{Z}",loc.getBlockZ()+"")
                        .replace("{status}",Main.getPlugin().getConfig().getString("list_status_strings." + getStatus(loc)).replace("&","§"))
                        .replace("{status_time}",millisToHumanString(System.currentTimeMillis() - getStatusTime(loc),true))
                        .replace("{time}",millisToHumanString(System.currentTimeMillis() - getSignTime(loc),true));
            }catch (Exception e){
                out += "§f - §cErrored DeathSign";
            }
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
        if(!signConfig.contains("signs"))return -1;
        for(String s : signConfig.getConfigurationSection("signs").getKeys(false)){
            if(signConfig.contains("signs."+s+"."+signloc)){
                return signConfig.getLong("signs."+s+"."+signloc+".time");
            }
        }
        return -1;
    }
    public static long getStatusTime(Location sign){
        String signloc = locToString(sign);
        if(!signConfig.contains("signs"))return -1;
        for(String s : signConfig.getConfigurationSection("signs").getKeys(false)){
            if(signConfig.contains("signs."+s+"."+signloc)){
                return signConfig.getLong("signs."+s+"."+signloc+".status_time");
            }
        }
        return -1;
    }
    public static String getStatus(Location sign){//RECOVERED TAKEN PROTECTED UNPROTECTED

        String signloc = locToString(sign);
        if(!signConfig.contains("signs"))return null;
        for(String s : signConfig.getConfigurationSection("signs").getKeys(false)){
            if(signConfig.contains("signs."+s+"."+signloc)){
                return signConfig.getString("signs."+s+"."+signloc+".status");
            }
        }
        return null;
    }
    public static void setStatus(Location sign, String status){
        String signloc = locToString(sign);
        if(!signConfig.contains("signs"))return;
        for(String s : signConfig.getConfigurationSection("signs").getKeys(false)){
            if(signConfig.contains("signs."+s+"."+signloc)){
                signConfig.set("signs."+s+"."+signloc+".status",status);
                signConfig.set("signs."+s+"."+signloc+".status_time",System.currentTimeMillis());
                saveSignFile();
                return;
            }
        }
    }
    public static void updateSigns(UUID player){
        //Bukkit.getLogger().info("updating player signs");
        if(!signConfig.contains("signs"))return;
        String path = "signs." + player.toString();
        //Bukkit.getLogger().info("yeh");
        for(String s : signConfig.getConfigurationSection(path).getKeys(false)){
            //Bukkit.getLogger().info("sign");
            if(signConfig.getString(path + "." + s + ".status").equals("PROTECTED")){
                //Bukkit.getLogger().info("protected sign");
                int timeout = Main.getPlugin().getConfig().getInt("signTimeoutSeconds") * 1000;
                long signtime = signConfig.getLong(path+"."+s+".time");
                //Bukkit.getLogger().info("timeout:"+timeout + " signtime:" + signtime + " after:" + (System.currentTimeMillis() > (signtime + timeout)));
                if(System.currentTimeMillis() > (signtime + timeout)){
                    signConfig.set(path+"."+s+".status","UNPROTECTED");
                    signConfig.set(path+"."+s+".status_time",(signtime + timeout));
                    saveSignFile();
                }
            }
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
    public static boolean signExistsInConfig(Location sign){
        String signloc = locToString(sign);
        if(!signConfig.contains("signs"))return false;
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
    public static String millisToHumanString(long millis, boolean addColor){
        if(millis < 0)return "null";
        long seconds = millis/1000;
        String pretext = "";
        if (seconds <= 60){//1 minute
            //seconds only
            if(addColor)pretext += "§a";
            return pretext + seconds + "s";
        }else{
            if (seconds < 3600){//1 hour
                //minutes and seconds
                long mins = seconds / 60;
                long remainder = seconds - (mins * 60);
                if(addColor)pretext += "§e";
                return pretext + mins + "m" + ((remainder == 0)? "" : (" " + remainder + "s"));
            }else{
                if (seconds < 86400){//1 day
                    //hours and minutes
                    long mins = seconds / 60;
                    long hours = mins / 60;
                    long remainder = mins - (hours * 60);
                    if(addColor)pretext += "§6";
                    return pretext + hours + "h" + ((remainder == 0)? "" : (" " + remainder + "m"));
                }else{
                    if (seconds < 604800){//7 days
                        //days and hours
                        long hours = seconds / 3600;
                        long days = hours / 24;
                        long remainder = hours - (days * 24);
                        if(addColor)pretext += "§c";
                        return pretext + days + "d" + ((remainder == 0)? "" : (" " + remainder + "h"));
                    }else{
                        //days only
                        long hours = seconds / 3600;
                        long days = hours / 24;
                        if(addColor)pretext += "§4";
                        return pretext + days + "d";
                    }
                }
            }
        }
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
}
