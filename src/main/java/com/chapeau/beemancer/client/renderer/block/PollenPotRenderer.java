/**
 * ============================================================
 * [PollenPotRenderer.java]
 * Description: Renderer du contenu du Pollen Pot (cube texturé avec hauteur dynamique)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | PollenPotBlockEntity    | Données pollen       | getPollenItem(), getPollenCount |
 * | PollenTextures          | Mapping textures     | getTexture(item)               |
 * | FluidCubeRenderer       | Rendu cube texturé   | renderFluidCube()              |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.client.color.PollenTextures;
import com.chapeau.beemancer.client.renderer.util.FluidCubeRenderer;
import com.chapeau.beemancer.common.block.pollenpot.PollenPotBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Rend un cube texturé à l'intérieur du pollen pot.
 * La hauteur du cube est proportionnelle au nombre de pollens stockés (0-16).
 * La texture dépend du type de pollen (concrete powder colorée).
 */
public class PollenPotRenderer implements BlockEntityRenderer<PollenPotBlockEntity> {

    // Limites intérieures du pot (en pixels /16)
    private static final float MIN_X = 3f / 16f;
    private static final float MAX_X = 13f / 16f;
    private static final float MIN_Z = 3f / 16f;
    private static final float MAX_Z = 13f / 16f;
    private static final float MIN_Y = 2f / 16f;
    private static final float MAX_FILL_HEIGHT = 8f / 16f;

    public PollenPotRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PollenPotBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        if (blockEntity.isEmpty()) {
            return;
        }

        float fillRatio = (float) blockEntity.getPollenCount() / PollenPotBlockEntity.MAX_POLLEN;
        if (fillRatio <= 0f) return;

        ResourceLocation textureLoc = PollenTextures.getTexture(blockEntity.getPollenItem());
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(textureLoc);

        VertexConsumer consumer = buffer.getBuffer(RenderType.solid());
        var pose = poseStack.last();

        float maxY = MIN_Y + (MAX_FILL_HEIGHT * fillRatio);

        // Rend le cube : face UP + 4 côtés, pas de face DOWN (fond du pot)
        FluidCubeRenderer.renderFluidCube(consumer, pose, sprite,
            MIN_X, MIN_Y, MIN_Z, MAX_X, maxY, MAX_Z,
            true, false, true, true, true, true);
    }
}
