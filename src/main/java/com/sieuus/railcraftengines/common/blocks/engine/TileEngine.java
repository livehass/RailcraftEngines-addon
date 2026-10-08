/*
 * Portions of this file are derived from the Railcraft project.
 *
 * Railcraft copyright (c) CovertJaguar.
 * Original project:
 * https://github.com/Railcraft/Railcraft
 *
 * Adapted for Minecraft 26.1.2 / NeoForge by sieuus.
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
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;


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

    public void resetEnergyStage() {
        if (level == null || level.isClientSide()) {
            return;
        }

        setEnergyStage(computeEnergyStage());
    }

    private EnergyHandler getEnergyReceiver() {
        if (level == null) {
            return null;
        }

        Direction direction = getFacing();

        BlockPos targetPos =
                worldPosition.relative(direction);

        return level.getCapability(
                Capabilities.Energy.BLOCK,
                targetPos,
                direction.getOpposite()
        );
    }

    private boolean canOutputEnergy() {
        EnergyHandler receiver = getEnergyReceiver();
        if (receiver == null) {
            return false;
        }

        // Closing without committing rolls back this acceptance check.
        try (Transaction transaction = Transaction.openRoot()) {
            return receiver.insert(1, transaction) > 0;
        }
    }

    private void pushEnergyToFacing() {
        EnergyHandler receiver = getEnergyReceiver();
        if (receiver == null) {
            return;
        }

        int requested = (int) Math.min(Integer.MAX_VALUE,
                Math.min(getEnergyStored(), getMaxEnergyOutput()));
        if (requested <= 0) {
            return;
        }

        // The receiver and the engine balance participate in the same transaction.
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = receiver.insert(requested, transaction);
            if (inserted <= 0) {
                return;
            }

            energyJournal.updateSnapshots(transaction);
            energyStored -= inserted;
            transaction.commit();
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

    private void readClientSyncTag(ValueInput input) {
        active = input.getBooleanOr("Active", false);
        powered = input.getBooleanOr("Powered", false);
        energyStage = EnergyStage.fromOrdinal(input.getIntOr("EnergyStage", 0));
    }
    private final SnapshotJournal<Long> energyJournal = new SnapshotJournal<>() {
        @Override
        protected Long createSnapshot() {
            return energyStored;
        }

        @Override
        protected void revertToSnapshot(Long snapshot) {
            energyStored = snapshot;
        }

        @Override
        protected void onRootCommit(Long originalState) {
            updateEnergyStage();
            setChanged();
        }
    };

    // Energy is pushed by the piston; external insertion and extraction stay blocked.
    private final EnergyHandler energyConnection = new EnergyHandler() {
        @Override
        public int insert(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            return 0;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            return 0;
        }

        @Override
        public long getAmountAsLong() {
            return TileEngine.this.getEnergyStored();
        }

        @Override
        public long getCapacityAsLong() {
            return TileEngine.this.getMaxEnergy();
        }
    };

    public EnergyHandler getEnergyConnection() {
        return energyConnection;
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        return createClientSyncTag();
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        readClientSyncTag(input);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ValueInput input) {
        readClientSyncTag(input);
    }
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        energyStored = input.getLongOr("EnergyStored", 0L);
        currentOutput = input.getDoubleOr("CurrentOutput", 0.0D);
        powered = input.getBooleanOr("Powered", false);
        active = input.getBooleanOr("Active", false);
        energyStage = EnergyStage.fromOrdinal(input.getIntOr("EnergyStage", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putLong("EnergyStored", energyStored);
        output.putDouble("CurrentOutput", currentOutput);
        output.putBoolean("Powered", powered);
        output.putBoolean("Active", active);
        output.putInt("EnergyStage", energyStage.ordinal());
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
