/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.transport.pipes;

import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.api.core.IIconProvider;
import buildcraft.api.mj.EnumMjDevice;
import buildcraft.api.mj.EnumMjPower;
import buildcraft.api.mj.IMjExternalStorage;
import buildcraft.api.mj.IMjInternalStorage;
import buildcraft.api.mj.reference.DefaultMjInternalStorage;
import buildcraft.core.lib.inventory.ITransactor;
import buildcraft.core.lib.inventory.Transactor;
import buildcraft.core.lib.inventory.filters.StackFilter;
import buildcraft.core.lib.utils.Utils;
import buildcraft.core.proxy.CoreProxy;
import buildcraft.transport.BuildCraftTransport;
import buildcraft.transport.Pipe;
import buildcraft.transport.PipeIconProvider;
import buildcraft.transport.PipeTransportItems;
import buildcraft.transport.TransportProxy;
import buildcraft.transport.TravelingItem;
import buildcraft.transport.pipes.events.PipeEventItem;
import buildcraft.transport.utils.TransportUtils;

public class PipeItemsObsidian extends Pipe<PipeTransportItems>implements IMjExternalStorage {
    private final DefaultMjInternalStorage storage = new DefaultMjInternalStorage(256, 1, 400, 1);

    private int[] entitiesDropped;
    private int entitiesDroppedIndex = 0;

    public PipeItemsObsidian(Item item) {
        super(new PipeTransportItems(), item);

        entitiesDropped = new int[32];
        Arrays.fill(entitiesDropped, -1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIconProvider getIconProvider() {
        return BuildCraftTransport.instance.pipeIconProvider;
    }

    @Override
    public int getIconIndex(EnumFacing direction) {
        return PipeIconProvider.TYPE.PipeItemsObsidian.ordinal();
    }

    @Override
    public void onEntityCollidedWithBlock(Entity entity) {
        super.onEntityCollidedWithBlock(entity);

        if (entity.isDead) {
            return;
        }

        if (canSuck(entity, 0)) {
            pullItemIntoPipe(entity, 0);
        }
    }

    private AxisAlignedBB getSuckingBox(EnumFacing orientation, int distance) {
        if (orientation == null) {
            return null;
        }
        Vec3 p1 = Utils.convert(container.getPos());
        Vec3 p2 = p1;

        switch (orientation) {
            case EAST:
                p1 = p1.addVector(distance, 0, 0);
                p2 = p2.addVector(distance + 1, 0, 0);
                break;
            case WEST:
                p1 = p1.addVector(-distance - 1, 0, 0);
                p2 = p2.addVector(-distance, 0, 0);
                break;
            case UP:
            case DOWN:
                p1 = p1.addVector(distance + 1, 0, distance + 1);
                p2 = p2.addVector(-distance, 0, -distance);
                break;
            case SOUTH:
                p1 = p1.addVector(0, 0, distance);
                p2 = p2.addVector(0, 0, distance + 1);
                break;
            case NORTH:
            default:
                p1 = p1.addVector(0, 0, -distance - 1);
                p2 = p2.addVector(0, 0, -distance);
                break;
        }

        switch (orientation) {
            case EAST:
            case WEST:
                p1 = p1.addVector(0, distance + 1, distance + 1);
                p2 = p2.addVector(0, -distance, -distance);
                break;
            case UP:
                p1 = p1.addVector(0, distance + 1, 0);
                p2 = p2.addVector(0, distance, 0);
                break;
            case DOWN:
                p1 = p1.addVector(0, -distance - 1, 0);
                p2 = p2.addVector(0, -distance, 0);
                break;
            case SOUTH:
            case NORTH:
            default:
                p1 = p1.addVector(distance + 1, distance + 1, 0);
                p2 = p2.addVector(-distance, -distance, 0);
                break;
        }

        Vec3 min = Utils.min(p1, p2);
        Vec3 max = Utils.max(p1, p2);

        return new AxisAlignedBB(min.xCoord, min.yCoord, min.zCoord, max.xCoord, max.yCoord, max.zCoord);
    }

    @Override
    public void update() {
        super.update();
        storage.tick(getWorld());

        if (storage.currentPower() > 0) {
            for (int j = 1; j < 5; ++j) {
                if (suckItem(j)) {
                    return;
                }
            }
        }
    }

    private boolean suckItem(int distance) {
        AxisAlignedBB box = getSuckingBox(getOpenOrientation(), distance);

        if (box == null) {
            return false;
        }

        @SuppressWarnings("unchecked")
        List<Entity> discoveredEntities = container.getWorld().getEntitiesWithinAABB(Entity.class, box);

        for (Entity entity : discoveredEntities) {
            if (canSuck(entity, distance)) {
                pullItemIntoPipe(entity, distance);
                return true;
            }

            if (distance == 1 && entity instanceof EntityMinecartChest) {
                EntityMinecartChest cart = (EntityMinecartChest) entity;
                if (!cart.isDead) {
                    ITransactor trans = Transactor.getTransactorFor(cart);
                    EnumFacing openOrientation = getOpenOrientation();
                    ItemStack stack = trans.remove(StackFilter.ALL, openOrientation, false);

                    if (stack != null && storage.extractPower(getWorld(), 1, 1, false) > 0) {
                        trans.remove(StackFilter.ALL, openOrientation, true);
                        EntityItem entityitem = new EntityItem(container.getWorld(), cart.posX, cart.posY + 0.3F, cart.posZ, stack);
                        entityitem.setDefaultPickupDelay();
                        container.getWorld().spawnEntityInWorld(entityitem);
                        pullItemIntoPipe(entityitem, 1);

                        return true;
                    }
                }
            }
        }

        return false;
    }

    public void pullItemIntoPipe(Entity entity, int distance) {
        if (container.getWorld().isRemote) {
            return;
        }

        EnumFacing orientation = getOpenOrientation().getOpposite();

        if (orientation != null) {
            container.getWorld().playSoundAtEntity(entity, "random.pop", 0.2F, ((container.getWorld().rand.nextFloat() - container.getWorld().rand
                    .nextFloat()) * 0.7F + 1.0F) * 2.0F);

            ItemStack stack = null;

            double speed = 0.01F;

            if (entity instanceof EntityItem) {
                EntityItem item = (EntityItem) entity;
                ItemStack contained = item.getEntityItem();

                if (contained == null) {
                    return;
                }

                TransportProxy.proxy.obsidianPipePickup(container.getWorld(), item, this.container);

                double energyUsed = Math.min(contained.stackSize * distance, storage.currentPower());

                if (distance == 0 || energyUsed / distance == contained.stackSize) {
                    stack = contained;
                    CoreProxy.proxy.removeEntity(entity);
                } else {
                    stack = contained.splitStack((int) (energyUsed / distance));
                }

                speed = Math.sqrt(item.motionX * item.motionX + item.motionY * item.motionY + item.motionZ * item.motionZ);
                speed = speed / 2F - 0.05;

                if (speed < 0.01) {
                    speed = 0.01;
                }
            } else if (entity instanceof EntityArrow && storage.extractPower(getWorld(), distance, distance, false) > 0) {
                stack = new ItemStack(Items.arrow, 1);
                CoreProxy.proxy.removeEntity(entity);
            } else {
                return;
            }

            if (stack == null) {
                return;
            }

            TravelingItem item = TravelingItem.make(Utils.convert(container.getPos()).addVector(0.5, TransportUtils.getPipeFloorOf(stack), 0.5),
                    stack);

            item.setSpeed((float) speed);

            transport.injectItem(item, orientation);
        }
    }

    public void eventHandler(PipeEventItem.DropItem event) {
        if (entitiesDroppedIndex + 1 >= entitiesDropped.length) {
            entitiesDroppedIndex = 0;
        } else {
            entitiesDroppedIndex++;
        }
        entitiesDropped[entitiesDroppedIndex] = event.entity.getEntityId();
    }

    public boolean canSuck(Entity entity, int distance) {
        if (!entity.isEntityAlive()) {
            return false;
        }
        if (entity instanceof EntityItem) {
            EntityItem item = (EntityItem) entity;

            if (item.getEntityItem().stackSize <= 0) {
                return false;
            }

            for (int element : entitiesDropped) {
                if (item.getEntityId() == element) {
                    return false;
                }
            }

            return storage.currentPower() >= distance;
        } else if (entity instanceof EntityArrow) {
            EntityArrow arrow = (EntityArrow) entity;
            return arrow.canBePickedUp == 1 && storage.currentPower() >= distance;
        }
        return false;
    }

    // IMjExternalHandler

    @Override
    public EnumMjDevice getDeviceType(EnumFacing side) {
        return EnumMjDevice.MACHINE;
    }

    @Override
    public EnumMjPower getPowerType(EnumFacing side) {
        return EnumMjPower.REDSTONE;
    }

    @Override
    public double extractPower(World world, EnumFacing flowDirection, IMjExternalStorage to, double minMj, double maxMj, boolean simulate) {
        return 0;
    }

    @Override
    public double insertPower(World world, EnumFacing flowDirection, IMjExternalStorage from, double mj, boolean simulate) {
        if (mj < 0) {
            return mj;
        }
        return storage.insertPower(getWorld(), mj, simulate);
    }

    @Override
    public double getSuction(World world, EnumFacing flowDirection) {
        return 0.75;
    }

    @Override
    public void setInternalStorage(IMjInternalStorage storage) {}

    @Override
    public double currentPower(EnumFacing side) {
        return storage.currentPower();
    }

    @Override
    public double maxPower(EnumFacing side) {
        return storage.maxPower();
    }
}
