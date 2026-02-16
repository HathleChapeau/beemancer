/**
 * ============================================================
 * [CodexTabButtonWidget.java]
 * Description: Bouton d'onglet custom du Codex avec fond texture et icone item
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexPage           | Enum des pages       | Nom et couleur de l'onglet     |
 * | Apica               | MOD_ID               | Chemin texture                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexScreen (creation des onglets)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.widget;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.codex.CodexPage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class CodexTabButtonWidget extends AbstractWidget {

    private static final ResourceLocation TAB_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/gui/codex/codex_tab_button.png");

    public static final int TEX_WIDTH = 45;
    public static final int TEX_HEIGHT = 35;

    private static final int HOVER_OVERLAY = 0x1A000000;

    private final CodexPage page;
    private final ItemStack iconStack;
    private final Runnable onClick;
    private boolean selected;

    public CodexTabButtonWidget(int x, int y, CodexPage page, ItemStack iconStack, Runnable onClick) {
        super(x, y, TEX_WIDTH, TEX_HEIGHT, page.getDisplayName());
        this.page = page;
        this.iconStack = iconStack;
        this.onClick = onClick;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public CodexPage getPage() {
        return page;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Fond texture
        graphics.blit(TAB_TEXTURE, getX(), getY(), 0, 0, TEX_WIDTH, TEX_HEIGHT, TEX_WIDTH, TEX_HEIGHT);

        // Hover: assombrir a 0.90 (overlay 10% noir)
        if (isHovered && !selected) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, HOVER_OVERLAY);
        }

        // Item centre
        int itemX = getX() + (TEX_WIDTH - 16) / 2;
        int itemY = getY() + (TEX_HEIGHT - 16) / 2;
        graphics.renderItem(iconStack, itemX, itemY);

        RenderSystem.disableBlend();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!selected) {
            onClick.run();
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
