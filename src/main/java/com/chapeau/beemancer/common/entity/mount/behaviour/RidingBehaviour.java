/**
 * ============================================================
 * [RidingBehaviour.java]
 * Description: Interface stateless définissant le comportement d'un ride
 * ============================================================
 *
 * PATTERN: Copié de Cobblemon api/riding/behaviour/RidingBehaviour.kt L33-144
 * - STATELESS: partagé entre toutes les instances
 * - Contient la logique de déplacement, rotation, saut, etc.
 * - Reçoit Settings (constants) et State (mutable) à chaque appel
 *
 * PRINCIPE:
 * - tick(): appelé chaque tick, met à jour l'état
 * - velocity(): calcule la vélocité en espace local
 * - rotation(): calcule la rotation de l'entité
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | RidingBehaviourSettings | Settings constants   | Paramètres du ride             |
 * | RidingBehaviourState    | État mutable         | Vélocité, états temporaires    |
 * | RidingStyle             | Type de terrain      | Identification du style        |
 * | ResourceLocation        | Clé unique           | Identification du behaviour    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - RidingController.java: Appelle les méthodes du behaviour actif
 * - RidingBehaviours.java: Registre des behaviours
 * - HorseBehaviour.java: Implémentation concrète
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount.behaviour;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Interface définissant le comportement d'un ride.
 * STATELESS - une seule instance partagée entre toutes les entités.
 *
 * Pattern Cobblemon: RidingBehaviour<Settings, State>
 *
 * @param <S> Type des settings (ex: HorseSettings)
 * @param <T> Type de l'état (ex: HorseState)
 */
public interface RidingBehaviour<S extends RidingBehaviourSettings, T extends RidingBehaviourState> {

    /**
     * Clé unique identifiant ce comportement.
     * Ex: "beemancer:land/horse"
     */
    ResourceLocation getKey();

    /**
     * Style de déplacement de ce comportement.
     */
    RidingStyle getRidingStyle(S settings, T state);

    /**
     * Vérifie si ce comportement est actif pour l'entité.
     * Utilisé pour les transitions automatiques.
     */
    boolean isActive(S settings, T state, LivingEntity vehicle);

    /**
     * Appelé chaque tick quand le ride est actif.
     * Met à jour l'état (sprinting, walking, etc.)
     *
     * @param settings Settings constants
     * @param state    État mutable
     * @param vehicle  L'entité montée
     * @param driver   Le joueur qui contrôle
     * @param input    Input de mouvement (xxa, zza)
     */
    default void tick(S settings, T state, LivingEntity vehicle, Player driver, Vec3 input) {}

    /**
     * Calcule la vitesse actuelle du ride.
     */
    float speed(S settings, T state, LivingEntity vehicle, Player driver);

    /**
     * Calcule la rotation de l'entité.
     * Retourne Vec2(xRot/pitch, yRot/yaw).
     */
    Vec2 rotation(S settings, T state, LivingEntity vehicle, LivingEntity driver);

    /**
     * Calcule la vélocité en ESPACE LOCAL.
     * X = strafe, Y = vertical, Z = forward
     *
     * Cette vélocité sera convertie en espace monde dans travel().
     */
    Vec3 velocity(S settings, T state, LivingEntity vehicle, Player driver, Vec3 input);

    /**
     * Vérifie si le ride peut sauter.
     */
    boolean canJump(S settings, T state, LivingEntity vehicle, Player driver);

    /**
     * Calcule la force de saut.
     *
     * @param jumpStrength Intensité du saut (0-100, vanilla horse)
     */
    Vec3 jumpForce(S settings, T state, LivingEntity vehicle, Player driver, int jumpStrength);

    /**
     * Retourne la gravité à appliquer.
     * Retourne 0 si le behaviour gère sa propre gravité.
     */
    double gravity(S settings, T state, LivingEntity vehicle, double regularGravity);

    /**
     * Inertie pour le lissage de vélocité (0.0 à 1.0).
     * Plus haut = moins de lissage, réponse plus directe.
     */
    double inertia(S settings, T state, LivingEntity vehicle);

    /**
     * Vérifie si le rider peut descendre avec Shift.
     */
    default boolean dismountOnShift(S settings, T state, LivingEntity vehicle) {
        return true;
    }

    /**
     * Crée l'état par défaut pour ce comportement.
     */
    T createDefaultState(S settings);

    // --- Helpers statiques ---

    /**
     * Scale une valeur entre min et max vers [0, 1].
     * Pattern Cobblemon: RidingBehaviour.scaleToRange
     */
    static double scaleToRange(double x, double min, double max) {
        if ((max - min) < 0.01) return 0.0;
        double result = (x - min) / (max - min);
        return Math.max(0.0, Math.min(1.0, result));
    }
}
