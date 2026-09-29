package com.sieuus.railcraftengines;

import com.mojang.logging.LogUtils;
import com.sieuus.railcraftengines.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;


@Mod(RailcraftEngines.MODID)
public final class RailcraftEngines {

    public static final String MODID = "railcraftengines";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RailcraftEngines(IEventBus modEventBus, ModContainer modContainer) {
        RailcraftEngineBlocks.register(modEventBus);
        RailcraftEngineItems.register(modEventBus);
        RailcraftEngineBlockEntities.register(modEventBus);
        RailcraftEngineMenus.MENUS.register(modEventBus);
        RailcraftEngineCreativeTabs.register(modEventBus);

        modEventBus.addListener(RailcraftEngineCapabilities::register);
    }
}