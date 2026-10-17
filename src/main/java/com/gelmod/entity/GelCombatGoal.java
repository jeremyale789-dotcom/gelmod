package com.gelmod.entity;

import com.gelmod.GelMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Cerebro de combate del Jugador Gelatinoso. Comportamiento "de jugador":
 *  - Se acerca corriendo, se planta y golpea; después del golpe a veces RETROCEDE (golpea y huye) o da un paso lateral corto.
 *    Ya no da vueltas sin parar alrededor del objetivo.
 *  - Escudo inteligente: lo levanta cuando el rival empieza a golpear o a disparar, y entre golpes; lo baja para atacar.
 *  - Con poca vida se aleja con el escudo arriba y COME de verdad (animación de comer, sonido y partículas), y luego vuelve a pelear.
 *  - Aimbot (mira siempre al objetivo y adelanta el tiro con el arco), kill aura (REACH) y cooldown de golpe (ATTACK_COOLDOWN_TICKS).
 *  - Equipa tótem con poca vida, elige la mejor arma, usa el arco a distancia.
 */
public class GelCombatGoal extends Goal {
    private final GelPlayerEntity m;
    private int attackCd, bowTime, eatCd, gearCd, tick, blockUntil, modeTicks, mode, fleeTicks;
    private float strafeDir = 1f;
    private boolean eating, eatGolden;
    private int eatNutrition;
    private Vec3 lastPos = Vec3.ZERO;

    private static final int NONE = 0, BACK = 1, SIDE = 2;

    public GelCombatGoal(GelPlayerEntity mob) {
        this.m = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override public boolean canUse() { LivingEntity t = m.getTarget(); return t != null && t.isAlive(); }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { lastPos = Vec3.ZERO; bowTime = 0; mode = NONE; modeTicks = 0; eating = false; fleeTicks = 0; }
    @Override public void stop() { m.stopUsingItem(); m.getNavigation().stop(); m.setSprinting(false); eating = false; }

    private float sign() { return m.getRandom().nextBoolean() ? 1f : -1f; }

    @Override
    public void tick() {
        LivingEntity t = m.getTarget();
        if (t == null || !t.isAlive()) return;
        tick++;

        Vec3 tv = lastPos == Vec3.ZERO ? Vec3.ZERO : t.position().subtract(lastPos);
        lastPos = t.position();
        double dist = m.distanceTo(t);

        if (attackCd > 0) attackCd--;
        if (eatCd > 0) eatCd--;
        if (modeTicks > 0) modeTicks--; else mode = NONE;
        boolean sees = m.hasLineOfSight(t);

        // comer / curarse (tiene prioridad)
        if (handleEating(t, dist)) { aimAt(t.getEyePosition()); jumpIfStuck(); return; }

        if (--gearCd <= 0) { gearCd = 5; manageGear(dist); }
        boolean bow = m.getMainHandItem().is(Items.BOW);

        Vec3 aim = t.getEyePosition();
        if (bow && m.isUsingItem()) aim = lead(t, tv, dist);
        aimAt(aim);

        if (bow) ranged(t, tv, dist, sees); else melee(t, dist, sees);
        jumpIfStuck();
    }

    private void jumpIfStuck() { if (m.horizontalCollision && m.onGround()) m.getJumpControl().jump(); }

    // ---------------------------------------------------------------- aimbot
    private void aimAt(Vec3 p) {
        Vec3 eye = m.getEyePosition();
        double dx = p.x - eye.x, dy = p.y - eye.y, dz = p.z - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * 57.29577951) - 90f;
        float pitch = (float) -(Mth.atan2(dy, h) * 57.29577951);
        m.setYRot(yaw); m.setXRot(pitch); m.setYHeadRot(yaw); m.setYBodyRot(yaw);
    }

    private Vec3 lead(LivingEntity t, Vec3 tv, double dist) {
        double ticks = dist / 3.0;
        return t.position().add(0, t.getBbHeight() * 0.6, 0).add(tv.x * ticks, 0, tv.z * ticks);
    }

    // ---------------------------------------------------------------- comer
    private boolean handleEating(LivingEntity t, double dist) {
        if (eating) {
            if (!m.isUsingItem()) { eating = false; eatCd = 100; return false; }
            if (m.getUseItemRemainingTicks() == 1 && !eatGolden) m.heal(eatNutrition);
            backAway();                                        // se va alejando mientras come
            return true;
        }
        float hp = m.getHealth();
        if (eatCd == 0 && hp <= 11f) {
            int idx = foodIndex();
            if (idx >= 0) {
                if (dist >= 4.5 || (hp <= 5f && dist >= 2.5) || fleeTicks > 30) { startEat(idx); fleeTicks = 0; return true; }
                fleeTicks++;                                   // primero retrocede con el escudo arriba
                if (m.getOffhandItem().is(Items.SHIELD) && !m.isUsingItem()) m.startUsingItem(InteractionHand.OFF_HAND);
                backAway();
                return true;
            }
        }
        fleeTicks = 0;
        return false;
    }

    private void startEat(int idx) {
        ItemStack s = m.getGelInventory().getItem(idx);
        eatGolden = s.is(Items.GOLDEN_APPLE) || s.is(Items.ENCHANTED_GOLDEN_APPLE);
        FoodProperties f = s.get(DataComponents.FOOD);
        eatNutrition = f != null ? f.nutrition() : 2;
        swap(EquipmentSlot.MAINHAND, idx);                     // la comida pasa a la mano principal
        m.startUsingItem(InteractionHand.MAIN_HAND);           // animación, sonido y partículas de comer
        eating = m.isUsingItem();
        if (!eating) eatCd = 40;
    }

    private int foodIndex() {
        SimpleContainer inv = m.getGelInventory();
        int any = -1;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(Items.ENCHANTED_GOLDEN_APPLE) || s.is(Items.GOLDEN_APPLE)) return i;
            if (any < 0 && s.has(DataComponents.FOOD)) any = i;
        }
        return any;
    }

    private void backAway() {
        m.getNavigation().stop();
        m.getMoveControl().strafe(-0.9f, 0f);
    }

    // ---------------------------------------------------------------- melee
    private void melee(LivingEntity t, double dist, boolean sees) {
        boolean hasShield = m.getOffhandItem().is(Items.SHIELD);
        boolean ready = dist <= GelMod.REACH && sees && attackCd == 0;

        // --- escudo inteligente: se cubre cuando el rival empieza a golpear / disparar y entre golpes
        float swing = t.getAttackAnim(1.0f);
        boolean threat = dist <= 5.5 && ((swing > 0f && swing < 0.75f) || (t.isUsingItem() && t.getUseItem().is(Items.BOW)));
        if (hasShield && threat) blockUntil = tick + 14;
        boolean wantBlock = hasShield && !ready && dist <= 6 && (tick < blockUntil || attackCd > 6);
        if (wantBlock && !m.isUsingItem()) m.startUsingItem(InteractionHand.OFF_HAND);
        else if (!wantBlock && m.isUsingItem() && m.getUseItem().is(Items.SHIELD)) m.stopUsingItem();

        // --- movimiento
        if (dist > 7) {
            m.setSprinting(true);
            m.getNavigation().moveTo(t, 1.35);
        } else {
            m.setSprinting(false);
            if (mode == BACK && modeTicks > 0) {
                m.getNavigation().stop();
                m.getMoveControl().strafe(-0.9f, strafeDir * 0.25f);       // retrocede mirando al rival
            } else if (mode == SIDE && modeTicks > 0) {
                m.getNavigation().stop();
                m.getMoveControl().strafe(0.1f, strafeDir * 0.9f);         // paso lateral corto
            } else if (dist > 3.2) {
                m.getNavigation().moveTo(t, 1.15);
                if (dist < 6 && m.onGround() && m.getRandom().nextFloat() < 0.03f) m.getJumpControl().jump();   // salto de aproximación
            } else if (dist < 1.6) {
                m.getNavigation().stop();
                m.getMoveControl().strafe(-0.6f, 0f);
            } else {
                m.getNavigation().stop();                                   // se planta y espera su momento
                if (attackCd > 3 && m.getRandom().nextFloat() < 0.02f) {
                    mode = SIDE; modeTicks = 10 + m.getRandom().nextInt(10); strafeDir = sign();
                }
            }
        }

        // --- ataque (kill aura)
        if (ready) {
            if (m.isUsingItem() && m.getUseItem().is(Items.SHIELD)) m.stopUsingItem();
            m.swing(InteractionHand.MAIN_HAND);
            m.doHurtTarget(t);
            attackCd = GelMod.ATTACK_COOLDOWN_TICKS;
            if (m.getRandom().nextFloat() < 0.5f) {                         // golpea y retrocede
                mode = BACK; modeTicks = 8 + m.getRandom().nextInt(7); strafeDir = sign();
            }
        }
    }

    // ---------------------------------------------------------------- arco
    private void ranged(LivingEntity t, Vec3 tv, double dist, boolean sees) {
        m.setSprinting(false);
        if (dist < 7) {
            Vec3 away = m.position().subtract(t.position()).normalize().scale(8).add(m.position());
            m.getNavigation().moveTo(away.x, away.y, away.z, 1.3);
        } else if (dist > 22) {
            m.getNavigation().moveTo(t, 1.1);
        } else {
            m.getNavigation().stop();
            if (mode == SIDE && modeTicks > 0) m.getMoveControl().strafe(0.1f, strafeDir * 0.9f);
            else if (m.getRandom().nextFloat() < 0.04f) { mode = SIDE; modeTicks = 8 + m.getRandom().nextInt(8); strafeDir = sign(); }
        }
        if (sees && findArrow() >= 0) {
            if (!m.isUsingItem()) { m.startUsingItem(InteractionHand.MAIN_HAND); bowTime = 0; }
            else if (++bowTime >= 20) { shoot(t, tv, dist); m.stopUsingItem(); bowTime = 0; }
        } else if (m.isUsingItem()) {
            m.stopUsingItem();
        }
    }

    private void shoot(LivingEntity t, Vec3 tv, double dist) {
        SimpleContainer inv = m.getGelInventory();
        int ai = findArrow();
        if (ai < 0) return;
        ItemStack ammo = inv.getItem(ai);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(m, ammo, 1.0f, m.getMainHandItem());
        Vec3 p = lead(t, tv, dist), from = m.getEyePosition();
        double dx = p.x - from.x, dy = p.y - from.y, dz = p.z - from.z;
        double h = Math.sqrt(dx * dx + dz * dz), flight = h / 3.0;
        dy += 0.03 * flight * flight;
        arrow.shoot(dx, dy, dz, 3.0f, 0.0f);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        m.level().addFreshEntity(arrow);
        m.playSound(SoundEvents.ARROW_SHOOT, 1.0f, 1.0f);
        ammo.shrink(1);
    }

    // ---------------------------------------------------------------- equipo
    private void manageGear(double dist) {
        SimpleContainer inv = m.getGelInventory();
        ItemStack off = m.getOffhandItem();
        int totem = find(Items.TOTEM_OF_UNDYING), shield = find(Items.SHIELD);

        if (totem >= 0 && !off.is(Items.TOTEM_OF_UNDYING)
                && (m.getHealth() <= 10f || (shield < 0 && !off.is(Items.SHIELD)))) {
            swap(EquipmentSlot.OFFHAND, totem);
        } else if (off.is(Items.TOTEM_OF_UNDYING) && m.getHealth() >= 16f && shield >= 0) {
            swap(EquipmentSlot.OFFHAND, shield);
        } else if (shield >= 0 && !off.is(Items.TOTEM_OF_UNDYING) && !off.is(Items.SHIELD)) {
            swap(EquipmentSlot.OFFHAND, shield);
        }

        ItemStack main = m.getMainHandItem();
        int bow = find(Items.BOW);
        boolean hasArrow = findArrow() >= 0;
        boolean wantBow = hasArrow && ((main.is(Items.BOW) && dist > 6) || (bow >= 0 && dist > 12));
        if (wantBow) {
            if (!main.is(Items.BOW) && bow >= 0) swap(EquipmentSlot.MAINHAND, bow);
        } else {
            int best = -1; double bestDmg = main.is(Items.BOW) ? 0 : attackDamage(main);
            for (int i = 0; i < inv.getContainerSize(); i++) {
                double d = attackDamage(inv.getItem(i));
                if (d > bestDmg) { bestDmg = d; best = i; }
            }
            if (best >= 0) swap(EquipmentSlot.MAINHAND, best);
        }
    }

    private double attackDamage(ItemStack s) {
        if (s.isEmpty()) return 0;
        ItemAttributeModifiers c = s.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        double d = 0;
        for (ItemAttributeModifiers.Entry e : c.modifiers()) {
            if (e.attribute().value() == Attributes.ATTACK_DAMAGE.value()) d += e.modifier().amount();
        }
        return d;
    }

    private void swap(EquipmentSlot slot, int invIdx) {
        SimpleContainer inv = m.getGelInventory();
        ItemStack fromInv = inv.getItem(invIdx);
        ItemStack current = m.getItemBySlot(slot);
        m.setItemSlot(slot, fromInv);
        inv.setItem(invIdx, current);
        m.stopUsingItem();
    }

    private int find(Item item) {
        SimpleContainer inv = m.getGelInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(item)) return i;
        return -1;
    }

    private int findArrow() {
        SimpleContainer inv = m.getGelInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.is(ItemTags.ARROWS)) return i;
        }
        return -1;
    }
}
