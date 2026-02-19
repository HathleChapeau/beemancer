/**
 * ============================================================
 * [AssemblyTableStatsRenderer.java]
 * Description: Panneau billboard affichant les stats d'une piece sur l'Assembly Table
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation                    |
 * |-----------------------------|----------------------|--------------------------------|
 * | AssemblyTableBlockEntity    | Source des donnees   | getStoredItem()                |
 * | HoverbikePartData           | Lecture stats        | getBaseStats(), getAll/Pre/Suf |
 * | HoverbikePartItem           | Info variant         | getCategory(), getVariantIndex |
 * | AppliedStat                 | Base stats           | Affichage                      |
 * | AppliedModifier             | Modifiers            | Affichage avec tier            |
 * | CustomDebugDisplayRenderer  | Pattern billboard    | renderTextAt() copie           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement de l'event
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.common.blockentity.mount.AssemblyTableBlockEntity;
import com.chapeau.apica.common.item.mount.AppliedModifier;
import com.chapeau.apica.common.item.mount.AppliedStat;
import com.chapeau.apica.common.item.mount.HoverbikePartData;
import com.chapeau.apica.common.item.mount.HoverbikePartItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

/**
 * Renderer billboard pour afficher les stats d'une piece sur l'Assembly Table.
 * Toujours visible quand une piece est posee, distance max 16 blocs.
 * Souscrit a RenderLevelStageEvent.AFTER_TRANSLUCENT_BLOCKS.
 * Billboard Y-axis (pattern CustomDebugDisplayRenderer).
 */
@OnlyIn(Dist.CLIENT)
public class AssemblyTableStatsRenderer {

    private static final float TEXT_SCALE = 0.018f;
    private static final int TEXT_BG_COLOR = 0x80000000;
    private static final double MAX_RENDER_DISTANCE_SQ = 16.0 * 16.0;
    private static final double PANEL_Y_OFFSET = 2.2;

    // Couleurs
    private static final int COLOR_TITLE = 0xFFFF55;
    private static final int COLOR_SEPARATOR = 0xAAAAAA;
    private static final int COLOR_BASE_STAT = 0xFFFFFF;
    private static final int COLOR_PREFIX = 0xFFAA00;
    private static final int COLOR_SUFFIX = 0x55FFFF;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 playerPos = mc.player.position();
        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        Level level = mc.level;

        // Scanner les block entities proches pour trouver les AssemblyTable avec item
        // On cherche dans un rayon de 16 blocs autour du joueur
        BlockPos playerBlockPos = mc.player.blockPosition();
        int range = 16;
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos checkPos = playerBlockPos.offset(dx, dy, dz);
                    BlockEntity be = level.getBlockEntity(checkPos);
                    if (!(be instanceof AssemblyTableBlockEntity table)) continue;

                    ItemStack stored = table.getStoredItem();
                    if (stored.isEmpty() || !(stored.getItem() instanceof HoverbikePartItem partItem)) continue;

                    Vec3 tableCenter = Vec3.atCenterOf(checkPos);
                    if (tableCenter.distanceToSqr(playerPos) > MAX_RENDER_DISTANCE_SQ) continue;

                    renderStatsPanel(poseStack, bufferSource, font, cameraPos, tableCenter, stored, partItem);
                }
            }
        }

        bufferSource.endBatch();
    }

    /**
     * Rend le panneau de stats complet au-dessus d'une Assembly Table.
     */
    private static void renderStatsPanel(PoseStack poseStack, MultiBufferSource bufferSource,
                                          Font font, Vec3 cameraPos, Vec3 tableCenter,
                                          ItemStack stored, HoverbikePartItem partItem) {

        Vec3 renderPos = tableCenter.add(0, PANEL_Y_OFFSET, 0);

        // Construire les lignes du panneau
        StringBuilder sb = new StringBuilder();

        // Titre: [Variant Name] MK X
        String variantName = partItem.getCategory().name() + " #" + partItem.getVariantIndex();
        int mk = HoverbikePartData.getMK(stored);
        String mkText = mk > 0 ? " MK " + toRoman(mk) : "";
        sb.append(variantName).append(mkText);

        // Base Stats
        List<AppliedStat> baseStats = HoverbikePartData.getBaseStats(stored);
        if (!baseStats.isEmpty()) {
            sb.append("\n--- Base Stats ---");
            for (AppliedStat stat : baseStats) {
                sb.append("\n  ").append(stat.statType().getJsonKey()).append(": +")
                        .append(formatValue(stat.value()));
            }
        }

        // Prefixes
        List<AppliedModifier> prefixes = HoverbikePartData.getPrefixes(stored);
        if (!prefixes.isEmpty()) {
            sb.append("\n--- Prefix ---");
            for (AppliedModifier mod : prefixes) {
                sb.append("\n  [").append(mod.modifierName()).append("] T").append(mod.tier())
                        .append(" ").append(mod.statType().getJsonKey()).append(" ")
                        .append(formatModValue(mod));
            }
        }

        // Suffixes
        List<AppliedModifier> suffixes = HoverbikePartData.getSuffixes(stored);
        if (!suffixes.isEmpty()) {
            sb.append("\n--- Suffix ---");
            for (AppliedModifier mod : suffixes) {
                sb.append("\n  [").append(mod.modifierName()).append("] T").append(mod.tier())
                        .append(" ").append(mod.statType().getJsonKey()).append(" ")
                        .append(formatModValue(mod));
            }
        }

        renderMultiColorTextAt(poseStack, bufferSource, font, cameraPos, renderPos, sb.toString(),
                baseStats.size(), prefixes.size(), suffixes.size());
    }

    /**
     * Rend un texte multi-ligne colore en billboard.
     * Chaque section (titre, base, prefix, suffix) a sa couleur.
     */
    private static void renderMultiColorTextAt(PoseStack poseStack, MultiBufferSource bufferSource,
                                                Font font, Vec3 cameraPos, Vec3 worldPos,
                                                String text, int baseCount, int prefixCount, int suffixCount) {
        double x = worldPos.x - cameraPos.x;
        double y = worldPos.y - cameraPos.y;
        double z = worldPos.z - cameraPos.z;

        poseStack.pushPose();
        poseStack.translate(x, y, z);

        // Billboard Y-axis (pattern CustomDebugDisplayRenderer)
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            poseStack.popPose();
            return;
        }
        Vec3 p = mc.player.position();
        double tan = -Math.atan2(worldPos.z - p.z, worldPos.x - p.x) + (Math.PI / 2.0);
        Quaternionf quat = new Quaternionf().rotationXYZ(0, (float) tan, 0);
        poseStack.mulPose(quat);
        poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        Matrix4f matrix = poseStack.last().pose();
        String[] lines = text.split("\n");

        // Rendre du bas vers le haut (index inverse)
        int lineY = 0;
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i];
            int color = getLineColor(i, lines.length, baseCount, prefixCount, suffixCount);
            int lineX = -font.width(line) / 2;
            font.drawInBatch(line, lineX, lineY, color, false,
                    matrix, bufferSource, Font.DisplayMode.NORMAL,
                    TEXT_BG_COLOR, 0xF000F0);
            lineY -= 10;
        }

        poseStack.popPose();
    }

    /**
     * Determine la couleur d'une ligne en fonction de sa position dans le panneau.
     * Ligne 0 = titre, puis separateurs et stats de base, puis prefixes, puis suffixes.
     */
    private static int getLineColor(int lineIndex, int totalLines, int baseCount, int prefixCount, int suffixCount) {
        // Ligne 0: titre
        if (lineIndex == 0) return COLOR_TITLE;

        // Compter les lignes par section
        int cursor = 1;

        // Section base stats: 1 separateur + baseCount lignes
        if (baseCount > 0) {
            if (lineIndex == cursor) return COLOR_SEPARATOR;
            cursor++;
            if (lineIndex < cursor + baseCount) return COLOR_BASE_STAT;
            cursor += baseCount;
        }

        // Section prefix: 1 separateur + prefixCount lignes
        if (prefixCount > 0) {
            if (lineIndex == cursor) return COLOR_SEPARATOR;
            cursor++;
            if (lineIndex < cursor + prefixCount) return COLOR_PREFIX;
            cursor += prefixCount;
        }

        // Section suffix: 1 separateur + suffixCount lignes
        if (suffixCount > 0) {
            if (lineIndex == cursor) return COLOR_SEPARATOR;
            cursor++;
            if (lineIndex < cursor + suffixCount) return COLOR_SUFFIX;
            cursor += suffixCount;
        }

        return COLOR_BASE_STAT;
    }

    private static String formatValue(double value) {
        if (value == (int) value) return String.valueOf((int) value);
        return String.format("%.3f", value);
    }

    private static String formatModValue(AppliedModifier mod) {
        if ("%".equals(mod.valueType())) {
            return String.format("+%.1f%%", mod.value() * 100);
        }
        return "+" + formatValue(mod.value());
    }

    private static String toRoman(int num) {
        if (num <= 0 || num > 10) return String.valueOf(num);
        String[] romans = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return romans[num];
    }
}
