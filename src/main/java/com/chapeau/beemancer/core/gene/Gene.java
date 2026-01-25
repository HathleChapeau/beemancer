/**
 * ============================================================
 * [Gene.java]
 * Description: Interface définissant un gène et son comportement
 * ============================================================
 * 
 * UTILISÉ PAR:
 * - Toutes les implémentations de gènes
 * - GeneRegistry.java, MagicBeeEntity.java
 * 
 * ============================================================
 */
package com.chapeau.beemancer.core.gene;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.Bee;

import java.util.List;
import java.util.Map;

public interface Gene {
    
    /**
     * @return L'identifiant unique du gène (ex: "meadow", "forest", "royal")
     */
    String getId();

    /**
     * @return La catégorie de ce gène
     */
    GeneCategory getCategory();

    /**
     * @return Le nom d'affichage traduit
     */
    Component getDisplayName();

    /**
     * @return Description du gène pour les tooltips
     */
    Component getDescription();

    /**
     * @return Liste des IDs de gènes incompatibles avec celui-ci
     */
    List<String> getIncompatibleGenes();

    /**
     * @return Paramètres de configuration du gène
     */
    Map<String, Object> getParameters();

    /**
     * Vérifie si ce gène est compatible avec un autre
     */
    default boolean isCompatibleWith(Gene other) {
        if (other == null) return true;
        return !getIncompatibleGenes().contains(other.getId()) 
                && !other.getIncompatibleGenes().contains(this.getId());
    }

    /**
     * Applique le comportement du gène à l'abeille (appelé chaque tick)
     */
    void applyBehavior(Bee bee);

    /**
     * Appelé quand le gène est assigné à une abeille
     */
    default void onAssigned(Bee bee) {}

    /**
     * Appelé quand le gène est retiré d'une abeille
     */
    default void onRemoved(Bee bee) {}
}
