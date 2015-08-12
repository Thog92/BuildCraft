package buildcraft.api.mj;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class DefaultMjInternalStorage implements IMjInternalStorage {
    // TODO (PASS 1): Tweak DefaultMjInternalStorage values to tested ones.
    public static final long MACHNE_LOSS_DELAY = 600;
    public static final double MACHINE_LOSS_RATE = 1;

    public static final long TRANSPORT_LOSS_DELAY = 80;
    public static final double TRANSPORT_LOSS_RATE = 0.5;

    public static final long ENGINE_LOSS_DELAY = 300;
    public static final double ENGINE_LOSS_RATE = 0.2;

    private final double maxPower, activationPower, lossRate;
    private final long lossDelay;
    private double power;
    private boolean on = false;
    private long lastRecievedPower = -1;

    /** @param maxPower The maximum amount of power that this can store.
     * @param activationPower The minimum amount of power needed to activate.
     * @param lossDelay The number of ticks to wait before starting to lose power.
     * @param lossRate The amount of power to lose per tick when it should start losing power. */
    public DefaultMjInternalStorage(double maxPower, double activationPower, long lossDelay, double lossRate) {
        this.maxPower = maxPower;
        this.activationPower = activationPower;
        this.lossDelay = lossDelay;
        this.lossRate = lossRate;
    }

    @Override
    public double currentPower() {
        return power;
    }

    @Override
    public double maxPower() {
        return maxPower;
    }

    @Override
    public double takePower(World world, double min, double max, boolean simulate) {
        if (power < min) {
            return 0;
        }
        double toTake = Math.min(max, power);
        if (!simulate) {
            power -= toTake;
            if (power == 0) {
                on = false;
            }
        }
        return toTake;
    }

    @Override
    public double givePower(World world, double mj, boolean simulate) {
        if (power == maxPower) {
            return mj;
        }
        double space = maxPower - power;
        double accepted = Math.min(space, mj);
        double excess = mj - accepted;
        if (!simulate) {
            power += accepted;
            lastRecievedPower = world.getTotalWorldTime();
        }
        return excess;
    }

    @Override
    public boolean tick(World world) {
        if (power == 0 && on) {
            on = false;
            return false;
        }

        long currentTick = world.getTotalWorldTime();
        if (lastRecievedPower == -1) {
            // Essentially an initialisation step
            lastRecievedPower = currentTick;
        } else if (lastRecievedPower + lossDelay < currentTick) {
            // Enough time has passed, start losing power
            power -= lossRate;
            if (power < 0) {
                power = 0;
            }
        }

        return power > activationPower || on;
    }

    @Override
    public NBTTagCompound writeToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setDouble("mj", power);
        nbt.setLong("lastRecievedPower", lastRecievedPower);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        power = nbt.getDouble("mj");
        lastRecievedPower = nbt.getLong("lastRecievedPower");
    }
}
