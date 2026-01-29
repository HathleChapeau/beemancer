/**
 * ============================================================
 * [BeeGeneData.java]
 * Description: Stocke les genes d'une abeille
 * ============================================================
 *
 * UTILISE PAR:
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
        // Initialiser avec les genes par defaut pour chaque categorie
        for (GeneCategory category : GeneCategory.getAll().values()) {
            Gene defaultGene = GeneRegistry.getDefaultGene(category);
            if (defaultGene != null) {
                genes.put(category, defaultGene);
            }
        }
    }

    /**
     * Recupere le gene d'une categorie
     */
    public Gene getGene(GeneCategory category) {
        return genes.get(category);
    }

    /**
     * Definit le gene d'une categorie
     * @return true si le changement est valide et a ete applique
     */
    public boolean setGene(Gene gene) {
        if (gene == null) return false;

        // Verifier la compatibilite avec les autres genes
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
     * Recupere tous les genes
     */
    public Collection<Gene> getAllGenes() {
        return Collections.unmodifiableCollection(genes.values());
    }

    /**
     * Recupere tous les genes tries par ordre de categorie
     */
    public List<Gene> getGenesSorted() {
        return genes.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getKey().getDisplayOrder()))
                .map(Map.Entry::getValue)
                .toList();
    }

    /**
     * Copie les donnees d'un autre BeeGeneData
     */
    public void copyFrom(BeeGeneData other) {
        this.genes.clear();
        this.genes.putAll(other.genes);
    }

    /**
     * Cree une copie de ces donnees
     */
    public BeeGeneData copy() {
        BeeGeneData copy = new BeeGeneData();
        copy.genes.clear();
        copy.genes.putAll(this.genes);
        return copy;
    }

    /**
     * Serialise vers NBT
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
        for (String key : tag.getAllKeys()) {
            GeneCategory category = GeneCategory.byId(key);
            if (category != null) {
                String geneId = tag.getString(key);
                Gene gene = GeneRegistry.getGene(category, geneId);
                if (gene != null) {
                    genes.put(category, gene);
                }
            }
        }

        // Remplir les categories manquantes avec les valeurs par defaut
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
