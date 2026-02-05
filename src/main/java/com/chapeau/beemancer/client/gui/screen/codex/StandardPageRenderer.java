/**
 * ============================================================
 * [StandardPageRenderer.java]
 * Description: Renderer pour les pages standard du Codex avec liens manuscrits
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexNodeWidget     | Widget standard      | Rendu des nodes                |
 * | CodexManager        | Visibilite nodes     | Filtrage des nodes visibles    |
 * | LineDrawingHelper   | Dessin lignes        | Rendu des connexions           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexScreen (pages APICA, ALCHEMY, ARTIFACTS, LOGISTICS)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.codex;

import com.chapeau.beemancer.client.gui.util.LineDrawingHelper;
import com.chapeau.beemancer.client.gui.util.LineDrawingHelper.LineStyle;
import com.chapeau.beemancer.client.gui.util.LineDrawingHelper.ArrowStyle;
import com.chapeau.beemancer.client.gui.widget.CodexNodeWidget;
import com.chapeau.beemancer.common.codex.*;
import net.minecraft.client.gui.GuiGraphics;

import java.util.*;

public class StandardPageRenderer implements CodexPageRenderer {

    private final List<CodexNodeWidget> widgets = new ArrayList<>();
    private final Map<String, int[]> nodePositions = new HashMap<>();

    // Couleurs des lignes - style encre/parchemin pour le codex
    private static final int LINE_COLOR_UNLOCKED = 0xFF5D4037;   // Marron chocolat
    private static final int LINE_COLOR_LOCKED = 0xFFA1887F;     // Marron gris clair

    @Override
    public void rebuildWidgets(List<CodexNode> nodes, Set<String> unlockedNodes, CodexPlayerData playerData,
                               int contentX, int contentY, int nodeSpacing, double scrollX, double scrollY) {
        widgets.clear();
        nodePositions.clear();

        int halfNode = CodexNodeWidget.NODE_SIZE / 2;

        for (CodexNode node : nodes) {
            if (!CodexManager.isVisible(node, unlockedNodes)) {
                continue;
            }

            boolean unlocked = playerData.isUnlocked(node);
            boolean canUnlock = CodexManager.canUnlock(node, unlockedNodes);

            // Centrer le node sur le point calculé
            int nodeScreenX = contentX + node.getX() * nodeSpacing + (int) scrollX - halfNode;
            int nodeScreenY = contentY + node.getY() * nodeSpacing + (int) scrollY - halfNode;

            CodexNodeWidget widget = new CodexNodeWidget(node, nodeScreenX, nodeScreenY, unlocked, canUnlock);
            widgets.add(widget);

            nodePositions.put(node.getId(), new int[]{nodeScreenX, nodeScreenY});
        }
    }

    @Override
    public void updatePositions(int contentX, int contentY, int nodeSpacing, double scrollX, double scrollY) {
        nodePositions.clear();
        int halfNode = CodexNodeWidget.NODE_SIZE / 2;

        for (CodexNodeWidget widget : widgets) {
            CodexNode node = widget.getNode();
            // Centrer le node sur le point calculé
            int nodeScreenX = contentX + node.getX() * nodeSpacing + (int) scrollX - halfNode;
            int nodeScreenY = contentY + node.getY() * nodeSpacing + (int) scrollY - halfNode;
            widget.setX(nodeScreenX);
            widget.setY(nodeScreenY);

            nodePositions.put(node.getId(), new int[]{nodeScreenX, nodeScreenY});
        }
    }

    @Override
    public void renderConnections(GuiGraphics graphics) {
        // Liens parent-enfant (même logique que BeeTreePageRenderer)
        for (CodexNodeWidget widget : widgets) {
            CodexNode node = widget.getNode();
            String parentId = node.getParentId();

            if (parentId != null) {
                CodexNodeWidget parentWidget = findWidgetByNodeId(parentId);
                if (parentWidget != null) {
                    renderConnection(graphics, parentWidget, widget);
                }
            }
        }
    }

    /**
     * Dessine une connexion entre deux nodes avec l'utilitaire LineDrawingHelper.
     */
    private void renderConnection(GuiGraphics graphics, CodexNodeWidget fromWidget, CodexNodeWidget toWidget) {
        int nodeSize = CodexNodeWidget.NODE_SIZE;

        // Centres des nodes
        int fromX = fromWidget.getX() + nodeSize / 2;
        int fromY = fromWidget.getY() + nodeSize / 2;
        int toX = toWidget.getX() + nodeSize / 2;
        int toY = toWidget.getY() + nodeSize / 2;

        // Direction
        float dx = toX - fromX;
        float dy = toY - fromY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return;

        float nx = dx / length;
        float ny = dy / length;

        // Points de départ/arrivée aux bords des nodes
        int offset = nodeSize / 2 + 3;
        int startX = (int) (fromX + nx * offset);
        int startY = (int) (fromY + ny * offset);
        int endX = (int) (toX - nx * (offset + 6)); // +6 pour la flèche
        int endY = (int) (toY - ny * (offset + 6));

        // Couleur et style selon état de déblocage
        boolean bothUnlocked = fromWidget.isUnlocked() && toWidget.isUnlocked();
        int color = bothUnlocked ? LINE_COLOR_UNLOCKED : LINE_COLOR_LOCKED;

        // Style simple pour performance optimale
        // STRAIGHT + SIMPLE = rendu rapide sans calculs complexes
        LineDrawingHelper.drawArrow(graphics,
                startX, startY, endX, endY,
                color, 2,
                LineStyle.STRAIGHT, ArrowStyle.SIMPLE,
                6, 0.4f, 0f);
    }

    @Override
    public void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        for (CodexNodeWidget widget : widgets) {
            widget.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, NodeUnlockCallback callback) {
        for (CodexNodeWidget widget : widgets) {
            if (widget.isMouseOver(mouseX, mouseY)) {
                callback.unlock(widget.getNode(), widget.isUnlocked(), widget.canUnlock());
                if (widget.canUnlock() && !widget.isUnlocked()) {
                    widget.setUnlocked(true);
                    return true;
                }
                return widget.isUnlocked();
            }
        }
        return false;
    }

    @Override
    public void clearWidgets() {
        widgets.clear();
        nodePositions.clear();
    }

    @Override
    public List<CodexNodeWidget> getWidgets() {
        return widgets;
    }

    private CodexNodeWidget findWidgetByNodeId(String nodeId) {
        for (CodexNodeWidget widget : widgets) {
            if (widget.getNode().getId().equals(nodeId)) {
                return widget;
            }
        }
        return null;
    }
}
