package com.sieuus.railcraftengines.common.util.steam;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class SolidFuelProvider implements IFuelProvider {

    private final ItemStacksResourceHandler inventory;
    private final int fuelSlot;
    private final int outputSlot;
    private final BooleanSupplier canConsumeFuel;
    private final Supplier<Level> levelSupplier;

    public SolidFuelProvider(
            ItemStacksResourceHandler inventory,
            int fuelSlot,
            int outputSlot,
            BooleanSupplier canConsumeFuel,
            Supplier<Level> levelSupplier
    ) {
        this.inventory = Objects.requireNonNull(inventory);
        this.canConsumeFuel = Objects.requireNonNull(canConsumeFuel);
        this.levelSupplier = Objects.requireNonNull(levelSupplier);
        if (fuelSlot < 0 || fuelSlot >= inventory.size()
                || outputSlot < 0 || outputSlot >= inventory.size()
                || fuelSlot == outputSlot) {
            throw new IllegalArgumentException("Fuel and output slots must be distinct valid indices");
        }
        this.fuelSlot = fuelSlot;
        this.outputSlot = outputSlot;
    }

    @Override
    public double burnFuelUnit() {
        if (!canConsumeFuel.getAsBoolean()) {
            return 0;
        }

        Level level = levelSupplier.get();
        if (level == null || level.isClientSide()) {
            return 0;
        }

        ItemStack fuel = getStack(fuelSlot);
        if (fuel.isEmpty()) {
            return 0;
        }

        int burnTime = fuel.getBurnTime(RecipeType.SMELTING, level.fuelValues());
        if (burnTime <= 0) {
            return 0;
        }

        var remainderTemplate = fuel.copyWithCount(1).getCraftingRemainder();
        ItemStack remainder = remainderTemplate == null
                ? ItemStack.EMPTY : remainderTemplate.create();

        if (!remainder.isEmpty() && !canStoreInOutput(remainder)) {
            return 0;
        }

        // Internal machine processing runs outside transfer transactions.
        // Direct setters allow the machine to populate its output-only slot.
        inventory.set(fuelSlot, ItemResource.of(fuel), fuel.getCount() - 1);
        if (!remainder.isEmpty()) {
            ItemStack current = getStack(outputSlot);
            inventory.set(outputSlot, ItemResource.of(remainder),
                    current.getCount() + remainder.getCount());
        }

        return burnTime;
    }

    @Override
    public boolean needsFuel() {
        return inventory.getAmountAsInt(fuelSlot) < 8;
    }

    private ItemStack getStack(int slot) {
        return inventory.getResource(slot).toStack(inventory.getAmountAsInt(slot));
    }

    private boolean canStoreInOutput(ItemStack stack) {
        ItemStack current = getStack(outputSlot);
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) {
            return false;
        }

        long capacity = Math.min(stack.getMaxStackSize(),
                inventory.getCapacityAsLong(outputSlot, ItemResource.of(stack)));
        return (long) current.getCount() + stack.getCount() <= capacity;
    }
}
