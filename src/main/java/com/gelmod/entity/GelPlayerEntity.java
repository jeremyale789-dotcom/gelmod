package com.gelmod.entity;

import com.gelmod.GelMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/** Fase final: clon gelatinoso del jugador. Pelea como un jugador (ver GelCombatGoal). */
public class GelPlayerEntity extends Monster {
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(GelPlayerEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private final SimpleContainer inventory = new SimpleContainer(36);

    public GelPlayerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            setDropChance(s, 2.0f); // >1 = siempre suelta y conserva durabilidad
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, GelMod.RANGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new GelCombatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Detecta jugadores (con línea de visión) hasta FOLLOW_RANGE = 30 bloques
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
    }

    public SimpleContainer getGelInventory() { return inventory; }

    public UUID getOwnerId() { return entityData.get(OWNER).orElse(null); }

    /** Copia skin (UUID/nombre), armadura, mano secundaria e inventario del jugador original. */
    public void copyFrom(UUID id, String name, SimpleContainer snap) {
        entityData.set(OWNER, Optional.ofNullable(id));
        setCustomName(Component.literal(name));
        setCustomNameVisible(true);
        for (int i = 0; i < 36; i++) inventory.setItem(i, snap.getItem(i).copy());
        setItemSlot(EquipmentSlot.FEET, snap.getItem(36).copy());
        setItemSlot(EquipmentSlot.LEGS, snap.getItem(37).copy());
        setItemSlot(EquipmentSlot.CHEST, snap.getItem(38).copy());
        setItemSlot(EquipmentSlot.HEAD, snap.getItem(39).copy());
        setItemSlot(EquipmentSlot.OFFHAND, snap.getItem(40).copy());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount % 8 == 0 && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 1.0, getZ(), 2, 0.25, 0.5, 0.25, 0.0);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) Containers.dropContents(level(), blockPosition(), inventory);
    }

    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.PLAYER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID o = getOwnerId();
        if (o != null) tag.putUUID("Owner", o);
        tag.put("GelInv", inventory.createTag(registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
        inventory.fromTag(tag.getList("GelInv", Tag.TAG_COMPOUND), registryAccess());
    }
}
