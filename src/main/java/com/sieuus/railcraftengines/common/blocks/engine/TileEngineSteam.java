/*
 * Portions of this file are derived from the Railcraft project.
 *
 * Railcraft copyright (c) CovertJaguar.
 * Original project:
 * https://github.com/Railcraft/Railcraft
 *
 * Legacy source branch: mc-1.7.10
 * Adapted for Minecraft 1.21.1 / NeoForge by sieuus.
 */

package com.sieuus.railcraftengines.common.blocks.engine;

import com.sieuus.railcraftengines.common.util.steam.SteamConstants;
import com.sieuus.railcraftengines.integration.railcraft.RailcraftFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import mods.railcraft.sounds.RailcraftSoundEvents;
import net.minecraft.sounds.SoundSource;



public abstract class TileEngineSteam extends TileEngine {

    public static final int DEFAULT_STEAM_TANK_CAPACITY =
            8 * FluidType.BUCKET_VOLUME;

    protected final FluidTank steamTank;

    private int steamUsed;

    protected TileEngineSteam(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        this(
                type,
                pos,
                state,
                DEFAULT_STEAM_TANK_CAPACITY
        );
    }

    protected TileEngineSteam(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            int steamTankCapacity
    ) {
        super(type, pos, state);

        this.steamTank = new FluidTank(
                steamTankCapacity,
                RailcraftFluids::isSteam
        ) {
            @Override
            protected void onContentsChanged() {
                TileEngineSteam.this.setChanged();
            }
        };
    }

    @Override
    protected void burn() {
        long output = 0L;

        if (getEnergyStage() != EnergyStage.OVERHEAT) {
            if (isPowered()) {
                FluidStack steam = steamTank.getFluid();

                int minimumSteam =
                        steamTank.getCapacity() / 2
                                - SteamConstants.STEAM_PER_UNIT_WATER;

                if (!steam.isEmpty()
                        && steam.getAmount() >= minimumSteam) {

                    FluidStack drained =
                            steamTank.drain(
                                    steamUsedPerTick() - 1,
                                    IFluidHandler.FluidAction.EXECUTE
                            );

                    if (!drained.isEmpty()) {
                        steamUsed += drained.getAmount();
                    }
                }
            }

            FluidStack passiveSteam =
                    steamTank.drain(
                            1,
                            IFluidHandler.FluidAction.EXECUTE
                    );

            if (!passiveSteam.isEmpty()) {
                steamUsed += passiveSteam.getAmount();
            }

            if (isPowered()) {
                if (steamUsed >= steamUsedPerTick()) {
                    steamUsed -= steamUsedPerTick();

                    output = getMaxOutputFE();

                    addEnergy(output);
                }
            } else {
                steamUsed = 0;
                ventSteam();
            }
        }

        currentOutput =
                (currentOutput * 74.0D + output)
                        / 75.0D;
    }

    @Override
    protected void overheat() {
        super.overheat();
        ventSteam();
    }

    protected void ventSteam() {
        steamTank.drain(
                5,
                IFluidHandler.FluidAction.EXECUTE
        );
    }

    public FluidTank getSteamTank() {
        return steamTank;
    }

    public int getSteamUsed() {
        return steamUsed;
    }

    protected void setSteamUsed(int steamUsed) {
        this.steamUsed = steamUsed;
    }

    public abstract long getMaxOutputFE();

    public abstract int steamUsedPerTick();

    @Override
    public void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);

        if (tag.contains("SteamTank")) {
            steamTank.readFromNBT(
                    registries,
                    tag.getCompound("SteamTank")
            );
        }

        steamUsed = tag.getInt("SteamUsed");
    }

    @Override
    public void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(tag, registries);

        CompoundTag steamTag =
                new CompoundTag();

        steamTank.writeToNBT(
                registries,
                steamTag
        );

        tag.put(
                "SteamTank",
                steamTag
        );

        tag.putInt(
                "SteamUsed",
                steamUsed
        );
    }

    @Override
    protected void playSoundOut() {
        playSteamBurst(0.5F);
    }

    @Override
    protected void playSoundIn() {
        playSteamBurst(1.0F);
    }

    private void playSteamBurst(float basePitch) {
        if (level == null || !level.isClientSide()) {
            return;
        }

        float pitch = (float) (
                basePitch + level.random.nextGaussian() * 0.1);

        level.playLocalSound(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5,
                RailcraftSoundEvents.STEAM_BURST.get(),
                SoundSource.BLOCKS,
                0.15F,
                pitch,
                false
        );
    }
}