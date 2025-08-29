package me.sintaxlabs.inventoryShield;

import me.sintaxlabs.inventoryShield.listeners.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class InventoryShield extends JavaPlugin implements Listener
{
    private Set<Material> blockedItems;


    @Override
    public void onEnable()
    {
        saveDefaultConfig();
        loadInventoryBlockedItems();
        Objects.requireNonNull(getCommand("reloadconfig")).setExecutor(this);
        getServer().getPluginManager().registerEvents(this,this);
        getLogger().info("InventoryShield Enabled");
        getServer().getPluginManager().registerEvents(new antiEndCrystal(), this);



        Global.configBlockEndCrystalPlacement = this.getConfig().getBoolean("block-End-Crystal-Placement");
        Global.configAntiBlockBreak = this.getConfig().getBoolean("anti-block-break");
    }


    public static class Global
    {
        public static boolean configBlockEndCrystalPlacement;
        public static boolean configAntiBlockBreak;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args)
    {
        if (args.length == 0)
        {
            sender.sendMessage("§eUsage: /inventoryshield reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload"))
        {
            if (sender.hasPermission("inventoryshield.reload"))
            {
                reloadConfig();
                loadInventoryBlockedItems();
                Global.configBlockEndCrystalPlacement = this.getConfig().getBoolean("block-End-Crystal-Placement");
                Global.configAntiBlockBreak = this.getConfig().getBoolean("anti-block-break");
                sender.sendMessage("§7[§6InventoryShield§7] §aConfig reloaded.");
            } else {
                sender.sendMessage("§cYou don't have permission to do that.");
            }
            return true;
        }

        sender.sendMessage("§cUnknown subcommand.");
        return true;
    }
    ////////////////////////////////////////////////////////////
    // Establishing banned items via Config.
    ////////////////////////////////////////////////////////////
    private void loadInventoryBlockedItems()
    {
        blockedItems = new HashSet<>();
        FileConfiguration config = getConfig();
        List<String> blockNames = config.getStringList("blocked-items");

        for (String name : blockNames)
        {
            try
            {
                Material material = Material.valueOf(name.toUpperCase());
                blockedItems.add(material);
            } catch (IllegalArgumentException e)
            {
                getLogger().warning("Invalid block type in config: " + name);
            }
        }
    }

    ////////////////////////////////////////////////////////////
    // Block Break Check
    // Runs off the Anti-Inventory List
    ////////////////////////////////////////////////////////////
    @EventHandler
    public void blockBreakChecker (BlockBreakEvent e)
    {
        if (Global.configAntiBlockBreak)
        {
            Material block = e.getBlock().getType();
            Player player = e.getPlayer();

            if (!player.hasPermission("inventoryshield.bypass"))
            {
                if (blockedItems.contains(block))
                {
                    e.setCancelled(true);
                }
            }
        }
    }


    ////////////////////////////////////////////////////////////
    // Block Place Check
    ////////////////////////////////////////////////////////////
    @EventHandler
    public void blockPlacementChecker (BlockPlaceEvent e)
    {
        Material block = e.getBlock().getType();
        Player player = e.getPlayer();

        if (!player.hasPermission("inventoryshield.bypass"))
        {
            PlayerInventory inventory = player.getInventory();
            ItemStack secondHand = inventory.getItemInOffHand();
            if (blockedItems.contains(block))
            {
                // Ender Eyes cannot be placed if EndPortalFrames are blocked.
                // This fixes that issue as long as the player is holding the Ender Eye in either hand.
                Block against = e.getBlockAgainst();
                if (against.getType() == Material.END_PORTAL_FRAME)
                {
                    // Main Hand Check
                    if (player.getInventory().getItemInMainHand().getType() == Material.ENDER_EYE)
                    {
                        // Check1
                        if (blockedItems.contains(secondHand.getType()))
                        {
                            e.setCancelled(true);
                            inventory.setItemInOffHand(null); // removes ALL stacks of that type
                        }
                        // Check2
                        for (ItemStack item : inventory.getStorageContents())
                        {
                            if (item != null && blockedItems.contains(item.getType()))
                            {
                                e.setCancelled(true);
                                inventory.remove(item.getType()); // removes ALL stacks of that type
                            }
                        }

                    }
                    // Offhand Check
                    else if (player.getInventory().getItemInOffHand().getType() == Material.ENDER_EYE)
                    {
                        for (ItemStack item : inventory.getStorageContents())
                        {
                            if (item != null && blockedItems.contains(item.getType()))
                            {
                                e.setCancelled(true);
                                inventory.remove(item.getType()); // removes ALL stacks of that type
                            }
                        }

                    }
                    else
                    {
                        e.setCancelled(true);
                    }

                }
                else
                {
                    e.setCancelled(true);
                    // Check1
                    if (blockedItems.contains(secondHand.getType()))
                    {
                        e.setCancelled(true);
                        inventory.setItemInOffHand(null); // removes ALL stacks of that type
                    }
                    // Check2
                    for (ItemStack item : inventory.getStorageContents())
                    {
                        if (item != null && blockedItems.contains(item.getType()))
                        {
                            e.setCancelled(true);
                            inventory.remove(item.getType()); // removes ALL stacks of that type
                        }
                    }
                }
            }
        }
    }


    ////////////////////////////////////////////////////////////
    // Opening any type of Inventory, Check Inventory for bad items.
    // Cant place in /listeners for some reason.
    ////////////////////////////////////////////////////////////
    @EventHandler
    public void inventoryCheck1 (InventoryOpenEvent e)
    {
        Player player = (Player) e.getPlayer();
        PlayerInventory inventory = player.getInventory();
        ItemStack secondHand = inventory.getItemInOffHand();

        if (!player.hasPermission("inventoryshield.bypass"))
        {
            // Loop through contents and remove blocked items
            for (ItemStack item : inventory.getContents())
            {
                if (item != null && blockedItems.contains(item.getType()))
                {
                    inventory.remove(item.getType()); // removes ALL stacks of that type
                    getServer().getScheduler().runTaskLater(this, task ->
                    {inventory.remove(item.getType());}, 40L); // 20 ticks = 1 second
                }
            }
            // Check1
            if (blockedItems.contains(secondHand.getType()))
            {
                e.setCancelled(true);
                inventory.setItemInOffHand(null); // removes ALL stacks of that type
            }
        }
    }

    ////////////////////////////////////////////////////////////
    // PlayerPickUpItem, Check Inventory for bad items,
    // Cant place in /listeners for some reason.
    ////////////////////////////////////////////////////////////
    @EventHandler
    public void inventoryCheck2 (EntityPickupItemEvent e)
    {
        @NotNull LivingEntity entity = e.getEntity();
        if (entity instanceof Player)
        {
            Player player = (Player) e.getEntity();
            Inventory inventory = player.getInventory();
            if (!player.hasPermission("inventoryshield.bypass"))
            {
                // Loop through contents and remove blocked items
                for (ItemStack item : inventory.getContents())
                {
                    if (item != null && blockedItems.contains(item.getType()))
                    {
                        inventory.remove(item.getType()); // removes ALL stacks of that type
                        getServer().getScheduler().runTaskLater(this, task ->
                        {inventory.remove(item.getType());}, 40L); // 20 ticks = 1 second
                    }
                }
            }
        }
    }

    @Override
    public void onDisable()
    {
        // Plugin shutdown logic
    }
}
