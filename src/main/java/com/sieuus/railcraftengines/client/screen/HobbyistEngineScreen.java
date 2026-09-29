package com.sieuus.railcraftengines.client.screen;

import com.sieuus.railcraftengines.RailcraftEngines;
import com.sieuus.railcraftengines.common.menu.HobbyistEngineMenu;
import com.sieuus.railcraftengines.integration.railcraft.RailcraftFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import java.util.Locale;

public final class HobbyistEngineScreen
        extends AbstractContainerScreen<HobbyistEngineMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    RailcraftEngines.MODID,
                    "textures/gui/gui_engine_hobby.png");

    public HobbyistEngineScreen(HobbyistEngineMenu menu,
                                Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick,
                            int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0,
                imageWidth, imageHeight);

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
            graphics.blit(TEXTURE, leftPos + 62, topPos + 34 - scale,
                    176, 59 - scale, 14, scale + 2);
        }
    }

    private void drawIndicator(GuiGraphics graphics, int x, int y,
                               int u, int v, double value, double maximum) {
        int height = scaled(value, maximum, 43);
        if (height <= 0) {
            return;
        }

        graphics.blit(TEXTURE, leftPos + x, topPos + y + 43 - height,
                u, v + 43 - height, 6, height);
    }

    private void drawFluidGauge(GuiGraphics graphics, Fluid fluid,
                                int amount, int capacity, int x, int y) {
        int height = scaled(amount, capacity, 47);
        if (fluid == Fluids.EMPTY || height <= 0) {
            return;
        }

        IClientFluidTypeExtensions extensions =
                IClientFluidTypeExtensions.of(fluid);
        ResourceLocation stillTexture = extensions.getStillTexture();

        if (stillTexture == null) {
            return;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(stillTexture);

        int color = extensions.getTintColor();
        float red = ((color >> 16) & 255) / 255.0F;
        float green = ((color >> 8) & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        float alpha = ((color >>> 24) & 255) / 255.0F;

        int screenX = leftPos + x;
        int screenY = topPos + y;

        graphics.enableScissor(screenX, screenY + 47 - height,
                screenX + 16, screenY + 47);

        try {
            graphics.setColor(red, green, blue, alpha);

            for (int offset = 0; offset < 47; offset += 16) {
                graphics.blit(screenX, screenY + offset,
                        0, 16, 16, sprite);
            }
        } finally {
            graphics.setColor(1, 1, 1, 1);
            graphics.disableScissor();
        }

        graphics.blit(TEXTURE, screenX, screenY, 176, 0, 16, 47);
    }

    private static int scaled(double value, double maximum, int pixels) {
        if (maximum <= 0) {
            return 0;
        }

        return (int) Math.floor(
                Math.clamp(value / maximum, 0.0, 1.0) * pixels);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component name = Component.translatable(
                "block.railcraftengines.hobbyist_steam_engine");

        graphics.drawString(font, name,
                (imageWidth - font.width(name)) / 2, 6, 0x404040, false);

        graphics.drawString(font, playerInventoryTitle,
                8, imageHeight - 94, 0x404040, false);

        graphics.drawString(font,
                Math.round(menu.getCurrentOutput()) + " FE/t",
                55, 60, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

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
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }
    }
}