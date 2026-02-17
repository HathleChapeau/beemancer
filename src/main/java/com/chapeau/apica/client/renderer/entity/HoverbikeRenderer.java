/**
 * ============================================================
 * [HoverbikeRenderer.java]
 * Description: Renderer pour l'entite Hoverbike avec banking, tilt et flottement
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Entite a rendre      | Source des donnees             |
 * | HoverbikeModel      | Modele 3D            | Geometrie base                 |
 * | HoverbikePartLayer  | Layer parties         | Rendu parties modulaires       |
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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Renderer du Hoverbike.
 * Rend le modele de base puis delegue aux parties modulaires via HoverbikePartLayer.
 * Ajoute des effets visuels client-only :
 * - Banking (roll) proportionnel a la rotation
 * - Inclinaison selon le terrain (pitch/roll via 4 raycasts)
 * - Leger flottement visuel au-dessus du sol
 */
public class HoverbikeRenderer extends MobRenderer<HoverbikeEntity, HoverbikeModel> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "hoverbike"), "main");

    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/block/iron_block.png");

    /** Offset Y visuel constant pour le flottement au-dessus du sol. */
    private static final float HOVER_FLOAT_OFFSET = 0.15f;

    /** Angle max de banking en degres. */
    private static final float MAX_BANK_ANGLE = 15.0f;

    /** Facteur de lissage pour le banking (lerp). */
    private static final float BANK_SMOOTHING = 0.15f;

    /** Distance de raycast pour le terrain tilt. */
    private static final double TERRAIN_RAY_DISTANCE = 2.0;

    /** Angle max de terrain tilt en degres. */
    private static final float MAX_TERRAIN_TILT = 8.0f;

    /** Banking courant (lisse). */
    private float currentBankAngle = 0;

    /** Terrain pitch/roll courants (lisses). */
    private float currentTerrainPitch = 0;
    private float currentTerrainRoll = 0;

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

        // Flottement visuel : decaler legerement vers le haut
        poseStack.translate(0, HOVER_FLOAT_OFFSET, 0);

        // Banking visuel : roll proportionnel au yaw delta et a la vitesse
        updateBanking(entity);
        if (Math.abs(currentBankAngle) > 0.01f) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(currentBankAngle));
        }

        // Inclinaison terrain : pitch/roll selon la surface sous le bike
        updateTerrainTilt(entity);
        if (Math.abs(currentTerrainPitch) > 0.01f) {
            poseStack.mulPose(Axis.XP.rotationDegrees(currentTerrainPitch));
        }
        if (Math.abs(currentTerrainRoll) > 0.01f) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(currentTerrainRoll));
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }

    /**
     * Met a jour l'angle de banking (roll) en fonction du yaw delta.
     * Tourne a gauche → penche a gauche (roll negatif).
     * Tourne a droite → penche a droite (roll positif).
     * Lissage via lerp pour transition fluide.
     */
    private void updateBanking(HoverbikeEntity entity) {
        float yawDelta = entity.getLastYawDelta();
        double speed = entity.getRideVelocity().horizontalDistance();

        // Banking proportionnel au yaw delta, amplifie par la vitesse
        float speedFactor = (float) Mth.clamp(speed / 0.3, 0.0, 1.0);
        float targetBank = -yawDelta * speedFactor * (MAX_BANK_ANGLE / 6.0f);
        targetBank = Mth.clamp(targetBank, -MAX_BANK_ANGLE, MAX_BANK_ANGLE);

        // Lissage
        currentBankAngle = Mth.lerp(BANK_SMOOTHING, currentBankAngle, targetBank);
    }

    /**
     * Met a jour l'inclinaison visuelle selon le terrain.
     * 4 raycasts aux coins du bike (avant/arriere/gauche/droite)
     * pour calculer pitch et roll.
     */
    private void updateTerrainTilt(HoverbikeEntity entity) {
        Level level = entity.level();
        Vec3 center = entity.position();
        float yaw = entity.getYRot();
        float yawRad = yaw * Mth.DEG_TO_RAD;
        float sin = Mth.sin(yawRad);
        float cos = Mth.cos(yawRad);

        // Offsets locaux des 4 coins (avant/arriere dans Z, gauche/droite dans X)
        double frontX = center.x + (-0.6 * sin);
        double frontZ = center.z + (0.6 * cos);
        double backX = center.x + (0.6 * sin);
        double backZ = center.z + (-0.6 * cos);
        double leftX = center.x + (-0.5 * cos);
        double leftZ = center.z + (-0.5 * sin);
        double rightX = center.x + (0.5 * cos);
        double rightZ = center.z + (0.5 * sin);

        double frontY = raycastGroundY(level, frontX, center.y, frontZ);
        double backY = raycastGroundY(level, backX, center.y, backZ);
        double leftY = raycastGroundY(level, leftX, center.y, leftZ);
        double rightY = raycastGroundY(level, rightX, center.y, rightZ);

        // Calculer pitch (avant/arriere) et roll (gauche/droite)
        float targetPitch = (float) Math.toDegrees(Math.atan2(frontY - backY, 1.2));
        float targetRoll = (float) Math.toDegrees(Math.atan2(rightY - leftY, 1.0));

        targetPitch = Mth.clamp(targetPitch, -MAX_TERRAIN_TILT, MAX_TERRAIN_TILT);
        targetRoll = Mth.clamp(targetRoll, -MAX_TERRAIN_TILT, MAX_TERRAIN_TILT);

        // Lissage
        currentTerrainPitch = Mth.lerp(0.1f, currentTerrainPitch, targetPitch);
        currentTerrainRoll = Mth.lerp(0.1f, currentTerrainRoll, targetRoll);
    }

    /**
     * Raycast vers le bas pour trouver la hauteur du terrain a un point donne.
     * Retourne la hauteur Y relative au centre de l'entite.
     */
    private static double raycastGroundY(Level level, double x, double centerY, double z) {
        Vec3 start = new Vec3(x, centerY + 0.5, z);
        Vec3 end = new Vec3(x, centerY - TERRAIN_RAY_DISTANCE, z);

        BlockHitResult result = level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity) null));

        if (result.getType() == HitResult.Type.BLOCK) {
            return result.getLocation().y - centerY;
        }
        return 0;
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
