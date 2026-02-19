/**
 * ============================================================
 * [HoverbikeInfoHud.java]
 * Description: HUD affichant nom et owner du hoverbike quand le joueur le regarde
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Entite cible         | getOwnerName()                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement event
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.hud;

import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Affiche le nom et le proprietaire du hoverbike en haut-centre de l'ecran
 * quand le joueur regarde un hoverbike (distance < 8 blocs).
 * Visible par tous les joueurs.
 */
@OnlyIn(Dist.CLIENT)
public class HoverbikeInfoHud {

    private static final double MAX_DISTANCE = 8.0;
    private static final int NAME_COLOR = 0xFFFFFFFF;
    private static final int OWNER_COLOR = 0xFFAAAAAA;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (event.getName() != VanillaGuiLayers.HOTBAR) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        HoverbikeEntity hoverbike = findLookedAtHoverbike(mc);
        if (hoverbike == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        String name = "Hoverbike";
        String ownerName = hoverbike.getOwnerName();
        String ownerLine = ownerName.isEmpty() ? "No Owner" : "Owner: " + ownerName;

        int topY = 10;
        graphics.drawCenteredString(mc.font, name, screenWidth / 2, topY, NAME_COLOR);
        graphics.drawCenteredString(mc.font, ownerLine, screenWidth / 2, topY + 12, OWNER_COLOR);
    }

    private static HoverbikeEntity findLookedAtHoverbike(Minecraft mc) {
        Entity camera = mc.getCameraEntity();
        if (camera == null) return null;

        Vec3 eyePos = camera.getEyePosition(1.0f);
        Vec3 lookVec = camera.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(lookVec.scale(MAX_DISTANCE));

        AABB searchBox = camera.getBoundingBox().expandTowards(lookVec.scale(MAX_DISTANCE)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                camera, eyePos, endPos, searchBox,
                e -> e instanceof HoverbikeEntity && !e.isSpectator(),
                MAX_DISTANCE * MAX_DISTANCE);

        if (hit != null && hit.getEntity() instanceof HoverbikeEntity hoverbike) {
            return hoverbike;
        }
        return null;
    }
}
