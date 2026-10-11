package com.gelmod.client;

import com.gelmod.entity.GelPlayerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Dibuja el cuerpo del clon: una capa de gel rosa + la skin real teñida de rosa, ambas semitransparentes.
 * Al formarse domina el gel (casi opaco, sin skin); al completarse domina la skin semitransparente.
 */
public class GelOverlayLayer extends RenderLayer<GelPlayerEntity, PlayerModel<GelPlayerEntity>> {
    private static final ResourceLocation GEL = ResourceLocation.fromNamespaceAndPath("gelmod", "textures/entity/gel_zombie.png");
    private final GelPlayerRenderer renderer;

    public GelOverlayLayer(GelPlayerRenderer r) { super(r); this.renderer = r; }

    @Override
    public void render(PoseStack pose, MultiBufferSource buf, int light, GelPlayerEntity e,
                       float limbAngle, float limbDistance, float partialTick, float anim, float headYaw, float headPitch) {
        if (e.isInvisible()) return;
        float p = e.getFormProgress(partialTick);                      // 0 = recién formándose, 1 = completo
        PlayerModel<GelPlayerEntity> m = getParentModel();
        m.setAllVisible(true);
        int overlay = LivingEntityRenderer.getOverlayCoords(e, 0f);

        int gelA = (int) (255 * (0.85f * (1f - p) + 0.22f * p));
        m.renderToBuffer(pose, buf.getBuffer(RenderType.entityTranslucent(GEL)), light, overlay, (gelA << 24) | 0xFF8CC8);

        int skinA = (int) (255 * 0.62f * p);
        if (skinA > 8) {
            m.renderToBuffer(pose, buf.getBuffer(RenderType.entityTranslucent(renderer.getTextureLocation(e))),
                    light, overlay, (skinA << 24) | 0xFFC0DC);          // skin teñida de rosa
        }
    }
}
