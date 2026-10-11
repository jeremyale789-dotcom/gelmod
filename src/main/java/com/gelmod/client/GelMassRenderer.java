package com.gelmod.client;

import com.gelmod.entity.GelMassEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class GelMassRenderer extends MobRenderer<GelMassEntity, GelMassModel> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("gelmod", "textures/entity/gel_mass.png");

    public GelMassRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GelMassModel(ctx.bakeLayer(GelMassModel.LAYER)), 0.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(GelMassEntity e) { return TEX; }

    @Override
    protected void scale(GelMassEntity e, PoseStack pose, float partialTick) {
        float s = 0.5f * e.getGrowth(partialTick);   // tamaño visual de la masa
        pose.scale(s, s, s);
    }
}
