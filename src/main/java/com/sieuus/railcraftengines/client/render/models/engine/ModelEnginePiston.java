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

public final class ModelEnginePiston {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath(
                            RailcraftEngines.MODID,
                            "steam_engine_piston"
                    ),
                    "main"
            );

    private final ModelPart piston;

    public ModelEnginePiston(ModelPart root) {
        this.piston = root.getChild("piston");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
                "piston",
                CubeListBuilder.create()
                        .texOffs(32, 24)
                        .addBox(
                                -5.0F,
                                -4.0F,
                                -5.0F,
                                10.0F,
                                2.0F,
                                10.0F
                        )
                        .texOffs(32, 24)
                        .addBox(
                                -7.0F,
                                -4.0F,
                                -1.0F,
                                1.0F,
                                2.0F,
                                2.0F
                        )
                        .texOffs(32, 24)
                        .addBox(
                                -1.0F,
                                -4.0F,
                                -7.0F,
                                2.0F,
                                2.0F,
                                1.0F
                        )
                        .texOffs(32, 24)
                        .addBox(
                                6.0F,
                                -4.0F,
                                -1.0F,
                                1.0F,
                                2.0F,
                                2.0F
                        )
                        .texOffs(32, 24)
                        .addBox(
                                -1.0F,
                                -4.0F,
                                6.0F,
                                2.0F,
                                2.0F,
                                1.0F
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
        piston.render(
                poseStack,
                consumer,
                packedLight,
                packedOverlay
        );
    }
}