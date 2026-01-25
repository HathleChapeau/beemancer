/**
 * ============================================================
 * [DataDrivenSpeciesGene.java]
 * Description: Gene d'espece charge dynamiquement depuis bee_species.json
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeSpeciesManager   | Donnees especes      | Stats et configuration         |
 * | AbstractGene        | Classe de base       | Implementation Gene            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - GeneInit.java: Enregistrement des genes
 * - MagicBeeEntity.java: Application des comportements
 *
 * ============================================================
 */
package com.chapeau.beemancer.content.gene.species;

import com.chapeau.beemancer.core.bee.BeeSpeciesManager;
import com.chapeau.beemancer.core.gene.AbstractGene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.Bee;

public class DataDrivenSpeciesGene extends AbstractGene {

    private final String tier;
    private final String texture;

    public DataDrivenSpeciesGene(String speciesId) {
        this(speciesId, "I", speciesId);
    }

    public DataDrivenSpeciesGene(String speciesId, String tier, String texture) {
        super(speciesId, GeneCategory.SPECIES);
        this.tier = tier;
        this.texture = texture;

        // Default parameters - will be updated when species data is loaded
        setParameter("speedModifier", 1.0);
        setParameter("productionModifier", 1.0);
        setParameter("tier", tier);
        setParameter("texture", texture);
    }

    @Override
    public Component getDisplayName() {
        // Use species-specific translation key
        return Component.translatable("species.beemancer." + id);
    }

    @Override
    public Component getDescription() {
        return Component.translatable("species.beemancer." + id + ".desc");
    }

    public String getTier() {
        return tier;
    }

    public String getTexture() {
        return texture;
    }

    /**
     * @return Multiplicateur de vitesse de l'abeille
     */
    public double getSpeedModifier() {
        // Try to get from BeeSpeciesManager if loaded
        BeeSpeciesManager.BeeSpeciesData data = BeeSpeciesManager.getSpecies(id);
        if (data != null && data.stats() != null) {
            int flyingSpeedLevel = data.stats().getOrDefault("flying_speed", 1);
            return BeeSpeciesManager.getLevelModifier(flyingSpeedLevel);
        }
        return getParameter("speedModifier", 1.0);
    }

    /**
     * @return Multiplicateur de production
     */
    public double getProductionModifier() {
        // Try to get from BeeSpeciesManager if loaded
        BeeSpeciesManager.BeeSpeciesData data = BeeSpeciesManager.getSpecies(id);
        if (data != null && data.stats() != null) {
            int dropLevel = data.stats().getOrDefault("drop_level", 1);
            return BeeSpeciesManager.getLevelModifier(dropLevel);
        }
        return getParameter("productionModifier", 1.0);
    }

    @Override
    public void applyBehavior(Bee bee) {
        // Behavior is applied via BeeSpeciesManager stats
        // No direct behavior modification needed here
    }

    /**
     * Creates a DataDrivenSpeciesGene from BeeSpeciesManager data.
     */
    public static DataDrivenSpeciesGene fromSpeciesData(String speciesId, BeeSpeciesManager.BeeSpeciesData data) {
        String tier = data.tier() != null ? data.tier() : "I";
        String texture = data.texture() != null ? data.texture() : speciesId;
        return new DataDrivenSpeciesGene(speciesId, tier, texture);
    }
}
