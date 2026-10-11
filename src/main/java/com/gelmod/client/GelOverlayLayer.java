package com.gelmod.client;

import com.gelmod.entity.GelPlayerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

public class GelOverlayLayer extends RenderLayer<GelPlayerEntity, PlayerModel<GelPlayerEntity>> {
    private final GelPlayerRenderer renderer;

    public GelOverlayLayer(GelPlayerRenderer r) { super(r); this.renderer = r; }

    @Override
    public void render(PoseStack pose, MultiBufferSource buf, int light, GelPlayerEntity e,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (e.isInvisible()) return;
        float p = e.getFormProgress(partialTick);                 // 0 = recién formándose, 1 = completo
        int alpha = (int) (255 - (255 - 0x9A) * p);                // al inicio es gel opaco; luego deja ver la skin
        int color = (alpha << 24) | 0x60FF70;                      // ARGB verde gelatina
        getParentModel().renderToBuffer(pose, buf.getBuffer(RenderType.entityTranslucent(renderer.getTextureLocation(e))),
                light, LivingEntityRenderer.getOverlayCoords(e, 0f), color);
    }
}
