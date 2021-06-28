package me.avixk.DeathSigns;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.tags.ItemTagType;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DeathSign {



    public UUID owner;
    public Location location;
    public int id;
    public Inventory inventory;

    public DeathSign(UUID owner, Location location, Inventory inventory, int id){
        this.id = id;
        this.owner = owner;
        this.location = location;
        this.inventory = inventory;
    }

    public int getId(){
        return id;
    }

    public String getStatus(){
        if(!Conf.storage.getConfig().contains("player." + owner + ".deaths." + id + ".status"))return null;
        return Conf.storage.getConfig().getString("player." + owner + ".deaths." + id + ".status");
    }

    public void setStatus(String status){
        Conf.storage.getConfig().set("player." + owner + ".deaths." + id + ".status", status);
        Conf.storage.save();
    }

    public long getStatusTime(){
        if(!Conf.storage.getConfig().contains("player." + owner + ".deaths." + id + ".status_time"))return -1;
        return Conf.storage.getConfig().getLong("player." + owner + ".deaths." + id + ".status_time");
    }

    public void setStatusTime(long statusTime){
        Conf.storage.getConfig().set("player." + owner + ".deaths." + id + ".status_time", statusTime);
        Conf.storage.save();
    }

    public String getDeathMessage(){
        if(!Conf.storage.getConfig().contains("player." + owner + ".deaths." + id + ".death_message"))return null;
        return Conf.storage.getConfig().getString("player." + owner + ".deaths." + id + ".death_message");
    }

    public long getCreationTime(){
        if(!Conf.storage.getConfig().contains("player." + owner + ".deaths." + id + ".time"))return -1;
        return Conf.storage.getConfig().getLong("player." + owner + ".deaths." + id + ".time");
    }

    public Inventory getInventory(){
        if(inventory == null)loadItems(false);
        return inventory;
    }

    public Location getLocation(){
        return location;
    }

    public UUID getOwner(){
        return owner;
    }

    public ItemStack getDeathsignCompass(){
        String player = Bukkit.getOfflinePlayer(owner).getName();
        ItemStack compass = Util.getLodestoneCompass(location);
        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        meta.setDisplayName(format(Main.plugin.getConfig().getString("death_compass.name"),location,player,getDeathMessage(),id));
        List<String> lore = new ArrayList<>();
        for(String s : Main.plugin.getConfig().getStringList("death_compass.lore")){
            lore.add(format(s,location,player,getDeathMessage(),id));
        }
        meta.setLore(lore);

        NamespacedKey key = new NamespacedKey(Main.plugin, "deathsigns.compass");// this gives the compass metadata containing the deathsign's location
        String identifier = owner + "." + id;
        meta.getCustomTagContainer().setCustomTag(key, ItemTagType.STRING, owner + "." + id);

        compass.setItemMeta(meta);
        return compass;
    }
    public String format(String string, Location loc, String player, String deathmessage, int id){
        return string.replace("&","§")
                .replace("{id}",""+id)
                .replace("{death_message}",deathmessage)
                .replace("{player}",player)
                .replace("{x}",loc.getBlockX()+"")
                .replace("{y}",loc.getBlockY()+"")
                .replace("{z}",loc.getBlockZ()+"")
                .replace("{world}",loc.getWorld().getName());
    }
    public boolean saveItems(){//saves items to file based on location
        if(inventory == null) return false;
        YamlConfiguration c = new YamlConfiguration();
        File file = new File(Main.plugin.getDataFolder() + "/Inventories/", Util.locationToString(location) + ".yml");
        if(file.exists()){
            try {
                c.load(file);
            } catch (InvalidConfigurationException | IOException e) {
                Bukkit.getLogger().warning("Could not save " + file.getAbsolutePath());
                e.printStackTrace();
            }
        }
        c.set("inventory.content", inventory.getContents());
        try {
            c.save(file);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void loadItems(boolean delete){//recalls items from file based on location, then deletes the file
        //Bukkit.broadcastMessage("trying to load from " + Util.locationToString(location));
        File file = new File(Main.plugin.getDataFolder() + "/Inventories/", Util.locationToString(location) + ".yml");
        if(!file.exists()) return;
        YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
        ItemStack[] items = ((List<ItemStack>) c.get("inventory.content")).toArray(new ItemStack[0]);
        if(delete)deleteSignFile(file);
        String deathMessage = getDeathMessage();
        if(deathMessage == null && c.contains("inventory.title")){
            deathMessage = c.getString("inventory.title");
        }
        if(deathMessage == null){
            inventory = Bukkit.createInventory(null,45,"Broken DeathSign");
        }else{
            inventory = Bukkit.createInventory(null,45,deathMessage);
        }
        inventory.setContents(items);
    }
    public static void deleteSignFile(File file){
        try {
            file.delete();
        }catch (Exception e){
            Bukkit.getLogger().warning("Delete sign file failed! " + file.getAbsolutePath());
        }
    }
    public void breakSign(Player breaker){
        if(Conf.deathsign_reward_enabled && Econ.set_up){
            boolean success = Econ.givePlayerMoney(breaker.getUniqueId(),Conf.deathsign_reward);
            if(!success)Main.plugin.getLogger().info("Reward transaction failed! Player didn't receive reward!");
            else breaker.sendMessage(Main.plugin.getConfig().getString("econ.deathsign.reward_message").replace("{reward}",Conf.deathsign_reward+"").replace("&","§"));
        }
        if(owner.equals(breaker.getUniqueId())){
            setStatus("RECOVERED");
        }else{
            setStatus("TAKEN");
        }
        location.getBlock().setType(Material.AIR);
        location.getBlock().getRelative(0, -1, 0).setType(Material.AIR);
        Location l1 = location.clone().add(.5,0,.5), l2 = location.clone().add(.5,-1,.5);
        location.getWorld().playSound(l1, Sound.BLOCK_WOOD_BREAK,1,1);
        location.getWorld().spawnParticle(Particle.BLOCK_DUST, l1, 50, .5,.5,.5, Material.OAK_PLANKS.createBlockData());
        location.getWorld().playSound(l2, Sound.BLOCK_STONE_BREAK,1,1);
        location.getWorld().spawnParticle(Particle.BLOCK_DUST, l2, 50, .5,.5,.5, Material.CRYING_OBSIDIAN.createBlockData());
        for (ItemStack i : inventory.getContents()) {
            if (i != null) location.getWorld().dropItem(location.add(0.5,0.5,0.5), i);
        }
        DeathSignHandler.removeSign(this);
        File file = new File(Main.plugin.getDataFolder() + "/Inventories/", Util.locationToString(location) + ".yml");
        deleteSignFile(file);
    }
    public void selfDestruct(){
        if(getInventory() == null)return;

        setStatus("DESTROYED");

        location.getBlock().setType(Material.AIR);
        location.getBlock().getRelative(0, -1, 0).setType(Material.AIR);


        Location l1 = location.clone().add(.5,0,.5), l2 = location.getBlock().getRelative(0, -1, 0).getLocation().clone().add(.5,0,.5);
        location.getWorld().playSound(l1, Sound.BLOCK_WOOD_BREAK,1,1);
        location.getWorld().spawnParticle(Particle.BLOCK_DUST, l1, 50, .5,.5,.5, Material.OAK_PLANKS.createBlockData());
        location.getWorld().playSound(l2, Sound.BLOCK_STONE_BREAK,1,1);
        location.getWorld().spawnParticle(Particle.BLOCK_DUST, l2, 50, .5,.5,.5, Material.CRYING_OBSIDIAN.createBlockData());

        double worth = Conf.judgeInventoryWorth(inventory);
        location.getWorld().createExplosion(location, (float) worth);

        DeathSignHandler.removeSign(this);
        File file = new File(Main.plugin.getDataFolder() + "/Inventories/", Util.locationToString(location) + ".yml");
        deleteSignFile(file);
    }
    public void spawn() {
        //Bukkit.broadcastMessage("saving to " + Util.locationToString(location));
        Block signBlock = location.getBlock();
        Block baseBlock = signBlock.getRelative(0,-1,0);
        String playername = Bukkit.getOfflinePlayer(owner).getName();
        Main.log("§c" + playername + "'s grave was spawned at " + signBlock.getX() + ", " + (signBlock.getY() + 1) + ", " + signBlock.getZ() + " in " + signBlock.getWorld().getName() + ".");

       /* player.sendMessage(Main.getPlugin().getConfig().getString("deathPrivateMessage")
                .replace("{x}",block.getX()+"")
                .replace("{y}",(block.getY()+1)+"")
                .replace("{z}",block.getZ()+"")
                .replace("{world}",block.getWorld().getName())
        );*/

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(Main.plugin.getConfig().getString("time_format"));
        LocalDateTime now = LocalDateTime.now();
        String dateTimeString = dtf.format(now);

        baseBlock.setType(Material.CRYING_OBSIDIAN);
        signBlock.setType(Material.OAK_SIGN);
        Sign sign = (Sign) signBlock.getState();
        sign.setLine(0,"§l§lR.I.P.");
        sign.setLine(1,playername);
        sign.setLine(3,dateTimeString);
        double rotation = 0;
        try {
            rotation = (Bukkit.getOfflinePlayer(owner).getPlayer().getLocation().getYaw() - 90) % 360;//taken from worldedit or something
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
        /*inventory = Bukkit.createInventory(null, 45, "§0§0" + getDeathMessage());
        inventory.setContents(loot);*/

        Bukkit.getScheduler().runTaskAsynchronously(Main.plugin, new Runnable(){
            @Override
            public void run() {
                saveItems();
            }
        });
        //DeathSignHandler.openSigns.add(dsign);
        //openSigns.put(signBlock.getLocation(),inv);

        Location l1 = sign.getLocation().clone().add(.5,0,.5), l2 = baseBlock.getLocation().clone().add(.5,0,.5);
        sign.getWorld().playSound(l1, Sound.BLOCK_WOOD_BREAK,1,1);
        sign.getWorld().spawnParticle(Particle.BLOCK_DUST, l1, 50, .5,.5,.5, Material.OAK_PLANKS.createBlockData());
        sign.getWorld().playSound(l2, Sound.BLOCK_STONE_BREAK,1,1);
        sign.getWorld().spawnParticle(Particle.BLOCK_DUST, l2, 50, .5,.5,.5, Material.CRYING_OBSIDIAN.createBlockData());
        //player.getWorld().spawnParticle(Particle.BLOCK_DUST, sign.getLocation(), 100, new MaterialData(Material.OAK_PLANKS));
        //player.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation(), 100, new MaterialData(Material.CRYING_OBSIDIAN));
    }
}
