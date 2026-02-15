/**
 * ============================================================
 * [HoverbikeStatObject.java]
 * Description: Modification d'une statistique dans un modifier hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeStatType   | Enum statistique     | Identifie la stat ciblee       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeModifier.java: Contenu dans la liste stat_objects
 * - HoverbikeConfigManager.java: Parsing depuis statistics.json
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount;

/**
 * Represente une modification de statistique dans un modifier.
 * Contient la stat ciblee, le type de valeur (+/%), et les ranges min/max par tier (T1-T8).
 */
public class HoverbikeStatObject {

    private static final int TIER_COUNT = 8;

    private final HoverbikeStatType statistic;
    private final String valueType;
    private final double[][] tierRanges;

    /**
     * @param statistic  la statistique ciblee
     * @param valueType  "+" pour valeur absolue, "%" pour pourcentage
     * @param tierRanges tableau [8][2] avec [tier_index][0=min, 1=max]
     */
    public HoverbikeStatObject(HoverbikeStatType statistic, String valueType, double[][] tierRanges) {
        this.statistic = statistic;
        this.valueType = valueType;
        this.tierRanges = tierRanges;
    }

    public HoverbikeStatType getStatistic() {
        return statistic;
    }

    public String getValueType() {
        return valueType;
    }

    /**
     * Retourne le range [min, max] pour le tier donne (1-8).
     */
    public double[] getTierRange(int tier) {
        if (tier < 1 || tier > TIER_COUNT) {
            return new double[]{0, 0};
        }
        return tierRanges[tier - 1];
    }

    public double getTierMin(int tier) {
        return getTierRange(tier)[0];
    }

    public double getTierMax(int tier) {
        return getTierRange(tier)[1];
    }
}
