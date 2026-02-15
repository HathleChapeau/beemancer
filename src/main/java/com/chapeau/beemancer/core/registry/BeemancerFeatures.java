/**
 * ============================================================
 * [BeemancerFeatures.java]
 * Description: Registre des Features custom de worldgen
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                  | Utilisation                    |
 * |--------------------------|------------------------|--------------------------------|
 * | BeeNestFeature           | Feature placement      | Nids d'abeilles dans le monde  |
 * | BeeNestFeatureConfig     | Config avec Codec      | Serialisation JSON             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Beemancer.java (registration sur le mod event bus)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.content.worldgen.BeeNestFeature;
import com.chapeau.beemancer.content.worldgen.BeeNestFeatureConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BeemancerFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Beemancer.MOD_ID);

    public static final Supplier<BeeNestFeature> BEE_NEST = FEATURES.register("bee_nest",
            () -> new BeeNestFeature(BeeNestFeatureConfig.CODEC));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
