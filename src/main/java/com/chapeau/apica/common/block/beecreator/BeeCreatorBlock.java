/**
 * ============================================================
 * [BeeCreatorBlock.java]
 * Description: Bloc Bee Creator — ouvre un menu de personnalisation d'abeille
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation                    |
 * |--------------------------|----------------------|--------------------------------|
 * | BeeCreatorBlockEntity    | BlockEntity associe  | Stockage couleurs              |
 * | BaseEntityBlock          | Support BlockEntity  | newBlockEntity()               |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaBlocks (enregistrement)
 * - ApicaItems (BlockItem)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.beecreator;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class BeeCreatorBlock extends BaseEntityBlock {

    public static final MapCodec<BeeCreatorBlock> CODEC = simpleCodec(BeeCreatorBlock::new);

    public BeeCreatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BeeCreatorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BeeCreatorBlockEntity creator) {
                serverPlayer.openMenu(creator, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
