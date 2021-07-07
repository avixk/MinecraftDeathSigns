package me.avixk.DeathSigns;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.tags.CustomItemTagContainer;
import org.bukkit.inventory.meta.tags.ItemTagType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.util.*;

public class Events implements Listener {
    public static List<Location> deathsignQueue = new ArrayList<>();
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        //long time = System.currentTimeMillis();
        Location loc = event.getEntity().getLocation();
        if (Main.plugin.getConfig().getBoolean("log_death_locations_to_console"))
            Main.getPlugin().getLogger().info("§c" + event.getEntity().getName() + " died at " + event.getEntity().getLocation().getBlockX() + ", " + event.getEntity().getLocation().getBlockY() + ", " + event.getEntity().getLocation().getBlockZ() + " in " + event.getEntity().getLocation().getWorld().getName());

        if (Main.plugin.getConfig().getBoolean("deathsigns_require_permission") &&
                !event.getEntity().hasPermission("deathsigns.deathsign")) return;

        if (!Main.getPlugin().getConfig().getBoolean("enable_in_all_worlds") && !Main.getPlugin().getConfig().getStringList("enabled_worlds").contains(event.getEntity().getWorld().getName())) {
            Main.getPlugin().getLogger().info("DeathSigns are disabled in this world.");
            return;
        }

        if (Main.disableSignPlayers.contains(event.getEntity().getName())) {
            Main.getPlugin().getLogger().info("§c" + event.getEntity().getName() + " has DeathSigns disabled, returning.");
            return;
        }

        if(event.getEntity().getKiller() != null &&
                Main.plugin.getConfig().getBoolean("bounties_disable_deathsigns") &&
                BountyHook.willClaimBounty(event.getEntity().getKiller(),event.getEntity())){

            Bukkit.getLogger().info("A DeathSign did not spawn for " + event.getEntity().getName() + " because they had a bounty.");
            event.getEntity().sendMessage("§cYour DeathSign was not placed because you had a bounty.");
            return;
        }

        event.getEntity().closeInventory();

        List<ItemStack> drops = new ArrayList<ItemStack>();
        drops.addAll(event.getDrops());
        for (ItemStack i : new ArrayList<>(drops)) {
            if (i.getType().name().contains("BANNER")) {
                if (i.hasItemMeta())
                    if (i.getItemMeta().hasDisplayName())
                        if (i.getItemMeta().getDisplayName().contains("The Flag")){
                            // If a banner has "The Flag" in the name, it will fall out of deathsigns :)
                            event.getEntity().getWorld().dropItem(event.getEntity().getEyeLocation(), i);
                            drops.remove(i);
                        }
            }else if(i.getType().equals(Material.COMPASS)){
                CompassMeta meta = (CompassMeta) i.getItemMeta();
                if (meta != null && meta.hasLodestone()) {
                    CustomItemTagContainer tagContainer = meta.getCustomTagContainer();
                    if (tagContainer.hasCustomTag(DeathSignHandler.key, ItemTagType.STRING)) {
                        drops.remove(i);
                    }
                }
            }
        }

        if (drops.isEmpty()) {
            event.getDrops().clear();
            Main.getPlugin().getLogger().info("§c" + event.getEntity().getName() + "'s inventory is empty, no DeathSign will be placed.'");
            return;
        }

        Location deathLocation = event.getEntity().getLocation().clone();
        if (deathLocation.getY() < 1) deathLocation.setY(1);
        if (deathLocation.getY() > 254) deathLocation.setY(250);

        Block block = deathLocation.getBlock();
        //Bukkit.broadcastMessage(Main.locationToString(block.getLocation()));

        Block spawnBlock = null;
        List<Block> checkedBlocks = new ArrayList<>();
        spawnBlock = DeathSignHandler.searchForSignPlacement(block, checkedBlocks,5);
        if(spawnBlock == null){
            spawnBlock = DeathSignHandler.searchForSignPlacement(block, checkedBlocks,20);
        }
        //Bukkit.broadcastMessage("DeathSign placement search took " + (System.currentTimeMillis() - time) + "ms" );
        if(spawnBlock != null){
            Location spawnLocation = spawnBlock.getLocation();
            if (Conf.deathsign_cost_enabled && Econ.set_up) {
                int freesigns = Conf.useFreeSign(event.getEntity().getUniqueId());
                if (freesigns == -1) {
                    if (Econ.getPlayerMoney(event.getEntity().getUniqueId()) < Conf.deathsign_cost) {
                        Main.getPlugin().getLogger().info(event.getEntity().getName() + " has no free deathsigns and cannot afford to buy one :(");
                        event.getEntity().sendMessage(Main.plugin.getConfig().getString("death_private_message.cost.not_enough_money")
                                .replace("{x}", spawnLocation.getBlockX() + "")
                                .replace("{y}", spawnLocation.getBlockY() + "")
                                .replace("{z}", spawnLocation.getBlockZ() + "")
                                .replace("{world}", spawnLocation.getWorld().getName() + "")
                                .replace("{price}", Conf.deathsign_cost + "")
                                .replace("{free}", freesigns + "")
                                .replace("&", "§")
                        );
                        return;
                    } else {
                        if (!Econ.takePlayerMoney(event.getEntity().getUniqueId(), Conf.deathsign_cost)) {
                            Main.getPlugin().getLogger().info(event.getEntity().getName() + "'s deathsign payment transaction failed..? Cost: "
                                    + Conf.deathsign_cost + " Balance: " + Econ.getPlayerMoney(event.getEntity().getUniqueId()));
                            event.getEntity().sendMessage(Main.plugin.getConfig().getString("death_private_message.couldnt_spawn")
                                    .replace("{x}", spawnLocation.getBlockX() + "")
                                    .replace("{y}", spawnLocation.getBlockY() + "")
                                    .replace("{z}", spawnLocation.getBlockZ() + "")
                                    .replace("{world}", spawnLocation.getWorld().getName() + "")
                                    .replace("{price}", Conf.deathsign_cost + "")
                                    .replace("{free}", freesigns + "")
                                    .replace("&", "§")
                            );
                            return;
                        } else {
                            Main.getPlugin().getLogger().info(event.getEntity().getName() + " paid $" + Conf.deathsign_cost + " for a deathsign.");
                            event.getEntity().sendMessage(Main.plugin.getConfig().getString("death_private_message.cost.success")
                                    .replace("{x}", spawnLocation.getBlockX() + "")
                                    .replace("{y}", spawnLocation.getBlockY() + "")
                                    .replace("{z}", spawnLocation.getBlockZ() + "")
                                    .replace("{world}", spawnLocation.getWorld().getName() + "")
                                    .replace("{price}", Conf.deathsign_cost + "")
                                    .replace("{free}", freesigns + "")
                                    .replace("&", "§")
                            );
                        }
                    }
                } else {
                    Main.getPlugin().getLogger().info(event.getEntity().getName() + " used a free deathsign, they have " + freesigns + " remaining.");
                    if (freesigns == 0) {
                        event.getEntity().sendMessage(Main.plugin.getConfig().getString("death_private_message.cost.used_last_free")
                                .replace("{x}", spawnLocation.getBlockX() + "")
                                .replace("{y}", spawnLocation.getBlockY() + "")
                                .replace("{z}", spawnLocation.getBlockZ() + "")
                                .replace("{world}", spawnLocation.getWorld().getName() + "")
                                .replace("{price}", Conf.deathsign_cost + "")
                                .replace("{free}", freesigns + "")
                                .replace("&", "§")
                        );
                    } else {
                        event.getEntity().sendMessage(Main.plugin.getConfig().getString("death_private_message.cost.used_free")
                                .replace("{x}", spawnLocation.getBlockX() + "")
                                .replace("{y}", spawnLocation.getBlockY() + "")
                                .replace("{z}", spawnLocation.getBlockZ() + "")
                                .replace("{world}", spawnLocation.getWorld().getName() + "")
                                .replace("{price}", Conf.deathsign_cost + "")
                                .replace("{free}", freesigns + "")
                                .replace("&", "§")
                        );
                    }
                }

            } else {
                event.getEntity().sendMessage(Main.plugin.getConfig().getString("death_private_message.no_cost")
                        .replace("{x}", spawnLocation.getBlockX() + "")
                        .replace("{y}", spawnLocation.getBlockY() + "")
                        .replace("{z}", spawnLocation.getBlockZ() + "")
                        .replace("{world}", spawnLocation.getWorld().getName() + "")
                        .replace("&", "§")
                );
            }

            Location finalSpawnLocation = spawnLocation;
            Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    DeathSign sign = DeathSignHandler.createDeathSign((ItemStack[]) drops.toArray(new ItemStack[drops.size()]), event.getEntity().getUniqueId(), finalSpawnLocation,event.getDeathMessage()==null?"Questionable DeathSign":event.getDeathMessage(),false);
                    sign.spawn();
                    recentDeathSigns.put(event.getEntity().getUniqueId(),sign);
                    //Main.spawnDeathSign(nearbyBlock, event.getEntity(), items.toArray(new ItemStack[0]), "§0" + event.getDeathMessage());
                }
            }, 5);
            event.getDrops().clear();
        }else{
            Main.log("§cNo valid grave location found for " + event.getEntity().getName() + ", dropping items instead.");
            event.getEntity().sendMessage(Main.plugin.getConfig().getString("death_private_message.couldnt_spawn")
                    .replace("{x}", loc.getBlockX() + "")
                    .replace("{y}", loc.getBlockY() + "")
                    .replace("{z}", loc.getBlockZ() + "")
                    .replace("{world}", loc.getWorld().getName() + "")
                    .replace("{price}", Conf.deathsign_cost + "")
                    .replace("&", "§")
            );
        }

    }

    public static boolean compass;

    @EventHandler
    public void onTab(TabCompleteEvent e) {
        if (e.getBuffer().toLowerCase().startsWith("/deathsigns") || e.getBuffer().toLowerCase().startsWith("/ds")) {// 0      1     2    3   4   5   6
            /*if(!e.getSender().hasPermission("headhunting.bounty")){
                e.setCompletions(new ArrayList<>());
                return;
            }      */                                  //   /bounty g
            List<String> ds_commands = new ArrayList<>(Arrays.asList("list", "compass", "on", "off", "destroy"));
            boolean admin = e.getSender().hasPermission("deathsigns.admin");
            if (admin) {
                ds_commands.add("recover");
            }
            if (compass) {
                ds_commands.add("compass");
            }
            //String[] commands = admin?ds_admin_commands:ds_commands;
            String[] buf = e.getBuffer().split(" ");//   /bounty place avixk 100 you lil fag
            int length = buf.length + (e.getBuffer().endsWith(" ") ? 1 : 0);
            String relevantString = e.getBuffer().endsWith(" ")?"":buf[buf.length - 1];
            if (buf.length == 1) {
                e.setCompletions(ds_commands);
                return;
            }
            if (length == 2) {
                final List<String> completions = new ArrayList<>();
                //copy matches of first argument from list (ex: if first arg is 'm' will return just 'minecraft')
                StringUtil.copyPartialMatches(relevantString, ds_commands, completions);
                //sort the list
                //Collections.sort(completions);
                e.setCompletions(completions);
            } else if (length == 3) {//ds list avixk 5
                if (buf[1].equalsIgnoreCase("list")) {
                    if (admin) {
                        if(Cooldown.getTimeRemaining("list." + e.getSender().getName())>0){
                            e.setCancelled(true);
                            return;
                        }
                        new Cooldown("list." + e.getSender().getName(),100).start();
                        List<String> comp = new ArrayList<>();
                        List<String> completions = new ArrayList<>();

                        if(e.getSender() instanceof Player){
                            int deathsigns = Conf.storage.getConfig().getConfigurationSection("player." + ((Player) e.getSender()).getUniqueId() + ".deaths").getKeys(false).size();

                            int max = Main.getPlugin().getConfig().getInt("list_page_size");
                            int pages = (deathsigns / max) + 1;

                            for (int i = 1; i <= pages; i++) {
                                comp.add(i+"");
                            }
                        }

                        StringUtil.copyPartialMatches(relevantString, comp, completions);
                        completions.addAll(Util.playerNamesOrOfflinePlayerNames(relevantString));
                        e.setCompletions(completions);
                    } else {
                        List<String> comp = new ArrayList<>();
                        List<String> completions = new ArrayList<>();

                        if(e.getSender() instanceof Player){
                            int deathsigns = Conf.storage.getConfig().getConfigurationSection("player." + ((Player) e.getSender()).getUniqueId() + ".deaths").getKeys(false).size();

                            int max = Main.getPlugin().getConfig().getInt("list_page_size");
                            int pages = (deathsigns / max) + 1;

                            for (int i = 0; i < pages; i++) {
                                comp.add(i+"");
                            }
                        }

                        StringUtil.copyPartialMatches(relevantString, comp, completions);
                        e.setCompletions(completions);
                    }
                } else if (buf[1].equalsIgnoreCase("compass")) {
                    if (admin) {
                        if(Cooldown.getTimeRemaining("compass." + e.getSender().getName())>0){
                            e.setCancelled(true);
                            return;
                        }
                        new Cooldown("compass." + e.getSender().getName(),100).start();
                        List<String> comp = new ArrayList<>();
                        List<String> completions = new ArrayList<>();
                        if(e.getSender()instanceof Player){
                            for(String s : Conf.storage.getConfig().getConfigurationSection("player." + ((Player)e.getSender()).getUniqueId() + ".deaths").getKeys(false)){
                                String status = Conf.storage.getConfig().getString("player." + ((Player)e.getSender()).getUniqueId() + ".deaths." + s + ".status");
                                if(!status.equals("RECOVERED") && !status.equals("TAKEN") && !status.equals("DESTROYED"))
                                    comp.add(s);
                            }
                        }
                        StringUtil.copyPartialMatches(relevantString, comp, completions);
                        completions.addAll(Util.playerNamesOrOfflinePlayerNames(relevantString));
                        e.setCompletions(completions);
                    } else {
                        List<String> comp = new ArrayList<>();
                        List<String> completions = new ArrayList<>();
                        if(e.getSender()instanceof Player){
                            for(String s : Conf.storage.getConfig().getConfigurationSection("player." + ((Player)e.getSender()).getUniqueId() + ".deaths").getKeys(false)){
                                String status = Conf.storage.getConfig().getString("player." + ((Player)e.getSender()).getUniqueId() + ".deaths." + s + ".status");
                                if(!status.equals("RECOVERED") && !status.equals("TAKEN") && !status.equals("DESTROYED"))
                                    comp.add(s);
                            }
                        }
                        StringUtil.copyPartialMatches(relevantString, comp, completions);
                        e.setCompletions(completions);
                    }
                }else if (buf[1].equalsIgnoreCase("destroy") && (!Main.plugin.getConfig().getBoolean("destroy_command.require_permission") || e.getSender().hasPermission("deathsigns.destroy"))) {
                    if (admin) {
                        if(Cooldown.getTimeRemaining("destroy." + e.getSender().getName())>0){
                            e.setCancelled(true);
                            return;
                        }
                        new Cooldown("destroy." + e.getSender().getName(),100).start();
                        List<String> comp = new ArrayList<>();
                        List<String> completions = new ArrayList<>();
                        if(e.getSender()instanceof Player){
                            for(String s : Conf.storage.getConfig().getConfigurationSection("player." + ((Player)e.getSender()).getUniqueId() + ".deaths").getKeys(false)){
                                String status = Conf.storage.getConfig().getString("player." + ((Player)e.getSender()).getUniqueId() + ".deaths." + s + ".status");
                                if(!status.equals("RECOVERED") && !status.equals("TAKEN") && !status.equals("DESTROYED"))
                                    comp.add(s);
                            }
                        }
                        StringUtil.copyPartialMatches(relevantString, comp, completions);
                        completions.addAll(Util.playerNamesOrOfflinePlayerNames(relevantString));
                        e.setCompletions(completions);
                    } else {
                        List<String> comp = new ArrayList<>();
                        List<String> completions = new ArrayList<>();
                        if(e.getSender()instanceof Player){
                            for(String s : Conf.storage.getConfig().getConfigurationSection("player." + ((Player)e.getSender()).getUniqueId() + ".deaths").getKeys(false)){
                                String status = Conf.storage.getConfig().getString("player." + ((Player)e.getSender()).getUniqueId() + ".deaths." + s + ".status");
                                if(!status.equals("RECOVERED") && !status.equals("TAKEN") && !status.equals("DESTROYED"))
                                    comp.add(s);
                            }
                        }
                        StringUtil.copyPartialMatches(relevantString, comp, completions);
                        e.setCompletions(completions);
                    }
                } else if (buf[1].equalsIgnoreCase("on") || buf[1].equalsIgnoreCase("off") || buf[1].equalsIgnoreCase("enable") || buf[1].equalsIgnoreCase("disable")) {
                    if (!admin) e.setCompletions(new ArrayList<>());
                }
            } else if (length == 4) {//ds list avixk 5
                if (buf[1].equalsIgnoreCase("list")) {
                    if (admin) {
                        if(Cooldown.getTimeRemaining("list." + e.getSender().getName())>0){
                            e.setCancelled(true);
                            return;
                        }
                        new Cooldown("list." + e.getSender().getName(),100).start();
                        List<String> comp = new ArrayList<>();
                        List<String> completions = new ArrayList<>();
                        if(e.getSender() instanceof Player){
                            int deathsigns = Conf.storage.getConfig().getConfigurationSection("player." + ((Player) e.getSender()).getUniqueId() + ".deaths").getKeys(false).size();

                            int max = Main.getPlugin().getConfig().getInt("list_page_size");
                            int pages = (deathsigns / max) + 1;

                            for (int i = 1; i <= pages; i++) {
                                comp.add(i+"");
                            }
                        }
                        StringUtil.copyPartialMatches(relevantString, comp, completions);
                        e.setCompletions(completions);
                    } else {
                        e.setCompletions(new ArrayList<>());
                    }
                } else if (buf[1].equalsIgnoreCase("compass")) {
                    if (admin) {
                        if(Cooldown.getTimeRemaining("compass." + e.getSender().getName())>0){
                            e.setCancelled(true);
                            return;
                        }
                        new Cooldown("compass." + e.getSender().getName(),100).start();
                        UUID target = Bukkit.getOfflinePlayer(buf[2]).getUniqueId();
                        List<String> comp = new ArrayList<>();
                        List<String> completions = new ArrayList<>();
                        if(e.getSender()instanceof Player){
                            if(Conf.storage.getConfig().contains("player." + target + ".deaths")){
                                for(String s : Conf.storage.getConfig().getConfigurationSection("player." + target + ".deaths").getKeys(false)){
                                    String status = Conf.storage.getConfig().getString("player." + target + ".deaths." + s + ".status");
                                    if(!status.equals("RECOVERED") && !status.equals("TAKEN") && !status.equals("DESTROYED"))
                                        comp.add(s);
                                }
                            }
                        }
                        StringUtil.copyPartialMatches(relevantString, comp, completions);
                        //completions.addAll(Util.playerNamesOrOfflinePlayerNames(relevantString));
                        e.setCompletions(completions);
                    }  else {
                        e.setCompletions(new ArrayList<>());
                    }/*else {
                        List<String> comp = new ArrayList<>();
                        List<String> completions = new ArrayList<>();
                        if(e.getSender()instanceof Player){
                            if(Conf.storage.getConfig().contains("player." + ((Player)e.getSender()).getUniqueId() + ".deaths")){
                                for(String s : Conf.storage.getConfig().getConfigurationSection("player." + ((Player)e.getSender()).getUniqueId() + ".deaths").getKeys(false)){
                                    String status = Conf.storage.getConfig().getString("player." + ((Player)e.getSender()).getUniqueId() + ".deaths." + s + ".status");
                                    if(!status.equals("RECOVERED") && !status.equals("TAKEN") && !status.equals("DESTROYED"))comp.add(s);
                                    comp.add(s);
                                }
                            }
                        }
                        StringUtil.copyPartialMatches(relevantString, comp, completions);
                        e.setCompletions(completions);
                    }*/
                } else if (buf[1].equalsIgnoreCase("destroy") && (!Main.plugin.getConfig().getBoolean("destroy_command.require_permission") || e.getSender().hasPermission("deathsigns.destroy"))) {
                    if (admin) {
                        if(Cooldown.getTimeRemaining("destroy." + e.getSender().getName())>0){
                            e.setCancelled(true);
                            return;
                        }
                        new Cooldown("destroy." + e.getSender().getName(),100).start();
                        UUID target = Bukkit.getOfflinePlayer(buf[2]).getUniqueId();
                        List<String> comp = new ArrayList<>();
                        List<String> completions = new ArrayList<>();
                        if(e.getSender()instanceof Player){
                            if(Conf.storage.getConfig().contains("player." + target + ".deaths")){
                                for(String s : Conf.storage.getConfig().getConfigurationSection("player." + target + ".deaths").getKeys(false)){
                                    String status = Conf.storage.getConfig().getString("player." + target + ".deaths." + s + ".status");
                                    if(!status.equals("RECOVERED") && !status.equals("TAKEN") && !status.equals("DESTROYED"))
                                        comp.add(s);
                                }
                            }
                        }
                        StringUtil.copyPartialMatches(relevantString, comp, completions);
                        //completions.addAll(Util.playerNamesOrOfflinePlayerNames(relevantString));
                        e.setCompletions(completions);
                    }  else {
                        e.setCompletions(new ArrayList<>());
                    }
                }else {
                    e.setCompletions(new ArrayList<>());
                }
            } else {
                e.setCompletions(new ArrayList<>());
            }
        }
    }

    @EventHandler
    public void onBlockExplode(EntityExplodeEvent e) {
        //Bukkit.getLogger().info("5");
        for (Block b : new ArrayList<>(e.blockList())) {
            if (b.getType().equals(Material.OAK_SIGN)) {
                //Bukkit.getLogger().info("6");
                Sign sign = (Sign) b.getState();
                if (sign.getLine(0).equals("§lR.I.P.") || sign.getLine(0).equals("§0§lR.I.P.")) {
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
                if (sign.getLine(0).equals("§lR.I.P.") || sign.getLine(0).equals("§0§lR.I.P.")) {
                    //Bukkit.getLogger().info("3");
                    //e.setCancelled(true);
                    e.blockList().remove(b);
                    //Bukkit.getLogger().info("4 :D");
                }
            }
        }
    }

    static Set<Block> bedrockDupeBandaid = new HashSet<Block>();

    @EventHandler
    public void onPlayerBreakBlock(BlockBreakEvent e) {
        if (e.getBlock().getType().equals(Material.OAK_SIGN)) {
            Sign sign = (Sign) e.getBlock().getState();
            if (sign.getLine(0).equals("§lR.I.P.") || sign.getLine(0).equals("§0§lR.I.P.")) {
                if (!e.getBlock().getRelative(BlockFace.DOWN).getType().equals(Material.CRYING_OBSIDIAN)) return;
                e.setCancelled(true);
            }
        } else if (e.getBlock().getType().equals(Material.CRYING_OBSIDIAN)) {
            if (e.getBlock().getRelative(0, 1, 0).getType().equals(Material.OAK_SIGN)) {
                Sign sign = (Sign) e.getBlock().getRelative(0, 1, 0).getState();
                if (sign.getLine(0).equals("§lR.I.P.") || sign.getLine(0).equals("§0§lR.I.P."))
                    e.setCancelled(true);
            }
        }
    }

    static List<Player> compass_click_cooldown = new ArrayList<>();

    @EventHandler
    public void onCompassClick(PlayerInteractEvent e) {
        ItemStack item = null;
        if (e.getHand() == null) return;
        if (e.getHand().equals(EquipmentSlot.HAND)) item = e.getPlayer().getInventory().getItemInMainHand();
        if (e.getHand().equals(EquipmentSlot.OFF_HAND)) item = e.getPlayer().getInventory().getItemInMainHand();
        if (item != null && /*!e.isCancelled() &&*/ item.getType().equals(Material.COMPASS)) {
            if (item.hasItemMeta() && item.getItemMeta() instanceof CompassMeta) {
                CompassMeta meta = (CompassMeta) item.getItemMeta();
                if (meta.hasLodestone()) {
                    if (compass_click_cooldown.contains(e.getPlayer())) return;
                    compass_click_cooldown.add(e.getPlayer());
                    Bukkit.getScheduler().scheduleSyncDelayedTask(Main.plugin, new Runnable() {
                        @Override
                        public void run() {
                            compass_click_cooldown.remove(e.getPlayer());
                        }
                    }, Main.plugin.getConfig().getInt("death_compass.click_cooldown_ticks"));
                    e.getPlayer().setCooldown(Material.COMPASS,Main.plugin.getConfig().getInt("death_compass.click_cooldown_ticks"));
                    NamespacedKey key = new NamespacedKey(Main.getPlugin(), "deathsigns.compass");
                    CustomItemTagContainer tagContainer = meta.getCustomTagContainer();
                    if (tagContainer.hasCustomTag(key, ItemTagType.STRING)) {
                        String foundValue = tagContainer.getCustomTag(key, ItemTagType.STRING);
                        DeathSign deathSign = DeathSignHandler.getDeathSignFromIdentifier(foundValue);
                        if(deathSign != null && !deathSign.getStatus().equals("RECOVERED") && !deathSign.getStatus().equals("TAKEN") && !deathSign.getStatus().equals("DESTROYED")){
                            if (!deathSign.getLocation().getWorld().equals(e.getPlayer().getWorld())) {
                                e.getPlayer().sendMessage("§cThis deathsign is in another world.");
                                return;
                            }
                            int distance = (int) e.getPlayer().getLocation().distance(deathSign.getLocation());
                            String color = "§";
                            if (distance < 20) {
                                color += "b";
                            } else if (distance < 50) {
                                color += "a";
                            } else if (distance < 100) {
                                color += "e";
                            } else if (distance < 500) {
                                color += "6";
                            } else if (distance < 1000) {
                                color += "c";
                            } else {
                                color += "4";
                            }
                            e.getPlayer().sendMessage(Main.plugin.getConfig().getString("death_compass.click_text")
                                    .replace("&", "§")
                                    .replace("{distance}", "" + distance)
                                    .replace("{distance_color}", color + distance));
                            if(Main.plugin.getConfig().getBoolean("death_compass.click_beam_effect")){
                                final boolean[] bong = {false};
                                Location location = deathSign.getLocation().clone().add(0.5,0,0.5);
                                final int[] y = {0};
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        y[0] += 5;
                                        location.setY(y[0]);
                                        if((deathSign.getLocation().getY() - 5) < y[0] && !bong[0]){
                                            bong[0] = true;
                                            deathSign.getLocation().getWorld().playSound(deathSign.getLocation(),Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 2F,0.7F);
                                        }
                                        try {
                                            location.getWorld().spawnParticle(Particle.END_ROD, location, 5,0,4,0, 0.05 ,null, true);
                                        }catch (Exception e){
                                            Bukkit.getLogger().warning("Particle Error");
                                            e.printStackTrace();
                                            cancel();
                                        }
                                        if(y[0] > 254){
                                            cancel();
                                        }
                                    }
                                }.runTaskTimer(Main.plugin,1,1);
                            }
                            return;
                        }
                        boolean compass = DeathSignHandler.breakIfCompass(e.getPlayer().getEyeLocation(),e.getItem());
                        if(compass){
                            e.setCancelled(true);
                            e.getPlayer().getInventory().remove(e.getItem());
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e){
        boolean compass = DeathSignHandler.breakIfCompass(e.getPlayer().getEyeLocation(),e.getItemDrop().getItemStack());
        if(compass){
            e.getItemDrop().remove();
        }
    }

    @EventHandler
    public void onPickUp(EntityPickupItemEvent e){
        if(!(e.getEntity()instanceof Player))return;
        boolean compass = DeathSignHandler.breakIfCompass(((Player) e.getEntity()).getEyeLocation(),e.getItem().getItemStack());
        if(compass){
            e.setCancelled(true);
            e.getItem().remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e){
        if(e.getClickedInventory() == null || e.isCancelled()) return;;
        ItemStack clicked = e.getClickedInventory().getItem(e.getSlot());
        if(clicked == null)return;
        boolean compass = DeathSignHandler.breakIfCompass(e.getWhoClicked().getEyeLocation(),clicked);
        if(compass){
            e.setCancelled(true);
            e.setResult(Event.Result.DENY);
            e.getClickedInventory().remove(clicked);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getPlayer().isSneaking()) return;
        if (event.getClickedBlock().getType().equals(Material.OAK_SIGN)) {
            //Bukkit.broadcastMessage("1");
            if (!event.getClickedBlock().getRelative(BlockFace.DOWN).getType().equals(Material.CRYING_OBSIDIAN)) return;
            Sign sign = (Sign) event.getClickedBlock().getState();
            if(sign.getLine(0).equals("§0§lR.I.P.")){
                event.getPlayer().sendMessage("§cThis sign is protected §eforever§c.");
                return;
            }else if (!sign.getLine(0).equals("§lR.I.P.")) return;

            Block b = event.getPlayer().getTargetBlockExact(10,FluidCollisionMode.NEVER);
            if(b == null || !b.getType().equals(event.getClickedBlock().getType()))return;
            DeathSign deathSign = DeathSignHandler.getDeathSignAtLoc(event.getClickedBlock().getLocation());
            //Bukkit.broadcastMessage("2");
            if(deathSign == null){
                File file = new File(Main.plugin.getDataFolder() + "/Inventories/", Util.locationToString(event.getClickedBlock().getLocation()) + ".yml");
                if(file.exists()) {
                    YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
                    ItemStack[] items = ((List<ItemStack>) c.get("inventory.content")).toArray(new ItemStack[0]);
                    String title = c.contains("inventory.title")?c.getString("inventory.title"):"Unknown DeathSign";
                    deathSign = DeathSignHandler.createDeathSign(items, UUID.fromString("00000000-0000-0000-0000-000000000000"), event.getClickedBlock().getLocation(),title,false);
                }
            }
            if (deathSign == null) {
                //Bukkit.broadcastMessage("3");
                DeathSignHandler.breakBrokenSign(event.getPlayer(),event.getClickedBlock());
                event.getPlayer().sendMessage("§cSomething is wrong with this deathsign.");
                return;
            }
            if (!event.getPlayer().getGameMode().equals(GameMode.SPECTATOR)
                    && !sign.getLine(1).equals(event.getPlayer().getName())) {
                //Bukkit.broadcastMessage("4");
                //if game mode isnt spec and the names don't match and sign exists, no items for u
                //THIS IS NOT YOUR SIGN FOOL
                long signTime = deathSign.getCreationTime();
                int timeout = Main.getPlugin().getConfig().getInt("signTimeoutSeconds") * 1000;
                if (System.currentTimeMillis() < (signTime + timeout)) {
                    if (event.getPlayer().hasPermission("deathsigns.admin")) {
                        event.getPlayer().sendMessage("Overriding protection due to admin permission.");
                    } else {

                        long expiresin = (signTime + timeout) - System.currentTimeMillis();
                        event.getPlayer().sendMessage(Main.getPlugin().getConfig().getString("sign_protected_message")
                                .replace("{time}", Util.millisToHumanString(expiresin, true))
                                .replace("{sign_owner}", sign.getLine(1))
                        );
                        return;
                    }
                }
            }
            //patch bedrock event spam
            if (bedrockDupeBandaid.contains(event.getClickedBlock())) return;
            bedrockDupeBandaid.add(event.getClickedBlock());
            /*if(dsign != null){
                if(currentlySaving.contains(sign.getLocation())){
                    bedrockDupeBandaid.remove(event.getPlayer().getName());
                    return;
                }else{
                    event.getPlayer().openInventory(dsign.inventory);
                    bedrockDupeBandaid.remove(event.getPlayer().getName());
                }
            }else{*/
            //Bukkit.broadcastMessage("5");
            //TODO load sign and open
            DeathSign finalDeathSign = deathSign;
            Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), new Runnable() {
                @Override
                public void run() {
                        /*Map.Entry<ItemStack[], String> items = Main.recallItems(sign.getLocation(),false);
                        String title = items.getValue();
                        if(title == null)title="Unknown DeathSign";
                        Inventory inv = Bukkit.createInventory(null,45, title);
                        Main.openSigns.put(sign.getLocation(),inv);*/
                    Inventory inv = finalDeathSign.getInventory();
                    //Bukkit.broadcastMessage("6");
                    Bukkit.getScheduler().runTask(Main.getPlugin(), new Runnable() {
                        @Override
                        public void run() {
                            if (!Main.getPlugin().getConfig().getBoolean("use_sign_inventory")) {
                                //Bukkit.broadcastMessage("7");
                                if (event.getPlayer().getGameMode().equals(GameMode.SPECTATOR)) return;
                                finalDeathSign.breakSign(event.getPlayer());
                                DeathSignHandler.removeSign(finalDeathSign);
                                //TODO finish debugging this
                                bedrockDupeBandaid.remove(event.getClickedBlock());
                                return;
                            }
                            event.getPlayer().openInventory(inv);
                            bedrockDupeBandaid.remove(event.getClickedBlock());
                        }
                    });
                }
            });
            // }
            if (event.getPlayer().hasPermission("deathsigns.admin") && !sign.getLine(1).equals(event.getPlayer().getName())) {
                return;
            }
            if (event.getPlayer().getGameMode().equals(GameMode.SPECTATOR)) return;
            //if (!Conf.signExistsInConfig(sign.getLocation())) return;
            String status = deathSign.getStatus();
            if (status == null || status.contains("PROTECTED")) {//key word CONTAINS, this is for protected AND unprotected
                event.getPlayer().getWorld().playSound(deathSign.getLocation().clone().add(0.5,0.5,0.5),Sound.BLOCK_WOODEN_DOOR_OPEN,SoundCategory.BLOCKS,0.5F,0F);
                if (event.getPlayer().getUniqueId().equals(deathSign.getOwner())) {
                    Main.log("§8[§cDeathSigns Admin§8]§c " + event.getPlayer().getName()
                            + " reclaimed their DeathSign at " + sign.getX() + ", " + sign.getY() + ", "
                            + sign.getZ() + " in " + sign.getWorld().getName() + ".");
                    deathSign.setStatus("OPENED_OWNER");
                } else {
                    Main.log("§8[§cDeathSigns Admin§8]§c " + event.getPlayer().getName()
                            + " claimed " + sign.getLine(1) + "'s DeathSign at " + sign.getX() + ", "
                            + sign.getY() + ", " + sign.getZ() + " in " + sign.getWorld().getName() + ".");
                    deathSign.setStatus("OPENED_OTHER");
                }
                deathSign.setStatusTime(System.currentTimeMillis());
            }
        }
    }

    public static void removeDeathCompasses(Location loc, Inventory inv) {//TODO tool break effect
        for (ItemStack item : inv.getContents()) {
            boolean compass = DeathSignHandler.breakIfCompass(loc,item);
            if(compass){
                inv.remove(item);
            }
        }
    }

    static HashMap<UUID, DeathSign> recentDeathSigns = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {

        if (recentDeathSigns.containsKey(e.getPlayer().getUniqueId())) {
            DeathSign deathSign = recentDeathSigns.get(e.getPlayer().getUniqueId());
            recentDeathSigns.remove(e.getPlayer().getUniqueId());

            if (Main.plugin.getConfig().getBoolean("death_compass.give_on_respawn.enabled")) {
                if (Main.plugin.getConfig().getBoolean("death_compass.give_on_respawn.require_permission")
                        && !e.getPlayer().hasPermission("deathsigns.compass.on_respawn")) return;
                if (Main.plugin.getConfig().getBoolean("death_compass.require_permission")
                        && !e.getPlayer().hasPermission("deathsigns.compass")) return;
                if (Main.plugin.getConfig().getBoolean("death_compass.only_give_if_respawn_world.enabled")
                        && !Main.plugin.getConfig().getStringList("death_compass.only_give_if_respawn_world.worlds")
                        .contains(e.getRespawnLocation().getWorld().getName())) return;

                String status = deathSign.getStatus();
                if (status == null) {
                    e.getPlayer().sendMessage("§cDeathSign not found in config, how did this happen?");
                    return;
                }
                if (status.equals("RECOVERED")) {
                    e.getPlayer().sendMessage("§cYou already broke this deathsign??? what???");
                    return;
                }
                if (status.equals("TAKEN")) {
                    e.getPlayer().sendMessage("§cSomeone already broke your deathsign.");
                    return;
                }
                //sender.sendMessage();
                //ItemStack comp = Main.getDeathsignCompass(Bukkit.getOfflinePlayer(dsign.owner).getName(),targetLocation,dsign.deathMessage, sign);
                ItemStack comp = deathSign.getDeathsignCompass();
                int delay = Main.plugin.getConfig().getInt("death_compass.give_on_respawn.delay");
                if(delay == -1){
                    ((Player) e.getPlayer()).getInventory().addItem(comp);
                }else{
                    Bukkit.getScheduler().scheduleSyncDelayedTask(Main.plugin, new Runnable() {
                        @Override
                        public void run() {
                            ((Player) e.getPlayer()).getInventory().addItem(comp);
                        }
                    },delay);
                }
                //e.getPlayer().sendMessage("§aEnjoy your compass.");
            }
        }

        //TODO put the death location message and whatnot here... or dont because theres a ton of econ shit now


    }

    static List<Location> currentlySaving = new ArrayList<>();

    @EventHandler
    public void onInvClose(InventoryCloseEvent e) {
        /*if(e.getPlayer().getName().equals("avixk")){
            e.getPlayer().sendMessage(e.getInventory().getType().name());
            e.getPlayer().sendMessage(e.getInventory().getStorageContents().length+"");
        }*/
        if (/*!(bedrockDupeBandaid.contains(e.getPlayer().getName()) && */ e.getViewers().size() == 1) {
            DeathSign deathSign = DeathSignHandler.getLoadedDeathSign(e.getInventory());//returns null if its not a deathsign
            if (deathSign != null) {
                boolean empty = true;
                for (ItemStack item : deathSign.inventory.getContents()) {// just seeing if the inv is empty or not
                    if (item != null && DeathSignHandler.getCompassID(item) == null) {
                        empty = false;
                        break;
                    }
                }
                if (empty) {
                    if(!e.getPlayer().getGameMode().equals(GameMode.SPECTATOR)){
                        deathSign.breakSign((Player) e.getPlayer());
                        removeDeathCompasses(e.getPlayer().getEyeLocation(),e.getPlayer().getInventory());
                    }
                } else {
                    deathSign.saveItems();
                }
            }
        }
        if(e.getInventory().getLocation() != null){
            removeDeathCompasses(e.getInventory().getLocation(),e.getInventory());
        }
    }
    /*@EventHandler
    public void onInvClick(InventoryClickEvent e){// is this even needed? ima just comment it out i guess
        if(DeathSignHandler.getDeathSign(e.getClickedInventory()) != null){
            if(e.getWhoClicked().getGameMode().equals(GameMode.SPECTATOR)){
                e.setCancelled(true);
            }
        }
    }*//*
    public static void saveSign(Location loc, ItemStack[] items, String title){
        currentlySaving.add(loc);
        Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), new Runnable() {
            @Override
            public void run() {
                try {
                    Main.saveItems(loc, items, title);
                    currentlySaving.remove(loc);
                    DeathSign deathSign = DeathSignHandler.getSign(loc);
                    if(deathSign != null)DeathSignHandler.removeSign(deathSign);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }*/

}

