/**
 * ============================================================
 * [StandardPageRenderer.java]
 * Description: Renderer pour les pages standard du Codex
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
import com.chapeau.beemancer.client.gui.util.LineDrawingHelper.ConnectionMode;
import com.chapeau.beemancer.client.gui.widget.CodexNodeWidget;
import com.chapeau.beemancer.common.codex.*;
import com.chapeau.beemancer.common.quest.NodeState;
import net.minecraft.client.gui.GuiGraphics;

import java.util.*;

public class StandardPageRenderer implements CodexPageRenderer {

    private final List<CodexNodeWidget> widgets = new ArrayList<>();
    private final Map<String, int[]> nodePositions = new HashMap<>();

    @Override
    public void rebuildWidgets(List<CodexNode> nodes, Set<String> unlockedNodes, CodexPlayerData playerData,
                               Set<String> completedQuests,
                               int contentX, int contentY, int nodeSpacing, double scrollX, double scrollY) {
        widgets.clear();
        nodePositions.clear();

        int halfNode = CodexNodeWidget.NODE_SIZE / 2;

        for (CodexNode node : nodes) {
            // Calculer l'état du node
            NodeState state = CodexManager.getNodeState(node, playerData, completedQuests);

            // Vérifier la visibilité avec le nouveau système
            if (!CodexManager.isNodeVisible(node, state, playerData)) {
                continue;
            }

            boolean unlocked = (state == NodeState.UNLOCKED);
            // canUnlock = DISCOVERED (quête complétée, parent débloqué)
            boolean canUnlock = (state == NodeState.DISCOVERED);

            int nodeScreenX = contentX + node.getX() * nodeSpacing + (int) scrollX - halfNode;
            int nodeScreenY = contentY + node.getY() * nodeSpacing + (int) scrollY - halfNode;

            CodexNodeWidget widget = new CodexNodeWidget(node, nodeScreenX, nodeScreenY, unlocked, canUnlock, state);
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
            int nodeScreenX = contentX + node.getX() * nodeSpacing + (int) scrollX - halfNode;
            int nodeScreenY = contentY + node.getY() * nodeSpacing + (int) scrollY - halfNode;
            widget.setX(nodeScreenX);
            widget.setY(nodeScreenY);

            nodePositions.put(node.getId(), new int[]{nodeScreenX, nodeScreenY});
        }
    }

    @Override
    public void renderConnections(GuiGraphics graphics) {
        // Premier pass: connexions non debloquees (gris)
        for (CodexNodeWidget widget : widgets) {
            CodexNode node = widget.getNode();
            String parentId = node.getParentId();

            if (parentId != null) {
                CodexNodeWidget parentWidget = findWidgetByNodeId(parentId);
                if (parentWidget != null) {
                    boolean bothUnlocked = parentWidget.isUnlocked() && widget.isUnlocked();
                    if (!bothUnlocked) {
                        renderConnection(graphics, parentWidget, widget, false);
                    }
                }
            }
        }

        // Deuxieme pass: connexions debloquees (ambre) - dessinees par-dessus
        for (CodexNodeWidget widget : widgets) {
            CodexNode node = widget.getNode();
            String parentId = node.getParentId();

            if (parentId != null) {
                CodexNodeWidget parentWidget = findWidgetByNodeId(parentId);
                if (parentWidget != null) {
                    boolean bothUnlocked = parentWidget.isUnlocked() && widget.isUnlocked();
                    if (bothUnlocked) {
                        renderConnection(graphics, parentWidget, widget, true);
                    }
                }
            }
        }
    }

    /**
     * Dessine une connexion en 3 segments entre deux nodes.
     */
    private void renderConnection(GuiGraphics graphics, CodexNodeWidget fromWidget, CodexNodeWidget toWidget, boolean unlocked) {
        int nodeSize = CodexNodeWidget.NODE_SIZE;

        int fromX = fromWidget.getX() + nodeSize / 2;
        int fromY = fromWidget.getY() + nodeSize / 2;
        int toX = toWidget.getX() + nodeSize / 2;
        int toY = toWidget.getY() + nodeSize / 2;

        int dx = Math.abs(toX - fromX);
        int dy = Math.abs(toY - fromY);

        ConnectionMode mode = (dx >= dy) ? ConnectionMode.HORIZONTAL : ConnectionMode.VERTICAL;
        float turnPercent = 0.5f;

        LineDrawingHelper.drawConnection(graphics, fromX, fromY, toX, toY, mode, turnPercent, unlocked);
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
