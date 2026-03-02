/**
 * ============================================================
 * [MiningLaserItem.java]
 * Description: Arme chargeable qui tire un rayon laser détruisant des blocs
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | MiningLaserBlockBreaker | Destruction blocs    | tryBreakBlock() server-side    |
 * | CustomData              | Stockage metadata    | chargeLevel + lastClickTick    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaItems.java (registration)
 * - MiningLaserItemRenderer.java (lecture chargeLevel pour rendu)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.tool;

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

/**
 * Mining Laser — arme chargeable inspirée du Leaf Blower.
 *
 * Mécanique :
 * 1. Double right-click : cycle le niveau de charge (0→1→2→3→0), affecte le rayon AoE
 * 2. Hold right-click : charge la jauge (animation overlay comme leaf blower)
 * 3. Jauge pleine : le laser tire en continu (1 tir / 5 ticks) tant que le joueur hold
 * 4. L'arme fonctionne à tous les niveaux, y compris 0 (tir sans AoE)
 */
public class MiningLaserItem extends Item {

    /** Nombre de ticks pour atteindre la pleine charge */
    public static final int CHARGE_TICKS = 40;

    /** Intervalle entre chaque tir en mode continu (en ticks) */
    private static final int FIRE_INTERVAL = 5;

    /** Portée maximale du laser en blocs */
    public static final int MAX_RANGE = 32;

    /** Nombre max de niveaux de charge (3 barres) */
    public static final int MAX_CHARGE_LEVEL = 3;

    /** Fenêtre de double-click en ticks */
    private static final int DOUBLE_CLICK_WINDOW = 10;

    private static final String TAG_CHARGE_LEVEL = "ChargeLevel";
    private static final String TAG_LAST_CLICK_TICK = "LastClickTick";

    public MiningLaserItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        long gameTime = level.getGameTime();
        long lastClick = getLastClickTick(stack);

        if (gameTime - lastClick <= DOUBLE_CLICK_WINDOW) {
            int current = getChargeLevel(stack);
            int next = (current + 1) % (MAX_CHARGE_LEVEL + 1);
            setChargeLevel(stack, next);
            setLastClickTick(stack, 0);

            if (!level.isClientSide()) {
                float pitch = 0.6f + next * 0.15f;
                level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(),
                        SoundSource.PLAYERS, 0.8f, pitch);
            }

            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
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

    /**
     * Client-side : sons de charge (pattern identique leaf blower).
     */
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

    /**
     * Server-side : tir continu une fois la jauge pleine.
     * Fonctionne à tous les niveaux de charge (0 inclus).
     * Le chargeLevel détermine le rayon AoE (0=single, 1=r1, 2=r2, 3=r3).
     */
    private void onServerUseTick(ServerLevel level, Player player, ItemStack stack,
                                  int useTicks, int chargeLevel) {
        if (useTicks < CHARGE_TICKS) return;

        if ((useTicks - CHARGE_TICKS) % FIRE_INTERVAL == 0) {
            MiningLaserBlockBreaker.tryBreakBlock(level, player, MAX_RANGE, chargeLevel);

            float pitch = 0.8f + level.random.nextFloat() * 0.4f;
            level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                    SoundSource.PLAYERS, 0.6f, pitch);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        // Le laser s'arrête quand le joueur relâche — pas de logique supplémentaire
    }

    // =========================================================================
    // CustomData helpers
    // =========================================================================

    public static int getChargeLevel(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0;
        CompoundTag tag = customData.copyTag();
        return tag.getInt(TAG_CHARGE_LEVEL);
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
        CompoundTag tag = customData.copyTag();
        return tag.getLong(TAG_LAST_CLICK_TICK);
    }

    private static void setLastClickTick(ItemStack stack, long tick) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putLong(TAG_LAST_CLICK_TICK, tick);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
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
