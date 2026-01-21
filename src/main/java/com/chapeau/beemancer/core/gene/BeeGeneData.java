/**
 * ============================================================
 * [BeeGeneData.java]
 * Description: Stocke les gènes d'une abeille + durée de vie
 * ============================================================
 * 
 * UTILISÉ PAR:
 * - MagicBeeEntity.java, MagicBeeItem.java, BeeCreatorMenu.java
 * 
 * ============================================================
 */
package com.chapeau.beemancer.core.gene;

import com.chapeau.beemancer.content.gene.lifetime.LifetimeGene;
import net.minecraft.nbt.CompoundTag;

import java.util.*;

public class BeeGeneData {
    private final Map<GeneCategory, Gene> genes = new HashMap<>();
    
    // Lifetime tracking
    private int remainingLifetime = 24000; // Ticks restants
    private int maxLifetime = 24000;       // Ticks maximum

    public BeeGeneData() {
        // Initialiser avec les gènes par défaut pour chaque catégorie
        for (GeneCategory category : GeneCategory.getAll().values()) {
            Gene defaultGene = GeneRegistry.getDefaultGene(category);
            if (defaultGene != null) {
                genes.put(category, defaultGene);
            }
        }
        // Initialiser lifetime depuis le gène
        initializeLifetimeFromGene();
    }

    /**
     * Initialise les valeurs de lifetime depuis le gène LIFETIME
     */
    public void initializeLifetimeFromGene() {
        Gene lifetimeGene = genes.get(GeneCategory.LIFETIME);
        if (lifetimeGene instanceof LifetimeGene lg) {
            this.maxLifetime = lg.getMaxLifetimeTicks();
            this.remainingLifetime = this.maxLifetime;
        }
    }

    /**
     * @return Ticks de vie restants
     */
    public int getRemainingLifetime() {
        return remainingLifetime;
    }

    /**
     * Définit les ticks de vie restants
     */
    public void setRemainingLifetime(int ticks) {
        this.remainingLifetime = Math.max(0, ticks);
    }

    /**
     * @return Ticks de vie maximum
     */
    public int getMaxLifetime() {
        return maxLifetime;
    }

    /**
     * @return Ratio de vie restante (0.0 à 1.0)
     */
    public float getLifetimeRatio() {
        if (maxLifetime <= 0) return 1.0f;
        return (float) remainingLifetime / maxLifetime;
    }

    /**
     * Décrémente le lifetime de N ticks
     * @return true si l'abeille est encore en vie
     */
    public boolean decrementLifetime(int ticks) {
        remainingLifetime = Math.max(0, remainingLifetime - ticks);
        return remainingLifetime > 0;
    }

    /**
     * Récupère le gène d'une catégorie
     */
    public Gene getGene(GeneCategory category) {
        return genes.get(category);
    }

    /**
     * Définit le gène d'une catégorie
     * @return true si le changement est valide et a été appliqué
     */
    public boolean setGene(Gene gene) {
        if (gene == null) return false;
        
        // Vérifier la compatibilité avec les autres gènes
        for (Map.Entry<GeneCategory, Gene> entry : genes.entrySet()) {
            if (entry.getKey() != gene.getCategory()) {
                if (!gene.isCompatibleWith(entry.getValue())) {
                    return false;
                }
            }
        }
        
        genes.put(gene.getCategory(), gene);
        
        // Si on change le gène de lifetime, réinitialiser
        if (gene.getCategory() == GeneCategory.LIFETIME) {
            initializeLifetimeFromGene();
        }
        
        return true;
    }

    /**
     * Récupère tous les gènes
     */
    public Collection<Gene> getAllGenes() {
        return Collections.unmodifiableCollection(genes.values());
    }

    /**
     * Récupère tous les gènes triés par ordre de catégorie
     */
    public List<Gene> getGenesSorted() {
        return genes.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getKey().getDisplayOrder()))
                .map(Map.Entry::getValue)
                .toList();
    }

    /**
     * Copie les données d'un autre BeeGeneData
     */
    public void copyFrom(BeeGeneData other) {
        this.genes.clear();
        this.genes.putAll(other.genes);
        this.remainingLifetime = other.remainingLifetime;
        this.maxLifetime = other.maxLifetime;
    }

    /**
     * Crée une copie de ces données
     */
    public BeeGeneData copy() {
        BeeGeneData copy = new BeeGeneData();
        copy.genes.clear();
        copy.genes.putAll(this.genes);
        copy.remainingLifetime = this.remainingLifetime;
        copy.maxLifetime = this.maxLifetime;
        return copy;
    }

    /**
     * Sérialise vers NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<GeneCategory, Gene> entry : genes.entrySet()) {
            tag.putString(entry.getKey().getId(), entry.getValue().getId());
        }
        // Sauvegarder le lifetime
        tag.putInt("remainingLifetime", remainingLifetime);
        tag.putInt("maxLifetime", maxLifetime);
        return tag;
    }

    /**
     * Charge depuis NBT
     */
    public void load(CompoundTag tag) {
        genes.clear();
        for (String key : tag.getAllKeys()) {
            // Ignorer les clés spéciales
            if (key.equals("remainingLifetime") || key.equals("maxLifetime")) continue;
            
            GeneCategory category = GeneCategory.byId(key);
            if (category != null) {
                String geneId = tag.getString(key);
                Gene gene = GeneRegistry.getGene(category, geneId);
                if (gene != null) {
                    genes.put(category, gene);
                }
            }
        }
        
        // Remplir les catégories manquantes avec les valeurs par défaut
        for (GeneCategory category : GeneCategory.getAll().values()) {
            if (!genes.containsKey(category)) {
                Gene defaultGene = GeneRegistry.getDefaultGene(category);
                if (defaultGene != null) {
                    genes.put(category, defaultGene);
                }
            }
        }
        
        // Charger le lifetime
        if (tag.contains("remainingLifetime")) {
            this.remainingLifetime = tag.getInt("remainingLifetime");
            this.maxLifetime = tag.getInt("maxLifetime");
        } else {
            // Si pas de données lifetime, initialiser depuis le gène
            initializeLifetimeFromGene();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("BeeGeneData{");
        genes.forEach((cat, gene) -> sb.append(cat.getId()).append("=").append(gene.getId()).append(", "));
        sb.append("lifetime=").append(remainingLifetime).append("/").append(maxLifetime);
        return sb.append("}").toString();
    }
}
