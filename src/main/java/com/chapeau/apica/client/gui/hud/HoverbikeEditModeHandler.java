/**
 * ============================================================
 * [HoverbikeEditModeHandler.java]
 * Description: Gestionnaire client pour les interactions edit mode du Hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePartPicker | Raycast parties      | Detection partie visee         |
 * | HoverbikePart       | Enum parties         | Stockage partie hover          |
 * | HoverbikeEntity     | Entite cible         | Recherche edit mode            |
 * | HoverbikePartScreen | Menu selection       | Ouverture sur clic droit       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement des events
 * - HoverbikePartLayer.java: Lecture de la partie hover
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.hud;

import com.chapeau.apica.client.gui.screen.HoverbikePartScreen;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * Gere les interactions du joueur avec les parties du hoverbike en edit mode.
 * Chaque tick client, lance un raycast pour detecter la partie visee.
 * Sur clic droit, ouvre le menu de selection de la partie.
 */
@OnlyIn(Dist.CLIENT)
public class HoverbikeEditModeHandler {

    private static HoverbikePart hoveredPart = null;
    private static HoverbikeEntity editingHoverbike = null;

    /** Retourne la partie actuellement survolee (ou null). */
    public static HoverbikePart getHoveredPart() {
        return hoveredPart;
    }

    /** Retourne le hoverbike en cours d'edition (ou null). */
    public static HoverbikeEntity getEditingHoverbike() {
        return editingHoverbike;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused()) {
            hoveredPart = null;
            editingHoverbike = null;
            return;
        }

        // Chercher le hoverbike que le joueur local edite
        editingHoverbike = findEditingHoverbike(mc);
        if (editingHoverbike == null) {
            hoveredPart = null;
            return;
        }

        // Raycast pour trouver la partie visee
        hoveredPart = HoverbikePartPicker.pick(editingHoverbike, mc.getTimer().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        if (hoveredPart == null || editingHoverbike == null) return;

        // Empecher l'interaction normale et ouvrir le menu de la partie
        event.setCanceled(true);
        event.setSwingHand(false);

        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new HoverbikePartScreen(hoveredPart, editingHoverbike));
    }

    /**
     * Recherche le hoverbike que le joueur local est en train d'editer.
     */
    private static HoverbikeEntity findEditingHoverbike(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return null;

        java.util.UUID playerUUID = player.getUUID();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof HoverbikeEntity hoverbike
                    && hoverbike.isEditMode()
                    && hoverbike.getEditingPlayerUUID().isPresent()
                    && hoverbike.getEditingPlayerUUID().get().equals(playerUUID)) {
                return hoverbike;
            }
        }
        return null;
    }
}
