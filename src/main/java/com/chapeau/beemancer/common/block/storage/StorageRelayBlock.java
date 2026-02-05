/**
 * ============================================================
 * [StorageRelayBlock.java]
 * Description: Bloc relais du reseau de stockage
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                  | Utilisation           |
 * |-------------------------------|------------------------|-----------------------|
 * | StorageRelayBlockEntity      | BlockEntity associe    | Logique relais        |
 * | BeemancerBlockEntities       | Type du BlockEntity    | Creation et ticker    |
 * | StorageEditModeHandler       | Mode edition           | Toggle mode           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeemancerBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import com.chapeau.beemancer.client.particle.ParticleEmitter;
import com.chapeau.beemancer.common.blockentity.storage.INetworkNode;
import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageRelayBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerParticles;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Bloc relais du reseau de stockage.
 *
 * Interactions:
 * - Shift+clic droit: Toggle mode edition (coffres et connexions)
 */
public class StorageRelayBlock extends BaseEntityBlock {
    public static final MapCodec<StorageRelayBlock> CODEC = simpleCodec(StorageRelayBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(0, 0, 0, 16, 5, 16),
        Block.box(4, 2, 4, 12, 16, 12)
    );

    public StorageRelayBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StorageRelayBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, BeemancerBlockEntities.STORAGE_RELAY.get(),
                (lvl, pos, st, be) -> StorageRelayBlockEntity.serverTick(be));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof StorageRelayBlockEntity relay)) {
            return InteractionResult.PASS;
        }

        // Shift+clic droit: toggle mode edition
        if (player.isShiftKeyDown()) {
            boolean nowEditing = relay.toggleEditMode(player.getUUID());
            if (nowEditing) {
                validateFromRelay(relay, level);
                StorageEditModeHandler.startEditing(player.getUUID(), pos);
                player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_relay.edit_mode_on"),
                    true
                );
            } else {
                StorageEditModeHandler.stopEditing(player.getUUID());
                player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_relay.edit_mode_off"),
                    true
                );
            }
            return InteractionResult.SUCCESS;
        }

        // Clic normal: afficher info
        int chests = relay.getRegisteredChests().size();
        int nodes = relay.getConnectedNodes().size();
        player.displayClientMessage(
            Component.translatable("message.beemancer.storage_relay.info", chests, nodes),
            true
        );

        return InteractionResult.SUCCESS;
    }

    /**
     * Trouve le controller depuis un relay via BFS et valide les blocs du registre.
     */
    private static void validateFromRelay(StorageRelayBlockEntity relay, Level level) {
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        java.util.Queue<BlockPos> queue = new java.util.LinkedList<>();
        visited.add(relay.getNodePos());
        for (BlockPos connected : relay.getConnectedNodes()) {
            queue.add(connected);
        }
        while (!queue.isEmpty()) {
            BlockPos nodePos = queue.poll();
            if (!visited.add(nodePos)) continue;
            if (!level.isLoaded(nodePos)) continue;
            BlockEntity nodeBe = level.getBlockEntity(nodePos);
            if (nodeBe instanceof StorageControllerBlockEntity controller) {
                controller.validateNetworkBlocks();
                return;
            }
            if (nodeBe instanceof INetworkNode node) {
                for (BlockPos neighbor : node.getConnectedNodes()) {
                    if (!visited.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        new ParticleEmitter(BeemancerParticles.RUNE.get())
            .at(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5)
            .speed(0, 0.01, 0)
            .spread(0.25, 0.15, 0.25)
            .speedVariance(0.01, 0.01, 0.01)
            .count(2)
            .lifetime(10)
            .gravity(-0.001f)
            .scale(0.05f)
            .fadeOut()
            .spawn(level);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StorageRelayBlockEntity relay) {
                relay.exitEditMode();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
