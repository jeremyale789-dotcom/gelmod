package com.gelmod;

import com.gelmod.entity.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(GelMod.ID)
public class GelMod {
    public static final String ID = "gelmod";
    /** Radio de detección del Jugador Gelatinoso (bloques). */
    public static final double RANGE = 30.0;
    /** Alcance del "kill aura" (bloques). Vanilla = 3. */
    public static final double REACH = 6.0;

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);

    public static final RegistryObject<EntityType<GelSlimeEntity>> GEL_SLIME = ENTITIES.register("gel_slime",
            () -> EntityType.Builder.<GelSlimeEntity>of(GelSlimeEntity::new, MobCategory.MONSTER)
                    .sized(0.52f, 0.52f).eyeHeight(0.325f).clientTrackingRange(10).build("gel_slime"));
    public static final RegistryObject<EntityType<GelZombieEntity>> GEL_ZOMBIE = ENTITIES.register("gel_zombie",
            () -> EntityType.Builder.<GelZombieEntity>of(GelZombieEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).eyeHeight(1.74f).clientTrackingRange(8).build("gel_zombie"));
    public static final RegistryObject<EntityType<GelMassEntity>> GEL_MASS = ENTITIES.register("gel_mass",
            () -> EntityType.Builder.<GelMassEntity>of(GelMassEntity::new, MobCategory.MONSTER)
                    .sized(0.52f, 0.52f).eyeHeight(0.325f).clientTrackingRange(16).build("gel_mass"));
    public static final RegistryObject<EntityType<GelPlayerEntity>> GEL_PLAYER = ENTITIES.register("gel_player",
            () -> EntityType.Builder.<GelPlayerEntity>of(GelPlayerEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(10).build("gel_player"));

    public static final RegistryObject<Item> EGG_SLIME = ITEMS.register("gel_slime_spawn_egg",
            () -> new ForgeSpawnEggItem(GEL_SLIME, 0x55DD55, 0x1E7A2E, new Item.Properties()));
    public static final RegistryObject<Item> EGG_ZOMBIE = ITEMS.register("gel_zombie_spawn_egg",
            () -> new ForgeSpawnEggItem(GEL_ZOMBIE, 0x55DD55, 0x2B5F2B, new Item.Properties()));
    public static final RegistryObject<Item> EGG_PLAYER = ITEMS.register("gel_player_spawn_egg",
            () -> new ForgeSpawnEggItem(GEL_PLAYER, 0x55DD55, 0xFFFFFF, new Item.Properties()));

    public GelMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(this::attributes);
        bus.addListener(this::spawnPlacement);
        bus.addListener(this::tabs);
    }

    private void attributes(EntityAttributeCreationEvent e) {
        e.put(GEL_SLIME.get(), Monster.createMonsterAttributes().build());
        e.put(GEL_MASS.get(), Monster.createMonsterAttributes().build());
        e.put(GEL_ZOMBIE.get(), GelZombieEntity.createGelAttributes().build());
        e.put(GEL_PLAYER.get(), GelPlayerEntity.createAttributes().build());
    }

    private void spawnPlacement(SpawnPlacementRegisterEvent e) {
        e.register(GEL_SLIME.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getDifficulty() != Difficulty.PEACEFUL
                        && Monster.isDarkEnoughToSpawn(level, pos, random)
                        && Mob.checkMobSpawnRules(type, level, reason, pos, random),
                SpawnPlacementRegisterEvent.Operation.OR);
    }

    private void tabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            e.accept(EGG_SLIME.get()); e.accept(EGG_ZOMBIE.get()); e.accept(EGG_PLAYER.get());
        }
    }
}
