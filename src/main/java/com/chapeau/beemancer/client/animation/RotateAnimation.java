/**
 * ============================================================
 * [RotateAnimation.java]
 * Description: Animation de rotation autour d'un axe avec pivot optionnel
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | Animation                     | Classe parent        | Pipeline play/pause/stop       |
 * | TimingType                    | Easing               | Via Animation                  |
 * | TimingEffect                  | Effet temporel       | Via Animation                  |
 * | com.mojang.math.Axis          | Axe de rotation      | mulPose(axis.rotationDegrees)  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - AnimationController.java (gestion)
 * - Tout renderer qui anime une rotation
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Animation de rotation entre un angle de depart et un angle d'arrivee.
 * Supporte un pivot optionnel (translate -> rotate -> translate back).
 * Pour des rotations multi-axes, chainer plusieurs RotateAnimation.
 */
public class RotateAnimation extends Animation {

    private final Axis axis;
    private final float startAngle;
    private final float endAngle;
    @Nullable
    private final Vec3 pivot;

    private RotateAnimation(Axis axis, float startAngle, float endAngle, @Nullable Vec3 pivot,
                             TimingType timingType, TimingEffect timingEffect,
                             float easePower, float duration, boolean resetAfterAnimation) {
        super(timingType, timingEffect, easePower, duration, resetAfterAnimation);
        this.axis = axis;
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        this.pivot = pivot;
    }

    @Override
    protected void doApply(PoseStack poseStack, float progress) {
        float angle = Mth.lerp(progress, startAngle, endAngle);
        if (pivot != null) {
            poseStack.translate(pivot.x, pivot.y, pivot.z);
        }
        poseStack.mulPose(axis.rotationDegrees(angle));
        if (pivot != null) {
            poseStack.translate(-pivot.x, -pivot.y, -pivot.z);
        }
    }

    public Axis getAxis() {
        return axis;
    }

    public float getStartAngle() {
        return startAngle;
    }

    public float getEndAngle() {
        return endAngle;
    }

    @Nullable
    public Vec3 getPivot() {
        return pivot;
    }

    public static RotateBuilder builder() {
        return new RotateBuilder();
    }

    public static class RotateBuilder extends Animation.Builder<RotateBuilder> {
        private Axis axis = Axis.YP;
        private float startAngle = 0f;
        private float endAngle = 360f;
        @Nullable
        private Vec3 pivot = null;

        public RotateBuilder axis(Axis axis) {
            this.axis = axis;
            return this;
        }

        public RotateBuilder startAngle(float startAngle) {
            this.startAngle = startAngle;
            return this;
        }

        public RotateBuilder endAngle(float endAngle) {
            this.endAngle = endAngle;
            return this;
        }

        public RotateBuilder pivot(Vec3 pivot) {
            this.pivot = pivot;
            return this;
        }

        public RotateBuilder pivot(double x, double y, double z) {
            this.pivot = new Vec3(x, y, z);
            return this;
        }

        @Override
        public RotateAnimation build() {
            return new RotateAnimation(axis, startAngle, endAngle, pivot,
                timingType, timingEffect, easePower, duration, resetAfterAnimation);
        }
    }
}
