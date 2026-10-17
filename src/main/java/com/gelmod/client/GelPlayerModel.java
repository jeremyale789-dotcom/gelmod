package com.gelmod.client;

import com.gelmod.entity.GelPlayerEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.UseAnim;

/** Modelo del jugador con la animación de comer/beber (el brazo sube a la boca y se mueve al masticar). */
public class GelPlayerModel extends PlayerModel<GelPlayerEntity> {
    public GelPlayerModel(ModelPart root, boolean slim) { super(root, slim); }

    @Override
    public void setupAnim(GelPlayerEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(e, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (e.isUsingItem()) {
            UseAnim ua = e.getUseItem().getUseAnimation();
            if (ua == UseAnim.EAT || ua == UseAnim.DRINK) {
                boolean right = e.getUsedItemHand() == InteractionHand.MAIN_HAND;
                ModelPart arm = right ? this.rightArm : this.leftArm;
                arm.xRot = -1.25f + Mth.sin(ageInTicks * 1.2f) * 0.08f;
                arm.yRot = right ? -0.5f : 0.5f;
                arm.zRot = 0f;
                (right ? this.rightSleeve : this.leftSleeve).copyFrom(arm);
                this.head.xRot = Math.max(this.head.xRot, 0.2f);
                this.hat.copyFrom(this.head);
            }
        }
    }
}
