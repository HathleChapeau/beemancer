/**
 * ============================================================
 * [RideableBeeDebugHud.java]
 * Description: HUD debug affiché quand on chevauche une RideableBee
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | DebugWandItem       | Flag displayDebug    | Condition d'affichage          |
 * | RideableBeeEntity   | Données à afficher   | Vélocité, état, settings       |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java: Enregistrement de l'event
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.hud;

import com.chapeau.beemancer.common.entity.mount.RideableBeeEntity;
import com.chapeau.beemancer.common.entity.mount.RidingMode;
import com.chapeau.beemancer.common.entity.mount.behaviour.types.land.HorseSettings;
import com.chapeau.beemancer.common.item.debug.DebugWandItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Renderer HUD affichant les informations de debug de la RideableBee.
 * Positionné en haut à droite de l'écran.
 *
 * Affiche:
 * - Vélocité (X, Z)
 * - Input (forward, strafe)
 * - Vitesse max
 * - État: WALK, RUN, AIRBORNE, JUMPING
 */
@OnlyIn(Dist.CLIENT)
public class RideableBeeDebugHud {

    private static final int PADDING = 6;
    private static final int LINE_HEIGHT = 11;
    private static final int BOX_MARGIN = 10;

    // Taille fixe du panneau (évite le resize permanent)
    private static final int FIXED_BOX_WIDTH = 180;
    private static final int FIXED_BOX_HEIGHT = 75;

    // Couleurs
    private static final int BG_COLOR = 0xDD000000;
    private static final int BORDER_COLOR = 0xFF555555;
    private static final int TITLE_COLOR = 0xFFFFAA00;
    private static final int LABEL_COLOR = 0xFFAAAAAA;
    private static final int VALUE_COLOR = 0xFFFFFFFF;
    private static final int STATE_WALK_COLOR = 0xFF55FF55;
    private static final int STATE_RUN_COLOR = 0xFFFFFF55;
    private static final int STATE_AIR_COLOR = 0xFF55FFFF;
    private static final int STATE_JUMP_COLOR = 0xFFFF5555;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (event.getName() != VanillaGuiLayers.HOTBAR) {
            return;
        }

        // Vérifier si le debug est actif
        if (!DebugWandItem.displayDebug) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        // Vérifier si le joueur chevauche une RideableBee
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof RideableBeeEntity bee)) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;

        renderPanel(graphics, font, bee, player);
    }

    private static void renderPanel(GuiGraphics graphics, Font font, RideableBeeEntity bee, Player player) {
        String title = "Rideable Bee Debug";

        // Récupérer les données
        Vec3 velocity = bee.getRideVelocity();
        float forward = player.zza;
        float strafe = player.xxa;
        String state = bee.getDebugState();
        RidingMode mode = bee.getRidingMode();
        HorseSettings settings = bee.getSettings();
        float maxSpeed = settings.getTopSpeed();

        // Formater les lignes
        String velLine = String.format("Vel: X=%+.3f Z=%+.3f", velocity.x, velocity.z);
        String inputLine = String.format("Input: fwd=%+.2f str=%+.2f", forward, strafe);
        String speedLine = String.format("Max Speed: %.2f", maxSpeed);
        String stateLine = "State: " + state;

        // Taille fixe (pas de resize)
        int boxWidth = FIXED_BOX_WIDTH;
        int boxHeight = FIXED_BOX_HEIGHT;

        // Position en haut à droite
        int screenWidth = graphics.guiWidth();
        int x = screenWidth - boxWidth - BOX_MARGIN;
        int y = BOX_MARGIN;

        // Fond
        graphics.fill(x, y, x + boxWidth, y + boxHeight, BG_COLOR);

        // Bordure
        drawBorder(graphics, x, y, boxWidth, boxHeight, BORDER_COLOR);

        int textX = x + PADDING;
        int textY = y + PADDING;

        // Titre
        graphics.drawString(font, title, textX, textY, TITLE_COLOR, false);
        textY += LINE_HEIGHT + 2;

        // Séparateur
        graphics.fill(x + 2, textY - 1, x + boxWidth - 2, textY, BORDER_COLOR);
        textY += 3;

        // Vélocité
        graphics.drawString(font, velLine, textX, textY, VALUE_COLOR, false);
        textY += LINE_HEIGHT;

        // Input
        graphics.drawString(font, inputLine, textX, textY, VALUE_COLOR, false);
        textY += LINE_HEIGHT;

        // Max Speed
        graphics.drawString(font, speedLine, textX, textY, VALUE_COLOR, false);
        textY += LINE_HEIGHT;

        // État avec couleur dynamique
        int stateColor = getStateColor(state);
        graphics.drawString(font, "State: ", textX, textY, LABEL_COLOR, false);
        graphics.drawString(font, state, textX + font.width("State: "), textY, stateColor, false);
    }

    private static int getStateColor(String state) {
        return switch (state) {
            case "WALK" -> STATE_WALK_COLOR;
            case "RUN" -> STATE_RUN_COLOR;
            case "AIRBORNE" -> STATE_AIR_COLOR;
            case "JUMPING" -> STATE_JUMP_COLOR;
            default -> VALUE_COLOR;
        };
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
