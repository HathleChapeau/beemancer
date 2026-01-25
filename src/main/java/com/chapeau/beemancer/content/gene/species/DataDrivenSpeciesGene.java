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
import org.jetbrains.annotations.Nullable;

public class DataDrivenSpeciesGene extends AbstractGene {

    private final String tier;
    private final String texture;

    // Cache pour eviter les lookups repetes
    @Nullable
    private transient BeeSpeciesManager.BeeSpeciesData cachedData = null;
    private transient boolean cacheChecked = false;

    public DataDrivenSpeciesGene(String speciesId) {
        this(speciesId, "I", capitalizeFirst(speciesId) + "_Bee");
    }

    public DataDrivenSpeciesGene(String speciesId, String tier, String texture) {
        super(speciesId, GeneCategory.SPECIES);
        this.tier = tier;
        this.texture = texture;

        setParameter("speedModifier", 1.0);
        setParameter("productionModifier", 1.0);
        setParameter("tier", tier);
        setParameter("texture", texture);
    }

    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("species.beemancer." + id);
    }

    @Override
    public Component getDescription() {
        // Utilise le nom d'affichage car les descriptions individuelles ne sont pas definies
        return getDisplayName();
    }

    public String getTier() {
        // Priorite aux donnees chargees depuis JSON
        BeeSpeciesManager.BeeSpeciesData data = getCachedData();
        return data != null ? data.tier : tier;
    }

    public String getTexture() {
        BeeSpeciesManager.BeeSpeciesData data = getCachedData();
        return data != null ? data.texture : texture;
    }

    /**
     * @return Multiplicateur de vitesse de l'abeille
     */
    public double getSpeedModifier() {
        BeeSpeciesManager.BeeSpeciesData data = getCachedData();
        if (data != null) {
            return BeeSpeciesManager.getLevelModifier(data.flyingSpeedLevel);
        }
        return getParameter("speedModifier", 1.0);
    }

    /**
     * @return Multiplicateur de production
     */
    public double getProductionModifier() {
        BeeSpeciesManager.BeeSpeciesData data = getCachedData();
        if (data != null) {
            return BeeSpeciesManager.getLevelModifier(data.dropLevel);
        }
        return getParameter("productionModifier", 1.0);
    }

    /**
     * Recupere les donnees de l'espece avec cache.
     * Le cache est invalide si BeeSpeciesManager est recharge.
     */
    @Nullable
    private BeeSpeciesManager.BeeSpeciesData getCachedData() {
        // Verifie si le manager est charge et si le cache doit etre actualise
        if (!BeeSpeciesManager.isLoaded()) {
            return null;
        }

        if (!cacheChecked) {
            cachedData = BeeSpeciesManager.getSpecies(id);
            cacheChecked = true;
        }
        return cachedData;
    }

    /**
     * Invalide le cache (appele apres rechargement des donnees).
     */
    public void invalidateCache() {
        cachedData = null;
        cacheChecked = false;
    }

    @Override
    public void applyBehavior(Bee bee) {
        // Comportement applique via BeeSpeciesManager/BeeBehaviorManager
    }
}
