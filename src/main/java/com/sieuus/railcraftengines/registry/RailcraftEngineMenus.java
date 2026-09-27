package com.sieuus.railcraftengines.registry;

import com.sieuus.railcraftengines.RailcraftEngines;
import com.sieuus.railcraftengines.common.menu.HobbyistEngineMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RailcraftEngineMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(
                    Registries.MENU,
                    RailcraftEngines.MODID
            );

    public static final DeferredHolder<
            MenuType<?>,
            MenuType<HobbyistEngineMenu>
            > HOBBYIST_ENGINE =
            MENUS.register(
                    "hobbyist_engine",
                    () -> new MenuType<>(
                            HobbyistEngineMenu::new,
                            FeatureFlags.DEFAULT_FLAGS
                    )
            );

    private RailcraftEngineMenus() {
    }
}