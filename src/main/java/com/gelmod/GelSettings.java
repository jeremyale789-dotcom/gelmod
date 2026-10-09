package com.gelmod;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Ajustes guardados con el mundo (se conservan al reiniciar). */
public class GelSettings extends SavedData {
    private static final String NAME = "gelmod_settings";
    private boolean naturalSpawn = true;

    public static GelSettings get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(GelSettings::new, GelSettings::load, null), NAME);
    }

    private static GelSettings load(CompoundTag tag, HolderLookup.Provider registries) {
        GelSettings s = new GelSettings();
        s.naturalSpawn = !tag.contains("NaturalSpawn") || tag.getBoolean("NaturalSpawn");
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("NaturalSpawn", naturalSpawn);
        return tag;
    }

    public boolean isNaturalSpawn() { return naturalSpawn; }

    public void setNaturalSpawn(boolean v) { this.naturalSpawn = v; setDirty(); }
}
