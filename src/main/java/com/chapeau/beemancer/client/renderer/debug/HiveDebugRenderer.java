/**
 * ============================================================
 * [HiveDebugRenderer.java]
 * Description: Renderer debug affichant les cooldowns des abeilles au-dessus des ruches
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | DebugWandItem       | Flag displayDebug    | Condition d'affichage          |
 * | MagicHiveBlockEntity| Données cooldown     | Cooldowns et états des abeilles|
 * | HiveBeeSlot         | États des slots      | EMPTY, INSIDE, OUTSIDE         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java: Enregistrement de l'event
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.debug;

import com.chapeau.beemancer.common.block.hive.HiveBeeSlot;
import com.chapeau.beemancer.common.block.hive.MagicHiveBlockEntity;
import com.chapeau.beemancer.common.item.debug.DebugWandItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Renderer affichant les cooldowns des abeilles au-dessus des MagicHive.
 * Affiche l'état et le cooldown de chaque slot occupé.
 */
@OnlyIn(Dist.CLIENT)
public class HiveDebugRenderer {

    private static final int SCAN_RADIUS = 32;
    private static final float TEXT_SCALE = 0.025f;
    private static final int TEXT_BG_COLOR = 0x80000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TITLE_COLOR = 0xFFFFAA00;
    private static final int INSIDE_COLOR = 0xFF55FF55;
    private static final int OUTSIDE_COLOR = 0xFF55FFFF;
    private static final int EMPTY_COLOR = 0xFF888888;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        if (!DebugWandItem.displayDebug) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) return;

        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        // Scanner les BlockEntities dans un rayon
        BlockPos playerPos = player.blockPosition();
        AABB searchBox = new AABB(playerPos).inflate(SCAN_RADIUS);

        poseStack.pushPose();

        for (int x = (int) searchBox.minX; x <= searchBox.maxX; x++) {
            for (int y = (int) searchBox.minY; y <= searchBox.maxY; y++) {
                for (int z = (int) searchBox.minZ; z <= searchBox.maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockEntity be = level.getBlockEntity(pos);

                    if (be instanceof MagicHiveBlockEntity hive) {
                        List<LineInfo> lines = getHiveContent(hive);
                        if (!lines.isEmpty()) {
                            renderTextAbove(poseStack, bufferSource, font, cameraPos, pos, lines);
                        }
                    }
                }
            }
        }

        poseStack.popPose();
        bufferSource.endBatch();
    }

    /**
     * Récupère le contenu d'une MagicHive sous forme de lignes.
     */
    private static List<LineInfo> getHiveContent(MagicHiveBlockEntity hive) {
        List<LineInfo> lines = new ArrayList<>();
        int[] cooldowns = hive.getBeeCooldowns();
        HiveBeeSlot.State[] states = hive.getBeeStates();

        lines.add(new LineInfo("[Hive Cooldowns]", TITLE_COLOR));

        boolean hasAnyBee = false;
        for (int i = 0; i < cooldowns.length; i++) {
            HiveBeeSlot.State state = states[i];

            if (state == HiveBeeSlot.State.EMPTY) {
                continue; // Ne pas afficher les slots vides
            }

            hasAnyBee = true;
            int cooldown = cooldowns[i];
            String stateStr = state.name();
            int color = (state == HiveBeeSlot.State.INSIDE) ? INSIDE_COLOR : OUTSIDE_COLOR;

            // Formater le cooldown en secondes
            String cooldownStr;
            if (cooldown > 0) {
                float seconds = cooldown / 20.0f;
                cooldownStr = String.format("%.1fs", seconds);
            } else {
                cooldownStr = "Ready";
            }

            String line = String.format("Slot %d: %s [%s]", i + 1, stateStr, cooldownStr);
            lines.add(new LineInfo(line, color));
        }

        if (!hasAnyBee) {
            lines.add(new LineInfo("(empty)", EMPTY_COLOR));
        }

        return lines;
    }

    /**
     * Rend du texte flottant au-dessus d'une position.
     */
    private static void renderTextAbove(PoseStack poseStack, MultiBufferSource bufferSource,
                                        Font font, Vec3 cameraPos, BlockPos pos,
                                        List<LineInfo> lines) {
        // Position au centre du bloc, légèrement au-dessus
        double x = pos.getX() + 0.5 - cameraPos.x;
        double y = pos.getY() + 1.3 - cameraPos.y;
        double z = pos.getZ() + 0.5 - cameraPos.z;

        poseStack.pushPose();
        poseStack.translate(x, y, z);

        // Faire face à la caméra (billboard)
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        // Inverser et mettre à l'échelle
        poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        Matrix4f matrix = poseStack.last().pose();

        // Dessiner chaque ligne
        int lineY = 0;
        for (LineInfo lineInfo : lines) {
            String line = lineInfo.text;
            int lineX = -font.width(line) / 2;

            // Dessiner avec fond (see-through)
            font.drawInBatch(
                line,
                lineX,
                lineY,
                lineInfo.color,
                false,
                matrix,
                bufferSource,
                Font.DisplayMode.SEE_THROUGH,
                TEXT_BG_COLOR,
                0xF000F0
            );

            lineY += 10;
        }

        poseStack.popPose();
    }

    /**
     * Info d'une ligne avec sa couleur.
     */
    private record LineInfo(String text, int color) {}
}
