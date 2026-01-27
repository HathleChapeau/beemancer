/**
 * ============================================================
 * [CodexScreen.java]
 * Description: Screen principal du Codex avec onglets et nodes
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexManager        | Accès aux nodes      | Récupération des données       |
 * | CodexPlayerData     | Progression joueur   | État des déblocages            |
 * | CodexNodeWidget     | Rendu des nodes      | Affichage interactif           |
 * | BeemancerSounds     | Sons                 | Feedback audio                 |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexItem (ouverture du screen)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.gui.widget.BeeNodeWidget;
import com.chapeau.beemancer.client.gui.widget.CodexNodeWidget;
import com.chapeau.beemancer.common.codex.*;
import com.chapeau.beemancer.core.network.packets.CodexUnlockPacket;
import com.chapeau.beemancer.core.registry.BeemancerAttachments;
import com.chapeau.beemancer.core.registry.BeemancerSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class CodexScreen extends Screen {
    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/codex_background.png"
    );
    private static final ResourceLocation WINDOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/codex_window.png"
    );
    // Achievement-style background for bee tree
    private static final ResourceLocation ADVANCEMENT_BACKGROUND = ResourceLocation.withDefaultNamespace(
        "textures/gui/advancements/backgrounds/stone.png"
    );

    private static final int WINDOW_WIDTH = 252;
    private static final int WINDOW_HEIGHT = 200;
    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 32;
    private static final int CONTENT_PADDING = 9;
    private static final int NODE_SPACING = 40;

    private CodexPage currentPage = CodexPage.BEES;
    private final Map<CodexPage, Button> tabButtons = new EnumMap<>(CodexPage.class);
    private final List<CodexNodeWidget> nodeWidgets = new ArrayList<>();
    private final List<BeeNodeWidget> beeWidgets = new ArrayList<>();

    private int windowX;
    private int windowY;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;

    // Scrolling
    private double scrollX = 0;
    private double scrollY = 0;
    private boolean isDragging = false;
    private double dragStartX;
    private double dragStartY;

    public CodexScreen() {
        super(Component.translatable("screen.beemancer.codex"));
    }

    @Override
    protected void init() {
        super.init();

        windowX = (width - WINDOW_WIDTH) / 2;
        windowY = (height - WINDOW_HEIGHT) / 2;
        contentX = windowX + CONTENT_PADDING;
        contentY = windowY + CONTENT_PADDING + TAB_HEIGHT - 4;
        contentWidth = WINDOW_WIDTH - CONTENT_PADDING * 2;
        contentHeight = WINDOW_HEIGHT - CONTENT_PADDING * 2 - TAB_HEIGHT + 4;

        createTabButtons();
        rebuildNodeWidgets();
    }

    private void createTabButtons() {
        tabButtons.clear();

        int tabX = windowX;
        for (CodexPage page : CodexPage.values()) {
            Button tabButton = Button.builder(page.getDisplayName(), btn -> {
                if (currentPage != page) {
                    playSound(BeemancerSounds.CODEX_PAGE_TURN.get());
                    currentPage = page;
                    rebuildNodeWidgets();
                }
            }).bounds(tabX, windowY - TAB_HEIGHT + 4, TAB_WIDTH, TAB_HEIGHT).build();

            tabButtons.put(page, tabButton);
            addRenderableWidget(tabButton);
            tabX += TAB_WIDTH + 2;
        }
    }

    private void rebuildNodeWidgets() {
        // Remove old widgets
        for (CodexNodeWidget widget : nodeWidgets) {
            removeWidget(widget);
        }
        nodeWidgets.clear();

        for (BeeNodeWidget widget : beeWidgets) {
            removeWidget(widget);
        }
        beeWidgets.clear();

        // Get player data
        CodexPlayerData playerData = getPlayerData();
        Set<String> unlockedNodes = playerData.getUnlockedNodes();

        // Get nodes for current page
        List<CodexNode> nodes = CodexManager.getNodesForPage(currentPage);

        for (CodexNode node : nodes) {
            // Check visibility
            if (!CodexManager.isVisible(node, unlockedNodes)) {
                continue;
            }

            boolean unlocked = playerData.isUnlocked(node);
            boolean canUnlock = CodexManager.canUnlock(node, unlockedNodes);

            int nodeScreenX = contentX + node.getX() * NODE_SPACING + (int) scrollX;
            int nodeScreenY = contentY + node.getY() * NODE_SPACING + (int) scrollY;

            // Use BeeNodeWidget for BEES page, CodexNodeWidget for others
            if (currentPage.isBeeTree()) {
                BeeNodeWidget widget = new BeeNodeWidget(node, nodeScreenX, nodeScreenY, unlocked, canUnlock);
                beeWidgets.add(widget);
                addRenderableWidget(widget);
            } else {
                CodexNodeWidget widget = new CodexNodeWidget(node, nodeScreenX, nodeScreenY, unlocked, canUnlock);
                nodeWidgets.add(widget);
                addRenderableWidget(widget);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render dark background
        renderBackground(graphics, mouseX, mouseY, partialTick);

        // Render tiled background pattern
        renderTiledBackground(graphics);

        // Render window frame
        graphics.blit(WINDOW_TEXTURE, windowX, windowY, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_WIDTH, WINDOW_HEIGHT);

        // Enable scissor for content area
        graphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);

        // Render connection lines between nodes
        renderConnections(graphics);

        // Render node widgets
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.disableScissor();

        // Render tooltips (outside scissor)
        if (currentPage.isBeeTree()) {
            for (BeeNodeWidget widget : beeWidgets) {
                widget.renderTooltip(graphics, mouseX, mouseY);
            }
        } else {
            for (CodexNodeWidget widget : nodeWidgets) {
                widget.renderTooltip(graphics, mouseX, mouseY);
            }
        }

        // Render title
        graphics.drawCenteredString(font, title, width / 2, windowY + 6, 0xFFFFFF);

        // Render page progress
        renderProgress(graphics);
    }

    private void renderTiledBackground(GuiGraphics graphics) {
        int tileSize = 16;
        ResourceLocation bgTexture = currentPage.isBeeTree() ? ADVANCEMENT_BACKGROUND : BACKGROUND_TEXTURE;

        for (int x = contentX; x < contentX + contentWidth; x += tileSize) {
            for (int y = contentY; y < contentY + contentHeight; y += tileSize) {
                // Apply scroll offset for parallax effect on bee tree
                int texU = currentPage.isBeeTree() ? (int)(-scrollX * 0.5) % tileSize : 0;
                int texV = currentPage.isBeeTree() ? (int)(-scrollY * 0.5) % tileSize : 0;
                if (texU < 0) texU += tileSize;
                if (texV < 0) texV += tileSize;

                graphics.blit(bgTexture, x, y, texU, texV,
                    Math.min(tileSize, contentX + contentWidth - x),
                    Math.min(tileSize, contentY + contentHeight - y),
                    tileSize, tileSize);
            }
        }
    }

    private void renderConnections(GuiGraphics graphics) {
        if (currentPage.isBeeTree()) {
            renderBeeConnections(graphics);
        } else {
            renderCodexConnections(graphics);
        }
    }

    private void renderCodexConnections(GuiGraphics graphics) {
        for (CodexNodeWidget widget : nodeWidgets) {
            CodexNode node = widget.getNode();
            String parentId = node.getParentId();

            if (parentId != null) {
                CodexNodeWidget parentWidget = findCodexWidgetByNodeId(parentId);
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

    private void renderBeeConnections(GuiGraphics graphics) {
        for (BeeNodeWidget widget : beeWidgets) {
            CodexNode node = widget.getNode();
            String parentId = node.getParentId();

            if (parentId != null) {
                BeeNodeWidget parentWidget = findBeeWidgetByNodeId(parentId);
                if (parentWidget != null) {
                    int startX = parentWidget.getX() + BeeNodeWidget.NODE_SIZE / 2;
                    int startY = parentWidget.getY() + BeeNodeWidget.NODE_SIZE / 2;
                    int endX = widget.getX() + BeeNodeWidget.NODE_SIZE / 2;
                    int endY = widget.getY() + BeeNodeWidget.NODE_SIZE / 2;

                    // Golden connections for bees
                    int lineColor = widget.isUnlocked() ? 0xFFFFAA00 : 0xFF555555;
                    int lineWidth = 2;

                    // Draw bezier-like connection
                    int midX = (startX + endX) / 2;
                    int midY = (startY + endY) / 2;

                    // Horizontal from parent
                    graphics.fill(Math.min(startX, midX), startY - lineWidth/2,
                                  Math.max(startX, midX), startY + lineWidth/2, lineColor);
                    // Vertical connector
                    graphics.fill(midX - lineWidth/2, Math.min(startY, endY),
                                  midX + lineWidth/2, Math.max(startY, endY), lineColor);
                    // Horizontal to child
                    graphics.fill(Math.min(midX, endX), endY - lineWidth/2,
                                  Math.max(midX, endX), endY + lineWidth/2, lineColor);
                }
            }
        }
    }

    private CodexNodeWidget findCodexWidgetByNodeId(String nodeId) {
        for (CodexNodeWidget widget : nodeWidgets) {
            if (widget.getNode().getId().equals(nodeId)) {
                return widget;
            }
        }
        return null;
    }

    private BeeNodeWidget findBeeWidgetByNodeId(String nodeId) {
        for (BeeNodeWidget widget : beeWidgets) {
            if (widget.getNode().getId().equals(nodeId)) {
                return widget;
            }
        }
        return null;
    }

    private void renderProgress(GuiGraphics graphics) {
        CodexPlayerData data = getPlayerData();
        int unlocked = data.getUnlockedCountForPage(currentPage);
        int total = CodexManager.getNodesForPage(currentPage).size();

        String progress = unlocked + "/" + total;
        graphics.drawString(font, progress, windowX + WINDOW_WIDTH - font.width(progress) - 8, 
            windowY + WINDOW_HEIGHT - 14, 0xAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Check node clicks based on current page type
            if (currentPage.isBeeTree()) {
                for (BeeNodeWidget widget : beeWidgets) {
                    if (widget.isMouseOver(mouseX, mouseY)) {
                        if (widget.canUnlock() && !widget.isUnlocked()) {
                            unlockBeeNode(widget);
                            return true;
                        } else if (widget.isUnlocked()) {
                            playSound(BeemancerSounds.CODEX_NODE_CLICK.get());
                            return true;
                        }
                    }
                }
            } else {
                for (CodexNodeWidget widget : nodeWidgets) {
                    if (widget.isMouseOver(mouseX, mouseY)) {
                        if (widget.canUnlock() && !widget.isUnlocked()) {
                            unlockNode(widget);
                            return true;
                        } else if (widget.isUnlocked()) {
                            playSound(BeemancerSounds.CODEX_NODE_CLICK.get());
                            return true;
                        }
                    }
                }
            }

            // Start dragging for scroll
            if (isInContentArea(mouseX, mouseY)) {
                isDragging = true;
                dragStartX = mouseX;
                dragStartY = mouseY;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging && button == 0) {
            scrollX += dragX;
            scrollY += dragY;
            updateNodePositions();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInContentArea(mouseX, mouseY)) {
            this.scrollY += scrollY * 20;
            updateNodePositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void updateNodePositions() {
        for (CodexNodeWidget widget : nodeWidgets) {
            CodexNode node = widget.getNode();
            int nodeScreenX = contentX + node.getX() * NODE_SPACING + (int) scrollX;
            int nodeScreenY = contentY + node.getY() * NODE_SPACING + (int) scrollY;
            widget.setX(nodeScreenX);
            widget.setY(nodeScreenY);
        }
        for (BeeNodeWidget widget : beeWidgets) {
            CodexNode node = widget.getNode();
            int nodeScreenX = contentX + node.getX() * NODE_SPACING + (int) scrollX;
            int nodeScreenY = contentY + node.getY() * NODE_SPACING + (int) scrollY;
            widget.setX(nodeScreenX);
            widget.setY(nodeScreenY);
        }
    }

    private boolean isInContentArea(double mouseX, double mouseY) {
        return mouseX >= contentX && mouseX < contentX + contentWidth
            && mouseY >= contentY && mouseY < contentY + contentHeight;
    }

    private void unlockNode(CodexNodeWidget widget) {
        // Send packet to server
        PacketDistributor.sendToServer(new CodexUnlockPacket(widget.getNode().getFullId()));

        // Optimistic update
        widget.setUnlocked(true);
        playSound(BeemancerSounds.CODEX_NODE_UNLOCK.get());

        // Update can-unlock status for children
        rebuildNodeWidgets();
    }

    private void unlockBeeNode(BeeNodeWidget widget) {
        // Send packet to server
        PacketDistributor.sendToServer(new CodexUnlockPacket(widget.getNode().getFullId()));

        // Optimistic update
        widget.setUnlocked(true);
        playSound(BeemancerSounds.CODEX_NODE_UNLOCK.get());

        // Update can-unlock status for children
        rebuildNodeWidgets();
    }

    private void playSound(net.minecraft.sounds.SoundEvent sound) {
        Minecraft.getInstance().player.playSound(sound, 0.5f, 1.0f);
    }

    private CodexPlayerData getPlayerData() {
        if (Minecraft.getInstance().player != null) {
            return Minecraft.getInstance().player.getData(BeemancerAttachments.CODEX_DATA);
        }
        return new CodexPlayerData();
    }

    @Override
    public void onClose() {
        playSound(BeemancerSounds.CODEX_CLOSE.get());
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
