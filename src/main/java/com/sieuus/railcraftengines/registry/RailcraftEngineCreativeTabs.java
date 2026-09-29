package com.sieuus.railcraftengines.registry;

import com.sieuus.railcraftengines.RailcraftEngines;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RailcraftEngineCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    RailcraftEngines.MODID
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENGINES =
            TABS.register("engines", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.railcraftengines"))
                    .icon(() -> RailcraftEngineItems.HOBBYIST_STEAM_ENGINE
                            .get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(RailcraftEngineItems.HOBBYIST_STEAM_ENGINE.get());
                        output.accept(RailcraftEngineItems.COMMERCIAL_STEAM_ENGINE.get());
                        output.accept(RailcraftEngineItems.INDUSTRIAL_STEAM_ENGINE.get());
                    })
                    .build());

    private RailcraftEngineCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}