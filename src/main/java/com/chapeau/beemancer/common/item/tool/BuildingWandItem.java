/**
 * ============================================================
 * [BuildingWandItem.java]
 * Description: Baguette de construction avec prévisualisation et placement
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | Level               | Accès monde          | Raycast, placement    |
 * | Player              | Inventaire           | Compte items          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerItems.java (enregistrement)
 * - BuildingWandRenderer.java (prévisualisation)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.item.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Baguette de construction magique.
 *
 * Fonctionnalités:
 * - Prévisualisation des blocs à placer (contour blanc)
 * - Extension 2D sur la face pointée (flood fill avec diagonales)
 * - Limite: min(15, items dans inventaire) - illimité en créatif
 * - Clic droit: place les blocs prévisualisés
 */
public class BuildingWandItem extends Item {

    public static final int MAX_BLOCKS = 15;

    public BuildingWandItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockState sourceState = level.getBlockState(clickedPos);

        if (sourceState.isAir()) return InteractionResult.PASS;

        // Calculer les positions de prévisualisation
        List<BlockPos> previewPositions = calculatePreviewPositions(
            level, player, clickedPos, face, sourceState
        );

        if (previewPositions.isEmpty()) return InteractionResult.PASS;

        // Côté serveur: placer les blocs
        if (!level.isClientSide()) {
            int placed = 0;
            Block sourceBlock = sourceState.getBlock();

            for (BlockPos pos : previewPositions) {
                // Vérifier que l'espace est libre
                if (!level.getBlockState(pos).canBeReplaced()) continue;

                // Récupérer l'état du bloc directement derrière (pour copier son orientation)
                BlockPos behindPos = pos.relative(face.getOpposite());
                BlockState behindState = level.getBlockState(behindPos);

                // Placer le bloc avec la même orientation que le bloc derrière
                level.setBlock(pos, behindState, 3);
                placed++;

                // Consommer l'item (sauf créatif)
                if (!player.isCreative()) {
                    consumeItem(player, sourceBlock);
                }
            }

            // Son de placement
            if (placed > 0) {
                SoundType sound = sourceState.getSoundType();
                level.playSound(null, clickedPos, sound.getPlaceSound(),
                    SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Calcule les positions de prévisualisation.
     */
    public static List<BlockPos> calculatePreviewPositions(Level level, Player player,
                                                            BlockPos sourcePos, Direction face,
                                                            BlockState sourceState) {
        List<BlockPos> result = new ArrayList<>();
        Block sourceBlock = sourceState.getBlock();

        // Position de départ: 1 bloc devant la face pointée
        BlockPos startPos = sourcePos.relative(face);

        // Vérifier que le bloc de départ est libre
        if (!level.getBlockState(startPos).canBeReplaced()) {
            return result;
        }

        // Calculer la limite (inventaire ou créatif)
        int limit = MAX_BLOCKS;
        if (!player.isCreative()) {
            limit = Math.min(limit, countItemsInInventory(player, sourceBlock));
        }

        if (limit <= 0) return result;

        // Directions pour le flood fill 2D (perpendiculaires à la face)
        Direction[] spreadDirs = getSpreadDirections(face);

        // Flood fill avec BFS
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty() && result.size() < limit) {
            BlockPos current = queue.poll();

            // Vérifier que cette position est valide pour placement
            if (!level.getBlockState(current).canBeReplaced()) {
                continue;
            }

            // Vérifier que le bloc derrière (source) est du bon type
            BlockPos behindPos = current.relative(face.getOpposite());
            BlockState behindState = level.getBlockState(behindPos);
            if (!behindState.is(sourceBlock)) {
                continue;
            }

            // Ajouter à la prévisualisation
            result.add(current);

            // Explorer les voisins (4 directions + 4 diagonales sur le plan 2D)
            for (BlockPos neighbor : getNeighbors2D(current, spreadDirs)) {
                if (visited.contains(neighbor)) continue;
                visited.add(neighbor);

                // Vérifier la distance max
                if (getDistance2D(startPos, neighbor, spreadDirs) <= MAX_BLOCKS) {
                    queue.add(neighbor);
                }
            }
        }

        return result;
    }

    /**
     * Retourne les directions de propagation perpendiculaires à la face.
     */
    private static Direction[] getSpreadDirections(Direction face) {
        return switch (face.getAxis()) {
            case X -> new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};
            case Y -> new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            case Z -> new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST};
        };
    }

    /**
     * Retourne les 8 voisins sur le plan 2D (4 cardinaux + 4 diagonaux).
     */
    private static List<BlockPos> getNeighbors2D(BlockPos pos, Direction[] spreadDirs) {
        List<BlockPos> neighbors = new ArrayList<>();

        // 4 directions cardinales
        for (Direction dir : spreadDirs) {
            neighbors.add(pos.relative(dir));
        }

        // 4 diagonales (combinaisons de 2 directions)
        if (spreadDirs.length >= 4) {
            neighbors.add(pos.relative(spreadDirs[0]).relative(spreadDirs[2]));
            neighbors.add(pos.relative(spreadDirs[0]).relative(spreadDirs[3]));
            neighbors.add(pos.relative(spreadDirs[1]).relative(spreadDirs[2]));
            neighbors.add(pos.relative(spreadDirs[1]).relative(spreadDirs[3]));
        }

        return neighbors;
    }

    /**
     * Calcule la distance 2D (Chebyshev) sur le plan.
     */
    private static int getDistance2D(BlockPos start, BlockPos pos, Direction[] spreadDirs) {
        int dx = 0, dy = 0;

        for (Direction dir : spreadDirs) {
            int diff = switch (dir.getAxis()) {
                case X -> pos.getX() - start.getX();
                case Y -> pos.getY() - start.getY();
                case Z -> pos.getZ() - start.getZ();
            };
            diff = Math.abs(diff);

            if (dir == spreadDirs[0] || dir == spreadDirs[1]) {
                dx = Math.max(dx, diff);
            } else {
                dy = Math.max(dy, diff);
            }
        }

        return Math.max(dx, dy);
    }

    /**
     * Compte le nombre d'items du bloc dans l'inventaire du joueur.
     */
    private static int countItemsInInventory(Player player, Block block) {
        int count = 0;
        Item blockItem = block.asItem();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == blockItem) {
                count += stack.getCount();
            }
        }

        return count;
    }

    /**
     * Consomme un item du bloc dans l'inventaire.
     */
    private static void consumeItem(Player player, Block block) {
        Item blockItem = block.asItem();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == blockItem) {
                stack.shrink(1);
                return;
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
