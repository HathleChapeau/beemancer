/**
 * ============================================================
 * [LeafBlowerItem.java]
 * Description: Item chargeable (style arc) qui tire un orbe detruisant la vegetation
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | LeafBlowerProjectileEntity    | Projectile           | Spawn au relachement           |
 * | IMagazineHolder               | Interface magazine   | Requiert honey pour fonctionner|
 * | MagazineData                  | Data magazine        | Lecture/consommation fluide    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaItems.java (registration)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.tool;

import com.chapeau.apica.common.entity.projectile.LeafBlowerProjectileEntity;
import com.chapeau.apica.common.item.magazine.IMagazineHolder;
import com.chapeau.apica.common.item.magazine.MagazineData;
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

import java.util.Set;

/**
 * Leaf Blower — outil chargeable qui tire un orbe detruisant la vegetation.
 * Necessite un magazine de honey pour fonctionner. Sans magazine = pas de charge.
 * Cout croissant: tier1=25mB, tier2=75mB, tier3=150mB.
 * Si fluide insuffisant pour un tier, bloque au tier inferieur.
 */
public class LeafBlowerItem extends Item implements IMagazineHolder {

    /** Tick thresholds: 0-9 = level 0, 10-24 = level 1, 25-39 = level 2, 40+ = level 3 */
    public static final int CHARGE_TIER1 = 10;
    public static final int CHARGE_TIER2 = 25;
    public static final int CHARGE_TIER3 = 40;

    /** Cout en mB par tier de charge. */
    private static final int COST_TIER1 = 25;
    private static final int COST_TIER2 = 75;
    private static final int COST_TIER3 = 150;

    public LeafBlowerItem(Properties properties) {
        super(properties);
    }

    @Override
    public Set<String> getAcceptedFluids() {
        return Set.of("apica:honey");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Sans magazine honey avec au moins le cout du tier 1, pas de charge
        if (!MagazineData.hasMagazine(stack) || MagazineData.getFluidAmount(stack) < COST_TIER1) {
            return InteractionResultHolder.pass(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        if (!level.isClientSide()) return;
        if (!(entity instanceof Player player)) return;

        int useTicks = getUseDuration(stack, entity) - remainingTicks;
        int maxTier = getMaxAffordableTier(stack);
        int currentLevel = getChargeLevelCapped(useTicks, maxTier);
        int prevLevel = getChargeLevelCapped(useTicks - 1, maxTier);

        // Son continu de charge (toutes les 4 ticks)
        if (useTicks % 4 == 0) {
            float pitch = 0.5f + currentLevel * 0.2f;
            level.playLocalSound(player.blockPosition(), SoundEvents.BREEZE_CHARGE,
                    SoundSource.PLAYERS, 0.3f, pitch, false);
        }

        // Son de gain de niveau de charge
        if (currentLevel > prevLevel && currentLevel > 0) {
            level.playLocalSound(player.blockPosition(), SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(),
                    SoundSource.PLAYERS, 1f, 0.7f + currentLevel * 0.15f, false);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide()) return;
        if (!(entity instanceof Player player)) return;

        int useDuration = getUseDuration(stack, entity) - timeLeft;
        int maxTier = getMaxAffordableTier(stack);
        int chargeLevel = getChargeLevelCapped(useDuration, maxTier);
        if (chargeLevel <= 0) return;

        // Consommer le fluide selon le tier
        int cost = getCostForTier(chargeLevel);
        MagazineData.consumeFluid(stack, cost);

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

    /** Determine le tier max payable avec le fluide restant. */
    private int getMaxAffordableTier(ItemStack stack) {
        int amount = MagazineData.getFluidAmount(stack);
        if (amount >= COST_TIER3) return 3;
        if (amount >= COST_TIER2) return 2;
        if (amount >= COST_TIER1) return 1;
        return 0;
    }

    /** Calcule le niveau de charge avec cap au tier max. */
    private int getChargeLevelCapped(int useTicks, int maxTier) {
        int raw = getChargeLevel(useTicks);
        return Math.min(raw, maxTier);
    }

    private int getChargeLevel(int useTicks) {
        if (useTicks >= CHARGE_TIER3) return 3;
        if (useTicks >= CHARGE_TIER2) return 2;
        if (useTicks >= CHARGE_TIER1) return 1;
        return 0;
    }

    /** Retourne le cout en mB pour un tier donne. */
    private static int getCostForTier(int tier) {
        return switch (tier) {
            case 1 -> COST_TIER1;
            case 2 -> COST_TIER2;
            case 3 -> COST_TIER3;
            default -> 0;
        };
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
