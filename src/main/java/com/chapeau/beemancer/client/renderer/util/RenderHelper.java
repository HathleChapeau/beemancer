/**
 * ============================================================
 * [RenderHelper.java]
 * Description: Utilitaires de rendu: lookup sprite fluide + tessellation model
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation               |
 * |------------------------------|----------------------|---------------------------|
 * | IClientFluidTypeExtensions   | Texture fluide       | getStillTexture()         |
 * | TextureAtlas                 | Atlas sprites        | Lookup atlas              |
 * | ModelRenderer                | Rendu model          | tesselateBlock()          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoneyTankRenderer, HoneyReservoirRenderer, MultiblockTankRenderer
 * - CentrifugeHeartRenderer, InfuserHeartRenderer, CrystallizerRenderer
 * - StorageControllerRenderer, CrankRenderer, ControllerPipeRenderer
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Utilitaires de rendu partages entre les block entity renderers.
 */
public final class RenderHelper {

    private RenderHelper() {}

    /**
     * Recupere le sprite atlas de la texture "still" d'un fluide.
     */
    public static TextureAtlasSprite getFluidSprite(Fluid fluid) {
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation stillTexture = extensions.getStillTexture();
        return Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(stillTexture);
    }

    /**
     * Wrapper pour tesselateBlock avec les parametres par defaut (no cull, ModelData.EMPTY).
     */
    public static void tesselateModel(BlockRenderDispatcher blockRenderer, BakedModel model,
                                       Level level, BlockState state, BlockPos pos,
                                       PoseStack poseStack, VertexConsumer consumer,
                                       RandomSource random, int light, int overlay,
                                       RenderType renderType) {
        blockRenderer.getModelRenderer().tesselateBlock(
            level, model, state, pos, poseStack, consumer, false, random,
            light, overlay, ModelData.EMPTY, renderType
        );
    }
}
