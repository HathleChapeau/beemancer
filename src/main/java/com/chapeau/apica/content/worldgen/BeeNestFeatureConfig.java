/**
 * ============================================================
 * [BeeNestFeatureConfig.java]
 * Description: Configuration pour la Feature de placement de nids
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                  | Utilisation                    |
 * |--------------------------|------------------------|--------------------------------|
 * | FeatureConfiguration     | Interface MC           | Serialisation worldgen         |
 * | Codec                    | Mojang serialisation   | JSON encode/decode             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeeNestFeature.java (lecture config dans place())
 * - Configured Feature JSON (serialisation)
 *
 * ============================================================
 */
package com.chapeau.apica.content.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Config: species_id (String) + placement_type (enum).
 * Determine quelle espece et quel algorithme de placement utiliser.
 */
public record BeeNestFeatureConfig(String speciesId, PlacementType placementType) implements FeatureConfiguration {

    public static final Codec<BeeNestFeatureConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("species_id").forGetter(BeeNestFeatureConfig::speciesId),
                    PlacementType.CODEC.fieldOf("placement_type").forGetter(BeeNestFeatureConfig::placementType)
            ).apply(instance, BeeNestFeatureConfig::new)
    );

    public enum PlacementType implements StringRepresentable {
        SURFACE("surface"),
        TREE("tree"),
        UNDERGROUND("underground"),
        NETHER_SURFACE("nether_surface"),
        END_SURFACE("end_surface"),
        WATER_SURFACE("water_surface");

        public static final Codec<PlacementType> CODEC = StringRepresentable.fromEnum(PlacementType::values);

        private final String name;

        PlacementType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
