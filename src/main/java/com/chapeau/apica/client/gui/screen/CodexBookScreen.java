/**
 * ============================================================
 * [CodexBookScreen.java]
 * Description: Ecran livre du Codex - affiche le contenu detaille d'un node
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexBookContent    | Contenu a afficher   | Sections modulaires            |
 * | CodexBookManager    | Chargement contenu   | Recuperation par nodeId        |
 * | BookPageLayout      | Split gauche/droite  | Separation au page_break       |
 * | CodexPlayerData     | Jour relatif         | Calcul Day X dans l'en-tete    |
 * | StickyNote          | Notes collantes      | Boutons et overlay             |
 * | ApicaSounds     | Sons                 | Feedback audio                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexScreen (ouverture lors du clic sur un node)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.gui.screen.codex.BookPageLayout;
import com.mojang.blaze3d.systems.RenderSystem;
import com.chapeau.apica.common.codex.CodexNode;
import com.chapeau.apica.common.codex.CodexPage;
import com.chapeau.apica.common.codex.CodexPlayerData;
import com.chapeau.apica.common.codex.book.CodexBookContent;
import com.chapeau.apica.common.codex.book.CodexBookManager;
import com.chapeau.apica.common.codex.book.CodexBookSection;
import com.chapeau.apica.common.codex.book.CraftSection;
import com.chapeau.apica.common.codex.book.HeaderSection;
import com.chapeau.apica.common.codex.book.StickyNote;
import com.chapeau.apica.common.item.debug.DebugWandItem;
import com.chapeau.apica.common.quest.Quest;
import com.chapeau.apica.common.quest.QuestManager;
import com.chapeau.apica.common.quest.QuestPlayerData;
import com.chapeau.apica.core.registry.ApicaAttachments;
import com.chapeau.apica.core.registry.ApicaSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CodexBookScreen extends Screen {

    private static final ResourceLocation BOOK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/gui/codex_book.png");

    // Dimensions du livre (taille native de la texture)
    private static final int BOOK_WIDTH = 330;
    private static final int BOOK_HEIGHT = 224;

    // Marges interieures (proportionnelles a la texture upscalee)
    private static final int MARGIN_LEFT = 22;
    private static final int MARGIN_RIGHT = 20;
    private static final int MARGIN_TOP = 16;
    private static final int MARGIN_BOTTOM = 22;
    // La reliure au centre de la texture
    private static final int SPINE_WIDTH = 25;

    private static final int PAGE_PADDING = 4;
    private static final float CONTENT_SCALE = 0.93f;
    private static final int RIGHT_PAGE_EXTRA_MARGIN = 3;

    // Sticky note constants
    private static final int NOTE_BUTTON_SIZE = 32;
    private static final int NOTE_BUTTON_GAP = 1;
    private static final int NOTE_BUTTON_OFFSET_X = -14;
    private static final int NOTE_OVERLAY_BG = 0xA0000000;

    private static final ResourceLocation[] NOTE_TEXTURES = {
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/gui/codex/codex_book/stickynote_1.png"),
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/gui/codex/codex_book/stickynote_2.png"),
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/gui/codex/codex_book/stickynote_3.png")
    };
    private static final int[][] NOTE_TEX_SIZES = {
        {31, 31}, {32, 34}, {32, 33}
    };
    private static final int NOTE_WIDTH = 180;
    private static final int NOTE_HEIGHT = 130;
    private static final int NOTE_BORDER_COLOR = 0xFF5C3A1E;
    private static final int NOTE_TITLE_COLOR = 0xFF3B2A1A;

    private final CodexNode node;
    private final CodexPage returnPage;

    private int bookX;
    private int bookY;
    private int leftPageX;
    private int rightPageX;
    private int pageWidth;

    private List<CodexBookSection> leftSections;
    private List<CodexBookSection> rightSections;

    private Button backButton;

    private List<StickyNote> stickyNotes = List.of();
    private List<ItemStack> noteIconStacks = List.of();
    private List<CraftSection> noteCraftSections = List.of();
    private int openedNoteIndex = -1;

    public CodexBookScreen(CodexNode node, CodexPage returnPage) {
        super(Component.translatable("screen.apica.codex_book"));
        this.node = node;
        this.returnPage = returnPage;
    }

    @Override
    protected void init() {
        super.init();

        CodexBookManager.ensureClientLoaded();
        playSound(ApicaSounds.CODEX_PAGE_TURN.get());

        bookX = (width - BOOK_WIDTH) / 2;
        bookY = (height - BOOK_HEIGHT) / 2;

        // Calculer les zones de page a partir des marges de la texture
        int spineX = bookX + (BOOK_WIDTH - SPINE_WIDTH) / 2;

        leftPageX = bookX + MARGIN_LEFT + PAGE_PADDING;
        rightPageX = spineX + SPINE_WIDTH + PAGE_PADDING;
        pageWidth = (spineX - bookX - MARGIN_LEFT) - PAGE_PADDING * 2;

        CodexBookContent content = CodexBookManager.getContent(node.getId());

        // Pass node reference to HeaderSections for breeding display
        for (CodexBookSection section : content.getSections()) {
            if (section instanceof HeaderSection header) {
                header.setNode(node);
            }
        }

        List<List<CodexBookSection>> split = BookPageLayout.splitAtPageBreak(content.getSections());
        leftSections = split.get(0);
        rightSections = split.get(1);
        stickyNotes = content.getStickyNotes();
        resolveNoteData();
        openedNoteIndex = -1;

        createBackButton();
    }

    private void createBackButton() {
        if (backButton != null) removeWidget(backButton);

        int buttonY = bookY + BOOK_HEIGHT - 18;

        backButton = Button.builder(Component.literal("\u2190 Back"), btn -> {
            playSound(ApicaSounds.CODEX_PAGE_TURN.get());
            Minecraft.getInstance().setScreen(new CodexScreen(returnPage));
        }).bounds(bookX + BOOK_WIDTH / 2 - 25, buttonY, 50, 14).build();
        addRenderableWidget(backButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        // Fond du livre (texture codex_book.png)
        graphics.blit(BOOK_TEXTURE, bookX, bookY, BOOK_WIDTH, BOOK_HEIGHT,
                0, 0, BOOK_WIDTH, BOOK_HEIGHT, BOOK_WIDTH, BOOK_HEIGHT);

        // Rendu du contenu des pages
        CodexPlayerData playerData = getPlayerData();
        Set<String> completedQuests = getCompletedQuests();
        String nodeTitle = node.getTitle().getString();
        long relativeDay = playerData.getRelativeDay(node.getFullId());

        int contentTopY = bookY + MARGIN_TOP + PAGE_PADDING;

        // Page gauche
        if (!leftSections.isEmpty()) {
            renderPageSections(graphics, leftSections,
                    leftPageX, contentTopY, pageWidth, nodeTitle, relativeDay, completedQuests);
        }

        // Page droite
        if (!rightSections.isEmpty()) {
            renderPageSections(graphics, rightSections,
                    rightPageX + RIGHT_PAGE_EXTRA_MARGIN, contentTopY, pageWidth, nodeTitle, relativeDay, completedQuests);
        }

        // Bouton retour
        backButton.render(graphics, mouseX, mouseY, partialTick);

        // Sticky note buttons (a droite du livre)
        renderStickyNoteButtons(graphics, mouseX, mouseY);

        // Sticky note overlay (par dessus tout)
        // flush() force le GPU a dessiner tout le contenu batche (items 3D inclus)
        // AVANT de rendre l'overlay, garantissant l'ordre de dessin correct
        if (openedNoteIndex >= 0 && openedNoteIndex < stickyNotes.size()) {
            graphics.flush();
            renderStickyNoteOverlay(graphics, stickyNotes.get(openedNoteIndex), openedNoteIndex);
        }

        // Debug: section quests panel
        if (DebugWandItem.displayDebug) {
            renderDebugSectionQuests(graphics, completedQuests);
        }
    }

    private void renderPageSections(GuiGraphics graphics, List<CodexBookSection> sections,
                                     int pageX, int pageY, int effectiveWidth, String nodeTitle,
                                     long relativeDay, Set<String> completedQuests) {
        graphics.pose().pushPose();
        graphics.pose().scale(CONTENT_SCALE, CONTENT_SCALE, 1.0f);

        int scaledX = Math.round(pageX / CONTENT_SCALE);
        int scaledY = Math.round(pageY / CONTENT_SCALE);
        int scaledWidth = Math.round(effectiveWidth / CONTENT_SCALE);

        int currentY = scaledY;
        for (CodexBookSection section : sections) {
            int height = section.getHeight(font, scaledWidth);
            String sectionQuest = section.getQuestId();
            if (sectionQuest != null && !completedQuests.contains(sectionQuest)) {
                currentY += height;
                continue;
            }
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            section.render(graphics, font, scaledX, currentY, scaledWidth, nodeTitle, relativeDay);
            currentY += height;
        }

        graphics.pose().popPose();
    }

    // ==================== Sticky Notes ====================

    private void resolveNoteData() {
        List<ItemStack> icons = new ArrayList<>();
        List<CraftSection> crafts = new ArrayList<>();
        for (StickyNote note : stickyNotes) {
            if (note.craftItem() != null && !note.craftItem().isEmpty()) {
                ResourceLocation loc = ResourceLocation.parse(note.craftItem());
                var item = BuiltInRegistries.ITEM.get(loc);
                icons.add(item != null ? new ItemStack(item) : ItemStack.EMPTY);
                crafts.add(new CraftSection(note.craftItem(), 0));
            } else {
                icons.add(ItemStack.EMPTY);
                crafts.add(null);
            }
        }
        noteIconStacks = icons;
        noteCraftSections = crafts;
    }

    private void renderStickyNoteButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        if (stickyNotes.isEmpty()) return;

        int btnX = bookX + BOOK_WIDTH + NOTE_BUTTON_OFFSET_X;
        int btnY = bookY + MARGIN_TOP;

        for (int i = 0; i < stickyNotes.size(); i++) {
            int texIdx = i % NOTE_TEXTURES.length;
            int texW = NOTE_TEX_SIZES[texIdx][0];
            int texH = NOTE_TEX_SIZES[texIdx][1];
            int y = btnY + i * (NOTE_BUTTON_SIZE + NOTE_BUTTON_GAP);

            boolean hovered = mouseX >= btnX && mouseX < btnX + texW
                    && mouseY >= y && mouseY < y + texH;

            // Teinte hover : plus foncé quand survolé
            if (hovered) {
                graphics.setColor(0.85f, 0.85f, 0.85f, 1.0f);
            }

            // Texture background (blend toujours actif pour l'alpha des PNG)
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.blit(NOTE_TEXTURES[texIdx], btnX, y, texW, texH,
                    0, 0, texW, texH, texW, texH);

            // Reset de la teinte
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

            // Item icon (centered in texture)
            if (i < noteIconStacks.size() && !noteIconStacks.get(i).isEmpty()) {
                int itemX = btnX + (texW - 16) / 2;
                int itemY = y + (texH - 16) / 2 + 2;
                graphics.renderItem(noteIconStacks.get(i), itemX, itemY);
            }
        }
    }

    private void renderStickyNoteOverlay(GuiGraphics graphics, StickyNote note, int noteIndex) {
        // Desactiver le depth test pour que l'overlay 2D passe devant
        // tous les modeles 3D rendus par renderItem() sur la page
        RenderSystem.disableDepthTest();

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 600);

        // Semi-transparent dark overlay (clicking here closes the note)
        graphics.fill(0, 0, width, height, NOTE_OVERLAY_BG);

        // Note centered on screen
        int noteX = (width - NOTE_WIDTH) / 2;
        int noteY = (height - NOTE_HEIGHT) / 2;

        // Note background
        graphics.fill(noteX, noteY, noteX + NOTE_WIDTH, noteY + NOTE_HEIGHT, note.color());

        // Note border (2px)
        int b = NOTE_BORDER_COLOR;
        graphics.fill(noteX, noteY, noteX + NOTE_WIDTH, noteY + 2, b);
        graphics.fill(noteX, noteY + NOTE_HEIGHT - 2, noteX + NOTE_WIDTH, noteY + NOTE_HEIGHT, b);
        graphics.fill(noteX, noteY, noteX + 2, noteY + NOTE_HEIGHT, b);
        graphics.fill(noteX + NOTE_WIDTH - 2, noteY, noteX + NOTE_WIDTH, noteY + NOTE_HEIGHT, b);

        // Title (from note title, or auto-resolve from item name)
        String displayTitle = note.title();
        if ((displayTitle == null || displayTitle.isEmpty())
                && noteIndex < noteIconStacks.size()
                && !noteIconStacks.get(noteIndex).isEmpty()) {
            displayTitle = noteIconStacks.get(noteIndex).getHoverName().getString();
        }
        graphics.drawString(font, displayTitle,
                noteX + 8, noteY + 8, NOTE_TITLE_COLOR, false);

        // Separator
        graphics.fill(noteX + 6, noteY + 8 + font.lineHeight + 2,
                noteX + NOTE_WIDTH - 6, noteY + 8 + font.lineHeight + 3,
                NOTE_BORDER_COLOR);

        // Reactiver le depth test pour les items 3D de la note
        RenderSystem.enableDepthTest();

        // Craft section rendering
        if (noteIndex < noteCraftSections.size() && noteCraftSections.get(noteIndex) != null) {
            CraftSection craft = noteCraftSections.get(noteIndex);
            int craftY = noteY + 8 + font.lineHeight + 8;
            int craftWidth = NOTE_WIDTH - 16;
            craft.render(graphics, font, noteX + 8, craftY, craftWidth, "", -1);
        }

        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Si une note est ouverte, cliquer ferme la note
        if (openedNoteIndex >= 0) {
            int noteX = (width - NOTE_WIDTH) / 2;
            int noteY = (height - NOTE_HEIGHT) / 2;
            if (mouseX >= noteX && mouseX < noteX + NOTE_WIDTH
                    && mouseY >= noteY && mouseY < noteY + NOTE_HEIGHT) {
                return true;
            }
            openedNoteIndex = -1;
            return true;
        }

        // Check sticky note button clicks
        if (!stickyNotes.isEmpty()) {
            int btnX = bookX + BOOK_WIDTH + NOTE_BUTTON_OFFSET_X;
            int btnY = bookY + MARGIN_TOP;

            for (int i = 0; i < stickyNotes.size(); i++) {
                int texIdx = i % NOTE_TEXTURES.length;
                int texW = NOTE_TEX_SIZES[texIdx][0];
                int texH = NOTE_TEX_SIZES[texIdx][1];
                int y = btnY + i * (NOTE_BUTTON_SIZE + NOTE_BUTTON_GAP);
                if (mouseX >= btnX && mouseX < btnX + texW
                        && mouseY >= y && mouseY < y + texH) {
                    openedNoteIndex = i;
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (openedNoteIndex >= 0) {
            if (keyCode == 256) {
                openedNoteIndex = -1;
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static int brighten(int color, int amount) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, ((color >> 16) & 0xFF) + amount);
        int g = Math.min(255, ((color >> 8) & 0xFF) + amount);
        int b = Math.min(255, (color & 0xFF) + amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ==================== Debug ====================

    private void renderDebugSectionQuests(GuiGraphics graphics, Set<String> completedQuests) {
        QuestManager.ensureClientLoaded();

        List<CodexBookSection> allSections = new ArrayList<>();
        allSections.addAll(leftSections);
        allSections.addAll(rightSections);

        // Collect sections with quest IDs
        List<String[]> questInfos = new ArrayList<>();
        for (CodexBookSection section : allSections) {
            String questId = section.getQuestId();
            if (questId != null) {
                String sectionType = section.getType().getId();
                boolean completed = completedQuests.contains(questId);
                Quest quest = QuestManager.getQuest(questId);
                String condition = quest != null ? quest.getType().name() : "???";
                if (quest != null && quest.getTargetMachine() != null) {
                    condition += " | " + quest.getTargetMachine();
                }
                if (quest != null && quest.getTargetTag() != null) {
                    condition += " | #" + quest.getTargetTag();
                }
                questInfos.add(new String[]{
                        sectionType, questId, completed ? "\u2713" : "\u2717", condition,
                        completed ? "completed" : "incomplete"
                });
            }
        }

        if (questInfos.isEmpty()) return;

        int lineH = font.lineHeight + 2;
        int panelH = 4 + lineH + questInfos.size() * lineH + 4;
        int panelW = 260;
        int px = bookX - panelW - 10;
        int py = bookY;

        // Panel background
        graphics.fill(px, py, px + panelW, py + panelH, 0xCC1A1A1A);
        graphics.fill(px, py, px + panelW, py + 1, 0xFF555555);
        graphics.fill(px, py + panelH - 1, px + panelW, py + panelH, 0xFF555555);
        graphics.fill(px, py, px + 1, py + panelH, 0xFF555555);
        graphics.fill(px + panelW - 1, py, px + panelW, py + panelH, 0xFF555555);

        // Title
        graphics.drawString(font, "Section Quests [Debug]", px + 4, py + 4, 0xFF00FFFF, false);

        // Entries
        int entryY = py + 4 + lineH;
        for (String[] info : questInfos) {
            int statusColor = "completed".equals(info[4]) ? 0xFF00FF00 : 0xFFFF4444;
            graphics.drawString(font, info[2], px + 4, entryY, statusColor, false);
            String line = "[" + info[0] + "] " + info[1] + " | " + info[3];
            if (font.width(line) > panelW - 20) {
                line = font.plainSubstrByWidth(line, panelW - 24) + "..";
            }
            graphics.drawString(font, line, px + 16, entryY, 0xFFCCCCCC, false);
            entryY += lineH;
        }
    }

    // ==================== Utils ====================

    private Set<String> getCompletedQuests() {
        if (Minecraft.getInstance().player != null) {
            QuestPlayerData questData = Minecraft.getInstance().player.getData(ApicaAttachments.QUEST_DATA);
            return questData.getCompletedQuests();
        }
        return new HashSet<>();
    }

    private CodexPlayerData getPlayerData() {
        if (Minecraft.getInstance().player != null) {
            return Minecraft.getInstance().player.getData(ApicaAttachments.CODEX_DATA);
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
        playSound(ApicaSounds.CODEX_CLOSE.get());
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
