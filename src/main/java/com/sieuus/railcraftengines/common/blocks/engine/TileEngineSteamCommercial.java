/*
 * Portions derived from Railcraft by CovertJaguar.
 * Original project: https://github.com/Railcraft/Railcraft
 * Reference branch: mc-1.7.10
 * Adapted for Minecraft 26.1.2 / NeoForge by sieuus.
 */

package com.sieuus.railcraftengines.common.blocks.engine;

import com.sieuus.railcraftengines.common.menu.CommercialEngineMenu;
import com.sieuus.railcraftengines.common.util.steam.SteamConstants;
import com.sieuus.railcraftengines.integration.railcraft.RailcraftFluids;
import com.sieuus.railcraftengines.registry.RailcraftEngineBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class TileEngineSteamCommercial extends TileEngineSteam
        implements MenuProvider {

    public static final int OUTPUT_FE = 40;

    private static final TagKey<Fluid> STEAM_TAG = TagKey.create(
            Registries.FLUID,
            Identifier.fromNamespaceAndPath("c", "steam")
    );

    private final ResourceHandler<FluidResource> fluidInput =
            new DelegatingResourceHandler<FluidResource>(steamTank) {
                @Override
                public int insert(int index, FluidResource resource, int amount,
                                  TransactionContext transaction) {
                    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
                    int tank = convertIndex(index);
                    return isPowered()
                            ? steamTank.insert(tank, resource, amount, transaction)
                            : 0;
                }

                @Override
                public int insert(FluidResource resource, int amount,
                                  TransactionContext transaction) {
                    return insert(0, resource, amount, transaction);
                }

                @Override
                public int extract(int index, FluidResource resource, int amount,
                                   TransactionContext transaction) {
                    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
                    convertIndex(index);
                    return 0;
                }

                @Override
                public int extract(FluidResource resource, int amount,
                                   TransactionContext transaction) {
                    return extract(0, resource, amount, transaction);
                }
            };

    public TileEngineSteamCommercial(BlockPos pos, BlockState state) {
        super(
                RailcraftEngineBlockEntities.COMMERCIAL_STEAM_ENGINE.get(),
                pos,
                state
        );
    }

    @Override
    protected boolean isSteamValidForTank(FluidStack stack) {
        return !stack.isEmpty()
                && (stack.is(STEAM_TAG) || RailcraftFluids.isSteam(stack));
    }

    public ResourceHandler<FluidResource> getFluidInputHandler() {
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
