/**
 * ============================================================
 * [HiveConfig.java]
 * Description: Configuration parametree par type de ruche
 * ============================================================
 *
 * DEPENDANCES: aucune
 *
 * UTILISE PAR:
 * - HiveBeeLifecycleManager.java (parametrage spawn, search, entry)
 * - MagicHiveBlockEntity.java (MAGIC_HIVE constant)
 * - HiveMultiblockBlockEntity.java (MULTIBLOCK constant)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.hive;

/**
 * Parametres qui different entre MagicHive et HiveMultiblock.
 * Utilise par HiveBeeLifecycleManager pour adapter son comportement.
 */
public record HiveConfig(
    int spawnHeightOffset,
    double searchInflateX,
    double searchInflateY,
    double searchInflateZ,
    double entryDistance
) {
    /** Config pour la ruche simple (1 bloc). */
    public static final HiveConfig MAGIC_HIVE = new HiveConfig(1, 2, 2, 2, 1.5);

    /** Config pour la ruche multibloc (3x3x3 + slabs). */
    public static final HiveConfig MULTIBLOCK = new HiveConfig(4, 4, 5, 4, 3.0);
}
