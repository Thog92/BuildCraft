/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.transport.pipes;

import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.api.core.IIconProvider;
import buildcraft.core.lib.utils.MathUtils;
import buildcraft.transport.BuildCraftTransport;
import buildcraft.transport.PipeIconProvider;
import buildcraft.transport.TransportConstants;
import buildcraft.transport.TravelingItem;
import buildcraft.transport.internal.pipes.Pipe;
import buildcraft.transport.internal.pipes.PipeTransportItems;

public class PipeItemsGold extends Pipe<PipeTransportItems> {

    public PipeItemsGold(Item item) {
        super(new PipeTransportItems(), item);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIconProvider getIconProvider() {
        return BuildCraftTransport.instance.pipeIconProvider;
    }

    @Override
    public int getIconIndex(EnumFacing direction) {
        return PipeIconProvider.TYPE.PipeItemsGold.ordinal();
    }

    public void eventHandler(PipeEventItem.AdjustSpeed event) {
        event.handled = true;
        TravelingItem item = event.item;
        item.setSpeed(MathUtils.clamp(item.getSpeed() * 4F, TransportConstants.PIPE_NORMAL_SPEED * 4F, TransportConstants.PIPE_NORMAL_SPEED * 15F));
    }
}