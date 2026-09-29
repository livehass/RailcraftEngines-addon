/*
 * Portions of this file are derived from the Railcraft project.
 *
 * Railcraft copyright (c) CovertJaguar.
 * Original project:
 * https://github.com/Railcraft/Railcraft
 *
 * Legacy source branch: mc-1.7.10
 * Adapted for Minecraft 1.21.1 / NeoForge by sieuus.
 */

package com.sieuus.railcraftengines.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sieuus.railcraftengines.RailcraftEngines;
import com.sieuus.railcraftengines.client.render.models.engine.ModelEngineBase;
import com.sieuus.railcraftengines.client.render.models.engine.ModelEngineFrame;
import com.sieuus.railcraftengines.client.render.models.engine.ModelEnginePiston;
import com.sieuus.railcraftengines.client.render.models.engine.ModelEngineTrunk;
import com.sieuus.railcraftengines.common.blocks.engine.TileEngineSteamHobby;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import com.sieuus.railcraftengines.common.blocks.engine.TileEngineSteam;
import com.sieuus.railcraftengines.common.blocks.engine.TileEngineSteamCommercial;

public final class SteamEngineRenderer<T extends TileEngineSteam>
        implements BlockEntityRenderer<T> {

    private static final ResourceLocation HOBBYIST_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    RailcraftEngines.MODID,
                    "textures/block/engine/steam_hobby.png"
            );
    private static final ResourceLocation COMMERCIAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    RailcraftEngines.MODID,
                    "textures/block/engine/steam_low.png"
            );

    private static final float MAX_PISTON_TRAVEL = 7.99F;
    private static final float PISTON_PREP = 0.01F;
    private static final float PISTON_SEGMENT = 2.0F / 16.0F;

    private final ModelEngineBase base;
    private final ModelEngineFrame frame;
    private final ModelEnginePiston piston;
    private final ModelEngineTrunk trunk;

    public SteamEngineRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        this.base = new ModelEngineBase(
                context.bakeLayer(ModelEngineBase.LAYER)
        );

        this.frame = new ModelEngineFrame(
                context.bakeLayer(ModelEngineFrame.LAYER)
        );

        this.piston = new ModelEnginePiston(
                context.bakeLayer(ModelEnginePiston.LAYER)
        );

        this.trunk = new ModelEngineTrunk(
                context.bakeLayer(ModelEngineTrunk.LAYER)
        );
    }

    @Override
    public void render(
            T engine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        ResourceLocation texture = engine instanceof TileEngineSteamCommercial
                ? COMMERCIAL_TEXTURE
                : HOBBYIST_TEXTURE;

        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityCutout(texture)
        );
        poseStack.pushPose();

        poseStack.translate(
                0.5D,
                0.5D,
                0.5D
        );

        applyFacing(
                poseStack,
                engine.getFacing()
        );

        poseStack.translate(
                -0.5D,
                -0.5D,
                -0.5D
        );

        float step = getPistonTravel(
                engine.getProgress()
        );

        trunk.render(
                engine.getEnergyStage(),
                poseStack,
                consumer,
                packedLight,
                packedOverlay
        );

        base.render(
                poseStack,
                consumer,
                packedLight,
                packedOverlay
        );

        renderFrame(
                step,
                poseStack,
                consumer,
                packedLight,
                packedOverlay
        );

        renderPiston(
                step,
                poseStack,
                consumer,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    private void renderFrame(
            float step,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();

        poseStack.translate(
                0.0D,
                step / 16.0F,
                0.0D
        );

        frame.render(
                poseStack,
                consumer,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    private void renderPiston(
            float step,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();

        poseStack.translate(
                0.0D,
                -PISTON_PREP,
                0.0D
        );

        for (int i = 0; i <= step + 2.0F; i += 2) {
            piston.render(
                    poseStack,
                    consumer,
                    packedLight,
                    packedOverlay
            );

            poseStack.translate(
                    0.0D,
                    PISTON_SEGMENT,
                    0.0D
            );
        }

        poseStack.popPose();
    }

    private static float getPistonTravel(
            float progress
    ) {
        if (progress > 0.5F) {
            return MAX_PISTON_TRAVEL
                    - (progress - 0.5F)
                    * 2.0F
                    * MAX_PISTON_TRAVEL;
        }

        return progress
                * 2.0F
                * MAX_PISTON_TRAVEL;
    }

    private static void applyFacing(
            PoseStack poseStack,
            Direction facing
    ) {
        switch (facing) {
            case EAST ->
                    poseStack.mulPose(
                            Axis.ZP.rotationDegrees(-90.0F)
                    );

            case WEST ->
                    poseStack.mulPose(
                            Axis.ZP.rotationDegrees(90.0F)
                    );

            case DOWN ->
                    poseStack.mulPose(
                            Axis.ZP.rotationDegrees(180.0F)
                    );

            case SOUTH ->
                    poseStack.mulPose(
                            Axis.XP.rotationDegrees(90.0F)
                    );

            case NORTH ->
                    poseStack.mulPose(
                            Axis.XP.rotationDegrees(-90.0F)
                    );

            case UP -> {
            }
        }
    }
}