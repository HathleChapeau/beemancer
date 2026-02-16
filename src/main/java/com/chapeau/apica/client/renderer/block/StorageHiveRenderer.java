/**
 * ============================================================
 * [StorageHiveRenderer.java]
 * Description: Renderer pour les Storage Hives avec animation d'oscillation
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation                    |
 * |-----------------------------|----------------------|--------------------------------|
 * | StorageHiveBlockEntity      | BlockEntity          | Donnees de rendu               |
 * | StorageHiveAnimator         | Animation bobbing    | Tick, apply oscillation        |
 * | AnimationController         | Apply animations     | tick(), applyAnimation()       |
 * | StorageHiveBlock            | HiveState            | Lecture etat visuel            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement du renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.client.animation.AnimationController;
import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.client.animation.StorageHiveAnimator;
import com.chapeau.apica.common.block.storage.StorageHiveBlock;
import com.chapeau.apica.common.blockentity.storage.StorageHiveBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renderer pour les Storage Hives.
 *
 * Quand la hive est linkee (LINKED ou ACTIVE), le modele blockstate normal est
 * masque via getRenderShape(ENTITYBLOCK_ANIMATED) et ce renderer prend le relais.
 * Le modele est rendu avec une animation d'oscillation verticale (bobbing).
 *
 * Quand la hive est UNLINKED, le modele blockstate est affiche normalement
 * (getRenderShape retourne MODEL) et ce renderer ne fait rien.
 */
public class StorageHiveRenderer implements BlockEntityRenderer<StorageHiveBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    public StorageHiveRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(StorageHiveBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        if (blockEntity.getLevel() == null) return;

        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof StorageHiveBlock)) return;

        StorageHiveBlock.HiveState hiveState = state.getValue(StorageHiveBlock.HIVE_STATE);

        // UNLINKED: le chunk mesh rend le modele, rien a faire ici
        if (hiveState == StorageHiveBlock.HiveState.UNLINKED) {
            StorageHiveAnimator.remove(blockEntity.getBlockPos());
            return;
        }

        // LINKED ou ACTIVE: rendre le modele avec animation d'oscillation
        BlockPos pos = blockEntity.getBlockPos();
        float currentTime = AnimationTimer.getRenderTime(partialTick);

        StorageHiveAnimator.tick(pos, currentTime, true);
        AnimationController ctrl = StorageHiveAnimator.getController(pos);

        BakedModel model = blockRenderer.getBlockModel(state);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.cutout());

        poseStack.pushPose();
        ctrl.applyAnimation("bob", poseStack);

        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(), model, state, pos,
            poseStack, vertexConsumer, false, random, packedLight, packedOverlay,
            ModelData.EMPTY, RenderType.cutout());

        poseStack.popPose();
    }
}
