package com.gelmod.entity;

import com.gelmod.GelMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Fase 1: slime rosa "derretido" que se ARRASTRA. Al notar a un jugador ~3 s empieza a transformarse:
 * se estira, se aplasta en un charco y, a mitad de ese proceso, el Zombi Gelatinoso ya está emergiendo
 * de él (así no hay un cambio brusco).
 */
public class GelSlimeEntity extends Monster {
    public static final int MORPH_TOTAL = 30;      // duración de la transformación (ticks)
    private static final int ZOMBIE_AT = 12;       // en este tick aparece el zombi (ya "semi formado")

    private static final EntityDataAccessor<Integer> MORPH =
            SynchedEntityData.defineId(GelSlimeEntity.class, EntityDataSerializers.INT);

    private int noticeTicks;
    private boolean zombieSpawned;

    public GelSlimeEntity(EntityType<? extends Monster> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.16)   // lento: se arrastra
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MORPH, 0);
    }

    /** Progreso de la transformación 0..1 (0 = normal). Para el renderer. */
    public float getMorphProgress(float partialTick) {
        int m = entityData.get(MORPH);
        if (m <= 0) return 0f;
        return Mth.clamp((m + partialTick) / MORPH_TOTAL, 0f, 1f);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl) || !isAlive()) return;

        int morph = entityData.get(MORPH);
        if (morph > 0) { tickMorph(sl, morph); return; }

        Player p = level().getNearestPlayer(getX(), getY(), getZ(), 8.0, true);
        noticeTicks = (p != null) ? noticeTicks + 1 : Math.max(0, noticeTicks - 2);
        if (noticeTicks >= 60) {
            entityData.set(MORPH, 1);
            setNoAi(true);
            setInvulnerable(true);
            sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.2f, 0.6f);
            return;
        }

        if (tickCount % 20 == 0 && getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) {
            playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.35f, 0.8f);   // sonido al arrastrarse
        }
    }

    private void tickMorph(ServerLevel sl, int morph) {
        morph++;
        entityData.set(MORPH, morph);

        if (morph % 3 == 0) {
            sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 0.3, getZ(), 4, 0.35, 0.15, 0.35, 0.03);
        }
        if (morph == ZOMBIE_AT && !zombieSpawned) {
            zombieSpawned = true;
            GelZombieEntity z = GelMod.GEL_ZOMBIE.get().create(sl);
            if (z != null) {
                z.moveTo(getX(), getY(), getZ(), getYRot(), 0f);
                z.beginForming();                          // nace "semi formado" y se va completando
                sl.addFreshEntity(z);
                sl.playSound(null, blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 1.2f, 0.5f);
            }
        }
        if (morph >= MORPH_TOTAL) discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Morph", entityData.get(MORPH));
        tag.putBoolean("ZombieSpawned", zombieSpawned);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(MORPH, tag.getInt("Morph"));
        zombieSpawned = tag.getBoolean("ZombieSpawned");
    }

    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.SLIME_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SLIME_DEATH; }
}
