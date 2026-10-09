package com.gelmod.entity;

import com.gelmod.GelMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/** Fase 2: zombi gelatinoso. Al golpear a un jugador se derrite y deja una Masa Gelatinosa. */
public class GelZombieEntity extends Zombie {
    public GelZombieEntity(EntityType<? extends Zombie> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createGelAttributes() { return Zombie.createAttributes(); }

    @Override
    protected boolean isSunSensitive() { return false; }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !level().isClientSide && target instanceof ServerPlayer p && !p.isCreative() && !p.isSpectator()) {
            GelMassEntity mass = GelMod.GEL_MASS.get().create(level());
            if (mass != null) {
                mass.moveTo(getX(), getY(), getZ(), getYRot(), 0f);
                mass.setVictim(p);
                level().addFreshEntity(mass);
                level().playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.5f, 0.5f);
                discard();
            }
        }
        return hit;
    }
}
