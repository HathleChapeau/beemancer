/**
 * ============================================================
 * [MultiblockPattern.java]
 * Description: Définition d'un pattern de multibloc réutilisable
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | BlockMatcher        | Validation blocs     | Predicates            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MultiblockPatterns.java (registre)
 * - MultiblockValidator.java (validation)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

import java.util.ArrayList;
import java.util.List;

/**
 * Définit un pattern de multibloc de manière déclarative.
 * Les positions sont relatives au bloc contrôleur (0,0,0).
 */
public class MultiblockPattern {

    private final String id;
    private final List<PatternElement> elements = new ArrayList<>();
    private final Vec3i size;

    private MultiblockPattern(String id, Vec3i size) {
        this.id = id;
        this.size = size;
    }

    public String getId() {
        return id;
    }

    public Vec3i getSize() {
        return size;
    }

    public List<PatternElement> getElements() {
        return elements;
    }

    /**
     * Retourne toutes les positions qui font partie du multibloc (hors air).
     */
    public List<Vec3i> getStructurePositions() {
        return getStructurePositions(0);
    }

    /**
     * Retourne toutes les positions qui font partie du multibloc (hors air),
     * avec rotation horizontale appliquée.
     * @param rotation 0=0°, 1=90°, 2=180°, 3=270° (sens horaire vu du dessus)
     */
    public List<Vec3i> getStructurePositions(int rotation) {
        List<Vec3i> positions = new ArrayList<>();
        for (PatternElement element : elements) {
            if (!BlockMatcher.isAirMatcher(element.matcher())) {
                positions.add(rotateY(element.offset(), rotation));
            }
        }
        return positions;
    }

    /**
     * Applique une rotation horizontale (autour de l'axe Y) à un offset.
     * @param offset L'offset relatif original
     * @param rotation 0=0°, 1=90°, 2=180°, 3=270° (sens horaire vu du dessus)
     * @return L'offset après rotation
     */
    public static Vec3i rotateY(Vec3i offset, int rotation) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (rotation & 3) {
            case 0 -> offset;                      // 0°:   (x, y, z)
            case 1 -> new Vec3i(-z, y, x);         // 90°:  (-z, y, x)
            case 2 -> new Vec3i(-x, y, -z);        // 180°: (-x, y, -z)
            case 3 -> new Vec3i(z, y, -x);         // 270°: (z, y, -x)
            default -> offset;
        };
    }

    /**
     * Un élément du pattern: offset relatif + matcher.
     */
    public record PatternElement(Vec3i offset, BlockMatcher.Matcher matcher) {}

    // ==================== Builder ====================

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private final String id;
        private final List<PatternElement> elements = new ArrayList<>();
        private int minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;

        private Builder(String id) {
            this.id = id;
        }

        /**
         * Ajoute un élément au pattern.
         * @param x Offset X relatif au contrôleur
         * @param y Offset Y relatif au contrôleur
         * @param z Offset Z relatif au contrôleur
         * @param matcher Le matcher pour cette position
         */
        public Builder add(int x, int y, int z, BlockMatcher.Matcher matcher) {
            elements.add(new PatternElement(new Vec3i(x, y, z), matcher));
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
            return this;
        }

        /**
         * Ajoute une couche horizontale complète (3x3 par défaut).
         */
        public Builder layer(int y, BlockMatcher.Matcher[][] layer) {
            int halfZ = layer.length / 2;
            for (int z = 0; z < layer.length; z++) {
                int halfX = layer[z].length / 2;
                for (int x = 0; x < layer[z].length; x++) {
                    if (layer[z][x] != null) {
                        add(x - halfX, y, z - halfZ, layer[z][x]);
                    }
                }
            }
            return this;
        }

        public MultiblockPattern build() {
            Vec3i size = new Vec3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
            MultiblockPattern pattern = new MultiblockPattern(id, size);
            pattern.elements.addAll(elements);
            return pattern;
        }
    }
}
