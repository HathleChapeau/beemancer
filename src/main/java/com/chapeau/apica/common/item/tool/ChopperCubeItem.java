/**
 * ============================================================
 * [ChopperCubeItem.java]
 * Description: Cube qui detecte et surligne les buches connectees au-dessus du curseur
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BlockTags           | Detection buches     | Verification tag logs          |
 * | ChopperCubePreviewRenderer | Rendu glow  | Consommation des positions     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaItems.java (registration)
 * - ChopperCubePreviewRenderer.java (lecture des positions)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Chopper Cube — outil de detection d'arbres.
 *
 * Quand le joueur regarde une buche en tenant cet item,
 * toutes les buches connectees du meme type au-dessus (y >= cible) sont surlignees.
 * Clic droit verrouille le glow tant que l'item est en main.
 */
public class ChopperCubeItem extends Item {

    /** Nombre max de blocs scannes pour eviter les lags */
    public static final int MAX_SCAN = 256;

    public ChopperCubeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            if (ChopperCubeLockHelper.isLocked()) {
                ChopperCubeLockHelper.reset();
            } else {
                // Scanner les buches depuis le curseur et verrouiller
                net.minecraft.world.phys.HitResult hitResult = net.minecraft.client.Minecraft.getInstance().hitResult;
                if (hitResult != null && hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    BlockPos targetPos = ((net.minecraft.world.phys.BlockHitResult) hitResult).getBlockPos();
                    List<BlockPos> positions = findConnectedLogs(level, targetPos);
                    if (!positions.isEmpty()) {
                        ChopperCubeLockHelper.lockWith(positions);
                    }
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Trouve toutes les buches connectees (26-way) du meme type,
     * a partir d'une position de depart, en ne descendant jamais en dessous du startY.
     *
     * @param level    Le Level (client ou server)
     * @param startPos La position de la buche ciblee
     * @return Liste des positions de buches connectees (inclut startPos)
     */
    public static List<BlockPos> findConnectedLogs(Level level, BlockPos startPos) {
        List<BlockPos> result = new ArrayList<>();
        BlockState startState = level.getBlockState(startPos);

        if (!startState.is(BlockTags.LOGS)) {
            return result;
        }

        Block targetBlock = startState.getBlock();
        int startY = startPos.getY();

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty() && result.size() < MAX_SCAN) {
            BlockPos current = queue.poll();
            result.add(current);

            // Scanner les 26 voisins (3x3x3 - centre)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        BlockPos neighbor = current.offset(dx, dy, dz);

                        if (visited.contains(neighbor)) continue;
                        if (neighbor.getY() < startY) continue;

                        BlockState neighborState = level.getBlockState(neighbor);
                        if (neighborState.is(targetBlock)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        return result;
    }
}
