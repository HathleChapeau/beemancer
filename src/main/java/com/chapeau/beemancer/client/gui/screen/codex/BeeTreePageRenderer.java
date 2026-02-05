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
import java.util.Random;
import java.util.Set;

public class BeeTreePageRenderer implements CodexPageRenderer {

    private final List<BeeNodeWidget> widgets = new ArrayList<>();

    // Couleurs des lignes pour les abeilles
    private static final int LINE_COLOR_UNLOCKED = 0xFFFFAA00;  // Doré/miel
    private static final int LINE_COLOR_LOCKED = 0xFF555555;    // Gris

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
                    renderHandDrawnArrow(graphics, parentWidget, widget);
                }
            }
        }
    }

    /**
     * Dessine une ligne droite style manuscrit avec flèche.
     */
    private void renderHandDrawnArrow(GuiGraphics graphics, BeeNodeWidget fromWidget, BeeNodeWidget toWidget) {
        int nodeSize = BeeNodeWidget.NODE_SIZE;

        // Points de départ et d'arrivée (centres des nodes)
        int startX = fromWidget.getX() + nodeSize / 2;
        int startY = fromWidget.getY() + nodeSize / 2;
        int endX = toWidget.getX() + nodeSize / 2;
        int endY = toWidget.getY() + nodeSize / 2;

        // Décaler les points pour partir/arriver au bord des nodes
        float dx = endX - startX;
        float dy = endY - startY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return;

        float nx = dx / length;
        float ny = dy / length;

        // Décaler depuis le bord du node (rayon = nodeSize/2)
        int offset = nodeSize / 2 + 2;
        int arrowOffset = offset + 6;

        int lineStartX = (int) (startX + nx * offset);
        int lineStartY = (int) (startY + ny * offset);
        int lineEndX = (int) (endX - nx * arrowOffset);
        int lineEndY = (int) (endY - ny * arrowOffset);

        // Couleur basée sur l'état de déblocage
        int lineColor = (fromWidget.isUnlocked() && toWidget.isUnlocked())
            ? LINE_COLOR_UNLOCKED
            : LINE_COLOR_LOCKED;

        // Dessiner la ligne principale avec effet manuscrit
        drawHandDrawnLine(graphics, lineStartX, lineStartY, lineEndX, lineEndY, lineColor);

        // Dessiner la flèche au bout
        drawArrowHead(graphics, lineEndX, lineEndY, nx, ny, lineColor);
    }

    /**
     * Dessine une ligne avec un léger effet manuscrit/tremblé.
     */
    private void drawHandDrawnLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return;

        float nx = dx / length;
        float ny = dy / length;
        float px = -ny;
        float py = nx;

        int segments = Math.max(3, (int) (length / 8));
        long seed = (long) x1 * 31 + y1 * 17 + x2 * 13 + y2 * 7;
        Random rand = new Random(seed);

        int prevX = x1;
        int prevY = y1;

        for (int i = 1; i <= segments; i++) {
            float t = (float) i / segments;
            int baseX = (int) (x1 + dx * t);
            int baseY = (int) (y1 + dy * t);

            int wobble = 0;
            if (i > 0 && i < segments) {
                wobble = (int) ((rand.nextFloat() - 0.5f) * 3);
            }

            int curX = (int) (baseX + px * wobble);
            int curY = (int) (baseY + py * wobble);

            drawThickLine(graphics, prevX, prevY, curX, curY, color, 2);

            prevX = curX;
            prevY = curY;
        }
    }

    /**
     * Dessine une ligne épaisse entre deux points.
     */
    private void drawThickLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color, int thickness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length < 0.5f) {
            graphics.fill(x1 - thickness / 2, y1 - thickness / 2,
                         x1 + thickness / 2, y1 + thickness / 2, color);
            return;
        }

        float px = -dy / length;
        float py = dx / length;

        for (int t = -thickness / 2; t <= thickness / 2; t++) {
            int offsetX = (int) (px * t);
            int offsetY = (int) (py * t);

            int steps = (int) Math.max(Math.abs(dx), Math.abs(dy));
            if (steps == 0) steps = 1;

            for (int s = 0; s <= steps; s++) {
                float progress = (float) s / steps;
                int drawX = (int) (x1 + dx * progress + offsetX);
                int drawY = (int) (y1 + dy * progress + offsetY);
                graphics.fill(drawX, drawY, drawX + 1, drawY + 1, color);
            }
        }
    }

    /**
     * Dessine une tête de flèche manuscrite.
     */
    private void drawArrowHead(GuiGraphics graphics, int tipX, int tipY, float dirX, float dirY, int color) {
        int arrowLength = 8;
        float arrowAngle = 0.5f;

        float cos = (float) Math.cos(arrowAngle);
        float sin = (float) Math.sin(arrowAngle);

        float leftDirX = dirX * cos + dirY * sin;
        float leftDirY = -dirX * sin + dirY * cos;
        int leftX = (int) (tipX - leftDirX * arrowLength);
        int leftY = (int) (tipY - leftDirY * arrowLength);

        float rightDirX = dirX * cos - dirY * sin;
        float rightDirY = dirX * sin + dirY * cos;
        int rightX = (int) (tipX - rightDirX * arrowLength);
        int rightY = (int) (tipY - rightDirY * arrowLength);

        drawThickLine(graphics, tipX, tipY, leftX, leftY, color, 2);
        drawThickLine(graphics, tipX, tipY, rightX, rightY, color, 2);
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
