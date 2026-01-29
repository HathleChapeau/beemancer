/**
 * ============================================================
 * [GeneCategory.java]
 * Description: Représente une catégorie de gènes (species, environment, etc.)
 * ============================================================
 * 
 * UTILISÉ PAR:
 * - Gene.java, GeneRegistry.java, MagicBeeEntity.java
 * 
 * ============================================================
 */
package com.chapeau.beemancer.core.gene;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public class GeneCategory {
    private static final Map<String, GeneCategory> CATEGORIES = new HashMap<>();
    
    private final String id;
    private final String translationKey;
    private final ChatFormatting color;
    private final int displayOrder;

    public GeneCategory(String id, ChatFormatting color, int displayOrder) {
        this.id = id;
        this.translationKey = "gene.category.beemancer." + id;
        this.color = color;
        this.displayOrder = displayOrder;
        CATEGORIES.put(id, this);
    }

    public String getId() {
        return id;
    }

    public Component getDisplayName() {
        return Component.translatable(translationKey).withStyle(color);
    }

    public ChatFormatting getColor() {
        return color;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public static GeneCategory byId(String id) {
        return CATEGORIES.get(id);
    }

    public static Map<String, GeneCategory> getAll() {
        return new HashMap<>(CATEGORIES);
    }

    // --- Catégories prédéfinies ---
    public static final GeneCategory SPECIES = new GeneCategory("species", ChatFormatting.GOLD, 0);
    public static final GeneCategory ENVIRONMENT = new GeneCategory("environment", ChatFormatting.GREEN, 1);
    public static final GeneCategory FLOWER = new GeneCategory("flower", ChatFormatting.LIGHT_PURPLE, 2);
}
