/**
 * ============================================================
 * [CodexPageRenderer.java]
 * Description: Interface pour les renderers de pages du Codex
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexNode           | Donnees des nodes    | Affichage et interaction       |
 * | CodexPlayerData     | Progression joueur   | Etat des deblocages            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexScreen (delegation du rendu par page)
 * - BeeTreePageRenderer
 * - StandardPageRenderer
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.codex;

import com.chapeau.beemancer.common.codex.CodexNode;
import com.chapeau.beemancer.common.codex.CodexPlayerData;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.Set;

public interface CodexPageRenderer {

    /**
     * Reconstruit les widgets pour la page
     * @param completedQuests Set des IDs de quêtes complétées
     */
    void rebuildWidgets(List<CodexNode> nodes, Set<String> unlockedNodes, CodexPlayerData playerData,
                        Set<String> completedQuests,
                        int contentX, int contentY, int nodeSpacing, double scrollX, double scrollY);

    /**
     * Met a jour les positions des widgets lors du scroll
     */
    void updatePositions(int contentX, int contentY, int nodeSpacing, double scrollX, double scrollY);

    /**
     * Rend les connexions entre les nodes
     */
    void renderConnections(GuiGraphics graphics);

    /**
     * Rend les tooltips des widgets
     */
    void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY);

    /**
     * Gere le clic souris sur les widgets
     * @return true si un widget a ete clique
     */
    boolean handleClick(double mouseX, double mouseY, NodeUnlockCallback callback);

    /**
     * Nettoie les widgets (avant changement de page)
     */
    void clearWidgets();

    /**
     * Retourne les widgets pour l'ajout au screen
     */
    List<?> getWidgets();

    /**
     * Callback pour debloquer un node
     */
    @FunctionalInterface
    interface NodeUnlockCallback {
        void unlock(CodexNode node, boolean isUnlocked, boolean canUnlock);
    }
}
