/**
 * ============================================================
 * [HoverbikeEditModeHandler.java]
 * Description: Gestionnaire client pour detecter le hoverbike en edit mode
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Entite cible         | Recherche edit mode            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement des events
 * - HoverbikePartLayer.java: Lecture du hoverbike en edition
 * - HoverbikeEditStatsHud.java: Reference au hoverbike en edition
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.hud;

import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Detecte chaque tick client le hoverbike que le joueur local edite.
 * L'interaction avec les pieces passe desormais par les InteractionMarkerEntity
 * (geres nativement par Minecraft).
 */
@OnlyIn(Dist.CLIENT)
public class HoverbikeEditModeHandler {

    private static HoverbikeEntity editingHoverbike = null;

    /** Retourne le hoverbike en cours d'edition (ou null). */
    public static HoverbikeEntity getEditingHoverbike() {
        return editingHoverbike;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused()) {
            editingHoverbike = null;
            return;
        }

        editingHoverbike = findEditingHoverbike(mc);
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
