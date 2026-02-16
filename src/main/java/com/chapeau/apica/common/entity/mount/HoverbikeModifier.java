/**
 * ============================================================
 * [HoverbikeModifier.java]
 * Description: Entree de modifier pour le systeme de statistiques hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeStatObject | Modifications stats  | Liste des modifications        |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeConfigManager.java: Stocke les modifiers charges
 *
 * ============================================================
 */
package com.chapeau.apica.common.entity.mount;

import java.util.List;

/**
 * Represente un modifier hoverbike charge depuis statistics.json.
 * Contient un nom, un flag prefix/suffix, des tags, des poids de pool par tier,
 * et une liste de modifications de statistiques.
 */
public class HoverbikeModifier {

    private static final int TIER_COUNT = 8;

    private final String name;
    private final boolean isPrefix;
    private final List<String> tags;
    private final int[] tierPools;
    private final List<HoverbikeStatObject> statObjects;

    /**
     * @param name        nom du modifier
     * @param isPrefix    true si prefix, false si suffix
     * @param tags        tags referencant hoverbike_tags.json
     * @param tierPools   poids de pool [T1_pool ... T8_pool]
     * @param statObjects liste des modifications de statistiques
     */
    public HoverbikeModifier(String name, boolean isPrefix, List<String> tags,
                             int[] tierPools, List<HoverbikeStatObject> statObjects) {
        this.name = name;
        this.isPrefix = isPrefix;
        this.tags = List.copyOf(tags);
        this.tierPools = tierPools;
        this.statObjects = List.copyOf(statObjects);
    }

    public String getName() {
        return name;
    }

    public boolean isPrefix() {
        return isPrefix;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<HoverbikeStatObject> getStatObjects() {
        return statObjects;
    }

    /**
     * Retourne le poids de pool pour le tier donne (1-8).
     */
    public int getTierPool(int tier) {
        if (tier < 1 || tier > TIER_COUNT) {
            return 0;
        }
        return tierPools[tier - 1];
    }
}
