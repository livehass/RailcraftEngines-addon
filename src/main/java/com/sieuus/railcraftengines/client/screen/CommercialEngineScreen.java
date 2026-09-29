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
        int x = leftPos;
        int y = topPos;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        try {
            graphics.blit(
                    TEXTURE, x, y, 0, 0,
                    imageWidth, imageHeight
            );

            int steamHeight = scaled(
                    menu.getSteamAmount(),
                    menu.getSteamCapacity(),
                    47
            );

            if (steamHeight > 0) {
                graphics.fill(
                        x + 71, y + 70 - steamHeight,
                        x + 87, y + 70,
                        0xFFD8D8D8
                );

                graphics.blit(
                        TEXTURE,
                        x + 71, y + 23,
                        176, 0,
                        16, 47
                );
            }

            int energyHeight = scaled(
                    menu.getEnergyStored(),
                    menu.getMaxEnergy(),
                    43
            );

            if (energyHeight > 0) {
                graphics.blit(
                        TEXTURE,
                        x + 94, y + 25 + 43 - energyHeight,
                        176, 47 + 43 - energyHeight,
                        6, energyHeight
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
}