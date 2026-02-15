/**
 * ============================================================
 * [ScoopItem.java]
 * Description: Outil de capture d'abeilles par clic droit
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                  | Utilisation                    |
 * |--------------------------|------------------------|--------------------------------|
 * | MagicBeeEntity           | Entite cible           | Detection + discard            |
 * | MagicBeeItem             | Capture factory        | captureFromEntity              |
 * | BeeNestBlockEntity       | Nid d'origine          | Reduction maxBees sur capture  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeemancerItems.java (registration)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.item.tool;

import com.chapeau.beemancer.common.block.hive.BeeNestBlockEntity;
import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.common.item.bee.MagicBeeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Scoop: clic droit sur une MagicBee pour la capturer dans l'inventaire.
 * Durabilite 64, perd 1 par capture. Pas de degats speciaux (base 1).
 */
public class ScoopItem extends Item {

    public ScoopItem(Properties properties) {
        super(properties.durability(64));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof MagicBeeEntity bee)) return InteractionResult.PASS;
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;

        ItemStack captured = MagicBeeItem.captureFromEntity(bee);
        if (!player.getInventory().add(captured)) {
            player.drop(captured, false);
        }

        // Notifier le nid d'origine: reduit maxBees de 1 (perte permanente)
        BlockPos nestPos = bee.getHomeNestPos();
        if (nestPos != null && player.level().getBlockEntity(nestPos) instanceof BeeNestBlockEntity nest) {
            nest.onBeeScooped(bee);
        }

        bee.discard();
        stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        player.level().playSound(null, target.blockPosition(), SoundEvents.BEEHIVE_WORK, SoundSource.NEUTRAL, 1.0f, 1.0f);

        return InteractionResult.SUCCESS;
    }
}
