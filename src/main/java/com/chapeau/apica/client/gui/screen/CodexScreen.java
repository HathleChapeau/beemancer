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
 * | CodexManager        | Donnees nodes        | Chargement des nodes           |
 * | ApicaSounds     | Sons                 | Feedback audio                 |
 * | CodexDebugQuestPanel| Panneau debug quetes | Affichage quetes en mode debug |
 * | DebugWandItem       | Flag displayDebug    | Activation mode debug          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexItem (ouverture du screen)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.gui.screen.codex.BeeTreePageRenderer;
import com.chapeau.apica.client.gui.screen.codex.CodexDebugQuestPanel;
import com.chapeau.apica.client.gui.screen.codex.CodexDecorationRenderer;
import com.chapeau.apica.client.gui.screen.codex.CodexPageRenderer;
import com.chapeau.apica.client.gui.screen.codex.StandardPageRenderer;
import com.chapeau.apica.client.gui.widget.CodexTabButtonWidget;
import com.chapeau.apica.common.codex.CodexManager;
import com.chapeau.apica.common.codex.CodexNode;
import com.chapeau.apica.common.codex.CodexPage;
import com.chapeau.apica.common.codex.CodexPlayerData;
import com.chapeau.apica.common.item.debug.DebugWandItem;
import com.chapeau.apica.common.quest.QuestManager;
import com.chapeau.apica.common.quest.QuestPlayerData;
import com.chapeau.apica.core.network.packets.CodexFirstOpenPacket;
import com.chapeau.apica.core.network.packets.CodexUnlockPacket;
import com.chapeau.apica.core.registry.ApicaAttachments;
import com.chapeau.apica.core.registry.ApicaItems;
import com.chapeau.apica.core.registry.ApicaSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodexScreen extends Screen {
    // Textures pour la frame
    private static final ResourceLocation CORNER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/gui/codex_corner.png"
    );
    private static final ResourceLocation BAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/gui/codex_bar.png"
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
    private static final int TAB_HEIGHT = CodexTabButtonWidget.TEX_HEIGHT;
    private static final int TAB_SPACING = 4;
    private static final int TAB_LABEL_HEIGHT = 12;
    private static final int TAB_LABEL_PADDING = 3;

    // Background color #F3E1BB (couleur unie, pas de tiling)
    private static final int BG_COLOR = 0xFFF3E1BB;

    // Espacement entre nodes (comme BEES)
    private static final int NODE_SPACING = 30;

    private CodexPage currentPage = CodexPage.APICA;
    private final Map<CodexPage, CodexTabButtonWidget> tabButtons = new EnumMap<>(CodexPage.class);
    private final Map<CodexPage, CodexPageRenderer> pageRenderers = new EnumMap<>(CodexPage.class);
    private CodexPageRenderer currentRenderer;

    private int frameX;
    private int frameY;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;

    // Décorations background (cracks, stains, borders)
    private List<CodexDecorationRenderer.Decoration> currentDecorations;

    // Scrolling
    private double scrollX = 0;
    private double scrollY = 0;
    private boolean isDragging = false;

    // Scroll bounds (calculated from node positions)
    private double minScrollX = 0;
    private double maxScrollX = 0;
    private double minScrollY = 0;
    private double maxScrollY = 0;
    private static final int SCROLL_MARGIN = 40; // Marge autour des nodes extremes

    // Debug quest panel
    private final CodexDebugQuestPanel debugQuestPanel = new CodexDebugQuestPanel();

    public CodexScreen() {
        this(CodexPage.APICA);
    }

    public CodexScreen(CodexPage initialPage) {
        super(Component.translatable("screen.apica.codex"));
        this.currentPage = initialPage;
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

        // S'assurer que les données du Codex et des quêtes sont chargées côté client
        CodexManager.ensureClientLoaded();
        QuestManager.ensureClientLoaded();

        // Vérifier les quêtes OBTAIN (items dans l'inventaire)
        if (Minecraft.getInstance().player != null) {
            QuestManager.checkObtainQuests(Minecraft.getInstance().player);

            // Enregistrer la première ouverture du Codex (envoie le jour MC au serveur)
            CodexPlayerData data = getPlayerData();
            if (data.getFirstOpenDay() == -1) {
                PacketDistributor.sendToServer(new CodexFirstOpenPacket());
            }
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

        // Créer les widgets de nodes (calcule aussi les scroll bounds)
        currentRenderer = pageRenderers.get(currentPage);
        rebuildNodeWidgets();

        // Générer les décorations dans l'espace scrollable (après calcul des bounds)
        regenerateDecorations();
    }

    private void createTabButtons() {
        tabButtons.clear();

        CodexPage[] pages = CodexPage.values();
        int tabWidth = CodexTabButtonWidget.TEX_WIDTH;
        int totalWidth = pages.length * tabWidth + (pages.length - 1) * TAB_SPACING;
        int tabX = (width - totalWidth) / 2;
        int tabY = frameY - TAB_HEIGHT - TAB_SPACING;

        for (CodexPage page : pages) {
            final CodexPage currentPageRef = page;
            CodexTabButtonWidget tabButton = new CodexTabButtonWidget(
                    tabX, tabY, page, getTabIcon(page), () -> {
                if (currentPage != currentPageRef) {
                    playSound(ApicaSounds.CODEX_PAGE_TURN.get());
                    switchToPage(currentPageRef);
                }
            });

            tabButtons.put(page, tabButton);
            addRenderableWidget(tabButton);
            tabX += tabWidth + TAB_SPACING;
        }

        updateTabButtonStyles();
    }

    private ItemStack getTabIcon(CodexPage page) {
        return switch (page) {
            case APICA -> new ItemStack(ApicaItems.CODEX.get());
            case BEES -> new ItemStack(Items.HONEYCOMB);
            case ALCHEMY -> new ItemStack(ApicaItems.POLLEN_POT.get());
            case ARTIFACTS -> new ItemStack(ApicaItems.NECTAR_DIAMOND.get());
            case LOGISTICS -> new ItemStack(ApicaItems.STORAGE_CONTROLLER.get());
        };
    }

    private void switchToPage(CodexPage page) {
        clearCurrentWidgets();

        currentPage = page;
        currentRenderer = pageRenderers.get(currentPage);
        scrollX = 0;
        scrollY = 0;

        rebuildNodeWidgets();
        regenerateDecorations();
        updateTabButtonStyles();
    }

    private void regenerateDecorations() {
        currentDecorations = CodexDecorationRenderer.generate(
                currentPage, minScrollX, maxScrollX, minScrollY, maxScrollY,
                contentWidth, contentHeight);
    }

    private void updateTabButtonStyles() {
        for (Map.Entry<CodexPage, CodexTabButtonWidget> entry : tabButtons.entrySet()) {
            entry.getValue().setSelected(entry.getKey() == currentPage);
        }
    }

    private void rebuildNodeWidgets() {
        clearCurrentWidgets();

        CodexPlayerData playerData = getPlayerData();
        Set<String> unlockedNodes = playerData.getUnlockedNodes();
        Set<String> completedQuests = getCompletedQuests();

        // Utiliser CodexManager pour TOUTES les pages (même système que BEES)
        List<CodexNode> nodes = CodexManager.getNodesForPage(currentPage);

        // Calculer les bornes de scroll AVANT de positionner les widgets
        calculateScrollBounds();
        clampScroll();

        currentRenderer.rebuildWidgets(nodes, unlockedNodes, playerData, completedQuests,
                contentX + contentWidth / 2, contentY + contentHeight / 2, NODE_SPACING, scrollX, scrollY);

        for (Object widget : currentRenderer.getWidgets()) {
            if (widget instanceof AbstractWidget abstractWidget) {
                addRenderableWidget(abstractWidget);
            }
        }

        // Reconstruire le panneau debug quetes (dynamique selon la page courante)
        debugQuestPanel.rebuild(nodes, completedQuests, 0, frameY, FRAME_HEIGHT);
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

        // Décorations (cracks, stains, borders) — suivent le scroll
        CodexDecorationRenderer.render(graphics, currentDecorations,
                contentX + contentWidth / 2, contentY + contentHeight / 2, scrollX, scrollY);

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

        // 4. Rendu des boutons de tab (en dehors de la frame) avec blend
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (CodexTabButtonWidget btn : tabButtons.values()) {
            btn.render(graphics, mouseX, mouseY, partialTick);
        }
        RenderSystem.disableBlend();

        // 5. Encadre avec le nom de la tab selectionnee, centre sous les boutons
        renderTabLabel(graphics);

        // 7. Tooltips
        currentRenderer.renderTooltips(graphics, mouseX, mouseY);

        // 8. Progress
        renderProgress(graphics);

        // 9. Debug quest panel (visible uniquement en mode debug)
        if (DebugWandItem.displayDebug) {
            debugQuestPanel.updateCompletion(getCompletedQuests());
            debugQuestPanel.render(graphics, font, mouseX, mouseY);
        }
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
        CodexPlayerData playerData = getPlayerData();
        int total = CodexManager.getNodesForPage(currentPage).size();
        int unlocked = playerData.getUnlockedCountForPage(currentPage);

        String progress = unlocked + "/" + total;
        graphics.drawString(font, progress, frameX + FRAME_WIDTH - font.width(progress) - 8,
                frameY + FRAME_HEIGHT - 14, 0x805030);
    }

    /**
     * Dessine un petit encadre sous les boutons de tab avec le nom de la tab selectionnee.
     */
    private void renderTabLabel(GuiGraphics graphics) {
        Component tabName = currentPage.getDisplayName();
        int textWidth = font.width(tabName);
        int boxWidth = textWidth + TAB_LABEL_PADDING * 2;
        int boxHeight = TAB_LABEL_HEIGHT;

        int tabY = frameY - TAB_HEIGHT - TAB_SPACING;
        int labelX = (width - boxWidth) / 2;
        int labelY = tabY + TAB_HEIGHT + 1;

        // Fond semi-transparent
        graphics.fill(labelX, labelY, labelX + boxWidth, labelY + boxHeight, 0xAA3B2412);

        // Bordure fine (1px)
        graphics.fill(labelX, labelY, labelX + boxWidth, labelY + 1, 0xFF5C3A1E);
        graphics.fill(labelX, labelY + boxHeight - 1, labelX + boxWidth, labelY + boxHeight, 0xFF5C3A1E);
        graphics.fill(labelX, labelY, labelX + 1, labelY + boxHeight, 0xFF5C3A1E);
        graphics.fill(labelX + boxWidth - 1, labelY, labelX + boxWidth, labelY + boxHeight, 0xFF5C3A1E);

        // Texte centre
        graphics.drawString(font, tabName, labelX + TAB_LABEL_PADDING, labelY + 2, 0xFFE8D5B0, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Debug panel intercepte les clics en priorite
        if (DebugWandItem.displayDebug && debugQuestPanel.mouseClicked(mouseX, mouseY, button, font)) {
            return true;
        }

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
            // DISCOVERED → envoyer unlock + ouvrir le book
            PacketDistributor.sendToServer(new CodexUnlockPacket(node.getFullId()));
            playSound(ApicaSounds.CODEX_NODE_UNLOCK.get());
            Minecraft.getInstance().setScreen(new CodexBookScreen(node, currentPage));
        } else if (isUnlocked) {
            // UNLOCKED → ouvrir le book
            playSound(ApicaSounds.CODEX_NODE_CLICK.get());
            Minecraft.getInstance().setScreen(new CodexBookScreen(node, currentPage));
        }
        // LOCKED → rien (pas de else)
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
            clampScroll();
            updateNodePositions();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollXDelta, double scrollYDelta) {
        // Debug panel scroll
        if (DebugWandItem.displayDebug && debugQuestPanel.mouseScrolled(mouseX, mouseY, scrollYDelta)) {
            return true;
        }

        if (isInContentArea(mouseX, mouseY)) {
            this.scrollY += scrollYDelta * 20;
            clampScroll();
            updateNodePositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollXDelta, scrollYDelta);
    }

    private void updateNodePositions() {
        // Même système pour toutes les pages: centré avec NODE_SPACING
        currentRenderer.updatePositions(contentX + contentWidth / 2, contentY + contentHeight / 2, NODE_SPACING, scrollX, scrollY);
    }

    /**
     * Calcule les limites de scroll basées sur les positions des nodes.
     * Les bornes permettent de voir tous les nodes mais empechent de scroller a l'infini.
     */
    private void calculateScrollBounds() {
        List<CodexNode> nodes = CodexManager.getNodesForPage(currentPage);
        if (nodes.isEmpty()) {
            minScrollX = maxScrollX = 0;
            minScrollY = maxScrollY = 0;
            return;
        }

        // Trouver les positions extremes des nodes (en coordonnees grille)
        int gridMinX = Integer.MAX_VALUE;
        int gridMaxX = Integer.MIN_VALUE;
        int gridMinY = Integer.MAX_VALUE;
        int gridMaxY = Integer.MIN_VALUE;

        for (CodexNode node : nodes) {
            gridMinX = Math.min(gridMinX, node.getX());
            gridMaxX = Math.max(gridMaxX, node.getX());
            gridMinY = Math.min(gridMinY, node.getY());
            gridMaxY = Math.max(gridMaxY, node.getY());
        }

        // Convertir en pixels (par rapport au centre de la zone de contenu)
        // Quand scroll = 0, le node en (0,0) est au centre
        // Pour voir le node le plus a gauche (gridMinX), il faut scroller a droite (scrollX positif)
        // Pour voir le node le plus a droite (gridMaxX), il faut scroller a gauche (scrollX negatif)
        int pixelMinX = gridMinX * NODE_SPACING;
        int pixelMaxX = gridMaxX * NODE_SPACING;
        int pixelMinY = gridMinY * NODE_SPACING;
        int pixelMaxY = gridMaxY * NODE_SPACING;

        // Demi-largeur et demi-hauteur de la zone de contenu
        int halfContentW = contentWidth / 2;
        int halfContentH = contentHeight / 2;

        // Bornes de scroll:
        // - Pour voir le node le plus a gauche, scrollX doit etre assez positif
        //   pour que pixelMinX + scrollX >= -halfContentW + margin
        // - Pour voir le node le plus a droite, scrollX doit etre assez negatif
        //   pour que pixelMaxX + scrollX <= halfContentW - margin
        minScrollX = -(pixelMaxX - halfContentW + SCROLL_MARGIN);
        maxScrollX = -(pixelMinX + halfContentW - SCROLL_MARGIN);
        minScrollY = -(pixelMaxY - halfContentH + SCROLL_MARGIN);
        maxScrollY = -(pixelMinY + halfContentH - SCROLL_MARGIN);

        // S'assurer que min <= max (si le contenu est plus petit que la zone)
        if (minScrollX > maxScrollX) {
            double center = (minScrollX + maxScrollX) / 2;
            minScrollX = maxScrollX = center;
        }
        if (minScrollY > maxScrollY) {
            double center = (minScrollY + maxScrollY) / 2;
            minScrollY = maxScrollY = center;
        }
    }

    /**
     * Limite les valeurs de scroll aux bornes calculees.
     */
    private void clampScroll() {
        scrollX = Math.max(minScrollX, Math.min(maxScrollX, scrollX));
        scrollY = Math.max(minScrollY, Math.min(maxScrollY, scrollY));
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
            return Minecraft.getInstance().player.getData(ApicaAttachments.CODEX_DATA);
        }
        return new CodexPlayerData();
    }

    private Set<String> getCompletedQuests() {
        if (Minecraft.getInstance().player != null) {
            QuestPlayerData questData = Minecraft.getInstance().player.getData(ApicaAttachments.QUEST_DATA);
            return questData.getCompletedQuests();
        }
        return new HashSet<>();
    }

    @Override
    public void onClose() {
        playSound(ApicaSounds.CODEX_CLOSE.get());
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
