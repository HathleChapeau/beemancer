/**
 * ============================================================
 * [QuestType.java]
 * Description: Type de quête pour débloquer un node
 * ============================================================
 *
 * UTILISE PAR:
 * - Quest (définition)
 * - QuestManager (vérification)
 * - QuestEvents (détection)
 *
 * ============================================================
 */
package com.chapeau.apica.common.quest;

/**
 * Type de quête déterminant comment elle est complétée.
 */
public enum QuestType {
    /**
     * Obtenir un item dans l'inventaire.
     * Vérifié à l'ouverture du Codex.
     */
    OBTAIN,

    /**
     * Extraire un item du slot de sortie d'une machine.
     * Détecté via events quand le joueur prend l'item.
     */
    MACHINE_OUTPUT,

    /**
     * Récupérer une magic bee depuis un incubateur.
     * L'espèce doit correspondre au node.
     */
    BEE_INCUBATOR,

    /**
     * Insérer un item (par tag) dans le slot d'entrée d'une machine.
     * Détecté via onMachineInsert avec vérification de tag.
     */
    MACHINE_INSERT,

    /**
     * Ouvrir le menu d'une machine spécifique.
     * Détecté à l'ouverture du menu côté serveur.
     */
    OPEN_MENU;

    /**
     * Parse depuis une string JSON (case-insensitive).
     */
    public static QuestType fromString(String value) {
        if (value == null || value.isEmpty()) {
            return OBTAIN;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OBTAIN;
        }
    }
}
