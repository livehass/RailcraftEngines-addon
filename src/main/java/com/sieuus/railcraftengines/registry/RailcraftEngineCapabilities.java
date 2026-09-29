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
                (engine, side) -> side == engine.getFacing()
                        ? null
                        : engine.getAutomationInventory()
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

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                RailcraftEngineBlockEntities.HOBBYIST_STEAM_ENGINE.get(),
                (engine, side) -> side == engine.getFacing()
                        ? engine.getEnergyConnection()
                        : null
        );


        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                RailcraftEngineBlockEntities.COMMERCIAL_STEAM_ENGINE.get(),
                (engine, side) -> side == engine.getFacing()
                        ? null
                        : engine.getFluidInputHandler()
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                RailcraftEngineBlockEntities.COMMERCIAL_STEAM_ENGINE.get(),
                (engine, side) -> side == engine.getFacing()
                        ? engine.getEnergyConnection()
                        : null
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                RailcraftEngineBlockEntities.INDUSTRIAL_STEAM_ENGINE.get(),
                (engine, side) -> side == engine.getFacing()
                        ? null
                        : engine.getFluidInputHandler()
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                RailcraftEngineBlockEntities.INDUSTRIAL_STEAM_ENGINE.get(),
                (engine, side) -> side == engine.getFacing()
                        ? engine.getEnergyConnection()
                        : null
        );


    }
}