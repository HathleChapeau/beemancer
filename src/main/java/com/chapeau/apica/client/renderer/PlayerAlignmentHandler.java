/**
 * ============================================================
 * [PlayerAlignmentHandler.java]
 * Description: Gere l'alignement tete/corps du joueur pour les animations
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | RenderPlayerEvent   | Event rendu joueur   | Modifier rotations avant rendu |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - MiningLaserItemRenderer.java
 * - LeafBlowerItemRenderer.java
 * - ChopperHiveItemRenderer.java
 * - BuildingStaffItemRenderer.java
 * - RailgunItemRenderer.java
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer;

import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Permet d'aligner temporairement la rotation Y du corps du joueur
 * avec sa tete pendant certaines animations (ex: reload).
 */
@OnlyIn(Dist.CLIENT)
public class PlayerAlignmentHandler {

    private static boolean alignmentEnabled = false;

    /**
     * Active ou desactive l'alignement tete/corps.
     */
    public static void setAlignmentEnabled(boolean enabled) {
        alignmentEnabled = enabled;
    }

    /**
     * Retourne true si l'alignement est actif.
     */
    public static boolean isAlignmentEnabled() {
        return alignmentEnabled;
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (!alignmentEnabled) return;

        Player player = event.getEntity();
        player.yBodyRotO = player.yHeadRotO;
        player.yBodyRot = player.yHeadRot;

        alignmentEnabled = false;
    }
}
