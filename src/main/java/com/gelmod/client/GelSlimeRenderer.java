package com.gelmod.client;

import com.gelmod.entity.GelSlimeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class GelSlimeRenderer extends MobRenderer<GelSlimeEntity, GelSlimeModel> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("gelmod", "textures/entity/gel_slime.png");

    public GelSlimeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GelSlimeModel(ctx.bakeLayer(GelSlimeModel.INNER), false), 0.4f);
        this.addLayer(new GelSlimeOuterLayer(this, new GelSlimeModel(ctx.bakeLayer(GelSlimeModel.OUTER), true)));
    }

    @Override
    public ResourceLocation getTextureLocation(GelSlimeEntity e) { return TEX; }

    /** Transformación: primero se estira hacia arriba (junta fuerza) y luego se desploma en un charco. */
    @Override
    protected void scale(GelSlimeEntity e, PoseStack pose, float partialTick) {
        float q = e.getMorphProgress(partialTick);
        if (q <= 0f) return;
        float sy, sxz;
        if (q < 0.4f) {
            float k = q / 0.4f;
            sy = 1f + 0.8f * k;       // se alarga
            sxz = 1f - 0.2f * k;
        } else {
            float k = (q - 0.4f) / 0.6f;
            k = k * k;                // se desploma cada vez más rápido
            sy = 1.8f - 1.75f * k;    // hasta quedar casi plano
            sxz = 0.8f + 0.6f * k;    // y se extiende como charco
        }
        pose.scale(sxz, Math.max(0.03f, sy), sxz);
    }
}
