package com.gelmod.client;

import com.gelmod.GelMod;
import com.gelmod.entity.GelSlimeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Slime derretido: 3 capas apiladas (muy aplastada -> más gruesa -> aún más gruesa) y un charco lateral.
 * Se usa dos veces: INNER (núcleo rosa fuerte con cara, opaco) y OUTER (capas rosa claro translúcidas).
 * Al moverse, las capas ondulan una tras otra, como si se arrastrara.
 */
public class GelSlimeModel extends EntityModel<GelSlimeEntity> {
    public static final ModelLayerLocation INNER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GelMod.ID, "gel_slime"), "inner");
    public static final ModelLayerLocation OUTER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GelMod.ID, "gel_slime"), "outer");

    // grosor de cada capa (para mantenerlas apiladas cuando se estiran)
    private static final float[] THICK_OUTER = {2f, 4f, 6f};
    private static final float[] THICK_INNER = {0f, 3f, 4f};

    private final ModelPart root;
    private final ModelPart[] layers = new ModelPart[3];
    private final ModelPart patch;
    private final float[] baseY = new float[3], baseZ = new float[3];
    private final float patchYRot;
    private final float[] thick;

    public GelSlimeModel(ModelPart root, boolean outer) {
        this.root = root;
        ModelPart body = root.getChild("body");
        for (int i = 0; i < 3; i++) {
            layers[i] = body.getChild("l" + i);
            baseY[i] = layers[i].y; baseZ[i] = layers[i].z;
        }
        patch = body.getChild("patch");
        patchYRot = patch.yRot;
        thick = outer ? THICK_OUTER : THICK_INNER;
    }

    public static LayerDefinition createLayer(boolean outer) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0f, 24f, 0f));
        if (outer) {
            body.addOrReplaceChild("l0", CubeListBuilder.create().texOffs(0, 32).addBox(-10f, -2f, -10f, 20f, 2f, 20f), PartPose.offset(0f, 0f, 0f));
            body.addOrReplaceChild("l1", CubeListBuilder.create().texOffs(0, 54).addBox(-7f, -4f, -7f, 14f, 4f, 14f), PartPose.offset(0f, -1f, 0f));
            body.addOrReplaceChild("l2", CubeListBuilder.create().texOffs(0, 72).addBox(-5f, -6f, -5f, 10f, 6f, 10f), PartPose.offset(0f, -4f, 0f));
            body.addOrReplaceChild("patch", CubeListBuilder.create().texOffs(0, 88).addBox(-6f, -1f, -4.5f, 12f, 1f, 9f),
                    PartPose.offsetAndRotation(8f, 0f, 5f, 0f, 0.5f, 0f));
        } else {
            body.addOrReplaceChild("l0", CubeListBuilder.create(), PartPose.offset(0f, 0f, 0f));
            body.addOrReplaceChild("l1", CubeListBuilder.create().texOffs(0, 0).addBox(-6f, -3f, -6f, 12f, 3f, 12f), PartPose.offset(0f, -1.5f, 0f));
            body.addOrReplaceChild("l2", CubeListBuilder.create().texOffs(0, 16).addBox(-4f, -4f, -4f, 8f, 4f, 8f), PartPose.offset(0f, -4.5f, 0f));
            body.addOrReplaceChild("patch", CubeListBuilder.create(), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(GelSlimeEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float t = ageInTicks;
        float move = Math.min(1f, limbSwingAmount * 3f);     // 0 = quieto, 1 = arrastrándose
        float shift = 0f;
        for (int i = 0; i < 3; i++) {
            float ph = i * 0.9f;
            float wave = (float) Math.sin(limbSwing * 0.9f - ph) * move;     // ola que recorre las capas
            float breathe = (float) Math.sin(t * 0.12f + ph);                // respiración lenta
            float ys = 1f + 0.10f * breathe + 0.18f * wave;
            float sp = 1f - 0.05f * breathe - 0.08f * wave;
            layers[i].yScale = ys;
            layers[i].xScale = sp; layers[i].zScale = sp;
            layers[i].z = baseZ[i] + wave * 2.2f;                            // cada capa va un poco "atrasada"
            layers[i].y = baseY[i] - shift;                                  // mantiene las capas apiladas
            shift += thick[i] * (ys - 1f);
        }
        patch.yRot = patchYRot + (float) Math.sin(t * 0.05f) * 0.1f;
        patch.xScale = 1f + 0.06f * (float) Math.sin(t * 0.1f);
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        root.render(pose, buffer, light, overlay, color);
    }
}
