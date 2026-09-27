package com.sieuus.railcraftengines.common.util.steam;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.function.BooleanSupplier;

public class SolidFuelProvider implements IFuelProvider {

    private final ItemStackHandler inventory;
    private final int fuelSlot;
    private final int outputSlot;
    private final BooleanSupplier canConsumeFuel;

    public SolidFuelProvider(
            ItemStackHandler inventory,
            int fuelSlot,
            int outputSlot,
            BooleanSupplier canConsumeFuel
    ) {
        this.inventory = inventory;
        this.fuelSlot = fuelSlot;
        this.outputSlot = outputSlot;
        this.canConsumeFuel = canConsumeFuel;
    }

    @Override
    public double burnFuelUnit() {
        if (!canConsumeFuel.getAsBoolean()) {
            return 0;
        }

        ItemStack fuel = inventory.getStackInSlot(fuelSlot);

        if (fuel.isEmpty()) {
            return 0;
        }

        int burnTime = fuel.getBurnTime(RecipeType.SMELTING);

        if (burnTime <= 0) {
            return 0;
        }

        ItemStack singleFuel = fuel.copyWithCount(1);
        ItemStack remainder = singleFuel.getCraftingRemainingItem();

        if (!remainder.isEmpty() && !canStoreInOutput(remainder)) {
            return 0;
        }

        inventory.extractItem(
                fuelSlot,
                1,
                false
        );

        if (!remainder.isEmpty()) {
            storeInOutput(remainder);
        }

        return burnTime;
    }

    @Override
    public boolean needsFuel() {
        ItemStack fuel = inventory.getStackInSlot(fuelSlot);

        return fuel.isEmpty() || fuel.getCount() < 8;
    }

    private boolean canStoreInOutput(ItemStack stack) {
        ItemStack current =
                inventory.getStackInSlot(outputSlot);

        if (current.isEmpty()) {
            return true;
        }

        return ItemStack.isSameItemSameComponents(
                current,
                stack
        ) && current.getCount() + stack.getCount()
                <= current.getMaxStackSize();
    }

    private void storeInOutput(ItemStack stack) {
        ItemStack current =
                inventory.getStackInSlot(outputSlot);

        if (current.isEmpty()) {
            inventory.setStackInSlot(
                    outputSlot,
                    stack.copy()
            );
            return;
        }

        ItemStack result = current.copy();
        result.grow(stack.getCount());

        inventory.setStackInSlot(
                outputSlot,
                result
        );
    }
}