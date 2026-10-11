package com.gelmod.client;

import com.gelmod.GelMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GelMod.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GelModClient {
    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(GelMassModel.LAYER, GelMassModel::createBodyLayer);
        e.registerLayerDefinition(GelSlimeModel.INNER, () -> GelSlimeModel.createLayer(false));
        e.registerLayerDefinition(GelSlimeModel.OUTER, () -> GelSlimeModel.createLayer(true));
        e.registerLayerDefinition(GelEggModel.LAYER, GelEggModel::createLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(GelMod.GEL_SLIME.get(), GelSlimeRenderer::new);
        e.registerEntityRenderer(GelMod.GEL_MASS.get(), GelMassRenderer::new);
        e.registerEntityRenderer(GelMod.GEL_ZOMBIE.get(), GelZombieRenderer::new);
        e.registerEntityRenderer(GelMod.GEL_PLAYER.get(), GelPlayerRenderer::new);
    }
}
