package com.sieuus.railcraftengines.common.menu;

import com.sieuus.railcraftengines.common.blocks.engine.TileEngineSteamHobby;
import com.sieuus.railcraftengines.registry.RailcraftEngineMenus;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import net.minecraft.world.item.crafting.RecipeType;

public final class HobbyistEngineMenu extends AbstractContainerMenu {

    private static final int ENGINE_SLOT_COUNT = 3;
    private static final int DATA_COUNT = 11;

    private static final int WATER = 0;
    private static final int WATER_CAPACITY = 1;
    private static final int STEAM = 2;
    private static final int STEAM_CAPACITY = 3;
    private static final int TEMPERATURE = 4;
    private static final int MAX_TEMPERATURE = 5;
    private static final int FUEL_PROGRESS = 6;
    private static final int HAS_FUEL = 7;
    private static final int ENERGY = 8;
    private static final int MAX_ENERGY = 9;
    private static final int OUTPUT = 10;

    private final TileEngineSteamHobby engine;
    private final ContainerData data;

    // Client-side constructor.
    public HobbyistEngineMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null,
                new ItemStacksResourceHandler(ENGINE_SLOT_COUNT),
                new SimpleContainerData(DATA_COUNT));
    }

    // Server-side constructor.
    public HobbyistEngineMenu(int containerId, Inventory playerInventory,
                              TileEngineSteamHobby engine) {
        this(containerId, playerInventory, engine, engine.getInventory(),
                createData(engine));
    }

    private HobbyistEngineMenu(int containerId, Inventory playerInventory,
                               TileEngineSteamHobby engine,
                               ItemStacksResourceHandler inventory, ContainerData data) {
        super(RailcraftEngineMenus.HOBBYIST_ENGINE.get(), containerId);
        this.engine = engine;
        this.data = data;

        checkContainerDataCount(data, DATA_COUNT);

        addSlot(new ResourceHandlerSlot(inventory, inventory::set,
                TileEngineSteamHobby.SLOT_FUEL, 62, 39) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.isEmpty() && stack.getBurnTime(
                        RecipeType.SMELTING,
                        playerInventory.player.level().fuelValues()) > 0;
            }
        });

        addSlot(new ResourceHandlerSlot(inventory, inventory::set,
                TileEngineSteamHobby.SLOT_LIQUID_INPUT, 143, 21) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return containsWater(stack);
            }
        });

        addSlot(new ResourceHandlerSlot(inventory, inventory::set,
                TileEngineSteamHobby.SLOT_LIQUID_OUTPUT, 143, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 84 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column,
                    8 + column * 18, 142));
        }

        addDataSlots(data);
    }

    private static ContainerData createData(TileEngineSteamHobby engine) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                var boiler = engine.getBoiler();

                return switch (index) {
                    case WATER -> boiler.getWaterAmount();
                    case WATER_CAPACITY -> boiler.getWaterCapacity();
                    case STEAM -> engine.getSteamAmount();
                    case STEAM_CAPACITY -> engine.getSteamCapacity();
                    case TEMPERATURE ->
                            (int) Math.round(boiler.getTemperature() * 10);
                    case MAX_TEMPERATURE ->
                            (int) Math.round(boiler.getMaxTemperature() * 10);
                    case FUEL_PROGRESS -> {
                        double total = boiler.getCurrentItemBurnTime();
                        double ratio = total > 0
                                ? boiler.getBurnTime() / total : 0;
                        yield (int) Math.round(
                                Math.clamp(ratio, 0.0, 1.0) * 1000);
                    }
                    case HAS_FUEL -> boiler.getBurnTime() > 0 ? 1 : 0;
                    case ENERGY -> boundedInt(engine.getEnergyStored());
                    case MAX_ENERGY -> boundedInt(engine.getMaxEnergy());
                    case OUTPUT ->
                            boundedInt(Math.round(engine.currentOutput * 100));
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Server-side values are read directly from the engine.
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    private static int boundedInt(long value) {
        return (int) Math.clamp(value, 0L, (long) Integer.MAX_VALUE);
    }

    private static boolean containsWater(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ItemStacksResourceHandler container = new ItemStacksResourceHandler(1);
        container.set(0, ItemResource.of(stack), 1);
        ResourceHandler<FluidResource> handler =
                ItemAccess.forHandlerIndexStrict(container, 0)
                        .getCapability(Capabilities.Fluid.ITEM);

        if (handler == null) {
            return false;
        }

        for (int tank = 0; tank < handler.size(); tank++) {
            if (handler.getAmountAsLong(tank) > 0
                    && handler.getResource(tank).toStack(1).is(FluidTags.WATER)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        if (engine == null) {
            return true;
        }

        if (engine.getLevel() == null
                || engine.getLevel().getBlockEntity(engine.getBlockPos())
                != engine) {
            return false;
        }

        return player.distanceToSqr(
                engine.getBlockPos().getX() + 0.5,
                engine.getBlockPos().getY() + 0.5,
                engine.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < ENGINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, ENGINE_SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, ENGINE_SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return original;
    }

    public TileEngineSteamHobby getEngine() {
        return engine;
    }

    public int getWaterAmount() {
        return data.get(WATER);
    }

    public int getWaterCapacity() {
        return data.get(WATER_CAPACITY);
    }

    public int getSteamAmount() {
        return data.get(STEAM);
    }

    public int getSteamCapacity() {
        return data.get(STEAM_CAPACITY);
    }

    public double getTemperature() {
        return data.get(TEMPERATURE) / 10.0;
    }

    public double getMaxTemperature() {
        return data.get(MAX_TEMPERATURE) / 10.0;
    }

    public int getFuelProgressScaled(int pixels) {
        return data.get(FUEL_PROGRESS) * pixels / 1000;
    }

    public boolean hasFuel() {
        return data.get(HAS_FUEL) != 0;
    }

    public int getEnergyStored() {
        return data.get(ENERGY);
    }

    public int getMaxEnergy() {
        return data.get(MAX_ENERGY);
    }

    public double getCurrentOutput() {
        return data.get(OUTPUT) / 100.0;
    }
}
