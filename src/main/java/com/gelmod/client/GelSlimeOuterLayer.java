package com.gelmod.client;

import com.gelmod.entity.GelSlimeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** Capas exteriores translúcidas del slime (rosa claro). */
public class GelSlimeOuterLayer extends RenderLayer<GelSlimeEntity, GelSlimeModel> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("gelmod", "textures/entity/gel_slime.png");
    private final GelSlimeModel outer;

    public GelSlimeOuterLayer(GelSlimeRenderer parent, GelSlimeModel outer) { super(parent); this.outer = outer; }

    @Override
    public void render(PoseStack pose, MultiBufferSource buf, int light, GelSlimeEntity e,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (e.isInvisible()) return;
        outer.setupAnim(e, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        outer.renderToBuffer(pose, buf.getBuffer(RenderType.entityTranslucent(TEX)),
                light, LivingEntityRenderer.getOverlayCoords(e, 0f), -1);
    }
}
