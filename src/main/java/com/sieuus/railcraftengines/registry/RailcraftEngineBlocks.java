package com.sieuus.railcraftengines.registry;

import com.sieuus.railcraftengines.RailcraftEngines;
import com.sieuus.railcraftengines.common.blocks.engine.BlockEngine;
import com.sieuus.railcraftengines.common.blocks.engine.TileEngineSteamHobby;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.sieuus.railcraftengines.common.blocks.engine.TileEngineSteamCommercial;

public final class RailcraftEngineBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(RailcraftEngines.MODID);

    public static final DeferredBlock<BlockEngine> HOBBYIST_STEAM_ENGINE =
            BLOCKS.registerBlock(
                    "hobbyist_steam_engine",
                    properties -> new BlockEngine(
                            properties,
                            TileEngineSteamHobby::new
                    ),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F, 6.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            );

    private RailcraftEngineBlocks() {
    }

    public static final DeferredBlock<BlockEngine> COMMERCIAL_STEAM_ENGINE =
            BLOCKS.registerBlock(
                    "commercial_steam_engine",
                    properties -> new BlockEngine(
                            properties,
                            TileEngineSteamCommercial::new
                    ),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F, 6.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}