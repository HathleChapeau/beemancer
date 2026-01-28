/**
 * ============================================================
 * [RidingBehaviourState.java]
 * Description: État mutable d'un ride, détruit au démontage
 * ============================================================
 *
 * PATTERN: Copié de Cobblemon api/riding/behaviour/RidingBehaviourState.kt L25-56
 * - Contient l'état mutable passé entre client et serveur
 * - État temporaire détruit quand le rider principal démonte
 * - rideVelocity: vélocité en espace local (Z = avant)
 * - stamina: toujours 1.0f dans Beemancer (pas de gestion stamina)
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | SidedRidingState    | État avec côté       | rideVelocity, stamina          |
 * | Side                | Enum des côtés       | Définition CLIENT/SERVER       |
 * | Vec3                | Vecteur 3D           | Vélocité                       |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - RidingBehaviour.java: Passé à toutes les méthodes
 * - RidingController.java: Stocké dans ActiveRidingContext
 * - HorseState.java: Classe fille avec états supplémentaires
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount.behaviour;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * État mutable pendant le ride.
 * Détruit quand le rider principal démonte.
 *
 * Pattern Cobblemon: RidingBehaviourState
 */
public class RidingBehaviourState {

    /**
     * Vélocité en espace local du ride.
     * X = strafe (gauche/droite)
     * Y = vertical
     * Z = forward (avant/arrière)
     *
     * Modifiable uniquement côté CLIENT car calculé à partir des inputs.
     */
    protected final SidedRidingState<Vec3> rideVelocity;

    /**
     * Stamina du ride.
     * TOUJOURS 1.0f dans Beemancer (pas de gestion de stamina).
     */
    protected final SidedRidingState<Float> stamina;

    public RidingBehaviourState() {
        this.rideVelocity = new SidedRidingState<>(Vec3.ZERO, Side.CLIENT);
        this.stamina = new SidedRidingState<>(1.0f, Side.CLIENT);
    }

    // --- Getters ---

    public Vec3 getRideVelocity() {
        return rideVelocity.get();
    }

    public void setRideVelocity(Vec3 velocity) {
        rideVelocity.set(velocity);
    }

    public void setRideVelocity(Vec3 velocity, boolean forced) {
        rideVelocity.set(velocity, forced);
    }

    public float getStamina() {
        return stamina.get();
    }

    public void setStamina(float value) {
        stamina.set(value);
    }

    public void setStamina(float value, boolean forced) {
        stamina.set(value, forced);
    }

    // --- Lifecycle ---

    /**
     * Réinitialise l'état à ses valeurs par défaut.
     * Appelé quand le rider démonte.
     */
    public void reset() {
        rideVelocity.set(Vec3.ZERO, true);
        stamina.set(1.0f, true);
    }

    /**
     * Crée une copie de cet état.
     * Utilisé pour comparer si sync nécessaire.
     */
    public RidingBehaviourState copy() {
        RidingBehaviourState copy = new RidingBehaviourState();
        copy.rideVelocity.set(this.rideVelocity.get(), true);
        copy.stamina.set(this.stamina.get(), true);
        return copy;
    }

    /**
     * Vérifie si l'état a changé par rapport à un état précédent.
     * Utilisé pour déterminer si une sync réseau est nécessaire.
     */
    public boolean shouldSync(RidingBehaviourState previous) {
        if (!previous.rideVelocity.get().equals(rideVelocity.get())) return true;
        if (!previous.stamina.get().equals(stamina.get())) return true;
        return false;
    }

    // --- Network ---

    /**
     * Encode l'état dans un buffer pour envoi réseau.
     */
    public void encode(FriendlyByteBuf buffer) {
        Vec3 vel = rideVelocity.get();
        buffer.writeDouble(vel.x);
        buffer.writeDouble(vel.y);
        buffer.writeDouble(vel.z);
        buffer.writeFloat(stamina.get());
    }

    /**
     * Décode l'état depuis un buffer reçu du réseau.
     */
    public void decode(FriendlyByteBuf buffer) {
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        rideVelocity.set(new Vec3(x, y, z), true);
        stamina.set(buffer.readFloat(), true);
    }
}
