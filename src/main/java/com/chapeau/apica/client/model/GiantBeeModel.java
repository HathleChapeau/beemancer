/**
 * ============================================================
 * [GiantBeeModel.java]
 * Description: Modele d'abeille geante reutilisable (geometrie vanilla + animations)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Mth                 | Math utilitaire      | Animation des ailes            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeModel.java: Modele du Hoverbike (specialise les animations)
 * - HoverbikeRenderer.java: Via HoverbikeModel
 * - ClientSetup.java: Enregistrement du layer via HoverbikeModel
 *
 * ============================================================
 */
package com.chapeau.apica.client.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Modele d'abeille geante generique.
 * Reproduit la geometrie du BeeModel vanilla (corps, ailes, pattes, antennes, dard).
 * Les animations sont pilotees via des methodes protegees que les sous-classes
 * peuvent appeler avec des parametres contextuels.
 *
 * Utilise la texture vanilla bee.png (64x64).
 *
 * @param <T> Type d'entite compatible
 */
public class GiantBeeModel<T extends Entity> extends HierarchicalModel<T> {

    private final ModelPart root;
    protected final ModelPart rightWing;
    protected final ModelPart leftWing;
    protected final ModelPart frontLegs;
    protected final ModelPart middleLegs;
    protected final ModelPart backLegs;

    public GiantBeeModel(ModelPart root) {
        this.root = root;
        ModelPart bone = root.getChild("bone");
        this.rightWing = bone.getChild("right_wing");
        this.leftWing = bone.getChild("left_wing");
        this.frontLegs = bone.getChild("front_legs");
        this.middleLegs = bone.getChild("middle_legs");
        this.backLegs = bone.getChild("back_legs");
    }

    /**
     * Geometrie identique au BeeModel vanilla.
     * Texture 64x64 (compatible avec textures/entity/bee/bee.png).
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        PartDefinition bone = partDefinition.addOrReplaceChild("bone",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 19.0F, 0.0F));

        PartDefinition body = bone.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.5F, -4.0F, -5.0F, 7.0F, 7.0F, 10.0F),
                PartPose.ZERO);

        body.addOrReplaceChild("stinger",
                CubeListBuilder.create()
                        .texOffs(26, 7)
                        .addBox(0.0F, -1.0F, 5.0F, 0.0F, 1.0F, 2.0F),
                PartPose.ZERO);

        body.addOrReplaceChild("left_antenna",
                CubeListBuilder.create()
                        .texOffs(2, 0)
                        .addBox(1.5F, -2.0F, -3.0F, 1.0F, 2.0F, 3.0F),
                PartPose.offset(0.0F, -2.0F, -5.0F));

        body.addOrReplaceChild("right_antenna",
                CubeListBuilder.create()
                        .texOffs(2, 3)
                        .addBox(-2.5F, -2.0F, -3.0F, 1.0F, 2.0F, 3.0F),
                PartPose.offset(0.0F, -2.0F, -5.0F));

        bone.addOrReplaceChild("right_wing",
                CubeListBuilder.create()
                        .texOffs(0, 18)
                        .addBox(-9.0F, 0.0F, 0.0F, 9.0F, 0.0F, 6.0F),
                PartPose.offsetAndRotation(-1.5F, -4.0F, -3.0F, 0.0F, -0.2618F, 0.0F));

        bone.addOrReplaceChild("left_wing",
                CubeListBuilder.create()
                        .texOffs(0, 18)
                        .mirror()
                        .addBox(0.0F, 0.0F, 0.0F, 9.0F, 0.0F, 6.0F),
                PartPose.offsetAndRotation(1.5F, -4.0F, -3.0F, 0.0F, 0.2618F, 0.0F));

        bone.addOrReplaceChild("front_legs",
                CubeListBuilder.create()
                        .texOffs(26, 1)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, -2.0F));

        bone.addOrReplaceChild("middle_legs",
                CubeListBuilder.create()
                        .texOffs(26, 3)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, 0.0F));

        bone.addOrReplaceChild("back_legs",
                CubeListBuilder.create()
                        .texOffs(26, 5)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, 2.0F));

        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    /**
     * Anime les ailes avec vitesse et amplitude configurables.
     *
     * @param ageInTicks Temps d'animation
     * @param speed      Vitesse de battement (0 = immobile, 1.8 = vol)
     * @param amplitude  Amplitude du battement en radians (0 = immobile)
     * @param upBias     Bias vers le haut en radians (decale la position neutre)
     */
    protected void animateWings(float ageInTicks, float speed, float amplitude, float upBias) {
        if (speed <= 0.001F || amplitude <= 0.001F) {
            rightWing.zRot = -upBias;
            leftWing.zRot = upBias;
            return;
        }
        float flap = Mth.cos(ageInTicks * speed) * amplitude;
        rightWing.zRot = flap - upBias;
        leftWing.zRot = -flap + upBias;
    }

    /**
     * Anime les pattes avec un angle de repli.
     *
     * @param tuckAngle Angle de repli en radians (0 = pendantes, 0.78 = repliees)
     */
    protected void animateLegs(float tuckAngle) {
        frontLegs.xRot = tuckAngle;
        middleLegs.xRot = tuckAngle;
        backLegs.xRot = tuckAngle;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        animateWings(ageInTicks, 1.8F, 0.12F, 0.05F);
        animateLegs(0.7854F);
    }
}
