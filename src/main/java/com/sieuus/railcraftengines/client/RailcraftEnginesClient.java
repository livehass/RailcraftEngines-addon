package com.sieuus.railcraftengines.client;

import com.sieuus.railcraftengines.RailcraftEngines;
import com.sieuus.railcraftengines.client.render.SteamEngineRenderer;
import com.sieuus.railcraftengines.registry.RailcraftEngineBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import com.sieuus.railcraftengines.client.render.models.engine.ModelEngineBase;
import com.sieuus.railcraftengines.client.render.models.engine.ModelEngineFrame;
import com.sieuus.railcraftengines.client.render.models.engine.ModelEnginePiston;
import com.sieuus.railcraftengines.client.render.models.engine.ModelEngineTrunk;
import com.sieuus.railcraftengines.client.screen.HobbyistEngineScreen;
import com.sieuus.railcraftengines.registry.RailcraftEngineMenus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import com.sieuus.railcraftengines.client.screen.CommercialEngineScreen;
import com.sieuus.railcraftengines.client.render.SteamEngineItemRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

@EventBusSubscriber(
        modid = RailcraftEngines.MODID,
        value = Dist.CLIENT
)
public final class RailcraftEnginesClient {


    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerBlockEntityRenderer(
                RailcraftEngineBlockEntities.HOBBYIST_STEAM_ENGINE.get(),
                context -> new SteamEngineRenderer<>(context)
        );

        event.registerBlockEntityRenderer(
                RailcraftEngineBlockEntities.COMMERCIAL_STEAM_ENGINE.get(),
                context -> new SteamEngineRenderer<>(context)
        );

        event.registerBlockEntityRenderer(
                RailcraftEngineBlockEntities.INDUSTRIAL_STEAM_ENGINE.get(),
                context -> new SteamEngineRenderer<>(context)
        );

    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(
                RailcraftEngineMenus.HOBBYIST_ENGINE.get(),
                HobbyistEngineScreen::new
        );

        event.register(
                RailcraftEngineMenus.COMMERCIAL_ENGINE.get(),
                CommercialEngineScreen::new
        );
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(
            EntityRenderersEvent.RegisterLayerDefinitions event
    ) {
        event.registerLayerDefinition(
                ModelEngineBase.LAYER,
                ModelEngineBase::createLayer
        );

        event.registerLayerDefinition(
                ModelEngineFrame.LAYER,
                ModelEngineFrame::createLayer
        );

        event.registerLayerDefinition(
                ModelEnginePiston.LAYER,
                ModelEnginePiston::createLayer
        );

        event.registerLayerDefinition(
                ModelEngineTrunk.LAYER,
                ModelEngineTrunk::createLayer
        );
    }

    @SubscribeEvent
    public static void registerSpecialModelRenderers(
            RegisterSpecialModelRendererEvent event
    ) {
        event.register(
                Identifier.fromNamespaceAndPath(
                        RailcraftEngines.MODID,
                        "steam_engine"
                ),
                SteamEngineItemRenderer.Unbaked.CODEC
        );
    }


}
