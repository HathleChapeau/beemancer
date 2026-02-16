/**
 * ============================================================
 * [PathCollisionHelper.java]
 * Description: Detection de collision et ligne de vue pour le pathfinding abeilles
 * ============================================================
 *
 * Responsabilites:
 * - Verification de traversabilite des blocs (isPassable)
 * - Detection de ligne de vue DDA 3D (hasLineOfSight)
 * - Calcul de penalite de proximite aux murs (computeWallPenalty)
 *
 * Reference: Create utilise isPathfindable(PathComputationType.AIR) sur 50+ blocs.
 * Reference: Cobblemon OmniPathNodeMaker.adjustNodeType() pour la classification.
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Level               | Monde Minecraft      | Lecture des BlockState         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ThetaStarSolver.java: Verification collisions pendant le pathfinding
 * - BeePathfinding.java: Vol direct (ligne de vue)
 *
 * ============================================================
 */
package com.chapeau.apica.common.entity.bee.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

public class PathCollisionHelper {

    private static final double WALL_PENALTY = 0.5;

    /**
     * Verifie si une position est traversable par une abeille.
     * Verifie le bloc et le bloc au-dessus (clearance 1x2 pour le hitbox).
     *
     * Utilise isPathfindable(AIR) comme check principal (hook vanilla/NeoForge),
     * puis bloque explicitement les blocs dangereux.
     */
    public static boolean isPassable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isBlockTraversable(state)) {
            return false;
        }
        BlockState above = level.getBlockState(pos.above());
        return isBlockTraversable(above);
    }

    /**
     * Verifie si un BlockState est traversable par une abeille.
     * Combine le hook vanilla isPathfindable avec des checks explicites
     * pour les blocs dangereux et les blocs a collision partielle.
     */
    private static boolean isBlockTraversable(BlockState state) {
        if (state.isSolid() || state.liquid()) {
            return false;
        }
        if (!state.isPathfindable(PathComputationType.AIR)) {
            return false;
        }
        return !isBlockDangerous(state);
    }

    /**
     * Verifie si un bloc est dangereux pour les abeilles.
     * Ces blocs sont techniquement traversables mais causent des degats ou ralentissent.
     */
    private static boolean isBlockDangerous(BlockState state) {
        return state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.POINTED_DRIPSTONE);
    }

    /**
     * Calcule la penalite de proximite aux murs.
     * Plus il y a de blocs solides adjacents, plus le cout est eleve.
     */
    public static double computeWallPenalty(Level level, BlockPos pos) {
        int solidCount = 0;
        for (Direction dir : Direction.values()) {
            BlockState neighborState = level.getBlockState(pos.relative(dir));
            if (neighborState.isSolid()) {
                solidCount++;
            }
        }
        return solidCount * WALL_PENALTY;
    }

    /**
     * Verifie la ligne de vue avec l'algorithme DDA 3D.
     * Avance pas-a-pas le long de l'axe dominant, interpolant les deux autres axes.
     * Utilise l'arithmetique entiere pour eviter les erreurs de floating point.
     */
    public static boolean hasLineOfSight(Level level, BlockPos start, BlockPos end) {
        int x0 = start.getX(), y0 = start.getY(), z0 = start.getZ();
        int x1 = end.getX(), y1 = end.getY(), z1 = end.getZ();

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int dz = Math.abs(z1 - z0);

        int steps = Math.max(dx, Math.max(dy, dz));
        if (steps == 0) return true;

        int deltaX = x1 - x0;
        int deltaY = y1 - y0;
        int deltaZ = z1 - z0;

        for (int i = 1; i < steps; i++) {
            int x = x0 + (deltaX * i + (deltaX > 0 ? steps / 2 : -steps / 2)) / steps;
            int y = y0 + (deltaY * i + (deltaY > 0 ? steps / 2 : -steps / 2)) / steps;
            int z = z0 + (deltaZ * i + (deltaZ > 0 ? steps / 2 : -steps / 2)) / steps;

            if (!isPassable(level, new BlockPos(x, y, z))) {
                return false;
            }
        }

        return true;
    }
}
