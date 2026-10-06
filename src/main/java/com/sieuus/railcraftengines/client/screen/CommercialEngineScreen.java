/*
 * Portions derived from Railcraft by CovertJaguar.
 * Original project: https://github.com/Railcraft/Railcraft
 * Adapted for Minecraft 26.1.2 / NeoForge by sieuus.
 */

package com.sieuus.railcraftengines.client.screen;

import com.sieuus.railcraftengines.RailcraftEngines;
import com.sieuus.railcraftengines.common.menu.CommercialEngineMenu;
import com.sieuus.railcraftengines.integration.railcraft.RailcraftFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.List;

public class CommercialEngineScreen
        extends AbstractContainerScreen<CommercialEngineMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            RailcraftEngines.MODID,
            "textures/gui/gui_engine_steam.png"
    );

    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    public CommercialEngineScreen(
            CommercialEngineMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);
        inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE, leftPos, topPos,
                0, 0, imageWidth, imageHeight,
                TEXTURE_WIDTH, TEXTURE_HEIGHT
        );

        drawSteamGauge(graphics);

        int energyHeight = scaled(menu.getEnergyStored(), menu.getMaxEnergy(), 43);
        if (energyHeight > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + 94, topPos + 25 + 43 - energyHeight,
                    176, 47 + 43 - energyHeight,
                    6, energyHeight,
                    TEXTURE_WIDTH, TEXTURE_HEIGHT
            );
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(
                font,
                Math.round(menu.getCurrentOutput()) + " FE/t",
                120, 40, 0xFF404040, false
        );
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        // The superclass already schedules the inventory slot tooltips.
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        Component tooltip = null;
        if (isHovering(71, 23, 16, 47, mouseX, mouseY)) {
            tooltip = Component.literal(
                    "Steam: " + menu.getSteamAmount()
                            + " / " + menu.getSteamCapacity() + " mB"
            );
        } else if (isHovering(94, 25, 6, 43, mouseX, mouseY)) {
            tooltip = Component.literal(
                    "Energy: " + menu.getEnergyStored()
                            + " / " + menu.getMaxEnergy() + " FE"
            );
        }

        if (tooltip != null) {
            graphics.setComponentTooltipForNextFrame(
                    font, List.of(tooltip), mouseX, mouseY
            );
        }
    }

    private static int scaled(int amount, int capacity, int height) {
        if (capacity <= 0) return 0;
        return (int) Math.clamp((long) amount * height / capacity, 0L, (long) height);
    }

    private void drawSteamGauge(GuiGraphicsExtractor graphics) {
        int height = scaled(menu.getSteamAmount(), menu.getSteamCapacity(), 47);
        Fluid steam = RailcraftFluids.getSteam();
        if (height <= 0 || steam == Fluids.EMPTY) return;

        FluidModel model = Minecraft.getInstance()
                .getModelManager()
                .getFluidStateModelSet()
                .get(steam.defaultFluidState());

        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        var tintSource = model.fluidTintSource();
        int color = tintSource == null
                ? 0xFFFFFFFF
                : tintSource.color(steam.defaultFluidState());

        int x = leftPos + 71;
        int y = topPos + 23;
        graphics.enableScissor(x, y + 47 - height, x + 16, y + 47);
        try {
            for (int offset = 0; offset < 47; offset += 16) {
                graphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        sprite, x, y + offset, 16, 16, color
                );
            }
        } finally {
            graphics.disableScissor();
        }

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE, x, y, 176, 0, 16, 47,
                TEXTURE_WIDTH, TEXTURE_HEIGHT
        );
    }
}
