/**
 * ============================================================
 * [HoneyFluid.java]
 * Description: Fluides Apica utilisant BaseFlowingFluid (NeoForge 1.21.1)
 * ============================================================
 */
package com.chapeau.apica.common.fluid;

import com.chapeau.apica.core.registry.ApicaBlocks;
import com.chapeau.apica.core.registry.ApicaFluids;
import com.chapeau.apica.core.registry.ApicaItems;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public class HoneyFluid {
    
    // ==================== HONEY ====================
    private static BaseFlowingFluid.Properties honeyProperties() {
        return new BaseFlowingFluid.Properties(
            ApicaFluids.HONEY_FLUID_TYPE,
            ApicaFluids.HONEY_SOURCE,
            ApicaFluids.HONEY_FLOWING
        ).slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(30)
         .block(ApicaBlocks.HONEY_FLUID_BLOCK).bucket(ApicaItems.HONEY_BUCKET);
    }

    public static class HoneySource extends BaseFlowingFluid.Source {
        public HoneySource() { super(honeyProperties()); }
    }
    public static class HoneyFlowing extends BaseFlowingFluid.Flowing {
        public HoneyFlowing() { super(honeyProperties()); }
    }

    // ==================== ROYAL JELLY ====================
    private static BaseFlowingFluid.Properties royalJellyProperties() {
        return new BaseFlowingFluid.Properties(
            ApicaFluids.ROYAL_JELLY_FLUID_TYPE,
            ApicaFluids.ROYAL_JELLY_SOURCE,
            ApicaFluids.ROYAL_JELLY_FLOWING
        ).slopeFindDistance(1).levelDecreasePerBlock(3).tickRate(40)
         .block(ApicaBlocks.ROYAL_JELLY_FLUID_BLOCK).bucket(ApicaItems.ROYAL_JELLY_BUCKET);
    }

    public static class RoyalJellySource extends BaseFlowingFluid.Source {
        public RoyalJellySource() { super(royalJellyProperties()); }
    }
    public static class RoyalJellyFlowing extends BaseFlowingFluid.Flowing {
        public RoyalJellyFlowing() { super(royalJellyProperties()); }
    }

    // ==================== NECTAR ====================
    private static BaseFlowingFluid.Properties nectarProperties() {
        return new BaseFlowingFluid.Properties(
            ApicaFluids.NECTAR_FLUID_TYPE,
            ApicaFluids.NECTAR_SOURCE,
            ApicaFluids.NECTAR_FLOWING
        ).slopeFindDistance(3).levelDecreasePerBlock(1).tickRate(15)
         .block(ApicaBlocks.NECTAR_FLUID_BLOCK).bucket(ApicaItems.NECTAR_BUCKET);
    }

    public static class NectarSource extends BaseFlowingFluid.Source {
        public NectarSource() { super(nectarProperties()); }
    }
    public static class NectarFlowing extends BaseFlowingFluid.Flowing {
        public NectarFlowing() { super(nectarProperties()); }
    }
}
