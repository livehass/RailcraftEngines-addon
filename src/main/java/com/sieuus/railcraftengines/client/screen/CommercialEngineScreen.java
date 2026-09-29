/*
 * Portions derived from Railcraft by CovertJaguar.
 * Original project: https://github.com/Railcraft/Railcraft
 * Adapted for Minecraft 1.21.1 / NeoForge by sieuus.
 */

package com.sieuus.railcraftengines.client.screen;

import com.sieuus.railcraftengines.RailcraftEngines;
import com.sieuus.railcraftengines.common.menu.CommercialEngineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.sieuus.railcraftengines.integration.railcraft.RailcraftFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

public class CommercialEngineScreen
        extends AbstractContainerScreen<CommercialEngineMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    RailcraftEngines.MODID,
                    "textures/gui/gui_engine_steam.png"
            );

    public CommercialEngineScreen(
            CommercialEngineMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics, float partialTick, int mouseX, int mouseY
    ) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        try {
            graphics.blit(
                    TEXTURE, leftPos, topPos,
                    0, 0, imageWidth, imageHeight
            );

            drawSteamGauge(graphics);

            int energyHeight = scaled(
                    menu.getEnergyStored(),
                    menu.getMaxEnergy(),
                    43
            );

            if (energyHeight > 0) {
                graphics.blit(
                        TEXTURE,
                        leftPos + 94,
                        topPos + 25 + 43 - energyHeight,
                        176,
                        47 + 43 - energyHeight,
                        6,
                        energyHeight
                );
            }
        } finally {
            RenderSystem.disableBlend();
        }
    }

    @Override
    protected void renderLabels(
            GuiGraphics graphics, int mouseX, int mouseY
    ) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(
                font,
                Math.round(menu.getCurrentOutput()) + " FE/t",
                120, 40, 0x404040, false
        );
    }

    @Override
    public void render(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick
    ) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        if (isHovering(71, 23, 16, 47, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.literal(
                            "Steam: " + menu.getSteamAmount()
                                    + " / " + menu.getSteamCapacity() + " mB"
                    ),
                    mouseX, mouseY
            );
        } else if (isHovering(94, 25, 6, 43, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.literal(
                            "Energy: " + menu.getEnergyStored()
                                    + " / " + menu.getMaxEnergy() + " FE"
                    ),
                    mouseX, mouseY
            );
        }
    }

    private static int scaled(int amount, int capacity, int height) {
        if (capacity <= 0) return 0;
        return (int) Math.clamp((long) amount * height / capacity, 0L, height);
    }

    private void drawSteamGauge(GuiGraphics graphics) {
        int height = scaled(
                menu.getSteamAmount(),
                menu.getSteamCapacity(),
                47
        );

        Fluid steam = RailcraftFluids.getSteam();

        if (height <= 0 || steam == Fluids.EMPTY) return;

        IClientFluidTypeExtensions extensions =
                IClientFluidTypeExtensions.of(steam);

        ResourceLocation stillTexture = extensions.getStillTexture();
        if (stillTexture == null) return;

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(stillTexture);

        int color = extensions.getTintColor();
        float red = ((color >> 16) & 255) / 255.0F;
        float green = ((color >> 8) & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        float alpha = ((color >>> 24) & 255) / 255.0F;

        int x = leftPos + 71;
        int y = topPos + 23;

        graphics.enableScissor(
                x, y + 47 - height,
                x + 16, y + 47
        );

        try {
            graphics.setColor(red, green, blue, alpha);

            for (int offset = 0; offset < 47; offset += 16) {
                graphics.blit(
                        x, y + offset,
                        0, 16, 16, sprite
                );
            }
        } finally {
            graphics.setColor(1, 1, 1, 1);
            graphics.disableScissor();
        }

        graphics.blit(TEXTURE, x, y, 176, 0, 16, 47);
    }
}