package com.gelmod.client;

import com.gelmod.GelMod;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GelMod.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GelModClient {
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(GelMod.GEL_SLIME.get(), SlimeRenderer::new);
        e.registerEntityRenderer(GelMod.GEL_MASS.get(), SlimeRenderer::new);
        e.registerEntityRenderer(GelMod.GEL_ZOMBIE.get(), GelZombieRenderer::new);
        e.registerEntityRenderer(GelMod.GEL_PLAYER.get(), GelPlayerRenderer::new);
    }
}
