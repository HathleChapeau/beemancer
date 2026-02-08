/**
 * ============================================================
 * [MoveAnimation.java]
 * Description: Animation de translation entre deux points
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | Animation                     | Classe parent        | Pipeline play/pause/stop       |
 * | TimingType                    | Easing               | Via Animation                  |
 * | TimingEffect                  | Effet temporel       | Via Animation                  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - AnimationController.java (gestion)
 * - Tout renderer qui anime un deplacement
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Animation de translation entre un point de depart et un point d'arrivee.
 * Interpole lineairement entre les deux positions apres application de l'easing.
 */
public class MoveAnimation extends Animation {

    private final Vec3 start;
    private final Vec3 end;

    private MoveAnimation(Vec3 start, Vec3 end,
                           TimingType timingType, TimingEffect timingEffect,
                           float easePower, float duration, boolean resetAfterAnimation) {
        super(timingType, timingEffect, easePower, duration, resetAfterAnimation);
        this.start = start;
        this.end = end;
    }

    @Override
    protected void doApply(PoseStack poseStack, float progress) {
        double x = Mth.lerp(progress, start.x, end.x);
        double y = Mth.lerp(progress, start.y, end.y);
        double z = Mth.lerp(progress, start.z, end.z);
        poseStack.translate(x, y, z);
    }

    public Vec3 getStart() {
        return start;
    }

    public Vec3 getEnd() {
        return end;
    }

    public static MoveBuilder builder() {
        return new MoveBuilder();
    }

    public static class MoveBuilder extends Animation.Builder<MoveBuilder> {
        private Vec3 start = Vec3.ZERO;
        private Vec3 end = Vec3.ZERO;

        public MoveBuilder from(Vec3 start) {
            this.start = start;
            return this;
        }

        public MoveBuilder from(double x, double y, double z) {
            this.start = new Vec3(x, y, z);
            return this;
        }

        public MoveBuilder to(Vec3 end) {
            this.end = end;
            return this;
        }

        public MoveBuilder to(double x, double y, double z) {
            this.end = new Vec3(x, y, z);
            return this;
        }

        @Override
        public MoveAnimation build() {
            return new MoveAnimation(start, end, timingType, timingEffect,
                easePower, duration, resetAfterAnimation);
        }
    }
}
