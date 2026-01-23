/**
 * ============================================================
 * [HoneyTankBlockEntity.java]
 * Description: BlockEntity pour le tank de stockage de fluides
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.menu.alchemy.HoneyTankMenu;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

public class HoneyTankBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = 16000;

    private final FluidTank fluidTank = new FluidTank(CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == BeemancerFluids.HONEY_SOURCE.get()
                || stack.getFluid() == BeemancerFluids.ROYAL_JELLY_SOURCE.get()
                || stack.getFluid() == BeemancerFluids.NECTAR_SOURCE.get();
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? fluidTank.getFluidAmount() : 0;
        }
        @Override
        public void set(int index, int value) {}
        @Override
        public int getCount() { return 1; }
    };

    public HoneyTankBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.HONEY_TANK.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HoneyTankBlockEntity be) {
        if (be.fluidTank.getFluidAmount() > 0) {
            BlockEntity below = level.getBlockEntity(pos.below());
            if (below != null) {
                var cap = level.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                    pos.below(), Direction.UP);
                if (cap != null) {
                    FluidStack toTransfer = be.fluidTank.drain(100, IFluidHandler.FluidAction.SIMULATE);
                    if (!toTransfer.isEmpty()) {
                        int filled = cap.fill(toTransfer, IFluidHandler.FluidAction.EXECUTE);
                        if (filled > 0) {
                            be.fluidTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                        }
                    }
                }
            }
        }
    }

    public FluidTank getFluidTank() { return fluidTank; }
    public int getFluidAmount() { return fluidTank.getFluidAmount(); }
    public FluidStack getFluid() { return fluidTank.getFluid(); }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.honey_tank");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new HoneyTankMenu(containerId, playerInv, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Fluid")) {
            fluidTank.readFromNBT(registries, tag.getCompound("Fluid"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
        return tag;
    }
}
