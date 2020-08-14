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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Events implements Listener {
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (Main.disableSignPlayers.contains(event.getEntity().getName())) return;
        if (event.getDrops().isEmpty()) return;

        Location deathLocation = event.getEntity().getLocation().clone();
        if (deathLocation.getY() < 3) deathLocation.setY(3);

        int radius = 2;    //radius in which it will search for a valid sign placement
        int yradius = 10;    //radius in which it will search for a valid sign placement
        Block block = deathLocation.getBlock();
        //Bukkit.broadcastMessage(Main.locationToString(block.getLocation()));
        if ((block.getType().equals(Material.AIR) ||
                block.getType().equals(Material.LAVA) ||
                block.getType().equals(Material.WATER)) && (
                block.getRelative(0, 1, 0).getType().equals(Material.AIR) ||
                        block.getRelative(0, 1, 0).getType().equals(Material.LAVA) ||
                        block.getRelative(0, 1, 0).getType().equals(Material.WATER))) {
            Main.spawnDeathSign(block, event.getEntity(), event.getDrops().toArray(new ItemStack[0]));
            event.getDrops().clear();
            return;
        } else
            for (int x = -(radius); x <= radius; x++) {// scan for a valid sign placement
                for (int y = 0; y <= yradius; y++) {
                    for (int z = -(radius); z <= radius; z++) {
                        Block nearbyBlock = block.getRelative(x, y, z);
                        //Bukkit.broadcastMessage("testing block "+Main.locationToString(nearbyBlock.getLocation()));
                        if ((nearbyBlock.getType().equals(Material.AIR) || nearbyBlock.getType().equals(Material.LAVA) || nearbyBlock.getType().equals(Material.WATER)) && (nearbyBlock.getRelative(0, 1, 0).getType().equals(Material.AIR) || nearbyBlock.getRelative(0, 1, 0).getType().equals(Material.LAVA) || nearbyBlock.getRelative(0, 1, 0).getType().equals(Material.WATER))) {
                            Main.spawnDeathSign(nearbyBlock, event.getEntity(), event.getDrops().toArray(new ItemStack[0]));
                            event.getDrops().clear();
                            return;
                        }
                    }
                }
            }
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
            if (sign.getLine(0).equals("§lR.I.P.")) ;
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

            //Bukkit.getLogger().info("DeathSignsDebug 1");
            Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    //Bukkit.getLogger().info("DeathSignsDebug 2");
                    ItemStack[] items = Main.recallItems(sign.getLocation());
                    Bukkit.getScheduler().runTask(Main.getPlugin(), new Runnable() {
                        @Override
                        public void run() {

                            //Bukkit.getLogger().info("DeathSignsDebug 3");
                            event.getClickedBlock().setType(Material.AIR);
                            event.getClickedBlock().getRelative(0, -1, 0).setType(Material.AIR);

                            for (ItemStack i : items) {
                                //Bukkit.getLogger().info("DeathSignsDebug 4");
                                if (i != null) sign.getWorld().dropItem(event.getClickedBlock().getLocation(), i);
                            }
                            bedrockDupeBandaid.remove(event.getPlayer().getName());
                        }
                    });
                }
            });
        }
    }
}

