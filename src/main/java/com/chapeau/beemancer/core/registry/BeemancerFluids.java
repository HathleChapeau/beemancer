/**
 * ============================================================
 * [BeemancerFluids.java]
 * Description: Registre des fluides Beemancer (NeoForge 1.21.1)
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.fluid.HoneyFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class BeemancerFluids {
    public static final DeferredRegister<Fluid> FLUIDS =
        DeferredRegister.create(Registries.FLUID, Beemancer.MOD_ID);
    
    public static final DeferredRegister<FluidType> FLUID_TYPES =
        DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Beemancer.MOD_ID);

    // ==================== HONEY ====================
    public static final Supplier<FluidType> HONEY_FLUID_TYPE = FLUID_TYPES.register("honey",
        () -> new FluidType(FluidType.Properties.create()
            .density(1500).viscosity(6000).temperature(300)
            .descriptionId("fluid.beemancer.honey")));

    public static final Supplier<FlowingFluid> HONEY_SOURCE = FLUIDS.register("honey",
        HoneyFluid.HoneySource::new);
    public static final Supplier<FlowingFluid> HONEY_FLOWING = FLUIDS.register("honey_flowing",
        HoneyFluid.HoneyFlowing::new);

    // ==================== ROYAL JELLY ====================
    public static final Supplier<FluidType> ROYAL_JELLY_FLUID_TYPE = FLUID_TYPES.register("royal_jelly",
        () -> new FluidType(FluidType.Properties.create()
            .density(1800).viscosity(8000).temperature(300)
            .descriptionId("fluid.beemancer.royal_jelly")));

    public static final Supplier<FlowingFluid> ROYAL_JELLY_SOURCE = FLUIDS.register("royal_jelly",
        HoneyFluid.RoyalJellySource::new);
    public static final Supplier<FlowingFluid> ROYAL_JELLY_FLOWING = FLUIDS.register("royal_jelly_flowing",
        HoneyFluid.RoyalJellyFlowing::new);

    // ==================== NECTAR ====================
    public static final Supplier<FluidType> NECTAR_FLUID_TYPE = FLUID_TYPES.register("nectar",
        () -> new FluidType(FluidType.Properties.create()
            .density(1200).viscosity(3000).temperature(300).lightLevel(8)
            .descriptionId("fluid.beemancer.nectar")));

    public static final Supplier<FlowingFluid> NECTAR_SOURCE = FLUIDS.register("nectar",
        HoneyFluid.NectarSource::new);
    public static final Supplier<FlowingFluid> NECTAR_FLOWING = FLUIDS.register("nectar_flowing",
        HoneyFluid.NectarFlowing::new);

    // ==================== FIRE NECTAR ====================
    public static final Supplier<FluidType> FIRE_NECTAR_FLUID_TYPE = FLUID_TYPES.register("fire_nectar",
        () -> new FluidType(FluidType.Properties.create()
            .density(1100).viscosity(2000).temperature(500).lightLevel(12)
            .descriptionId("fluid.beemancer.fire_nectar")));

    public static final Supplier<FlowingFluid> FIRE_NECTAR_SOURCE = FLUIDS.register("fire_nectar",
        HoneyFluid.FireNectarSource::new);
    public static final Supplier<FlowingFluid> FIRE_NECTAR_FLOWING = FLUIDS.register("fire_nectar_flowing",
        HoneyFluid.FireNectarFlowing::new);

    // ==================== FROST NECTAR ====================
    public static final Supplier<FluidType> FROST_NECTAR_FLUID_TYPE = FLUID_TYPES.register("frost_nectar",
        () -> new FluidType(FluidType.Properties.create()
            .density(1300).viscosity(4000).temperature(200).lightLevel(6)
            .descriptionId("fluid.beemancer.frost_nectar")));

    public static final Supplier<FlowingFluid> FROST_NECTAR_SOURCE = FLUIDS.register("frost_nectar",
        HoneyFluid.FrostNectarSource::new);
    public static final Supplier<FlowingFluid> FROST_NECTAR_FLOWING = FLUIDS.register("frost_nectar_flowing",
        HoneyFluid.FrostNectarFlowing::new);

    // ==================== STORM NECTAR ====================
    public static final Supplier<FluidType> STORM_NECTAR_FLUID_TYPE = FLUID_TYPES.register("storm_nectar",
        () -> new FluidType(FluidType.Properties.create()
            .density(1000).viscosity(1500).temperature(300).lightLevel(10)
            .descriptionId("fluid.beemancer.storm_nectar")));

    public static final Supplier<FlowingFluid> STORM_NECTAR_SOURCE = FLUIDS.register("storm_nectar",
        HoneyFluid.StormNectarSource::new);
    public static final Supplier<FlowingFluid> STORM_NECTAR_FLOWING = FLUIDS.register("storm_nectar_flowing",
        HoneyFluid.StormNectarFlowing::new);

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
    }
}
