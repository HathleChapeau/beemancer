/**
 * ============================================================
 * [MultiblockProperty.java]
 * Description: Enum et factory pour la propriete blockstate des multiblocs
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | EnumProperty        | Type de propriete    | Etat du multibloc              |
 * | StringRepresentable | Serialisation        | Noms dans blockstate JSON      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Tous les blocs participant a un multibloc
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.multiblock;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Enum representant les differents types de multiblocs.
 * Utilise comme valeur de la propriete blockstate "multiblock".
 * Valeur NONE = bloc non forme. Autres valeurs = nom du multibloc auquel le bloc appartient.
 */
public enum MultiblockProperty implements StringRepresentable {
    NONE("none"),
    ALTAR("altar"),
    EXTRACTOR("extractor"),
    STORAGE("storage"),
    HIVE("hive"),
    ALEMBIC("alembic"),
    ALEMBIC_0("alembic_0"),
    ALEMBIC_1("alembic_1"),
    INFUSER("infuser"),
    CENTRIFUGE("centrifuge");

    private final String name;

    MultiblockProperty(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /**
     * Cree une EnumProperty "multiblock" avec NONE + les types de multiblocs fournis.
     * @param types types de multiblocs auxquels ce bloc peut participer
     */
    public static EnumProperty<MultiblockProperty> create(MultiblockProperty... types) {
        Set<MultiblockProperty> values = new LinkedHashSet<>();
        values.add(NONE);
        values.addAll(Arrays.asList(types));
        return EnumProperty.create("multiblock", MultiblockProperty.class, values);
    }

    /**
     * Cree une EnumProperty "multiblock" a partir de noms de multiblocs (pour compatibilite).
     * @param multiblockNames noms des multiblocs auxquels ce bloc peut participer
     */
    public static EnumProperty<MultiblockProperty> create(String... multiblockNames) {
        Set<MultiblockProperty> values = new LinkedHashSet<>();
        values.add(NONE);
        for (String name : multiblockNames) {
            values.add(fromName(name));
        }
        return EnumProperty.create("multiblock", MultiblockProperty.class, values);
    }

    /**
     * Trouve le MultiblockProperty correspondant au nom donne.
     */
    public static MultiblockProperty fromName(String name) {
        for (MultiblockProperty prop : values()) {
            if (prop.name.equals(name)) {
                return prop;
            }
        }
        throw new IllegalArgumentException("Unknown multiblock type: " + name);
    }
}
