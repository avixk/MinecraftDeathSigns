package me.avixk.DeathSigns;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Events implements Listener {
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event){
        event.getEntity().closeInventory();
        Main.getPlugin().getLogger().info("§c"+event.getEntity().getName() + " died at " + event.getEntity().getLocation().getBlockX() + ", " + event.getEntity().getLocation().getBlockY() + ", " + event.getEntity().getLocation().getBlockZ() + " in " + event.getEntity().getLocation().getWorld().getName());
        if(!Main.getPlugin().getConfig().getBoolean("enable_in_all_worlds") && !Main.getPlugin().getConfig().getStringList("enabled_worlds").contains(event.getEntity().getWorld().getName())) return;

        if (Main.disableSignPlayers.contains(event.getEntity().getName())) {
            Main.getPlugin().getLogger().info("§c" + event.getEntity().getName() + " has DeathSigns disabled, returning.");
            return;
        }
        if (event.getDrops().isEmpty()) {
            Main.getPlugin().getLogger().info("§c" + event.getEntity().getName() + "'s inventory is empty, no DeathSign will be placed.'");
            return;
        }

        Location deathLocation = event.getEntity().getLocation().clone();
        if (deathLocation.getY() < 3) deathLocation.setY(3);

        Block block = deathLocation.getBlock();
        //Bukkit.broadcastMessage(Main.locationToString(block.getLocation()));

        int maxDistance = 5;
        for (int currentDistance = 0; currentDistance <= maxDistance; currentDistance++) {
            for (int z = -(currentDistance); z <= currentDistance; z++) {// scan for a valid sign placement
                for (int x = -(currentDistance); x <= currentDistance; x++) {
                    for (int y = 0; y <= (currentDistance * 2); y++) {
                        Block nearbyBlock = block.getRelative(x, y, z);
                        if ((nearbyBlock.getType().equals(Material.AIR)
                                || nearbyBlock.getType().equals(Material.CAVE_AIR)
                                || nearbyBlock.getType().equals(Material.LAVA)
                                || nearbyBlock.getType().equals(Material.WATER))
                                && (nearbyBlock.getRelative(0, 1, 0).getType().equals(Material.AIR)
                                || nearbyBlock.getRelative(0, 1, 0).getType().equals(Material.CAVE_AIR)
                                || nearbyBlock.getRelative(0, 1, 0).getType().equals(Material.LAVA)
                                || nearbyBlock.getRelative(0, 1, 0).getType().equals(Material.WATER))) {
                            List<ItemStack> drops = new ArrayList<ItemStack>();
                            drops.addAll(event.getDrops());
                            for(ItemStack i : drops){
                                if(i.getType().name().contains("BANNER")){
                                    if(i.hasItemMeta())
                                        if(i.getItemMeta().hasDisplayName())
                                            if(i.getItemMeta().getDisplayName().contains("The Flag"))
                                                event.getEntity().getWorld().dropItem(event.getEntity().getEyeLocation(),i);
                                                event.getDrops().remove(i);
                                }
                            }
                            List<ItemStack> items = new ArrayList<>(event.getDrops());
                            Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), new Runnable() {
                                @Override
                                public void run() {
                                    Main.spawnDeathSign(nearbyBlock, event.getEntity(), items.toArray(new ItemStack[0]), "§0"+event.getDeathMessage());
                                }
                            },5);
                            event.getDrops().clear();

                            return;
                        }
                    }
                }
            }
        }
        Main.log("§cNo valid grave location found for " + event.getEntity().getName() + ", dropping items instead.");
        event.getEntity().sendMessage(Main.getPlugin().getConfig().getString("deathPrivateMessageNoGrave")
                .replace("{x}",block.getX()+"")
                .replace("{y}",block.getY()+"")
                .replace("{z}",block.getZ()+"")
                .replace("{world}",block.getWorld().getName())
        );
    }

    /*String[] ds_admin_commands = {"list","enable","disable","on","off","recover"};
    String[] ds_commands = {"list","enable","disable","on","off"};
    @EventHandler
    public void onTabComplete(TabCompleteEvent e){
        String[] buff = e.getBuffer().toLowerCase().trim().split(" ");
        if(buff[0].equalsIgnoreCase("/ds") || buff[0].equalsIgnoreCase("/deathsigns")){
            if(e.getSender().hasPermission("deathsigns.admin")){
                e.getCompletions().clear();
                for(String s : ds_admin_commands){
                    if(buff.length == 2 && s.startsWith(buff[1]))e.getCompletions().add(s);
                }
            }else{
                e.getCompletions().clear();
                for(String s : ds_commands){
                    if(buff.length == 2 && s.startsWith(buff[1]))e.getCompletions().add(s);
                }
            }
        }else if(e.getBuffer().equalsIgnoreCase("")){

        }
    }// fits("/ds en","enable")
    static HashMap<String,String[]> commands = new HashMap<>();
    public boolean fits(String buffer, String autofill){
        if(){

        }
        return false;
    }
    public static void registerTab(){
    }*/

    @EventHandler
    public void onBlockExplode(EntityExplodeEvent e) {
        //Bukkit.getLogger().info("5");
        for (Block b : new ArrayList<>(e.blockList())) {
            if (b.getType().equals(Material.OAK_SIGN)) {
                //Bukkit.getLogger().info("6");
                Sign sign = (Sign) b.getState();
                if (sign.getLine(0).equals("§lR.I.P.")) {
                    //Bukkit.getLogger().info("7");
                    e.blockList().remove(b);
                }
            }
        }

    }

    @EventHandler
    public void onBedExplode(BlockExplodeEvent e) {
       // Bukkit.getLogger().info("1");
        for (Block b : new ArrayList<>(e.blockList())) {
            //Bukkit.getLogger().info("1.1");
            if (b.getType().equals(Material.OAK_SIGN)) {
                //Bukkit.getLogger().info("2");
                Sign sign = (Sign) b.getState();
                if (sign.getLine(0).equals("§lR.I.P.")) {
                    //Bukkit.getLogger().info("3");
                    //e.setCancelled(true);
                    e.blockList().remove(b);
                    //Bukkit.getLogger().info("4 :D");
                }
            }
        }
    }

    static Set<String> bedrockDupeBandaid = new HashSet<String>();

    @EventHandler
    public void onPlayerBreakBlock(BlockBreakEvent e) {
        if (e.getBlock().getType().equals(Material.OAK_SIGN)) {
            Sign sign = (Sign) e.getBlock().getState();
            if (sign.getLine(0).equals("§lR.I.P.")) {
                if(!e.getBlock().getRelative(BlockFace.DOWN).getType().equals(Material.CRYING_OBSIDIAN))return;
                e.setCancelled(true);
            }
        } else if (e.getBlock().getType().equals(Material.CRYING_OBSIDIAN)) {
            if (e.getBlock().getRelative(0, 1, 0).getType().equals(Material.OAK_SIGN)) {
                Sign sign = (Sign) e.getBlock().getRelative(0, 1, 0).getState();
                if (sign.getLine(0).equals("§lR.I.P."))
                    e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getPlayer().isSneaking()) return;
        if (event.getClickedBlock().getType().equals(Material.OAK_SIGN)) {
            if(!event.getClickedBlock().getRelative(BlockFace.DOWN).getType().equals(Material.CRYING_OBSIDIAN))return;
            Sign sign = (Sign) event.getClickedBlock().getState();
            if (!sign.getLine(0).equals("§lR.I.P.")) return;
            if (!event.getPlayer().getGameMode().equals(GameMode.SPECTATOR)
                    && !sign.getLine(1).equals(event.getPlayer().getName())
                    && Conf.signExistsInConfig(sign.getLocation())) {
                //if game mode isnt spec and the names don't match and sign exists, no items for u
                //THIS IS NOT YOUR SIGN FOOL
                long signTime = Conf.getSignTime(sign.getLocation());
                int timeout = Main.getPlugin().getConfig().getInt("signTimeoutSeconds") * 1000;
                if(System.currentTimeMillis() < (signTime + timeout)){
                    if(event.getPlayer().hasPermission("deathsigns.admin")){
                        event.getPlayer().sendMessage("Overriding protection due to admin permission.");
                    }else{

                        long expiresin = (signTime + timeout) - System.currentTimeMillis();
                        event.getPlayer().sendMessage(Main.getPlugin().getConfig().getString("notYourSignMessage")
                                .replace("{time}",Conf.millisToHumanString(expiresin,true))
                                .replace("{sign_owner}", sign.getLine(1))
                        );
                        return;
                    }
                }
            }
            //patch bedrock event spam
            if (bedrockDupeBandaid.contains(event.getPlayer().getName())) return;
            bedrockDupeBandaid.add(event.getPlayer().getName());
            if(Main.openSigns.containsKey(sign.getLocation())){
                if(currentlySaving.contains(sign.getLocation())){
                    bedrockDupeBandaid.remove(event.getPlayer().getName());
                    return;
                }else{
                    event.getPlayer().openInventory(Main.openSigns.get(sign.getLocation()));
                    bedrockDupeBandaid.remove(event.getPlayer().getName());
                }
            }else{
                Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), new Runnable() {
                    @Override
                    public void run() {
                        Map.Entry<ItemStack[], String> items = Main.recallItems(sign.getLocation(),false);
                        String title = items.getValue();
                        if(title == null)title="Unknown DeathSign";
                        Inventory inv = Bukkit.createInventory(null,45, title);
                        Main.openSigns.put(sign.getLocation(),inv);
                        Bukkit.getScheduler().runTask(Main.getPlugin(), new Runnable() {
                            @Override
                            public void run() {
                                if(!Conf.signExistsInConfig(sign.getLocation()) || items == null ||
                                        !Main.getPlugin().getConfig().getBoolean("use_sign_inventory")){
                                    if(event.getPlayer().getGameMode().equals(GameMode.SPECTATOR))return;
                                    breakSign(event.getClickedBlock(),items.getKey());
                                    bedrockDupeBandaid.remove(event.getPlayer().getName());
                                    return;
                                }
                                inv.setContents(items.getKey());
                                event.getPlayer().openInventory(inv);
                                bedrockDupeBandaid.remove(event.getPlayer().getName());
                            }
                        });
                    }
                });
            }
            if(event.getPlayer().hasPermission("deathsigns.admin") && !sign.getLine(1).equals(event.getPlayer().getName())) {
                return;
            }
            if(event.getPlayer().getGameMode().equals(GameMode.SPECTATOR))return;
            if(!Conf.signExistsInConfig(sign.getLocation()))return;
            String status = Conf.getStatus(sign.getLocation());
            if(status.contains("PROTECTED")){
                if(Conf.signBelongsTo(sign.getLocation(),event.getPlayer().getUniqueId())){
                    Main.log("§8[§cDeathSigns Admin§8]§c " + event.getPlayer().getName()
                            + " reclaimed their DeathSign at " + sign.getX() + ", " + sign.getY() + ", "
                            + sign.getZ() + " in " + sign.getWorld().getName() + ".");
                    Conf.setStatus(sign.getLocation(),"OPENED_OWNER");
                }else{
                    Main.log("§8[§cDeathSigns Admin§8]§c " + event.getPlayer().getName()
                            + " claimed " + sign.getLine(1) + "'s DeathSign at " + sign.getX() + ", "
                            + sign.getY() + ", " + sign.getZ() + " in " + sign.getWorld().getName() + ".");
                    Conf.setStatus(sign.getLocation(),"OPENED_OTHER");
                }
            }
        }
    }
    static List<Location> currentlySaving = new ArrayList<>();
    @EventHandler
    public void onInvClose(InventoryCloseEvent e){
        if(e.getPlayer().getGameMode().equals(GameMode.SPECTATOR))return;
        if(bedrockDupeBandaid.contains(e.getPlayer().getName()))return;
        //if(e.getView().getTitle().endsWith("'s DeathSign")){

            if(Main.openSigns.containsValue(e.getInventory())){
                if(e.getViewers().size() <= 1){
                    for(Map.Entry<Location,Inventory> es : Main.openSigns.entrySet()){
                        if(es.getValue().equals(e.getInventory())){
                            for(ItemStack item : e.getInventory().getContents().clone()){
                                if(item != null){
                                    saveSign(es.getKey(),e.getInventory().getContents().clone(),e.getView().getTitle());
                                    return;
                                }
                            }
                            //if(Main.openSigns.containsKey(es.getKey()))Main.openSigns.remove(es.getKey());
                            breakSign(es.getKey().getBlock(),e.getInventory().getContents());
                            return;
                        }
                    }
                }
            }
        //}
    }
    @EventHandler
    public void onInvClick(InventoryClickEvent e){
        if(Main.openSigns.containsValue(e.getInventory())){
            if(e.getWhoClicked().getGameMode().equals(GameMode.SPECTATOR)){
                e.setCancelled(true);
            }
        }
    }
    public static void saveSign(Location loc, ItemStack[] items, String title){
        currentlySaving.add(loc);
        Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), new Runnable() {
            @Override
            public void run() {
                try {
                    Main.saveItems(loc, items, title);
                    currentlySaving.remove(loc);
                    if(Main.openSigns.containsKey(loc))Main.openSigns.remove(loc);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    public void breakSign(Block sign, ItemStack[] items){
        if(Conf.signExistsInConfig(sign.getLocation())){
            if(Conf.getStatus(sign.getLocation()).equals("OPENED_OWNER")){
                Conf.setStatus(sign.getLocation(),"RECOVERED");
            }else{
                Conf.setStatus(sign.getLocation(),"TAKEN");
            }
        }
        sign.setType(Material.AIR);
        sign.getRelative(0, -1, 0).setType(Material.AIR);
        Location l1 = sign.getLocation().clone().add(.5,0,.5), l2 = sign.getLocation().clone().add(.5,-1,.5);
        sign.getWorld().playSound(l1, Sound.BLOCK_WOOD_BREAK,1,1);
        sign.getWorld().spawnParticle(Particle.BLOCK_DUST, l1, 50, .5,.5,.5, Material.OAK_PLANKS.createBlockData());
        sign.getWorld().playSound(l2, Sound.BLOCK_STONE_BREAK,1,1);
        sign.getWorld().spawnParticle(Particle.BLOCK_DUST, l2, 50, .5,.5,.5, Material.CRYING_OBSIDIAN.createBlockData());
        for (ItemStack i : items) {
            if (i != null) sign.getWorld().dropItem(sign.getLocation().add(0.5,0.5,0.5), i);
        }
        if(Main.openSigns.containsKey(sign.getLocation()))Main.openSigns.remove(sign.getLocation());
        Main.deleteSignFile(sign.getLocation());
    }
}

