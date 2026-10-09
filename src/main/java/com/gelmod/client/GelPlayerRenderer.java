package com.gelmod.client;

import com.gelmod.entity.GelPlayerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

/** Dibuja al clon con la skin real del jugador copiado + una capa verde translúcida de gelatina. */
public class GelPlayerRenderer extends LivingEntityRenderer<GelPlayerEntity, PlayerModel<GelPlayerEntity>> {
    private final PlayerModel<GelPlayerEntity> wide;
    private final PlayerModel<GelPlayerEntity> slim;

    public GelPlayerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.wide = this.getModel();
        this.slim = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
        addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
        addLayer(new GelOverlayLayer(this));
    }

    private PlayerSkin skin(GelPlayerEntity e) {
        UUID id = e.getOwnerId();
        if (id == null) return DefaultPlayerSkin.get(e.getUUID());
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        PlayerInfo info = conn == null ? null : conn.getPlayerInfo(id);
        return info != null ? info.getSkin() : DefaultPlayerSkin.get(id);
    }

    @Override
    public ResourceLocation getTextureLocation(GelPlayerEntity e) { return skin(e).texture(); }

    @Override
    public void render(GelPlayerEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buf, int light) {
        this.model = skin(e).model() == PlayerSkin.Model.SLIM ? slim : wide;
        this.model.rightArmPose = armPose(e, InteractionHand.MAIN_HAND);
        this.model.leftArmPose = armPose(e, InteractionHand.OFF_HAND);
        super.render(e, yaw, partialTick, pose, buf, light);
    }

    private ArmPose armPose(GelPlayerEntity e, InteractionHand hand) {
        ItemStack s = e.getItemInHand(hand);
        if (s.isEmpty()) return ArmPose.EMPTY;
        if (e.isUsingItem() && e.getUsedItemHand() == hand) {
            if (s.is(Items.BOW)) return ArmPose.BOW_AND_ARROW;
            if (s.is(Items.SHIELD)) return ArmPose.BLOCK;
        }
        return ArmPose.ITEM;
    }
}
