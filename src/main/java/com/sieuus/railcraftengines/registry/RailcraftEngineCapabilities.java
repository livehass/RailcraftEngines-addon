package com.sieuus.railcraftengines.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class RailcraftEngineCapabilities {

    private RailcraftEngineCapabilities() {
    }

    public static void register(
            RegisterCapabilitiesEvent event
    ) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                RailcraftEngineBlockEntities.HOBBYIST_STEAM_ENGINE.get(),
                (engine, side) -> engine.getInventory()
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                RailcraftEngineBlockEntities.HOBBYIST_STEAM_ENGINE.get(),
                (engine, side) -> {
                    if (side != null
                            && side == engine.getFacing()) {
                        return null;
                    }

                    return engine.getFluidInputHandler();
                }
        );
    }
}