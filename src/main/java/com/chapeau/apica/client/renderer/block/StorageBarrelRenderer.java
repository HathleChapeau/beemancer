/**
 * ============================================================
 * [StorageBarrelRenderer.java]
 * Description: Renderer pour le barrel - affiche item et quantite sur la face avant
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | StorageBarrelBlockEntity      | Donnees du barrel    | Item stocke, quantite, void    |
 * | StorageBarrelBlock            | FACING property      | Orientation de la face avant   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.storage.StorageBarrelBlock;
import com.chapeau.apica.common.blockentity.storage.StorageBarrelBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class StorageBarrelRenderer implements BlockEntityRenderer<StorageBarrelBlockEntity> {

    private static final ResourceLocation VOID_ICON = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/gui/void_upgrade_icon.png");

    private final ItemRenderer itemRenderer;
    private final Font font;

    public StorageBarrelRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
        this.font = context.getFont();
    }

    @Override
    public void render(StorageBarrelBlockEntity barrel, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (barrel.getLevel() == null) return;

        ItemStack storedItem = barrel.getStoredItem();
        int storedCount = barrel.getStoredCount();
        boolean hasVoid = barrel.hasVoidUpgrade();

        if (storedItem.isEmpty() && !hasVoid) return;

        BlockState state = barrel.getBlockState();
        Direction facing = state.getValue(StorageBarrelBlock.FACING);

        poseStack.pushPose();

        // Move to center of block
        poseStack.translate(0.5, 0.5, 0.5);

        // Rotate to face the correct direction
        applyFacingRotation(poseStack, facing);

        // Now we're facing SOUTH (+Z direction)
        // The front face is at z=+0.5

        // Render stored item inside the indent (1px deep = 1/16 block)
        // Inner wall at z=0.4375, frame edge at z=0.5
        if (!storedItem.isEmpty() && storedCount > 0) {
            poseStack.pushPose();
            poseStack.translate(0, 0.03, 0.47); // Centered in indent
            poseStack.scale(0.5f, 0.5f, 0.001f); // Flat on the face
            itemRenderer.renderStatic(storedItem, ItemDisplayContext.GUI,
                    packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, barrel.getLevel(), 0);
            poseStack.popPose();

            // Render count text below item
            String countText = formatCount(storedCount);
            poseStack.pushPose();
            poseStack.translate(0, -0.25, 0.47);
            poseStack.scale(-0.015f, -0.015f, 0.015f);
            float textWidth = font.width(countText);
            font.drawInBatch(countText, -textWidth / 2f, 0, 0xFFFFFF,
                    true, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL,
                    0x40000000, packedLight);
            poseStack.popPose();
        }

        // Render void upgrade icon (bottom-right inside indent)
        if (hasVoid) {
            poseStack.pushPose();
            poseStack.translate(0.28, -0.28, 0.48);
            poseStack.scale(0.15f, 0.15f, 0.001f);

            // Render a small "V" text as icon placeholder
            font.drawInBatch("\u00a74V", -font.width("V") / 2f, -4, 0xFF4444,
                    true, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL,
                    0x40000000, packedLight);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    /**
     * Applique la rotation pour orienter le rendu vers la face FACING.
     * Apres cette rotation, +Z pointe vers la face avant du barrel.
     */
    private void applyFacingRotation(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case SOUTH -> {} // Default, no rotation needed
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case EAST  -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            case WEST  -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case UP    -> poseStack.mulPose(Axis.XP.rotationDegrees(270));
            case DOWN  -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
        }
    }

    /**
     * Formate le nombre d'items pour l'affichage.
     * Ex: 1234 -> "1234", 12345 -> "12.3k", 1234567 -> "1.2M"
     */
    private String formatCount(int count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 10000) return String.format("%.1fk", count / 1000.0);
        if (count < 1000000) return String.format("%dk", count / 1000);
        return String.format("%.1fM", count / 1000000.0);
    }
}
