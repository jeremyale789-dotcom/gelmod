package com.gelmod.entity;

import com.gelmod.GelMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/** Fase 2: zombi gelatinoso. Al golpear a un jugador se derrite y deja una Masa Gelatinosa. */
public class GelZombieEntity extends Zombie {
    /** Duración (ticks) de la animación de "formarse". */
    public static final int FORM_TICKS = 30;
    private static final EntityDataAccessor<Integer> FORM =
            SynchedEntityData.defineId(GelZombieEntity.class, EntityDataSerializers.INT);

    public GelZombieEntity(EntityType<? extends Zombie> type, Level level) { super(type, level); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FORM, 0);
    }

    /** Empieza "semi formado": quieto e invulnerable mientras se va completando. */
    public void beginForming() {
        entityData.set(FORM, FORM_TICKS);
        setNoAi(true);
        setInvulnerable(true);
    }

    /** 0 = recién empieza a formarse, 1 = completo. Para el renderer. */
    public float getFormProgress(float partialTick) {
        int f = entityData.get(FORM);
        if (f <= 0) return 1f;
        return Mth.clamp(1f - (f - partialTick) / FORM_TICKS, 0f, 1f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        int f = entityData.get(FORM);
        if (f > 0) {
            f--;
            entityData.set(FORM, f);
            if (level() instanceof ServerLevel sl && f % 2 == 0) {
                sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 0.2, getZ(), 3, 0.3, 0.1, 0.3, 0.02);
            }
            if (f == 0) { setNoAi(false); setInvulnerable(false); }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Form", entityData.get(FORM));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(FORM, tag.getInt("Form"));
    }

    /** Zombi normal con el DOBLE de velocidad (0.23 x 2 = 0.46). */
    public static AttributeSupplier.Builder createGelAttributes() {
        return Zombie.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.46);
    }

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
