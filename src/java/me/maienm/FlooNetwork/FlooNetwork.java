package me.maienm.FlooNetwork;

import java.io.File;
import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class FlooNetwork extends JavaPlugin implements Listener 
{
    protected static FlooNetwork plugin;
    protected FileConfiguration config;
    private File portalFile;

    protected HashMap<String, Location> destinations = new HashMap<String, Location>();
    
    @Override
    public void onEnable () 
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

        /*
        saveConfig();
        if (dests != null) {
            for (String key : dests.getKeys(false)) {
                String world = dests.getString(key+".world");
                if (getServer().getWorld(world) == null) {
                    WorldCreator wc = makeWorld(world, null);
                    Bukkit.createWorld(wc);
                }
                key = key.toLowerCase();
                double x = dests.getDouble(key+".x");
                double y = dests.getDouble(key+".y");
                double z = dests.getDouble(key+".z");
                List<Float> floats = dests.getFloatList(key+".yawpitch");
                float yaw = floats.get(0);
                float pitch = floats.get(1);
                Location loc = new Location(getServer().getWorld(world), x, y, z, yaw, pitch);
                destinations.put(key, loc);
            }
        }
        portalConfig = getPortals();
        forceWorldLoads();
        getServer().getPluginManager().registerEvents(this, this);
        */
    }

    /*@Override
    public void onDisable() 
    {
    }*/
    
    @Override
    public boolean onCommand (CommandSender sender, Command cmd, String commandLabel, String[] args) 
    {
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
                    Player player = null;
                    if (args.length == 1)
                    {
                        
                    }
                    else if (args.length == 2)
                    {

                    }
                    else 
                    {
                        sender.sendMessage(ChatColor.RED + "Invalid number of arguments.");
                    }
                    break;
            }
        }
        return false;
    }
}
