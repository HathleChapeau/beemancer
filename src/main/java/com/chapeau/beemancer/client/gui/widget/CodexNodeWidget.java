/**
 * ============================================================
 * [CodexNodeWidget.java]
 * Description: Widget pour afficher un node individuel du Codex
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
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.widget;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.codex.CodexNode;
import com.chapeau.beemancer.common.codex.CodexNodeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CodexNodeWidget extends AbstractWidget {
    public static final int NODE_SIZE = 20;
    public static final int ICON_SIZE = 14;
    private static final ResourceLocation WIDGETS_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/codex_widgets.png"
    );

    private final CodexNode node;
    private boolean unlocked;
    private boolean canUnlock;
    private boolean hovered;

    public CodexNodeWidget(CodexNode node, int screenX, int screenY, boolean unlocked, boolean canUnlock) {
        super(screenX, screenY, NODE_SIZE, NODE_SIZE, node.getTitle());
        this.node = node;
        this.unlocked = unlocked;
        this.canUnlock = canUnlock;
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

        // Draw frame background based on category and state
        renderFrame(graphics);

        // Draw icon
        renderIcon(graphics);

        // Draw glow effect if hovered and can unlock
        if (hovered && canUnlock && !unlocked) {
            renderGlow(graphics);
        }
    }

    private void renderFrame(GuiGraphics graphics) {
        CodexNodeCategory category = node.getCategory();
        int frameU = category.getTextureU();
        int frameV = category.getTextureV();

        // Offset for locked state
        if (!unlocked) {
            frameV += NODE_SIZE;
        }

        graphics.blit(WIDGETS_TEXTURE, getX(), getY(), frameU, frameV, NODE_SIZE, NODE_SIZE, 256, 256);
    }

    private void renderIcon(GuiGraphics graphics) {
        int iconX = getX() + (NODE_SIZE - ICON_SIZE) / 2;
        int iconY = getY() + (NODE_SIZE - ICON_SIZE) / 2;

        // Draw item icon
        ResourceLocation iconLoc = node.getIcon();
        
        // If it's an item texture, we need to render differently
        // For now, we'll use a simple blit assuming it's a texture path
        if (unlocked) {
            graphics.blit(iconLoc, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        } else {
            // Draw darker/greyed version for locked
            graphics.setColor(0.3f, 0.3f, 0.3f, 1.0f);
            graphics.blit(iconLoc, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private void renderGlow(GuiGraphics graphics) {
        // Simple glow effect using a highlight rectangle
        graphics.fill(getX() - 1, getY() - 1, getX() + NODE_SIZE + 1, getY() + NODE_SIZE + 1, 0x40FFFF00);
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hovered) {
            Minecraft mc = Minecraft.getInstance();
            
            if (unlocked) {
                graphics.renderTooltip(mc.font, java.util.List.of(
                    node.getTitle(),
                    node.getDescription()
                ), java.util.Optional.empty(), mouseX, mouseY);
            } else if (canUnlock) {
                graphics.renderTooltip(mc.font, java.util.List.of(
                    node.getTitle(),
                    Component.translatable("codex.beemancer.click_to_unlock")
                ), java.util.Optional.empty(), mouseX, mouseY);
            } else {
                graphics.renderTooltip(mc.font, java.util.List.of(
                    Component.translatable("codex.beemancer.locked"),
                    Component.translatable("codex.beemancer.unlock_parent_first")
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
        return mouseX >= getX() && mouseX < getX() + NODE_SIZE
            && mouseY >= getY() && mouseY < getY() + NODE_SIZE;
    }
}
