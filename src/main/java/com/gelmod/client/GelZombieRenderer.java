package com.gelmod.client;

import com.gelmod.entity.GelZombieEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

public class GelZombieRenderer extends ZombieRenderer {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("gelmod", "textures/entity/gel_zombie.png");

    public GelZombieRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public ResourceLocation getTextureLocation(Zombie entity) { return TEX; }

    /** Al formarse: sale como un montículo bajo y ancho que se va estirando hasta su altura normal. */
    @Override
    protected void scale(Zombie entity, PoseStack pose, float partialTick) {
        if (entity instanceof GelZombieEntity g) {
            float p = g.getFormProgress(partialTick);
            if (p < 1f) {
                float e = p * p * (3f - 2f * p);
                float sy = 0.12f + 0.88f * e + (float) Math.sin(p * 18f) * 0.05f * (1f - p);
                float sxz = 1.5f - 0.5f * e;
                pose.scale(sxz, Math.max(0.05f, sy), sxz);
            }
        }
    }
}
