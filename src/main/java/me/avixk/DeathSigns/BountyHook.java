package me.avixk.DeathSigns;

import me.avixk.HeadHunting.BountyAPI;
import org.bukkit.entity.Player;

public class BountyHook {
    public static BountyAPI bountyAPI;
    public static boolean init() {
        if (Main.plugin.getServer().getPluginManager().getPlugin("HeadHunting") == null) {
            return false;
        }
        try {
            bountyAPI = BountyAPI.instance;
        }catch (Exception e){
            return false;
        }
        return true;
    }
    public static boolean willClaimBounty(Player killer, Player player){
        if(bountyAPI == null) return false;
        return bountyAPI.willBountyClaim(killer,player);
    }
}
