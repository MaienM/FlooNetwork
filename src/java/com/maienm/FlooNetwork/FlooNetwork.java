package com.maienm.FlooNetwork;

import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.maienm.FlooNetwork.Fireplace;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
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

    /**
     *  LWC plugin (protection)
     */
    private LWC lwc;
 
    /**
     * On plugin load.
     */
    @Override
    public void onEnable() 
    {
        // Load the config.
        reloadConfigCustom();

        // Get LWC, if it exists.
        Plugin plugLWC = Bukkit.getPluginManager().getPlugin("LWC");
        if (plugLWC != null)
        {
            lwc = ((LWCPlugin)plugLWC).getLWC();
        }

        // Register all event handlers.
        getServer().getPluginManager().registerEvents(this, this);
    }

    /**
     * On plugin unload.
     */
    @Override
    public void onDisable()
    {
        // Save the config.
        saveConfigCustom();
    }

    /**
     * (Re)loads the config file.
     */
    private void reloadConfigCustom()
    {
        // Reload the config.
        reloadConfig();
        config = getConfig();

        // Load the list of fireplaces from the config.
        ConfigurationSection cfgFireplaces = config.getConfigurationSection("fireplaces");
        fireplaces.clear();
        if (cfgFireplaces != null)
        {
            OfflinePlayer player;
            ConfigurationSection fpConfig;
            List<Integer> coords;
            World world;
            Location location;
            Fireplace fp;

            // Loop over all players.
            for (Map.Entry<String, Object> playerEntry : cfgFireplaces.getValues(false).entrySet())
            {
                // Get the player object.
                player = getServer().getOfflinePlayer(playerEntry.getKey());
                fireplaces.put(player, new HashMap<String, Fireplace>());

                // Loop over this users fireplaces.
                for (Map.Entry<String, Object> fpEntry : ((ConfigurationSection)playerEntry.getValue()).getValues(false).entrySet())
                {
                    // Get the ConfigurationSection.
                    fpConfig = (ConfigurationSection) fpEntry.getValue();

                    // Read out the location.
                    coords = fpConfig.getIntegerList("coordinates");
                    world = getServer().getWorld(fpConfig.getString("world"));
                    location = new Location(world, coords.get(0), coords.get(1), coords.get(2));

                    // Create the fireplace.
                    fp = Fireplace.detect(location);
                    if (fp == null)
                    {
                        System.out.println(String.format("Fireplace %s of player %s read from config file is invalid; it has been ignored.", playerEntry.getKey(), fpEntry.getKey()));
                        continue;
                    }

                    // Save the fireplace.
                    fp.owner = player;
                    fp.name = fpEntry.getKey();
                    fireplaces.get(player).put(fpEntry.getKey(), fp);
                }
            }
        }
    }

    /**
     * Saves the config file.
     */
    private void saveConfigCustom()
    {
        // Rewrite the fireplaces section.
        ConfigurationSection cfgFireplaces = config.createSection("fireplaces");
        ConfigurationSection cfgPlayer;
        ConfigurationSection cfgFireplace;
        Location location;

        for (Map.Entry<OfflinePlayer, HashMap<String, Fireplace>> playerEntry : fireplaces.entrySet())
        {
            // Create the section for this player.
            cfgPlayer = cfgFireplaces.createSection(playerEntry.getKey().getName());

            for (Map.Entry<String, Fireplace> fpEntry : playerEntry.getValue().entrySet())
            {
                // Create the section for this fireplace.
                cfgFireplace = cfgPlayer.createSection(fpEntry.getKey());

                // Set the data.
                location = fpEntry.getValue().getSignLocation();
                cfgFireplace.set("world", location.getWorld().getName());
                cfgFireplace.set("coordinates", Arrays.asList(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
            }
        }

        // Save the config.
        saveConfig();
    }

    /**
     * Command event.
     *
     * Handles the /fn command.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] arguments)
    {
        // Convert the arguments to a list.
        ArrayList<String> args = new ArrayList<String>(Arrays.asList(arguments));

        // No arguments => usage.
        if (args.size() == 0)
        {
            String version = getDescription().getVersion();
            sender.sendMessage(ChatColor.GOLD + "FlooNetwork " + ChatColor.BLUE + version + ChatColor.GOLD + " by MaienM");
            sender.sendMessage("--------------------");
            if (sender.hasPermission("floonetwork.command.list"))
            {
                sender.sendMessage("/fn list: List your own fireplaces.");
                sender.sendMessage("/fn list <sender>: List the fireplaces of <player> you have access to.");
                sender.sendMessage("/fn listall: List all fireplaces you have access to.");
            }
            if (sender.hasPermission("floonetwork.command.warp"))
                sender.sendMessage("/fn warpto <sender> <fireplace>: Warp self to <fireplace> of <player>.");
            if (sender.hasPermission("floonetwork.command.warp.other"))
                sender.sendMessage("/fn warpto <sender> <fireplace> <target>: Warp <target> to <fireplace> of <player>.");
            if (sender.hasPermission("floonetwork.command.reload"))
                sender.sendMessage("/fn reload: Reload the config.");
            return true;
        }
        else 
        {
            String command = args.remove(0);
            OfflinePlayer subject;
            switch (command.toLowerCase())
            {
                case "list":
                    // Get the subject.
                    subject = getSubject(sender, args);
                    if (subject == null)
                    {
                        return false;
                    }

                    // Check permission.
                    if (!requirePermission(sender, "floonetwork.command.list"))
                    {
                        return false;
                    }

                    // If more arguments => error.
                    if (args.size() > 0)
                    {
                        return sendError(sender, "Invalid number of arguments.");
                    }

                    // Check if user has any fireplaces.
                    if (!fireplaces.containsKey(subject))
                    {
                        return sendError(sender, "No fireplaces found.");
                    }

                    // List all fireplaces.
                    sender.sendMessage(ChatColor.BLUE + "Fireplaces of " + subject.getName());
                    for (String key : fireplaces.get(subject).keySet())
                    {
                        sender.sendMessage(key);
                    }
                    break;

                case "listall":
                    // Check permission.
                    if (!requirePermission(sender, "floonetwork.command.list"))
                    {
                        return false;
                    }

                    // If more arguments => error.
                    if (args.size() > 0)
                    {
                        return sendError(sender, "Invalid number of arguments.");
                    }

                    // List all fireplaces.
                    for (Map.Entry<OfflinePlayer, HashMap<String, Fireplace>> playerEntry : fireplaces.entrySet())
                    {
                        sender.sendMessage(ChatColor.BLUE + "Fireplaces of " + playerEntry.getKey().getName());
                        for (String key : playerEntry.getValue().keySet())
                        {
                            sender.sendMessage(key);
                        }
                    }
                    break;

                case "reload":
                    if (!requirePermission(sender, "floonetwork.command.reload"))
                    {
                        return false;
                    }

                    reloadConfigCustom();
                    break;

                case "warpto":
                case "tp":
                    // Get the fireplace.
                    if (args.size() < 2)
                    {
                        return sendError(sender, "Invalid number of arguments.");
                    }
                    Fireplace fp = getFireplace(getServer().getOfflinePlayer(args.remove(0)), args.remove(0));
                    if (fp == null)
                    {
                        return sendError(sender, "Unable to find fireplace.");
                    }

                    // Get the subject.
                    subject = getSubject(sender, args);
                    if (subject == null)
                    {
                        return false;
                    }

                    // Check permission.
                    if (!requirePermission(sender, "floonetwork.command.warp" + (subject.equals(sender) ? "" : ".other") + (fp.isOwner((Player)sender) ? "" : ".anywhere")))
                    {
                        return false;
                    }

                    // If more arguments => error.
                    if (args.size() > 0)
                    {
                        return sendError(sender, "Invalid number of arguments.");
                    }

                    // Warp to the fireplace.
                    Player player = subject.getPlayer();
                    if (player == null)
                    {
                        return sendError(sender, "Unable to find target player.");
                    }
                    fp.warpTo(player);
            }
        }
        return false;
    }

    /**
     * Convenience method to get the subject of a command. The subject can be either the first remaining argument, the current player, or none.
     */
    private OfflinePlayer getSubject(CommandSender sender, List<String> args)
    {
        // If no arguments => current player.
        if (args.size() == 0)
        {
            // If the sender is not a player (console), give an error.
            if (sender instanceof Player)
            {
                return (OfflinePlayer)sender;
            }
            else
            {
                sendError(sender, "You must specify a player when using this from the console.");
                return null;
            }
        }

        // Else => get player by name.
        else 
        {
            OfflinePlayer player = getServer().getOfflinePlayer(args.remove(0));

            if (player == null)
            {
                sendError(sender, "Unknown user.");
                return null;
            }

            return player;
        }
    }

    /**
     * Block break event.
     *
     * Protects the fireplace when needed.
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event)
    {
        // Check if the block belongs to a fireplace.
        Fireplace fp = getFireplace(event.getBlock().getLocation(), false, true, true, false);
        if (fp == null)
        {
            return;
        }

        // Get the player that triggered the event.
        Player player = event.getPlayer();

        // If the event was not triggered by a player, block it.
        if (player == null)
        {
            event.setCancelled(true);
            return;
        }

        // Check the permissions.
        if (!requirePermission(player, "floonetwork.destroy" + (fp.isOwner(player) ? "" : ".other")))
        {
            event.setCancelled(true);
            return;
        }

        // Destroy the fireplace.
        fireplaces.get(fp.owner).remove(fp.name);
        player.sendMessage(ChatColor.BLUE + String.format("Destroyed fireplace %s%s.", fp.name, fp.isOwner(player) ? "" : " of " + fp.owner.getName()));

        // Mark the sign as deactivated.
        Sign sign = (Sign)fp.getSignLocation().getBlock().getState();
        sign.setLine(0, ChatColor.RED + sign.getLine(0));
        sign.setLine(1, ChatColor.RED + sign.getLine(1));
        sign.setLine(2, ChatColor.RED + sign.getLine(2));
        sign.setLine(3, ChatColor.RED + "DEACTIVATED");
        sign.update();
    }

    /**
     * Sign placement/change event.
     *
     * Handles detection of new fireplaces.
     */
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
        if (!requirePermission(player, "floonetwork.create"))
        {
            rejectSign(event);
            return;
        }

        // Check whether the the second line is valid.
        String name = event.getLine(1);
        if (name.equals(""))
        {
            sendError(player, "You need to give the fireplace a name.");
            rejectSign(event);
            return;
        }
        if (getFireplace(player, name) != null)
        {
            sendError(player, "You already have a fireplace with this name.");
            rejectSign(event);
            return;
        }

        // Check whether we can find a valid fireplace here.
        Fireplace fireplace = Fireplace.detect(event.getBlock().getLocation());
        if (fireplace == null) 
        {
            sendError(player, "That does not seem to be a valid fireplace layout.");
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

        // Save the config.
        saveConfigCustom();

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

    /**
     * Player damage event.
     *
     * Handles protecting a player from fire while in a fireplace.
     */
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
        if (getFireplace(player.getLocation(), true, false, false, true) == null)
        {
            return;
        }

        // Cancel the damage.
        event.setCancelled(true);
        event.getEntity().setFireTicks(0);
    }

    /**
     * Item use event.
     *
     * Handles a player using a fireplace.
     */
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
        Fireplace fp = getFireplace(player.getLocation(), true, false, false, true);
        if (fp == null)
        {
            return;
        }

        // Block the event, in case the catalyst is set to something placeable/useable (such as redstone).
        event.setCancelled(true);

        // Check whether the user has the required permissions.
        if (!requirePermission(player, "floonetwork.use" + (fp.isOwner(player) ? "" : ".other")))
        {
            return;
        }

        // Check whether the fireplace is lighted.
        if (!fp.isLighted())
        {
            sendError(player, "The fireplace is not burning. What a poor excuse for a fireplace it is.");
            return;
        }

        // Show  menu.
        // @TODO

        // Check permission.
        if (!hasAccess(player, fp))
        {
            sendError(player, "You do not have permission to access this fireplace.");
            return;
        }

        // Consume item.
        ItemStack item = event.getItem();
        item.setAmount(item.getAmount() - 1);
    }

    /**
     * Convenience method to check whether a player has access to a fireplace.
     */
    private boolean hasAccess(Player player, Fireplace fireplace)
    {
        // Check whether the user has the required permission.
        if (!player.hasPermission("floonetwork.travel" + (fireplace.isOwner(player) ? "" : "other")))
        {
            return false;
        }

        // The owner of a fireplace always has access.
        if (fireplace.isOwner(player))
        {
            return true;
        }

        // If LWC is present, use LWC.
        if (lwc != null)
        {
            return lwc.canAccessProtection(player, fireplace.getSignLocation().getBlock());
        }

        // No protection plugin was found, so the user is granted access on the merit of having the required permission.
        return true;
    }

    /**
     * Convenience method to send an error message to the user.
     */
    private boolean sendError(CommandSender sender, String error)
    {
        sender.sendMessage(ChatColor.RED + error);
        return false;
    }

    /**
     * Convenience method to check for permission.
     */
    private boolean requirePermission(CommandSender sender, String permission)
    {
        if (sender.hasPermission(permission))
        {
            return true;
        }

        sendError(sender, "You do not have the required permission to do this: " + ChatColor.BLUE + permission);
        return false;
    }

    /**
     * Convenience methods to find a fireplace.
     *
     * @param location The location to check.
     * @param fuzzyLookup If true, the given location will be checked in a less strict way. This is useful for if the position is not an exact position (such as a block has). For example, the position from an entity.
     * @param includeMaterial See Fireplace#contains.
     * @param includeSign See Fireplace#contains.
     * @param includeAir See Fireplace#contains.
     */
    private Fireplace getFireplace(Location location, boolean fuzzyLookup, boolean includeMaterial, boolean includeSign, boolean includeAir)
    {
        // Build the list of locations.
        ArrayList<Location> locations = new ArrayList<Location>();
        if (fuzzyLookup)
        {
            locations.add(location.clone().add(0.5,  0, 0));
            locations.add(location.clone().add(-0.5, 0, 0.5));
            locations.add(location.clone().add(-0.5, 0, -0.5));
            locations.add(location.clone().add(0,    0, -0.5));
        }
        else
        {
            locations.add(location.clone());
        }

        // Loop over the fireplaces.
        for (HashMap<String, Fireplace> userFireplaces : fireplaces.values())
        {
            for (Fireplace fireplace : userFireplaces.values())
            {
                // Loop over the locations.
                for (Location loc : locations)
                {
                    if (fireplace.contains(loc, includeMaterial, includeSign, includeAir))
                    {
                        return fireplace;
                    }
                }
            }
        }
        return null;
    }
    private Fireplace getFireplace(OfflinePlayer player, String name)
    {
        if (fireplaces.containsKey(player) && fireplaces.get(player).containsKey(name))
        {
            return fireplaces.get(player).get(name);
        }
        return null;
    }
}
