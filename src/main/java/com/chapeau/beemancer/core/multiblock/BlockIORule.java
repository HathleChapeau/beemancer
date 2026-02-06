/**
 * ============================================================
 * [BlockIORule.java]
 * Description: Regles IO per-face pour un bloc a une position dans un multibloc
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | IOMode              | Mode par face        | EnumMap values         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - MultiblockIOConfig (stocke les regles par position)
 * - CentrifugeHeartBlockEntity, AlembicHeartBlockEntity (definition IO_CONFIG)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.multiblock;

import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import java.util.EnumMap;

/**
 * Definit le mode IO (NONE/INPUT/OUTPUT/BOTH) pour chaque face d'un bloc
 * a une position donnee dans un multibloc.
 * Immutable apres construction.
 */
public class BlockIORule {

    private final EnumMap<Direction, IOMode> faceModes;
    private final IOMode defaultMode;

    private BlockIORule(EnumMap<Direction, IOMode> faceModes, IOMode defaultMode) {
        this.faceModes = faceModes;
        this.defaultMode = defaultMode;
    }

    /**
     * Retourne le mode IO pour la face donnee.
     * @param face la direction de la face, ou null pour une query sans direction
     * @return le mode IO (jamais null)
     */
    public IOMode getModeFor(@Nullable Direction face) {
        if (face == null) return defaultMode;
        return faceModes.getOrDefault(face, IOMode.NONE);
    }

    /**
     * Cree une regle avec le meme mode sur toutes les 6 faces + default.
     */
    public static BlockIORule allFaces(IOMode mode) {
        EnumMap<Direction, IOMode> map = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            map.put(dir, mode);
        }
        return new BlockIORule(map, mode);
    }

    /**
     * Cree une regle avec le mode sur N/S/E/W et NONE sur UP/DOWN.
     */
    public static BlockIORule sides(IOMode mode) {
        EnumMap<Direction, IOMode> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, mode);
        map.put(Direction.SOUTH, mode);
        map.put(Direction.EAST, mode);
        map.put(Direction.WEST, mode);
        map.put(Direction.UP, IOMode.NONE);
        map.put(Direction.DOWN, IOMode.NONE);
        return new BlockIORule(map, IOMode.NONE);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final EnumMap<Direction, IOMode> faceModes = new EnumMap<>(Direction.class);
        private IOMode defaultMode = IOMode.NONE;

        public Builder face(Direction dir, IOMode mode) {
            faceModes.put(dir, mode);
            return this;
        }

        public Builder sides(IOMode mode) {
            faceModes.put(Direction.NORTH, mode);
            faceModes.put(Direction.SOUTH, mode);
            faceModes.put(Direction.EAST, mode);
            faceModes.put(Direction.WEST, mode);
            return this;
        }

        public Builder up(IOMode mode) {
            faceModes.put(Direction.UP, mode);
            return this;
        }

        public Builder down(IOMode mode) {
            faceModes.put(Direction.DOWN, mode);
            return this;
        }

        public Builder allFaces(IOMode mode) {
            for (Direction dir : Direction.values()) {
                faceModes.put(dir, mode);
            }
            this.defaultMode = mode;
            return this;
        }

        public Builder defaultMode(IOMode mode) {
            this.defaultMode = mode;
            return this;
        }

        public BlockIORule build() {
            return new BlockIORule(new EnumMap<>(faceModes), defaultMode);
        }
    }
}
