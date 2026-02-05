/**
 * ============================================================
 * [BeeTreePageRenderer.java]
 * Description: Renderer pour la page BEES avec modeles 3D et liens manuscrits
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeNodeWidget       | Widget abeille 3D    | Rendu des nodes                |
 * | CodexManager        | Visibilite nodes     | Filtrage des nodes visibles    |
 * | LineDrawingHelper   | Dessin lignes        | Rendu des connexions           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexScreen (page BEES)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.codex;

import com.chapeau.beemancer.client.gui.util.LineDrawingHelper;
import com.chapeau.beemancer.client.gui.util.LineDrawingHelper.LineStyle;
import com.chapeau.beemancer.client.gui.util.LineDrawingHelper.ArrowStyle;
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

    // Couleurs des lignes pour les abeilles - theme miel/ambre
    private static final int LINE_COLOR_UNLOCKED = 0xFFE6A700;  // Ambre dore
    private static final int LINE_COLOR_LOCKED = 0xFF8D6E63;    // Marron chaud

    @Override
    public void rebuildWidgets(List<CodexNode> nodes, Set<String> unlockedNodes, CodexPlayerData playerData,
                               int contentX, int contentY, int nodeSpacing, double scrollX, double scrollY) {
        widgets.clear();
        int halfNode = BeeNodeWidget.NODE_SIZE / 2;

        for (CodexNode node : nodes) {
            if (!CodexManager.isVisible(node, unlockedNodes)) {
                continue;
            }

            boolean unlocked = playerData.isUnlocked(node);
            boolean canUnlock = CodexManager.canUnlock(node, unlockedNodes);

            // Centrer le node sur le point calculé
            int nodeScreenX = contentX + node.getX() * nodeSpacing + (int) scrollX - halfNode;
            int nodeScreenY = contentY + node.getY() * nodeSpacing + (int) scrollY - halfNode;

            BeeNodeWidget widget = new BeeNodeWidget(node, nodeScreenX, nodeScreenY, unlocked, canUnlock);
            widgets.add(widget);
        }
    }

    @Override
    public void updatePositions(int contentX, int contentY, int nodeSpacing, double scrollX, double scrollY) {
        int halfNode = BeeNodeWidget.NODE_SIZE / 2;

        for (BeeNodeWidget widget : widgets) {
            CodexNode node = widget.getNode();
            // Centrer le node sur le point calculé
            int nodeScreenX = contentX + node.getX() * nodeSpacing + (int) scrollX - halfNode;
            int nodeScreenY = contentY + node.getY() * nodeSpacing + (int) scrollY - halfNode;
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
                    renderConnection(graphics, parentWidget, widget);
                }
            }
        }
    }

    /**
     * Dessine une connexion entre deux nodes avec l'utilitaire LineDrawingHelper.
     */
    private void renderConnection(GuiGraphics graphics, BeeNodeWidget fromWidget, BeeNodeWidget toWidget) {
        int nodeSize = BeeNodeWidget.NODE_SIZE;

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
        int endX = (int) (toX - nx * (offset + 6));
        int endY = (int) (toY - ny * (offset + 6));

        // Couleur et style selon état de déblocage
        boolean bothUnlocked = fromWidget.isUnlocked() && toWidget.isUnlocked();
        int color = bothUnlocked ? LINE_COLOR_UNLOCKED : LINE_COLOR_LOCKED;

        // Style simple pour performance optimale
        LineDrawingHelper.drawArrow(graphics,
                startX, startY, endX, endY,
                color, 2,
                LineStyle.STRAIGHT, ArrowStyle.SIMPLE,
                6, 0.4f, 0f);
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
