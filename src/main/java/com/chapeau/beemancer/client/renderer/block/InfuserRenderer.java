/**
 * ============================================================
 * [InfuserRenderer.java]
 * Description: Renderer pour l'item flottant et les particules de l'Infuser
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                | Raison                | Utilisation                    |
 * |---------------------------|----------------------|--------------------------------|
 * | InfuserBlockEntity        | Données item/état    | getInputSlot()                 |
 * | InfuserBlock              | WORKING property     | Détection état actif           |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.common.block.alchemy.InfuserBlock;
import com.chapeau.beemancer.common.blockentity.alchemy.InfuserBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * Renderer pour l'Infuser.
 * - Affiche l'item d'input flottant au centre du bloc.
 * - Génère des particules jaune miel tournant autour du bloc quand il fonctionne.
 */
public class InfuserRenderer implements BlockEntityRenderer<InfuserBlockEntity> {

    private static final DustParticleOptions HONEY_PARTICLE =
        new DustParticleOptions(new Vector3f(1.0f, 0.75f, 0.1f), 0.8f);

    private static final float PARTICLE_RADIUS = 0.55f;
    private static final float PARTICLE_SPEED = 0.08f;
    private static final int PARTICLE_COUNT = 3;

    private final ItemRenderer itemRenderer;

    public InfuserRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(InfuserBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        Level level = blockEntity.getLevel();
        if (level == null) return;

        ItemStack inputItem = blockEntity.getInputSlot().getStackInSlot(0);

        // --- Rendu de l'item au centre du bloc ---
        if (!inputItem.isEmpty()) {
            renderFloatingItem(blockEntity, inputItem, partialTick, poseStack, buffer, packedLight, packedOverlay);
        }

        // --- Particules quand l'infuser fonctionne ---
        boolean working = blockEntity.getBlockState().hasProperty(InfuserBlock.WORKING)
            && blockEntity.getBlockState().getValue(InfuserBlock.WORKING);

        if (working) {
            spawnOrbitingParticles(blockEntity, level, partialTick);
        }
    }

    private void renderFloatingItem(InfuserBlockEntity blockEntity, ItemStack stack, float partialTick,
                                     PoseStack poseStack, MultiBufferSource buffer,
                                     int packedLight, int packedOverlay) {
        poseStack.pushPose();

        float time = (blockEntity.getLevel().getGameTime() + partialTick);
        float bob = (float) Math.sin(time * 0.1) * 0.03f;

        poseStack.translate(0.5, 0.5 + bob, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.5f));
        poseStack.scale(0.4f, 0.4f, 0.4f);

        itemRenderer.renderStatic(
            stack,
            ItemDisplayContext.FIXED,
            packedLight,
            packedOverlay,
            poseStack,
            buffer,
            blockEntity.getLevel(),
            0
        );

        poseStack.popPose();
    }

    private void spawnOrbitingParticles(InfuserBlockEntity blockEntity, Level level, float partialTick) {
        long gameTime = level.getGameTime();
        if (gameTime % 2 != 0) return;

        double cx = blockEntity.getBlockPos().getX() + 0.5;
        double cy = blockEntity.getBlockPos().getY() + 0.5;
        double cz = blockEntity.getBlockPos().getZ() + 0.5;

        float time = (gameTime + partialTick) * PARTICLE_SPEED;

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float angle = time + (i * (2.0f * (float) Math.PI / PARTICLE_COUNT));
            double px = cx + Math.cos(angle) * PARTICLE_RADIUS;
            double pz = cz + Math.sin(angle) * PARTICLE_RADIUS;

            level.addParticle(HONEY_PARTICLE, px, cy, pz, 0, 0.01, 0);
        }
    }
}
