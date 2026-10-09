package com.gelmod;

import com.gelmod.entity.GelSlimeEntity;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Comando:  /gelmod spawn            -> muestra el estado
 *           /gelmod spawn true|false -> activa/desactiva el spawn natural del Slime Gelatinoso en el overworld
 * (Los huevos de spawn y /summon siguen funcionando siempre.)
 */
@Mod.EventBusSubscriber(modid = GelMod.ID)
public class GelEvents {

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("gelmod")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("spawn")
                        .executes(ctx -> {
                            boolean on = GelSettings.get(ctx.getSource().getServer()).isNaturalSpawn();
                            msg(ctx.getSource(), "Spawn natural del Slime Gelatinoso: " + (on ? "ACTIVADO" : "DESACTIVADO"));
                            return on ? 1 : 0;
                        })
                        .then(Commands.argument("activado", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    boolean v = BoolArgumentType.getBool(ctx, "activado");
                                    GelSettings.get(ctx.getSource().getServer()).setNaturalSpawn(v);
                                    msg(ctx.getSource(), "Spawn natural del Slime Gelatinoso: " + (v ? "ACTIVADO" : "DESACTIVADO"));
                                    return 1;
                                }))));
    }

    private static void msg(CommandSourceStack src, String text) {
        src.sendSuccess(() -> Component.literal(text), true);
    }

    /** Bloquea solo los spawns naturales cuando el ajuste está desactivado. */
    @SubscribeEvent
    public static void onSpawn(MobSpawnEvent.FinalizeSpawn e) {
        if (!(e.getEntity() instanceof GelSlimeEntity)) return;
        MobSpawnType t = e.getSpawnType();
        if (t != MobSpawnType.NATURAL && t != MobSpawnType.CHUNK_GENERATION) return;
        if (!GelSettings.get(e.getLevel().getLevel().getServer()).isNaturalSpawn()) e.setSpawnCancelled(true);
    }
}
