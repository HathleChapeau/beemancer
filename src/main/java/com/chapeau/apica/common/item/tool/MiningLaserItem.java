/**
 * ============================================================
 * [MiningLaserItem.java]
 * Description: Arme chargeable qui tire un rayon laser detruisant des blocs
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | MiningLaserBlockBreaker | Destruction blocs    | tryBreakBlock() server-side    |
 * | CustomData              | Stockage metadata    | chargeLevel + lastClickTick    |
 * | IMagazineHolder         | Interface magazine   | Requiert magazine pour charger  |
 * | MagazineData            | Data magazine        | Lecture/consommation fluide    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaItems.java (registration)
 * - MiningLaserItemRenderer.java (lecture chargeLevel pour rendu)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.tool;

import com.chapeau.apica.common.item.magazine.IMagazineHolder;
import com.chapeau.apica.common.item.magazine.MagazineData;
import com.chapeau.apica.common.item.magazine.MagazineFluidData;
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

import java.util.Set;

/**
 * Mining Laser — arme chargeable qui tire un rayon laser detruisant des blocs.
 * Necessite un magazine pour charger. Sans magazine = pas de charge.
 * Accepte honey, royal_jelly, nectar. Cout par tir selon niveau AoE + multiplicateur fluide.
 *
 * Mecanique :
 * 1. Sans magazine : bloque au niveau 0, pas de charge
 * 2. Avec magazine : double right-click cycle le niveau (1-2-3-1)
 * 3. Hold right-click : charge la jauge
 * 4. Jauge pleine : tir continu (1 tir / 5 ticks), consomme du fluide
 * 5. Fluide epuise en cours de tir : arret automatique
 * AoE : niveau 1 = bloc unique, niveau 2 = voisins directs (r1), niveau 3 = sphere r2
 */
public class MiningLaserItem extends Item implements IMagazineHolder {

    private static final int HONEY_COLOR = 0xE8A317;
    private static final int ROYAL_JELLY_COLOR = 0xFFF8DC;
    private static final int NECTAR_COLOR = 0xB050FF;
    private static final int DEFAULT_COLOR = 0x888888;

    /** Nombre de ticks pour atteindre la pleine charge */
    public static final int CHARGE_TICKS = 40;

    /** Intervalle entre chaque tir en mode continu (en ticks) */
    private static final int FIRE_INTERVAL = 5;

    /** Portee maximale du laser en blocs */
    public static final int MAX_RANGE = 24;

    /** Nombre max de niveaux de charge (3 barres) */
    public static final int MAX_CHARGE_LEVEL = 3;

    /** Fenetre de double-click en ticks */
    private static final int DOUBLE_CLICK_WINDOW = 10;

    /** Cout base par tir selon niveau de charge (avant multiplicateur fluide) */
    private static final int COST_LEVEL1 = 2;
    private static final int COST_LEVEL2 = 8;
    private static final int COST_LEVEL3 = 15;

    private static final String TAG_CHARGE_LEVEL = "ChargeLevel";
    private static final String TAG_LAST_CLICK_TICK = "LastClickTick";

    public MiningLaserItem(Properties properties) {
        super(properties);
    }

    @Override
    public Set<String> getAcceptedFluids() {
        return Set.of("apica:honey", "apica:royal_jelly", "apica:nectar");
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
        if (fluidId.contains("honey")) return HONEY_COLOR;
        if (fluidId.contains("royal_jelly")) return ROYAL_JELLY_COLOR;
        if (fluidId.contains("nectar")) return NECTAR_COLOR;
        return DEFAULT_COLOR;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        long gameTime = level.getGameTime();
        long lastClick = getLastClickTick(stack);

        boolean hasMag = MagazineData.hasMagazine(stack) && MagazineData.getFluidAmount(stack) > 0;

        // Double-click: cycle le niveau (1-2-3-1), requiert magazine
        if (gameTime - lastClick <= DOUBLE_CLICK_WINDOW) {
            setLastClickTick(stack, 0);

            if (!hasMag) {
                setChargeLevel(stack, 0);
                return InteractionResultHolder.pass(stack);
            }

            int current = getChargeLevel(stack);
            int next = current >= MAX_CHARGE_LEVEL ? 1 : current + 1;
            if (next < 1) next = 1;
            setChargeLevel(stack, next);

            if (!level.isClientSide()) {
                float pitch = 0.6f + next * 0.15f;
                level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(),
                        SoundSource.PLAYERS, 0.8f, pitch);
            }

            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        // Sans magazine = bloque au niveau 0, pas de charge
        if (!hasMag) {
            setChargeLevel(stack, 0);
            return InteractionResultHolder.pass(stack);
        }

        // Avec magazine, force minimum niveau 1
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

    /**
     * Server-side : tir continu une fois la jauge pleine.
     * Consomme du fluide a chaque tir. Arret auto si fluide epuise.
     */
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

            // Magazine vide apres consommation → passer au niveau 0
            if (MagazineData.getFluidAmount(stack) <= 0) {
                setChargeLevel(stack, 0);
            }

            boolean brokeBlock = MiningLaserBlockBreaker.tryBreakBlock(level, player, MAX_RANGE, chargeLevel);

            if (brokeBlock) {
                float pitch = 0.8f + level.random.nextFloat() * 0.4f;
                level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                        SoundSource.PLAYERS, 0.6f, pitch);
            }
        }
    }

    /** Retourne le cout base par tir selon le niveau de charge. */
    private static int getBaseCostForAoE(int chargeLevel) {
        return switch (chargeLevel) {
            case 2 -> COST_LEVEL2;
            case 3 -> COST_LEVEL3;
            default -> COST_LEVEL1;
        };
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        // Le laser s'arrete quand le joueur relache
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
