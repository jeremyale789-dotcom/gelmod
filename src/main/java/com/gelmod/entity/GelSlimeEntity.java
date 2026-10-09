package com.gelmod.entity;

import com.gelmod.GelMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/** Fase 1: slime pequeño que, al notar a un jugador cerca, se transforma en Zombi Gelatinoso. */
public class GelSlimeEntity extends Slime {
    private int noticeTicks;

    public GelSlimeEntity(EntityType<? extends Slime> type, Level level) { super(type, level); }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, diff, reason, data);
        this.setSize(1, true); // siempre pequeño: no se divide al morir
        return d;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive()) return;
        Player p = level().getNearestPlayer(getX(), getY(), getZ(), 8.0, true);
        noticeTicks = (p != null) ? noticeTicks + 1 : Math.max(0, noticeTicks - 2);
        if (noticeTicks >= 60) transform();
    }

    private void transform() {
        GelZombieEntity z = this.convertTo(GelMod.GEL_ZOMBIE.get(), false);
        if (z != null && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ITEM_SLIME, z.getX(), z.getY() + 1, z.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
            sl.playSound(null, z.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.2f, 0.6f);
        }
    }
}
