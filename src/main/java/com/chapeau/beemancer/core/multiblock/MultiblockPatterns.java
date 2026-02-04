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

            // Étage 4 (Y+1): Honeyed Stone centre + 4 Conduits cardinaux
            .add(0, 1, 0, block(BeemancerBlocks.HONEYED_STONE))
            .add(0, 1, -1, block(BeemancerBlocks.HONEY_CRYSTAL_CONDUIT))   // N
            .add(0, 1, 1, block(BeemancerBlocks.HONEY_CRYSTAL_CONDUIT))    // S
            .add(1, 1, 0, block(BeemancerBlocks.HONEY_CRYSTAL_CONDUIT))    // E
            .add(-1, 1, 0, block(BeemancerBlocks.HONEY_CRYSTAL_CONDUIT))   // W
            .add(-1, 1, -1, air())  // Coins vides (pas de conduits)
            .add(1, 1, -1, air())
            .add(-1, 1, 1, air())
            .add(1, 1, 1, air())

            // Étage 5 (Y+2): Centre Honeyed Stone + 4 Honey Reservoirs
            .add(0, 2, 0, block(BeemancerBlocks.HONEYED_STONE))
            .add(0, 2, -1, block(BeemancerBlocks.HONEY_RESERVOIR))   // N
            .add(0, 2, 1, block(BeemancerBlocks.HONEY_RESERVOIR))    // S
            .add(1, 2, 0, block(BeemancerBlocks.HONEY_RESERVOIR))    // E
            .add(-1, 2, 0, block(BeemancerBlocks.HONEY_RESERVOIR))   // W
            .add(-1, 2, -1, air())  // Coins vides
            .add(1, 2, -1, air())
            .add(-1, 2, 1, air())
            .add(1, 2, 1, air())

            .build()
    );

    // ==================== HIVE MULTIBLOCK ====================
    // Structure 3x3x3 + 3x3 slabs on top, contrôleur au centre Y+0
    //
    // Étage 3 (Y+3 relatif): 3x3 Honeyed Slabs
    // Étage 0-2 (Y+0 à Y+2 relatif): 3x3x3 Hive Multiblock blocks
    // Controller at (0, 0, 0) = center of bottom layer

    public static final MultiblockPattern HIVE_MULTIBLOCK = register(
        MultiblockPattern.builder("hive_multiblock")
            // Layer 0 (Y+0): 3x3 hive blocks (center is controller, don't check it)
            .add(-1, 0, -1, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(0, 0, -1, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(1, 0, -1, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(-1, 0, 0, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            // (0, 0, 0) is the controller - skip
            .add(1, 0, 0, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(-1, 0, 1, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(0, 0, 1, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(1, 0, 1, block(BeemancerBlocks.HIVE_MULTIBLOCK))

            // Layer 1 (Y+1): 3x3 hive blocks
            .add(-1, 1, -1, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(0, 1, -1, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(1, 1, -1, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(-1, 1, 0, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(0, 1, 0, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(1, 1, 0, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(-1, 1, 1, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(0, 1, 1, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(1, 1, 1, block(BeemancerBlocks.HIVE_MULTIBLOCK))

            // Layer 2 (Y+2): 3x3 hive blocks
            .add(-1, 2, -1, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(0, 2, -1, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(1, 2, -1, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(-1, 2, 0, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(0, 2, 0, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(1, 2, 0, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(-1, 2, 1, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(0, 2, 1, block(BeemancerBlocks.HIVE_MULTIBLOCK))
            .add(1, 2, 1, block(BeemancerBlocks.HIVE_MULTIBLOCK))

            // Layer 3 (Y+3): 3x3 honeyed slabs on top
            .add(-1, 3, -1, slab(BeemancerBlocks.HONEYED_SLAB))
            .add(0, 3, -1, slab(BeemancerBlocks.HONEYED_SLAB))
            .add(1, 3, -1, slab(BeemancerBlocks.HONEYED_SLAB))
            .add(-1, 3, 0, slab(BeemancerBlocks.HONEYED_SLAB))
            .add(0, 3, 0, slab(BeemancerBlocks.HONEYED_SLAB))
            .add(1, 3, 0, slab(BeemancerBlocks.HONEYED_SLAB))
            .add(-1, 3, 1, slab(BeemancerBlocks.HONEYED_SLAB))
            .add(0, 3, 1, slab(BeemancerBlocks.HONEYED_SLAB))
            .add(1, 3, 1, slab(BeemancerBlocks.HONEYED_SLAB))

            .build()
    );

    // ==================== ESSENCE EXTRACTOR ====================
    // Structure 3x3x4, contrôleur au Y+0 (Extractor Heart) qui est à l'étage 3
    //
    // Étage 4 (Y+1 relatif): Honeyed Stone aux coins/centre + Reservoirs aux bords
    //         [S][R][S]
    //         [R][S][R]
    //         [S][R][S]
    //
    // Étage 3 (Y+0 relatif): Contrôleur au centre (Extractor Heart)
    //            [ ]
    //         [ ][H][ ]
    //            [ ]
    //
    // Étage 2 (Y-1 relatif): Vide (air)
    //            [ ]
    //         [ ][ ][ ]
    //            [ ]
    //
    // Étage 1 (Y-2 relatif): 5 Pedestals en croix + 4 Honeyed Stone aux coins
    //         [S][P][S]
    //         [P][P][P]
    //         [S][P][S]

    public static final MultiblockPattern ESSENCE_EXTRACTOR = register(
        MultiblockPattern.builder("essence_extractor")
            // Étage 1 (Y-2): 5 Pedestals en croix + 4 Honeyed Stone aux coins
            .add(0, -2, 0, block(BeemancerBlocks.HONEY_PEDESTAL))   // Centre
            .add(0, -2, -1, block(BeemancerBlocks.HONEY_PEDESTAL))  // Nord
            .add(0, -2, 1, block(BeemancerBlocks.HONEY_PEDESTAL))   // Sud
            .add(1, -2, 0, block(BeemancerBlocks.HONEY_PEDESTAL))   // Est
            .add(-1, -2, 0, block(BeemancerBlocks.HONEY_PEDESTAL))  // Ouest
            .add(-1, -2, -1, block(BeemancerBlocks.HONEYED_STONE))  // Coin NO
            .add(1, -2, -1, block(BeemancerBlocks.HONEYED_STONE))   // Coin NE
            .add(-1, -2, 1, block(BeemancerBlocks.HONEYED_STONE))   // Coin SO
            .add(1, -2, 1, block(BeemancerBlocks.HONEYED_STONE))    // Coin SE

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

            // Étage 3 (Y+0): Contrôleur au centre (ne pas vérifier), reste vide
            .add(0, 0, -1, air())
            .add(0, 0, 1, air())
            .add(1, 0, 0, air())
            .add(-1, 0, 0, air())
            .add(-1, 0, -1, air())
            .add(1, 0, -1, air())
            .add(-1, 0, 1, air())
            .add(1, 0, 1, air())

            // Étage 4 (Y+1): Honeyed Stone aux coins/centre + Reservoirs aux bords (N/S/E/W)
            .add(0, 1, 0, block(BeemancerBlocks.HONEYED_STONE))        // Centre
            .add(0, 1, -1, block(BeemancerBlocks.HONEY_RESERVOIR))     // Nord
            .add(0, 1, 1, block(BeemancerBlocks.HONEY_RESERVOIR))      // Sud
            .add(1, 1, 0, block(BeemancerBlocks.HONEY_RESERVOIR))      // Est
            .add(-1, 1, 0, block(BeemancerBlocks.HONEY_RESERVOIR))     // Ouest
            .add(-1, 1, -1, block(BeemancerBlocks.HONEYED_STONE))      // Coin NO
            .add(1, 1, -1, block(BeemancerBlocks.HONEYED_STONE))       // Coin NE
            .add(-1, 1, 1, block(BeemancerBlocks.HONEYED_STONE))       // Coin SO
            .add(1, 1, 1, block(BeemancerBlocks.HONEYED_STONE))        // Coin SE

            .build()
    );

    // ==================== STORAGE CONTROLLER ====================
    // Structure 3x3x3, contrôleur au centre Y+0
    //
    // Vue de face (Nord vers le joueur):
    //
    // Étage 3 (Y+1):  [P][H][P]    P=Controller Pipe, H=Controlled Hive
    // Étage 2 (Y+0):  [R][C][R]    R=Reservoir, C=Controller, T=Terminal (devant)
    // Étage 1 (Y-1):  [P][S][P]    S=Honeyed Stone
    //
    // Terminal à z=-1 (devant le controller)

    public static final MultiblockPattern STORAGE_CONTROLLER = register(
        MultiblockPattern.builder("storage_controller")
            // Étage 1 (Y-1): Controller Pipes + Honeyed Stone en ligne (z=0)
            .add(-1, -1, 0, block(BeemancerBlocks.CONTROLLER_PIPE))   // Pipe gauche
            .add(0, -1, 0, block(BeemancerBlocks.HONEYED_STONE))      // Pierre centre
            .add(1, -1, 0, block(BeemancerBlocks.CONTROLLER_PIPE))    // Pipe droite
            // Positions air autour de l'étage 1
            .add(-1, -1, -1, air())
            .add(0, -1, -1, air())
            .add(1, -1, -1, air())
            .add(-1, -1, 1, air())
            .add(0, -1, 1, air())
            .add(1, -1, 1, air())

            // Étage 2 (Y+0): Reservoirs + Controller + Terminal devant
            .add(-1, 0, 0, block(BeemancerBlocks.HONEY_RESERVOIR))    // Reservoir gauche
            // (0, 0, 0) = Controller - skip
            .add(1, 0, 0, block(BeemancerBlocks.HONEY_RESERVOIR))     // Reservoir droite
            .add(0, 0, -1, block(BeemancerBlocks.STORAGE_TERMINAL))   // Terminal devant
            // Positions air autour de l'étage 2
            .add(-1, 0, -1, air())
            .add(1, 0, -1, air())
            .add(-1, 0, 1, air())
            .add(0, 0, 1, air())
            .add(1, 0, 1, air())

            // Étage 3 (Y+1): Controller Pipes + Controlled Hive en ligne (z=0)
            .add(-1, 1, 0, block(BeemancerBlocks.CONTROLLER_PIPE))    // Pipe gauche
            .add(0, 1, 0, block(BeemancerBlocks.CONTROLLED_HIVE))     // Controlled Hive
            .add(1, 1, 0, block(BeemancerBlocks.CONTROLLER_PIPE))     // Pipe droite
            // Positions air autour de l'étage 3
            .add(-1, 1, -1, air())
            .add(0, 1, -1, air())
            .add(1, 1, -1, air())
            .add(-1, 1, 1, air())
            .add(0, 1, 1, air())
            .add(1, 1, 1, air())

            .build()
    );

    // ==================== ALEMBIC ====================
    // Structure 3x1x3 (ligne plate), contrôleur au centre Y+0
    //
    // Vue de face (axe X = gauche-droite):
    //
    // Étage 3 (Y+1):  [G][R][G]    G=Royal Gold Block, R=Reservoir
    // Étage 2 (Y+0):  [R][H][R]    H=Alembic Heart (contrôleur)
    // Étage 1 (Y-1):  [G][R][G]

    public static final MultiblockPattern ALEMBIC_MULTIBLOCK = register(
        MultiblockPattern.builder("alembic")
            // Étage 1 (Y-1): Royal Gold + Reservoir + Royal Gold
            .add(-1, -1, 0, block(BeemancerBlocks.ROYAL_GOLD_BLOCK))
            .add(0, -1, 0, block(BeemancerBlocks.HONEY_RESERVOIR))
            .add(1, -1, 0, block(BeemancerBlocks.ROYAL_GOLD_BLOCK))

            // Étage 2 (Y+0): Reservoir + Heart(skip) + Reservoir
            .add(-1, 0, 0, block(BeemancerBlocks.HONEY_RESERVOIR))
            // (0, 0, 0) = Alembic Heart - skip
            .add(1, 0, 0, block(BeemancerBlocks.HONEY_RESERVOIR))

            // Étage 3 (Y+1): Royal Gold + Reservoir + Royal Gold
            .add(-1, 1, 0, block(BeemancerBlocks.ROYAL_GOLD_BLOCK))
            .add(0, 1, 0, block(BeemancerBlocks.HONEYED_GLASS))
            .add(1, 1, 0, block(BeemancerBlocks.ROYAL_GOLD_BLOCK))

            .build()
    );

    // ==================== INFUSER MULTIBLOCK ====================
    // Structure 3x3x3, contrôleur au centre Y+0
    // Front ouvert (z=-1, nord) pour accès visuel
    //
    // Étage 3 (Y+1): 3x3 Honeyed Stone plein
    //         [S][S][S]
    //         [S][S][S]
    //         [S][S][S]
    //
    // Étage 2 (Y+0): Walls aux coins, rest air
    //         [W][ ][W]     W=Honeyed Stone Wall
    //         [ ][H][ ]     H=Infuser Heart (contrôleur), [ ]=Air
    //         [W][ ][W]
    //
    // Étage 1 (Y-1): 3x3 Honeyed Stone plein

    public static final MultiblockPattern INFUSER_MULTIBLOCK = register(
        MultiblockPattern.builder("infuser_multiblock")
            // Étage 1 (Y-1): 3x3 Honeyed Stone
            .add(-1, -1, -1, block(BeemancerBlocks.HONEYED_STONE))
            .add(0, -1, -1, block(BeemancerBlocks.HONEYED_STONE))
            .add(1, -1, -1, block(BeemancerBlocks.HONEYED_STONE))
            .add(-1, -1, 0, block(BeemancerBlocks.HONEYED_STONE))
            .add(0, -1, 0, block(BeemancerBlocks.HONEYED_STONE))
            .add(1, -1, 0, block(BeemancerBlocks.HONEYED_STONE))
            .add(-1, -1, 1, block(BeemancerBlocks.HONEYED_STONE))
            .add(0, -1, 1, block(BeemancerBlocks.HONEYED_STONE))
            .add(1, -1, 1, block(BeemancerBlocks.HONEYED_STONE))

            // Étage 2 (Y+0): Walls aux coins, rest air, Heart centre
            .add(-1, 0, -1, block(BeemancerBlocks.HONEYED_STONE_WALL))  // Coin NO
            .add(0, 0, -1, air())                                       // Front (ouvert)
            .add(1, 0, -1, block(BeemancerBlocks.HONEYED_STONE_WALL))   // Coin NE
            .add(-1, 0, 0, air())                                       // Ouest
            // (0, 0, 0) = Infuser Heart - skip
            .add(1, 0, 0, air())                                        // Est
            .add(-1, 0, 1, block(BeemancerBlocks.HONEYED_STONE_WALL))   // Coin SO
            .add(0, 0, 1, air())                                        // Sud
            .add(1, 0, 1, block(BeemancerBlocks.HONEYED_STONE_WALL))    // Coin SE

            // Étage 3 (Y+1): 3x3 Honeyed Stone
            .add(-1, 1, -1, block(BeemancerBlocks.HONEYED_STONE))
            .add(0, 1, -1, block(BeemancerBlocks.HONEYED_STONE))
            .add(1, 1, -1, block(BeemancerBlocks.HONEYED_STONE))
            .add(-1, 1, 0, block(BeemancerBlocks.HONEYED_STONE))
            .add(0, 1, 0, block(BeemancerBlocks.HONEYED_STONE))
            .add(1, 1, 0, block(BeemancerBlocks.HONEYED_STONE))
            .add(-1, 1, 1, block(BeemancerBlocks.HONEYED_STONE))
            .add(0, 1, 1, block(BeemancerBlocks.HONEYED_STONE))
            .add(1, 1, 1, block(BeemancerBlocks.HONEYED_STONE))

            .build()
    );

    // ==================== CENTRIFUGE MULTIBLOCK ====================
    // Structure 3x3x3, comme l'infuser mais SANS honeyed glass
    // Front ouvert (z=-1, nord)
    //
    // Étage 3 (Y+1): 3x3 Honeyed Stone plein
    //
    // Étage 2 (Y+0): Walls aux coins, rest air
    //         [W][ ][W]     W=Honeyed Stone Wall
    //         [ ][H][ ]     H=Centrifuge Heart (contrôleur), [ ]=Air
    //         [W][ ][W]
    //
    // Étage 1 (Y-1): 3x3 Honeyed Stone plein

    public static final MultiblockPattern CENTRIFUGE_MULTIBLOCK = register(
        MultiblockPattern.builder("centrifuge_multiblock")
            // Étage 1 (Y-1): 3x3 Honeyed Stone
            .add(-1, -1, -1, block(BeemancerBlocks.HONEYED_STONE))
            .add(0, -1, -1, block(BeemancerBlocks.HONEYED_STONE))
            .add(1, -1, -1, block(BeemancerBlocks.HONEYED_STONE))
            .add(-1, -1, 0, block(BeemancerBlocks.HONEYED_STONE))
            .add(0, -1, 0, block(BeemancerBlocks.HONEYED_STONE))
            .add(1, -1, 0, block(BeemancerBlocks.HONEYED_STONE))
            .add(-1, -1, 1, block(BeemancerBlocks.HONEYED_STONE))
            .add(0, -1, 1, block(BeemancerBlocks.HONEYED_STONE))
            .add(1, -1, 1, block(BeemancerBlocks.HONEYED_STONE))

            // Étage 2 (Y+0): Walls aux coins, rest air
            .add(-1, 0, -1, block(BeemancerBlocks.HONEYED_STONE_WALL))  // Coin NO
            .add(0, 0, -1, air())                                       // Front
            .add(1, 0, -1, block(BeemancerBlocks.HONEYED_STONE_WALL))   // Coin NE
            .add(-1, 0, 0, air())                                       // Ouest
            // (0, 0, 0) = Centrifuge Heart - skip
            .add(1, 0, 0, air())                                        // Est
            .add(-1, 0, 1, block(BeemancerBlocks.HONEYED_STONE_WALL))   // Coin SO
            .add(0, 0, 1, air())                                        // Sud
            .add(1, 0, 1, block(BeemancerBlocks.HONEYED_STONE_WALL))    // Coin SE

            // Étage 3 (Y+1): 3x3 Honeyed Stone
            .add(-1, 1, -1, block(BeemancerBlocks.HONEYED_STONE))
            .add(0, 1, -1, block(BeemancerBlocks.HONEYED_STONE))
            .add(1, 1, -1, block(BeemancerBlocks.HONEYED_STONE))
            .add(-1, 1, 0, block(BeemancerBlocks.HONEYED_STONE))
            .add(0, 1, 0, block(BeemancerBlocks.HONEYED_STONE))
            .add(1, 1, 0, block(BeemancerBlocks.HONEYED_STONE))
            .add(-1, 1, 1, block(BeemancerBlocks.HONEYED_STONE))
            .add(0, 1, 1, block(BeemancerBlocks.HONEYED_STONE))
            .add(1, 1, 1, block(BeemancerBlocks.HONEYED_STONE))

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
