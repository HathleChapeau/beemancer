/**
 * ============================================================
 * [NodeVisibility.java]
 * Description: Mode de visibilité d'un node du Codex
 * ============================================================
 *
 * UTILISE PAR:
 * - CodexNode (définition du mode)
 * - CodexManager (calcul de l'affichage)
 * - CodexScreen (rendu)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.quest;

/**
 * Mode de visibilité d'un node dans le Codex.
 * Défini dans le JSON du node.
 */
public enum NodeVisibility {
    /**
     * Visible si le node parent est débloqué.
     * Texte affiché normalement même si locked.
     */
    VISIBLE,

    /**
     * Visible si le node parent est débloqué.
     * Texte affiché comme "???" tant que pas discovered.
     */
    SECRET,

    /**
     * Invisible jusqu'à ce que le node soit discovered.
     * Révélé quand la quête associée est complétée.
     */
    HIDDEN;

    /**
     * Parse depuis une string JSON (case-insensitive).
     */
    public static NodeVisibility fromString(String value) {
        if (value == null || value.isEmpty()) {
            return VISIBLE;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return VISIBLE;
        }
    }
}
