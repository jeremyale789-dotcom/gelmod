package com.gelmod;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Ajustes guardados con el mundo (se conservan al reiniciar). */
public class GelSettings extends SavedData {
    private static final String NAME = "gelmod_settings";
    private boolean naturalSpawn = true;
    private boolean daySpawn = false;
    /** Copia en memoria para consultarla desde las reglas de spawn sin acceder al servidor. */
    public static volatile boolean DAY_CACHE = false;

    public static GelSettings get(MinecraftServer server) {
        GelSettings s = server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(GelSettings::new, GelSettings::load, null), NAME);
        DAY_CACHE = s.daySpawn;
        return s;
    }

    private static GelSettings load(CompoundTag tag, HolderLookup.Provider registries) {
        GelSettings s = new GelSettings();
        s.naturalSpawn = !tag.contains("NaturalSpawn") || tag.getBoolean("NaturalSpawn");
        s.daySpawn = tag.getBoolean("DaySpawn");
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("NaturalSpawn", naturalSpawn);
        tag.putBoolean("DaySpawn", daySpawn);
        return tag;
    }

    public boolean isNaturalSpawn() { return naturalSpawn; }

    public void setNaturalSpawn(boolean v) { this.naturalSpawn = v; setDirty(); }

    public boolean isDaySpawn() { return daySpawn; }

    public void setDaySpawn(boolean v) { this.daySpawn = v; DAY_CACHE = v; setDirty(); }
}
