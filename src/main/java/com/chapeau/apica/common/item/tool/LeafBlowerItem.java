/**
 * ============================================================
 * [LeafBlowerItem.java]
 * Description: Item chargeable (style arc) qui tire un orbe détruisant la végétation
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | LeafBlowerProjectileEntity    | Projectile           | Spawn au relâchement           |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaItems.java (registration)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.tool;

import com.chapeau.apica.common.entity.projectile.LeafBlowerProjectileEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class LeafBlowerItem extends Item {

    /** Tick thresholds: 0-14 = level 0 (no fire), 15-29 = level 1, 30-44 = level 2, 45+ = level 3 */
    public static final int CHARGE_TIER1 = 10;
    public static final int CHARGE_TIER2 = 25;
    public static final int CHARGE_TIER3 = 40;

    public LeafBlowerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        if (!(entity instanceof Player player)) return;

        int useTicks = getUseDuration(stack, entity) - remainingTicks;
        int currentLevel = getChargeLevel(useTicks);
        int prevLevel = getChargeLevel(useTicks - 1);

        // Son continu de charge (toutes les 4 ticks)
        if (useTicks % 4 == 0) {
            float pitch = 0.5f + currentLevel * 0.2f;
            level.playSound(player, player.blockPosition(), SoundEvents.BREEZE_CHARGE.value(),
                    SoundSource.PLAYERS, 0.3f, pitch);
        }

        // Son de gain de niveau de charge
        if (currentLevel > prevLevel && currentLevel > 0) {
            level.playSound(player, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.5f, 0.7f + currentLevel * 0.3f);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide()) return;
        if (!(entity instanceof Player player)) return;

        int useDuration = getUseDuration(stack, entity) - timeLeft;
        int chargeLevel = getChargeLevel(useDuration);
        if (chargeLevel <= 0) return;

        Vec3 look = player.getLookAngle();
        Vec3 spawnPos = player.getEyePosition().add(look);

        LeafBlowerProjectileEntity orb = new LeafBlowerProjectileEntity(
                level, spawnPos.x, spawnPos.y, spawnPos.z, chargeLevel);
        orb.setOwner(player);
        orb.setDeltaMovement(look.scale(1.5));
        level.addFreshEntity(orb);

        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 1.0f, 0.8f + chargeLevel * 0.2f);
    }

    private int getChargeLevel(int useTicks) {
        if (useTicks >= CHARGE_TIER3) return 3;
        if (useTicks >= CHARGE_TIER2) return 2;
        if (useTicks >= CHARGE_TIER1) return 1;
        return 0;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }
}
