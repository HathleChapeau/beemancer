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

    /** Tick thresholds: 0-19 = level 0 (no fire), 20-39 = level 1, 40-59 = level 2, 60+ = level 3 */
    private static final int CHARGE_TIER1 = 20;
    private static final int CHARGE_TIER2 = 40;
    private static final int CHARGE_TIER3 = 60;

    public LeafBlowerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
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
