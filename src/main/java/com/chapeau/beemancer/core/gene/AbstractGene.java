/**
 * ============================================================
 * [AbstractGene.java]
 * Description: Classe abstraite de base pour les gènes
 * ============================================================
 * 
 * UTILISÉ PAR:
 * - Toutes les implémentations concrètes de gènes
 * 
 * ============================================================
 */
package com.chapeau.beemancer.core.gene;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.Bee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractGene implements Gene {
    protected final String id;
    protected final GeneCategory category;
    protected final List<String> incompatibleGenes;
    protected final Map<String, Object> parameters;

    protected AbstractGene(String id, GeneCategory category) {
        this.id = id;
        this.category = category;
        this.incompatibleGenes = new ArrayList<>();
        this.parameters = new HashMap<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public GeneCategory getCategory() {
        return category;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gene.beemancer." + category.getId() + "." + id);
    }

    @Override
    public Component getDescription() {
        return Component.translatable("gene.beemancer." + category.getId() + "." + id + ".desc");
    }

    @Override
    public List<String> getIncompatibleGenes() {
        return incompatibleGenes;
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Ajoute un gène incompatible
     */
    protected void addIncompatible(String geneId) {
        incompatibleGenes.add(geneId);
    }

    /**
     * Définit un paramètre
     */
    protected void setParameter(String key, Object value) {
        parameters.put(key, value);
    }

    /**
     * Récupère un paramètre avec valeur par défaut
     */
    @SuppressWarnings("unchecked")
    protected <T> T getParameter(String key, T defaultValue) {
        Object value = parameters.get(key);
        if (value == null) return defaultValue;
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    @Override
    public void applyBehavior(Bee bee) {
        // Comportement par défaut: rien
        // À surcharger dans les sous-classes
    }
}
