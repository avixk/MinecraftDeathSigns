package me.avixk.DeathSigns;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Events implements Listener {
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Main.getPlugin().getLogger().info("§c"+event.getEntity().getName() + " died at " + event.getEntity().getLocation().getBlockX() + ", " + event.getEntity().getLocation().getBlockY() + ", " + event.getEntity().getLocation().getBlockZ() + " in " + event.getEntity().getLocation().getWorld().getName());
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
                            Main.spawnDeathSign(nearbyBlock, event.getEntity(), event.getDrops().toArray(new ItemStack[0]));
                            event.getDrops().clear();
                            return;
                        }
                    }
                }
            }
        }
        Main.getPlugin().getLogger().info("§cNo valid grave location found for " + event.getEntity().getName() + ", dropping items instead.");
        event.getEntity().sendMessage(Main.getPlugin().getConfig().getString("deathPrivateMessageNoGrave")
                .replace("{x}",block.getX()+"")
                .replace("{y}",block.getY()+"")
                .replace("{z}",block.getZ()+"")
                .replace("{world}",block.getWorld().getName())
        );
    }

    @EventHandler
    public void onBlockExplode(EntityExplodeEvent e) {
        for (Block b : e.blockList()) {
            if (b.getType().equals(Material.OAK_SIGN)) {
                Sign sign = (Sign) b.getState();
                if (sign.getLine(0).equals("§lR.I.P.")) {
                    e.setCancelled(true);
                }
            }
        }

    }

    @EventHandler
    public void onBedExplode(BlockExplodeEvent e) {
        for (Block b : e.blockList()) {
            if (b.getType().equals(Material.OAK_SIGN)) {
                Sign sign = (Sign) b.getState();
                if (sign.getLine(0).equals("§lR.I.P.")) {
                    e.setCancelled(true);
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
            Sign sign = (Sign) event.getClickedBlock().getState();
            if (!sign.getLine(0).equals("§lR.I.P.")) return;
            if (!sign.getLine(1).equals(event.getPlayer().getName())) {//if the names don't match, no items for u
                //THIS IS NOT YOUR SIGN FOOL
                try {
                    long signEpochSecondsTimeout = (new java.text.SimpleDateFormat("HH:mm MMM/dd/yy").parse(sign.getLine(3)).getTime() / 1000) + Main.getPlugin().getConfig().getLong("signTimeoutSeconds");//time the sign was made in minutes + timeout
                    long localEpochSeconds = System.currentTimeMillis() / 1000;
                    if (signEpochSecondsTimeout > localEpochSeconds) {//if sign is protected, print message and return
                        long diff = signEpochSecondsTimeout - localEpochSeconds;
                        long minutes = TimeUnit.SECONDS.toMinutes(diff);
                        long seconds = diff - (minutes * 60);
                        event.getPlayer().sendMessage(Main.getPlugin().getConfig().getString("notYourFuckinSignMessage")
                                .replace("{seconds_remaining}", diff + "")
                                .replace("{minutes}", minutes + "")
                                .replace("{seconds}", seconds + "")
                                .replace("{sign_owner}", sign.getLine(1))
                        );
                        return;//no items for u
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            //patch bedrock event spam
            if (bedrockDupeBandaid.contains(event.getPlayer().getName())) return;
            bedrockDupeBandaid.add(event.getPlayer().getName());

            if(sign.getLine(1).equals(event.getPlayer().getName())){
                Bukkit.getLogger().info("§c" + event.getPlayer().getName() + " reclaimed their DeathSign at " + sign.getX() + ", " + sign.getY() + ", " + sign.getZ() + " in " + sign.getWorld().getName() + ".");
            }else{
                Bukkit.getLogger().info("§c" + event.getPlayer().getName() + " claimed " + sign.getLine(1) + "'s DeathSign at " + sign.getX() + ", " + sign.getY() + ", " + sign.getZ() + " in " + sign.getWorld().getName() + ".");
            }


            Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    ItemStack[] items = Main.recallItems(sign.getLocation());
                    Bukkit.getScheduler().runTask(Main.getPlugin(), new Runnable() {
                        @Override
                        public void run() {
                            event.getClickedBlock().setType(Material.AIR);
                            event.getClickedBlock().getRelative(0, -1, 0).setType(Material.AIR);

                            for (ItemStack i : items) {
                                if (i != null) sign.getWorld().dropItem(event.getClickedBlock().getLocation().add(0.5,0.5,0.5), i);
                            }
                            bedrockDupeBandaid.remove(event.getPlayer().getName());
                        }
                    });
                }
            });
        }
    }
}

