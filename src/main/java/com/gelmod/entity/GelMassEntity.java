package com.gelmod.entity;

import com.gelmod.GelMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Fase 3: masa de carne pálida con ramas y caras (estilo transfiguración de Mahito). Crece, se retuerce,
 * va brotando "lóbulos" (masas más pequeñas) alrededor y luego colapsa. Al terminar nace el Jugador Gelatinoso.
 * El aspecto lo dibuja GelMassRenderer/GelMassModel (cliente) usando el valor sincronizado GROWTH.
 *
 * Ticks (masa principal): 0-80 crece (1->8), 80-100 mantiene, 90 el clon aparece dentro y empieza a formarse,
 * 100-120 la masa colapsa revelándolo, 120 la masa desaparece.
 */
public class GelMassEntity extends Slime {
    private static final EntityDataAccessor<Float> GROWTH =
            SynchedEntityData.defineId(GelMassEntity.class, EntityDataSerializers.FLOAT);

    private static final int GROW = 80, HOLD = 100, END = 120;
    private static final int CLONE_AT = 90;   // el clon aparece dentro de la masa y se va formando mientras ésta se contrae
    private static final float MAX_GROWTH = 8f;
    private static final int MAX_LOBES = 10;

    // --- masa principal
    private UUID victimId;
    private String victimName = "";
    private final SimpleContainer snapshot = new SimpleContainer(41); // 0-35 inv, 36-39 armadura (pies..cabeza), 40 offhand
    private int phase;
    private int lobesSpawned;

    // --- lóbulo
    private boolean lobe;
    private int lobeMax = 3, lobeLife = 40, lobeAge;

    // interpolación en el cliente
    private float growthO = 1f, growthN = 1f;

    public GelMassEntity(EntityType<? extends Slime> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GROWTH, 1f);
    }

    /** Tamaño visual actual (1..8), suavizado entre ticks. */
    public float getGrowth(float partialTick) { return Mth.lerp(partialTick, growthO, growthN); }

    private void setGrowth(float g) {
        entityData.set(GROWTH, g);
        int size = Math.max(1, Math.round(g));
        if (size != getSize()) setSize(size, false);
    }

    /** Guarda la víctima y una COPIA de su inventario en este instante. */
    public void setVictim(ServerPlayer p) {
        victimId = p.getUUID();
        victimName = p.getGameProfile().getName();
        for (int i = 0; i < 36; i++) snapshot.setItem(i, p.getInventory().items.get(i).copy());
        for (int i = 0; i < 4; i++) snapshot.setItem(36 + i, p.getInventory().armor.get(i).copy());
        snapshot.setItem(40, p.getInventory().offhand.get(0).copy());
    }

    @Override
    public void tick() {
        super.tick();
        growthO = growthN;
        growthN = entityData.get(GROWTH);
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        if (lobe) { tickLobe(sl); return; }

        phase++;

        float g;
        if (phase <= GROW) { float t = phase / (float) GROW; g = 1f + (MAX_GROWTH - 1f) * (t * t * (3f - 2f * t)); }
        else if (phase <= HOLD) g = MAX_GROWTH;
        else g = Math.max(1f, MAX_GROWTH - (MAX_GROWTH - 1f) * (phase - HOLD) / (float) (END - HOLD));
        setGrowth(g);

        // van brotando masas más pequeñas alrededor (forma irregular)
        if (phase >= 6 && phase <= 66 && phase % 6 == 0 && lobesSpawned < MAX_LOBES) spawnLobe(sl);

        if (phase % 3 == 0) {
            sl.sendParticles(ParticleTypes.ASH, getX(), getY() + g * 0.8, getZ(),
                    8, g * 0.35, g * 0.45, g * 0.35, 0.02);
        }
        if (phase % 10 == 0) {
            sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.6f, 0.4f + (phase % 20) * 0.01f);
        }
        if (phase % 16 == 0) {
            sl.playSound(null, blockPosition(), SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 1.4f, 0.5f);
        }
        if (victimId != null && phase <= HOLD && phase % 20 == 0) {
            Player v = sl.getPlayerByUUID(victimId);
            if (v != null) v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }
        if (phase == CLONE_AT) spawnClone(sl);
        if (phase >= END) discard();
    }

    private void spawnLobe(ServerLevel sl) {
        GelMassEntity l = GelMod.GEL_MASS.get().create(sl);
        if (l == null) return;
        double ang = random.nextDouble() * Math.PI * 2;
        double r = 1.5 + random.nextDouble() * 3.5;
        l.moveTo(getX() + Math.cos(ang) * r, getY(), getZ() + Math.sin(ang) * r, random.nextFloat() * 360f, 0f);
        l.lobe = true;
        l.lobeMax = 2 + random.nextInt(4);            // tamaños 2..5
        l.lobeLife = Math.max(30, END - phase);        // desaparece junto con la masa
        sl.addFreshEntity(l);
        sl.playSound(null, l.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.0f, 0.4f + random.nextFloat() * 0.4f);
        lobesSpawned++;
    }

    private void tickLobe(ServerLevel sl) {
        lobeAge++;
        int remaining = lobeLife - lobeAge;
        float g;
        if (remaining <= 20) g = 1f + Math.max(0, remaining) * (lobeMax - 1) / 20f;   // se encoge al final
        else g = Math.min(lobeMax, 1f + lobeAge * (lobeMax - 1) / 20f);                // crece rápido
        setGrowth(g);
        if (lobeAge % 5 == 0) {
            sl.sendParticles(ParticleTypes.ASH, getX(), getY() + g * 0.6, getZ(), 2, 0.2, 0.2, 0.2, 0.01);
        }
        if (remaining <= 0) discard();
    }

    private void spawnClone(ServerLevel sl) {
        GelPlayerEntity gp = GelMod.GEL_PLAYER.get().create(sl);
        if (gp != null) {
            gp.moveTo(getX(), getY(), getZ(), getYRot(), 0f);
            gp.copyFrom(victimId, victimName, snapshot);
            gp.beginForming();                              // nace "semi formado" dentro de la masa
            sl.addFreshEntity(gp);
            sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 1, getZ(), 40, 0.5, 0.9, 0.5, 0.08);
            sl.playSound(null, blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 1.5f, 0.5f);
        }
    }

    /** La masa no empuja a otras entidades (para que el clon no salga disparado mientras se forma). */
    @Override
    public boolean isPushable() { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Lobe", lobe);
        tag.putInt("LobeMax", lobeMax);
        tag.putInt("LobeLife", lobeLife);
        tag.putInt("LobeAge", lobeAge);
        tag.putInt("LobesSpawned", lobesSpawned);
        if (victimId != null) tag.putUUID("Victim", victimId);
        tag.putString("VictimName", victimName);
        tag.putInt("Phase", phase);
        tag.put("Snapshot", snapshot.createTag(registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lobe = tag.getBoolean("Lobe");
        lobeMax = Math.max(1, tag.getInt("LobeMax"));
        lobeLife = tag.getInt("LobeLife");
        lobeAge = tag.getInt("LobeAge");
        lobesSpawned = tag.getInt("LobesSpawned");
        if (tag.hasUUID("Victim")) victimId = tag.getUUID("Victim");
        victimName = tag.getString("VictimName");
        phase = tag.getInt("Phase");
        snapshot.fromTag(tag.getList("Snapshot", Tag.TAG_COMPOUND), registryAccess());
    }
}
