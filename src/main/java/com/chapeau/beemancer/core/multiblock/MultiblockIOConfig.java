/**
 * ============================================================
 * [MultiblockIOConfig.java]
 * Description: Configuration IO declarative pour un type de multibloc
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | IOMode              | Mode retourne        | Lookup                |
 * | BlockIORule         | Regles par position  | Stockage              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CentrifugeHeartBlockEntity (IO_CONFIG statique)
 * - AlembicHeartBlockEntity (IO_CONFIG statique)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration IO complete pour un type de multibloc.
 * Mappe chaque position relative (offset par rapport au controleur)
 * a des regles fluid et item per-face.
 * Immutable apres construction via le Builder.
 */
public class MultiblockIOConfig {

    private final Map<Vec3i, BlockIORule> fluidRules;
    private final Map<Vec3i, BlockIORule> itemRules;

    private MultiblockIOConfig(Map<Vec3i, BlockIORule> fluidRules, Map<Vec3i, BlockIORule> itemRules) {
        this.fluidRules = Map.copyOf(fluidRules);
        this.itemRules = Map.copyOf(itemRules);
    }

    /**
     * Retourne le mode IO fluid pour une position monde et une face.
     * @param controllerPos position monde du controleur
     * @param queriedPos position monde du bloc querie
     * @param face face du bloc querie (nullable)
     * @return le mode IO, ou null si aucune regle pour cette position
     */
    @Nullable
    public IOMode getFluidMode(BlockPos controllerPos, BlockPos queriedPos, @Nullable Direction face) {
        Vec3i offset = queriedPos.subtract(controllerPos);
        BlockIORule rule = fluidRules.get(offset);
        return rule != null ? rule.getModeFor(face) : null;
    }

    /**
     * Retourne le mode IO item pour une position monde et une face.
     * @param controllerPos position monde du controleur
     * @param queriedPos position monde du bloc querie
     * @param face face du bloc querie (nullable)
     * @return le mode IO, ou null si aucune regle pour cette position
     */
    @Nullable
    public IOMode getItemMode(BlockPos controllerPos, BlockPos queriedPos, @Nullable Direction face) {
        Vec3i offset = queriedPos.subtract(controllerPos);
        BlockIORule rule = itemRules.get(offset);
        return rule != null ? rule.getModeFor(face) : null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<Vec3i, BlockIORule> fluidRules = new HashMap<>();
        private final Map<Vec3i, BlockIORule> itemRules = new HashMap<>();

        /**
         * Definit la regle fluid pour un offset.
         */
        public Builder fluid(int x, int y, int z, BlockIORule rule) {
            fluidRules.put(new Vec3i(x, y, z), rule);
            return this;
        }

        /**
         * Definit la regle item pour un offset.
         */
        public Builder item(int x, int y, int z, BlockIORule rule) {
            itemRules.put(new Vec3i(x, y, z), rule);
            return this;
        }

        /**
         * Definit les regles fluid et item pour un meme offset.
         * @param fluidRule regle fluid (null pour ignorer)
         * @param itemRule regle item (null pour ignorer)
         */
        public Builder position(int x, int y, int z,
                                @Nullable BlockIORule fluidRule,
                                @Nullable BlockIORule itemRule) {
            Vec3i offset = new Vec3i(x, y, z);
            if (fluidRule != null) fluidRules.put(offset, fluidRule);
            if (itemRule != null) itemRules.put(offset, itemRule);
            return this;
        }

        public MultiblockIOConfig build() {
            return new MultiblockIOConfig(fluidRules, itemRules);
        }
    }
}
