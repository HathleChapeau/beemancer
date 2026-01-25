/**
 * ============================================================
 * [BuildingWandPreviewRenderer.java]
 * Description: Renderer pour la prévisualisation de la Building Wand
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | BuildingWandItem    | Calcul positions     | Prévisualisation      |
 * | RenderLevelStageEvent| Event rendu         | Hook rendu            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement event)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer;

import com.chapeau.beemancer.common.item.tool.BuildingWandItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

/**
 * Renderer pour afficher la prévisualisation de la Building Wand.
 * Dessine des contours blancs autour des blocs qui seront placés.
 */
@OnlyIn(Dist.CLIENT)
public class BuildingWandPreviewRenderer {

    // Cache pour éviter de recalculer à chaque frame
    private static BlockPos cachedTargetPos = null;
    private static Direction cachedFace = null;
    private static Block cachedSourceBlock = null;
    private static int cachedInventoryCount = -1;
    private static List<BlockPos> cachedPositions = List.of();

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // Vérifier que le joueur tient la Building Wand
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean hasWandMainHand = mainHand.getItem() instanceof BuildingWandItem;
        boolean hasWandOffHand = offHand.getItem() instanceof BuildingWandItem;

        if (!hasWandMainHand && !hasWandOffHand) return;

        // Récupérer le bloc ciblé
        HitResult hitResult = mc.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos targetPos = blockHit.getBlockPos();
        Direction face = blockHit.getDirection();
        Level level = player.level();

        BlockState sourceState = level.getBlockState(targetPos);
        if (sourceState.isAir()) return;

        Block sourceBlock = sourceState.getBlock();

        // Calculer le count d'inventaire pour la clé de cache
        int inventoryCount = player.isCreative() ? -1 : countBlocksInInventory(player, sourceBlock);

        // Vérifier si le cache est valide
        List<BlockPos> previewPositions;
        if (isCacheValid(targetPos, face, sourceBlock, inventoryCount)) {
            previewPositions = cachedPositions;
        } else {
            // Recalculer et mettre en cache
            previewPositions = BuildingWandItem.calculatePreviewPositions(
                level, player, targetPos, face, sourceState
            );
            updateCache(targetPos, face, sourceBlock, inventoryCount, previewPositions);
        }

        if (previewPositions.isEmpty()) return;

        // Rendu des contours
        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        // Obtenir le buffer pour les lignes
        var bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());

        // Dessiner chaque bloc de prévisualisation
        for (BlockPos pos : previewPositions) {
            renderBlockOutline(poseStack, buffer, pos, 1.0f, 1.0f, 1.0f, 0.8f);
        }

        // Flush le buffer
        bufferSource.endBatch(RenderType.lines());

        poseStack.popPose();
    }

    /**
     * Dessine un contour autour d'un bloc.
     */
    private static void renderBlockOutline(PoseStack poseStack, VertexConsumer buffer,
                                            BlockPos pos, float r, float g, float b, float a) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        LevelRenderer.renderLineBox(
            poseStack, buffer,
            x, y, z,
            x + 1, y + 1, z + 1,
            r, g, b, a
        );
    }

    /**
     * Vérifie si le cache est toujours valide.
     */
    private static boolean isCacheValid(BlockPos targetPos, Direction face,
                                         Block sourceBlock, int inventoryCount) {
        return targetPos.equals(cachedTargetPos)
            && face == cachedFace
            && sourceBlock == cachedSourceBlock
            && inventoryCount == cachedInventoryCount;
    }

    /**
     * Met à jour le cache avec les nouvelles valeurs.
     */
    private static void updateCache(BlockPos targetPos, Direction face, Block sourceBlock,
                                     int inventoryCount, List<BlockPos> positions) {
        cachedTargetPos = targetPos.immutable();
        cachedFace = face;
        cachedSourceBlock = sourceBlock;
        cachedInventoryCount = inventoryCount;
        cachedPositions = positions;
    }

    /**
     * Compte le nombre de blocs dans l'inventaire du joueur.
     */
    private static int countBlocksInInventory(Player player, Block block) {
        int count = 0;
        var blockItem = block.asItem();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.getItem() == blockItem) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
