/*
 * Portions derived from Railcraft by CovertJaguar.
 * Original project: https://github.com/Railcraft/Railcraft
 * Adapted for Minecraft 1.21.1 / NeoForge by sieuus.
 */

package com.sieuus.railcraftengines.common.menu;

import com.sieuus.railcraftengines.common.blocks.engine.TileEngineSteamCommercial;
import com.sieuus.railcraftengines.registry.RailcraftEngineMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CommercialEngineMenu extends AbstractContainerMenu {

    private static final int DATA_COUNT = 6;

    private final TileEngineSteamCommercial engine;
    private final ContainerData data;

    public CommercialEngineMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public CommercialEngineMenu(
            int containerId,
            Inventory inventory,
            TileEngineSteamCommercial engine
    ) {
        super(RailcraftEngineMenus.COMMERCIAL_ENGINE.get(), containerId);
        this.engine = engine;

        this.data = engine == null
                ? new SimpleContainerData(DATA_COUNT)
                : new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> engine.getSteamTank().getFluidAmount();
                    case 1 -> engine.getSteamTank().getCapacity();
                    case 2 -> (int) engine.getEnergyStored();
                    case 3 -> (int) engine.getMaxEnergy();
                    case 4 -> (int) Math.round(engine.currentOutput * 100);
                    case 5 -> engine.getEnergyStage().ordinal();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };

        addDataSlots(data);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        inventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        84 + row * 18
                ));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    public int getSteamAmount() {
        return data.get(0);
    }

    public int getSteamCapacity() {
        return data.get(1);
    }

    public int getEnergyStored() {
        return data.get(2);
    }

    public int getMaxEnergy() {
        return data.get(3);
    }

    public double getCurrentOutput() {
        return data.get(4) / 100.0;
    }

    public int getEnergyStage() {
        return data.get(5);
    }

    @Override
    public boolean stillValid(Player player) {
        if (engine == null) return true;

        var level = engine.getLevel();
        var pos = engine.getBlockPos();

        return level != null
                && player.level() == level
                && level.getBlockEntity(pos) == engine
                && player.distanceToSqr(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        ) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;

        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < 27) {
            if (!moveItemStackTo(stack, 27, 36, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 27, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, stack);
        return original;
    }
}