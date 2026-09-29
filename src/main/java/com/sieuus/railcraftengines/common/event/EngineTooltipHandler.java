package com.sieuus.railcraftengines.common.event;

import com.sieuus.railcraftengines.RailcraftEngines;
import com.sieuus.railcraftengines.registry.RailcraftEngineItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(
        modid = RailcraftEngines.MODID,
        value = Dist.CLIENT
)
public final class EngineTooltipHandler {

    private EngineTooltipHandler() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        List<Component> description = new ArrayList<>();

        if (stack.is(RailcraftEngineItems.HOBBYIST_STEAM_ENGINE.get())) {
            description.add(line("output", 20));
            description.add(line("hobbyist_fuel"));
            description.add(line("internal_steam"));
        } else if (stack.is(
                RailcraftEngineItems.COMMERCIAL_STEAM_ENGINE.get()
        )) {
            description.add(line("output", 40));
            description.add(line("steam_consumption", 20));
        } else {
            return;
        }

        description.add(line("redstone"));

        var tooltip = event.getToolTip();


        tooltip.addAll(Math.min(1, tooltip.size()), description);

        tooltip.add(
                Component.translatable("tooltip.railcraftengines.mod_name")
                        .withStyle(
                                ChatFormatting.BLUE,
                                ChatFormatting.ITALIC
                        )
        );
    }

    private static Component line(String key, Object... arguments) {
        return Component.translatable(
                "tooltip.railcraftengines." + key,
                arguments
        ).withStyle(ChatFormatting.GRAY);
    }
}