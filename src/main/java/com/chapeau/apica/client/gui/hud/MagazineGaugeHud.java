/**
 * ============================================================
 * [MagazineGaugeHud.java]
 * Description: Jauge HUD affichant le niveau de fluide du magazine equipe
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | IMagazineHolder     | Detection items      | instanceof check               |
 * | MagazineData        | Lecture magazine     | Niveau fluide                  |
 * | GuiRenderHelper     | Rendu barre          | renderTintedBar                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement event handler)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.hud;

import com.chapeau.apica.client.gui.GuiRenderHelper;
import com.chapeau.apica.common.item.magazine.IMagazineHolder;
import com.chapeau.apica.common.item.magazine.MagazineData;
import com.chapeau.apica.common.item.magazine.MagazineFluidData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/**
 * Affiche une jauge verticale a droite de l'ecran quand le joueur tient
 * un item IMagazineHolder avec un magazine equipe.
 * Utilise le style des barres de l'Infuser (right_honeybar_bg + tint).
 */
@OnlyIn(Dist.CLIENT)
public class MagazineGaugeHud {

    private static final int HONEY_COLOR = 0xE8A317;
    private static final int ROYAL_JELLY_COLOR = 0xFFF8DC;
    private static final int NECTAR_COLOR = 0xFFD700;
    private static final int DEFAULT_COLOR = 0x888888;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Verifier main hand
        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof IMagazineHolder)) return;
        if (!MagazineData.hasMagazine(mainHand)) return;

        int amount = MagazineData.getFluidAmount(mainHand);
        String fluidId = MagazineData.getFluidId(mainHand);
        float ratio = (float) amount / MagazineFluidData.MAX_CAPACITY;

        int color = getFluidColor(fluidId);

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Position: droite de l'ecran, centree verticalement pres de la hotbar
        int x = screenWidth - 28;
        int y = screenHeight - 62;

        GuiRenderHelper.renderTintedBar(graphics, x, y, ratio, color);

        // Label fluide en-dessous
        String label = getFluidLabel(fluidId);
        int textWidth = mc.font.width(label);
        graphics.drawString(mc.font, label,
                x + 8 - textWidth / 2, y + 52,
                0xCCCCCC, true);
    }

    private static int getFluidColor(String fluidId) {
        if (fluidId.contains("honey")) return HONEY_COLOR;
        if (fluidId.contains("royal_jelly")) return ROYAL_JELLY_COLOR;
        if (fluidId.contains("nectar")) return NECTAR_COLOR;
        return DEFAULT_COLOR;
    }

    private static String getFluidLabel(String fluidId) {
        if (fluidId.contains("honey")) return "Honey";
        if (fluidId.contains("royal_jelly")) return "R. Jelly";
        if (fluidId.contains("nectar")) return "Nectar";
        return "Fluid";
    }
}
