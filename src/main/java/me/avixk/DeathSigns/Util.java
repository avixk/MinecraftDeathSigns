package me.avixk.DeathSigns;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class Util {

    public static ItemStack getLodestoneCompass(Location location){
        ItemStack compass = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) Bukkit.getItemFactory().getItemMeta(compass.getType());
        meta.setLodestoneTracked(false);// set to false so lodestone isnt required
        meta.setLodestone(location);
        compass.setItemMeta(meta);
        return compass;
    }
    public static Location locationFromString(String locString) {
        try {
            String[] s = locString.split(",");
            return new Location(Bukkit.getWorld(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2]),Integer.parseInt(s[3]));
        }     catch (Exception e){

        }
        return null;

    }
    public static String locationToString(Location loc) {//just turns a location into a string
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," +loc.getBlockY() + "," +loc.getBlockZ();
    }

    public static List<String> playerNamesOrOfflinePlayerNames(String relevantString){
        long time = System.currentTimeMillis();
        List<String> names = new ArrayList<>();
        for(Player p : Bukkit.getOnlinePlayers()){
            names.add(p.getName());
        }
        List<String> completions = new ArrayList<>();
        StringUtil.copyPartialMatches(relevantString, names, completions);
        if(completions.size() > 0){
            return completions;
        }
        names.clear();
        for(OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()){
            names.add(offlinePlayer.getName());
        }
        StringUtil.copyPartialMatches(relevantString, names, completions);
        //Bukkit.getLogger().info("Player name collection took " + (System.currentTimeMillis() - time) + " ms");
        return completions;
    }
    public static String millisToHumanString(long millis, boolean addColor){//returns a string from seconds to days
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
}
