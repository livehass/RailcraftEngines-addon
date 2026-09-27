package com.sieuus.railcraftengines.common.menu;

import com.sieuus.railcraftengines.common.blocks.engine.TileEngineSteamHobby;
import com.sieuus.railcraftengines.registry.RailcraftEngineMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class HobbyistEngineMenu extends AbstractContainerMenu {

    private static final int ENGINE_SLOT_COUNT = 3;

    private final TileEngineSteamHobby engine;

    public HobbyistEngineMenu(
            int containerId,
            Inventory playerInventory
    ) {
        this(
                containerId,
                playerInventory,
                null,
                new ItemStackHandler(ENGINE_SLOT_COUNT)
        );
    }

    public HobbyistEngineMenu(
            int containerId,
            Inventory playerInventory,
            TileEngineSteamHobby engine
    ) {
        this(
                containerId,
                playerInventory,
                engine,
                engine.getInventory()
        );
    }

    private HobbyistEngineMenu(
            int containerId,
            Inventory playerInventory,
            TileEngineSteamHobby engine,
            ItemStackHandler engineInventory
    ) {
        super(
                RailcraftEngineMenus.HOBBYIST_ENGINE.get(),
                containerId
        );

        this.engine = engine;

        addSlot(
                new SlotItemHandler(
                        engineInventory,
                        TileEngineSteamHobby.SLOT_FUEL,
                        62,
                        39
                )
        );

        addSlot(
                new SlotItemHandler(
                        engineInventory,
                        TileEngineSteamHobby.SLOT_LIQUID_INPUT,
                        143,
                        21
                )
        );

        addSlot(
                new SlotItemHandler(
                        engineInventory,
                        TileEngineSteamHobby.SLOT_LIQUID_OUTPUT,
                        143,
                        56
                )
        );

        addPlayerInventory(playerInventory);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(
                        new Slot(
                                inventory,
                                column + row * 9 + 9,
                                8 + column * 18,
                                84 + row * 18
                        )
                );
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(
                    new Slot(
                            inventory,
                            column,
                            8 + column * 18,
                            142
                    )
            );
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (engine == null) {
            return true;
        }

        if (engine.getLevel() == null) {
            return false;
        }

        if (engine.getLevel().getBlockEntity(
                engine.getBlockPos()
        ) != engine) {
            return false;
        }

        return player.distanceToSqr(
                engine.getBlockPos().getX() + 0.5D,
                engine.getBlockPos().getY() + 0.5D,
                engine.getBlockPos().getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {
        Slot slot = slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < ENGINE_SLOT_COUNT) {
            if (!moveItemStackTo(
                    stack,
                    ENGINE_SLOT_COUNT,
                    slots.size(),
                    true
            )) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(
                    stack,
                    0,
                    ENGINE_SLOT_COUNT,
                    false
            )) {
                return ItemStack.EMPTY;
            }
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
}