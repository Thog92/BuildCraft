/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.core.blueprints;

import java.util.LinkedList;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import buildcraft.api.blueprints.IBuilderContext;
import buildcraft.api.blueprints.MappingRegistry;
import buildcraft.api.blueprints.SchematicBlockBase;
import buildcraft.api.blueprints.SchematicFactory;
import buildcraft.api.blueprints.SchematicMask;
import buildcraft.api.core.Position;

public class BuildingSlotBlock extends BuildingSlot implements Comparable<BuildingSlotBlock> {

	public int x, y, z;
	public SchematicBlockBase schematic;

	public enum Mode {
		ClearIfInvalid, Build
	};

	public Mode mode = Mode.Build;

	@Override
	public SchematicBlockBase getSchematic () {
		if (schematic == null) {
			return new SchematicMask(false);
		} else {
			return schematic;
		}
	}

	@Override
	public void writeToWorld(IBuilderContext context) {
		if (mode == Mode.ClearIfInvalid) {
			if (!getSchematic().isAlreadyBuilt(context, x, y, z)) {
				context.world().setBlockToAir(x, y, z);
			}
		} else {
			try {
				getSchematic().writeToWorld(context, x, y, z, stackConsumed);

				// Once the schematic has been written, we're going to issue
				// calls
				// to various functions, in particular updating the tile entity.
				// If these calls issue problems, in order to avoid corrupting
				// the world, we're logging the problem and setting the block to
				// air.

				TileEntity e = context.world().getTileEntity(x, y, z);

				if (e != null) {
					e.updateEntity();
				}
			} catch (Throwable t) {
				t.printStackTrace();
				context.world().setBlockToAir(x, y, z);
			}
		}
	}

	@Override
	public void postProcessing (IBuilderContext context) {
		getSchematic().postProcessing(context, x, y, z);
	}

	@Override
	public LinkedList<ItemStack> getRequirements (IBuilderContext context) {
		if (mode == Mode.ClearIfInvalid) {
			return new LinkedList<ItemStack>();
		} else {
			return getSchematic().getRequirements(context);
		}
	}

	@Override
	public int compareTo(BuildingSlotBlock o) {
		if (o.schematic instanceof Comparable && schematic instanceof Comparable ) {
			Comparable comp1 = (Comparable) schematic;
			Comparable comp2 = (Comparable) o.schematic;

			int cmp = comp1.compareTo(comp2);

			if (cmp != 0) {
				return cmp;
			}
		}

		if (y < o.y) {
			return -1;
		} else if (y > o.y) {
			return 1;
		} else if (x < o.x) {
			return -1;
		} else if (x > o.x) {
			return 1;
		} else if (z < o.z) {
			return -1;
		} else if (z > o.z) {
			return 1;
		} else {
			return 0;
		}
	}

	@Override
	public Position getDestination () {
		return new Position (x + 0.5, y + 0.5, z + 0.5);
	}

	@Override
	public void writeCompleted (IBuilderContext context, double complete) {
		getSchematic().writeCompleted(context, x, y, z, complete);
	}

	@Override
	public boolean isAlreadyBuilt(IBuilderContext context) {
		return schematic.isAlreadyBuilt(context, x, y, z);
	}

	@Override
	public void writeToNBT (NBTTagCompound nbt, MappingRegistry registry) {
		nbt.setByte("mode", (byte) mode.ordinal());
		nbt.setInteger("x", x);
		nbt.setInteger("y", y);
		nbt.setInteger("z", z);

		NBTTagCompound schematicNBT = new NBTTagCompound();
		SchematicFactory.getFactory(schematic.getClass())
				.saveSchematicToWorldNBT(schematicNBT, schematic, registry);
		nbt.setTag("schematic", schematicNBT);
	}

	@Override
	public void readFromNBT (NBTTagCompound nbt, MappingRegistry registry) {
		mode = Mode.values() [nbt.getByte("mode")];
		x = nbt.getInteger("x");
		y = nbt.getInteger("y");
		z = nbt.getInteger("z");

		schematic = (SchematicBlockBase) SchematicFactory
				.createSchematicFromWorldNBT(nbt, registry);
	}
}