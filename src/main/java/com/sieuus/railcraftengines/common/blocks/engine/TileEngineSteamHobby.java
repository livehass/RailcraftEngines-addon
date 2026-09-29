/*
 * Portions of this file are derived from the Railcraft project.
 *
 * Railcraft copyright (c) CovertJaguar.
 * Original project:
 * https://github.com/Railcraft/Railcraft
 *
 * Adapted for Minecraft 1.21.1 / NeoForge by sieuus.
 */

package com.sieuus.railcraftengines.common.blocks.engine;

import com.sieuus.railcraftengines.common.blocks.logic.BoilerLogic;
import com.sieuus.railcraftengines.common.blocks.logic.BoilerLogic.BoilerData;
import com.sieuus.railcraftengines.common.menu.HobbyistEngineMenu;
import com.sieuus.railcraftengines.common.util.steam.SolidFuelProvider;
import com.sieuus.railcraftengines.common.util.steam.SteamConstants;
import com.sieuus.railcraftengines.registry.RailcraftEngineBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public final class TileEngineSteamHobby
        extends TileEngineSteam
        implements MenuProvider {

    private static final int TANK_STEAM = 0;
    private static final int TANK_WATER = 1;

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_LIQUID_INPUT = 1;
    public static final int SLOT_LIQUID_OUTPUT = 2;

    private static final int CONTAINER_PROCESS_INTERVAL = 8;
    private static final double FUEL_PER_CONVERSION_MULTIPLIER = 1.25D;
    private static final int TICKS_PER_BOILER_CYCLE = 20;

    private static final int WATER_CAPACITY = 4 * FluidType.BUCKET_VOLUME;
    private static final int INTERNAL_STEAM_CAPACITY =
            4 * FluidType.BUCKET_VOLUME;

    private static final long OUTPUT_FE = 20L;
    private static final long MAX_ENERGY = 100_000L;
    private static final long MAX_ENERGY_OUTPUT = OUTPUT_FE * 8L;

    private final IFluidHandler fluidInputHandler;

    private final ItemStackHandler inventory = new ItemStackHandler(3) {

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_FUEL -> isValidFuel(stack);
                case SLOT_LIQUID_INPUT -> containsWater(stack);
                case SLOT_LIQUID_OUTPUT -> false;
                default -> false;
            };
        }

        @Override
        protected void onContentsChanged(int slot) {
            TileEngineSteamHobby.this.setChanged();
        }
    };

    private final BoilerLogic boiler;

    public TileEngineSteamHobby(
            BlockPos pos,
            BlockState state
    ) {
        super(
                RailcraftEngineBlockEntities.HOBBYIST_STEAM_ENGINE.get(),
                pos,
                state,
                INTERNAL_STEAM_CAPACITY
        );

        SolidFuelProvider fuelProvider = new SolidFuelProvider(
                inventory,
                SLOT_FUEL,
                SLOT_LIQUID_OUTPUT,
                () -> isPowered()
                        && getEnergyStage() != EnergyStage.OVERHEAT
        );

        this.boiler = new BoilerLogic(
                new BoilerData(
                        1,
                        TICKS_PER_BOILER_CYCLE,
                        FUEL_PER_CONVERSION_MULTIPLIER,
                        SteamConstants.MAX_HEAT_LOW,
                        WATER_CAPACITY,
                        INTERNAL_STEAM_CAPACITY
                ),
                getSteamTank(),
                fuelProvider,
                this::setChanged
        );

        this.fluidInputHandler = new IFluidHandler() {

            @Override
            public int getTanks() {
                return 2;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return switch (tank) {
                    case TANK_STEAM ->
                            getSteamTank().getFluidInTank(0);

                    case TANK_WATER ->
                            boiler.getWaterTank().getFluidInTank(0);

                    default ->
                            FluidStack.EMPTY;
                };
            }

            @Override
            public int getTankCapacity(int tank) {
                return switch (tank) {
                    case TANK_STEAM ->
                            getSteamTank().getTankCapacity(0);

                    case TANK_WATER ->
                            boiler.getWaterTank().getTankCapacity(0);

                    default ->
                            0;
                };
            }

            @Override
            public boolean isFluidValid(
                    int tank,
                    FluidStack stack
            ) {
                if (stack.isEmpty()) {
                    return false;
                }

                return switch (tank) {
                    case TANK_STEAM ->
                            getSteamTank().isFluidValid(stack);

                    case TANK_WATER ->
                            stack.is(FluidTags.WATER);

                    default ->
                            false;
                };
            }

            @Override
            public int fill(
                    FluidStack resource,
                    FluidAction action
            ) {
                if (resource.isEmpty()) {
                    return 0;
                }

                if (isPowered()
                        && getSteamTank().isFluidValid(resource)) {
                    return getSteamTank().fill(
                            resource,
                            action
                    );
                }

                if (resource.is(FluidTags.WATER)) {
                    return boiler.getWaterTank().fill(
                            resource,
                            action
                    );
                }

                return 0;
            }

            @Override
            public FluidStack drain(
                    FluidStack resource,
                    FluidAction action
            ) {
                return FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(
                    int maxDrain,
                    FluidAction action
            ) {
                return FluidStack.EMPTY;
            }
        };
    }

    private void processWaterContainer() {
        ItemStack input = inventory.getStackInSlot(
                SLOT_LIQUID_INPUT
        );

        if (input.isEmpty()) {
            return;
        }

        ItemStack singleContainer = input.copyWithCount(1);

        FluidActionResult simulated = FluidUtil.tryEmptyContainer(
                singleContainer,
                boiler.getWaterTank(),
                FluidType.BUCKET_VOLUME,
                null,
                false
        );

        if (!simulated.isSuccess()) {
            return;
        }

        ItemStack result = simulated.getResult();

        if (!result.isEmpty()
                && !canStoreInLiquidOutput(result)) {
            return;
        }

        FluidActionResult executed = FluidUtil.tryEmptyContainer(
                singleContainer,
                boiler.getWaterTank(),
                FluidType.BUCKET_VOLUME,
                null,
                true
        );

        if (!executed.isSuccess()) {
            return;
        }

        inventory.extractItem(
                SLOT_LIQUID_INPUT,
                1,
                false
        );

        ItemStack emptyContainer = executed.getResult();

        if (!emptyContainer.isEmpty()) {
            storeInLiquidOutput(emptyContainer);
        }

        setChanged();
    }

    private boolean canStoreInLiquidOutput(ItemStack stack) {
        ItemStack current = inventory.getStackInSlot(
                SLOT_LIQUID_OUTPUT
        );

        if (current.isEmpty()) {
            return true;
        }

        return ItemStack.isSameItemSameComponents(
                current,
                stack
        ) && current.getCount() + stack.getCount()
                <= current.getMaxStackSize();
    }

    private void storeInLiquidOutput(ItemStack stack) {
        ItemStack current = inventory.getStackInSlot(
                SLOT_LIQUID_OUTPUT
        );

        if (current.isEmpty()) {
            inventory.setStackInSlot(
                    SLOT_LIQUID_OUTPUT,
                    stack.copy()
            );
            return;
        }

        ItemStack merged = current.copy();
        merged.grow(stack.getCount());

        inventory.setStackInSlot(
                SLOT_LIQUID_OUTPUT,
                merged
        );
    }

    @Override
    public int steamUsedPerTick() {
        return 10;
    }

    @Override
    public long getMaxOutputFE() {
        return OUTPUT_FE;
    }

    @Override
    public long getMaxEnergy() {
        return MAX_ENERGY;
    }

    @Override
    public long getMaxEnergyOutput() {
        return MAX_ENERGY_OUTPUT;
    }

    @Override
    protected void burn() {
        super.burn();

        if (level != null
                && level.getGameTime()
                % CONTAINER_PROCESS_INTERVAL == 0) {
            processWaterContainer();
        }

        boiler.update();
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public IFluidHandler getFluidInputHandler() {
        return fluidInputHandler;
    }

    public ItemStack getFuelStack() {
        return inventory.getStackInSlot(SLOT_FUEL);
    }

    public static boolean isValidFuel(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    private static boolean containsWater(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        IFluidHandlerItem fluidHandler =
                stack.getCapability(Capabilities.FluidHandler.ITEM);

        if (fluidHandler == null) {
            return false;
        }

        for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
            FluidStack fluid = fluidHandler.getFluidInTank(tank);

            if (!fluid.isEmpty() && fluid.is(FluidTags.WATER)) {
                return true;
            }
        }

        return false;
    }

    public BoilerLogic getBoiler() {
        return boiler;
    }

    public FluidTank getWaterTank() {
        return boiler.getWaterTank();
    }

    public double getTemperature() {
        return boiler.getTemperature();
    }

    @Override
    public void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);

        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(
                    registries,
                    tag.getCompound("Inventory")
            );
        }

        boiler.load(tag, registries);
    }

    @Override
    public void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(tag, registries);

        tag.put(
                "Inventory",
                inventory.serializeNBT(registries)
        );

        boiler.save(tag, registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "container.railcraftengines.hobbyist_engine"
        );
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int containerId,
            Inventory playerInventory,
            Player player
    ) {
        return new HobbyistEngineMenu(
                containerId,
                playerInventory,
                this
        );
    }
}