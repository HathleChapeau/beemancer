/**
 * ============================================================
 * [AssemblyTableRenderer.java]
 * Description: Renderer pour afficher le modele 3D de la piece de moto sur la table
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation                    |
 * |-----------------------------|----------------------|--------------------------------|
 * | AssemblyTableBlockEntity    | Donnees a rendre     | getStoredItem()                |
 * | HoverbikePartItem           | Type d'item          | getCategory(), getVariantIndex |
 * | HoverbikePartVariants       | Registre modeles     | getVariants(), ModelFactory     |
 * | HoverbikePartModel          | Modele 3D            | renderToBuffer()               |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement renderer
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartModel;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartVariants;
import com.chapeau.apica.common.blockentity.mount.AssemblyTableBlockEntity;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
import com.chapeau.apica.common.item.mount.HoverbikePartItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Renderer pour l'Assembly Table.
 * Affiche le modele 3D de la piece de moto stockee, en rotation lente au-dessus du slab.
 * Utilise les memes modeles HoverbikePartModel que le HoverbikePartLayer.
 */
public class AssemblyTableRenderer implements BlockEntityRenderer<AssemblyTableBlockEntity> {

    private static final float ROTATION_SPEED = 1.0f;
    private static final float MODEL_SCALE = 0.35f;
    private static final double MODEL_Y = 1.1;

    /** Modeles bakes par categorie → liste des variantes. */
    private final Map<HoverbikePart, List<HoverbikePartModel>> partModels = new EnumMap<>(HoverbikePart.class);

    public AssemblyTableRenderer(BlockEntityRendererProvider.Context context) {
        for (HoverbikePart part : HoverbikePart.values()) {
            List<HoverbikePartVariants.VariantEntry> variants = HoverbikePartVariants.getVariants(part);
            List<HoverbikePartModel> models = new ArrayList<>();
            for (HoverbikePartVariants.VariantEntry entry : variants) {
                models.add(entry.factory().constructor().apply(
                        context.bakeLayer(entry.factory().layerLocation())));
            }
            partModels.put(part, models);
        }
    }

    @Override
    public void render(AssemblyTableBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        ItemStack storedItem = blockEntity.getStoredItem();
        if (storedItem.isEmpty() || !(storedItem.getItem() instanceof HoverbikePartItem partItem)) {
            AssemblyTableOrbitRenderer.removeState(blockEntity.getBlockPos());
            return;
        }

        HoverbikePart category = partItem.getCategory();
        int variantIndex = partItem.getVariantIndex();

        List<HoverbikePartModel> models = partModels.get(category);
        if (models == null || models.isEmpty()) return;

        int clampedIndex = Math.floorMod(variantIndex, models.size());
        HoverbikePartModel model = models.get(clampedIndex);

        float rotation = (AnimationTimer.getRenderTime(partialTick) * ROTATION_SPEED) % 360f;

        poseStack.pushPose();

        // Position au centre du bloc, au-dessus du slab
        poseStack.translate(0.5, MODEL_Y, 0.5);

        // Rotation lente sur l'axe Y
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // Echelle reduite pour tenir sur le slab
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

        // Inversion Y pour les modeles entity (convention Minecraft: Y flippe)
        poseStack.scale(1.0f, -1.0f, 1.0f);

        VertexConsumer vertexConsumer = buffer.getBuffer(
                RenderType.entityCutoutNoCull(model.getTextureLocation()));

        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();

        // Cubes orbitants avec beam
        AssemblyTableOrbitRenderer.render(blockEntity.getBlockPos(), partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRenderOffScreen(AssemblyTableBlockEntity blockEntity) {
        return true;
    }
}
