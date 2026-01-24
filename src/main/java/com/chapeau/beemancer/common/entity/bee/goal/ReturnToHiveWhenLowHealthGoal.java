/**
 * ============================================================
 * [ReturnToHiveWhenLowHealthGoal.java]
 * Description: Goal pour faire fuir l'abeille vers la ruche si vie < 30%
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagicBeeEntity      | Entité abeille       | Accès aux méthodes d'état      |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MagicBeeEntity.java: Enregistré dans registerGoals()
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
 * Goal de priorité 1: L'abeille fuit vers la ruche si sa vie tombe sous 30%.
 * Ce goal a la plus haute priorité et ne peut pas être interrompu.
 */
public class ReturnToHiveWhenLowHealthGoal extends Goal {
    
    private static final float FLEE_THRESHOLD = 0.3f; // 30%
    private static final double REACH_DISTANCE = 1.5;
    private static final double FLEE_SPEED = 0.12;
    private static final double FLIGHT_ALTITUDE = 1.0; // Altitude de vol
    
    private final MagicBeeEntity bee;
    
    public ReturnToHiveWhenLowHealthGoal(MagicBeeEntity bee) {
        this.bee = bee;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        // Vérifier si l'abeille a une ruche assignée et si elle doit fuir
        return bee.hasAssignedHive() && bee.shouldFlee();
    }
    
    @Override
    public boolean canContinueToUse() {
        // Continuer jusqu'à atteindre la ruche ou mourir
        if (!bee.hasAssignedHive()) return false;
        if (!bee.shouldFlee()) return false;
        
        BlockPos hivePos = bee.getAssignedHivePos();
        if (hivePos == null) return false;
        
        double distance = bee.position().distanceTo(Vec3.atCenterOf(hivePos));
        return distance > REACH_DISTANCE;
    }
    
    @Override
    public void start() {
        // Forcer le reset de la cible d'attaque quand on fuit
        bee.setTarget(null);
        // Signaler à la ruche qu'on revient
        bee.setReturning(true);
    }
    
    @Override
    public void tick() {
        BlockPos hivePos = bee.getAssignedHivePos();
        if (hivePos == null) return;

        Vec3 hiveVec = Vec3.atCenterOf(hivePos);
        double distance = bee.position().distanceTo(hiveVec);

        // Voler en altitude sauf quand proche de la ruche (descente finale)
        Vec3 targetVec = distance > REACH_DISTANCE * 2
                ? hiveVec.add(0, FLIGHT_ALTITUDE, 0)
                : hiveVec;

        Vec3 direction = targetVec.subtract(bee.position()).normalize();

        bee.setDeltaMovement(direction.scale(FLEE_SPEED));

        // Rotation vers la ruche
        double dx = hiveVec.x - bee.getX();
        double dz = hiveVec.z - bee.getZ();
        float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        bee.setYRot(targetYaw);
        bee.yBodyRot = targetYaw;
    }
    
    @Override
    public void stop() {
        bee.setDeltaMovement(Vec3.ZERO);
    }
    
    @Override
    public boolean isInterruptable() {
        // Ce goal ne peut pas être interrompu - la survie prime
        return false;
    }
}
