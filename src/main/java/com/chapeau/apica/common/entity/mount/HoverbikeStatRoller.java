/**
 * ============================================================
 * [HoverbikeStatRoller.java]
 * Description: Système de tirage de stats aléatoires pour les pièces de hoverbike
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | HoverbikeConfigManager  | Modifiers chargés    | Source des pools               |
 * | HoverbikePartData       | Stockage modifiers   | canAdd, addModifier            |
 * | AppliedModifier         | Record modifier      | Création après roll            |
 * | HoverbikeModifier       | Modifier définition  | Pool weights, stat objects     |
 * | HoverbikeStatObject     | Stat ranges          | Min/max par tier               |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - InteractionMarkerTypes.java (assembly_focus handler): Roll au clic Creative Focus
 *
 * ============================================================
 */
package com.chapeau.apica.common.entity.mount;

import com.chapeau.apica.common.item.mount.AppliedModifier;
import com.chapeau.apica.common.item.mount.HoverbikePartData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Système de tirage de stats pour les pièces de hoverbike.
 * Construit un pool plat pondéré par les tierPools de chaque modifier,
 * puis tire une entrée aléatoire et roll une valeur dans le range du tier.
 * Architecture flexible : le buildPool peut être étendu avec des filtres.
 */
public final class HoverbikeStatRoller {

    private HoverbikeStatRoller() {}

    /** Entrée dans le pool plat : un modifier + une stat + un tier. */
    record PoolEntry(HoverbikeModifier modifier, HoverbikeStatObject statObject, int tier) {}

    /**
     * Construit le pool plat pour un type de modifier (prefix ou suffix).
     * Pour chaque modifier du type, pour chaque statObject, pour chaque tier T1-T8,
     * ajoute tierPool[tier] entrées dans le pool.
     */
    private static List<PoolEntry> buildPool(boolean prefix) {
        List<HoverbikeModifier> mods = prefix
                ? HoverbikeConfigManager.getPrefixes()
                : HoverbikeConfigManager.getSuffixes();

        List<PoolEntry> pool = new ArrayList<>();
        for (HoverbikeModifier mod : mods) {
            for (HoverbikeStatObject stat : mod.getStatObjects()) {
                for (int tier = 1; tier <= 8; tier++) {
                    int weight = mod.getTierPool(tier);
                    for (int i = 0; i < weight; i++) {
                        pool.add(new PoolEntry(mod, stat, tier));
                    }
                }
            }
        }
        return pool;
    }

    /**
     * Roll et applique une stat aléatoire sur le stack.
     * Choisit d'abord prefix ou suffix, construit le pool, tire une entrée,
     * roll la valeur dans [tierMin, tierMax], et ajoute le modifier au stack.
     *
     * @return true si un modifier a été ajouté, false si le stack est plein ou pas de pool
     */
    public static boolean rollAndApply(ItemStack stack, RandomSource random) {
        boolean canP = HoverbikePartData.canAddPrefix(stack);
        boolean canS = HoverbikePartData.canAddSuffix(stack);
        if (!canP && !canS) return false;

        // Choisir prefix ou suffix
        boolean doPrefix;
        if (canP && canS) {
            doPrefix = random.nextBoolean();
        } else {
            doPrefix = canP;
        }

        List<PoolEntry> pool = buildPool(doPrefix);
        if (pool.isEmpty()) return false;

        // Tirer une entrée aléatoire du pool
        PoolEntry picked = pool.get(random.nextInt(pool.size()));

        // Roll la valeur dans le range du tier
        double min = picked.statObject().getTierMin(picked.tier());
        double max = picked.statObject().getTierMax(picked.tier());
        double value = min + random.nextDouble() * (max - min);

        // Créer et appliquer le modifier
        AppliedModifier applied = new AppliedModifier(
                picked.modifier().getName(),
                doPrefix,
                picked.tier(),
                picked.statObject().getStatistic(),
                picked.statObject().getValueType(),
                value
        );
        HoverbikePartData.addModifier(stack, applied);
        return true;
    }
}
