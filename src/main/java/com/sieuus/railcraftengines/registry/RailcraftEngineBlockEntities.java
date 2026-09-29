package com.sieuus.railcraftengines.registry;

import com.sieuus.railcraftengines.RailcraftEngines;
import com.sieuus.railcraftengines.common.blocks.engine.TileEngineSteamCommercial;
import com.sieuus.railcraftengines.common.blocks.engine.TileEngineSteamHobby;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RailcraftEngineBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(
                    Registries.BLOCK_ENTITY_TYPE,
                    RailcraftEngines.MODID
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<TileEngineSteamHobby>
            > HOBBYIST_STEAM_ENGINE =
            BLOCK_ENTITIES.register(
                    "hobbyist_steam_engine",
                    () -> BlockEntityType.Builder.of(
                            TileEngineSteamHobby::new,
                            RailcraftEngineBlocks.HOBBYIST_STEAM_ENGINE.get()
                    ).build(null)
            );

    private RailcraftEngineBlockEntities() {
    }

    public static final DeferredHolder<
            BlockEntityType<?>, BlockEntityType<TileEngineSteamCommercial>
            > COMMERCIAL_STEAM_ENGINE = BLOCK_ENTITIES.register(
            "commercial_steam_engine",
            () -> BlockEntityType.Builder.of(
                    TileEngineSteamCommercial::new,
                    RailcraftEngineBlocks.COMMERCIAL_STEAM_ENGINE.get()
            ).build(null)
    );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}