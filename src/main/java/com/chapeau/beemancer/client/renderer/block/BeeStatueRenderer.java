/**
 * ============================================================
 * [BeeStatueRenderer.java]
 * Description: Renderer pour la statue d'abeille avec modèle et nom
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation           |
 * |-------------------------|----------------------|-----------------------|
 * | BeeStatueBlockEntity    | Données à rendre     | getSpeciesId()        |
 * | BeeModel                | Modèle vanilla       | Rendu de l'abeille    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.statue.BeeStatueBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Renderer pour la statue d'abeille.
 * Affiche le modèle de l'abeille avec le nom de l'espèce flottant au-dessus.
 */
public class BeeStatueRenderer implements BlockEntityRenderer<BeeStatueBlockEntity> {

    private final BeeModel<?> beeModel;
    private final Font font;

    private static final ResourceLocation VANILLA_BEE =
            ResourceLocation.withDefaultNamespace("textures/entity/bee/bee.png");

    // Cache des textures par espèce pour éviter les lookups à chaque frame
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new HashMap<>();

    public BeeStatueRenderer(BlockEntityRendererProvider.Context context) {
        this.beeModel = new BeeModel<>(context.bakeLayer(ModelLayers.BEE));
        this.font = context.getFont();
    }

    @Override
    public void render(BeeStatueBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        String speciesId = blockEntity.getSpeciesId();

        poseStack.pushPose();

        // ===== Render Bee Model =====
        // Position au centre du bloc, sur le piédestal (Y+1 bloc au-dessus)
        poseStack.translate(0.5, 1.85, 0.5);

        // Rotation pour faire face au joueur (ou rotation fixe)
        float time = (blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0) + partialTick;
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 2)); // Rotation lente

        // Rotation 180° sur X pour retourner le modèle (au lieu de scale négatif qui inverse les normales)
        poseStack.mulPose(Axis.XP.rotationDegrees(180));

        // Échelle de l'abeille
        float scale = 0.7f;
        poseStack.scale(scale, scale, scale);

        // Récupérer la texture de l'espèce
        ResourceLocation texture = getTextureForSpecies(speciesId);

        // Rendre le modèle
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(texture));
        beeModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();

        // ===== Render Name Tag =====
        poseStack.pushPose();

        // Position au-dessus de l'abeille
        poseStack.translate(0.5, 1.6, 0.5);

        // Toujours face au joueur
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.015f, -0.015f, 0.015f);

        // Texte à afficher
        Component speciesName = Component.translatable("species.beemancer." + speciesId);
        float textWidth = font.width(speciesName);

        // Fond semi-transparent
        Matrix4f matrix = poseStack.last().pose();
        float bgOpacity = 0.4f;
        int bgColor = (int)(bgOpacity * 255.0f) << 24;

        font.drawInBatch(
            speciesName,
            -textWidth / 2,
            0,
            0xFFFFFFFF, // Blanc
            false,
            matrix,
            buffer,
            Font.DisplayMode.NORMAL,
            bgColor,
            packedLight
        );

        poseStack.popPose();
    }

    /**
     * Récupère la texture pour une espèce donnée (avec cache).
     */
    private ResourceLocation getTextureForSpecies(String speciesId) {
        return TEXTURE_CACHE.computeIfAbsent(speciesId, id -> {
            ResourceLocation customTexture = ResourceLocation.fromNamespaceAndPath(
                Beemancer.MOD_ID,
                "textures/entity/bee/" + id + "_bee.png"
            );

            // Vérifier si la texture existe
            try {
                if (Minecraft.getInstance().getResourceManager().getResource(customTexture).isPresent()) {
                    return customTexture;
                }
            } catch (Exception ignored) {}

            return VANILLA_BEE;
        });
    }

    /**
     * Vide le cache des textures (appelé lors du rechargement des ressources).
     */
    public static void clearTextureCache() {
        TEXTURE_CACHE.clear();
    }

    @Override
    public boolean shouldRenderOffScreen(BeeStatueBlockEntity blockEntity) {
        return true; // Le nom flotte au-dessus
    }
}
