/**
 * ============================================================
 * [AlembicBlockEntity.java]
 * Description: Mélange Honey et Royal Jelly pour produire du Nectar
 * ============================================================
 * 
 * FONCTIONNEMENT:
 * - Input 1: Honey (500mB)
 * - Input 2: Royal Jelly (250mB)
 * - Output: Nectar (500mB)
 * - Process time: 80 ticks
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.AlembicBlock;
import com.chapeau.beemancer.common.menu.alchemy.AlembicMenu;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

public class AlembicBlockEntity extends BlockEntity implements MenuProvider {
    private static final int HONEY_CONSUMPTION = 500;
    private static final int ROYAL_JELLY_CONSUMPTION = 250;
    private static final int NECTAR_PRODUCTION = 500;
    private static final int PROCESS_TIME = 80;

    private final FluidTank honeyTank = new FluidTank(4000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == BeemancerFluids.HONEY_SOURCE.get();
        }
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private final FluidTank royalJellyTank = new FluidTank(4000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == BeemancerFluids.ROYAL_JELLY_SOURCE.get();
        }
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private final FluidTank nectarTank = new FluidTank(4000) {
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private int progress = 0;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> PROCESS_TIME;
                case 2 -> honeyTank.getFluidAmount();
                case 3 -> royalJellyTank.getFluidAmount();
                case 4 -> nectarTank.getFluidAmount();
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
        }
        @Override
        public int getCount() { return 5; }
    };

    public AlembicBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.ALEMBIC.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlembicBlockEntity be) {
        boolean wasDistilling = state.getValue(AlembicBlock.DISTILLING);
        boolean isDistilling = false;

        if (be.canProcess()) {
            be.progress++;
            isDistilling = true;

            if (be.progress >= PROCESS_TIME) {
                be.processFluids();
                be.progress = 0;
            }
        } else {
            be.progress = 0;
        }

        // Auto-output nectar to block below
        if (be.nectarTank.getFluidAmount() > 0) {
            var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.below(), Direction.UP);
            if (cap != null) {
                FluidStack toTransfer = be.nectarTank.drain(100, IFluidHandler.FluidAction.SIMULATE);
                int filled = cap.fill(toTransfer, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) be.nectarTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            }
        }

        if (wasDistilling != isDistilling) {
            level.setBlock(pos, state.setValue(AlembicBlock.DISTILLING, isDistilling), 3);
        }

        be.setChanged();
    }

    private boolean canProcess() {
        // Check if we have enough inputs
        if (honeyTank.getFluidAmount() < HONEY_CONSUMPTION) return false;
        if (royalJellyTank.getFluidAmount() < ROYAL_JELLY_CONSUMPTION) return false;
        
        // Check if we have space for output
        if (nectarTank.getFluidAmount() + NECTAR_PRODUCTION > nectarTank.getCapacity()) return false;
        
        return true;
    }

    private void processFluids() {
        // Consume inputs
        honeyTank.drain(HONEY_CONSUMPTION, IFluidHandler.FluidAction.EXECUTE);
        royalJellyTank.drain(ROYAL_JELLY_CONSUMPTION, IFluidHandler.FluidAction.EXECUTE);
        
        // Produce nectar
        nectarTank.fill(new FluidStack(BeemancerFluids.NECTAR_SOURCE.get(), NECTAR_PRODUCTION),
            IFluidHandler.FluidAction.EXECUTE);
    }

    public FluidTank getHoneyTank() { return honeyTank; }
    public FluidTank getRoyalJellyTank() { return royalJellyTank; }
    public FluidTank getNectarTank() { return nectarTank; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.alembic");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new AlembicMenu(containerId, playerInv, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("HoneyTank", honeyTank.writeToNBT(registries, new CompoundTag()));
        tag.put("RoyalJellyTank", royalJellyTank.writeToNBT(registries, new CompoundTag()));
        tag.put("NectarTank", nectarTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        honeyTank.readFromNBT(registries, tag.getCompound("HoneyTank"));
        royalJellyTank.readFromNBT(registries, tag.getCompound("RoyalJellyTank"));
        nectarTank.readFromNBT(registries, tag.getCompound("NectarTank"));
        progress = tag.getInt("Progress");
    }
}
