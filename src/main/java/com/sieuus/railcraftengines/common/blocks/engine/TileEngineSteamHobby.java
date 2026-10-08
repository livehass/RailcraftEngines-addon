/*
 * Portions of this file are derived from the Railcraft project.
 *
 * Railcraft copyright (c) CovertJaguar.
 * Original project:
 * https://github.com/Railcraft/Railcraft
 *
 * Adapted for Minecraft 26.1.2 / NeoForge by sieuus.
 */

package com.sieuus.railcraftengines.common.blocks.engine;

import com.sieuus.railcraftengines.common.blocks.logic.BoilerLogic;
import com.sieuus.railcraftengines.common.blocks.logic.BoilerLogic.BoilerData;
import com.sieuus.railcraftengines.common.menu.HobbyistEngineMenu;
import com.sieuus.railcraftengines.common.util.steam.SolidFuelProvider;
import com.sieuus.railcraftengines.common.util.steam.SteamConstants;
import com.sieuus.railcraftengines.registry.RailcraftEngineBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.minecraft.world.Containers;
import net.minecraft.world.ItemStackWithSlot;
import java.util.Objects;

public final class TileEngineSteamHobby
        extends TileEngineSteam
        implements MenuProvider {

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

    private final ResourceHandler<FluidResource> fluidInputHandler;

    private final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(3) {
        @Override
        public boolean isValid(int slot, ItemResource resource) {
            ItemStack stack = resource.toStack();
            return switch (slot) {
                case SLOT_FUEL -> isValidFuel(stack);
                case SLOT_LIQUID_INPUT -> containsWater(stack);
                default -> false;
            };
        }

        @Override
        protected void onContentsChanged(int slot, ItemStack previousContents) {
            TileEngineSteamHobby.this.setChanged();
        }
    };

    private final ResourceHandler<ItemResource> automationInventory =
            new DelegatingResourceHandler<ItemResource>(inventory) {
                @Override
                public int extract(int slot, ItemResource resource, int amount,
                                   TransactionContext transaction) {
                    Objects.checkIndex(slot, size());
                    return slot == SLOT_LIQUID_OUTPUT
                            ? inventory.extract(slot, resource, amount, transaction) : 0;
                }

                @Override
                public int extract(ItemResource resource, int amount,
                                   TransactionContext transaction) {
                    return extract(SLOT_LIQUID_OUTPUT, resource, amount, transaction);
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
                        && getEnergyStage() != EnergyStage.OVERHEAT,
                () -> level
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

        // Only water is exposed to pipes. Steam remains internal to this engine.
        this.fluidInputHandler = new DelegatingResourceHandler<FluidResource>(
                () -> boiler.getWaterTank()) {
            @Override
            public boolean isValid(int index, FluidResource resource) {
                Objects.checkIndex(index, size());
                return isWater(resource) && super.isValid(index, resource);
            }

            @Override
            public int insert(int index, FluidResource resource, int amount,
                              TransactionContext transaction) {
                Objects.checkIndex(index, size());
                return isWater(resource)
                        ? super.insert(index, resource, amount, transaction) : 0;
            }

            @Override
            public int insert(FluidResource resource, int amount,
                              TransactionContext transaction) {
                return insert(0, resource, amount, transaction);
            }

            @Override
            public int extract(int index, FluidResource resource, int amount,
                               TransactionContext transaction) {
                Objects.checkIndex(index, size());
                return 0;
            }

            @Override
            public int extract(FluidResource resource, int amount,
                               TransactionContext transaction) {
                return 0;
            }
        };
    }

    public ItemStack getInventoryStack(int slot) {
        return inventory.getResource(slot).toStack(inventory.getAmountAsInt(slot));
    }

    private static boolean isWater(FluidResource resource) {
        return !resource.isEmpty() && resource.toStack(1).is(FluidTags.WATER);
    }

    private void processWaterContainer() {
        ItemStack input = getInventoryStack(SLOT_LIQUID_INPUT);
        if (input.isEmpty()) {
            return;
        }

        // Isolate one container so its capability can replace it with the result.
        ItemStacksResourceHandler container = new ItemStacksResourceHandler(1);
        container.set(0, ItemResource.of(input), 1);
        ItemAccess access = ItemAccess.forHandlerIndexStrict(container, 0);
        ResourceHandler<FluidResource> fluidHandler = access.getCapability(Capabilities.Fluid.ITEM);
        if (fluidHandler == null) {
            return;
        }

        ItemStack result;
        try (Transaction transaction = Transaction.openRoot()) {
            int transferred = ResourceHandlerUtil.move(
                    fluidHandler, fluidInputHandler, TileEngineSteamHobby::isWater,
                    FluidType.BUCKET_VOLUME, transaction);
            if (transferred <= 0) {
                return;
            }

            result = container.getResource(0).toStack(container.getAmountAsInt(0));
            if (!result.isEmpty() && !canStoreInLiquidOutput(result)) {
                return;
            }

            if (inventory.extract(SLOT_LIQUID_INPUT, ItemResource.of(input),
                    1, transaction) != 1) {
                return;
            }
            transaction.commit();
        }

        // Internal output placement bypasses the external insertion restriction.
        if (!result.isEmpty()) {
            inventory.set(SLOT_LIQUID_OUTPUT, ItemResource.of(result),
                    inventory.getAmountAsInt(SLOT_LIQUID_OUTPUT) + result.getCount());
        }
        setChanged();
    }

    private boolean canStoreInLiquidOutput(ItemStack stack) {
        ItemStack current = getInventoryStack(SLOT_LIQUID_OUTPUT);
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) {
            return false;
        }
        long capacity = Math.min(stack.getMaxStackSize(),
                inventory.getCapacityAsLong(SLOT_LIQUID_OUTPUT, ItemResource.of(stack)));
        return (long) current.getCount() + stack.getCount() <= capacity;
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

    public ItemStacksResourceHandler getInventory() {
        return inventory;
    }

    public ResourceHandler<ItemResource> getAutomationInventory() {
        return automationInventory;
    }

    public ResourceHandler<FluidResource> getFluidInputHandler() {
        return fluidInputHandler;
    }

    public ItemStack getFuelStack() {
        return getInventoryStack(SLOT_FUEL);
    }

    public boolean isValidFuel(ItemStack stack) {
        return level != null && !stack.isEmpty()
                && stack.getBurnTime(RecipeType.SMELTING, level.fuelValues()) > 0;
    }

    private static boolean containsWater(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemStacksResourceHandler container = new ItemStacksResourceHandler(1);
        container.set(0, ItemResource.of(stack), 1);
        ResourceHandler<FluidResource> handler = ItemAccess.forHandlerIndexStrict(container, 0)
                .getCapability(Capabilities.Fluid.ITEM);
        if (handler == null) {
            return false;
        }
        for (int tank = 0; tank < handler.size(); tank++) {
            if (handler.getAmountAsLong(tank) > 0 && isWater(handler.getResource(tank))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide()) {
            return;
        }
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = getInventoryStack(slot);
            if (!stack.isEmpty()) {
                inventory.set(slot, ItemResource.EMPTY, 0);
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }

    public BoilerLogic getBoiler() {
        return boiler;
    }

    public FluidStacksResourceHandler getWaterTank() {
        return boiler.getWaterTank();
    }

    public double getTemperature() {
        return boiler.getTemperature();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        // Keep the existing slot-based save format.
        for (int slot = 0; slot < inventory.size(); slot++) {
            inventory.set(slot, ItemResource.EMPTY, 0);
        }
        input.childOrEmpty("Inventory").listOrEmpty("Items", ItemStackWithSlot.CODEC)
                .forEach(entry -> {
                    if (entry.isValidInContainer(inventory.size())) {
                        inventory.set(entry.slot(), ItemResource.of(entry.stack()),
                                entry.stack().getCount());
                    }
                });
        boiler.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        ValueOutput inventoryOutput = output.child("Inventory");
        var items = inventoryOutput.list("Items", ItemStackWithSlot.CODEC);
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = getInventoryStack(slot);
            if (!stack.isEmpty()) {
                items.add(new ItemStackWithSlot(slot, stack));
            }
        }
        inventoryOutput.putInt("Size", inventory.size());
        boiler.save(output);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "container.railcraftengines.hobbyist_engine"
        );
    }

    @Override
    public AbstractContainerMenu createMenu(
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
