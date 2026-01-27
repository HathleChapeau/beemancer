/**
 * ============================================================
 * [CodexScreen.java]
 * Description: Screen principal du Codex avec onglets et delegation
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexPageRenderer   | Interface renderers  | Delegation par page            |
 * | BeeTreePageRenderer | Page BEES            | Rendu abeilles 3D              |
 * | StandardPageRenderer| Pages standard       | Rendu nodes classiques         |
 * | BeemancerSounds     | Sons                 | Feedback audio                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexItem (ouverture du screen)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.gui.screen.codex.BeeTreePageRenderer;
import com.chapeau.beemancer.client.gui.screen.codex.CodexPageRenderer;
import com.chapeau.beemancer.client.gui.screen.codex.StandardPageRenderer;
import com.chapeau.beemancer.common.codex.*;
import com.chapeau.beemancer.core.network.packets.CodexUnlockPacket;
import com.chapeau.beemancer.core.registry.BeemancerAttachments;
import com.chapeau.beemancer.core.registry.BeemancerSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class CodexScreen extends Screen {
    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/codex_background.png"
    );
    private static final ResourceLocation WINDOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/codex_window.png"
    );

    private static final int WINDOW_WIDTH = 252;
    private static final int WINDOW_HEIGHT = 200;
    private static final int TAB_WIDTH = 60;
    private static final int TAB_HEIGHT = 20;
    private static final int CONTENT_PADDING = 9;
    private static final int NODE_SPACING = 30;

    private CodexPage currentPage = CodexPage.BEES;
    private final Map<CodexPage, Button> tabButtons = new EnumMap<>(CodexPage.class);
    private final Map<CodexPage, CodexPageRenderer> pageRenderers = new EnumMap<>(CodexPage.class);
    private CodexPageRenderer currentRenderer;

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

    public CodexScreen() {
        super(Component.translatable("screen.beemancer.codex"));
        initRenderers();
    }

    private void initRenderers() {
        pageRenderers.put(CodexPage.BEES, new BeeTreePageRenderer());
        pageRenderers.put(CodexPage.BEE, new StandardPageRenderer());
        pageRenderers.put(CodexPage.ALCHEMY, new StandardPageRenderer());
        pageRenderers.put(CodexPage.LOGISTICS, new StandardPageRenderer());
    }

    @Override
    protected void init() {
        super.init();

        windowX = (width - WINDOW_WIDTH) / 2;
        windowY = (height - WINDOW_HEIGHT) / 2;
        contentX = windowX + CONTENT_PADDING;
        contentY = windowY + CONTENT_PADDING + 16;
        contentWidth = WINDOW_WIDTH - CONTENT_PADDING * 2;
        contentHeight = WINDOW_HEIGHT - CONTENT_PADDING * 2 - 20;

        createTabButtons();
        currentRenderer = pageRenderers.get(currentPage);
        rebuildNodeWidgets();
    }

    private void createTabButtons() {
        tabButtons.clear();

        // Position tabs horizontally centered at top of screen, above window
        CodexPage[] pages = CodexPage.values();
        int totalWidth = pages.length * TAB_WIDTH + (pages.length - 1) * 4;
        int tabX = (width - totalWidth) / 2;
        int tabY = windowY - TAB_HEIGHT - 5;

        for (CodexPage page : pages) {
            final CodexPage currentPageRef = page;
            Button tabButton = Button.builder(page.getDisplayName(), btn -> {
                if (currentPage != currentPageRef) {
                    playSound(BeemancerSounds.CODEX_PAGE_TURN.get());
                    switchToPage(currentPageRef);
                }
            }).bounds(tabX, tabY, TAB_WIDTH, TAB_HEIGHT).build();

            tabButtons.put(page, tabButton);
            addRenderableWidget(tabButton);
            tabX += TAB_WIDTH + 4;
        }

        updateTabButtonStyles();
    }

    private void switchToPage(CodexPage page) {
        // Clear current widgets
        clearCurrentWidgets();

        currentPage = page;
        currentRenderer = pageRenderers.get(currentPage);
        scrollX = 0;
        scrollY = 0;

        rebuildNodeWidgets();
        updateTabButtonStyles();
    }

    private void updateTabButtonStyles() {
        for (Map.Entry<CodexPage, Button> entry : tabButtons.entrySet()) {
            Button btn = entry.getValue();
            boolean isActive = entry.getKey() == currentPage;
            btn.active = !isActive;
        }
    }

    private void rebuildNodeWidgets() {
        clearCurrentWidgets();

        CodexPlayerData playerData = getPlayerData();
        Set<String> unlockedNodes = playerData.getUnlockedNodes();
        List<CodexNode> nodes = CodexManager.getNodesForPage(currentPage);

        currentRenderer.rebuildWidgets(nodes, unlockedNodes, playerData,
                contentX, contentY, NODE_SPACING, scrollX, scrollY);

        // Add widgets to screen
        for (Object widget : currentRenderer.getWidgets()) {
            if (widget instanceof AbstractWidget abstractWidget) {
                addRenderableWidget(abstractWidget);
            }
        }
    }

    private void clearCurrentWidgets() {
        if (currentRenderer != null) {
            for (Object widget : currentRenderer.getWidgets()) {
                if (widget instanceof AbstractWidget abstractWidget) {
                    removeWidget(abstractWidget);
                }
            }
            currentRenderer.clearWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        renderTiledBackground(graphics);

        graphics.blit(WINDOW_TEXTURE, windowX, windowY, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_WIDTH, WINDOW_HEIGHT);

        graphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);

        currentRenderer.renderConnections(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.disableScissor();

        currentRenderer.renderTooltips(graphics, mouseX, mouseY);

        graphics.drawCenteredString(font, currentPage.getDisplayName(), width / 2, windowY + 6, 0xFFFFFF);
        renderProgress(graphics);
    }

    private void renderTiledBackground(GuiGraphics graphics) {
        int tileSize = 16;
        for (int x = contentX; x < contentX + contentWidth; x += tileSize) {
            for (int y = contentY; y < contentY + contentHeight; y += tileSize) {
                graphics.blit(BACKGROUND_TEXTURE, x, y, 0, 0,
                        Math.min(tileSize, contentX + contentWidth - x),
                        Math.min(tileSize, contentY + contentHeight - y),
                        tileSize, tileSize);
            }
        }
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
            boolean handled = currentRenderer.handleClick(mouseX, mouseY, this::handleNodeClick);
            if (handled) {
                return true;
            }

            if (isInContentArea(mouseX, mouseY)) {
                isDragging = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleNodeClick(CodexNode node, boolean isUnlocked, boolean canUnlock) {
        if (canUnlock && !isUnlocked) {
            PacketDistributor.sendToServer(new CodexUnlockPacket(node.getFullId()));
            playSound(BeemancerSounds.CODEX_NODE_UNLOCK.get());
            rebuildNodeWidgets();
        } else if (isUnlocked) {
            playSound(BeemancerSounds.CODEX_NODE_CLICK.get());
        }
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
            currentRenderer.updatePositions(contentX, contentY, NODE_SPACING, scrollX, scrollY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollXDelta, double scrollYDelta) {
        if (isInContentArea(mouseX, mouseY)) {
            this.scrollY += scrollYDelta * 20;
            currentRenderer.updatePositions(contentX, contentY, NODE_SPACING, scrollX, scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollXDelta, scrollYDelta);
    }

    private boolean isInContentArea(double mouseX, double mouseY) {
        return mouseX >= contentX && mouseX < contentX + contentWidth
                && mouseY >= contentY && mouseY < contentY + contentHeight;
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
