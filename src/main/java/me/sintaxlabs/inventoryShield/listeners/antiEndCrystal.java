package me.sintaxlabs.inventoryShield.listeners;

import me.sintaxlabs.inventoryShield.InventoryShield;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;


public class antiEndCrystal implements Listener
{
    @EventHandler
    public void stopEndCrystal (PlayerInteractEvent e)
    {
        Action action = e.getAction();
        Material material = e.getMaterial();
        Player player = e.getPlayer();

        if (player.hasPermission("inventoryshield.bypass"))
        {
            return;
        }
        else
        {
            if (InventoryShield.Global.configBlockEndCrystalPlacement)
            {
                if (action.isRightClick())
                {
                    if(material == Material.getMaterial(String.valueOf(Material.END_CRYSTAL)))
                    {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }
}
