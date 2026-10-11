package com.gelmod.client;

import com.gelmod.GelMod;
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

/** Huevo de la transformación del slime: 7 capas apiladas que forman un óvalo (más ancho abajo). */
public class GelEggModel {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GelMod.ID, "gel_egg"), "main");

    private final ModelPart root, body;

    public GelEggModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0f, 24f, 0f));
        // {ancho, alto, desplazamiento Y, u, v}
        float[][] c = {
                {10, 2, 0, 0, 0},
                {13, 3, -2, 0, 12},
                {14, 4, -5, 0, 28},
                {13, 3, -9, 0, 46},
                {11, 3, -12, 0, 62},
                {8, 2, -15, 0, 76},
                {5, 2, -17, 0, 86},
        };
        for (int i = 0; i < c.length; i++) {
            float w = c[i][0], h = c[i][1];
            body.addOrReplaceChild("e" + i,
                    CubeListBuilder.create().texOffs((int) c[i][3], (int) c[i][4]).addBox(-w / 2f, -h, -w / 2f, w, h, w),
                    PartPose.offset(0f, c[i][2], 0f));
        }
        return LayerDefinition.create(mesh, 64, 128);
    }

    /** wobble = balanceo lateral, tilt = inclinación adelante/atrás, squash = escala vertical. */
    public void animate(float wobble, float tilt, float squash) {
        body.zRot = wobble;
        body.xRot = tilt;
        body.yScale = squash;
    }

    public void render(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        root.render(pose, buffer, light, overlay, color);
    }
}
