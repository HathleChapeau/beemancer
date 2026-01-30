/**
 * ============================================================
 * [MoveToTargetGoal.java]
 * Description: Goal de navigation manuelle via BeeWand pour MagicBeeEntity
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagicBeeEntity      | Entité bee           | hasTarget(), getTargetPos()    |
 * | Goal                | Base AI goal         | Système de goals Minecraft     |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MagicBeeEntity.java (registerGoals)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.bee.goal;

import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Goal pour déplacer l'abeille vers une position cible définie par le joueur (BeeWand).
 * Se désactive quand la cible est atteinte ou supprimée.
 */
public class MoveToTargetGoal extends Goal {
    private final MagicBeeEntity bee;
    private static final double REACH_DISTANCE = 1.5;

    public MoveToTargetGoal(MagicBeeEntity bee) {
        this.bee = bee;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return bee.hasTarget();
    }

    @Override
    public boolean canContinueToUse() {
        if (!bee.hasTarget()) return false;
        BlockPos target = bee.getTargetPos();
        if (target == null) return false;
        double distance = bee.position().distanceTo(Vec3.atCenterOf(target));
        return distance > REACH_DISTANCE;
    }

    @Override
    public void tick() {
        BlockPos target = bee.getTargetPos();
        if (target == null) return;

        Vec3 targetVec = Vec3.atCenterOf(target);
        Vec3 direction = targetVec.subtract(bee.position()).normalize();
        double speed = 0.08;

        Vec3 movement = direction.scale(speed);
        bee.setDeltaMovement(movement);

        double dx = targetVec.x - bee.getX();
        double dz = targetVec.z - bee.getZ();
        float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        bee.setYRot(targetYaw);
        bee.yBodyRot = targetYaw;
    }

    @Override
    public void stop() {
        bee.setDeltaMovement(Vec3.ZERO);
    }
}
