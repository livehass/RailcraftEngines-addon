/*
 * Portions of this file are derived from the Railcraft project.
 *
 * Railcraft copyright (c) CovertJaguar.
 * Original project:
 * https://github.com/Railcraft/Railcraft
 *
 * Adapted for Minecraft 1.21.1 / NeoForge by sieuus.
 * See the project documentation for license and attribution details.
 */

package com.sieuus.railcraftengines.common.blocks.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

public abstract class TileEngine extends BlockEntity {

    public double currentOutput;

    private float pistonProgress;
    private int pistonStage;

    private boolean powered;
    private boolean active;

    private long energyStored;

    private EnergyStage energyStage = EnergyStage.BLUE;

    protected TileEngine(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    public static void tick(
            Level level,
            BlockPos pos,
            BlockState state,
            TileEngine engine
    ) {
        if (level.isClientSide()) {
            engine.clientTick();
        } else {
            engine.serverTick();
        }
    }
    private float getClientPistonSpeed() {
        return switch (energyStage) {
            case BLUE -> 0.01F;
            case GREEN -> 0.02F;
            case YELLOW -> 0.04F;
            case ORANGE -> 0.08F;
            case RED -> 0.16F;
            case OVERHEAT -> 0.0F;
        };
    }


    protected void clientTick() {
        if (pistonStage != 0) {
            pistonProgress += getClientPistonSpeed();

            if (pistonProgress > 0.5F && pistonStage == 1) {
                pistonStage = 2;
                playSoundOut();
            } else if (pistonProgress >= 1.0F) {
                pistonStage = 0;
                pistonProgress = 0.0F;
                playSoundIn();
            }
        } else if (active) {
            pistonStage = 1;
        }
    }

    protected void serverTick() {
        if (level == null) {
            return;
        }

        setPowered(
                level.hasNeighborSignal(worldPosition)
        );

        if (!isPowered() && getEnergyStored() > 1L) {
            extractEnergy(1L);
        }

        if (getEnergyStage() == EnergyStage.OVERHEAT) {
            overheat();
        } else if (pistonStage != 0) {
            pistonProgress += getServerPistonSpeed();

            if (pistonProgress > 0.5F
                    && pistonStage == 1) {

                pistonStage = 2;

                pushEnergyToFacing();

            } else if (pistonProgress >= 1.0F) {

                pistonProgress = 0.0F;
                pistonStage = 0;
            }

        } else if (isPowered()) {

            if (getEnergyStored() > 0
                    && canOutputEnergy()) {

                pistonStage = 1;
                setActive(true);

            } else {

                setActive(false);
            }

        } else {

            setActive(false);
        }

        burn();
    }

    private float getServerPistonSpeed() {
        return (float) Math.max(
                0.16D * getEnergyLevel(),
                0.01D
        );
    }

    protected void burn() {
    }

    protected void overheat() {
        extractEnergy(50L);
    }
    protected void playSoundIn() {
    }

    protected void playSoundOut() {
    }

    public Direction getFacing() {
        BlockState state = getBlockState();

        if (state.hasProperty(BlockEngine.FACING)) {
            return state.getValue(BlockEngine.FACING);
        }

        return Direction.UP;
    }

    public void setFacing(Direction facing) {
        if (level == null) {
            return;
        }

        BlockState state = getBlockState();

        if (!state.hasProperty(BlockEngine.FACING)) {
            return;
        }

        if (state.getValue(BlockEngine.FACING) == facing) {
            return;
        }

        level.setBlock(
                worldPosition,
                state.setValue(BlockEngine.FACING, facing),
                Block.UPDATE_ALL
        );

        level.invalidateCapabilities(worldPosition);

        setChanged();
    }

    public long getEnergyStored() {
        return energyStored;
    }

    public abstract long getMaxEnergy();

    public abstract long getMaxEnergyOutput();

    protected void addEnergy(long amount) {
        if (amount <= 0) {
            return;
        }

        energyStored = Math.min(
                getMaxEnergy(),
                energyStored + amount
        );

        updateEnergyStage();
        setChanged();
    }

    protected long extractEnergy(long amount) {
        if (amount <= 0) {
            return 0;
        }

        long extracted = Math.min(
                energyStored,
                amount
        );

        energyStored -= extracted;

        updateEnergyStage();
        setChanged();

        return extracted;
    }

    public double getEnergyLevel() {
        if (getMaxEnergy() <= 0) {
            return 0.0D;
        }

        return energyStored / (double) getMaxEnergy();
    }

    protected void updateEnergyStage() {
        if (energyStage == EnergyStage.OVERHEAT) {
            return;
        }

        setEnergyStage(
                computeEnergyStage()
        );
    }

    protected EnergyStage computeEnergyStage() {
        double energyLevel = getEnergyLevel();

        if (energyLevel < 0.2D) {
            return EnergyStage.BLUE;
        }

        if (energyLevel < 0.4D) {
            return EnergyStage.GREEN;
        }

        if (energyLevel < 0.6D) {
            return EnergyStage.YELLOW;
        }

        if (energyLevel < 0.8D) {
            return EnergyStage.ORANGE;
        }

        if (energyLevel < 1.0D) {
            return EnergyStage.RED;
        }

        return EnergyStage.OVERHEAT;
    }

    protected void resetEnergyStage() {
        setEnergyStage(
                computeEnergyStage()
        );
    }

    private IEnergyStorage getEnergyReceiver() {
        if (level == null) {
            return null;
        }

        Direction direction = getFacing();

        BlockPos targetPos =
                worldPosition.relative(direction);

        return level.getCapability(
                Capabilities.EnergyStorage.BLOCK,
                targetPos,
                direction.getOpposite()
        );
    }

    private boolean canOutputEnergy() {
        IEnergyStorage receiver = getEnergyReceiver();

        return receiver != null
                && receiver.canReceive();
    }

    private void pushEnergyToFacing() {
        IEnergyStorage receiver = getEnergyReceiver();

        if (receiver == null
                || !receiver.canReceive()) {
            return;
        }

        long available = Math.min(
                getEnergyStored(),
                getMaxEnergyOutput()
        );

        if (available <= 0) {
            return;
        }

        int requested = (int) Math.min(
                Integer.MAX_VALUE,
                available
        );

        int accepted = receiver.receiveEnergy(
                requested,
                true
        );

        if (accepted <= 0) {
            return;
        }

        long extracted = extractEnergy(accepted);

        if (extracted <= 0) {
            return;
        }

        int inserted = receiver.receiveEnergy(
                (int) extracted,
                false
        );

        if (inserted < extracted) {
            addEnergy(extracted - inserted);
        }
    }

    public float getProgress() {
        return pistonProgress;
    }

    public boolean isActive() {
        return active;
    }

    protected void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;

            setChanged();
            syncToClient();
        }
    }

    public boolean isPowered() {
        return powered;
    }

    protected void setPowered(boolean powered) {
        if (this.powered != powered) {
            this.powered = powered;

            setChanged();
            syncToClient();
        }
    }

    public EnergyStage getEnergyStage() {
        return energyStage;
    }

    protected void setEnergyStage(EnergyStage energyStage) {
        if (this.energyStage != energyStage) {
            this.energyStage = energyStage;

            setChanged();
            syncToClient();
        }
    }
    private void syncToClient() {
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockState state = getBlockState();

        level.sendBlockUpdated(
                worldPosition,
                state,
                state,
                Block.UPDATE_CLIENTS
        );
    }

    private CompoundTag createClientSyncTag() {
        CompoundTag tag = new CompoundTag();

        tag.putBoolean("Active", active);
        tag.putBoolean("Powered", powered);
        tag.putInt("EnergyStage", energyStage.ordinal());

        return tag;
    }

    private void readClientSyncTag(CompoundTag tag) {
        if (tag.contains("Active")) {
            active = tag.getBoolean("Active");
        }

        if (tag.contains("Powered")) {
            powered = tag.getBoolean("Powered");
        }

        if (tag.contains("EnergyStage")) {
            energyStage = EnergyStage.fromOrdinal(
                    tag.getInt("EnergyStage")
            );
        }
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        return createClientSyncTag();
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        readClientSyncTag(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries
    ) {
        readClientSyncTag(packet.getTag());
    }
    @Override
    public void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);

        energyStored = tag.getLong("EnergyStored");
        currentOutput = tag.getDouble("CurrentOutput");
        powered = tag.getBoolean("Powered");
        active = tag.getBoolean("Active");

        energyStage = EnergyStage.fromOrdinal(
                tag.getInt("EnergyStage")
        );
    }

    @Override
    public void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(tag, registries);

        tag.putLong("EnergyStored", energyStored);
        tag.putDouble("CurrentOutput", currentOutput);
        tag.putBoolean("Powered", powered);
        tag.putBoolean("Active", active);
        tag.putInt("EnergyStage", energyStage.ordinal());
    }

    public enum EnergyStage {
        BLUE,
        GREEN,
        YELLOW,
        ORANGE,
        RED,
        OVERHEAT;

        private static final EnergyStage[] VALUES = values();

        public static EnergyStage fromOrdinal(int ordinal) {
            if (ordinal < 0 || ordinal >= VALUES.length) {
                return BLUE;
            }

            return VALUES[ordinal];
        }
    }
}