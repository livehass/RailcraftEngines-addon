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
import com.sieuus.railcraftengines.common.blocks.engine.TileEngine;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public final class ModelEngineTrunk {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath(
                            RailcraftEngines.MODID,
                            "steam_engine_trunk"
                    ),
                    "main"
            );

    private final ModelPart blue;
    private final ModelPart green;
    private final ModelPart yellow;
    private final ModelPart orange;
    private final ModelPart red;

    public ModelEngineTrunk(ModelPart root) {
        this.blue = root.getChild("blue");
        this.green = root.getChild("green");
        this.yellow = root.getChild("yellow");
        this.orange = root.getChild("orange");
        this.red = root.getChild("red");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        addTrunk(
                root,
                "blue",
                1,
                57
        );

        addTrunk(
                root,
                "green",
                35,
                57
        );

        addTrunk(
                root,
                "yellow",
                69,
                57
        );

        addTrunk(
                root,
                "orange",
                1,
                79
        );

        addTrunk(
                root,
                "red",
                35,
                79
        );

        return LayerDefinition.create(
                mesh,
                128,
                128
        );
    }

    private static void addTrunk(
            PartDefinition root,
            String name,
            int textureU,
            int textureV
    ) {
        root.addOrReplaceChild(
                name,
                CubeListBuilder.create()
                        .texOffs(
                                textureU,
                                textureV
                        )
                        .addBox(
                                -4.0F,
                                -4.0F,
                                -4.0F,
                                8.0F,
                                12.0F,
                                8.0F
                        ),
                PartPose.offset(
                        8.0F,
                        8.0F,
                        8.0F
                )
        );
    }

    public void render(
            TileEngine.EnergyStage stage,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay
    ) {
        ModelPart part = switch (stage) {
            case BLUE -> blue;
            case GREEN -> green;
            case YELLOW -> yellow;
            case ORANGE -> orange;
            case RED, OVERHEAT -> red;
        };

        part.render(
                poseStack,
                consumer,
                packedLight,
                packedOverlay
        );
    }
}