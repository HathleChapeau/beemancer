/**
 * ============================================================
 * [SidedRidingState.java]
 * Description: Wrapper pour état avec restriction de côté (client/server)
 * ============================================================
 *
 * PATTERN: Copié de Cobblemon api/riding/behaviour/RidingBehaviourState.kt L60-86
 * - Encapsule une valeur avec restriction de modification
 * - set() vérifie le côté avant modification
 * - forced=true bypass la restriction
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Side                | Enum des côtés       | Filtrage des modifications     |
 * | FMLEnvironment      | Détection client     | Vérification du côté actuel    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - RidingBehaviourState.java: Tous les états mutables
 * - HorseState.java: États spécifiques au cheval
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount.behaviour;

import net.neoforged.fml.loading.FMLEnvironment;

import java.util.Objects;

/**
 * État avec restriction de côté pour la modification.
 * Pattern Cobblemon: SidedRidingState<T>
 *
 * @param <T> Type de la valeur encapsulée
 */
public class SidedRidingState<T> {
    private T value;
    private final Side side;

    public SidedRidingState(T initialValue, Side side) {
        this.value = initialValue;
        this.side = side;
    }

    /**
     * Récupère la valeur actuelle.
     */
    public T get() {
        return value;
    }

    /**
     * Modifie la valeur si le côté correspond ou si forcé.
     *
     * @param newValue Nouvelle valeur
     * @param forced   Si true, bypass la vérification de côté
     */
    public void set(T newValue, boolean forced) {
        if (forced) {
            this.value = newValue;
            return;
        }

        switch (side) {
            case BOTH -> this.value = newValue;
            case CLIENT -> {
                if (isClient()) {
                    this.value = newValue;
                }
            }
            case SERVER -> {
                if (!isClient()) {
                    this.value = newValue;
                }
            }
        }
    }

    /**
     * Modifie la valeur (non forcé).
     */
    public void set(T newValue) {
        set(newValue, false);
    }

    /**
     * Vérifie si on est côté client.
     */
    private boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }

    public Side getSide() {
        return side;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SidedRidingState<?> that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
