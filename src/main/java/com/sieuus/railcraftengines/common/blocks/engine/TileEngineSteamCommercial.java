/*
 * Portions derived from Railcraft by CovertJaguar.
 * Original project: https://github.com/Railcraft/Railcraft
 * Reference branch: mc-1.7.10
 * Adapted for Minecraft 1.21.1 / NeoForge by sieuus.
 */

package com.sieuus.railcraftengines.common.blocks.engine;

import com.sieuus.railcraftengines.common.menu.CommercialEngineMenu;
import com.sieuus.railcraftengines.common.util.steam.SteamConstants;
import com.sieuus.railcraftengines.integration.railcraft.RailcraftFluids;
import com.sieuus.railcraftengines.registry.RailcraftEngineBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class TileEngineSteamCommercial extends TileEngineSteam
        implements MenuProvider {

    public static final int OUTPUT_FE = 40;

    private static final TagKey<Fluid> STEAM_TAG = TagKey.create(
            Registries.FLUID,
            ResourceLocation.fromNamespaceAndPath("c", "steam")
    );

    private final IFluidHandler fluidInput = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? steamTank.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? steamTank.getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && isAcceptedSteam(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return isPowered() ? steamTank.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    public TileEngineSteamCommercial(BlockPos pos, BlockState state) {
        super(
                RailcraftEngineBlockEntities.COMMERCIAL_STEAM_ENGINE.get(),
                pos,
                state
        );

        steamTank.setValidator(TileEngineSteamCommercial::isAcceptedSteam);
    }

    private static boolean isAcceptedSteam(FluidStack stack) {
        return !stack.isEmpty()
                && (stack.is(STEAM_TAG) || RailcraftFluids.isSteam(stack));
    }

    public IFluidHandler getFluidInputHandler() {
        return fluidInput;
    }

    @Override
    public long getMaxOutputFE() {
        return OUTPUT_FE;
    }

    @Override
    public int steamUsedPerTick() {
        return SteamConstants.STEAM_PER_10RF * (OUTPUT_FE / 10);
    }

    @Override
    public long getMaxEnergy() {
        return 200_000L;
    }

    @Override
    public long getMaxEnergyOutput() {
        return OUTPUT_FE * 8L;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "block.railcraftengines.commercial_steam_engine"
        );
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory inventory, Player player
    ) {
        return new CommercialEngineMenu(containerId, inventory, this);
    }
}