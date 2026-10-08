/*
 * Portions of this file are derived from the Railcraft project.
 *
 * Railcraft copyright (c) CovertJaguar.
 * Original project:
 * https://github.com/Railcraft/Railcraft
 *
 * Legacy source branch: mc-1.7.10
 * Adapted for Minecraft 26.1.2 / NeoForge by sieuus.
 */

package com.sieuus.railcraftengines.common.blocks.engine;

import com.sieuus.railcraftengines.common.util.steam.SteamConstants;
import com.sieuus.railcraftengines.integration.railcraft.RailcraftFluids;
import mods.railcraft.sounds.RailcraftSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public abstract class TileEngineSteam extends TileEngine {

    public static final int DEFAULT_STEAM_TANK_CAPACITY =
            8 * FluidType.BUCKET_VOLUME;

    protected final FluidStacksResourceHandler steamTank;
    private final int steamTankCapacity;

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

        this.steamTankCapacity = steamTankCapacity;
        this.steamTank = new FluidStacksResourceHandler(1, steamTankCapacity) {
            @Override
            public boolean isValid(int index, FluidResource resource) {
                return index == 0 && !resource.isEmpty()
                        && isSteamValidForTank(resource.toStack(1));
            }

            @Override
            protected void onContentsChanged(int index, FluidStack previousContents) {
                TileEngineSteam.this.setChanged();
            }
        };
    }

    protected boolean isSteamValidForTank(FluidStack stack) {
        return RailcraftFluids.isSteam(stack);
    }

    public FluidStack getSteamStack() {
        return steamTank.getResource(0).toStack(steamTank.getAmountAsInt(0));
    }

    public int getSteamAmount() {
        return steamTank.getAmountAsInt(0);
    }

    public int getSteamCapacity() {
        return steamTankCapacity;
    }

    protected int consumeSteam(int amount) {
        if (amount <= 0 || getSteamAmount() == 0) {
            return 0;
        }

        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = steamTank.extract(
                    0, steamTank.getResource(0), amount, transaction);
            transaction.commit();
            return extracted;
        }
    }

    @Override
    protected void burn() {
        long output = 0L;

        if (getEnergyStage() != EnergyStage.OVERHEAT) {
            if (isPowered()) {
                FluidStack steam = getSteamStack();

                int minimumSteam =
                        getSteamCapacity() / 2
                                - SteamConstants.STEAM_PER_UNIT_WATER;

                if (!steam.isEmpty()
                        && steam.getAmount() >= minimumSteam) {

                    steamUsed += consumeSteam(steamUsedPerTick() - 1);
                }
            }

            steamUsed += consumeSteam(1);

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
        consumeSteam(5);
    }

    public FluidStacksResourceHandler getSteamTank() {
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        // Preserve the existing save format while changing the tank implementation.
        FluidStack storedSteam = input.childOrEmpty("SteamTank")
                .read("Fluid", FluidStack.CODEC).orElse(FluidStack.EMPTY);
        steamTank.set(0, FluidResource.of(storedSteam),
                Math.min(storedSteam.getAmount(), steamTankCapacity));
        steamUsed = input.getIntOr("SteamUsed", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        ValueOutput tankOutput = output.child("SteamTank");
        FluidStack storedSteam = getSteamStack();
        if (!storedSteam.isEmpty()) {
            tankOutput.store("Fluid", FluidStack.CODEC, storedSteam);
        }
        output.putInt("SteamUsed", steamUsed);
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
                basePitch + level.getRandom().nextGaussian() * 0.1);

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
