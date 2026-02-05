/**
 * ============================================================
 * [CodexNodeWidget.java]
 * Description: Widget pour afficher un node individuel du Codex avec nom
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexNode           | Données du node      | Affichage et interaction       |
 * | CodexNodeCategory   | Style visuel         | Sélection du background        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexScreen (création et rendu des nodes)
 * - StandardPageRenderer (rendu des nodes)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.widget;

import com.chapeau.beemancer.common.codex.CodexNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class CodexNodeWidget extends AbstractWidget {
    public static final int NODE_SIZE = 20;
    public static final int NODE_WIDTH = 80;
    public static final int NODE_HEIGHT = 30;

    private final CodexNode node;
    private final String displayName;
    private boolean unlocked;
    private boolean canUnlock;
    private boolean hovered;

    public CodexNodeWidget(CodexNode node, int screenX, int screenY, boolean unlocked, boolean canUnlock) {
        super(screenX, screenY, NODE_WIDTH, NODE_HEIGHT, node.getTitle());
        this.node = node;
        this.unlocked = unlocked;
        this.canUnlock = canUnlock;

        // Extraire le nom d'affichage depuis l'ID
        String id = node.getId();
        this.displayName = formatDisplayName(id);
    }

    private String formatDisplayName(String id) {
        // Convertir snake_case en Title Case
        String[] parts = id.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (result.length() > 0) result.append(" ");
                result.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    result.append(part.substring(1));
                }
            }
        }
        return result.toString();
    }

    public CodexNode getNode() {
        return node;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setCanUnlock(boolean canUnlock) {
        this.canUnlock = canUnlock;
    }

    public boolean canUnlock() {
        return canUnlock;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.hovered = isMouseOver(mouseX, mouseY);

        // Dessiner le fond du node
        renderBackground(graphics);

        // Dessiner le nom
        renderName(graphics);

        // Effet de survol
        if (hovered && canUnlock && !unlocked) {
            renderGlow(graphics);
        }
    }

    private void renderBackground(GuiGraphics graphics) {
        int bgColor;
        int borderColor;

        if (unlocked) {
            bgColor = 0xFF2C2C2C;     // Fond sombre
            borderColor = 0xFFF1C40F; // Bordure dorée
        } else if (canUnlock) {
            bgColor = 0xFF1A1A1A;     // Fond très sombre
            borderColor = 0xFF888888; // Bordure grise
        } else {
            bgColor = 0xFF0F0F0F;     // Fond noir
            borderColor = 0xFF444444; // Bordure sombre
        }

        int x = getX();
        int y = getY();

        // Fond
        graphics.fill(x, y, x + NODE_WIDTH, y + NODE_HEIGHT, bgColor);

        // Bordure (2px)
        graphics.fill(x, y, x + NODE_WIDTH, y + 2, borderColor);                           // Haut
        graphics.fill(x, y + NODE_HEIGHT - 2, x + NODE_WIDTH, y + NODE_HEIGHT, borderColor); // Bas
        graphics.fill(x, y, x + 2, y + NODE_HEIGHT, borderColor);                           // Gauche
        graphics.fill(x + NODE_WIDTH - 2, y, x + NODE_WIDTH, y + NODE_HEIGHT, borderColor); // Droite
    }

    private void renderName(GuiGraphics graphics) {
        Font font = Minecraft.getInstance().font;
        int textColor = unlocked ? 0xFFFFFFFF : 0xFF888888;

        // Centrer le texte
        int textWidth = font.width(displayName);
        int textX = getX() + (NODE_WIDTH - textWidth) / 2;
        int textY = getY() + (NODE_HEIGHT - font.lineHeight) / 2;

        // Si le texte est trop long, le réduire
        String textToDraw = displayName;
        if (textWidth > NODE_WIDTH - 8) {
            // Tronquer et ajouter "..."
            while (font.width(textToDraw + "...") > NODE_WIDTH - 8 && textToDraw.length() > 1) {
                textToDraw = textToDraw.substring(0, textToDraw.length() - 1);
            }
            textToDraw += "...";
            textWidth = font.width(textToDraw);
            textX = getX() + (NODE_WIDTH - textWidth) / 2;
        }

        graphics.drawString(font, textToDraw, textX, textY, textColor, false);
    }

    private void renderGlow(GuiGraphics graphics) {
        // Effet de lueur jaune autour du node
        int x = getX();
        int y = getY();
        graphics.fill(x - 2, y - 2, x + NODE_WIDTH + 2, y + NODE_HEIGHT + 2, 0x40FFFF00);
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hovered) {
            Minecraft mc = Minecraft.getInstance();

            if (unlocked) {
                graphics.renderTooltip(mc.font, java.util.List.of(
                    Component.literal(displayName).withStyle(style -> style.withBold(true)),
                    node.getDescription()
                ), java.util.Optional.empty(), mouseX, mouseY);
            } else if (canUnlock) {
                graphics.renderTooltip(mc.font, java.util.List.of(
                    Component.literal(displayName),
                    Component.translatable("codex.beemancer.click_to_unlock").withStyle(style -> style.withColor(0xAAAAAAFF))
                ), java.util.Optional.empty(), mouseX, mouseY);
            } else {
                graphics.renderTooltip(mc.font, java.util.List.of(
                    Component.translatable("codex.beemancer.locked"),
                    Component.translatable("codex.beemancer.unlock_parent_first").withStyle(style -> style.withColor(0xFF6666))
                ), java.util.Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX < getX() + NODE_WIDTH
            && mouseY >= getY() && mouseY < getY() + NODE_HEIGHT;
    }
}
