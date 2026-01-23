/**
 * ============================================================
 * [HoneyFluid.java]
 * Description: Classes de fluides pour Honey, Royal Jelly, Nectar et Nectars élémentaires
 * ============================================================
 */
package com.chapeau.beemancer.common.fluid;

import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;

public abstract class HoneyFluid extends FlowingFluid {

    @Override
    public Fluid getFlowing() { return BeemancerFluids.HONEY_FLOWING.get(); }
    @Override
    public Fluid getSource() { return BeemancerFluids.HONEY_SOURCE.get(); }
    @Override
    public Item getBucket() { return BeemancerItems.HONEY_BUCKET.get(); }
    @Override
    protected LiquidBlock getBlock() { return BeemancerBlocks.HONEY_FLUID_BLOCK.get(); }
    @Override
    public FluidType getFluidType() { return BeemancerFluids.HONEY_FLUID_TYPE.get(); }
    @Override
    protected boolean canConvertToSource(Level level) { return false; }
    @Override
    protected int getSlopeFindDistance(Level level) { return 2; }
    @Override
    protected int getDropOff(Level level) { return 2; }
    @Override
    public int getTickDelay(Level level) { return 30; }
    @Override
    protected float getExplosionResistance() { return 100.0F; }

    public static class Source extends HoneyFluid {
        @Override
        public boolean isSource(FluidState state) { return true; }
        @Override
        public int getAmount(FluidState state) { return 8; }
    }

    public static class Flowing extends HoneyFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        @Override
        public boolean isSource(FluidState state) { return false; }
        @Override
        public int getAmount(FluidState state) { return state.getValue(LEVEL); }
    }

    // ==================== ROYAL JELLY ====================
    public static abstract class RoyalJellyFluid extends FlowingFluid {
        @Override
        public Fluid getFlowing() { return BeemancerFluids.ROYAL_JELLY_FLOWING.get(); }
        @Override
        public Fluid getSource() { return BeemancerFluids.ROYAL_JELLY_SOURCE.get(); }
        @Override
        public Item getBucket() { return BeemancerItems.ROYAL_JELLY_BUCKET.get(); }
        @Override
        protected LiquidBlock getBlock() { return BeemancerBlocks.ROYAL_JELLY_FLUID_BLOCK.get(); }
        @Override
        public FluidType getFluidType() { return BeemancerFluids.ROYAL_JELLY_FLUID_TYPE.get(); }
        @Override
        protected boolean canConvertToSource(Level level) { return false; }
        @Override
        protected int getSlopeFindDistance(Level level) { return 1; }
        @Override
        protected int getDropOff(Level level) { return 3; }
        @Override
        public int getTickDelay(Level level) { return 40; }
        @Override
        protected float getExplosionResistance() { return 100.0F; }
    }

    public static class RoyalJellySource extends RoyalJellyFluid {
        @Override
        public boolean isSource(FluidState state) { return true; }
        @Override
        public int getAmount(FluidState state) { return 8; }
    }

    public static class RoyalJellyFlowing extends RoyalJellyFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        @Override
        public boolean isSource(FluidState state) { return false; }
        @Override
        public int getAmount(FluidState state) { return state.getValue(LEVEL); }
    }

    // ==================== NECTAR ====================
    public static abstract class NectarFluid extends FlowingFluid {
        @Override
        public Fluid getFlowing() { return BeemancerFluids.NECTAR_FLOWING.get(); }
        @Override
        public Fluid getSource() { return BeemancerFluids.NECTAR_SOURCE.get(); }
        @Override
        public Item getBucket() { return BeemancerItems.NECTAR_BUCKET.get(); }
        @Override
        protected LiquidBlock getBlock() { return BeemancerBlocks.NECTAR_FLUID_BLOCK.get(); }
        @Override
        public FluidType getFluidType() { return BeemancerFluids.NECTAR_FLUID_TYPE.get(); }
        @Override
        protected boolean canConvertToSource(Level level) { return false; }
        @Override
        protected int getSlopeFindDistance(Level level) { return 3; }
        @Override
        protected int getDropOff(Level level) { return 1; }
        @Override
        public int getTickDelay(Level level) { return 15; }
        @Override
        protected float getExplosionResistance() { return 100.0F; }
    }

    public static class NectarSource extends NectarFluid {
        @Override
        public boolean isSource(FluidState state) { return true; }
        @Override
        public int getAmount(FluidState state) { return 8; }
    }

    public static class NectarFlowing extends NectarFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        @Override
        public boolean isSource(FluidState state) { return false; }
        @Override
        public int getAmount(FluidState state) { return state.getValue(LEVEL); }
    }

    // ==================== FIRE NECTAR ====================
    public static abstract class FireNectarFluid extends FlowingFluid {
        @Override
        public Fluid getFlowing() { return BeemancerFluids.FIRE_NECTAR_FLOWING.get(); }
        @Override
        public Fluid getSource() { return BeemancerFluids.FIRE_NECTAR_SOURCE.get(); }
        @Override
        public Item getBucket() { return BeemancerItems.FIRE_NECTAR_BUCKET.get(); }
        @Override
        protected LiquidBlock getBlock() { return BeemancerBlocks.FIRE_NECTAR_FLUID_BLOCK.get(); }
        @Override
        public FluidType getFluidType() { return BeemancerFluids.FIRE_NECTAR_FLUID_TYPE.get(); }
        @Override
        protected boolean canConvertToSource(Level level) { return false; }
        @Override
        protected int getSlopeFindDistance(Level level) { return 3; }
        @Override
        protected int getDropOff(Level level) { return 1; }
        @Override
        public int getTickDelay(Level level) { return 10; }
        @Override
        protected float getExplosionResistance() { return 100.0F; }
    }

    public static class FireNectarSource extends FireNectarFluid {
        @Override
        public boolean isSource(FluidState state) { return true; }
        @Override
        public int getAmount(FluidState state) { return 8; }
    }

    public static class FireNectarFlowing extends FireNectarFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        @Override
        public boolean isSource(FluidState state) { return false; }
        @Override
        public int getAmount(FluidState state) { return state.getValue(LEVEL); }
    }

    // ==================== FROST NECTAR ====================
    public static abstract class FrostNectarFluid extends FlowingFluid {
        @Override
        public Fluid getFlowing() { return BeemancerFluids.FROST_NECTAR_FLOWING.get(); }
        @Override
        public Fluid getSource() { return BeemancerFluids.FROST_NECTAR_SOURCE.get(); }
        @Override
        public Item getBucket() { return BeemancerItems.FROST_NECTAR_BUCKET.get(); }
        @Override
        protected LiquidBlock getBlock() { return BeemancerBlocks.FROST_NECTAR_FLUID_BLOCK.get(); }
        @Override
        public FluidType getFluidType() { return BeemancerFluids.FROST_NECTAR_FLUID_TYPE.get(); }
        @Override
        protected boolean canConvertToSource(Level level) { return false; }
        @Override
        protected int getSlopeFindDistance(Level level) { return 3; }
        @Override
        protected int getDropOff(Level level) { return 1; }
        @Override
        public int getTickDelay(Level level) { return 10; }
        @Override
        protected float getExplosionResistance() { return 100.0F; }
    }

    public static class FrostNectarSource extends FrostNectarFluid {
        @Override
        public boolean isSource(FluidState state) { return true; }
        @Override
        public int getAmount(FluidState state) { return 8; }
    }

    public static class FrostNectarFlowing extends FrostNectarFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        @Override
        public boolean isSource(FluidState state) { return false; }
        @Override
        public int getAmount(FluidState state) { return state.getValue(LEVEL); }
    }

    // ==================== STORM NECTAR ====================
    public static abstract class StormNectarFluid extends FlowingFluid {
        @Override
        public Fluid getFlowing() { return BeemancerFluids.STORM_NECTAR_FLOWING.get(); }
        @Override
        public Fluid getSource() { return BeemancerFluids.STORM_NECTAR_SOURCE.get(); }
        @Override
        public Item getBucket() { return BeemancerItems.STORM_NECTAR_BUCKET.get(); }
        @Override
        protected LiquidBlock getBlock() { return BeemancerBlocks.STORM_NECTAR_FLUID_BLOCK.get(); }
        @Override
        public FluidType getFluidType() { return BeemancerFluids.STORM_NECTAR_FLUID_TYPE.get(); }
        @Override
        protected boolean canConvertToSource(Level level) { return false; }
        @Override
        protected int getSlopeFindDistance(Level level) { return 3; }
        @Override
        protected int getDropOff(Level level) { return 1; }
        @Override
        public int getTickDelay(Level level) { return 10; }
        @Override
        protected float getExplosionResistance() { return 100.0F; }
    }

    public static class StormNectarSource extends StormNectarFluid {
        @Override
        public boolean isSource(FluidState state) { return true; }
        @Override
        public int getAmount(FluidState state) { return 8; }
    }

    public static class StormNectarFlowing extends StormNectarFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        @Override
        public boolean isSource(FluidState state) { return false; }
        @Override
        public int getAmount(FluidState state) { return state.getValue(LEVEL); }
    }
}
