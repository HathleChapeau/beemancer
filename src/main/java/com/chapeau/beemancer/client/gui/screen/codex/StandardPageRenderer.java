/**
 * ============================================================
 * [StandardPageRenderer.java]
 * Description: Renderer pour les pages standard du Codex avec noms de nodes
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexNodeWidget     | Widget standard      | Rendu des nodes                |
 * | CodexManager        | Visibilite nodes     | Filtrage des nodes visibles    |
 * | CodexJsonLoader     | Donnees JSON         | Liens entre nodes              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexScreen (pages APICA, ALCHEMY, ARTIFACTS, LOGISTICS)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.codex;

import com.chapeau.beemancer.client.gui.widget.CodexNodeWidget;
import com.chapeau.beemancer.common.codex.CodexJsonLoader;
import com.chapeau.beemancer.common.codex.CodexManager;
import com.chapeau.beemancer.common.codex.CodexNode;
import com.chapeau.beemancer.common.codex.CodexPlayerData;
import net.minecraft.client.gui.GuiGraphics;

import java.util.*;

public class StandardPageRenderer implements CodexPageRenderer {

    private final List<CodexNodeWidget> widgets = new ArrayList<>();
    private final Map<String, int[]> nodePositions = new HashMap<>();
    private CodexJsonLoader.TabData currentTabData = null;

    @Override
    public void rebuildWidgets(List<CodexNode> nodes, Set<String> unlockedNodes, CodexPlayerData playerData,
                               int contentX, int contentY, int nodeSpacing, double scrollX, double scrollY) {
        widgets.clear();
        nodePositions.clear();

        // Récupérer les données du tab pour les liens
        if (!nodes.isEmpty()) {
            currentTabData = CodexJsonLoader.getTabData(nodes.get(0).getPage());
        }

        for (CodexNode node : nodes) {
            boolean unlocked = playerData.isUnlocked(node);
            boolean canUnlock = CodexManager.canUnlock(node, unlockedNodes);

            int nodeScreenX = contentX + node.getX() * nodeSpacing + (int) scrollX;
            int nodeScreenY = contentY + node.getY() * nodeSpacing + (int) scrollY;

            CodexNodeWidget widget = new CodexNodeWidget(node, nodeScreenX, nodeScreenY, unlocked, canUnlock);
            widgets.add(widget);

            // Stocker la position pour le rendu des liens
            nodePositions.put(node.getId(), new int[]{nodeScreenX, nodeScreenY});
        }
    }

    @Override
    public void updatePositions(int contentX, int contentY, int nodeSpacing, double scrollX, double scrollY) {
        nodePositions.clear();

        for (CodexNodeWidget widget : widgets) {
            CodexNode node = widget.getNode();
            int nodeScreenX = contentX + node.getX() * nodeSpacing + (int) scrollX;
            int nodeScreenY = contentY + node.getY() * nodeSpacing + (int) scrollY;
            widget.setX(nodeScreenX);
            widget.setY(nodeScreenY);

            nodePositions.put(node.getId(), new int[]{nodeScreenX, nodeScreenY});
        }
    }

    @Override
    public void renderConnections(GuiGraphics graphics) {
        // Utiliser les liens du JSON si disponibles
        if (currentTabData != null && currentTabData.links != null) {
            renderJsonLinks(graphics);
            return;
        }

        // Fallback: liens parent-enfant classiques
        for (CodexNodeWidget widget : widgets) {
            CodexNode node = widget.getNode();
            String parentId = node.getParentId();

            if (parentId != null) {
                CodexNodeWidget parentWidget = findWidgetByNodeId(parentId);
                if (parentWidget != null) {
                    int startX = parentWidget.getX() + CodexNodeWidget.NODE_WIDTH / 2;
                    int startY = parentWidget.getY() + CodexNodeWidget.NODE_HEIGHT / 2;
                    int endX = widget.getX() + CodexNodeWidget.NODE_WIDTH / 2;
                    int endY = widget.getY() + CodexNodeWidget.NODE_HEIGHT / 2;

                    int lineColor = widget.isUnlocked() ? 0xFF00FF00 : 0xFF666666;

                    int midX = (startX + endX) / 2;
                    graphics.fill(startX, startY - 1, midX, startY + 1, lineColor);
                    graphics.fill(midX - 1, startY, midX + 1, endY, lineColor);
                    graphics.fill(midX, endY - 1, endX, endY + 1, lineColor);
                }
            }
        }
    }

    private void renderJsonLinks(GuiGraphics graphics) {
        Map<String, List<String>> links = currentTabData.links;
        if (links == null) return;

        for (Map.Entry<String, List<String>> entry : links.entrySet()) {
            String fromName = entry.getKey();
            List<String> toNames = entry.getValue();

            CodexNodeWidget fromWidget = findWidgetByName(fromName);
            if (fromWidget == null) continue;

            for (String toName : toNames) {
                CodexNodeWidget toWidget = findWidgetByName(toName);
                if (toWidget == null) continue;

                int startX = fromWidget.getX() + CodexNodeWidget.NODE_WIDTH / 2;
                int startY = fromWidget.getY() + CodexNodeWidget.NODE_HEIGHT / 2;
                int endX = toWidget.getX() + CodexNodeWidget.NODE_WIDTH / 2;
                int endY = toWidget.getY() + CodexNodeWidget.NODE_HEIGHT / 2;

                // Couleur basée sur l'état de déblocage
                int lineColor = (fromWidget.isUnlocked() && toWidget.isUnlocked())
                    ? 0xFFF1C40F  // Doré si les deux sont débloqués
                    : 0xFF666666; // Gris sinon

                // Dessiner une ligne en 3 segments (horizontal - vertical - horizontal)
                int midX = (startX + endX) / 2;
                int lineWidth = 2;

                // Segment horizontal depuis le parent
                graphics.fill(Math.min(startX, midX), startY - lineWidth / 2,
                        Math.max(startX, midX), startY + lineWidth / 2, lineColor);
                // Segment vertical
                graphics.fill(midX - lineWidth / 2, Math.min(startY, endY),
                        midX + lineWidth / 2, Math.max(startY, endY), lineColor);
                // Segment horizontal vers l'enfant
                graphics.fill(Math.min(midX, endX), endY - lineWidth / 2,
                        Math.max(midX, endX), endY + lineWidth / 2, lineColor);
            }
        }
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

    private CodexNodeWidget findWidgetByName(String name) {
        String normalizedName = name.toLowerCase().replace(" ", "_");
        for (CodexNodeWidget widget : widgets) {
            if (widget.getNode().getId().equals(normalizedName)) {
                return widget;
            }
        }
        return null;
    }
}
