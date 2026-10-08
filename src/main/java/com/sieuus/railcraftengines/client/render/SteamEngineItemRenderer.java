package com.sieuus.railcraftengines.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sieuus.railcraftengines.RailcraftEngines;
import com.sieuus.railcraftengines.client.render.models.engine.ModelEngineBase;
import com.sieuus.railcraftengines.client.render.models.engine.ModelEngineFrame;
import com.sieuus.railcraftengines.client.render.models.engine.ModelEnginePiston;
import com.sieuus.railcraftengines.client.render.models.engine.ModelEngineTrunk;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Locale;
import java.util.function.Consumer;

/** Renders the three steam-engine block items using the 26.1 special-model API. */
public final class SteamEngineItemRenderer implements SpecialModelRenderer<Void> {

    private static final Codec<EngineVariant> ENGINE_VARIANT_CODEC = Codec.STRING.xmap(
            name -> EngineVariant.valueOf(name.toUpperCase(Locale.ROOT)),
            variant -> variant.name().toLowerCase(Locale.ROOT)
    );

    private static final Identifier HOBBYIST_TEXTURE = texture("steam_hobby.png");
    private static final Identifier COMMERCIAL_TEXTURE = texture("steam_low.png");
    private static final Identifier INDUSTRIAL_TEXTURE = texture("steam_high.png");

    private static Identifier texture(String file) {
        return Identifier.fromNamespaceAndPath(
                RailcraftEngines.MODID,
                "textures/block/engine/" + file
        );
    }

    private final ModelPart base;
    private final ModelPart frame;
    private final ModelPart piston;
    private final ModelPart trunk;
    private final Identifier texture;

    private SteamEngineItemRenderer(SpecialModelRenderer.BakingContext baker, EngineVariant variant) {
        this.base = baker.entityModelSet().bakeLayer(ModelEngineBase.LAYER).getChild("base");
        this.frame = baker.entityModelSet().bakeLayer(ModelEngineFrame.LAYER).getChild("frame");
        this.piston = baker.entityModelSet().bakeLayer(ModelEnginePiston.LAYER).getChild("piston");
        this.trunk = baker.entityModelSet().bakeLayer(ModelEngineTrunk.LAYER)
                .getChild("blue");
        this.texture = switch (variant) {
            case HOBBYIST -> HOBBYIST_TEXTURE;
            case COMMERCIAL -> COMMERCIAL_TEXTURE;
            case INDUSTRIAL -> INDUSTRIAL_TEXTURE;
        };
    }

    @Override
    public Void extractArgument(ItemStack stack) {
        return null;
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    output.accept(new Vector3f(x, y, z));
                }
            }
        }
    }

    @Override
    public void submit(
            Void argument,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor
    ) {
        RenderType renderType = RenderTypes.entityCutoutCull(texture);

        // Keep the same block-centered coordinates and resting piston position as
        // the previous BlockEntityWithoutLevelRenderer implementation.
        poseStack.pushPose();
        try {
            poseStack.translate(0.5D, 0.5D, 0.5D);
            poseStack.translate(-0.5D, -0.5D, -0.5D);

            submitPart(trunk, poseStack, collector, renderType,
                    lightCoords, overlayCoords, hasFoil, outlineColor);
            submitPart(base, poseStack, collector, renderType,
                    lightCoords, overlayCoords, hasFoil, outlineColor);
            submitPart(frame, poseStack, collector, renderType,
                    lightCoords, overlayCoords, hasFoil, outlineColor);

            // The original renderer submits two piston segments at rest.
            poseStack.pushPose();
            try {
                poseStack.translate(0.0D, -0.01D, 0.0D);
                for (int segment = 0; segment < 2; segment++) {
                    submitPart(piston, poseStack, collector, renderType,
                            lightCoords, overlayCoords, hasFoil, outlineColor);
                    poseStack.translate(0.0D, 2.0D / 16.0D, 0.0D);
                }
            } finally {
                poseStack.popPose();
            }
        } finally {
            poseStack.popPose();
        }
    }

    public static void submitPart(
            ModelPart part,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor
    ) {
        collector.submitModelPart(
                part,
                poseStack,
                renderType,
                lightCoords,
                overlayCoords,
                null,
                false,
                hasFoil,
                -1,
                null,
                outlineColor
        );
    }

    public enum EngineVariant {
        HOBBYIST,
        COMMERCIAL,
        INDUSTRIAL
    }

    public record Unbaked(EngineVariant variant)
            implements SpecialModelRenderer.Unbaked<Void> {

        public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        ENGINE_VARIANT_CODEC.fieldOf("engine").forGetter(Unbaked::variant)
                ).apply(instance, Unbaked::new)
        );

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext baker) {
            return new SteamEngineItemRenderer(baker, variant);
        }
    }
}
