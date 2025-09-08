package me.sintaxlabs.YouCantHaveThat;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class main extends JavaPlugin implements Listener
{
    private Set<Material> blockedItems;

    @Override
    public void onEnable()
    {
        saveDefaultConfig();
        loadConfiguration();
        Objects.requireNonNull(getCommand("reloadconfig")).setExecutor(this);
        getServer().getPluginManager().registerEvents(this,this);
        getLogger().info("YouCantHaveThat Enabled");

        Global.configAntiBlockBreak = this.getConfig().getBoolean("AntiBreak");
        Global.configBreakIntoAir = this.getConfig().getBoolean("BreakIntoAir");
    }


    public static class Global
    {
        public static boolean configAntiBlockBreak;
        public static boolean configBreakIntoAir;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args)
    {
        if (args.length == 0)
        {
            sender.sendMessage("§eUsage: /youcanthavethat reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload"))
        {
            if (sender.hasPermission("youcanthavethat.reload"))
            {
                reloadConfig();
                loadConfiguration();
                sender.sendMessage("§7[§6YouCantHaveThat§7] §aConfig reloaded.");
            }
            else sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }

        sender.sendMessage("§cUnknown subcommand.");
        return true;
    }
    ////////////////////////////////////////////////////////////
    // Establishing banned items via Config.
    ////////////////////////////////////////////////////////////
    private void loadConfiguration()
    {
        Global.configAntiBlockBreak = this.getConfig().getBoolean("AntiBreak");
        Global.configBreakIntoAir = this.getConfig().getBoolean("BreakIntoAir");

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
    // Interaction Check
    // Checks for any placements of items/materials
    ////////////////////////////////////////////////////////////
    @EventHandler
    public void interactionCheck (PlayerInteractEvent e)
    {
        Material material = e.getMaterial();
        Player player = e.getPlayer();
        PlayerInventory inventory = player.getInventory();
        ItemStack secondHand = inventory.getItemInOffHand();

        if (!player.hasPermission("youcanthavethat.bypass"))
        {
            if (blockedItems.contains(material))
            {
                e.setCancelled(true);
                checkInventoryProcess(inventory, secondHand);
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
        Material material = e.getBlock().getType();
        Player player = e.getPlayer();
        Block block = e.getBlock();

        if (!player.hasPermission("youcanthavethat.bypass"))
        {
            if (Global.configAntiBlockBreak)
            {
                if (blockedItems.contains(material))
                {
                    if (Global.configBreakIntoAir) block.setType(Material.AIR);
                    else e.setCancelled(true);
                }
            }
        }
    }


    ////////////////////////////////////////////////////////////
    // Opening any container will trigger the check.
    // Cant place in /listeners for some reason.
    ////////////////////////////////////////////////////////////
    @EventHandler
    public void inventoryCheck1 (InventoryOpenEvent e)
    {
        Player player = (Player) e.getPlayer();
        PlayerInventory inventory = player.getInventory();
        ItemStack secondHand = inventory.getItemInOffHand();

        if (!player.hasPermission("youcanthavethat.bypass")) checkInventoryProcess(inventory, secondHand);
    }



    ////////////////////////////////////////////////////////////
    // PlayerPickUpItem, Check Inventory for bad items,
    // Cant place in /listeners for some reason.
    ////////////////////////////////////////////////////////////
    @EventHandler
    public void inventoryCheck2 (EntityPickupItemEvent e)
    {
        Item item = e.getItem();
        Player player = (Player) e.getEntity();
        PlayerInventory inventory = player.getInventory();
        ItemStack secondHand = inventory.getItemInOffHand();
        @NotNull LivingEntity entity = e.getEntity();
        @NotNull Material material = e.getItem().getItemStack().getType();

        if (entity instanceof Player && !player.hasPermission("youcanthavethat.bypass"))
        {
            if (blockedItems.contains(material))
            {
                e.setCancelled(true);
                item.remove();
                checkInventoryProcess(inventory, secondHand);
            }
        }
    }

    private void checkInventoryProcess(PlayerInventory inventory, ItemStack secondHand)
    {
        // Loop through contents and remove blocked items
        for (ItemStack item : inventory.getContents())
        {
            if (item != null && blockedItems.contains(item.getType()))
            {
                inventory.remove(item.getType()); // removes ALL stacks of that type
                getServer().getScheduler().runTaskLater(this, task -> {inventory.remove(item.getType());}, 40L); // 20 ticks = 1 second
            }
        }
        // Check1
        if (blockedItems.contains(secondHand.getType())) inventory.setItemInOffHand(null);
    }

    @Override
    public void onDisable()
    {
        // Plugin shutdown logic
    }
}
