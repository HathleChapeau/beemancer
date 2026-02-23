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
 * | ApicaBlocks     | Accès blocs          | Définition patterns   |
 * | MultiblockPattern   | Structure pattern    | Création              |
 * | BlockMatcher        | Predicates           | Définition            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - Blocs contrôleurs de multiblocs
 *
 * ============================================================
 */
package com.chapeau.apica.core.multiblock;

import com.chapeau.apica.core.registry.ApicaBlocks;

import java.util.HashMap;
import java.util.Map;

import static com.chapeau.apica.core.multiblock.BlockMatcher.*;

/**
 * Registre central des patterns de multiblocs.
 * Chaque pattern est défini de manière déclarative.
 */
public class MultiblockPatterns {

    private static final Map<String, MultiblockPattern> PATTERNS = new HashMap<>();

    // ==================== HONEY ALTAR ====================

    public static final MultiblockPattern HONEY_ALTAR = register(
        MultiblockPattern.builder("honey_altar")
            // Étage 1 (Y-2): Pedestal + Honeyed Stone cardinaux
            .add(0, -2, 0, block(ApicaBlocks.HONEY_PEDESTAL))
            //.add(0, -2, -1, air())   // Nord
            //.add(0, -2, 1, air())    // Sud
            //.add(1, -2, 0, air())    // Est
            //.add(-1, -2, 0, air())   // Ouest
            //.add(-1, -2, -1, air())  // Coins vides
            //.add(1, -2, -1, air())
            //.add(-1, -2, 1, air())
            //.add(1, -2, 1, air())

            // Étage 2 (Y-1): Vide
            .add(0, -1, 0, air())
            //.add(0, -1, -1, air())
            //.add(0, -1, 1, air())
            //.add(1, -1, 0, air())
            //.add(-1, -1, 0, air())
            //.add(-1, -1, -1, air())
            //.add(1, -1, -1, air())
            //.add(-1, -1, 1, air())
            //.add(1, -1, 1, air())

            // Étage 3 (Y+0): Contrôleur (Honey Crystal) - pas besoin de vérifier
            // Les positions autour doivent être vides
            //.add(0, 0, -1, air())
            //.add(0, 0, 1, air())
            //.add(1, 0, 0, air())
            //.add(-1, 0, 0, air())
            //.add(-1, 0, -1, air())
            //.add(1, 0, -1, air())
            //.add(-1, 0, 1, air())
            //.add(1, 0, 1, air())

            // Étage 4 (Y+1): Honeyed Stone centre + 4 Conduits cardinaux
            .add(0, 1, 0, block(ApicaBlocks.IRON_FOUNDATION))
            .add(0, 1, -1, block(ApicaBlocks.HONEY_CRYSTAL_CONDUIT))   // N
            .add(0, 1, 1, block(ApicaBlocks.HONEY_CRYSTAL_CONDUIT))    // S
            .add(1, 1, 0, block(ApicaBlocks.HONEY_CRYSTAL_CONDUIT))    // E
            .add(-1, 1, 0, block(ApicaBlocks.HONEY_CRYSTAL_CONDUIT))   // W
            .add(-1, 1, -1, block(ApicaBlocks.IRON_FOUNDATION))  // Coins vides (pas de conduits)
            .add(1, 1, -1, block(ApicaBlocks.IRON_FOUNDATION))
            .add(-1, 1, 1, block(ApicaBlocks.IRON_FOUNDATION))
            .add(1, 1, 1, block(ApicaBlocks.IRON_FOUNDATION))

            // Étage 5 (Y+2): Centre Honeyed Stone + 4 Honey Reservoirs
            .add(0, 2, 0, block(ApicaBlocks.IRON_FOUNDATION))
            .add(0, 2, -1, block(ApicaBlocks.HONEY_RESERVOIR))   // N
            .add(0, 2, 1, block(ApicaBlocks.HONEY_RESERVOIR))    // S
            .add(1, 2, 0, block(ApicaBlocks.HONEY_RESERVOIR))    // E
            .add(-1, 2, 0, block(ApicaBlocks.HONEY_RESERVOIR))   // W
            .add(-1, 2, -1, block(ApicaBlocks.IRON_FOUNDATION))  // Coins vides
            .add(1, 2, -1, block(ApicaBlocks.IRON_FOUNDATION))
            .add(-1, 2, 1, block(ApicaBlocks.IRON_FOUNDATION))
            .add(1, 2, 1, block(ApicaBlocks.IRON_FOUNDATION))

            .build()
    );

    // ==================== HIVE MULTIBLOCK ====================
    // Structure 3x3x3 pure, contrôleur au centre Y+0
    // Étage 0-2 (Y+0 à Y+2 relatif): 3x3x3 Hive Multiblock blocks
    // Controller at (0, 0, 0) = center of bottom layer

    public static final MultiblockPattern HIVE_MULTIBLOCK = register(
        MultiblockPattern.builder("hive_multiblock")
            // Layer 0 (Y-1): 3x3 hive blocks (bottom layer)
            .add(-1, -1, -1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(0, -1, -1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(1, -1, -1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(-1, -1, 0, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(0, -1, 0, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(1, -1, 0, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(-1, -1, 1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(0, -1, 1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(1, -1, 1, block(ApicaBlocks.HIVE_MULTIBLOCK))

            // Layer 1 (Y+0): 3x3 hive blocks (middle layer, center is controller)
            .add(-1, 0, -1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(0, 0, -1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(1, 0, -1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(-1, 0, 0, block(ApicaBlocks.HIVE_MULTIBLOCK))
            // (0, 0, 0) is the controller - skip
            .add(1, 0, 0, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(-1, 0, 1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(0, 0, 1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(1, 0, 1, block(ApicaBlocks.HIVE_MULTIBLOCK))

            // Layer 2 (Y+1): 3x3 hive blocks (top layer)
            .add(-1, 1, -1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(0, 1, -1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(1, 1, -1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(-1, 1, 0, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(0, 1, 0, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(1, 1, 0, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(-1, 1, 1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(0, 1, 1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(1, 1, 1, block(ApicaBlocks.HIVE_MULTIBLOCK))

            .build()
    );

    // Pattern display-only pour le codex (inclut les iron foundation slabs sous la structure)
    public static final MultiblockPattern HIVE_MULTIBLOCK_DISPLAY = register(
        MultiblockPattern.builder("hive_multiblock_display")
            // Layer 3 (Y+2): 3x3 iron foundation slabs (top)
            .add(-1, 2, -1, block(ApicaBlocks.IRON_FOUNDATION_SLAB))
            .add(0, 2, -1, block(ApicaBlocks.IRON_FOUNDATION_SLAB))
            .add(1, 2, -1, block(ApicaBlocks.IRON_FOUNDATION_SLAB))
            .add(-1, 2, 0, block(ApicaBlocks.IRON_FOUNDATION_SLAB))
            .add(0, 2, 0, block(ApicaBlocks.IRON_FOUNDATION_SLAB))
            .add(1, 2, 0, block(ApicaBlocks.IRON_FOUNDATION_SLAB))
            .add(-1, 2, 1, block(ApicaBlocks.IRON_FOUNDATION_SLAB))
            .add(0, 2, 1, block(ApicaBlocks.IRON_FOUNDATION_SLAB))
            .add(1, 2, 1, block(ApicaBlocks.IRON_FOUNDATION_SLAB))

            // Layer 0 (Y-1): 3x3 hive blocks (bottom layer)
            .add(-1, -1, -1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(0, -1, -1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(1, -1, -1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(-1, -1, 0, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(0, -1, 0, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(1, -1, 0, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(-1, -1, 1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(0, -1, 1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(1, -1, 1, block(ApicaBlocks.HIVE_MULTIBLOCK))

            // Layer 1 (Y+0): 3x3 hive blocks (middle layer, center is controller)
            .add(-1, 0, -1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(0, 0, -1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(1, 0, -1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(-1, 0, 0, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(1, 0, 0, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(-1, 0, 1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(0, 0, 1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(1, 0, 1, block(ApicaBlocks.HIVE_MULTIBLOCK))

            // Layer 2 (Y+1): 3x3 hive blocks (top layer)
            .add(-1, 1, -1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(0, 1, -1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(1, 1, -1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(-1, 1, 0, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(0, 1, 0, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(1, 1, 0, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(-1, 1, 1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(0, 1, 1, block(ApicaBlocks.HIVE_MULTIBLOCK))
            .add(1, 1, 1, block(ApicaBlocks.HIVE_MULTIBLOCK))

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
            .add(0, -2, 0, block(ApicaBlocks.HONEY_PEDESTAL))   // Centre
            .add(0, -2, -1, block(ApicaBlocks.HONEY_PEDESTAL))  // Nord
            .add(0, -2, 1, block(ApicaBlocks.HONEY_PEDESTAL))   // Sud
            .add(1, -2, 0, block(ApicaBlocks.HONEY_PEDESTAL))   // Est
            .add(-1, -2, 0, block(ApicaBlocks.HONEY_PEDESTAL))  // Ouest
            //.add(-1, -2, -1, air())  // Coin NO
            //.add(1, -2, -1, air())   // Coin NE
            //.add(-1, -2, 1, air())   // Coin SO
            //.add(1, -2, 1, air())    // Coin SE

            // Étage 2 (Y-1): Vide
            .add(0, -1, 0, air())
            //.add(0, -1, -1, air())
            //.add(0, -1, 1, air())
            //.add(1, -1, 0, air())
            //.add(-1, -1, 0, air())
            //.add(-1, -1, -1, air())
            //.add(1, -1, -1, air())
            //.add(-1, -1, 1, air())
            //.add(1, -1, 1, air())

            // Étage 3 (Y+0): Contrôleur au centre (ne pas vérifier), reste vide
            /*.add(0, 0, -1, air())
            .add(0, 0, 1, air())
            .add(1, 0, 0, air())
            .add(-1, 0, 0, air())
            .add(-1, 0, -1, air())
            .add(1, 0, -1, air())
            .add(-1, 0, 1, air())
            .add(1, 0, 1, air())*/

            // Étage 4 (Y+1): Honeyed Stone aux coins/centre + Reservoirs aux bords (N/S/E/W)
            .add(0, 1, 0, block(ApicaBlocks.IRON_FOUNDATION))        // Centre
            .add(0, 1, -1, block(ApicaBlocks.HONEY_RESERVOIR))     // Nord
            .add(0, 1, 1, block(ApicaBlocks.HONEY_RESERVOIR))      // Sud
            .add(1, 1, 0, block(ApicaBlocks.HONEY_RESERVOIR))      // Est
            .add(-1, 1, 0, block(ApicaBlocks.HONEY_RESERVOIR))     // Ouest
            //.add(-1, 1, -1, air())      // Coin NO
            //.add(1, 1, -1, air())       // Coin NE
            //.add(-1, 1, 1, air())       // Coin SO
            //.add(1, 1, 1, air())        // Coin SE

            .build()
    );

    // ==================== STORAGE CONTROLLER ====================
    // Structure 3x3x3 symétrique, contrôleur au centre Y+0
    //
    // Étage 3 (Y+1):  [F][R][F]    F=Iron Foundation, R=Reservoir, H=Controlled Hive
    //                  [R][H][R]    (couche supérieure = miroir retourné x=180)
    //                  [F][R][F]
    //
    // Étage 2 (Y+0):  [ ][T][ ]    T=Terminal, C=Controller (coeur)
    //                  [T][C][T]
    //                  [ ][T][ ]
    //
    // Étage 1 (Y-1):  [F][R][F]    F=Iron Foundation, R=Reservoir, H=Controlled Hive
    //                  [R][H][R]    (couche inférieure = base)
    //                  [F][R][F]

    public static final MultiblockPattern STORAGE_CONTROLLER = register(
        MultiblockPattern.builder("storage_controller")
            // Étage 1 (Y-1): Base — Iron Foundation coins, Reservoirs cardinaux, Controlled Hive centre
            .add(-1, -1, -1, block(ApicaBlocks.IRON_FOUNDATION))  // Coin NO
            .add(0, -1, -1, block(ApicaBlocks.HONEY_RESERVOIR))   // Nord
            .add(1, -1, -1, block(ApicaBlocks.IRON_FOUNDATION))   // Coin NE
            .add(-1, -1, 0, block(ApicaBlocks.HONEY_RESERVOIR))   // Ouest
            .add(0, -1, 0, block(ApicaBlocks.CONTROLLED_HIVE))    // Centre
            .add(1, -1, 0, block(ApicaBlocks.HONEY_RESERVOIR))    // Est
            .add(-1, -1, 1, block(ApicaBlocks.IRON_FOUNDATION))   // Coin SO
            .add(0, -1, 1, block(ApicaBlocks.HONEY_RESERVOIR))    // Sud
            .add(1, -1, 1, block(ApicaBlocks.IRON_FOUNDATION))    // Coin SE

            // Étage 2 (Y+0): Terminals cardinaux, Controller au centre (skip)
            .add(0, 0, -1, block(ApicaBlocks.STORAGE_TERMINAL))   // Terminal Nord
            .add(-1, 0, 0, block(ApicaBlocks.STORAGE_TERMINAL))   // Terminal Ouest
            // (0, 0, 0) = Controller - skip
            .add(1, 0, 0, block(ApicaBlocks.STORAGE_TERMINAL))    // Terminal Est
            .add(0, 0, 1, block(ApicaBlocks.STORAGE_TERMINAL))    // Terminal Sud

            // Étage 3 (Y+1): Miroir du bas — retourné (x=180 via blockstate)
            .add(-1, 1, -1, block(ApicaBlocks.IRON_FOUNDATION))   // Coin NO
            .add(0, 1, -1, block(ApicaBlocks.HONEY_RESERVOIR))    // Nord
            .add(1, 1, -1, block(ApicaBlocks.IRON_FOUNDATION))    // Coin NE
            .add(-1, 1, 0, block(ApicaBlocks.HONEY_RESERVOIR))    // Ouest
            .add(0, 1, 0, block(ApicaBlocks.CONTROLLED_HIVE))     // Centre
            .add(1, 1, 0, block(ApicaBlocks.HONEY_RESERVOIR))     // Est
            .add(-1, 1, 1, block(ApicaBlocks.IRON_FOUNDATION))    // Coin SO
            .add(0, 1, 1, block(ApicaBlocks.HONEY_RESERVOIR))     // Sud
            .add(1, 1, 1, block(ApicaBlocks.IRON_FOUNDATION))     // Coin SE

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
            .add(-1, -1, 0, block(ApicaBlocks.ROYAL_GOLD_BLOCK))
            .add(0, -1, 0, block(ApicaBlocks.HONEY_RESERVOIR))
            .add(1, -1, 0, block(ApicaBlocks.ROYAL_GOLD_BLOCK))

            // Étage 2 (Y+0): Reservoir + Heart(skip) + Reservoir
            .add(-1, 0, 0, block(ApicaBlocks.HONEY_RESERVOIR))
            // (0, 0, 0) = Alembic Heart - skip
            .add(1, 0, 0, block(ApicaBlocks.HONEY_RESERVOIR))

            // Étage 3 (Y+1): Royal Gold + Reservoir + Royal Gold
            .add(-1, 1, 0, block(ApicaBlocks.ROYAL_GOLD_BLOCK))
            .add(0, 1, 0, block(ApicaBlocks.HONEYED_GLASS))
            .add(1, 1, 0, block(ApicaBlocks.ROYAL_GOLD_BLOCK))

            .build()
    );

    // ==================== INFUSER MULTIBLOCK ====================
    // Structure 3x3x3 avec reservoirs aux cardinaux (meme layout que centrifuge)
    //
    // Étage 3 (Y+1): Reservoirs cardinaux (input), Honeyed Stone coins + centre
    //         [S][R][S]     R=Reservoir (input), S=Honeyed Stone
    //         [R][S][R]
    //         [S][R][S]
    //
    // Étage 2 (Y+0): Air autour du coeur
    //         [ ][ ][ ]
    //         [ ][H][ ]     H=Infuser Heart (contrôleur), [ ]=Air
    //         [ ][ ][ ]
    //
    // Étage 1 (Y-1): Reservoirs cardinaux (output), Honeyed Stone coins + centre
    //         [S][R][S]     R=Reservoir (output), S=Honeyed Stone
    //         [R][S][R]
    //         [S][R][S]

    public static final MultiblockPattern INFUSER_MULTIBLOCK = register(
        MultiblockPattern.builder("infuser_multiblock")
            // Étage 1 (Y-1): Honeyed Stone coins + centre, Reservoirs cardinaux
            .add(-1, -1, -1, block(ApicaBlocks.IRON_FOUNDATION))    // Coin NO
            .add(0, -1, -1, block(ApicaBlocks.HONEY_RESERVOIR))   // Nord - output
            .add(1, -1, -1, block(ApicaBlocks.IRON_FOUNDATION))     // Coin NE
            .add(-1, -1, 0, block(ApicaBlocks.HONEY_RESERVOIR))   // Ouest - output
            .add(0, -1, 0, block(ApicaBlocks.IRON_FOUNDATION))      // Centre
            .add(1, -1, 0, block(ApicaBlocks.HONEY_RESERVOIR))    // Est - output
            .add(-1, -1, 1, block(ApicaBlocks.IRON_FOUNDATION))     // Coin SO
            .add(0, -1, 1, block(ApicaBlocks.HONEY_RESERVOIR))    // Sud - output
            .add(1, -1, 1, block(ApicaBlocks.IRON_FOUNDATION))      // Coin SE

            // Étage 2 (Y+0): Air autour du coeur
            .add(-1, 0, -1, air())
            .add(0, 0, -1, air())
            .add(1, 0, -1, air())
            .add(-1, 0, 0, air())
            // (0, 0, 0) = Infuser Heart - skip
            .add(1, 0, 0, air())
            .add(-1, 0, 1, air())
            .add(0, 0, 1, air())
            .add(1, 0, 1, air())

            // Étage 3 (Y+1): Honeyed Stone coins + centre, Reservoirs cardinaux
            .add(-1, 1, -1, block(ApicaBlocks.IRON_FOUNDATION))     // Coin NO
            .add(0, 1, -1, block(ApicaBlocks.HONEY_RESERVOIR))    // Nord - input
            .add(1, 1, -1, block(ApicaBlocks.IRON_FOUNDATION))      // Coin NE
            .add(-1, 1, 0, block(ApicaBlocks.HONEY_RESERVOIR))    // Ouest - input
            .add(0, 1, 0, block(ApicaBlocks.IRON_FOUNDATION))       // Centre
            .add(1, 1, 0, block(ApicaBlocks.HONEY_RESERVOIR))     // Est - input
            .add(-1, 1, 1, block(ApicaBlocks.IRON_FOUNDATION))      // Coin SO
            .add(0, 1, 1, block(ApicaBlocks.HONEY_RESERVOIR))     // Sud - input
            .add(1, 1, 1, block(ApicaBlocks.IRON_FOUNDATION))       // Coin SE

            .build()
    );

    // ==================== CENTRIFUGE MULTIBLOCK ====================
    // Structure 3x3x3 avec reservoirs aux cardinaux
    //
    // Étage 3 (Y+1): Reservoirs cardinaux (output), Honeyed Stone coins + centre
    //         [S][R][S]     R=Reservoir (output), S=Honeyed Stone
    //         [R][S][R]
    //         [S][R ][S]
    //
    // Étage 2 (Y+0): Air autour du coeur
    //         [ ][ ][ ]
    //         [ ][H][ ]     H=Centrifuge Heart (contrôleur), [ ]=Air
    //         [ ][ ][ ]
    //
    // Étage 1 (Y-1): Reservoirs cardinaux (fuel), Honeyed Stone coins + centre
    //         [S][R][S]     R=Reservoir (fuel), S=Honeyed Stone
    //         [R][S][R]
    //         [S][R][S]

    public static final MultiblockPattern CENTRIFUGE_MULTIBLOCK = register(
        MultiblockPattern.builder("centrifuge_multiblock")
            // Étage 1 (Y-1): Honeyed Stone coins + centre, Reservoirs cardinaux
            .add(-1, -1, -1, block(ApicaBlocks.IRON_FOUNDATION))    // Coin NO
            .add(0, -1, -1, block(ApicaBlocks.HONEY_RESERVOIR))   // Nord - fuel
            .add(1, -1, -1, block(ApicaBlocks.IRON_FOUNDATION))     // Coin NE
            .add(-1, -1, 0, block(ApicaBlocks.HONEY_RESERVOIR))   // Ouest - fuel
            .add(0, -1, 0, block(ApicaBlocks.IRON_FOUNDATION))      // Centre
            .add(1, -1, 0, block(ApicaBlocks.HONEY_RESERVOIR))    // Est - fuel
            .add(-1, -1, 1, block(ApicaBlocks.IRON_FOUNDATION))     // Coin SO
            .add(0, -1, 1, block(ApicaBlocks.HONEY_RESERVOIR))    // Sud - fuel
            .add(1, -1, 1, block(ApicaBlocks.IRON_FOUNDATION))      // Coin SE

            // Étage 2 (Y+0): Air autour du coeur
            .add(-1, 0, -1, air())
            .add(0, 0, -1, air())
            .add(1, 0, -1, air())
            .add(-1, 0, 0, air())
            // (0, 0, 0) = Centrifuge Heart - skip
            .add(1, 0, 0, air())
            .add(-1, 0, 1, air())
            .add(0, 0, 1, air())
            .add(1, 0, 1, air())

            // Étage 3 (Y+1): Honeyed Stone coins + centre, Reservoirs cardinaux
            .add(-1, 1, -1, block(ApicaBlocks.IRON_FOUNDATION))     // Coin NO
            .add(0, 1, -1, block(ApicaBlocks.HONEY_RESERVOIR))    // Nord - output
            .add(1, 1, -1, block(ApicaBlocks.IRON_FOUNDATION))      // Coin NE
            .add(-1, 1, 0, block(ApicaBlocks.HONEY_RESERVOIR))    // Ouest - output
            .add(0, 1, 0, block(ApicaBlocks.IRON_FOUNDATION))       // Centre
            .add(1, 1, 0, block(ApicaBlocks.HONEY_RESERVOIR))     // Est - output
            .add(-1, 1, 1, block(ApicaBlocks.IRON_FOUNDATION))      // Coin SO
            .add(0, 1, 1, block(ApicaBlocks.HONEY_RESERVOIR))     // Sud - output
            .add(1, 1, 1, block(ApicaBlocks.IRON_FOUNDATION))       // Coin SE

            .build()
    );

    // ==================== MULTIBLOCK TANK ====================
    // Structure 2x2x2 de tank_placeholder (prévisualisation GUI uniquement)
    //
    // Étage 2 (Y+0): [T][T]
    //                 [T][T]
    //
    // Étage 1 (Y-1): [T][T]
    //                 [T][T]

    public static final MultiblockPattern TANK_MULTIBLOCK = register(
        MultiblockPattern.builder("tank_multiblock")
            // Étage 1 (Y-1)
            .add(0, -1, 0, block(ApicaBlocks.TANK_PLACEHOLDER))
            .add(1, -1, 0, block(ApicaBlocks.TANK_PLACEHOLDER))
            .add(0, -1, 1, block(ApicaBlocks.TANK_PLACEHOLDER))
            .add(1, -1, 1, block(ApicaBlocks.TANK_PLACEHOLDER))

            // Étage 2 (Y+0) — controller au (0,0,0)
            .add(1, 0, 0, block(ApicaBlocks.TANK_PLACEHOLDER))
            .add(0, 0, 1, block(ApicaBlocks.TANK_PLACEHOLDER))
            .add(1, 0, 1, block(ApicaBlocks.TANK_PLACEHOLDER))

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
