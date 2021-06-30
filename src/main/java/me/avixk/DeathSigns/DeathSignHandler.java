package me.avixk.DeathSigns;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.tags.CustomItemTagContainer;
import org.bukkit.inventory.meta.tags.ItemTagType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeathSignHandler {
    public static List<DeathSign> openSigns = new ArrayList<>();
    public static NamespacedKey key;
    /*public static DeathSign getSign(Location signLoc){
        for(DeathSign sign : openSigns){
            if(sign.location.equals(signLoc))
                return sign;
        }
        return null;
    }
    public static DeathSign getOrLoadSign(Location signLoc){
        for(DeathSign sign : openSigns){
            if(sign.location.equals(signLoc))
                return sign;
        }
        return loadSign(signLoc);
    }*/
    /*public static DeathSign loadSign(Location signLoc){
        Map.Entry<ItemStack[], String> items = Main.recallItems(signLoc,false);
        if(items == null)return null;
        UUID owner = Conf.signBelongsTo(signLoc);
        String title = items.getValue();
        if(title == null)title = "Unknown DeathSign";
        Inventory inv = Bukkit.createInventory(null,45,title);
        inv.setContents(items.getKey());
        DeathSign dsign = new DeathSign(owner,items.getValue(),signLoc,inv,Conf.getSignID(owner,signLoc));
        openSigns.add(dsign);
        return dsign;
    }*/
    public static void removeSign(DeathSign sign){
        openSigns.remove(sign);
    }
    public static DeathSign getDeathSignFromIdentifier(String identifier){
        try{
            String[] parts = identifier.split("\\.");
            String uuid = parts[0];
            String id = parts[1];
            for(DeathSign loadedSign : openSigns){
                if(loadedSign.owner.toString().equals(uuid) && id.equals(loadedSign.id + ""))
                    return loadedSign;
            }
            Location location = Util.locationFromString(Conf.storage.getConfig().getString("player." + uuid + ".deaths." + id + ".loc"));
            if(location != null){
                DeathSign deathSign = new DeathSign(UUID.fromString(uuid),location,null, Integer.parseInt(id));
                openSigns.add(deathSign);
                return deathSign;
            }
        }catch (Exception e){

        }
        return null;
    }
    public static boolean breakIfCompass(Location effectLoc, ItemStack clicked){
        String compassID = getCompassID(clicked);
        if(compassID == null)return false;
        DeathSign deathSign = DeathSignHandler.getDeathSignFromIdentifier(compassID);
        if(deathSign != null && !deathSign.getStatus().equals("RECOVERED") && !deathSign.getStatus().equals("TAKEN")){
            return false;
        }
        effectLoc.getWorld().spawnParticle(Particle.ITEM_CRACK,effectLoc,10,0.1,0.1,0.1, 0.05,new ItemStack(Material.COMPASS));
        effectLoc.getWorld().playSound(effectLoc,Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 0.7F,1);
        return true;
    }
    public static String getCompassID(ItemStack compass){
        if(compass != null && compass.getType().equals(Material.COMPASS)){
            CompassMeta meta = (CompassMeta) compass.getItemMeta();
            if (meta != null && meta.hasLodestone()) {
                CustomItemTagContainer tagContainer = meta.getCustomTagContainer();
                if (tagContainer.hasCustomTag(key, ItemTagType.STRING)) {
                    return tagContainer.getCustomTag(key, ItemTagType.STRING);
                }
            }
        }
        return null;
    }
    public static DeathSign getDeathSignAtLoc(Location loc){
        DeathSign sign = getLoadedDeathSign(loc);
        if(sign != null){
            return sign;
        }
        String targetLoc = Util.locationToString(loc);
        if(!Conf.storage.getConfig().contains("player"))return null;
        for(String uuid : Conf.storage.getConfig().getConfigurationSection("player").getKeys(false)){
            if(Conf.storage.getConfig().contains("player." + uuid + ".deaths"))for(String id : Conf.storage.getConfig().getConfigurationSection("player." + uuid + ".deaths").getKeys(false)){
                if(Conf.storage.getConfig().contains("player." + uuid + ".deaths." + id + ".loc") && Conf.storage.getConfig().getString("player." + uuid + ".deaths." + id + ".loc").equals(targetLoc)){
                    String status = Conf.storage.getConfig().getString("player." + uuid + ".deaths." + id + ".status");
                    if(!status.equals("RECOVERED") && !status.equals("TAKEN") && !status.equals("DESTROYED")){
                        DeathSign deathSign = new DeathSign(UUID.fromString(uuid),loc,null, Integer.parseInt(id));
                        openSigns.add(deathSign);
                        return deathSign;
                    }
                }
            }
        }
        return null;
    }
    public static DeathSign getLoadedDeathSign(Inventory inv){
        for(DeathSign sign : openSigns){
            if(sign.inventory != null && sign.inventory.equals(inv))
                return sign;
        }
        return null;
    }
    public static DeathSign getLoadedDeathSign(Location loc){
        for(DeathSign sign : openSigns){
            if(sign.location != null && sign.location.equals(loc))
                return sign;
        }
        return null;
    }
    public static void updateSigns(UUID player){
        //Bukkit.getLogger().info("updating player signs");
        if(!Conf.storage.getConfig().contains("player"))return;
        String path = "player." + player.toString() + ".deaths";
        //Bukkit.getLogger().info("yeh");
        if(Conf.storage.getConfig().contains(path))for(String id : Conf.storage.getConfig().getConfigurationSection(path).getKeys(false)){
            //Bukkit.getLogger().info("sign");
            if(Conf.storage.getConfig().getString(path + "." + id + ".status").equals("PROTECTED")){
                //Bukkit.getLogger().info("protected sign");
                int timeout = Main.getPlugin().getConfig().getInt("sign_timeout_seconds") * 1000;
                long signtime = Conf.storage.getConfig().getLong(path+"."+id+".time");
                //Bukkit.getLogger().info("timeout:"+timeout + " signtime:" + signtime + " after:" + (System.currentTimeMillis() > (signtime + timeout)));
                if(System.currentTimeMillis() > (signtime + timeout)){
                    Conf.storage.getConfig().set(path+"."+id+".status","UNPROTECTED");
                    Conf.storage.getConfig().set(path+"."+id+".status_time",(signtime + timeout));
                    Conf.storage.save();
                }
            }
        }
    }

    public static DeathSign createDeathSign(ItemStack[] items, UUID owner, Location location, String deathMessage){
        int id = 1;
        if(Conf.storage.getConfig().contains("player." + owner + ".last_id")){
            id = Conf.storage.getConfig().getInt("player." + owner + ".last_id") + 1;
        }
        Conf.storage.getConfig().set("player." + owner + ".last_id", id);

        if(Main.plugin.getConfig().getInt("sign_timeout_seconds") > 0)
            Conf.storage.getConfig().set("player." + owner + ".deaths." + id + ".status", "PROTECTED");
        else
            Conf.storage.getConfig().set("player." + owner + ".deaths." + id + ".status", "UNPROTECTED");

        Conf.storage.getConfig().set("player." + owner + ".deaths." + id + ".status_time", System.currentTimeMillis());
        Conf.storage.getConfig().set("player." + owner + ".deaths." + id + ".time", System.currentTimeMillis());
        Conf.storage.getConfig().set("player." + owner + ".deaths." + id + ".loc", Util.locationToString(location));
        Conf.storage.getConfig().set("player." + owner + ".deaths." + id + ".death_message", deathMessage);

        Inventory inventory = Bukkit.createInventory(null,45,deathMessage);
        inventory.setContents(items);

        DeathSign deathSign = new DeathSign(owner,location,inventory, id);
        //deathSign.saveItems();//redundant i believe
        Conf.storage.save();

        openSigns.add(deathSign);
        return deathSign;
    }
    public static void breakBrokenSign(Player breaker, Block sign){

        if(Conf.deathsign_reward_enabled && Econ.set_up){
            boolean success = Econ.givePlayerMoney(breaker.getUniqueId(),Conf.deathsign_reward);
            if(!success)Main.plugin.getLogger().info("Reward transaction failed! Player didn't receive reward!");
            else breaker.sendMessage(Main.plugin.getConfig().getString("econ.deathsign.reward_message").replace("{reward}",Conf.deathsign_reward+"").replace("&","§"));
        }

        sign.setType(Material.AIR);
        sign.getRelative(0, -1, 0).setType(Material.AIR);
        Location l1 = sign.getLocation().clone().add(.5,0,.5), l2 = sign.getLocation().clone().add(.5,-1,.5);
        sign.getWorld().playSound(l1, Sound.BLOCK_WOOD_BREAK,1,1);
        sign.getWorld().spawnParticle(Particle.BLOCK_DUST, l1, 50, .5,.5,.5, Material.OAK_PLANKS.createBlockData());
        sign.getWorld().playSound(l2, Sound.BLOCK_STONE_BREAK,1,1);
        sign.getWorld().spawnParticle(Particle.BLOCK_DUST, l2, 50, .5,.5,.5, Material.CRYING_OBSIDIAN.createBlockData());

        File file = new File(Main.plugin.getDataFolder() + "/Inventories/", Util.locationToString(sign.getLocation()) + ".yml");
        if(file.exists()) {
            Bukkit.getLogger().info("Dropping items from old or errored sign: " + Util.locationToString(sign.getLocation()));
            YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
            ItemStack[] items = ((List<ItemStack>) c.get("inventory.content")).toArray(new ItemStack[0]);
            DeathSign.deleteSignFile(file);
            for (ItemStack i : items) {
                if (i != null) sign.getWorld().dropItemNaturally(sign.getLocation().add(0.5,0.5,0.5), i);
            }
        }

        DeathSign deathSign = getDeathSignAtLoc(sign.getLocation());
        if(deathSign != null){
            if(deathSign.getOwner().equals(breaker.getUniqueId())){
                deathSign.setStatus("RECOVERED");
            }else{
                deathSign.setStatus("TAKEN");
            }
            DeathSignHandler.removeSign(deathSign);
        }
    }

    public static Block searchForSignPlacement(Block center, List<Block> checkedBlocks, int maxDistance) {
        for (int currentDistance = 0; currentDistance <= maxDistance; currentDistance++) { // Search for valid spawn location
            for (int z = -(currentDistance); z <= currentDistance; z++) {
                for (int x = -(currentDistance); x <= currentDistance; x++) {
                    for (int y = 0; y <= (currentDistance * 2); y++) {
                        Block nearbyBlock = center.getRelative(x, y, z);
                        if(checkedBlocks.contains(nearbyBlock))continue;
                        if ((nearbyBlock.getType().equals(Material.AIR)
                                || nearbyBlock.getType().equals(Material.CAVE_AIR)
                                || nearbyBlock.getType().equals(Material.LAVA)
                                || nearbyBlock.getType().equals(Material.WATER))
                                && (nearbyBlock.getRelative(0, 1, 0).getType().equals(Material.AIR)
                                || nearbyBlock.getRelative(0, 1, 0).getType().equals(Material.CAVE_AIR)
                                || nearbyBlock.getRelative(0, 1, 0).getType().equals(Material.LAVA)
                                || nearbyBlock.getRelative(0, 1, 0).getType().equals(Material.WATER))) {
                            if(DeathSignHandler.getDeathSignAtLoc(nearbyBlock.getRelative(0, 1, 0).getLocation()) != null)continue;
                            if(DeathSignHandler.getDeathSignAtLoc(nearbyBlock.getLocation()) != null)continue;
                            if(Events.deathsignQueue.contains(nearbyBlock.getLocation()) || Events.deathsignQueue.contains(nearbyBlock.getRelative(0,1,0).getLocation()))continue;//TODO not even fuckin used
                            if(isEntityAtBlock(nearbyBlock))continue;

                            return nearbyBlock.getRelative(0,1,0);
                        }
                    }
                }
            }
        }
        return null;
    }
    public static boolean isEntityAtBlock(Block block){
        for(Entity entity : block.getWorld().getEntities()){
            if(entity.getLocation().getBlock().equals(block)){
                return true;
            }
        }
        return false;
    }
}
