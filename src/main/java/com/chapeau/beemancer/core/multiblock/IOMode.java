/**
 * ============================================================
 * [IOMode.java]
 * Description: Mode d'acces IO pour une face de bloc dans un multibloc
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | (aucune)            |                      |                       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BlockIORule (mode par face)
 * - MultiblockIOConfig (lookup)
 * - CentrifugeHeartBlockEntity, AlembicHeartBlockEntity (resolution handlers)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.multiblock;

/**
 * Definit le mode d'acces d'une face de bloc pour les capabilities fluid/item.
 * Utilise par les MultiblockIOConfig pour configurer declarativement
 * quelles faces acceptent l'insertion, l'extraction, les deux, ou rien.
 */
public enum IOMode {
    /** Face bloquee - aucune capability exposee */
    NONE,
    /** Insertion seulement (fill pour fluid, insertItem pour item) */
    INPUT,
    /** Extraction seulement (drain pour fluid, extractItem pour item) */
    OUTPUT,
    /** Les deux directions autorisees */
    BOTH;

    public boolean allowsInput() {
        return this == INPUT || this == BOTH;
    }

    public boolean allowsOutput() {
        return this == OUTPUT || this == BOTH;
    }
}
