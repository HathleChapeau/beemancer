/**
 * ============================================================
 * [FluidGaugeWidget.java]
 * Description: Widget de jauge de fluide avec rendu visuel
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | GuiGraphics         | Rendu GUI            | Dessin de la jauge             |
 * | FluidStack          | Info fluide          | Affichage du contenu           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Tous les ecrans d'alchimie avec tanks
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.widget;

import com.chapeau.beemancer.Beemancer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.function.Supplier;

public class FluidGaugeWidget {
    private static final ResourceLocation WIDGETS = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/widgets.png");

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final Supplier<Integer> capacitySupplier;
    private final Supplier<FluidStack> fluidSupplier;
    private final Supplier<Integer> amountSupplier;

    // Couleurs de fluides Beemancer
    private static final int HONEY_COLOR = 0xFFE8A317;
    private static final int ROYAL_JELLY_COLOR = 0xFFFFF8DC;
    private static final int NECTAR_COLOR = 0xFFFFD700;

    public FluidGaugeWidget(int x, int y, int width, int height, int capacity,
                            Supplier<FluidStack> fluidSupplier, Supplier<Integer> amountSupplier) {
        this(x, y, width, height, () -> capacity, fluidSupplier, amountSupplier);
    }

    public FluidGaugeWidget(int x, int y, int width, int height, Supplier<Integer> capacitySupplier,
                            Supplier<FluidStack> fluidSupplier, Supplier<Integer> amountSupplier) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.capacitySupplier = capacitySupplier;
        this.fluidSupplier = fluidSupplier;
        this.amountSupplier = amountSupplier;
    }

    /**
     * Constructeur simplifie avec seulement le montant (utilise couleur par defaut)
     */
    public FluidGaugeWidget(int x, int y, int width, int height, int capacity,
                            Supplier<Integer> amountSupplier, int fluidColor) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.capacitySupplier = () -> capacity;
        this.fluidSupplier = null;
        this.amountSupplier = amountSupplier;
    }

    /**
     * Rend la jauge de fluide
     */
    public void render(GuiGraphics graphics, int screenX, int screenY) {
        int gaugeX = screenX + x;
        int gaugeY = screenY + y;

        // Rendre le fond de la jauge (cadre)
        renderGaugeFrame(graphics, gaugeX, gaugeY);

        // Calculer la hauteur du fluide
        int amount = amountSupplier.get();
        int cap = capacitySupplier.get();
        if (amount > 0 && cap > 0) {
            int fluidHeight = (int) ((float) amount / cap * (height - 2));
            if (fluidHeight > 0) {
                renderFluidBar(graphics, gaugeX + 1, gaugeY + height - 1 - fluidHeight,
                               width - 2, fluidHeight);
            }
        }

        // Rendre l'overlay de la jauge (bulles, graduations)
        renderGaugeOverlay(graphics, gaugeX, gaugeY);
    }

    /**
     * Rend le cadre de la jauge en utilisant les textures Minecraft
     */
    private void renderGaugeFrame(GuiGraphics graphics, int x, int y) {
        // Utilise le style de cadre de conteneur Minecraft
        // Bord haut
        graphics.fill(x, y, x + width, y + 1, 0xFF373737);
        // Bord gauche
        graphics.fill(x, y, x + 1, y + height, 0xFF373737);
        // Bord bas
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        // Bord droit
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFFFFFFFF);
        // Fond interne
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF8B8B8B);
    }

    /**
     * Rend la barre de fluide
     */
    private void renderFluidBar(GuiGraphics graphics, int x, int y, int w, int h) {
        int color = getFluidColor();

        // Rendre avec un degrade pour plus de profondeur
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // Couleur principale
        graphics.fill(x, y, x + w, y + h, 0xFF000000 | color);

        // Highlight sur le cote gauche
        int highlightColor = 0xFF000000 |
            (Math.min(255, r + 40) << 16) |
            (Math.min(255, g + 40) << 8) |
            Math.min(255, b + 40);
        graphics.fill(x, y, x + 1, y + h, highlightColor);

        // Shadow sur le cote droit
        int shadowColor = 0xFF000000 |
            (Math.max(0, r - 40) << 16) |
            (Math.max(0, g - 40) << 8) |
            Math.max(0, b - 40);
        graphics.fill(x + w - 1, y, x + w, y + h, shadowColor);
    }

    /**
     * Rend l'overlay (graduations)
     */
    private void renderGaugeOverlay(GuiGraphics graphics, int x, int y) {
        // Lignes de graduation (25%, 50%, 75%)
        int gradColor = 0x40FFFFFF;
        int innerHeight = height - 2;

        for (int i = 1; i <= 3; i++) {
            int gradY = y + height - 1 - (innerHeight * i / 4);
            graphics.fill(x + 1, gradY, x + width - 1, gradY + 1, gradColor);
        }
    }

    /**
     * Obtient la couleur du fluide
     */
    private int getFluidColor() {
        if (fluidSupplier != null) {
            FluidStack stack = fluidSupplier.get();
            if (!stack.isEmpty()) {
                Fluid fluid = stack.getFluid();

                // Couleurs specifiques pour les fluides Beemancer
                String fluidName = fluid.builtInRegistryHolder().key().location().getPath();
                if (fluidName.contains("honey")) {
                    return HONEY_COLOR;
                } else if (fluidName.contains("royal_jelly")) {
                    return ROYAL_JELLY_COLOR;
                } else if (fluidName.contains("nectar")) {
                    return NECTAR_COLOR;
                }

                // Essayer d'obtenir la couleur du fluide via NeoForge
                try {
                    IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid);
                    return extensions.getTintColor(stack);
                } catch (Exception e) {
                    return HONEY_COLOR; // Fallback
                }
            }
        }
        return HONEY_COLOR; // Couleur par defaut
    }

    /**
     * Verifie si la souris survole la jauge
     */
    public boolean isMouseOver(int screenX, int screenY, int mouseX, int mouseY) {
        int gaugeX = screenX + x;
        int gaugeY = screenY + y;
        return mouseX >= gaugeX && mouseX < gaugeX + width &&
               mouseY >= gaugeY && mouseY < gaugeY + height;
    }

    /**
     * Obtient le tooltip pour la jauge
     */
    public List<Component> getTooltip(String fluidName) {
        int amount = amountSupplier.get();
        int cap = capacitySupplier.get();
        return List.of(
            Component.literal(fluidName + ": " + amount + " / " + cap + " mB"),
            Component.literal(String.format("%.1f%%", cap > 0 ? (float) amount / cap * 100 : 0))
                .withStyle(style -> style.withColor(0xAAAAAA))
        );
    }

    // Getters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
