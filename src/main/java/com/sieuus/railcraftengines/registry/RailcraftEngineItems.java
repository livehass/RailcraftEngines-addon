package com.sieuus.railcraftengines.registry;

import com.sieuus.railcraftengines.RailcraftEngines;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RailcraftEngineItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(RailcraftEngines.MODID);

    public static final DeferredItem<BlockItem> HOBBYIST_STEAM_ENGINE =
            ITEMS.registerSimpleBlockItem(
                    RailcraftEngineBlocks.HOBBYIST_STEAM_ENGINE
            );

    private RailcraftEngineItems() {
    }

    public static final DeferredItem<BlockItem> COMMERCIAL_STEAM_ENGINE =
            ITEMS.registerSimpleBlockItem(
                    RailcraftEngineBlocks.COMMERCIAL_STEAM_ENGINE
            );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}