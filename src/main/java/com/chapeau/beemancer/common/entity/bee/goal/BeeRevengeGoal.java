/**
 * ============================================================
 * [BeeRevengeGoal.java]
 * Description: Goal pour faire attaquer l'abeille quand elle est blessée
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagicBeeEntity      | Entité abeille       | Accès aux méthodes d'état      |
 * | BeeBehaviorConfig   | Configuration        | Paramètres d'agressivité       |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MagicBeeEntity.java: Enregistré dans registerGoals()
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.bee.goal;

import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.core.behavior.BeeBehaviorConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Goal de priorité 2: L'abeille attaque celui qui l'a blessée.
 * Hérite de HurtByTargetGoal pour le ciblage automatique.
 *
 * L'état enragé utilise un timer (MagicBeeEntity.DEFAULT_ENRAGED_DURATION = 10 sec)
 * qui est automatiquement décrémenté dans MagicBeeEntity.tick().
 * Quand le timer expire, l'abeille retourne à son comportement normal.
 */
public class BeeRevengeGoal extends HurtByTargetGoal {

    private static final double ATTACK_RANGE = 1.5;
    private static final int ATTACK_COOLDOWN = 20; // 1 seconde entre les attaques

    private final MagicBeeEntity bee;
    private int attackCooldown = 0;
    
    public BeeRevengeGoal(MagicBeeEntity bee) {
        super(bee);
        this.bee = bee;
        this.setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE));
    }
    
    @Override
    public boolean canUse() {
        // Ne pas attaquer si on doit fuir
        if (bee.shouldFlee()) return false;
        
        // Vérifier si on a été blessé récemment
        LivingEntity attacker = bee.getLastHurtByMob();
        if (attacker == null || !attacker.isAlive()) return false;
        
        // Vérifier les paramètres d'agressivité
        BeeBehaviorConfig config = bee.getBehaviorConfig();
        
        if (attacker instanceof Player && !config.isAggressiveToPlayers()) {
            // Quand même attaquer si on a été directement blessé
            return bee.isEnraged();
        }
        
        return true;
    }
    
    @Override
    public boolean canContinueToUse() {
        // Arrêter si on doit fuir
        if (bee.shouldFlee()) return false;
        
        LivingEntity target = bee.getTarget();
        return target != null && target.isAlive() && bee.isEnraged();
    }
    
    @Override
    public void start() {
        super.start();
        // Le timer enragé est déjà activé dans MagicBeeEntity.hurt()
        // On s'assure qu'il est bien actif pour ce goal
        if (!bee.isEnraged()) {
            bee.setEnraged(true);
        }
        attackCooldown = 0;
    }
    
    @Override
    public void tick() {
        LivingEntity target = bee.getTarget();
        if (target == null) return;
        
        // Décrémenter le cooldown
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        
        double distance = bee.distanceTo(target);
        
        if (distance <= ATTACK_RANGE) {
            // À portée d'attaque
            if (attackCooldown <= 0) {
                bee.doHurtTarget(target);
                attackCooldown = ATTACK_COOLDOWN;
            }
            bee.setDeltaMovement(Vec3.ZERO);
        } else {
            // Poursuivre la cible
            Vec3 targetVec = target.position().add(0, target.getBbHeight() / 2, 0);
            Vec3 direction = targetVec.subtract(bee.position()).normalize();
            
            // Utiliser la vitesse enragée
            double speed = bee.getBehaviorConfig().getEnragedFlyingSpeed() / 10.0;
            bee.setDeltaMovement(direction.scale(speed));
            
            // Rotation vers la cible
            double dx = target.getX() - bee.getX();
            double dz = target.getZ() - bee.getZ();
            float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            bee.setYRot(targetYaw);
            bee.yBodyRot = targetYaw;
        }
    }
    
    @Override
    public void stop() {
        super.stop();
        bee.setDeltaMovement(Vec3.ZERO);
        // Le timer enragé continue de décrémenter naturellement dans MagicBeeEntity.tick()
        // L'état enragé expire automatiquement après DEFAULT_ENRAGED_DURATION (10 sec)
    }
}
