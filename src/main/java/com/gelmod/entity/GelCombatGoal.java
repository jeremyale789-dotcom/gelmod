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
 * Cerebro de combate del Jugador Gelatinoso:
 *  - Aimbot: mira al objetivo cada tick (sin suavizado) y predice su movimiento con el arco.
 *  - Kill aura: golpea hasta GelMod.REACH bloques (con línea de visión).
 *  - Escudo: bloquea entre golpes. Tótem: lo equipa en la mano secundaria con vida baja.
 *  - Arco: lo usa a distancia si tiene arco + flechas. Come manzanas doradas/comida con poca vida.
 */
public class GelCombatGoal extends Goal {
    private final GelPlayerEntity m;
    private int attackCd, bowTime, eatCd, gearCd, strafeTime;
    private float strafeDir = 1f;
    private Vec3 lastPos = Vec3.ZERO;

    public GelCombatGoal(GelPlayerEntity mob) {
        this.m = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override public boolean canUse() { LivingEntity t = m.getTarget(); return t != null && t.isAlive(); }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { lastPos = Vec3.ZERO; bowTime = 0; }
    @Override public void stop() { m.stopUsingItem(); m.getNavigation().stop(); }

    @Override
    public void tick() {
        LivingEntity t = m.getTarget();
        if (t == null || !t.isAlive()) return;

        Vec3 tv = lastPos == Vec3.ZERO ? Vec3.ZERO : t.position().subtract(lastPos);
        lastPos = t.position();
        double dist = m.distanceTo(t);

        if (attackCd > 0) attackCd--;
        if (eatCd > 0) eatCd--;
        if (--gearCd <= 0) { gearCd = 5; manageGear(dist); }
        if (eatCd == 0) tryEat();

        boolean bow = m.getMainHandItem().is(Items.BOW);
        boolean sees = m.hasLineOfSight(t);

        Vec3 aim = t.getEyePosition();
        if (bow && m.isUsingItem()) aim = lead(t, tv, dist);
        aimAt(aim);

        if (bow) ranged(t, tv, dist, sees); else melee(t, dist, sees);

        if (m.horizontalCollision && m.onGround()) m.getJumpControl().jump();
    }

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
        double ticks = dist / 3.0; // velocidad de flecha a carga completa
        return t.position().add(0, t.getBbHeight() * 0.6, 0).add(tv.x * ticks, 0, tv.z * ticks);
    }

    // ---------------------------------------------------------------- melee
    private void melee(LivingEntity t, double dist, boolean sees) {
        if (dist > 3.0) {
            m.getNavigation().moveTo(t, 1.2);
        } else {
            m.getNavigation().stop();
            strafe();
        }
        boolean shield = m.getOffhandItem().is(Items.SHIELD);
        if (dist <= GelMod.REACH && sees && attackCd == 0) {            // kill aura
            if (m.isUsingItem()) m.stopUsingItem();
            m.swing(InteractionHand.MAIN_HAND);
            m.doHurtTarget(t);
            attackCd = cooldownFor(m.getMainHandItem());
        } else if (shield && dist <= 8 && attackCd > 3 && !m.isUsingItem()) {
            m.startUsingItem(InteractionHand.OFF_HAND);                  // bloquea entre golpes
        } else if (m.isUsingItem() && attackCd <= 3 && m.getUseItem().is(Items.SHIELD)) {
            m.stopUsingItem();
        }
    }

    private void strafe() {
        if (--strafeTime <= 0) { strafeTime = 15 + m.getRandom().nextInt(25); strafeDir = m.getRandom().nextBoolean() ? 1f : -1f; }
        m.getMoveControl().strafe(0f, strafeDir * 0.9f);
    }

    /** Cooldown de golpe: 0.5 segundos = 10 ticks, sin importar el arma. */
    private int cooldownFor(ItemStack s) { return 10; }

    // ---------------------------------------------------------------- arco
    private void ranged(LivingEntity t, Vec3 tv, double dist, boolean sees) {
        if (dist < 7) {
            Vec3 away = m.position().subtract(t.position()).normalize().scale(8).add(m.position());
            m.getNavigation().moveTo(away.x, away.y, away.z, 1.3);
        } else if (dist > 22) {
            m.getNavigation().moveTo(t, 1.1);
        } else {
            m.getNavigation().stop();
            strafe();
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
        dy += 0.03 * flight * flight; // compensación de caída
        arrow.shoot(dx, dy, dz, 3.0f, 0.0f); // 0 de dispersión = aimbot
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

        // Mano secundaria: tótem con vida baja (o si no hay escudo), si no escudo.
        if (totem >= 0 && !off.is(Items.TOTEM_OF_UNDYING)
                && (m.getHealth() <= 10f || (shield < 0 && !off.is(Items.SHIELD)))) {
            swap(EquipmentSlot.OFFHAND, totem);
        } else if (off.is(Items.TOTEM_OF_UNDYING) && m.getHealth() >= 16f && shield >= 0) {
            swap(EquipmentSlot.OFFHAND, shield);
        } else if (shield >= 0 && !off.is(Items.TOTEM_OF_UNDYING) && !off.is(Items.SHIELD)) {
            swap(EquipmentSlot.OFFHAND, shield);
        }

        // Mano principal: arco si está lejos y tiene flechas; si no, la mejor arma cuerpo a cuerpo.
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

    private void tryEat() {
        if (m.getHealth() > 12f) return;
        SimpleContainer inv = m.getGelInventory();
        int idx = -1; boolean golden = false;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(Items.ENCHANTED_GOLDEN_APPLE) || s.is(Items.GOLDEN_APPLE)) { idx = i; golden = true; break; }
            if (idx < 0 && s.has(DataComponents.FOOD)) idx = i;
        }
        if (idx < 0) return;
        ItemStack s = inv.getItem(idx);
        FoodProperties f = s.get(DataComponents.FOOD);
        s.finishUsingItem(m.level(), m);
        if (!golden && f != null) m.heal(f.nutrition());
        eatCd = 50;
    }
}
