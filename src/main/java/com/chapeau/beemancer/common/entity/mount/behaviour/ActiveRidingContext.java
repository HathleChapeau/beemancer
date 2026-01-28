/**
 * ============================================================
 * [ActiveRidingContext.java]
 * Description: Contexte actif contenant behaviour + settings + state + style
 * ============================================================
 *
 * PATTERN: Copié de Cobblemon api/riding/behaviour/RidingController.kt L152-157
 * - Encapsule toutes les informations du ride actif
 * - Créé lors d'une transition de comportement
 * - Détruit quand le rider démonte ou change de style
 *
 * UTILISÉ PAR:
 * - RidingController.java: Stockage du contexte actif
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount.behaviour;

import net.minecraft.resources.ResourceLocation;

/**
 * Contexte actif d'un ride.
 * Contient le behaviour, ses settings, son état et le style.
 *
 * Pattern Cobblemon: ActiveRidingContext
 */
public class ActiveRidingContext {
    private final ResourceLocation behaviourKey;
    private final RidingBehaviourSettings settings;
    private final RidingBehaviourState state;
    private final RidingStyle style;

    public ActiveRidingContext(
            ResourceLocation behaviourKey,
            RidingBehaviourSettings settings,
            RidingBehaviourState state,
            RidingStyle style
    ) {
        this.behaviourKey = behaviourKey;
        this.settings = settings;
        this.state = state;
        this.style = style;
    }

    public ResourceLocation getBehaviourKey() {
        return behaviourKey;
    }

    public RidingBehaviourSettings getSettings() {
        return settings;
    }

    public RidingBehaviourState getState() {
        return state;
    }

    public RidingStyle getStyle() {
        return style;
    }
}
