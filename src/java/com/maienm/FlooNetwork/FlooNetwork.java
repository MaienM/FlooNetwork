package com.maienm.FlooNetwork;

import com.maienm.FlooNetwork.Fireplace;
import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class FlooNetwork extends JavaPlugin implements Listener 
{
    protected static FlooNetwork plugin;
    protected FileConfiguration config;
    private File portalFile;

    /**
     * The material used to travel.
     */
    final protected Material TRAVALCATALYST = Material.REDSTONE;

    /**
     * The list of all DamageCause types we want to ignore when in a fireplace.
     */
    private static final Set<DamageCause> IGNORED_DAMAGECAUSES = new HashSet<DamageCause>(Arrays.asList(
        new DamageCause[] {DamageCause.FIRE, DamageCause.FIRE_TICK}
    ));

    /**
     * The list of all Action types we want to accept to travel.
     */
    private static final Set<Action> ACCEPTED_TRAVEL_ACTIONS = new HashSet<Action>(Arrays.asList(
        new Action[] {Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK}
    ));

    /**
     * Map of all fireplaces.
     */
    private HashMap<OfflinePlayer, HashMap<String, Fireplace>> fireplaces = new HashMap<OfflinePlayer, HashMap<String, Fireplace>>();

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

        // Check whether the user has the required permissions.
        if (!player.hasPermission("floonetwork.createFireplace"))
        {
            player.sendMessage(ChatColor.RED + "You do not have permission to create a fireplace.");
            rejectSign(event);
            return;
        }

        // Check whether the the second line is valid.
        String name = event.getLine(1);
        if (name.equals(""))
        {
            player.sendMessage(ChatColor.RED + "You need to give the fireplace a name.");
            rejectSign(event);
            return;
        }
        if (fireplaces.containsKey(player) && fireplaces.get(player).containsKey(name))
        {
            player.sendMessage(ChatColor.RED + "You already have a fireplace with this name.");
            rejectSign(event);
            return;
        }

        // Check whether we can find a valid fireplace here.
        Fireplace fireplace = Fireplace.detect(event.getBlock().getLocation());
        if (fireplace == null) 
        {
            player.sendMessage(ChatColor.RED + "That does not seem to be a valid fireplace layout.");
            rejectSign(event);
            return;
        }

        // Set all values of the fireplace.
        fireplace.owner = player;
        fireplace.name = name;

        // Store the fireplace.
        if (!fireplaces.containsKey(player))
            fireplaces.put(player, new HashMap<String, Fireplace>());
        fireplaces.get(player).put(name, fireplace);

        // Notify the player.
        player.sendMessage(ChatColor.BLUE + "Fireplace created");
        System.out.println(fireplace.toString());
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event)
    {
        // Check if the entity is a player.
        if (event.getEntityType() != EntityType.PLAYER)
        {
            return;
        }

        // Check if the damage is due to fire.
        if (!IGNORED_DAMAGECAUSES.contains(event.getCause()))
        {
            return;
        }

        // Check if the source of damage is a fireplace.
        Player player = (Player) event.getEntity();
        if (!isFireplace(player.getLocation(), true))
        {
            return;
        }

        // Cancel the damage.
        event.setCancelled(true);
        event.getEntity().setFireTicks(0);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerUse(PlayerInteractEvent event)
    {
        // Check whether the player is using the correct material.
        if (event.getMaterial() != TRAVALCATALYST)
        {
            return;
        }

        // Check whether the user is right-clicking.
        if (!ACCEPTED_TRAVEL_ACTIONS.contains(event.getAction()))
        {
            return;
        }

        // Check whether the player is in a fireplace.
        Player player = event.getPlayer();
        if (!isFireplace(player.getLocation(), true))
        {
            return;
        }

        // Check whether the user has the required permissions.
        if (!player.hasPermission("floonetwork.useFireplace"))
        {
            player.sendMessage(ChatColor.RED + "You do not have permission to use a FlooNetwork fireplace.");
            return;
        }

        // Consume item.
        ItemStack item = event.getItem();
        item.setAmount(item.getAmount() - 1);
    }

    /**
     * Determine whether the Location is part of a FlooNetwork Fireplace.
     */
    private boolean isFireplace(Location location, boolean fuzzyLookup)
    {
        // Build the list of locations.
        ArrayList<Location> locations = new ArrayList<Location>();
        locations.add(location.clone());
        if (fuzzyLookup)
        {
            locations.add(location.clone().add(0.5,  0, 0));
            locations.add(location.clone().add(-0.5, 0, 0.5));
            locations.add(location.clone().add(-0.5, 0, -0.5));
            locations.add(location.clone().add(0,    0, -0.5));
        }

        // Loop over the fireplaces.
        for (HashMap<String, Fireplace> userFireplaces : fireplaces.values())
        {
            for (Fireplace fireplace : userFireplaces.values())
            {
                // Loop over the locations.
                for (Location loc : locations)
                {
                    if (fireplace.contains(loc))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
