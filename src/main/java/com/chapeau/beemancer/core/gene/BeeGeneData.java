/**
 * ============================================================
 * [BeeGeneData.java]
 * Description: Stocke les gènes d'une abeille
 * ============================================================
 * 
 * UTILISÉ PAR:
 * - MagicBeeEntity.java, MagicBeeItem.java, BeeCreatorMenu.java
 * 
 * ============================================================
 */
package com.chapeau.beemancer.core.gene;

import net.minecraft.nbt.CompoundTag;

import java.util.*;

public class BeeGeneData {
    private final Map<GeneCategory, Gene> genes = new HashMap<>();

    public BeeGeneData() {
        // Initialiser avec les gènes par défaut pour chaque catégorie
        for (GeneCategory category : GeneCategory.getAll().values()) {
            Gene defaultGene = GeneRegistry.getDefaultGene(category);
            if (defaultGene != null) {
                genes.put(category, defaultGene);
            }
        }
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
    }

    /**
     * Crée une copie de ces données
     */
    public BeeGeneData copy() {
        BeeGeneData copy = new BeeGeneData();
        copy.genes.clear();
        copy.genes.putAll(this.genes);
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
        return tag;
    }

    /**
     * Charge depuis NBT
     */
    public void load(CompoundTag tag) {
        genes.clear();
        for (String categoryId : tag.getAllKeys()) {
            GeneCategory category = GeneCategory.byId(categoryId);
            if (category != null) {
                String geneId = tag.getString(categoryId);
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
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("BeeGeneData{");
        genes.forEach((cat, gene) -> sb.append(cat.getId()).append("=").append(gene.getId()).append(", "));
        return sb.append("}").toString();
    }
}
