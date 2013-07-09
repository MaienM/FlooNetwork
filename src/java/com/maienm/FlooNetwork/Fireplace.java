package com.maienm.FlooNetwork;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.material.Sign;

public class Fireplace
{
	/**
	 * The list of materials of which a fireplace may be made.
	 */
	private static final Set<Material> ACCEPTED_MATERIALS = new HashSet<Material>(Arrays.asList(
		new Material[] {Material.BRICK}
	));

	/**
	 * The location of the fireplace.
	 *
	 * This is the location of the bottom left block.
	 */
	private Location location = null;

	/**
	 * The x direction of the fireplace.
	 */
	private int xDirection = 0;

	/**
	 * The z direction of the fireplace.
	 */
	private int zDirection = 0;

	/**
	 * The owner of the fireplace.
	 */
	public Player owner = null;

	/**
	 * The name of the fireplace.
	 */
	public String name = null;

	/**
	 * Check if a block is a valid fireplace block.
	 */
	private static boolean isValidMaterial(Location location)
	{
		return ACCEPTED_MATERIALS.contains(location.getBlock().getType());
	}

	/**
	 * Check if a there is a fireplace starting from this block.
	 */
	private static Fireplace detectAt(Location location, int x, int z)
	{
		Location loc = location.clone();
		if (isValidMaterial(loc) && 
			isValidMaterial(loc.add(0, 1, 0)) &&
			isValidMaterial(loc.add(0, 1, 0)) &&
			isValidMaterial(loc.add(x, 0, z)) &&
			isValidMaterial(loc.add(0, 1, 0)) &&
			isValidMaterial(loc.add(x, -1, z)) &&
			isValidMaterial(loc.add(0, 1, 0)) &&
			isValidMaterial(loc.add(x, -1, z)) &&
			isValidMaterial(loc.add(0, -1, 0)) &&
			isValidMaterial(loc.add(0, -1, 0)))
		{
			Fireplace fp = new Fireplace();
			fp.location = location;
			fp.xDirection = x;
			fp.zDirection = z;
			return fp;
		}
		return null;
	}

	/**
	 * Detect a fireplace from a sign.
	 * 
	 * Returns a new Fireplace object if one could be found, or null if the block is not part of a fireplace.
	 */
	static Fireplace detect(Location location)
	{
		Fireplace fp = null;

		// Get the sign data.
		Block block = location.getBlock();
		Sign sign = (Sign) block.getState().getData();

		// Determine which block the sign was attached to.
		Location attached = block.getRelative(sign.getAttachedFace()).getLocation();

		// Detect the fireplace. This is based on the position of the sign.
		switch (sign.getFacing())
		{
			case NORTH:
				fp = detectAt(attached.clone().add(3, -1, 0), -1, 0);
				break;

			case EAST:
				fp = detectAt(attached.clone().add(0, -1, 3), 0, -1);
				break;

			case SOUTH:
				fp = detectAt(attached.clone().add(-3, -1, 0), 1, 0);
				break;

			case WEST:
				fp = detectAt(attached.clone().add(0, -1, -3), 0, 1);
				break;
		}

		return fp;
	}

	/**
	 * Print important information about a fireplace.
	 */
	public String toString()
	{
		return String.format("Fireplace %s by %s, spanning from %d, %d, %d to %d, %d, %d", 
			name, owner != null ? owner.getDisplayName() : "Unknown",
			location.getBlockX(), location.getBlockY(), location.getBlockZ(), 
			location.getBlockX() + xDirection * 3, location.getBlockY(), location.getBlockZ() + zDirection * 3);
	}
}
