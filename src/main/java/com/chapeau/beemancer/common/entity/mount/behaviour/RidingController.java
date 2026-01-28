/**
 * ============================================================
 * [RidingController.java]
 * Description: Orchestre les transitions de comportement et tick le ride
 * ============================================================
 *
 * PATTERN: Copié de Cobblemon api/riding/behaviour/RidingController.kt L31-150
 * - Wrapper autour d'un RidingBehaviour
 * - Gère les transitions entre styles (LAND/LIQUID/AIR)
 * - Maintient le contexte actif (behaviour + settings + state)
 *
 * SIMPLIFICATION Beemancer:
 * - Pas de multi-style pour l'instant (uniquement LAND)
 * - Pas de transitions automatiques complexes
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | RidingBehaviour         | Interface behaviour  | Délégation des appels          |
 * | RidingBehaviourSettings | Settings constants   | Stocké dans contexte           |
 * | RidingBehaviourState    | État mutable         | Stocké dans contexte           |
 * | ActiveRidingContext     | Contexte complet     | Encapsulation                  |
 * | RidingBehaviours        | Registre             | Lookup du behaviour            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - RideableBeeEntity.java: Stocke et utilise le controller
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount.behaviour;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Orchestre les comportements de ride pour une entité.
 * Pattern Cobblemon: RidingController
 */
public class RidingController {

    private final LivingEntity entity;
    private final RidingBehaviourSettings defaultSettings;

    private int lastTransitionAge = 0;

    @Nullable
    private ActiveRidingContext context;

    /**
     * Crée un nouveau controller pour une entité.
     *
     * @param entity          L'entité montable
     * @param defaultSettings Les settings par défaut à utiliser
     */
    public RidingController(LivingEntity entity, RidingBehaviourSettings defaultSettings) {
        this.entity = entity;
        this.defaultSettings = defaultSettings;
    }

    /**
     * Initialise le contexte avec le behaviour par défaut.
     * Appelé quand un joueur monte.
     */
    public void initContext() {
        if (context != null) return;

        ResourceLocation key = defaultSettings.getKey();
        RidingBehaviour<RidingBehaviourSettings, RidingBehaviourState> behaviour = RidingBehaviours.get(key);
        RidingBehaviourState state = behaviour.createDefaultState(defaultSettings);

        // Stamina toujours à 1.0 dans Beemancer
        state.setStamina(1.0f, true);

        context = new ActiveRidingContext(
                key,
                defaultSettings,
                state,
                RidingStyle.LAND
        );
        lastTransitionAge = entity.tickCount;
    }

    /**
     * Tick le controller.
     * Appelé chaque tick dans l'entité.
     */
    public void tick() {
        // Pas de transitions automatiques pour l'instant
        // Les transitions seront ajoutées plus tard si nécessaire
    }

    /**
     * Récupère le behaviour actif.
     *
     * @return Le behaviour ou null si pas de contexte
     */
    @Nullable
    public RidingBehaviour<RidingBehaviourSettings, RidingBehaviourState> getBehaviour() {
        if (context == null) return null;
        return RidingBehaviours.get(context.getBehaviourKey());
    }

    /**
     * Récupère le contexte actif.
     */
    @Nullable
    public ActiveRidingContext getContext() {
        return context;
    }

    /**
     * Réinitialise le controller.
     * Appelé quand le rider démonte.
     */
    public void reset() {
        if (context != null) {
            context.getState().reset();
        }
        context = null;
    }

    /**
     * Vérifie si le riding est disponible.
     */
    public boolean isRidingAvailable() {
        return context != null && getBehaviour() != null;
    }

    /**
     * Exécute une action si le riding est disponible.
     */
    public void ifRidingAvailable(RidingAction action) {
        if (!isRidingAvailable()) return;

        RidingBehaviour<RidingBehaviourSettings, RidingBehaviourState> behaviour = getBehaviour();
        if (behaviour == null) return;

        action.execute(behaviour, context.getSettings(), context.getState());
    }

    /**
     * Exécute une action et retourne un résultat, ou la valeur par défaut.
     */
    public <R> R ifRidingAvailableSupply(R fallback, RidingSupplier<R> supplier) {
        if (!isRidingAvailable()) return fallback;

        RidingBehaviour<RidingBehaviourSettings, RidingBehaviourState> behaviour = getBehaviour();
        if (behaviour == null) return fallback;

        return supplier.get(behaviour, context.getSettings(), context.getState());
    }

    // --- Functional Interfaces ---

    @FunctionalInterface
    public interface RidingAction {
        void execute(
                RidingBehaviour<RidingBehaviourSettings, RidingBehaviourState> behaviour,
                RidingBehaviourSettings settings,
                RidingBehaviourState state
        );
    }

    @FunctionalInterface
    public interface RidingSupplier<R> {
        R get(
                RidingBehaviour<RidingBehaviourSettings, RidingBehaviourState> behaviour,
                RidingBehaviourSettings settings,
                RidingBehaviourState state
        );
    }
}
