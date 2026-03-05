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
 * - HoverbikeModel.java: Modele du Hoverbike (alias type)
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
 * Reproduit la geometrie du BeeModel vanilla (corps, ailes, pattes, antennes, dard)
 * avec un setupAnim custom adapte au vol permanent (battement d'ailes continu,
 * pattes repliees).
 *
 * Utilise la texture vanilla bee.png (64x64).
 * Le scale est gere par le renderer qui utilise ce modele.
 *
 * @param <T> Type d'entite compatible
 */
public class GiantBeeModel<T extends Entity> extends HierarchicalModel<T> {

    private final ModelPart root;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart frontLegs;
    private final ModelPart middleLegs;
    private final ModelPart backLegs;

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

        // Bone racine — offset Y=19 comme le modele vanilla
        PartDefinition bone = partDefinition.addOrReplaceChild("bone",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 19.0F, 0.0F));

        // Corps (thorax + abdomen) — 7x7x10
        PartDefinition body = bone.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.5F, -4.0F, -5.0F, 7.0F, 7.0F, 10.0F),
                PartPose.ZERO);

        // Dard — plan vertical plat
        body.addOrReplaceChild("stinger",
                CubeListBuilder.create()
                        .texOffs(26, 7)
                        .addBox(0.0F, -1.0F, 5.0F, 0.0F, 1.0F, 2.0F),
                PartPose.ZERO);

        // Antenne gauche
        body.addOrReplaceChild("left_antenna",
                CubeListBuilder.create()
                        .texOffs(2, 0)
                        .addBox(1.5F, -2.0F, -3.0F, 1.0F, 2.0F, 3.0F),
                PartPose.offset(0.0F, -2.0F, -5.0F));

        // Antenne droite
        body.addOrReplaceChild("right_antenna",
                CubeListBuilder.create()
                        .texOffs(2, 3)
                        .addBox(-2.5F, -2.0F, -3.0F, 1.0F, 2.0F, 3.0F),
                PartPose.offset(0.0F, -2.0F, -5.0F));

        // Aile droite — plan horizontal plat
        bone.addOrReplaceChild("right_wing",
                CubeListBuilder.create()
                        .texOffs(0, 18)
                        .addBox(-9.0F, 0.0F, 0.0F, 9.0F, 0.0F, 6.0F),
                PartPose.offsetAndRotation(-1.5F, -4.0F, -3.0F, 0.0F, -0.2618F, 0.0F));

        // Aile gauche — miroir de la droite
        bone.addOrReplaceChild("left_wing",
                CubeListBuilder.create()
                        .texOffs(0, 18)
                        .mirror()
                        .addBox(0.0F, 0.0F, 0.0F, 9.0F, 0.0F, 6.0F),
                PartPose.offsetAndRotation(1.5F, -4.0F, -3.0F, 0.0F, 0.2618F, 0.0F));

        // Pattes avant
        bone.addOrReplaceChild("front_legs",
                CubeListBuilder.create()
                        .texOffs(26, 1)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, -2.0F));

        // Pattes milieu
        bone.addOrReplaceChild("middle_legs",
                CubeListBuilder.create()
                        .texOffs(26, 3)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, 0.0F));

        // Pattes arriere
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

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Battement d'ailes rapide et permanent (vol)
        float wingFlap = Mth.cos(ageInTicks * 2.1F) * (float) Math.PI * 0.15F;
        rightWing.zRot = wingFlap;
        leftWing.zRot = -wingFlap;

        // Pattes repliees vers l'arriere (pose de vol)
        float legTuck = 0.7854F; // 45 degrees
        frontLegs.xRot = legTuck;
        middleLegs.xRot = legTuck;
        backLegs.xRot = legTuck;
    }
}
