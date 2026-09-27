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

package com.sieuus.railcraftengines.client.render.models.engine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sieuus.railcraftengines.RailcraftEngines;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public final class ModelEngineFrame {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath(
                            RailcraftEngines.MODID,
                            "steam_engine_frame"
                    ),
                    "main"
            );

    private final ModelPart frame;

    public ModelEngineFrame(ModelPart root) {
        this.frame = root.getChild("frame");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
                "frame",
                CubeListBuilder.create()
                        .texOffs(65, 1)
                        .addBox(
                                -6.0F,
                                -4.0F,
                                -6.0F,
                                12.0F,
                                4.0F,
                                12.0F
                        )
                        .texOffs(1, 23)
                        .addBox(
                                -3.0F,
                                -4.0F,
                                -8.0F,
                                6.0F,
                                4.0F,
                                16.0F
                        )
                        .texOffs(1, 45)
                        .addBox(
                                -8.0F,
                                -4.0F,
                                -3.0F,
                                16.0F,
                                4.0F,
                                6.0F
                        ),
                PartPose.offset(
                        8.0F,
                        8.0F,
                        8.0F
                )
        );

        return LayerDefinition.create(
                mesh,
                128,
                128
        );
    }

    public void render(
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay
    ) {
        frame.render(
                poseStack,
                consumer,
                packedLight,
                packedOverlay
        );
    }
}