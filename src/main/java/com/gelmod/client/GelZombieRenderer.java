package com.gelmod.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

public class GelZombieRenderer extends ZombieRenderer {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("gelmod", "textures/entity/gel_zombie.png");

    public GelZombieRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public ResourceLocation getTextureLocation(Zombie entity) { return TEX; }
}
