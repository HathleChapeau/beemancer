/**
 * ============================================================
 * [FlywheelTestBeeVisual.java]
 * Description: Visual Flywheel pour rendu GPU instancie des test bees
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                  | Utilisation                    |
 * |-------------------------|------------------------|--------------------------------|
 * | AbstractEntityVisual    | Base visual entite     | Lifecycle + position           |
 * | SimpleDynamicVisual     | Mise a jour par frame  | Animation ailes                |
 * | InstanceTree            | Arbre d'instances GPU  | Corps + ailes instancies       |
 * | ModelTrees              | Conversion vanilla     | BeeModel vers mesh Flywheel    |
 * | SimpleMaterial          | Materiau GPU           | Texture espece abeille         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - FlywheelTestBeeVisualizer.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.visual;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.content.flywheeltest.FlywheelTestBeeEntity;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.part.InstanceTree;
import dev.engine_room.flywheel.lib.model.part.ModelTrees;
import dev.engine_room.flywheel.lib.visual.AbstractEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class FlywheelTestBeeVisual extends AbstractEntityVisual<FlywheelTestBeeEntity>
        implements SimpleDynamicVisual {

    private final InstanceTree instances;
    private final InstanceTree rightWing;
    private final InstanceTree leftWing;

    public FlywheelTestBeeVisual(VisualizationContext ctx, FlywheelTestBeeEntity entity, float partialTick) {
        super(ctx, entity, partialTick);

        String speciesId = entity.getSpeciesId();
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/entity/bee/" + speciesId + "_bee.png");

        var material = SimpleMaterial.builder()
            .texture(texture)
            .cutout(CutoutShaders.ONE_TENTH)
            .mipmap(false)
            .build();

        var modelTree = ModelTrees.of(ModelLayers.BEE, material);

        this.instances = InstanceTree.create(instancerProvider(), modelTree);

        InstanceTree bone = instances.childOrThrow("bone");
        this.rightWing = bone.childOrThrow("right_wing");
        this.leftWing = bone.childOrThrow("left_wing");
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        if (!isVisible(ctx.frustum())) {
            return;
        }

        var visualPos = getVisualPosition(ctx.partialTick());
        float bob = Mth.sin((entity.tickCount + ctx.partialTick()) * 0.1f) * 0.15f;

        Matrix4f pose = new Matrix4f();
        pose.translate(visualPos.x, visualPos.y + bob, visualPos.z);

        float yaw = Mth.lerp(ctx.partialTick(), entity.yRotO, entity.getYRot());
        pose.rotateY(-yaw * Mth.DEG_TO_RAD);

        pose.translate(0.0f, -1.5f, 0.0f);
        pose.scale(1.0f, -1.0f, -1.0f);

        float wingAngle = Mth.sin(entity.tickCount + ctx.partialTick()) * 0.4f;
        rightWing.zRot(wingAngle);
        leftWing.zRot(-wingAngle);

        instances.updateInstances(pose);

        instances.traverse(instance -> {
            int blockLight = cycleBlockLight();
            int skyLight = cycleSkyLight();
            instance.light(blockLight, skyLight).setChanged();
        });
    }

    private int cycleBlockLight() {
        var pos = entity.blockPosition();
        return entity.level().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
    }

    private int cycleSkyLight() {
        var pos = entity.blockPosition();
        return entity.level().getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
    }

    @Override
    protected void _delete() {
        instances.delete();
    }
}
