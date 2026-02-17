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

    private static final int TEX_WIDTH = 45;
    private static final int TEX_HEIGHT = 35;
    private static final float RENDER_SCALE = 0.8f;
    public static final int RENDER_WIDTH = (int) (TEX_WIDTH * RENDER_SCALE);
    public static final int RENDER_HEIGHT = (int) (TEX_HEIGHT * RENDER_SCALE);

    private final CodexPage page;
    private final ItemStack iconStack;
    private final Runnable onClick;
    private boolean selected;

    public CodexTabButtonWidget(int x, int y, CodexPage page, ItemStack iconStack, Runnable onClick) {
        super(x, y, RENDER_WIDTH, RENDER_HEIGHT, page.getDisplayName());
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

        // Teinte: assombrir a 0.90 pour hover ou selected
        if (selected || (isHovered && !selected)) {
            graphics.setColor(0.90f, 0.90f, 0.90f, 1.0f);
        }

        // Fond texture (reduit a 80%)
        graphics.pose().pushPose();
        graphics.pose().translate(getX(), getY(), 0);
        graphics.pose().scale(RENDER_SCALE, RENDER_SCALE, 1);
        graphics.blit(TAB_TEXTURE, 0, 0, 0, 0, TEX_WIDTH, TEX_HEIGHT, TEX_WIDTH, TEX_HEIGHT);
        graphics.pose().popPose();

        // Reset teinte
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Item centre dans la zone reduite
        int itemX = getX() + (RENDER_WIDTH - 16) / 2;
        int itemY = getY() + (RENDER_HEIGHT - 16) / 2;
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
