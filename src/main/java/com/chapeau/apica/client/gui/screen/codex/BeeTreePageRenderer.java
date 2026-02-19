/**
 * ============================================================
 * [BeeTreePageRenderer.java]
 * Description: Renderer pour la page BEES avec modeles 3D et nodes outils
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeNodeWidget       | Widget abeille 3D    | Rendu des nodes abeilles       |
 * | CodexNodeWidget     | Widget item 2D       | Rendu des nodes outils         |
 * | CodexManager        | Visibilite nodes     | Filtrage des nodes visibles    |
 * | LineDrawingHelper   | Dessin lignes        | Rendu des connexions           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexScreen (page BEES)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen.codex;

import com.chapeau.apica.client.gui.util.LineDrawingHelper;
import com.chapeau.apica.client.gui.util.LineDrawingHelper.ConnectionMode;
import com.chapeau.apica.client.gui.widget.BeeNodeWidget;
import com.chapeau.apica.client.gui.widget.CodexNodeWidget;
import com.chapeau.apica.common.codex.CodexManager;
import com.chapeau.apica.common.codex.CodexNode;
import com.chapeau.apica.common.codex.CodexPlayerData;
import com.chapeau.apica.common.quest.NodeState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BeeTreePageRenderer implements CodexPageRenderer {

    private final List<BeeNodeWidget> beeWidgets = new ArrayList<>();
    private final List<CodexNodeWidget> toolWidgets = new ArrayList<>();

    @Override
    public void rebuildWidgets(List<CodexNode> nodes, Set<String> unlockedNodes, CodexPlayerData playerData,
                               Set<String> completedQuests,
                               int contentX, int contentY, int nodeSpacing, double scrollX, double scrollY) {
        beeWidgets.clear();
        toolWidgets.clear();
        int halfNode = BeeNodeWidget.NODE_SIZE / 2;

        for (CodexNode node : nodes) {
            NodeState state = CodexManager.getNodeState(node, playerData, completedQuests);

            if (!CodexManager.isNodeVisible(node, state, playerData)) {
                continue;
            }

            boolean unlocked = (state == NodeState.UNLOCKED);
            boolean canUnlock = (state == NodeState.DISCOVERED);

            int nodeScreenX = contentX + node.getX() * nodeSpacing + (int) scrollX - halfNode;
            int nodeScreenY = contentY + node.getY() * nodeSpacing + (int) scrollY - halfNode;

            if (isBeeNode(node)) {
                BeeNodeWidget widget = new BeeNodeWidget(node, nodeScreenX, nodeScreenY, unlocked, canUnlock, state);
                beeWidgets.add(widget);
            } else {
                CodexNodeWidget widget = new CodexNodeWidget(node, nodeScreenX, nodeScreenY, unlocked, canUnlock, state);
                toolWidgets.add(widget);
            }
        }
    }

    @Override
    public void updatePositions(int contentX, int contentY, int nodeSpacing, double scrollX, double scrollY) {
        int halfNode = BeeNodeWidget.NODE_SIZE / 2;

        for (BeeNodeWidget widget : beeWidgets) {
            CodexNode node = widget.getNode();
            int nodeScreenX = contentX + node.getX() * nodeSpacing + (int) scrollX - halfNode;
            int nodeScreenY = contentY + node.getY() * nodeSpacing + (int) scrollY - halfNode;
            widget.setX(nodeScreenX);
            widget.setY(nodeScreenY);
        }

        for (CodexNodeWidget widget : toolWidgets) {
            CodexNode node = widget.getNode();
            int nodeScreenX = contentX + node.getX() * nodeSpacing + (int) scrollX - halfNode;
            int nodeScreenY = contentY + node.getY() * nodeSpacing + (int) scrollY - halfNode;
            widget.setX(nodeScreenX);
            widget.setY(nodeScreenY);
        }
    }

    @Override
    public void renderConnections(GuiGraphics graphics) {
        List<AbstractWidget> allWidgets = getAllWidgets();

        // Premier pass: connexions non debloquees (gris)
        for (AbstractWidget widget : allWidgets) {
            CodexNode node = getNodeFrom(widget);
            String parentId = node.getParentId();

            if (parentId != null) {
                AbstractWidget parentWidget = findWidgetByNodeId(parentId, allWidgets);
                if (parentWidget != null) {
                    boolean bothUnlocked = isWidgetUnlocked(parentWidget) && isWidgetUnlocked(widget);
                    if (!bothUnlocked) {
                        renderConnection(graphics, parentWidget, widget, node, false);
                    }
                }
            }
        }

        // Deuxieme pass: connexions debloquees (ambre) - dessinees par-dessus
        for (AbstractWidget widget : allWidgets) {
            CodexNode node = getNodeFrom(widget);
            String parentId = node.getParentId();

            if (parentId != null) {
                AbstractWidget parentWidget = findWidgetByNodeId(parentId, allWidgets);
                if (parentWidget != null) {
                    boolean bothUnlocked = isWidgetUnlocked(parentWidget) && isWidgetUnlocked(widget);
                    if (bothUnlocked) {
                        renderConnection(graphics, parentWidget, widget, node, true);
                    }
                }
            }
        }
    }

    private void renderConnection(GuiGraphics graphics, AbstractWidget fromWidget, AbstractWidget toWidget,
                                  CodexNode toNode, boolean unlocked) {
        int nodeSize = BeeNodeWidget.NODE_SIZE;

        int fromX = fromWidget.getX() + nodeSize / 2;
        int fromY = fromWidget.getY() + nodeSize / 2;
        int toX = toWidget.getX() + nodeSize / 2;
        int toY = toWidget.getY() + nodeSize / 2;

        int dx = Math.abs(toX - fromX);
        int dy = Math.abs(toY - fromY);

        String modeOverride = toNode.getConnectionMode();
        ConnectionMode mode;
        if ("HORIZONTAL".equalsIgnoreCase(modeOverride)) {
            mode = ConnectionMode.HORIZONTAL;
        } else if ("VERTICAL".equalsIgnoreCase(modeOverride)) {
            mode = ConnectionMode.VERTICAL;
        } else {
            mode = (dx > dy) ? ConnectionMode.HORIZONTAL : ConnectionMode.VERTICAL;
        }
        float turnPercent = 0.5f;

        LineDrawingHelper.drawConnection(graphics, fromX, fromY, toX, toY, mode, turnPercent, unlocked);
    }

    @Override
    public void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        for (BeeNodeWidget widget : beeWidgets) {
            widget.renderTooltip(graphics, mouseX, mouseY);
        }
        for (CodexNodeWidget widget : toolWidgets) {
            widget.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, NodeUnlockCallback callback) {
        for (BeeNodeWidget widget : beeWidgets) {
            if (widget.isMouseOver(mouseX, mouseY)) {
                callback.unlock(widget.getNode(), widget.isUnlocked(), widget.canUnlock());
                if (widget.canUnlock() && !widget.isUnlocked()) {
                    widget.setUnlocked(true);
                    return true;
                }
                return widget.isUnlocked();
            }
        }
        for (CodexNodeWidget widget : toolWidgets) {
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
        beeWidgets.clear();
        toolWidgets.clear();
    }

    @Override
    public List<?> getWidgets() {
        List<AbstractWidget> all = new ArrayList<>();
        all.addAll(beeWidgets);
        all.addAll(toolWidgets);
        return all;
    }

    // --- Helpers ---

    private static boolean isBeeNode(CodexNode node) {
        return node.getId().endsWith("_bee");
    }

    private List<AbstractWidget> getAllWidgets() {
        List<AbstractWidget> all = new ArrayList<>();
        all.addAll(beeWidgets);
        all.addAll(toolWidgets);
        return all;
    }

    private static CodexNode getNodeFrom(AbstractWidget widget) {
        if (widget instanceof BeeNodeWidget bee) return bee.getNode();
        if (widget instanceof CodexNodeWidget codex) return codex.getNode();
        throw new IllegalStateException("Unknown widget type");
    }

    private static boolean isWidgetUnlocked(AbstractWidget widget) {
        if (widget instanceof BeeNodeWidget bee) return bee.isUnlocked();
        if (widget instanceof CodexNodeWidget codex) return codex.isUnlocked();
        return false;
    }

    private static AbstractWidget findWidgetByNodeId(String nodeId, List<AbstractWidget> widgets) {
        for (AbstractWidget widget : widgets) {
            if (getNodeFrom(widget).getId().equals(nodeId)) {
                return widget;
            }
        }
        return null;
    }
}
