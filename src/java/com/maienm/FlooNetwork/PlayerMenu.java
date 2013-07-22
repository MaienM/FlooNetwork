package com.maienm.FlooNetwork;

import com.m0pt0pmatt.menuservice.api.MenuComponent;
import com.maienm.FlooNetwork.Fireplace;
import com.maienm.FlooNetwork.FlooNetwork;
import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

class PlayerMenu extends MenuComponent
{
	/**
	 * Whether the menu is valid.
	 */
	public boolean isValid = false;
	public int counter = 0;

	public PlayerMenu(Player player)
	{
		super();

		// Set title.
		addAttribute("title", player.getName());

		// Add items.
		FlooNetwork fn = (FlooNetwork) Bukkit.getPluginManager().getPlugin("FlooNetwork");
		for (Fireplace fp : fn.getAllFireplaces())
		{
			if (fp.hasAccess(player))
			{
				isValid = true;
				addFireplace(fp);
			}
		}
	}
	
	/**
	 * Add a fireplace to the menu.
	 */
	private void addFireplace(Fireplace fp)
	{
		// Create a new component.
		MenuComponent component = new MenuComponent();

		// Set attributes.
		System.out.println(fp.name);
		component.addAttribute("type", "button");
		component.addAttribute("tag", fp.owner.getName() + "-" + fp.name);
		component.addAttribute("text", fp.name);
		component.addAttribute("lore", Arrays.asList(fp.owner.getName()));
		component.addAttribute("image", fp.item);

		// Add click handler.
		//component.addAttribute("actions");

		// Add to menu.
		addComponent(component);
	}
}
