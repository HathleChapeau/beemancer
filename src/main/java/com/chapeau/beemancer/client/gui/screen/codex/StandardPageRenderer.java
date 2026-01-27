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
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexScreen (pages BEE, ALCHEMY, LOGISTICS)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.codex;

import com.chapeau.beemancer.client.gui.widget.CodexNodeWidget;
import com.chapeau.beemancer.common.codex.CodexManager;
import com.chapeau.beemancer.common.codex.CodexNode;
import com.chapeau.beemancer.common.codex.CodexPlayerData;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StandardPageRenderer implements CodexPageRenderer {

    private final List<CodexNodeWidget> widgets = new ArrayList<>();

    @Override
    public void rebuildWidgets(List<CodexNode> nodes, Set<String> unlockedNodes, CodexPlayerData playerData,
                               int contentX, int contentY, int nodeSpacing, double scrollX, double scrollY) {
        widgets.clear();

        for (CodexNode node : nodes) {
            if (!CodexManager.isVisible(node, unlockedNodes)) {
                continue;
            }

            boolean unlocked = playerData.isUnlocked(node);
            boolean canUnlock = CodexManager.canUnlock(node, unlockedNodes);

            int nodeScreenX = contentX + node.getX() * nodeSpacing + (int) scrollX;
            int nodeScreenY = contentY + node.getY() * nodeSpacing + (int) scrollY;

            CodexNodeWidget widget = new CodexNodeWidget(node, nodeScreenX, nodeScreenY, unlocked, canUnlock);
            widgets.add(widget);
        }
    }

    @Override
    public void updatePositions(int contentX, int contentY, int nodeSpacing, double scrollX, double scrollY) {
        for (CodexNodeWidget widget : widgets) {
            CodexNode node = widget.getNode();
            int nodeScreenX = contentX + node.getX() * nodeSpacing + (int) scrollX;
            int nodeScreenY = contentY + node.getY() * nodeSpacing + (int) scrollY;
            widget.setX(nodeScreenX);
            widget.setY(nodeScreenY);
        }
    }

    @Override
    public void renderConnections(GuiGraphics graphics) {
        for (CodexNodeWidget widget : widgets) {
            CodexNode node = widget.getNode();
            String parentId = node.getParentId();

            if (parentId != null) {
                CodexNodeWidget parentWidget = findWidgetByNodeId(parentId);
                if (parentWidget != null) {
                    int startX = parentWidget.getX() + CodexNodeWidget.NODE_SIZE / 2;
                    int startY = parentWidget.getY() + CodexNodeWidget.NODE_SIZE / 2;
                    int endX = widget.getX() + CodexNodeWidget.NODE_SIZE / 2;
                    int endY = widget.getY() + CodexNodeWidget.NODE_SIZE / 2;

                    int lineColor = widget.isUnlocked() ? 0xFF00FF00 : 0xFF666666;

                    int midX = (startX + endX) / 2;
                    graphics.fill(startX, startY - 1, midX, startY + 1, lineColor);
                    graphics.fill(midX - 1, startY, midX + 1, endY, lineColor);
                    graphics.fill(midX, endY - 1, endX, endY + 1, lineColor);
                }
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
