package com.gelmod.client;

import com.gelmod.entity.GelSlimeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class GelSlimeRenderer extends MobRenderer<GelSlimeEntity, GelSlimeModel> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("gelmod", "textures/entity/gel_slime.png");
    private static final ResourceLocation COCOON = ResourceLocation.fromNamespaceAndPath("gelmod", "textures/entity/gel_cocoon.png");
    private static final ResourceLocation COCOON_CRACKED = ResourceLocation.fromNamespaceAndPath("gelmod", "textures/entity/gel_cocoon_cracked.png");

    private final GelCocoonModel cocoon;

    public GelSlimeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GelSlimeModel(ctx.bakeLayer(GelSlimeModel.INNER), false), 0.4f);
        this.addLayer(new GelSlimeOuterLayer(this, new GelSlimeModel(ctx.bakeLayer(GelSlimeModel.OUTER), true)));
        this.cocoon = new GelCocoonModel(ctx.bakeLayer(GelCocoonModel.LAYER));
    }

    @Override
    public ResourceLocation getTextureLocation(GelSlimeEntity e) { return TEX; }

    @Override
    public void render(GelSlimeEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buf, int light) {
        float ticks = e.getMorphTicks(partialTick);
        if (ticks >= GelSlimeEntity.T_FLAT) { renderCocoon(ticks, pose, buf, light); return; }
        super.render(e, yaw, partialTick, pose, buf, light);
    }

    /** Primer paso de la transformación: el slime se aplasta todavía más, como un charco con cara. */
    @Override
    protected void scale(GelSlimeEntity e, PoseStack pose, float partialTick) {
        float ticks = e.getMorphTicks(partialTick);
        if (ticks <= 0f || ticks >= GelSlimeEntity.T_FLAT) return;
        float k = ticks / GelSlimeEntity.T_FLAT;
        pose.scale(1f + 0.15f * k, 1f - 0.45f * k, 1f + 0.15f * k);
    }

    private void renderCocoon(float ticks, PoseStack pose, MultiBufferSource buf, int light) {
        boolean cracked = ticks >= GelSlimeEntity.T_MERGE_END + 6;              // al final aparecen grietas con luz
        cocoon.animate(ticks);
        pose.pushPose();
        pose.scale(-1f, -1f, 1f);
        pose.scale(0.9f, 0.9f, 0.9f);
        pose.translate(0f, -1.501f, 0f);
        cocoon.render(pose, buf.getBuffer(RenderType.entityTranslucent(cracked ? COCOON_CRACKED : COCOON)),
                cracked ? LightTexture.FULL_BRIGHT : light, OverlayTexture.NO_OVERLAY, -1);
        pose.popPose();
    }
}
