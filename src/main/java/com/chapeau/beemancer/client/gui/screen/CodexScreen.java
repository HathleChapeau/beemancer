/**
 * ============================================================
 * [CodexScreen.java]
 * Description: Screen principal du Codex avec frame dynamique et scroll
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexPageRenderer   | Interface renderers  | Delegation par page            |
 * | BeeTreePageRenderer | Page BEES            | Rendu abeilles 3D              |
 * | StandardPageRenderer| Pages standard       | Rendu nodes classiques         |
 * | CodexJsonLoader     | Donnees JSON         | Positions des nodes            |
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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
    // Textures pour la frame
    private static final ResourceLocation CORNER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/codex_corner.png"
    );
    private static final ResourceLocation BAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/codex_bar.png"
    );

    // Textures source
    private static final int CORNER_SRC = 5;
    private static final int BAR_SRC_W = 80;
    private static final int BAR_SRC_H = 5;

    // Rendu scale x3
    private static final int SCALE = 3;
    private static final int BORDER = CORNER_SRC * SCALE; // 15px

    // Dimensions de la frame
    private static final int FRAME_WIDTH = 300;
    private static final int FRAME_HEIGHT = 200;
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_SPACING = 4;

    // Background color #F3E1BB (couleur unie, pas de tiling)
    private static final int BG_COLOR = 0xFFF3E1BB;

    // Espacement entre nodes (comme BEES)
    private static final int NODE_SPACING = 30;

    private CodexPage currentPage = CodexPage.APICA;
    private final Map<CodexPage, Button> tabButtons = new EnumMap<>(CodexPage.class);
    private final Map<CodexPage, CodexPageRenderer> pageRenderers = new EnumMap<>(CodexPage.class);
    private CodexPageRenderer currentRenderer;

    private int frameX;
    private int frameY;
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
        pageRenderers.put(CodexPage.APICA, new StandardPageRenderer());
        pageRenderers.put(CodexPage.BEES, new BeeTreePageRenderer());
        pageRenderers.put(CodexPage.ALCHEMY, new StandardPageRenderer());
        pageRenderers.put(CodexPage.ARTIFACTS, new StandardPageRenderer());
        pageRenderers.put(CodexPage.LOGISTICS, new StandardPageRenderer());
    }

    @Override
    protected void init() {
        super.init();

        // Charger les données JSON
        if (!CodexJsonLoader.isLoaded()) {
            CodexJsonLoader.load();
        }

        // Position de la frame (centrée, avec espace pour les tabs au-dessus)
        frameX = (width - FRAME_WIDTH) / 2;
        frameY = (height - FRAME_HEIGHT - TAB_HEIGHT - TAB_SPACING) / 2 + TAB_HEIGHT + TAB_SPACING;

        // Zone de contenu (à l'intérieur de la frame)
        contentX = frameX + BORDER;
        contentY = frameY + BORDER;
        contentWidth = FRAME_WIDTH - BORDER * 2;
        contentHeight = FRAME_HEIGHT - BORDER * 2;

        // Créer les boutons de tab
        createTabButtons();

        // Créer les widgets de nodes
        currentRenderer = pageRenderers.get(currentPage);
        rebuildNodeWidgets();
    }

    private void createTabButtons() {
        tabButtons.clear();

        CodexPage[] pages = CodexPage.values();
        int tabWidth = 55;
        int totalWidth = pages.length * tabWidth + (pages.length - 1) * TAB_SPACING;
        int tabX = (width - totalWidth) / 2;
        int tabY = frameY - TAB_HEIGHT - TAB_SPACING;

        for (CodexPage page : pages) {
            final CodexPage currentPageRef = page;
            Button tabButton = Button.builder(page.getDisplayName(), btn -> {
                if (currentPage != currentPageRef) {
                    playSound(BeemancerSounds.CODEX_PAGE_TURN.get());
                    switchToPage(currentPageRef);
                }
            }).bounds(tabX, tabY, tabWidth, TAB_HEIGHT).build();

            tabButtons.put(page, tabButton);
            addRenderableWidget(tabButton);
            tabX += tabWidth + TAB_SPACING;
        }

        updateTabButtonStyles();
    }

    private void switchToPage(CodexPage page) {
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

        // Pour BEES, utiliser le système existant
        if (currentPage == CodexPage.BEES) {
            List<CodexNode> nodes = CodexManager.getNodesForPage(currentPage);

            currentRenderer.rebuildWidgets(nodes, unlockedNodes, playerData,
                    contentX + contentWidth / 2, contentY + contentHeight / 2, NODE_SPACING, scrollX, scrollY);

            for (Object widget : currentRenderer.getWidgets()) {
                if (widget instanceof AbstractWidget abstractWidget) {
                    addRenderableWidget(abstractWidget);
                }
            }
            return;
        }

        // Pour les autres pages, utiliser CodexJsonLoader avec positions de grille
        CodexJsonLoader.TabData tabData = CodexJsonLoader.getTabData(currentPage);
        if (tabData == null) {
            return;
        }

        // Convertir les JsonNodeData en CodexNodes avec positions de grille
        List<CodexNode> nodes = new ArrayList<>();
        for (CodexJsonLoader.JsonNodeData jsonNode : tabData.nodes) {
            // Utiliser directement gridX et gridY (déjà relatifs au node header)
            CodexNode node = createNodeFromJson(jsonNode, jsonNode.gridX, jsonNode.gridY);
            nodes.add(node);
        }

        // Utiliser le même nodeSpacing que BEES, centré dans le contenu
        currentRenderer.rebuildWidgets(nodes, unlockedNodes, playerData,
                contentX + contentWidth / 2, contentY + contentHeight / 2, NODE_SPACING, scrollX, scrollY);

        for (Object widget : currentRenderer.getWidgets()) {
            if (widget instanceof AbstractWidget abstractWidget) {
                addRenderableWidget(abstractWidget);
            }
        }
    }

    private CodexNode createNodeFromJson(CodexJsonLoader.JsonNodeData jsonNode, int x, int y) {
        String id = jsonNode.name.toLowerCase().replace(" ", "_");
        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/codex_node_default.png"
        );

        return new CodexNode(
            id, jsonNode.page, x, y, icon,
            CodexNodeCategory.NORMAL, null, false,
            new com.google.gson.JsonObject(), new com.google.gson.JsonObject(),
            null, null
        );
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

        // 1. Rendu du background uni (pas de tiling!)
        graphics.fill(frameX, frameY, frameX + FRAME_WIDTH, frameY + FRAME_HEIGHT, BG_COLOR);

        // 2. Rendu du contenu avec scissor (clippe aux bords)
        graphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);

        // Connexions et nodes
        currentRenderer.renderConnections(graphics);
        for (Object widget : currentRenderer.getWidgets()) {
            if (widget instanceof AbstractWidget aw) {
                aw.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        graphics.disableScissor();

        // 3. Rendu de la frame PAR-DESSUS le contenu
        renderFrame(graphics);

        // 4. Rendu des boutons de tab (en dehors de la frame)
        for (Button btn : tabButtons.values()) {
            btn.render(graphics, mouseX, mouseY, partialTick);
        }

        // 5. Tooltips
        currentRenderer.renderTooltips(graphics, mouseX, mouseY);

        // 6. Progress
        renderProgress(graphics);
    }

    private void renderFrame(GuiGraphics graphics) {
        PoseStack pose = graphics.pose();
        int barRenderW = BAR_SRC_W * SCALE;
        int innerLeft = frameX + BORDER;
        int innerRight = frameX + FRAME_WIDTH - BORDER;
        int innerWidth = FRAME_WIDTH - BORDER * 2;
        int innerTop = frameY + BORDER;
        int innerBottom = frameY + FRAME_HEIGHT - BORDER;
        int innerHeight = FRAME_HEIGHT - BORDER * 2;

        // Barres horizontales
        // Barre du haut
        graphics.enableScissor(innerLeft, frameY, innerRight, frameY + BORDER);
        for (int offset = 0; offset < innerWidth; offset += barRenderW) {
            pose.pushPose();
            pose.translate(innerLeft + offset, frameY, 0);
            pose.scale(SCALE, SCALE, 1);
            graphics.blit(BAR_TEXTURE, 0, 0, 0, 0, BAR_SRC_W, BAR_SRC_H, BAR_SRC_W, BAR_SRC_H);
            pose.popPose();
        }
        graphics.disableScissor();

        // Barre du bas
        graphics.enableScissor(innerLeft, frameY + FRAME_HEIGHT - BORDER, innerRight, frameY + FRAME_HEIGHT);
        for (int offset = 0; offset < innerWidth; offset += barRenderW) {
            pose.pushPose();
            pose.translate(innerLeft + offset, frameY + FRAME_HEIGHT - BORDER, 0);
            pose.scale(SCALE, SCALE, 1);
            graphics.blit(BAR_TEXTURE, 0, 0, BAR_SRC_W, BAR_SRC_H,
                    0, BAR_SRC_H, BAR_SRC_W, -BAR_SRC_H, BAR_SRC_W, BAR_SRC_H);
            pose.popPose();
        }
        graphics.disableScissor();

        // Barres verticales
        // Barre gauche
        graphics.enableScissor(frameX, innerTop, frameX + BORDER, innerBottom);
        for (int offset = 0; offset < innerHeight; offset += barRenderW) {
            pose.pushPose();
            pose.translate(frameX, innerTop + offset + barRenderW, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(-90));
            pose.scale(SCALE, SCALE, 1);
            graphics.blit(BAR_TEXTURE, 0, 0, 0, 0, BAR_SRC_W, BAR_SRC_H, BAR_SRC_W, BAR_SRC_H);
            pose.popPose();
        }
        graphics.disableScissor();

        // Barre droite
        graphics.enableScissor(frameX + FRAME_WIDTH - BORDER, innerTop, frameX + FRAME_WIDTH, innerBottom);
        for (int offset = 0; offset < innerHeight; offset += barRenderW) {
            pose.pushPose();
            pose.translate(frameX + FRAME_WIDTH, innerTop + offset, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(90));
            pose.scale(SCALE, SCALE, 1);
            graphics.blit(BAR_TEXTURE, 0, 0, 0, 0, BAR_SRC_W, BAR_SRC_H, BAR_SRC_W, BAR_SRC_H);
            pose.popPose();
        }
        graphics.disableScissor();

        // Coins
        // Haut-gauche
        pose.pushPose();
        pose.translate(frameX, frameY, 0);
        pose.scale(SCALE, SCALE, 1);
        graphics.blit(CORNER_TEXTURE, 0, 0, 0, 0, CORNER_SRC, CORNER_SRC, CORNER_SRC, CORNER_SRC);
        pose.popPose();

        // Haut-droit
        pose.pushPose();
        pose.translate(frameX + FRAME_WIDTH - BORDER, frameY, 0);
        pose.scale(SCALE, SCALE, 1);
        graphics.blit(CORNER_TEXTURE, 0, 0, CORNER_SRC, CORNER_SRC,
                CORNER_SRC, 0, -CORNER_SRC, CORNER_SRC, CORNER_SRC, CORNER_SRC);
        pose.popPose();

        // Bas-gauche
        pose.pushPose();
        pose.translate(frameX, frameY + FRAME_HEIGHT - BORDER, 0);
        pose.scale(SCALE, SCALE, 1);
        graphics.blit(CORNER_TEXTURE, 0, 0, CORNER_SRC, CORNER_SRC,
                0, CORNER_SRC, CORNER_SRC, -CORNER_SRC, CORNER_SRC, CORNER_SRC);
        pose.popPose();

        // Bas-droit
        pose.pushPose();
        pose.translate(frameX + FRAME_WIDTH - BORDER, frameY + FRAME_HEIGHT - BORDER, 0);
        pose.scale(SCALE, SCALE, 1);
        graphics.blit(CORNER_TEXTURE, 0, 0, CORNER_SRC, CORNER_SRC,
                CORNER_SRC, CORNER_SRC, -CORNER_SRC, -CORNER_SRC, CORNER_SRC, CORNER_SRC);
        pose.popPose();
    }

    private void renderProgress(GuiGraphics graphics) {
        CodexJsonLoader.TabData tabData = CodexJsonLoader.getTabData(currentPage);
        int total = tabData != null ? tabData.nodes.size() : 0;
        int unlocked = 0;

        String progress = unlocked + "/" + total;
        graphics.drawString(font, progress, frameX + FRAME_WIDTH - font.width(progress) - 8,
                frameY + FRAME_HEIGHT - 14, 0x805030);
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
            updateNodePositions();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollXDelta, double scrollYDelta) {
        if (isInContentArea(mouseX, mouseY)) {
            this.scrollY += scrollYDelta * 20;
            updateNodePositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollXDelta, scrollYDelta);
    }

    private void updateNodePositions() {
        // Même système pour toutes les pages: centré avec NODE_SPACING
        currentRenderer.updatePositions(contentX + contentWidth / 2, contentY + contentHeight / 2, NODE_SPACING, scrollX, scrollY);
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
