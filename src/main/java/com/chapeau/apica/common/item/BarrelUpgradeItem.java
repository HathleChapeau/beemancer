/**
 * ============================================================
 * [BarrelUpgradeItem.java]
 * Description: Upgrade pour transformer un barrel au tier superieur
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | StorageBarrelBlock            | Detection du barrel  | Verification tier              |
 * | StorageBarrelBlockEntity      | Donnees du barrel    | Preservation contenu           |
 * | ApicaBlocks                   | Blocs barrel cibles  | Remplacement de bloc           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaItems.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item;

import com.chapeau.apica.common.block.storage.StorageBarrelBlock;
import com.chapeau.apica.common.blockentity.storage.StorageBarrelBlockEntity;
import com.chapeau.apica.core.registry.ApicaBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BarrelUpgradeItem extends Item {

    private final int targetTier;

    public BarrelUpgradeItem(Properties properties, int targetTier) {
        super(properties);
        this.targetTier = targetTier;
    }

    public int getTargetTier() {
        return targetTier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof StorageBarrelBlock barrelBlock)) {
            return InteractionResult.PASS;
        }

        // Only upgrade from the tier directly below
        int currentTier = barrelBlock.getTier();
        if (currentTier != targetTier - 1) {
            return InteractionResult.PASS;
        }

        // Do not allow upgrade from front face (reserved for insert/extract)
        Direction facing = state.getValue(StorageBarrelBlock.FACING);
        if (context.getClickedFace() == facing) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        StorageBarrelBlockEntity oldBe = (StorageBarrelBlockEntity) level.getBlockEntity(pos);
        if (oldBe == null) return InteractionResult.FAIL;

        // Clear old barrel data (prevents drop in onRemove)
        StorageBarrelBlockEntity.BarrelData data = oldBe.clearForUpgrade();

        // Get the target block
        Block targetBlock = switch (targetTier) {
            case 2 -> ApicaBlocks.STORAGE_BARREL_MK2.get();
            case 3 -> ApicaBlocks.STORAGE_BARREL_MK3.get();
            case 4 -> ApicaBlocks.STORAGE_BARREL_MK4.get();
            default -> null;
        };
        if (targetBlock == null) return InteractionResult.FAIL;

        // Replace block, preserving facing
        BlockState newState = targetBlock.defaultBlockState().setValue(StorageBarrelBlock.FACING, facing);
        level.setBlock(pos, newState, 3);

        // Restore data to new BlockEntity
        StorageBarrelBlockEntity newBe = (StorageBarrelBlockEntity) level.getBlockEntity(pos);
        if (newBe != null) {
            newBe.restoreFromUpgrade(data);
        }

        // Consume item
        if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
            context.getItemInHand().shrink(1);
        }

        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.1f);
        return InteractionResult.SUCCESS;
    }
}
