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
        getParentModel().renderToBuffer(pose, buf.getBuffer(RenderType.entityTranslucent(renderer.getTextureLocation(e))),
                light, LivingEntityRenderer.getOverlayCoords(e, 0f), 0x9A60FF70); // ARGB verde gelatina
    }
}
