/**
 * ============================================================
 * [NoOpNavigation.java]
 * Description: PathNavigation vide — empeche le pathfinding vanilla inutile
 * ============================================================
 *
 * MagicBeeEntity utilise setDeltaMovement() + Theta* custom pour se deplacer.
 * FlyingPathNavigation est du code mort qui gaspille des ticks.
 * Cette navigation no-op remplace vanilla sans rien calculer.
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | PathNavigation      | Classe parente       | Override des methodes          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - MagicBeeEntity.java: createNavigation() retourne cette instance
 *
 * ============================================================
 */
package com.chapeau.apica.common.entity.bee.pathfinding;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Navigation no-op pour MagicBeeEntity.
 * Toutes les methodes retournent des valeurs neutres sans calcul.
 */
public class NoOpNavigation extends PathNavigation {

    public NoOpNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new FlyNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    @Override
    protected boolean canUpdatePath() {
        return false;
    }

    @Override
    protected Vec3 getTempMobPos() {
        return this.mob.position();
    }

    @Override
    protected boolean canMoveDirectly(Vec3 from, Vec3 to) {
        return false;
    }

    @Override
    public Path createPath(net.minecraft.core.BlockPos pos, int accuracy) {
        return null;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public void tick() {
        // No-op: pas de calcul de navigation vanilla
    }
}
