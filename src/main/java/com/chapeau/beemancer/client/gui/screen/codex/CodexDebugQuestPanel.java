/**
 * ============================================================
 * [CodexDebugQuestPanel.java]
 * Description: Panneau debug listant les quetes des nodes de la page courante
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexNode           | Donnees des nodes    | Quest IDs et node IDs          |
 * | QuestManager        | Acces aux quetes     | Details et conditions          |
 * | Quest               | Definition quete     | Type, item, machine, species   |
 * | DebugWandItem       | Flag debug           | Activation du panneau          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexScreen (rendu et interaction du panneau debug)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.codex;

import com.chapeau.beemancer.common.codex.CodexNode;
import com.chapeau.beemancer.common.quest.Quest;
import com.chapeau.beemancer.common.quest.QuestManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CodexDebugQuestPanel {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_GAP = -40;
    private static final int LINE_HEIGHT = 11;
    private static final int ENTRY_SPACING = 4;
    private static final int LINES_PER_ENTRY = 3;
    private static final int ENTRY_HEIGHT = LINES_PER_ENTRY * LINE_HEIGHT + ENTRY_SPACING;
    private static final int ARROW_HEIGHT = 12;
    private static final int PADDING = 4;
    private static final int TITLE_COLOR = 0xFF00FFFF;
    private static final int BG_COLOR = 0xCC1A1A1A;
    private static final int BORDER_COLOR = 0xFF555555;
    private static final int COMPLETED_COLOR = 0xFF00FF00;
    private static final int INCOMPLETE_COLOR = 0xFFFF4444;
    private static final int LABEL_COLOR = 0xFF888888;
    private static final int VALUE_COLOR = 0xFFCCCCCC;
    private static final int SEPARATOR_COLOR = 0xFF333333;
    private static final int ARROW_ACTIVE_COLOR = 0xFFFFFFFF;
    private static final int ARROW_INACTIVE_COLOR = 0xFF555555;

    private int panelX;
    private int panelY;
    private int panelHeight;
    private int scrollOffset = 0;
    private int maxVisible = 0;

    private final List<QuestEntry> entries = new ArrayList<>();

    /**
     * Reconstruit la liste des quetes a partir des nodes de la page courante.
     * Appele a chaque changement de page.
     */
    public void rebuild(List<CodexNode> nodes, Set<String> completedQuests,
                        int frameRightX, int frameY, int frameHeight) {
        this.panelX = frameRightX + PANEL_GAP;
        this.panelY = frameY;
        this.panelHeight = frameHeight;
        this.scrollOffset = 0;

        QuestManager.ensureClientLoaded();

        entries.clear();
        for (CodexNode node : nodes) {
            if (node.hasQuest()) {
                boolean completed = completedQuests.contains(node.getQuestId());
                Quest quest = QuestManager.getQuest(node.getQuestId());
                String condition = buildConditionText(quest);
                entries.add(new QuestEntry(node.getId(), node.getQuestId(), completed, condition));
            }
        }
    }

    /**
     * Met a jour le statut de completion sans reconstruire la liste.
     */
    public void updateCompletion(Set<String> completedQuests) {
        for (int i = 0; i < entries.size(); i++) {
            QuestEntry e = entries.get(i);
            boolean completed = completedQuests.contains(e.questId());
            if (completed != e.completed()) {
                entries.set(i, new QuestEntry(e.nodeId(), e.questId(), completed, e.condition()));
            }
        }
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (entries.isEmpty()) return;

        // Fond du panneau
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, BG_COLOR);
        renderBorder(graphics);

        // Titre
        int titleY = panelY + PADDING;
        graphics.drawString(font, "Quests [Debug]", panelX + PADDING, titleY, TITLE_COLOR, false);

        // Fleche haut
        int arrowUpY = titleY + font.lineHeight + 2;
        boolean canScrollUp = scrollOffset > 0;
        renderArrow(graphics, font, arrowUpY, "\u25B2", canScrollUp, mouseX, mouseY);

        // Zone de liste
        int listStartY = arrowUpY + ARROW_HEIGHT;
        int listEndY = panelY + panelHeight - ARROW_HEIGHT - PADDING;
        int availableHeight = listEndY - listStartY;
        maxVisible = Math.max(1, availableHeight / ENTRY_HEIGHT);

        int maxScroll = Math.max(0, entries.size() - maxVisible);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        graphics.enableScissor(panelX, listStartY, panelX + PANEL_WIDTH, listEndY);
        for (int i = 0; i < maxVisible && (i + scrollOffset) < entries.size(); i++) {
            QuestEntry entry = entries.get(i + scrollOffset);
            int entryY = listStartY + i * ENTRY_HEIGHT;

            // Indicateur de statut
            String status = entry.completed() ? "\u2713" : "\u2717";
            int statusColor = entry.completed() ? COMPLETED_COLOR : INCOMPLETE_COLOR;
            graphics.drawString(font, status, panelX + PADDING, entryY, statusColor, false);

            // Ligne 1: Node
            String nodeLabel = "Node: ";
            int labelEnd = panelX + PADDING + 12;
            graphics.drawString(font, nodeLabel, labelEnd, entryY, LABEL_COLOR, false);
            graphics.drawString(font, entry.nodeId(), labelEnd + font.width(nodeLabel), entryY, VALUE_COLOR, false);

            // Ligne 2: Quest
            int line2Y = entryY + LINE_HEIGHT;
            String questLabel = "Quest: ";
            graphics.drawString(font, questLabel, labelEnd, line2Y, LABEL_COLOR, false);
            String questText = truncate(font, entry.questId(), PANEL_WIDTH - PADDING * 2 - 12 - font.width(questLabel));
            graphics.drawString(font, questText, labelEnd + font.width(questLabel), line2Y, VALUE_COLOR, false);

            // Ligne 3: Condition
            int line3Y = entryY + LINE_HEIGHT * 2;
            String condLabel = "Cond: ";
            graphics.drawString(font, condLabel, labelEnd, line3Y, LABEL_COLOR, false);
            String condText = truncate(font, entry.condition(), PANEL_WIDTH - PADDING * 2 - 12 - font.width(condLabel));
            graphics.drawString(font, condText, labelEnd + font.width(condLabel), line3Y, 0xFFFFAA00, false);

            // Separateur entre les entrees
            int sepY = entryY + ENTRY_HEIGHT - 2;
            graphics.fill(panelX + PADDING, sepY, panelX + PANEL_WIDTH - PADDING, sepY + 1, SEPARATOR_COLOR);
        }
        graphics.disableScissor();

        // Fleche bas
        int arrowDownY = listEndY;
        boolean canScrollDown = scrollOffset < maxScroll;
        renderArrow(graphics, font, arrowDownY, "\u25BC", canScrollDown, mouseX, mouseY);

        // Compteur
        int endIdx = Math.min(scrollOffset + maxVisible, entries.size());
        String count = (scrollOffset + 1) + "-" + endIdx + "/" + entries.size();
        graphics.drawString(font, count,
                panelX + PANEL_WIDTH - font.width(count) - PADDING,
                panelY + panelHeight - font.lineHeight - 2, 0xFF888888, false);
    }

    private void renderBorder(GuiGraphics graphics) {
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 1, BORDER_COLOR);
        graphics.fill(panelX, panelY + panelHeight - 1, panelX + PANEL_WIDTH, panelY + panelHeight, BORDER_COLOR);
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, BORDER_COLOR);
        graphics.fill(panelX + PANEL_WIDTH - 1, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, BORDER_COLOR);
    }

    private void renderArrow(GuiGraphics graphics, Font font, int y, String arrow,
                              boolean active, int mouseX, int mouseY) {
        int centerX = panelX + PANEL_WIDTH / 2;
        int color = active ? ARROW_ACTIVE_COLOR : ARROW_INACTIVE_COLOR;

        // Surbrillance si survole et actif
        if (active && isInArrowZone(mouseX, mouseY, y)) {
            color = 0xFFFFFF00;
        }

        graphics.drawCenteredString(font, arrow, centerX, y, color);
    }

    /**
     * Gere les clics sur les fleches de navigation.
     * @return true si le clic a ete consomme
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button, Font font) {
        if (button != 0 || entries.isEmpty()) return false;
        if (!isInPanel(mouseX, mouseY)) return false;

        int titleY = panelY + PADDING;
        int arrowUpY = titleY + font.lineHeight + 2;
        int listEndY = panelY + panelHeight - ARROW_HEIGHT - PADDING;
        int arrowDownY = listEndY;

        // Clic fleche haut
        if (isInArrowZone(mouseX, mouseY, arrowUpY) && scrollOffset > 0) {
            scrollOffset--;
            return true;
        }

        // Clic fleche bas
        int maxScroll = Math.max(0, entries.size() - maxVisible);
        if (isInArrowZone(mouseX, mouseY, arrowDownY) && scrollOffset < maxScroll) {
            scrollOffset++;
            return true;
        }

        return true;
    }

    /**
     * Gere le scroll molette sur le panneau.
     * @return true si le scroll a ete consomme
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (entries.isEmpty() || !isInPanel(mouseX, mouseY)) return false;

        int maxScroll = Math.max(0, entries.size() - maxVisible);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) delta));
        return true;
    }

    private boolean isInPanel(double mouseX, double mouseY) {
        return mouseX >= panelX && mouseX < panelX + PANEL_WIDTH
                && mouseY >= panelY && mouseY < panelY + panelHeight;
    }

    private boolean isInArrowZone(double mouseX, double mouseY, int arrowY) {
        return mouseX >= panelX && mouseX < panelX + PANEL_WIDTH
                && mouseY >= arrowY && mouseY < arrowY + ARROW_HEIGHT;
    }

    /**
     * Construit le texte de condition a partir d'un Quest.
     */
    private static String buildConditionText(Quest quest) {
        if (quest == null) return "???";

        StringBuilder sb = new StringBuilder();
        sb.append(quest.getType().name());

        if (quest.getTargetItem() != null) {
            String item = quest.getTargetItem().toString();
            sb.append(" | item:").append(item);
        }
        if (quest.getTargetMachine() != null) {
            sb.append(" | machine:").append(quest.getTargetMachine());
        }
        if (quest.getTargetSpecies() != null) {
            sb.append(" | species:").append(quest.getTargetSpecies());
        }
        if (quest.getTargetCount() > 1) {
            sb.append(" x").append(quest.getTargetCount());
        }

        return sb.toString();
    }

    private static String truncate(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        return font.plainSubstrByWidth(text, maxWidth - font.width("..")) + "..";
    }

    private record QuestEntry(String nodeId, String questId, boolean completed, String condition) {}
}
