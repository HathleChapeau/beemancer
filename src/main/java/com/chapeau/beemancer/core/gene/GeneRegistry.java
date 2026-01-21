/**
 * ============================================================
 * [GeneRegistry.java]
 * Description: Registre central de tous les gènes disponibles
 * ============================================================
 * 
 * UTILISÉ PAR:
 * - MagicBeeEntity, BeeCreatorMenu, GeneConfig
 * 
 * ============================================================
 */
package com.chapeau.beemancer.core.gene;

import com.chapeau.beemancer.Beemancer;

import java.util.*;
import java.util.stream.Collectors;

public class GeneRegistry {
    private static final Map<String, Gene> GENES = new HashMap<>();
    private static final Map<GeneCategory, List<Gene>> GENES_BY_CATEGORY = new HashMap<>();

    /**
     * Enregistre un gène dans le registre
     */
    public static void register(Gene gene) {
        String fullId = gene.getCategory().getId() + ":" + gene.getId();
        if (GENES.containsKey(fullId)) {
            Beemancer.LOGGER.warn("Gene {} already registered, overwriting", fullId);
        }
        GENES.put(fullId, gene);
        GENES_BY_CATEGORY.computeIfAbsent(gene.getCategory(), k -> new ArrayList<>()).add(gene);
        Beemancer.LOGGER.debug("Registered gene: {}", fullId);
    }

    /**
     * Récupère un gène par son ID complet (category:id)
     */
    public static Gene getGene(String fullId) {
        return GENES.get(fullId);
    }

    /**
     * Récupère un gène par catégorie et id
     */
    public static Gene getGene(GeneCategory category, String id) {
        return GENES.get(category.getId() + ":" + id);
    }

    /**
     * Récupère tous les gènes d'une catégorie
     */
    public static List<Gene> getGenesByCategory(GeneCategory category) {
        return GENES_BY_CATEGORY.getOrDefault(category, Collections.emptyList());
    }

    /**
     * Récupère tous les gènes
     */
    public static Collection<Gene> getAllGenes() {
        return Collections.unmodifiableCollection(GENES.values());
    }

    /**
     * Récupère toutes les catégories triées par ordre d'affichage
     */
    public static List<GeneCategory> getAllCategories() {
        return GENES_BY_CATEGORY.keySet().stream()
                .sorted(Comparator.comparingInt(GeneCategory::getDisplayOrder))
                .collect(Collectors.toList());
    }

    /**
     * Récupère le premier gène d'une catégorie (gène par défaut)
     */
    public static Gene getDefaultGene(GeneCategory category) {
        List<Gene> genes = GENES_BY_CATEGORY.get(category);
        return (genes != null && !genes.isEmpty()) ? genes.get(0) : null;
    }

    /**
     * Récupère le gène suivant dans la liste d'une catégorie
     */
    public static Gene getNextGene(Gene current) {
        List<Gene> genes = GENES_BY_CATEGORY.get(current.getCategory());
        if (genes == null || genes.isEmpty()) return current;
        
        int index = genes.indexOf(current);
        int nextIndex = (index + 1) % genes.size();
        return genes.get(nextIndex);
    }

    /**
     * Récupère le gène précédent dans la liste d'une catégorie
     */
    public static Gene getPreviousGene(Gene current) {
        List<Gene> genes = GENES_BY_CATEGORY.get(current.getCategory());
        if (genes == null || genes.isEmpty()) return current;
        
        int index = genes.indexOf(current);
        int prevIndex = (index - 1 + genes.size()) % genes.size();
        return genes.get(prevIndex);
    }

    /**
     * Vérifie si une combinaison de gènes est valide (pas d'incompatibilités)
     */
    public static boolean isValidCombination(Collection<Gene> genes) {
        List<Gene> geneList = new ArrayList<>(genes);
        for (int i = 0; i < geneList.size(); i++) {
            for (int j = i + 1; j < geneList.size(); j++) {
                if (!geneList.get(i).isCompatibleWith(geneList.get(j))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Réinitialise le registre (pour les tests)
     */
    public static void clear() {
        GENES.clear();
        GENES_BY_CATEGORY.clear();
    }
}
