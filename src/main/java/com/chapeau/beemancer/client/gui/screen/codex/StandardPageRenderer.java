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
import com.chapeau.beemancer.common.codex.*;
import net.minecraft.client.gui.GuiGraphics;

import java.util.*;

public class StandardPageRenderer implements CodexPageRenderer {

    private final List<CodexNodeWidget> widgets = new ArrayList<>();
    private final Map<String, int[]> nodePositions = new HashMap<>();
    private CodexJsonLoader.TabData currentTabData = null;

    // Couleurs des lignes
    private static final int LINE_COLOR_UNLOCKED = 0xFF805030;   // Marron/sépia pour effet encre
    private static final int LINE_COLOR_LOCKED = 0xFF504030;     // Gris-marron pour verrouillé

    @Override
    public void rebuildWidgets(List<CodexNode> nodes, Set<String> unlockedNodes, CodexPlayerData playerData,
                               int contentX, int contentY, int nodeSpacing, double scrollX, double scrollY) {
        widgets.clear();
        nodePositions.clear();

        // Récupérer les données du tab pour les liens
        if (!nodes.isEmpty()) {
            currentTabData = CodexJsonLoader.getTabData(nodes.get(0).getPage());
        }

        int halfNode = CodexNodeWidget.NODE_SIZE / 2;

        for (CodexNode node : nodes) {
            // Vérifier si c'est un node header (débloqué par défaut)
            boolean isHeader = CodexPlayerData.isDefaultNode(node.getFullId());

            boolean unlocked = playerData.isUnlocked(node) || isHeader;
            boolean canUnlock = CodexManager.canUnlock(node, unlockedNodes);

            // Centrer le node sur le point calculé
            int nodeScreenX = contentX + node.getX() * nodeSpacing + (int) scrollX - halfNode;
            int nodeScreenY = contentY + node.getY() * nodeSpacing + (int) scrollY - halfNode;

            CodexNodeWidget widget = new CodexNodeWidget(node, nodeScreenX, nodeScreenY, unlocked, canUnlock, isHeader);
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
                    renderHandDrawnArrow(graphics, parentWidget, widget);
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

                renderHandDrawnArrow(graphics, fromWidget, toWidget);
            }
        }
    }

    /**
     * Dessine une ligne droite style manuscrit avec flèche.
     */
    private void renderHandDrawnArrow(GuiGraphics graphics, CodexNodeWidget fromWidget, CodexNodeWidget toWidget) {
        int nodeSize = CodexNodeWidget.NODE_SIZE;

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
        int arrowOffset = offset + 6; // La flèche est un peu avant le node

        int lineStartX = (int) (startX + nx * offset);
        int lineStartY = (int) (startY + ny * offset);
        int lineEndX = (int) (endX - nx * arrowOffset);
        int lineEndY = (int) (endY - ny * arrowOffset);

        // Couleur basée sur l'état de déblocage
        int lineColor = (fromWidget.isUnlocked() && toWidget.isUnlocked())
            ? LINE_COLOR_UNLOCKED
            : LINE_COLOR_LOCKED;

        // Dessiner la ligne principale avec effet manuscrit (léger tremblé)
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

        // Normaliser
        float nx = dx / length;
        float ny = dy / length;

        // Perpendiculaire pour le tremblé
        float px = -ny;
        float py = nx;

        // Nombre de segments pour l'effet manuscrit
        int segments = Math.max(3, (int) (length / 8));

        // Seed basé sur les coordonnées pour un tremblé consistant
        long seed = (long) x1 * 31 + y1 * 17 + x2 * 13 + y2 * 7;
        Random rand = new Random(seed);

        int prevX = x1;
        int prevY = y1;

        for (int i = 1; i <= segments; i++) {
            float t = (float) i / segments;
            int baseX = (int) (x1 + dx * t);
            int baseY = (int) (y1 + dy * t);

            // Ajouter un léger tremblé perpendiculaire (sauf aux extrémités)
            int wobble = 0;
            if (i > 0 && i < segments) {
                wobble = (int) ((rand.nextFloat() - 0.5f) * 3);
            }

            int curX = (int) (baseX + px * wobble);
            int curY = (int) (baseY + py * wobble);

            // Dessiner le segment (ligne de 2px d'épaisseur)
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
            // Point unique
            graphics.fill(x1 - thickness / 2, y1 - thickness / 2,
                         x1 + thickness / 2, y1 + thickness / 2, color);
            return;
        }

        // Perpendiculaire normalisée
        float px = -dy / length;
        float py = dx / length;

        // Dessiner plusieurs lignes parallèles pour l'épaisseur
        for (int t = -thickness / 2; t <= thickness / 2; t++) {
            int offsetX = (int) (px * t);
            int offsetY = (int) (py * t);

            // Bresenham-style drawing avec fill pour chaque point
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
        // Longueur et angle des branches de la flèche
        int arrowLength = 8;
        float arrowAngle = 0.5f; // ~30 degrés

        // Calculer les deux branches de la flèche
        float cos = (float) Math.cos(arrowAngle);
        float sin = (float) Math.sin(arrowAngle);

        // Branche gauche
        float leftDirX = dirX * cos + dirY * sin;
        float leftDirY = -dirX * sin + dirY * cos;
        int leftX = (int) (tipX - leftDirX * arrowLength);
        int leftY = (int) (tipY - leftDirY * arrowLength);

        // Branche droite
        float rightDirX = dirX * cos - dirY * sin;
        float rightDirY = dirX * sin + dirY * cos;
        int rightX = (int) (tipX - rightDirX * arrowLength);
        int rightY = (int) (tipY - rightDirY * arrowLength);

        // Dessiner les deux branches avec un léger effet manuscrit
        drawThickLine(graphics, tipX, tipY, leftX, leftY, color, 2);
        drawThickLine(graphics, tipX, tipY, rightX, rightY, color, 2);
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
