/**
 * ============================================================
 * [RidingBehaviours.java]
 * Description: Registre singleton des comportements de ride
 * ============================================================
 *
 * PATTERN: Copié de Cobblemon api/riding/behaviour/RidingBehaviours.kt
 * - Registre global des RidingBehaviour
 * - Une seule instance de chaque behaviour (stateless)
 * - Lookup par ResourceLocation key
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | RidingBehaviour     | Interface behaviour  | Type stocké                    |
 * | ResourceLocation    | Clé unique           | Index du map                   |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - RidingController.java: Récupération du behaviour actif
 * - Beemancer.java: Enregistrement au démarrage
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount.behaviour;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Registre singleton des comportements de ride.
 * Pattern Cobblemon: RidingBehaviours object
 */
public final class RidingBehaviours {

    private static final Map<ResourceLocation, RidingBehaviour<?, ?>> BEHAVIOURS = new HashMap<>();

    private RidingBehaviours() {
        // Singleton
    }

    /**
     * Enregistre un nouveau comportement.
     *
     * @param key       Clé unique (ex: "beemancer:land/horse")
     * @param behaviour Instance du comportement
     * @throws IllegalStateException Si la clé est déjà enregistrée
     */
    public static void register(ResourceLocation key, RidingBehaviour<?, ?> behaviour) {
        if (BEHAVIOURS.containsKey(key)) {
            throw new IllegalStateException("Behaviour already registered to key " + key);
        }
        BEHAVIOURS.put(key, behaviour);
    }

    /**
     * Récupère un comportement par sa clé.
     *
     * @param key Clé du comportement
     * @return Le comportement (cast nécessaire)
     * @throws IllegalStateException Si la clé n'est pas enregistrée
     */
    @SuppressWarnings("unchecked")
    public static <S extends RidingBehaviourSettings, T extends RidingBehaviourState>
    RidingBehaviour<S, T> get(ResourceLocation key) {
        if (!BEHAVIOURS.containsKey(key)) {
            throw new IllegalStateException("Behaviour not registered to key " + key);
        }
        return (RidingBehaviour<S, T>) BEHAVIOURS.get(key);
    }

    /**
     * Vérifie si un comportement est enregistré.
     */
    public static boolean contains(ResourceLocation key) {
        return BEHAVIOURS.containsKey(key);
    }

    /**
     * Initialise les comportements par défaut de Beemancer.
     * Appelé au démarrage du mod.
     */
    public static void init() {
        // Enregistré dans Beemancer.java après création de HorseBehaviour
    }
}
