/**
 * ============================================================
 * [LeafBlowerItem.java]
 * Description: Item chargeable qui tire un orbe detruisant la vegetation
 * ============================================================
 */
package com.chapeau.apica.common.item.tool;

import com.chapeau.apica.common.entity.projectile.LeafBlowerProjectileEntity;
import com.chapeau.apica.common.item.magazine.*;
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

public class LeafBlowerItem extends Item implements IMagazineHolder {

    public static final int CHARGE_TIER1 = 10;
    public static final int CHARGE_TIER2 = 25;
    public static final int CHARGE_TIER3 = 40;

    private static final int COST_TIER1 = 25;
    private static final int COST_TIER2 = 75;
    private static final int COST_TIER3 = 150;

    public LeafBlowerItem(Properties properties) {
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
        return MagazineConstants.getBarColorForFluid(MagazineData.getFluidId(stack));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            if (MagazineInputHelper.isMouseDown()) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.chapeau.apica.core.network.packets.MagazineReloadPacket(hand == InteractionHand.MAIN_HAND)
                );
                return InteractionResultHolder.consume(stack);
            }
        }

        int minCost = MagazineData.computeEffectiveCost(stack, COST_TIER1);
        if (!MagazineData.hasMagazine(stack) || MagazineData.getFluidAmount(stack) < minCost) {
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
        int maxTier = getMaxAffordableTier(stack);
        int currentLevel = getChargeLevelCapped(useTicks, speedMult, maxTier);
        int prevLevel = getChargeLevelCapped(useTicks - 1, speedMult, maxTier);

        if (useTicks % 4 == 0) {
            float pitch = 0.5f + currentLevel * 0.2f;
            level.playLocalSound(player.blockPosition(), SoundEvents.BREEZE_CHARGE,
                    SoundSource.PLAYERS, 0.3f, pitch, false);
        }

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
        float speedMult = getChargeSpeedMultiplier(stack);
        int maxTier = getMaxAffordableTier(stack);
        int chargeLevel = getChargeLevelCapped(useDuration, speedMult, maxTier);
        if (chargeLevel <= 0) return;

        int cost = MagazineData.computeEffectiveCost(stack, getCostForTier(chargeLevel));
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

    public static float getChargeSpeedMultiplier(ItemStack stack) {
        String fluidId = MagazineData.getFluidId(stack);
        if (fluidId.contains("nectar")) return 2f;
        if (fluidId.contains("royal_jelly")) return 1.4f;
        return 1f;
    }

    private int getMaxAffordableTier(ItemStack stack) {
        int amount = MagazineData.getFluidAmount(stack);
        if (amount >= MagazineData.computeEffectiveCost(stack, COST_TIER3)) return 3;
        if (amount >= MagazineData.computeEffectiveCost(stack, COST_TIER2)) return 2;
        if (amount >= MagazineData.computeEffectiveCost(stack, COST_TIER1)) return 1;
        return 0;
    }

    private int getChargeLevelCapped(int useTicks, float speedMult, int maxTier) {
        int raw = getChargeLevel(useTicks, speedMult);
        return Math.min(raw, maxTier);
    }

    private int getChargeLevel(int useTicks, float speedMult) {
        float effective = useTicks * speedMult;
        if (effective >= CHARGE_TIER3) return 3;
        if (effective >= CHARGE_TIER2) return 2;
        if (effective >= CHARGE_TIER1) return 1;
        return 0;
    }

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

    @Override
    public void startReloadAnimation(Player player, ItemStack holder, float currentTime) {
        setReloading(player, true);
        if (player.level().isClientSide()) {
            com.chapeau.apica.client.renderer.item.LeafBlowerItemRenderer renderer =
                    com.chapeau.apica.client.renderer.item.LeafBlowerItemRenderer.getInstance();
            if (renderer != null) {
                renderer.getReloadAnimator().startReloadAnimation(currentTime, () -> {
                    boolean mainHand = player.getMainHandItem() == holder;
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                            new com.chapeau.apica.core.network.packets.MagazineReloadCompletePacket(mainHand));
                    setReloading(player, false);
                    float time = com.chapeau.apica.client.animation.AnimationTimer.getRenderTime(
                            net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
                    renderer.getReloadAnimator().triggerSweep(time);
                });
            }
        }
    }
}
