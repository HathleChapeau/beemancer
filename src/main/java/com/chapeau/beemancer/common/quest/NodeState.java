/**
 * ============================================================
 * [NodeState.java]
 * Description: État d'un node du Codex pour un joueur
 * ============================================================
 *
 * UTILISE PAR:
 * - CodexManager (calcul de l'état)
 * - CodexScreen (rendu)
 * - CodexNodeWidget (affichage)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.quest;

/**
 * État d'un node dans le Codex.
 * Calculé dynamiquement selon les données du joueur.
 */
public enum NodeState {
    /**
     * Node non disponible.
     * Parent pas débloqué ou quête pas complétée.
     * Non cliquable.
     */
    LOCKED,

    /**
     * Node disponible pour déblocage.
     * Quête complétée, le joueur peut cliquer pour débloquer.
     * Révèle le texte complet (retire les "???").
     */
    DISCOVERED,

    /**
     * Node débloqué par le joueur.
     * Affiche les nodes enfants.
     */
    UNLOCKED
}
