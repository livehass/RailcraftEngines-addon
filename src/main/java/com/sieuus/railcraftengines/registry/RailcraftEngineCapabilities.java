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
                Capabilities.Item.BLOCK,
                RailcraftEngineBlockEntities.HOBBYIST_STEAM_ENGINE.get(),
                (engine, side) -> side == engine.getFacing()
                        ? null
                        : engine.getAutomationInventory()
        );

        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                RailcraftEngineBlockEntities.HOBBYIST_STEAM_ENGINE.get(),
                (engine, side) -> side == engine.getFacing()
                        ? null
                        : engine.getFluidInputHandler()
        );

        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                RailcraftEngineBlockEntities.HOBBYIST_STEAM_ENGINE.get(),
                (engine, side) -> side == engine.getFacing()
                        ? engine.getEnergyConnection()
                        : null
        );

        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                RailcraftEngineBlockEntities.COMMERCIAL_STEAM_ENGINE.get(),
                (engine, side) -> side == engine.getFacing()
                        ? null
                        : engine.getFluidInputHandler()
        );

        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                RailcraftEngineBlockEntities.COMMERCIAL_STEAM_ENGINE.get(),
                (engine, side) -> side == engine.getFacing()
                        ? engine.getEnergyConnection()
                        : null
        );

        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                RailcraftEngineBlockEntities.INDUSTRIAL_STEAM_ENGINE.get(),
                (engine, side) -> side == engine.getFacing()
                        ? null
                        : engine.getFluidInputHandler()
        );

        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                RailcraftEngineBlockEntities.INDUSTRIAL_STEAM_ENGINE.get(),
                (engine, side) -> side == engine.getFacing()
                        ? engine.getEnergyConnection()
                        : null
        );
    }
}