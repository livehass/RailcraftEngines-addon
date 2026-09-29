package com.sieuus.railcraftengines.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sieuus.railcraftengines.common.blocks.engine.BlockEngine;
import com.sieuus.railcraftengines.common.blocks.engine.TileEngineSteam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

public final class SteamEngineItemRenderer
        extends BlockEntityWithoutLevelRenderer {

    private final Map<Block, TileEngineSteam> previews = new HashMap<>();

    public SteamEngineItemRenderer() {
        super(
                Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels()
        );
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        if (!(blockItem.getBlock() instanceof BlockEngine block)) return;

        TileEngineSteam preview = previews.computeIfAbsent(block, key -> {
            var blockEntity = block.newBlockEntity(
                    BlockPos.ZERO,
                    block.defaultBlockState()
            );

            return blockEntity instanceof TileEngineSteam engine
                    ? engine
                    : null;
        });

        if (preview == null) return;

        // Resolve the current renderer so resource reloads remain supported.
        var renderer = Minecraft.getInstance()
                .getBlockEntityRenderDispatcher()
                .getRenderer(preview);

        if (renderer == null) return;

        poseStack.pushPose();
        try {
            renderer.render(
                    preview,
                    0.0F,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay
            );
        } finally {
            poseStack.popPose();
        }
    }
}