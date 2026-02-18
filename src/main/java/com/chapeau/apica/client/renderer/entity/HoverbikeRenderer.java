/**
 * ============================================================
 * [HoverbikeRenderer.java]
 * Description: Renderer pour l'entite Hoverbike avec bob sinusoidal, breathing et banking lisse
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Entite a rendre      | Source des donnees             |
 * | HoverbikeModel      | Modele 3D            | Geometrie base                 |
 * | HoverbikePartLayer  | Layer parties         | Rendu parties modulaires       |
 * | HoverbikeMode       | Enum mode            | Adapter visuels selon mode     |
 * | Apica               | MOD_ID               | Chemin texture                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement du renderer
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.model.HoverbikeModel;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.chapeau.apica.common.entity.mount.HoverbikeMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Renderer du Hoverbike.
 * Rend le modele de base puis delegue aux parties modulaires via HoverbikePartLayer.
 * Effets visuels proceduraux (zero raycasts) :
 * - Bob sinusoidal (oscillation Y douce) pour impression de flottement
 * - Idle breathing (micro-roll/pitch) pour donner de la vie
 * - Banking lisse interpole avec partialTick, amplifie en mode RUN
 */
public class HoverbikeRenderer extends MobRenderer<HoverbikeEntity, HoverbikeModel> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "hoverbike"), "main");

    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/block/iron_block.png");

    /** Offset Y visuel de base pour le flottement au-dessus du sol. */
    private static final float HOVER_FLOAT_OFFSET = 0.15f;

    /** Oscillation bob : amplitude en blocs, frequence en radians/tick. */
    private static final float BOB_AMPLITUDE_HOVER = 0.04f;
    private static final float BOB_AMPLITUDE_RUN = 0.01f;
    private static final float BOB_FREQUENCY = 0.05f;

    /** Idle breathing : amplitude en degres, frequences en radians/tick. */
    private static final float BREATH_AMPLITUDE = 0.5f;
    private static final float BREATH_PITCH_FREQ = 0.03f;
    private static final float BREATH_ROLL_FREQ = 0.021f;
    private static final float BREATH_RUN_DAMPING = 0.2f;

    /** Angle max de banking en degres (HOVER). */
    private static final float MAX_BANK_ANGLE_HOVER = 15.0f;

    /** Angle max de banking en degres (RUN) — plus fort pour effet moto. */
    private static final float MAX_BANK_ANGLE_RUN = 25.0f;

    /** Facteur de lissage pour le banking (lerp par frame). */
    private static final float BANK_SMOOTHING = 0.08f;

    /** Banking courant (lisse). */
    private float currentBankAngle = 0;

    public HoverbikeRenderer(EntityRendererProvider.Context context) {
        super(context, new HoverbikeModel(context.bakeLayer(LAYER_LOCATION)), 0.8f);
        this.addLayer(new HoverbikePartLayer(this, context));
    }

    @Override
    public ResourceLocation getTextureLocation(HoverbikeEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(HoverbikeEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        poseStack.pushPose();

        // Mode courant (synched pour clients distants)
        HoverbikeMode mode = entity.getSynchedMode();
        float ageInTicks = entity.tickCount + partialTick;

        // Bob sinusoidal : oscillation Y douce, amplitude reduite en RUN
        float bobAmplitude = (mode == HoverbikeMode.RUN) ? BOB_AMPLITUDE_RUN : BOB_AMPLITUDE_HOVER;
        float bob = HOVER_FLOAT_OFFSET + Mth.sin(ageInTicks * BOB_FREQUENCY) * bobAmplitude;
        poseStack.translate(0, bob, 0);

        // Idle breathing : micro-oscillations pitch/roll (attenuees en RUN)
        float breathDamping = (mode == HoverbikeMode.RUN) ? BREATH_RUN_DAMPING : 1.0f;
        float breathPitch = Mth.sin(ageInTicks * BREATH_PITCH_FREQ) * BREATH_AMPLITUDE * breathDamping;
        float breathRoll = Mth.sin(ageInTicks * BREATH_ROLL_FREQ) * BREATH_AMPLITUDE * 0.5f * breathDamping;
        poseStack.mulPose(Axis.XP.rotationDegrees(breathPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(breathRoll));

        // Banking lisse : interpole yaw delta avec partialTick, amplifie en RUN
        updateBanking(entity, partialTick, mode);
        if (Math.abs(currentBankAngle) > 0.01f) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(currentBankAngle));
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }

    /**
     * Banking lisse avec interpolation partialTick.
     * En mode RUN, le banking est amplifie (effet moto en virage).
     * Plus le yaw delta est grand, plus le bike penche fort.
     */
    private void updateBanking(HoverbikeEntity entity, float partialTick, HoverbikeMode mode) {
        // Interpoler le yaw delta entre le tick precedent et le tick courant
        float interpolatedYawDelta = Mth.lerp(partialTick, entity.getPrevYawDelta(), entity.getLastYawDelta());

        double speed = entity.getRideVelocity().horizontalDistance();
        float speedFactor = (float) Mth.clamp(speed / 0.3, 0.0, 1.0);

        float maxBankAngle = (mode == HoverbikeMode.RUN) ? MAX_BANK_ANGLE_RUN : MAX_BANK_ANGLE_HOVER;
        float targetBank = -interpolatedYawDelta * speedFactor * (maxBankAngle / 6.0f);
        targetBank = Mth.clamp(targetBank, -maxBankAngle, maxBankAngle);

        // Lissage frame-to-frame
        currentBankAngle = Mth.lerp(BANK_SMOOTHING, currentBankAngle, targetBank);
    }

    /**
     * En edit mode, force le block light a 15 (fullbright).
     * L'entite est ainsi plus lumineuse que l'environnement assombri par le shader.
     */
    @Override
    protected int getBlockLightLevel(HoverbikeEntity entity, BlockPos pos) {
        return entity.isEditMode() ? 15 : super.getBlockLightLevel(entity, pos);
    }
}
