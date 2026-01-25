/**
 * ============================================================
 * [MultiblockPatterns.java]
 * Description: Registre central de tous les patterns multiblocs
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | BeemancerBlocks     | Accès blocs          | Définition patterns   |
 * | MultiblockPattern   | Structure pattern    | Création              |
 * | BlockMatcher        | Predicates           | Définition            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - Blocs contrôleurs de multiblocs
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.multiblock;

import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import net.minecraft.core.Direction;

import java.util.HashMap;
import java.util.Map;

import static com.chapeau.beemancer.core.multiblock.BlockMatcher.*;

/**
 * Registre central des patterns de multiblocs.
 * Chaque pattern est défini de manière déclarative.
 */
public class MultiblockPatterns {

    private static final Map<String, MultiblockPattern> PATTERNS = new HashMap<>();

    // ==================== HONEY ALTAR ====================
    // Structure 3x3x5, contrôleur au Y+2 (Honey Crystal)
    //
    // Étage 5 (Y+2 relatif):     [S]
    //                         [S][S][S]
    //                            [S]
    //
    // Étage 4 (Y+1 relatif):     [C]
    //                         [C][S][C]
    //                            [C]
    //
    // Étage 3 (Y+0 relatif):     (contrôleur)
    //
    // Étage 2 (Y-1 relatif):    (vide)
    //
    // Étage 1 (Y-2 relatif):     [T]
    //                         [T][P][T]
    //                            [T]

    public static final MultiblockPattern HONEY_ALTAR = register(
        MultiblockPattern.builder("honey_altar")
            // Étage 1 (Y-2): Pedestal + Stairs
            .add(0, -2, 0, block(BeemancerBlocks.HONEY_PEDESTAL))
            .add(0, -2, -1, stairFacing(BeemancerBlocks.HONEYED_STONE_STAIR, Direction.SOUTH))  // Nord → pointe Sud
            .add(0, -2, 1, stairFacing(BeemancerBlocks.HONEYED_STONE_STAIR, Direction.NORTH))   // Sud → pointe Nord
            .add(1, -2, 0, stairFacing(BeemancerBlocks.HONEYED_STONE_STAIR, Direction.WEST))    // Est → pointe Ouest
            .add(-1, -2, 0, stairFacing(BeemancerBlocks.HONEYED_STONE_STAIR, Direction.EAST))   // Ouest → pointe Est
            .add(-1, -2, -1, air())  // Coins vides
            .add(1, -2, -1, air())
            .add(-1, -2, 1, air())
            .add(1, -2, 1, air())

            // Étage 2 (Y-1): Vide
            .add(0, -1, 0, air())
            .add(0, -1, -1, air())
            .add(0, -1, 1, air())
            .add(1, -1, 0, air())
            .add(-1, -1, 0, air())
            .add(-1, -1, -1, air())
            .add(1, -1, -1, air())
            .add(-1, -1, 1, air())
            .add(1, -1, 1, air())

            // Étage 3 (Y+0): Contrôleur (Honey Crystal) - pas besoin de vérifier
            // Les positions autour doivent être vides
            .add(0, 0, -1, air())
            .add(0, 0, 1, air())
            .add(1, 0, 0, air())
            .add(-1, 0, 0, air())
            .add(-1, 0, -1, air())
            .add(1, 0, -1, air())
            .add(-1, 0, 1, air())
            .add(1, 0, 1, air())

            // Étage 4 (Y+1): Honeyed Stone centre + 4 Conduits
            .add(0, 1, 0, block(BeemancerBlocks.HONEYED_STONE))
            .add(0, 1, -1, block(BeemancerBlocks.HONEY_CRYSTAL_CONDUIT))
            .add(0, 1, 1, block(BeemancerBlocks.HONEY_CRYSTAL_CONDUIT))
            .add(1, 1, 0, block(BeemancerBlocks.HONEY_CRYSTAL_CONDUIT))
            .add(-1, 1, 0, block(BeemancerBlocks.HONEY_CRYSTAL_CONDUIT))
            .add(-1, 1, -1, air())  // Coins vides
            .add(1, 1, -1, air())
            .add(-1, 1, 1, air())
            .add(1, 1, 1, air())

            // Étage 5 (Y+2): 5 Honeyed Stone en croix
            .add(0, 2, 0, block(BeemancerBlocks.HONEYED_STONE))
            .add(0, 2, -1, block(BeemancerBlocks.HONEYED_STONE))
            .add(0, 2, 1, block(BeemancerBlocks.HONEYED_STONE))
            .add(1, 2, 0, block(BeemancerBlocks.HONEYED_STONE))
            .add(-1, 2, 0, block(BeemancerBlocks.HONEYED_STONE))
            .add(-1, 2, -1, air())  // Coins vides
            .add(1, 2, -1, air())
            .add(-1, 2, 1, air())
            .add(1, 2, 1, air())

            .build()
    );

    private static MultiblockPattern register(MultiblockPattern pattern) {
        PATTERNS.put(pattern.getId(), pattern);
        return pattern;
    }

    public static MultiblockPattern get(String id) {
        return PATTERNS.get(id);
    }
}
