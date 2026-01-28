/**
 * ============================================================
 * [PipeDebugRenderer.java]
 * Description: Renderer debug affichant le contenu des pipes au-dessus d'elles
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | DebugWandItem       | Flag displayDebug    | Condition d'affichage          |
 * | ItemPipeBlockEntity | Contenu items        | Buffer ItemStackHandler        |
 * | HoneyPipeBlockEntity| Contenu fluide       | Buffer FluidTank               |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java: Enregistrement de l'event
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.debug;

import com.chapeau.beemancer.common.blockentity.alchemy.HoneyPipeBlockEntity;
import com.chapeau.beemancer.common.blockentity.alchemy.ItemPipeBlockEntity;
import com.chapeau.beemancer.common.item.debug.DebugWandItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renderer affichant le contenu des pipes en texte flottant.
 * - ItemPipe: liste des items et quantités
 * - HoneyPipe: type de fluide et quantité en mB
 */
@OnlyIn(Dist.CLIENT)
public class PipeDebugRenderer {

    private static final int SCAN_RADIUS = 32;
    private static final float TEXT_SCALE = 0.025f;
    private static final int TEXT_BG_COLOR = 0x80000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int ITEM_COLOR = 0xFF55FF55;
    private static final int FLUID_COLOR = 0xFF55FFFF;

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

                    if (be instanceof ItemPipeBlockEntity itemPipe) {
                        List<String> lines = getItemPipeContent(itemPipe);
                        if (!lines.isEmpty()) {
                            renderTextAbove(poseStack, bufferSource, font, cameraPos, pos, lines, ITEM_COLOR);
                        }
                    } else if (be instanceof HoneyPipeBlockEntity honeyPipe) {
                        List<String> lines = getHoneyPipeContent(honeyPipe);
                        if (!lines.isEmpty()) {
                            renderTextAbove(poseStack, bufferSource, font, cameraPos, pos, lines, FLUID_COLOR);
                        }
                    }
                }
            }
        }

        poseStack.popPose();
        bufferSource.endBatch();
    }

    /**
     * Récupère le contenu d'un ItemPipe sous forme de lignes.
     */
    private static List<String> getItemPipeContent(ItemPipeBlockEntity pipe) {
        List<String> lines = new ArrayList<>();

        // Agréger les items par type
        Map<String, Integer> itemCounts = new HashMap<>();
        for (ItemPipeBlockEntity.PipeItem pipeItem : pipe.getItems()) {
            ItemStack stack = pipeItem.stack;
            if (!stack.isEmpty()) {
                String name = stack.getHoverName().getString();
                itemCounts.merge(name, stack.getCount(), Integer::sum);
            }
        }

        if (itemCounts.isEmpty()) {
            return lines;
        }

        lines.add("[Items " + pipe.getItemCount() + "/" + pipe.getMaxSlots() + "]");
        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            lines.add(entry.getKey() + " x" + entry.getValue());
        }

        return lines;
    }

    /**
     * Récupère le contenu d'un HoneyPipe sous forme de lignes.
     */
    private static List<String> getHoneyPipeContent(HoneyPipeBlockEntity pipe) {
        List<String> lines = new ArrayList<>();
        FluidStack fluid = pipe.getBuffer().getFluid();

        if (fluid.isEmpty()) {
            return lines;
        }

        String fluidName = fluid.getHoverName().getString();
        int amount = fluid.getAmount();

        lines.add("[Fluid]");
        lines.add(fluidName);
        lines.add(amount + " mB");

        return lines;
    }

    /**
     * Rend du texte flottant au-dessus d'une position.
     */
    private static void renderTextAbove(PoseStack poseStack, MultiBufferSource bufferSource,
                                        Font font, Vec3 cameraPos, BlockPos pos,
                                        List<String> lines, int textColor) {
        // Position au centre du bloc, légèrement au-dessus
        double x = pos.getX() + 0.5 - cameraPos.x;
        double y = pos.getY() + 1.2 - cameraPos.y;
        double z = pos.getZ() + 0.5 - cameraPos.z;

        poseStack.pushPose();
        poseStack.translate(x, y, z);

        // Faire face à la caméra (billboard)
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        // Inverser et mettre à l'échelle
        poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        Matrix4f matrix = poseStack.last().pose();

        // Calculer dimensions du fond
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }
        int totalHeight = lines.size() * 10;

        // Dessiner le fond
        int bgX = -maxWidth / 2 - 2;
        int bgY = -2;
        int bgWidth = maxWidth + 4;
        int bgHeight = totalHeight + 4;

        // Rendre le fond via vertex buffer (simplifié: utiliser le font renderer)
        // Le fond sera dessiné par ligne avec le mode see-through

        // Dessiner chaque ligne
        int lineY = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineX = -font.width(line) / 2;

            // Couleur différente pour le titre
            int color = (i == 0) ? textColor : TEXT_COLOR;

            // Dessiner avec fond (see-through)
            font.drawInBatch(
                line,
                lineX,
                lineY,
                color,
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
}
