/**
 * ============================================================
 * [BeeTreePageRenderer.java]
 * Description: Renderer pour la page BEES avec modeles 3D
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeNodeWidget       | Widget abeille 3D    | Rendu des nodes                |
 * | CodexManager        | Visibilite nodes     | Filtrage des nodes visibles    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexScreen (page BEES)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.codex;

import com.chapeau.beemancer.client.gui.widget.BeeNodeWidget;
import com.chapeau.beemancer.common.codex.CodexManager;
import com.chapeau.beemancer.common.codex.CodexNode;
import com.chapeau.beemancer.common.codex.CodexPlayerData;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BeeTreePageRenderer implements CodexPageRenderer {

    private final List<BeeNodeWidget> widgets = new ArrayList<>();

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

            BeeNodeWidget widget = new BeeNodeWidget(node, nodeScreenX, nodeScreenY, unlocked, canUnlock);
            widgets.add(widget);
        }
    }

    @Override
    public void updatePositions(int contentX, int contentY, int nodeSpacing, double scrollX, double scrollY) {
        for (BeeNodeWidget widget : widgets) {
            CodexNode node = widget.getNode();
            int nodeScreenX = contentX + node.getX() * nodeSpacing + (int) scrollX;
            int nodeScreenY = contentY + node.getY() * nodeSpacing + (int) scrollY;
            widget.setX(nodeScreenX);
            widget.setY(nodeScreenY);
        }
    }

    @Override
    public void renderConnections(GuiGraphics graphics) {
        for (BeeNodeWidget widget : widgets) {
            CodexNode node = widget.getNode();
            String parentId = node.getParentId();

            if (parentId != null) {
                BeeNodeWidget parentWidget = findWidgetByNodeId(parentId);
                if (parentWidget != null) {
                    int startX = parentWidget.getX() + BeeNodeWidget.NODE_SIZE / 2;
                    int startY = parentWidget.getY() + BeeNodeWidget.NODE_SIZE / 2;
                    int endX = widget.getX() + BeeNodeWidget.NODE_SIZE / 2;
                    int endY = widget.getY() + BeeNodeWidget.NODE_SIZE / 2;

                    // Golden connections for bees
                    int lineColor = widget.isUnlocked() ? 0xFFFFAA00 : 0xFF555555;
                    int lineWidth = 2;

                    int midX = (startX + endX) / 2;

                    // Horizontal from parent
                    graphics.fill(Math.min(startX, midX), startY - lineWidth / 2,
                            Math.max(startX, midX), startY + lineWidth / 2, lineColor);
                    // Vertical connector
                    graphics.fill(midX - lineWidth / 2, Math.min(startY, endY),
                            midX + lineWidth / 2, Math.max(startY, endY), lineColor);
                    // Horizontal to child
                    graphics.fill(Math.min(midX, endX), endY - lineWidth / 2,
                            Math.max(midX, endX), endY + lineWidth / 2, lineColor);
                }
            }
        }
    }

    @Override
    public void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        for (BeeNodeWidget widget : widgets) {
            widget.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, NodeUnlockCallback callback) {
        for (BeeNodeWidget widget : widgets) {
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
    public List<BeeNodeWidget> getWidgets() {
        return widgets;
    }

    private BeeNodeWidget findWidgetByNodeId(String nodeId) {
        for (BeeNodeWidget widget : widgets) {
            if (widget.getNode().getId().equals(nodeId)) {
                return widget;
            }
        }
        return null;
    }
}
