/**
 * ============================================================
 * [CustomDebugDisplayRenderer.java]
 * Description: Renderer pour les affichages debug enregistrés via DebugWandItem.addDisplay()
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | DebugWandItem       | Entrées + flag debug | displayDebug, getDisplayEntries|
 * | DebugDisplayEntry   | Données d'affichage  | Position, texte, offset        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java: Enregistrement de l'event
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.debug;

import com.chapeau.beemancer.common.item.debug.DebugDisplayEntry;
import com.chapeau.beemancer.common.item.debug.DebugWandItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Renderer pour les entrées de debug custom enregistrées via DebugWandItem.addDisplay().
 * Affiche un texte billboard au-dessus de chaque objet enregistré quand displayDebug=true.
 * Retire automatiquement les entrées dont la position renvoie null.
 */
@OnlyIn(Dist.CLIENT)
public class CustomDebugDisplayRenderer {

    private static final float TEXT_SCALE = 0.025f;
    private static final int TEXT_BG_COLOR = 0x80000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final String ERROR_TEXT = "Str. Error";

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        if (!DebugWandItem.displayDebug) {
            return;
        }

        List<DebugDisplayEntry> entries = DebugWandItem.getDisplayEntries();
        if (entries.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        List<DebugDisplayEntry> toRemove = new ArrayList<>();

        for (DebugDisplayEntry entry : entries) {
            // Récupérer la position — null = objet supprimé, marquer pour retrait
            Vec3 pos;
            try {
                pos = entry.positionSupplier().get();
            } catch (Exception e) {
                toRemove.add(entry);
                continue;
            }

            if (pos == null) {
                toRemove.add(entry);
                continue;
            }

            // Récupérer le texte — try/catch, erreur = "Str. Error"
            String text;
            try {
                text = entry.textSupplier().get();
            } catch (Exception e) {
                text = ERROR_TEXT;
            }

            if (text == null) {
                text = ERROR_TEXT;
            }

            // Appliquer l'offset et rendre
            Vec3 renderPos = pos.add(entry.offset());
            renderTextAt(poseStack, bufferSource, font, cameraPos, renderPos, text);
        }

        // Retirer les entrées invalides
        if (!toRemove.isEmpty()) {
            entries.removeAll(toRemove);
        }

        bufferSource.endBatch();
    }

    /**
     * Rend un texte billboard à une position monde.
     * Le texte fait toujours face à la caméra (billboard).
     */
    private static void renderTextAt(PoseStack poseStack, MultiBufferSource bufferSource,
                                      Font font, Vec3 cameraPos, Vec3 worldPos, String text) {
        double x = worldPos.x - cameraPos.x;
        double y = worldPos.y - cameraPos.y;
        double z = worldPos.z - cameraPos.z;

        poseStack.pushPose();
        poseStack.translate(x, y, z);

        // Billboard: faire face à la caméra
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        Matrix4f matrix = poseStack.last().pose();
        int textX = -font.width(text) / 2;

        font.drawInBatch(
            text, textX, 0, TEXT_COLOR, false,
            matrix, bufferSource, Font.DisplayMode.SEE_THROUGH,
            TEXT_BG_COLOR, 0xF000F0
        );

        poseStack.popPose();
    }
}
