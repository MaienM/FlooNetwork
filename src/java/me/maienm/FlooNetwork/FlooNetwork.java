package me.maienm.FlooNetwork;

import java.io.File;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class FlooNetwork extends JavaPlugin implements Listener 
{
    protected static FlooNetwork plugin;
    protected FileConfiguration config;
    private File portalFile;

    protected HashMap<String, Location> destinations = new HashMap<String, Location>();
    
    @Override
    public void onEnable() 
    {
        System.out.println("FlooNetwork:");

        // Load the list of fireplaces from the config.
        config = getConfig();
        ConfigurationSection fireplaces = config.getConfigurationSection("fireplaces");
        if (fireplaces != null)
        {
            for (String name : fireplaces.getKeys(false))
            {
                System.out.println(name);
            }
        }

        // Register all event handlers.
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) 
    {
        // No arguments => usage.
        if (args.length == 0)
        {
            String version = getDescription().getVersion();
            sender.sendMessage(ChatColor.GOLD + "FlooNetwork " + ChatColor.BLUE + version + ChatColor.GOLD + " by MaienM");
            sender.sendMessage("--------------------");
            sender.sendMessage("/fn list: List your own fireplaces.");
            sender.sendMessage("/fn list <player>: List your own fireplaces.");
            sender.sendMessage("/fn listall: List all fireplaces.");
            sender.sendMessage("/fn warpto <fireplace>: Warp to fireplace.");
            return true;
        }
        else 
        {
            String command = args[0];
            switch (command.toLowerCase())
            {
                case "list":
                    String player = null;

                    // If no arguments => current player.
                    if (args.length == 1)
                    {
                        // If the sender is not a player (console), give an error.
                        if (sender instanceof Player)
                        {
                            player = ((Player) sender).getPlayerListName().toLowerCase();
                        }
                        else
                        {
                            sender.sendMessage(ChatColor.RED + "You must specify an username when using this from the console.");
                            return false;
                        }
                    }

                    // If one argument => get player by name.
                    else if (args.length == 2)
                    {
                        player = args[1].toLowerCase();
                    }

                    // If more arguments => error.
                    else 
                    {
                        sender.sendMessage(ChatColor.RED + "Invalid number of arguments.");
                        return false;
                    }
                    break;
            }
        }
        return false;
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) 
    {
        // Check if the sign matches the criterea.
        if (!event.getLine(0).equals("[fn]"))
        {
            return;
        }

        // Get the player that triggered the event.
        Player player = event.getPlayer();

        // Check the player for permissions.
        if (!player.hasPermission("floonetwork.createFireplace"))
        {
            player.sendMessage(ChatColor.RED + "You do not have permission create a FlooNetwork fireplace.");
            rejectSign(event);
        }
    }

    /**
     * Reject an SignChangeEvent, dropping the sign.
     */
    private void rejectSign(SignChangeEvent event)
    {
        // Get the sign.
        Block sign = event.getBlock();

        // Replace the sign by air, drop as resource.
        sign.setTypeId(0);
        sign.getWorld().dropItem(sign.getLocation(), new ItemStack(Material.SIGN, 1));

        // Mark event as cancelled.
        event.setCancelled(true);
    }
}
