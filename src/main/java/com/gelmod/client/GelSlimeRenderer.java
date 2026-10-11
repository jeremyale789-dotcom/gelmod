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
import net.minecraft.util.Mth;

public class GelSlimeRenderer extends MobRenderer<GelSlimeEntity, GelSlimeModel> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("gelmod", "textures/entity/gel_slime.png");
    private static final ResourceLocation[] EGG_TEX = {
            ResourceLocation.fromNamespaceAndPath("gelmod", "textures/entity/gel_egg_0.png"),
            ResourceLocation.fromNamespaceAndPath("gelmod", "textures/entity/gel_egg_1.png"),
            ResourceLocation.fromNamespaceAndPath("gelmod", "textures/entity/gel_egg_2.png")};
    /** Fracción de la transformación en la que el slime ya se convirtió en huevo. */
    private static final float EGG_START = 0.14f;

    private final GelEggModel egg;

    public GelSlimeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GelSlimeModel(ctx.bakeLayer(GelSlimeModel.INNER), false), 0.4f);
        this.addLayer(new GelSlimeOuterLayer(this, new GelSlimeModel(ctx.bakeLayer(GelSlimeModel.OUTER), true)));
        this.egg = new GelEggModel(ctx.bakeLayer(GelEggModel.LAYER));
    }

    @Override
    public ResourceLocation getTextureLocation(GelSlimeEntity e) { return TEX; }

    @Override
    public void render(GelSlimeEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buf, int light) {
        float q = e.getMorphProgress(partialTick);
        if (q >= EGG_START) { renderEgg(e, q, partialTick, pose, buf, light); return; }
        super.render(e, yaw, partialTick, pose, buf, light);
    }

    /** Antes de ser huevo: el slime se junta y se alarga, como comprimiéndose en forma de huevo. */
    @Override
    protected void scale(GelSlimeEntity e, PoseStack pose, float partialTick) {
        float q = e.getMorphProgress(partialTick);
        if (q <= 0f || q >= EGG_START) return;
        float k = q / EGG_START;
        pose.scale(1f - 0.35f * k, 1f + 0.5f * k, 1f - 0.35f * k);
    }

    private void renderEgg(GelSlimeEntity e, float q, float partialTick, PoseStack pose, MultiBufferSource buf, int light) {
        float ep = Mth.clamp((q - EGG_START) / (1f - EGG_START), 0f, 1f);   // 0..1 dentro de la fase de huevo
        float t = e.tickCount + partialTick;

        // aparición con un pequeño "pop"
        float pop = Mth.clamp((q - EGG_START) / 0.05f, 0f, 1f);
        float s = 1.15f * (0.6f + 0.4f * pop + 0.15f * (float) Math.sin(pop * Math.PI));

        // se mece cada vez más fuerte y más rápido, como a punto de eclosionar
        float amp = 0.05f + 0.38f * ep * ep;
        float phase = t * 0.32f + 16f * ep * ep;
        float wobble = (float) Math.sin(phase) * amp + (float) Math.sin(t * 2.7f) * 0.05f * ep;
        float tilt = (float) Math.cos(phase * 0.5f) * amp * 0.4f;
        float squash = 1f + 0.03f * (float) Math.sin(t * 0.6f) * (0.3f + ep);
        egg.animate(wobble, tilt, squash);

        int stage = ep < 0.35f ? 0 : (ep < 0.7f ? 1 : 2);                  // más grietas con el tiempo
        int lightUsed = stage == 2 ? LightTexture.FULL_BRIGHT : light;        // al final "brilla" por las grietas

        pose.pushPose();
        pose.scale(-1f, -1f, 1f);
        pose.scale(s, s, s);
        pose.translate(0f, -1.501f, 0f);
        egg.render(pose, buf.getBuffer(RenderType.entityTranslucent(EGG_TEX[stage])), lightUsed, OverlayTexture.NO_OVERLAY, -1);
        pose.popPose();
    }
}
