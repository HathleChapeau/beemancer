/**
 * ============================================================
 * [MiningLaserItem.java]
 * Description: Arme chargeable qui tire un rayon laser detruisant des blocs
 * ============================================================
 */
package com.chapeau.apica.common.item.tool;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.item.magazine.*;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MiningLaserItem extends Item implements IMagazineHolder {

    public static final int CHARGE_TICKS = 40;
    private static final int FIRE_INTERVAL = 5;
    public static final int MAX_RANGE = 24;
    public static final int MAX_CHARGE_LEVEL = 3;
    private static final int DOUBLE_CLICK_WINDOW = 10;

    private static final int COST_LEVEL1 = 2;
    private static final int COST_LEVEL2 = 16;
    private static final int COST_LEVEL3 = 30;

    private static final String TAG_CHARGE_LEVEL = "ChargeLevel";
    private static final String TAG_LAST_CLICK_TICK = "LastClickTick";

    /** Client-side only: saved charge levels before reload, keyed by player UUID */
    private static final Map<UUID, Integer> CLIENT_SAVED_CHARGE = new HashMap<>();
    /** Client-side only: tracks if we're waiting for reload to complete */
    private static final Map<UUID, Boolean> CLIENT_AWAITING_RELOAD = new HashMap<>();

    public MiningLaserItem(Properties properties) {
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

        // Client: mouseDown -> envoie packet au serveur
        if (level.isClientSide()) {
            if (MagazineInputHelper.isMouseDown()) {
                // Only save chargeLevel if we're actually going to reload
                if (needsReload(stack)) {
                    UUID playerId = player.getUUID();
                    int currentLevel = getChargeLevel(stack);
                    if (currentLevel > 0) {
                        CLIENT_SAVED_CHARGE.put(playerId, currentLevel);
                    }
                    CLIENT_AWAITING_RELOAD.put(playerId, true);
                }

                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.chapeau.apica.core.network.packets.MagazineReloadPacket(hand == InteractionHand.MAIN_HAND)
                );
                return InteractionResultHolder.consume(stack);
            }
        }

        if (!canUse(player, stack)) {
            return InteractionResultHolder.fail(stack);
        }

        long gameTime = level.getGameTime();
        long lastClick = getLastClickTick(stack);

        // Double-click: cycle le niveau (1-2-3-1)
        if (gameTime - lastClick <= DOUBLE_CLICK_WINDOW) {
            setLastClickTick(stack, 0);
            int current = getChargeLevel(stack);
            int next = current >= MAX_CHARGE_LEVEL ? 1 : current + 1;
            setChargeLevel(stack, next);

            if (!level.isClientSide()) {
                float pitch = 0.6f + next * 0.15f;
                level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(),
                        SoundSource.PLAYERS, 0.8f, pitch);
            }

            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        if (getChargeLevel(stack) < 1) {
            setChargeLevel(stack, 1);
        }

        setLastClickTick(stack, gameTime);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        if (!(entity instanceof Player player)) return;

        int useTicks = getUseDuration(stack, entity) - remainingTicks;
        int chargeLevel = getChargeLevel(stack);

        if (level.isClientSide()) {
            onClientUseTick(level, player, useTicks, chargeLevel);
        } else {
            onServerUseTick((ServerLevel) level, player, stack, useTicks, chargeLevel);
        }
    }

    private void onClientUseTick(Level level, Player player, int useTicks, int chargeLevel) {
        if (useTicks % 4 == 0 && useTicks < CHARGE_TICKS) {
            float pitch = 0.5f + chargeLevel * 0.15f;
            level.playLocalSound(player.blockPosition(), SoundEvents.BREEZE_CHARGE,
                    SoundSource.PLAYERS, 0.3f, pitch, false);
        }

        if (useTicks == CHARGE_TICKS) {
            level.playLocalSound(player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(),
                    SoundSource.PLAYERS, 1.0f, 1.5f, false);
        }
    }

    private void onServerUseTick(ServerLevel level, Player player, ItemStack stack,
                                  int useTicks, int chargeLevel) {
        if (useTicks < CHARGE_TICKS) return;

        if ((useTicks - CHARGE_TICKS) % FIRE_INTERVAL == 0) {
            int cost = MagazineData.computeEffectiveCost(stack, getBaseCostForAoE(chargeLevel));
            if (!MagazineData.consumeFluid(stack, cost)) {
                setChargeLevel(stack, 0);
                player.stopUsingItem();
                return;
            }

            boolean brokeBlock = MiningLaserBlockBreaker.tryBreakBlock(level, player, MAX_RANGE, chargeLevel);

            if (brokeBlock) {
                float pitch = 0.8f + level.random.nextFloat() * 0.4f;
                level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                        SoundSource.PLAYERS, 0.6f, pitch);
            }
        }
    }

    private static int getBaseCostForAoE(int chargeLevel) {
        return switch (chargeLevel) {
            case 2 -> COST_LEVEL2;
            case 3 -> COST_LEVEL3;
            default -> COST_LEVEL1;
        };
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
    }

    // =========================================================================
    // CustomData helpers
    // =========================================================================

    public static int getChargeLevel(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0;
        return customData.copyTag().getInt(TAG_CHARGE_LEVEL);
    }

    public static void setChargeLevel(ItemStack stack, int level) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putInt(TAG_CHARGE_LEVEL, level);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static long getLastClickTick(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0;
        return customData.copyTag().getLong(TAG_LAST_CLICK_TICK);
    }

    private static void setLastClickTick(ItemStack stack, long tick) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putLong(TAG_LAST_CLICK_TICK, tick);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // =========================================================================
    // Client-side chargeLevel restoration after reload
    // =========================================================================

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!level.isClientSide()) return;
        if (!(entity instanceof Player player)) return;

        UUID playerId = player.getUUID();
        if (!CLIENT_AWAITING_RELOAD.getOrDefault(playerId, false)) return;

        // Check if reload completed (magazine now has fluid)
        if (MagazineData.hasMagazine(stack) && MagazineData.getFluidAmount(stack) > 0) {
            Integer saved = CLIENT_SAVED_CHARGE.remove(playerId);
            if (saved != null && saved > 0) {
                setChargeLevel(stack, saved);
            }
            CLIENT_AWAITING_RELOAD.remove(playerId);
        }
    }

    // =========================================================================
    // Item properties
    // =========================================================================

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        ResourceKey<Enchantment> key = enchantment.unwrapKey().orElse(null);
        if (key == null) return false;
        return key.equals(Enchantments.FORTUNE) || key.equals(Enchantments.SILK_TOUCH);
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }
}
