/**
 * ============================================================
 * [CodexBookScreen.java]
 * Description: Écran livre du Codex - affiche le contenu détaillé d'un node
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexBookContent    | Contenu à afficher   | Sections modulaires            |
 * | CodexBookManager    | Chargement contenu   | Récupération par nodeId        |
 * | BookPageLayout      | Pagination           | Répartition sur les pages      |
 * | CodexPlayerData     | Jour relatif         | Calcul Day X dans l'en-tête    |
 * | BeemancerSounds     | Sons                 | Feedback audio                 |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexScreen (ouverture lors du clic sur un node)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.gui.screen.codex.BookPageLayout;
import com.chapeau.beemancer.common.codex.CodexNode;
import com.chapeau.beemancer.common.codex.CodexPage;
import com.chapeau.beemancer.common.codex.CodexPlayerData;
import com.chapeau.beemancer.common.codex.book.CodexBookContent;
import com.chapeau.beemancer.common.codex.book.CodexBookManager;
import com.chapeau.beemancer.common.codex.book.CodexBookSection;
import com.chapeau.beemancer.core.registry.BeemancerAttachments;
import com.chapeau.beemancer.core.registry.BeemancerSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class CodexBookScreen extends Screen {

    private static final ResourceLocation BOOK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/codex_bar.png");

    private static final int BOOK_WIDTH = 320;
    private static final int BOOK_HEIGHT = 200;
    private static final int PAGE_PADDING = 16;
    private static final int SPINE_WIDTH = 8;
    private static final int BG_COLOR = 0xFFF3E1BB;
    private static final int SPINE_COLOR = 0xFFD4B896;
    private static final int BORDER_COLOR = 0xFF8B6914;
    private static final int ARROW_COLOR = 0xFF5C3A1E;
    private static final int ARROW_HOVER_COLOR = 0xFFB8956A;
    private static final int ARROW_DISABLED_COLOR = 0xFF9C8A70;

    private final CodexNode node;
    private final CodexPage returnPage;

    private int bookX;
    private int bookY;
    private int leftPageX;
    private int rightPageX;
    private int pageWidth;
    private int pageHeight;

    private List<List<CodexBookSection>> paginatedContent;
    private int currentSpread = 0;
    private int totalSpreads = 1;

    private Button prevButton;
    private Button nextButton;
    private Button backButton;

    public CodexBookScreen(CodexNode node, CodexPage returnPage) {
        super(Component.translatable("screen.beemancer.codex_book"));
        this.node = node;
        this.returnPage = returnPage;
    }

    @Override
    protected void init() {
        super.init();

        CodexBookManager.ensureClientLoaded();

        bookX = (width - BOOK_WIDTH) / 2;
        bookY = (height - BOOK_HEIGHT) / 2;

        int halfBook = (BOOK_WIDTH - SPINE_WIDTH) / 2;
        pageWidth = halfBook - PAGE_PADDING * 2;
        pageHeight = BOOK_HEIGHT - PAGE_PADDING * 2 - 20;

        leftPageX = bookX + PAGE_PADDING;
        rightPageX = bookX + halfBook + SPINE_WIDTH + PAGE_PADDING;

        CodexBookContent content = CodexBookManager.getContent(node.getId());
        paginatedContent = BookPageLayout.paginate(content.getSections(), font, pageWidth, pageHeight);
        totalSpreads = BookPageLayout.getSpreadCount(paginatedContent.size());
        currentSpread = 0;

        createButtons();
    }

    private void createButtons() {
        if (prevButton != null) removeWidget(prevButton);
        if (nextButton != null) removeWidget(nextButton);
        if (backButton != null) removeWidget(backButton);

        int buttonY = bookY + BOOK_HEIGHT - 18;

        // Flèche retour (droite → gauche) pour revenir au codex
        backButton = Button.builder(Component.literal("\u2190 Back"), btn -> {
            playSound(BeemancerSounds.CODEX_PAGE_TURN.get());
            Minecraft.getInstance().setScreen(new CodexScreen());
        }).bounds(bookX + 4, buttonY, 50, 14).build();
        addRenderableWidget(backButton);

        // Flèche page précédente
        prevButton = Button.builder(Component.literal("<"), btn -> {
            if (currentSpread > 0) {
                currentSpread--;
                playSound(BeemancerSounds.CODEX_PAGE_TURN.get());
                updateButtonStates();
            }
        }).bounds(bookX + BOOK_WIDTH / 2 - 30, buttonY, 20, 14).build();
        addRenderableWidget(prevButton);

        // Flèche page suivante
        nextButton = Button.builder(Component.literal(">"), btn -> {
            if (currentSpread < totalSpreads - 1) {
                currentSpread++;
                playSound(BeemancerSounds.CODEX_PAGE_TURN.get());
                updateButtonStates();
            }
        }).bounds(bookX + BOOK_WIDTH / 2 + 10, buttonY, 20, 14).build();
        addRenderableWidget(nextButton);

        updateButtonStates();
    }

    private void updateButtonStates() {
        prevButton.active = currentSpread > 0;
        nextButton.active = currentSpread < totalSpreads - 1;
        prevButton.visible = totalSpreads > 1;
        nextButton.visible = totalSpreads > 1;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        // Fond du livre
        graphics.fill(bookX, bookY, bookX + BOOK_WIDTH, bookY + BOOK_HEIGHT, BG_COLOR);

        // Bordure
        graphics.fill(bookX, bookY, bookX + BOOK_WIDTH, bookY + 1, BORDER_COLOR);
        graphics.fill(bookX, bookY + BOOK_HEIGHT - 1, bookX + BOOK_WIDTH, bookY + BOOK_HEIGHT, BORDER_COLOR);
        graphics.fill(bookX, bookY, bookX + 1, bookY + BOOK_HEIGHT, BORDER_COLOR);
        graphics.fill(bookX + BOOK_WIDTH - 1, bookY, bookX + BOOK_WIDTH, bookY + BOOK_HEIGHT, BORDER_COLOR);

        // Reliure centrale
        int spineX = bookX + (BOOK_WIDTH - SPINE_WIDTH) / 2;
        graphics.fill(spineX, bookY, spineX + SPINE_WIDTH, bookY + BOOK_HEIGHT, SPINE_COLOR);

        // Rendu du contenu des pages
        CodexPlayerData playerData = getPlayerData();
        String nodeTitle = node.getTitle().getString();
        long relativeDay = playerData.getRelativeDay(node.getFullId());

        int[] spreadPages = BookPageLayout.getSpreadPages(currentSpread);

        // Page gauche
        if (spreadPages[0] < paginatedContent.size()) {
            renderPageSections(graphics, paginatedContent.get(spreadPages[0]),
                    leftPageX, bookY + PAGE_PADDING, nodeTitle, relativeDay);
        }

        // Page droite
        if (spreadPages[1] < paginatedContent.size()) {
            renderPageSections(graphics, paginatedContent.get(spreadPages[1]),
                    rightPageX, bookY + PAGE_PADDING, nodeTitle, relativeDay);
        }

        // Numéro de page
        if (totalSpreads > 1) {
            String pageNum = (currentSpread + 1) + "/" + totalSpreads;
            int numWidth = font.width(pageNum);
            graphics.drawString(font, pageNum,
                    bookX + BOOK_WIDTH / 2 - numWidth / 2,
                    bookY + BOOK_HEIGHT - 16, ARROW_DISABLED_COLOR, false);
        }

        // Boutons
        backButton.render(graphics, mouseX, mouseY, partialTick);
        prevButton.render(graphics, mouseX, mouseY, partialTick);
        nextButton.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPageSections(GuiGraphics graphics, List<CodexBookSection> sections,
                                     int pageX, int pageY, String nodeTitle, long relativeDay) {
        int currentY = pageY;
        for (CodexBookSection section : sections) {
            section.render(graphics, font, pageX, currentY, pageWidth, nodeTitle, relativeDay);
            currentY += section.getHeight(font, pageWidth);
        }
    }

    private CodexPlayerData getPlayerData() {
        if (Minecraft.getInstance().player != null) {
            return Minecraft.getInstance().player.getData(BeemancerAttachments.CODEX_DATA);
        }
        return new CodexPlayerData();
    }

    private void playSound(net.minecraft.sounds.SoundEvent sound) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(sound, 0.5f, 1.0f);
        }
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
