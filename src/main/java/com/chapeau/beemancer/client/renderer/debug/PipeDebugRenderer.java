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
import net.neoforged.neoforge.items.ItemStackHandler;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renderer affichant le contenu des pipes en texte flottant (nametag).
 * - ItemPipe: liste des items et quantités
 * - HoneyPipe: type de fluide et quantité en mB
 */
@OnlyIn(Dist.CLIENT)
public class PipeDebugRenderer {

    private static final int SCAN_RADIUS = 32;
    private static final float TEXT_SCALE = 0.025f;
    private static final int TEXT_BG_COLOR = 0x80000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int ITEM_TITLE_COLOR = 0xFF55FF55;
    private static final int FLUID_TITLE_COLOR = 0xFF55FFFF;

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

        BlockPos playerPos = player.blockPosition();
        AABB searchBox = new AABB(playerPos).inflate(SCAN_RADIUS);

        poseStack.pushPose();

        for (int x = (int) searchBox.minX; x <= searchBox.maxX; x++) {
            for (int y = (int) searchBox.minY; y <= searchBox.maxY; y++) {
                for (int z = (int) searchBox.minZ; z <= searchBox.maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockEntity be = level.getBlockEntity(pos);

                    if (be instanceof ItemPipeBlockEntity itemPipe) {
                        List<LineInfo> lines = getItemPipeContent(itemPipe);
                        if (!lines.isEmpty()) {
                            renderTextAbove(poseStack, bufferSource, font, cameraPos, pos, lines);
                        }
                    } else if (be instanceof HoneyPipeBlockEntity honeyPipe) {
                        List<LineInfo> lines = getHoneyPipeContent(honeyPipe);
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
     * Récupère le contenu d'un ItemPipe sous forme de lignes.
     */
    private static List<LineInfo> getItemPipeContent(ItemPipeBlockEntity pipe) {
        List<LineInfo> lines = new ArrayList<>();
        ItemStackHandler buffer = pipe.getBuffer();

        // Agréger les items par type
        Map<String, Integer> itemCounts = new HashMap<>();
        for (int i = 0; i < buffer.getSlots(); i++) {
            ItemStack stack = buffer.getStackInSlot(i);
            if (!stack.isEmpty()) {
                String name = stack.getHoverName().getString();
                itemCounts.merge(name, stack.getCount(), Integer::sum);
            }
        }

        if (itemCounts.isEmpty()) {
            return lines;
        }

        lines.add(new LineInfo("[Item Pipe]", ITEM_TITLE_COLOR));
        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            lines.add(new LineInfo(entry.getKey() + " x" + entry.getValue(), TEXT_COLOR));
        }

        return lines;
    }

    /**
     * Récupère le contenu d'un HoneyPipe sous forme de lignes.
     */
    private static List<LineInfo> getHoneyPipeContent(HoneyPipeBlockEntity pipe) {
        List<LineInfo> lines = new ArrayList<>();
        FluidStack fluid = pipe.getBuffer().getFluid();

        if (fluid.isEmpty()) {
            return lines;
        }

        String fluidName = fluid.getHoverName().getString();
        int amount = fluid.getAmount();
        int capacity = pipe.getBuffer().getCapacity();

        lines.add(new LineInfo("[Fluid Pipe]", FLUID_TITLE_COLOR));
        lines.add(new LineInfo(fluidName, TEXT_COLOR));
        lines.add(new LineInfo(amount + "/" + capacity + " mB", TEXT_COLOR));

        return lines;
    }

    /**
     * Rend du texte flottant au-dessus d'une position (style nametag).
     */
    private static void renderTextAbove(PoseStack poseStack, MultiBufferSource bufferSource,
                                        Font font, Vec3 cameraPos, BlockPos pos,
                                        List<LineInfo> lines) {
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

    private record LineInfo(String text, int color) {}
}
