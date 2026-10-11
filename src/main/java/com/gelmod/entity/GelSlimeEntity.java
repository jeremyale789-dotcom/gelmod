package com.gelmod.entity;

import com.gelmod.GelMod;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;

/**
 * Fase 1: slime rosa "derretido" que se ARRASTRA (20 corazones). Al notar a un jugador ~3 s se transforma:
 * charco -> dos tentáculos con cara -> masa grande -> la masa se abre y emerge el Zombi Gelatinoso.
 */
public class GelSlimeEntity extends Monster {
    // Línea de tiempo de la transformación (ticks), según el concepto:
    //   0-8   el slime se aplasta como un charco con cara
    //   8-34  brotan dos tentáculos con cara
    //   34-58 los tentáculos se funden en una masa grande
    //   58-72 la masa tiembla y se agrieta
    //   72    la masa se abre: sale el zombi (semi formado) con una mini explosión
    //   72-92 lo que queda de la masa se desploma y desaparece
    public static final int T_FLAT = 8, T_TEND_END = 34, T_MERGE_END = 58, T_BURST = 72, T_END = 92;

    private static final EntityDataAccessor<Integer> MORPH =
            SynchedEntityData.defineId(GelSlimeEntity.class, EntityDataSerializers.INT);

    private int noticeTicks;
    private boolean zombieSpawned;

    public GelSlimeEntity(EntityType<? extends Monster> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)   // 20 corazones
                .add(Attributes.MOVEMENT_SPEED, 0.16)   // lento: se arrastra
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MORPH, 0);
    }

    /** Ticks transcurridos de la transformación (0 = normal), con parte fraccionaria. Para el renderer. */
    public float getMorphTicks(float partialTick) {
        int m = entityData.get(MORPH);
        return m <= 0 ? 0f : m + partialTick;
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
        double x = getX(), y = getY(), z = getZ();

        if (morph == T_FLAT) {                                                   // se aplasta: empiezan los tentáculos
            sl.playSound(null, blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 1.0f, 1.2f);
            sl.sendParticles(ParticleTypes.ITEM_SLIME, x, y + 0.3, z, 10, 0.4, 0.2, 0.4, 0.05);
        }
        if (morph > T_FLAT && morph <= T_TEND_END && morph % 6 == 0) {           // brotan los tentáculos
            sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.HOSTILE, 1.0f, 0.7f + 0.5f * morph / T_TEND_END);
            sl.sendParticles(ParticleTypes.ITEM_SLIME, x, y + 0.8, z, 4, 0.4, 0.5, 0.4, 0.03);
        }
        if (morph > T_TEND_END && morph <= T_MERGE_END && morph % 5 == 0) {      // se funden en la masa
            sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.3f, 0.5f);
            sl.sendParticles(ParticleTypes.ITEM_SLIME, x, y + 1.0, z, 6, 0.5, 0.7, 0.5, 0.04);
        }
        if (morph > T_MERGE_END && morph < T_BURST && morph % 4 == 0) {          // crujidos antes de abrirse
            float pitch = 0.7f + 0.8f * (morph - T_MERGE_END) / (T_BURST - T_MERGE_END);
            sl.playSound(null, blockPosition(), SoundEvents.TURTLE_EGG_CRACK, SoundSource.HOSTILE, 1.1f, pitch);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PINK_CONCRETE.defaultBlockState()),
                    x, y + 1.0, z, 5, 0.5, 0.7, 0.5, 0.05);
        }
        if (morph == T_BURST) burst(sl);
        if (morph > T_BURST && morph % 3 == 0) {                                 // restos de la masa derritiéndose
            sl.sendParticles(ParticleTypes.ITEM_SLIME, x, y + 0.5, z, 3, 0.6, 0.3, 0.6, 0.02);
        }
        if (morph >= T_END) discard();
    }

    /** La masa se abre con una mini explosión y sale el zombi (semi formado). La masa restante se desploma después. */
    private void burst(ServerLevel sl) {
        if (zombieSpawned) return;
        zombieSpawned = true;
        GelZombieEntity z = GelMod.GEL_ZOMBIE.get().create(sl);
        if (z != null) {
            z.moveTo(getX(), getY(), getZ(), getYRot(), 0f);
            z.beginForming();
            sl.addFreshEntity(z);
        }
        double x = getX(), y = getY(), zz = getZ();
        sl.sendParticles(ParticleTypes.EXPLOSION, x, y + 0.8, zz, 1, 0, 0, 0, 0);                       // mini explosión
        sl.sendParticles(ParticleTypes.POOF, x, y + 0.8, zz, 16, 0.4, 0.4, 0.4, 0.08);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PINK_CONCRETE.defaultBlockState()),
                x, y + 1.0, zz, 50, 0.5, 0.6, 0.5, 0.15);
        sl.playSound(null, blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 1.5f, 0.5f);
        sl.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 0.6f, 1.5f);
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
