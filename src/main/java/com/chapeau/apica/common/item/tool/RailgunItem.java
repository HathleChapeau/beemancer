/**
 * ============================================================
 * [RailgunItem.java]
 * Description: Arme chargeable one-shot avec beam instantane
 * ============================================================
 */
package com.chapeau.apica.common.item.tool;

import com.chapeau.apica.common.item.magazine.*;
import com.chapeau.apica.core.util.ParticleHelper;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.Set;

public class RailgunItem extends Item implements IMagazineHolder {

    public static final int HONEY_TINT = 0xFADE29;
    public static final int ROYAL_JELLY_TINT = 0xFFFAD0;
    public static final int NECTAR_TINT = 0xE855FF;
    public static final int DEFAULT_TINT = 0xFFFFFF;

    public static final int CHARGE_THRESHOLD = 43;
    public static final int MAX_RANGE = 48;
    private static final int FIRE_COOLDOWN = 40;
    private static final int SHOT_COST = 100;

    private static final float DAMAGE_HONEY = 15f;
    private static final float DAMAGE_ROYAL_JELLY = 20f;
    private static final float DAMAGE_NECTAR = 26f;

    public RailgunItem(Properties properties) {
        super(properties);
    }

    @Override
    public Set<String> getAcceptedFluids() {
        return MagazineConstants.STANDARD_FLUIDS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return MagazineData.hasMagazine(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int amount = MagazineData.getFluidAmount(stack);
        return Math.round((float) amount / MagazineFluidData.MAX_CAPACITY * 13f);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        String fluidId = MagazineData.getFluidId(stack);
        if (fluidId.contains("honey")) return HONEY_TINT;
        if (fluidId.contains("royal_jelly")) return ROYAL_JELLY_TINT;
        if (fluidId.contains("nectar")) return NECTAR_TINT;
        return DEFAULT_TINT;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);

        if (level.isClientSide()) {
            if (MagazineInputHelper.isMouseDown()) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.chapeau.apica.core.network.packets.MagazineReloadPacket(hand == InteractionHand.MAIN_HAND)
                );
                return InteractionResultHolder.consume(stack);
            }
        }

        if (!canUse(player, stack)) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        if (!level.isClientSide()) return;
        if (!(entity instanceof Player player)) return;

        int useTicks = getUseDuration(stack, entity) - remainingTicks;
        float speedMult = getChargeSpeedMultiplier(stack);
        float effective = useTicks * speedMult;

        if (useTicks % 4 == 0 && effective < CHARGE_THRESHOLD) {
            float pitch = 0.5f + Math.min(1f, effective / CHARGE_THRESHOLD);
            level.playLocalSound(player.blockPosition(), SoundEvents.BREEZE_CHARGE,
                SoundSource.PLAYERS, 0.2f, pitch, false);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide()) return;
        if (!(entity instanceof Player player)) return;

        int useDuration = getUseDuration(stack, entity) - timeLeft;
        if (useDuration <= 0) return;

        int cost = MagazineData.computeEffectiveCost(stack, SHOT_COST);
        if (!MagazineData.consumeFluid(stack, cost)) return;

        float chargeProgress = Math.min(1f, useDuration * getChargeSpeedMultiplier(stack) / CHARGE_THRESHOLD);

        player.getCooldowns().addCooldown(this, FIRE_COOLDOWN);

        ServerLevel serverLevel = (ServerLevel) level;
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 endPos = eyePos.add(look.scale(MAX_RANGE));

        BlockHitResult blockHit = level.clip(new ClipContext(
            eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 beamEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : endPos;

        Entity closestHit = null;
        Vec3 impactPos = beamEnd;
        double closestDistSq = beamEnd.distanceToSqr(eyePos);
        AABB searchBox = new AABB(eyePos, beamEnd).inflate(1.0);
        for (Entity e : level.getEntities(player, searchBox,
                e -> !e.isSpectator() && e.isAlive() && e.isPickable())) {
            Optional<Vec3> hitOpt = e.getBoundingBox().inflate(0.3).clip(eyePos, beamEnd);
            if (hitOpt.isPresent()) {
                double distSq = hitOpt.get().distanceToSqr(eyePos);
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closestHit = e;
                    impactPos = hitOpt.get();
                }
            }
        }

        if (closestHit != null) {
            float maxDmg = getDamageForFluid(stack);
            float dmg = chargeProgress >= 1f ? maxDmg : maxDmg * 0.5f * chargeProgress;
            closestHit.hurt(player.damageSources().playerAttack(player), dmg);
        }

        ParticleHelper.burst(serverLevel, impactPos, ParticleHelper.EffectType.ELECTRIC, 12);
        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
            SoundSource.PLAYERS, 1.5f, 0.5f);
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(),
            SoundSource.PLAYERS, 1.0f, 2.0f);
    }

    public static float getChargeSpeedMultiplier(ItemStack stack) {
        String fluidId = MagazineData.getFluidId(stack);
        if (fluidId.contains("nectar")) return 1.5f;
        if (fluidId.contains("royal_jelly")) return 1.25f;
        return 1f;
    }

    private static float getDamageForFluid(ItemStack stack) {
        String fluidId = MagazineData.getFluidId(stack);
        if (fluidId.contains("nectar")) return DAMAGE_NECTAR;
        if (fluidId.contains("royal_jelly")) return DAMAGE_ROYAL_JELLY;
        return DAMAGE_HONEY;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        ResourceKey<Enchantment> key = enchantment.unwrapKey().orElse(null);
        if (key == null) return false;
        return key.equals(Enchantments.LOOTING);
    }

    @Override
    public int getEnchantmentValue() { return 1; }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }
}
