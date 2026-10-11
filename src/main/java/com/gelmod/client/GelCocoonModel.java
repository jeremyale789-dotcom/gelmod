package com.gelmod.client;

import com.gelmod.GelMod;
import com.gelmod.entity.GelSlimeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Capullo de la transformación slime -> zombi, siguiendo el concepto dibujado:
 *   1) un charco plano  2) dos tentáculos con cara que se alzan  3) se funden en una masa grande  4) la masa se abre y sale el zombi.
 * Se anima por completo desde el tiempo (ticks) de la transformación.
 */
public class GelCocoonModel {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GelMod.ID, "gel_cocoon"), "main");

    private static final int SEGS = 6;
    private static final float[] TH = {6f, 5f, 5f, 4f, 4f, 3f};
    private static final float[] LEN = {9f, 9f, 8f, 8f, 7f, 7f};

    private final ModelPart root, body, base, mass, lumpA, lumpB;
    private final ModelPart[][] seg = new ModelPart[2][SEGS];
    private final float[][] baseX = new float[2][SEGS], baseZ = new float[2][SEGS];

    public GelCocoonModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.base = body.getChild("base");
        this.mass = body.getChild("mass");
        this.lumpA = mass.getChild("lumpA");
        this.lumpB = mass.getChild("lumpB");
        for (int i = 0; i < 2; i++) {
            ModelPart parent = body;
            for (int j = 0; j < SEGS; j++) {
                ModelPart p = parent.getChild("t" + i + "_" + j);
                seg[i][j] = p; baseX[i][j] = p.xRot; baseZ[i][j] = p.zRot;
                parent = p;
            }
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0f, 24f, 0f));

        // charco plano de la base
        body.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0).addBox(-14f, -2f, -14f, 28f, 2f, 28f), PartPose.ZERO);

        // masa grande (panel 3 del concepto): capas apiladas, más ancha en el medio
        PartDefinition mass = body.addOrReplaceChild("mass", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-9f, -4f, -8f, 18f, 4f, 16f)
                .addBox(-11f, -11f, -10f, 22f, 7f, 20f)
                .addBox(-12f, -20f, -11f, 24f, 9f, 22f)
                .addBox(-11f, -28f, -10f, 22f, 8f, 20f)
                .addBox(-9f, -34f, -8f, 18f, 6f, 16f)
                .addBox(-6f, -38f, -5f, 12f, 4f, 10f), PartPose.offset(0f, -1f, 0f));
        mass.addOrReplaceChild("lumpA", CubeListBuilder.create().texOffs(0, 0).addBox(-5f, -9f, -5f, 10f, 9f, 10f),
                PartPose.offsetAndRotation(-14f, -6f, 2f, 0.15f, 0.5f, 0.25f));
        mass.addOrReplaceChild("lumpB", CubeListBuilder.create().texOffs(0, 0).addBox(-4f, -7f, -4f, 8f, 7f, 8f),
                PartPose.offsetAndRotation(13f, -22f, -2f, -0.2f, 0.4f, -0.3f));

        // dos tentáculos (panel 2): cadenas de 6 segmentos que terminan en una cabeza con cara
        for (int i = 0; i < 2; i++) {
            float side = (i == 0) ? -1f : 1f;
            PartDefinition prev = body;
            for (int j = 0; j < SEGS; j++) {
                PartPose pose = (j == 0)
                        ? PartPose.offsetAndRotation(side * 8f, -1f, 0f, 0f, 0f, side * -0.12f)
                        : PartPose.offsetAndRotation(0f, -LEN[j - 1] + 1f, 0f, 0f, 0f, side * -0.14f);
                prev = prev.addOrReplaceChild("t" + i + "_" + j,
                        CubeListBuilder.create().texOffs(0, 0).addBox(-TH[j] / 2f, -LEN[j], -TH[j] / 2f, TH[j], LEN[j], TH[j]), pose);
            }
            prev.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 40).addBox(-3f, -6f, -3f, 6f, 6f, 6f),
                    PartPose.offsetAndRotation(0f, -LEN[SEGS - 1] + 1f, 0f, -0.15f, side * 0.5f, 0f));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static float ss(float x) { x = Math.max(0f, Math.min(1f, x)); return x * x * (3f - 2f * x); }

    /** ticks = tiempo transcurrido de la transformación (con parte fraccionaria). */
    public void animate(float ticks) {
        float g = ss((ticks - GelSlimeEntity.T_FLAT) / (GelSlimeEntity.T_TEND_END - GelSlimeEntity.T_FLAT));        // tentáculos crecen
        float m = ss((ticks - GelSlimeEntity.T_TEND_END) / (GelSlimeEntity.T_MERGE_END - GelSlimeEntity.T_TEND_END)); // se funden en la masa
        float c = ss((ticks - GelSlimeEntity.T_BURST) / (GelSlimeEntity.T_END - GelSlimeEntity.T_BURST));            // la masa se desploma

        // temblor cada vez más fuerte antes de abrirse
        float shake = 0f;
        if (ticks > GelSlimeEntity.T_MERGE_END && ticks < GelSlimeEntity.T_BURST) {
            shake = 0.03f + 0.10f * (ticks - GelSlimeEntity.T_MERGE_END) / (GelSlimeEntity.T_BURST - GelSlimeEntity.T_MERGE_END);
        }
        body.zRot = (float) Math.sin(ticks * 1.7f) * shake;
        body.xRot = (float) Math.cos(ticks * 2.3f) * shake * 0.6f;

        // charco
        float bs = 0.35f + 0.65f * ss((ticks - GelSlimeEntity.T_FLAT) / 10f);
        base.xScale = bs * (1f + 0.25f * c); base.zScale = base.xScale; base.yScale = 1f;

        // masa
        float pulse = (m > 0.9f) ? 1f + 0.03f * (float) Math.sin(ticks * 0.9f) : 1f;
        float xz = (0.25f + 0.75f * m) * (1f + 0.30f * c) * pulse;
        mass.xScale = xz; mass.zScale = xz;
        mass.yScale = Math.max(0.001f, m * (1f - 0.92f * c));
        float ls = Math.max(0.001f, ss((m - 0.5f) / 0.4f));
        lumpA.xScale = ls; lumpA.yScale = ls; lumpA.zScale = ls;
        lumpB.xScale = ls; lumpB.yScale = ls; lumpB.zScale = ls;

        // tentáculos: crecen, se mecen, y al fundirse se inclinan hacia adentro y se encogen
        float ts = Math.max(0.001f, g * (1f - m));
        for (int i = 0; i < 2; i++) {
            float side = (i == 0) ? -1f : 1f;
            seg[i][0].xScale = ts; seg[i][0].yScale = ts; seg[i][0].zScale = ts;
            for (int j = 0; j < SEGS; j++) {
                float sway = (float) Math.sin(ticks * 0.14f + i * 2f + j * 0.6f) * (0.06f + 0.03f * j);
                seg[i][j].xRot = baseX[i][j] + sway;
                seg[i][j].zRot = baseZ[i][j] + side * m * 0.6f + (float) Math.cos(ticks * 0.11f + i + j * 0.5f) * 0.05f;
            }
        }
    }

    public void render(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        root.render(pose, buffer, light, overlay, color);
    }
}
