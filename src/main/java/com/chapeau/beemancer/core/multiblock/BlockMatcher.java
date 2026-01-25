/**
 * ============================================================
 * [BlockMatcher.java]
 * Description: Predicates réutilisables pour validation de multiblocs
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | Level               | Accès monde          | Vérification blocs    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MultiblockPattern (définition patterns)
 * - MultiblockValidator (validation)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.function.Supplier;

/**
 * Fournit des predicates réutilisables pour la validation de multiblocs.
 */
public class BlockMatcher {

    /**
     * Interface fonctionnelle pour matcher un bloc à une position.
     */
    @FunctionalInterface
    public interface Matcher {
        boolean matches(Level level, BlockPos pos);
    }

    /**
     * Accepte l'air ou les blocs remplaçables.
     */
    public static Matcher air() {
        return (level, pos) -> {
            BlockState state = level.getBlockState(pos);
            return state.isAir() || state.canBeReplaced();
        };
    }

    /**
     * Accepte un bloc spécifique.
     */
    public static Matcher block(Supplier<? extends Block> blockSupplier) {
        return (level, pos) -> level.getBlockState(pos).is(blockSupplier.get());
    }

    /**
     * Accepte un escalier pointant dans une direction spécifique.
     */
    public static Matcher stairFacing(Supplier<? extends Block> blockSupplier, Direction facing) {
        return (level, pos) -> {
            BlockState state = level.getBlockState(pos);

            if (!state.is(blockSupplier.get())) {
                return false;
            }

            if (state.hasProperty(StairBlock.FACING)) {
                if (state.getValue(StairBlock.FACING) != facing) {
                    return false;
                }
            }

            if (state.hasProperty(StairBlock.HALF)) {
                if (state.getValue(StairBlock.HALF) != Half.BOTTOM) {
                    return false;
                }
            }

            return true;
        };
    }

    /**
     * Accepte n'importe quel bloc (skip cette position dans la validation).
     */
    public static Matcher any() {
        return (level, pos) -> true;
    }

    /**
     * Combine plusieurs matchers avec OR.
     */
    public static Matcher or(Matcher... matchers) {
        return (level, pos) -> {
            for (Matcher m : matchers) {
                if (m.matches(level, pos)) return true;
            }
            return false;
        };
    }

    /**
     * Combine plusieurs matchers avec AND.
     */
    public static Matcher and(Matcher... matchers) {
        return (level, pos) -> {
            for (Matcher m : matchers) {
                if (!m.matches(level, pos)) return false;
            }
            return true;
        };
    }

    /**
     * Accepte une slab (top ou bottom).
     */
    public static Matcher slab(Supplier<? extends Block> blockSupplier) {
        return (level, pos) -> level.getBlockState(pos).is(blockSupplier.get());
    }
}
