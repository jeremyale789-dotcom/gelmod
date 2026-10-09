package com.gelmod.entity;

import com.gelmod.GelMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Fase 3: masa que se expande (estilo transfiguración final de Mahito), se queda un momento y se contrae.
 * Al terminar nace el Jugador Gelatinoso con skin e inventario copiados.
 * Ticks: 0-80 expande (1->8), 80-100 mantiene, 100-120 colapsa, 120 nace el clon.
 */
public class GelMassEntity extends Slime {
    private static final int GROW = 80, HOLD = 100, END = 120, MAX_SIZE = 8;

    private UUID victimId;
    private String victimName = "";
    private final SimpleContainer snapshot = new SimpleContainer(41); // 0-35 inv, 36-39 armadura (pies..cabeza), 40 offhand
    private int phase;

    public GelMassEntity(EntityType<? extends Slime> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
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
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        phase++;

        int size;
        if (phase <= GROW) size = 1 + phase * (MAX_SIZE - 1) / GROW;
        else if (phase <= HOLD) size = MAX_SIZE;
        else size = Math.max(1, MAX_SIZE - (phase - HOLD) * (MAX_SIZE - 1) / (END - HOLD));
        if (size != getSize()) setSize(size, false);

        if (phase % 4 == 0) {
            sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + getSize() * 0.5, getZ(),
                    6, getSize() * 0.3, getSize() * 0.3, getSize() * 0.3, 0.02);
        }
        if (phase % 10 == 0) {
            sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.5f, 0.4f + (phase % 20) * 0.01f);
        }
        if (victimId != null && phase <= HOLD && phase % 20 == 0) {
            Player v = sl.getPlayerByUUID(victimId);
            if (v != null) v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }
        if (phase >= END) spawnClone(sl);
    }

    private void spawnClone(ServerLevel sl) {
        GelPlayerEntity gp = GelMod.GEL_PLAYER.get().create(sl);
        if (gp != null) {
            gp.moveTo(getX(), getY(), getZ(), getYRot(), 0f);
            gp.copyFrom(victimId, victimName, snapshot);
            sl.addFreshEntity(gp);
            sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 1, getZ(), 40, 0.5, 0.9, 0.5, 0.08);
            sl.playSound(null, blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 1.5f, 0.5f);
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (victimId != null) tag.putUUID("Victim", victimId);
        tag.putString("VictimName", victimName);
        tag.putInt("Phase", phase);
        tag.put("Snapshot", snapshot.createTag(registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Victim")) victimId = tag.getUUID("Victim");
        victimName = tag.getString("VictimName");
        phase = tag.getInt("Phase");
        snapshot.fromTag(tag.getList("Snapshot", Tag.TAG_COMPOUND), registryAccess());
    }
}
