package com.gelmod.client;

import com.gelmod.entity.GelZombieEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

/** Dibuja todo el cuerpo del zombi en rosa semitransparente (el cuerpo "normal" se oculta en el renderer). */
public class GelZombieOverlayLayer extends RenderLayer<Zombie, ZombieModel<Zombie>> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("gelmod", "textures/entity/gel_zombie.png");

    public GelZombieOverlayLayer(RenderLayerParent<Zombie, ZombieModel<Zombie>> parent) { super(parent); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buf, int light, Zombie e,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (e.isInvisible()) return;
        float p = (e instanceof GelZombieEntity g) ? g.getFormProgress(partialTick) : 1f;
        int alpha = (int) (255 * (0.85f - 0.25f * p));            // más sólido al formarse (0.85) y luego semitransparente (0.60)
        ZombieModel<Zombie> m = getParentModel();
        m.setAllVisible(true);
        m.renderToBuffer(pose, buf.getBuffer(RenderType.entityTranslucent(TEX)),
                light, LivingEntityRenderer.getOverlayCoords(e, 0f), (alpha << 24) | 0xFFFFFF);
    }
}
