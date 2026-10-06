package com.sieuus.railcraftengines.client.screen;

import com.sieuus.railcraftengines.RailcraftEngines;
import com.sieuus.railcraftengines.common.menu.HobbyistEngineMenu;
import com.sieuus.railcraftengines.integration.railcraft.RailcraftFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;

import java.util.Locale;

public final class HobbyistEngineScreen
        extends AbstractContainerScreen<HobbyistEngineMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(
                    RailcraftEngines.MODID,
                    "textures/gui/gui_engine_hobby.png");

    public HobbyistEngineScreen(HobbyistEngineMenu menu,
                                Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX,
                                  int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0,
                imageWidth, imageHeight, 256, 256);

        drawFluidGauge(graphics, RailcraftFluids.getSteam(),
                menu.getSteamAmount(), menu.getSteamCapacity(), 17, 23);

        drawFluidGauge(graphics, Fluids.WATER,
                menu.getWaterAmount(), menu.getWaterCapacity(), 107, 23);

        drawIndicator(graphics, 40, 25, 176, 61,
                menu.getTemperature(), menu.getMaxTemperature());

        drawIndicator(graphics, 94, 25, 182, 61,
                menu.getEnergyStored(), menu.getMaxEnergy());

        if (menu.hasFuel()) {
            int scale = menu.getFuelProgressScaled(12);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + 62, topPos + 34 - scale,
                    176, 59 - scale, 14, scale + 2, 256, 256);
        }
    }

    private void drawIndicator(GuiGraphicsExtractor graphics, int x, int y,
                               int u, int v, double value, double maximum) {
        int height = scaled(value, maximum, 43);
        if (height <= 0) {
            return;
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + x, topPos + y + 43 - height,
                u, v + 43 - height, 6, height, 256, 256);
    }

    private void drawFluidGauge(GuiGraphicsExtractor graphics, Fluid fluid,
                                int amount, int capacity, int x, int y) {
        int height = scaled(amount, capacity, 47);
        if (fluid == Fluids.EMPTY || height <= 0) {
            return;
        }

        FluidModel fluidModel = Minecraft.getInstance()
                .getModelManager()
                .getFluidStateModelSet()
                .get(fluid.defaultFluidState());
        TextureAtlasSprite sprite = fluidModel.stillMaterial().sprite();
        int color = fluidModel.fluidTintSource() != null
                ? fluidModel.fluidTintSource().colorAsStack(new FluidStack(fluid, 1))
                : 0xFFFFFFFF;

        int screenX = leftPos + x;
        int screenY = topPos + y;

        graphics.enableScissor(screenX, screenY + 47 - height,
                screenX + 16, screenY + 47);

        try {
            for (int offset = 0; offset < 47; offset += 16) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite,
                        screenX, screenY + offset, 16, 16, color);
            }
        } finally {
            graphics.disableScissor();
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, screenX, screenY, 176, 0, 16, 47, 256, 256);
    }

    private static int scaled(double value, double maximum, int pixels) {
        if (maximum <= 0) {
            return 0;
        }

        return (int) Math.floor(
                Math.clamp(value / maximum, 0.0, 1.0) * pixels);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Component name = Component.translatable(
                "block.railcraftengines.hobbyist_steam_engine");

        graphics.text(font, name,
                (imageWidth - font.width(name)) / 2, 6, 0xFF404040, false);

        graphics.text(font, playerInventoryTitle,
                8, imageHeight - 94, 0xFF404040, false);

        graphics.text(font,
                Math.round(menu.getCurrentOutput()) + " FE/t",
                55, 60, 0xFF404040, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        Component tooltip = null;

        if (isHovering(17, 23, 16, 47, mouseX, mouseY)) {
            tooltip = Component.literal("Internal steam: "
                    + menu.getSteamAmount() + " / "
                    + menu.getSteamCapacity() + " mB");
        } else if (isHovering(107, 23, 16, 47, mouseX, mouseY)) {
            tooltip = Component.literal("Water: "
                    + menu.getWaterAmount() + " / "
                    + menu.getWaterCapacity() + " mB");
        } else if (isHovering(40, 25, 6, 43, mouseX, mouseY)) {
            tooltip = Component.literal(String.format(Locale.ROOT,
                    "Temperature: %.1f / %.1f °C",
                    menu.getTemperature(), menu.getMaxTemperature()));
        } else if (isHovering(94, 25, 6, 43, mouseX, mouseY)) {
            tooltip = Component.literal("Energy: "
                    + menu.getEnergyStored() + " / "
                    + menu.getMaxEnergy() + " FE");
        } else if (isHovering(62, 22, 14, 14, mouseX, mouseY)) {
            tooltip = Component.literal("Remaining Fuel: "
                    + menu.getFuelProgressScaled(100) + "%");
        } else if (isHovering(55, 60, 38, 9, mouseX, mouseY)) {
            tooltip = Component.literal(String.format(Locale.ROOT,
                    "Average generation: %.2f FE/t", menu.getCurrentOutput()));
        }

        if (tooltip != null) {
            graphics.setTooltipForNextFrame(tooltip, mouseX, mouseY);
        }
    }
}
