/**
 * ============================================================
 * [HoneyFluid.java]
 * Description: Fluides Beemancer utilisant BaseFlowingFluid (NeoForge 1.21.1)
 * ============================================================
 */
package com.chapeau.beemancer.common.fluid;

import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public class HoneyFluid {
    
    // ==================== HONEY ====================
    private static BaseFlowingFluid.Properties honeyProperties() {
        return new BaseFlowingFluid.Properties(
            BeemancerFluids.HONEY_FLUID_TYPE,
            BeemancerFluids.HONEY_SOURCE,
            BeemancerFluids.HONEY_FLOWING
        ).slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(30)
         .block(BeemancerBlocks.HONEY_FLUID_BLOCK).bucket(BeemancerItems.HONEY_BUCKET);
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
            BeemancerFluids.ROYAL_JELLY_FLUID_TYPE,
            BeemancerFluids.ROYAL_JELLY_SOURCE,
            BeemancerFluids.ROYAL_JELLY_FLOWING
        ).slopeFindDistance(1).levelDecreasePerBlock(3).tickRate(40)
         .block(BeemancerBlocks.ROYAL_JELLY_FLUID_BLOCK).bucket(BeemancerItems.ROYAL_JELLY_BUCKET);
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
            BeemancerFluids.NECTAR_FLUID_TYPE,
            BeemancerFluids.NECTAR_SOURCE,
            BeemancerFluids.NECTAR_FLOWING
        ).slopeFindDistance(3).levelDecreasePerBlock(1).tickRate(15)
         .block(BeemancerBlocks.NECTAR_FLUID_BLOCK).bucket(BeemancerItems.NECTAR_BUCKET);
    }

    public static class NectarSource extends BaseFlowingFluid.Source {
        public NectarSource() { super(nectarProperties()); }
    }
    public static class NectarFlowing extends BaseFlowingFluid.Flowing {
        public NectarFlowing() { super(nectarProperties()); }
    }

    // ==================== FIRE NECTAR ====================
    private static BaseFlowingFluid.Properties fireNectarProperties() {
        return new BaseFlowingFluid.Properties(
            BeemancerFluids.FIRE_NECTAR_FLUID_TYPE,
            BeemancerFluids.FIRE_NECTAR_SOURCE,
            BeemancerFluids.FIRE_NECTAR_FLOWING
        ).slopeFindDistance(3).levelDecreasePerBlock(1).tickRate(10)
         .block(BeemancerBlocks.FIRE_NECTAR_FLUID_BLOCK).bucket(BeemancerItems.FIRE_NECTAR_BUCKET);
    }

    public static class FireNectarSource extends BaseFlowingFluid.Source {
        public FireNectarSource() { super(fireNectarProperties()); }
    }
    public static class FireNectarFlowing extends BaseFlowingFluid.Flowing {
        public FireNectarFlowing() { super(fireNectarProperties()); }
    }

    // ==================== FROST NECTAR ====================
    private static BaseFlowingFluid.Properties frostNectarProperties() {
        return new BaseFlowingFluid.Properties(
            BeemancerFluids.FROST_NECTAR_FLUID_TYPE,
            BeemancerFluids.FROST_NECTAR_SOURCE,
            BeemancerFluids.FROST_NECTAR_FLOWING
        ).slopeFindDistance(3).levelDecreasePerBlock(1).tickRate(10)
         .block(BeemancerBlocks.FROST_NECTAR_FLUID_BLOCK).bucket(BeemancerItems.FROST_NECTAR_BUCKET);
    }

    public static class FrostNectarSource extends BaseFlowingFluid.Source {
        public FrostNectarSource() { super(frostNectarProperties()); }
    }
    public static class FrostNectarFlowing extends BaseFlowingFluid.Flowing {
        public FrostNectarFlowing() { super(frostNectarProperties()); }
    }

    // ==================== STORM NECTAR ====================
    private static BaseFlowingFluid.Properties stormNectarProperties() {
        return new BaseFlowingFluid.Properties(
            BeemancerFluids.STORM_NECTAR_FLUID_TYPE,
            BeemancerFluids.STORM_NECTAR_SOURCE,
            BeemancerFluids.STORM_NECTAR_FLOWING
        ).slopeFindDistance(3).levelDecreasePerBlock(1).tickRate(10)
         .block(BeemancerBlocks.STORM_NECTAR_FLUID_BLOCK).bucket(BeemancerItems.STORM_NECTAR_BUCKET);
    }

    public static class StormNectarSource extends BaseFlowingFluid.Source {
        public StormNectarSource() { super(stormNectarProperties()); }
    }
    public static class StormNectarFlowing extends BaseFlowingFluid.Flowing {
        public StormNectarFlowing() { super(stormNectarProperties()); }
    }
}
