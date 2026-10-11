package com.gelmod.client;

import com.gelmod.GelMod;
import com.gelmod.entity.GelMassEntity;
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

import java.util.Random;

/**
 * Modelo de la masa: grumos de carne pálida + ramas que se retuercen + caras con ojos y dientes.
 * Todo se construye por código (no hace falta un archivo de modelo). Textura: gel_mass.png (128x128).
 * Las ramas y las caras "brotan" progresivamente según el crecimiento de la masa.
 */
public class GelMassModel extends EntityModel<GelMassEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GelMod.ID, "gel_mass"), "main");

    private static final int TENDRILS = 9, SEGS = 4, FACES = 11;
    private static final float[] TH = {4.5f, 3.6f, 2.8f, 2.0f};
    private static final float[] LEN = {13f, 12f, 11f, 10f};

    private final ModelPart root, body;
    private final ModelPart[][] tendril = new ModelPart[TENDRILS][SEGS];
    private final float[][] baseX = new float[TENDRILS][SEGS], baseZ = new float[TENDRILS][SEGS];
    private final ModelPart[] faces = new ModelPart[FACES];

    public GelMassModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        for (int i = 0; i < TENDRILS; i++) {
            ModelPart parent = body;
            for (int j = 0; j < SEGS; j++) {
                ModelPart p = parent.getChild("t" + i + "_" + j);
                tendril[i][j] = p; baseX[i][j] = p.xRot; baseZ[i][j] = p.zRot;
                parent = p;
            }
        }
        for (int f = 0; f < FACES; f++) faces[f] = body.getChild("face" + f);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0f, 24f, 0f));
        Random r = new Random(1337);

        // grumos del cuerpo: {x,y,z, ancho,alto,fondo, rotX,rotY,rotZ}
        float[][] lumps = {
                {0, 0, 0, 18, 16, 18, 0f, 0f, 0f},
                {8, -6, 3, 14, 14, 14, 0.3f, 0.6f, 0.2f},
                {-9, -5, -2, 12, 12, 12, 0.2f, -0.5f, -0.3f},
                {2, -14, -9, 12, 10, 12, -0.3f, 0.2f, 0.4f},
                {-4, -17, 7, 10, 9, 10, 0.4f, 0.9f, -0.2f},
                {6, -18, 6, 9, 9, 9, -0.2f, 0.3f, 0.5f},
                {-8, -16, -6, 9, 10, 9, 0.3f, -0.8f, 0.1f},
        };
        for (int i = 0; i < lumps.length; i++) {
            float[] l = lumps[i];
            body.addOrReplaceChild("lump" + i,
                    CubeListBuilder.create().texOffs(0, 24).addBox(-l[3] / 2f, -l[4], -l[5] / 2f, l[3], l[4], l[5]),
                    PartPose.offsetAndRotation(l[0], l[1], l[2], l[6], l[7], l[8]));
        }

        // caras (placas finas con ojos/boca pintados por delante y por detrás)
        for (int f = 0; f < FACES; f++) {
            float a = (float) (f * Math.PI * 2 / FACES + (r.nextFloat() - 0.5f) * 0.4f);
            float rad = 11f + r.nextFloat() * 4f;
            float y = -(4f + r.nextFloat() * 20f);
            body.addOrReplaceChild("face" + f,
                    CubeListBuilder.create().texOffs((f % 3) * 28, 0).addBox(-6f, -6f, -0.5f, 12f, 12f, 1f),
                    PartPose.offsetAndRotation((float) Math.sin(a) * rad, y, (float) Math.cos(a) * rad,
                            (r.nextFloat() - 0.5f) * 0.5f, a, 0f));
        }

        // ramas: cadenas de 4 segmentos que salen hacia arriba y hacia afuera
        for (int i = 0; i < TENDRILS; i++) {
            float a = (float) (i * Math.PI * 2 / TENDRILS + (r.nextFloat() - 0.5f) * 0.5f);
            float rr = 2f + r.nextFloat() * 8f;
            float by = -(8f + r.nextFloat() * 12f);
            float lean = -(0.35f + r.nextFloat() * 0.6f);     // negativo = inclinada hacia afuera
            PartDefinition prev = body;
            for (int j = 0; j < SEGS; j++) {
                PartPose pose = (j == 0)
                        ? PartPose.offsetAndRotation((float) Math.sin(a) * rr, by, (float) Math.cos(a) * rr, lean, a, 0f)
                        : PartPose.offsetAndRotation(0f, -LEN[j - 1] + 1f, 0f,
                                -0.15f + (r.nextFloat() - 0.5f) * 0.5f, 0f, (r.nextFloat() - 0.5f) * 0.6f);
                prev = prev.addOrReplaceChild("t" + i + "_" + j,
                        CubeListBuilder.create().texOffs(0, 24).addBox(-TH[j] / 2f, -LEN[j], -TH[j] / 2f, TH[j], LEN[j], TH[j]),
                        pose);
            }
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(GelMassEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float t = ageInTicks;
        float partial = ageInTicks - e.tickCount;
        float p = Math.max(0f, Math.min(1f, (e.getGrowth(partial) - 1f) / 7f));   // progreso de crecimiento 0..1

        body.yScale = 1f + 0.05f * (float) Math.sin(t * 0.25f);
        body.xScale = 1f + 0.04f * (float) Math.cos(t * 0.21f);
        body.zScale = body.xScale;

        for (int i = 0; i < TENDRILS; i++) {
            float start = i / (float) TENDRILS * 0.6f;                 // cada rama brota en un momento distinto
            float s = Math.max(0f, Math.min(1f, (p - start) / 0.25f));
            tendril[i][0].xScale = s; tendril[i][0].yScale = s; tendril[i][0].zScale = s;
            for (int j = 0; j < SEGS; j++) {
                float amp = 0.12f + 0.08f * j;
                tendril[i][j].xRot = baseX[i][j] + (float) Math.sin(t * 0.11f + i * 1.3f + j * 0.9f) * amp;
                tendril[i][j].zRot = baseZ[i][j] + (float) Math.cos(t * 0.09f + i * 0.7f + j * 1.1f) * amp;
            }
        }
        for (int f = 0; f < FACES; f++) {
            float s = Math.max(0f, Math.min(1f, (p * 1.3f - f * 0.07f) / 0.2f));
            faces[f].xScale = s; faces[f].yScale = s; faces[f].zScale = s;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        root.render(pose, buffer, light, overlay, color);
    }
}
