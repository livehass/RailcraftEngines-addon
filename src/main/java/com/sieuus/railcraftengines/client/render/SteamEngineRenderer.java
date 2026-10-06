/*
 * Portions of this file are derived from the Railcraft project.
 *
 * Railcraft copyright (c) CovertJaguar.
 * Original project:
 * https://github.com/Railcraft/Railcraft
 *
 * Legacy source branch: mc-1.7.10
 * Adapted for Minecraft 26.1.2 / NeoForge by sieuus.
 */

package com.sieuus.railcraftengines.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sieuus.railcraftengines.RailcraftEngines;
import com.sieuus.railcraftengines.client.render.models.engine.ModelEngineBase;
import com.sieuus.railcraftengines.client.render.models.engine.ModelEngineFrame;
import com.sieuus.railcraftengines.client.render.models.engine.ModelEnginePiston;
import com.sieuus.railcraftengines.client.render.models.engine.ModelEngineTrunk;
import com.sieuus.railcraftengines.common.blocks.engine.TileEngine;
import com.sieuus.railcraftengines.common.blocks.engine.TileEngineSteam;
import com.sieuus.railcraftengines.common.blocks.engine.TileEngineSteamCommercial;
import com.sieuus.railcraftengines.common.blocks.engine.TileEngineSteamIndustrial;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class SteamEngineRenderer<T extends TileEngineSteam>
        implements BlockEntityRenderer<T, SteamEngineRenderer.EngineRenderState> {


    private static final Identifier HOBBYIST_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    RailcraftEngines.MODID,
                    "textures/block/engine/steam_hobby.png"
            );
    private static final Identifier COMMERCIAL_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    RailcraftEngines.MODID,
                    "textures/block/engine/steam_low.png"
            );

    private static final Identifier INDUSTRIAL_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    RailcraftEngines.MODID,
                    "textures/block/engine/steam_high.png"
            );

    private static final float MAX_PISTON_TRAVEL = 7.99F;
    private static final float PISTON_PREP = 0.01F;
    private static final float PISTON_SEGMENT = 2.0F / 16.0F;

    private final ModelEngineBase base;
    private final ModelEngineFrame frame;
    private final ModelEnginePiston piston;
    private final ModelEngineTrunk trunk;

    public static final class EngineRenderState extends BlockEntityRenderState {
        public Direction facing;
        public float progress;
        public TileEngine.EnergyStage energyStage;
        public Identifier texture;
    }

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
    public EngineRenderState createRenderState() {
        return new EngineRenderState();
    }

    @Override
    public void extractRenderState(
            T engine,
            EngineRenderState state,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(
                engine,
                state,
                partialTick,
                cameraPosition,
                breakProgress
        );

        state.facing = engine.getFacing();
        state.progress = engine.getProgress();
        state.energyStage = engine.getEnergyStage();

        if (engine instanceof TileEngineSteamIndustrial) {
            state.texture = INDUSTRIAL_TEXTURE;
        } else if (engine instanceof TileEngineSteamCommercial) {
            state.texture = COMMERCIAL_TEXTURE;
        } else {
            state.texture = HOBBYIST_TEXTURE;
        }
    }

    @Override
    public void submit(
            EngineRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        RenderType renderType = RenderTypes.entityCutoutCull(state.texture);
        int light = state.lightCoords;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        applyFacing(poseStack, state.facing);
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        submitPart(
                trunk.getPart(state.energyStage),
                poseStack,
                collector,
                renderType,
                light,
                state.breakProgress
        );

        submitPart(
                base.getPart(),
                poseStack,
                collector,
                renderType,
                light,
                state.breakProgress
        );

        float step = getPistonTravel(state.progress);

        poseStack.pushPose();
        poseStack.translate(0.0D, step / 16.0F, 0.0D);
        submitPart(
                frame.getPart(),
                poseStack,
                collector,
                renderType,
                light,
                state.breakProgress
        );
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, -PISTON_PREP, 0.0D);

        for (int i = 0; i <= step + 2.0F; i += 2) {
            submitPart(
                    piston.getPart(),
                    poseStack,
                    collector,
                    renderType,
                    light,
                    state.breakProgress
            );
            poseStack.translate(0.0D, PISTON_SEGMENT, 0.0D);
        }

        poseStack.popPose();
        poseStack.popPose();
    }

    private static void submitPart(
            net.minecraft.client.model.geom.ModelPart part,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            RenderType renderType,
            int light,
            ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        collector.submitModelPart(
                part,
                poseStack,
                renderType,
                light,
                OverlayTexture.NO_OVERLAY,
                null,
                false,
                false,
                0xFFFFFFFF,
                breakProgress,
                0
        );
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
