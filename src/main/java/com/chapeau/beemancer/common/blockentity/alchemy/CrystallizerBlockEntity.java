/**
 * ============================================================
 * [CrystallizerBlockEntity.java]
 * Description: Transforme Royal Jelly en Nectar avec des cristaux
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.CrystallizerBlock;
import com.chapeau.beemancer.common.menu.alchemy.CrystallizerMenu;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

public class CrystallizerBlockEntity extends BlockEntity implements MenuProvider {
    private static final int MAX_CRYSTALS = 4;
    private static final int BASE_PROCESS_RATE = 1;
    private static final int HONEY_CONSUMPTION = 5;

    private final NonNullList<ItemStack> crystals = NonNullList.withSize(MAX_CRYSTALS, ItemStack.EMPTY);

    private final FluidTank royalJellyTank = new FluidTank(8000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == BeemancerFluids.ROYAL_JELLY_SOURCE.get();
        }
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private final FluidTank honeyTank = new FluidTank(4000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == BeemancerFluids.HONEY_SOURCE.get();
        }
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private final FluidTank nectarTank = new FluidTank(8000) {
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> royalJellyTank.getFluidAmount();
                case 1 -> honeyTank.getFluidAmount();
                case 2 -> nectarTank.getFluidAmount();
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) {}
        @Override
        public int getCount() { return 3; }
    };

    public CrystallizerBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.CRYSTALLIZER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrystallizerBlockEntity be) {
        int speedMultiplier = be.calculateSpeedMultiplier();
        boolean wasActive = state.getValue(CrystallizerBlock.ACTIVE);
        boolean isActive = false;

        if (speedMultiplier > 0 && be.canProcess()) {
            int processRate = BASE_PROCESS_RATE * speedMultiplier;
            int honeyNeeded = HONEY_CONSUMPTION;

            if (be.honeyTank.getFluidAmount() >= honeyNeeded && be.royalJellyTank.getFluidAmount() >= processRate) {
                be.honeyTank.drain(honeyNeeded, IFluidHandler.FluidAction.EXECUTE);
                FluidStack drained = be.royalJellyTank.drain(processRate, IFluidHandler.FluidAction.EXECUTE);
                be.nectarTank.fill(new FluidStack(BeemancerFluids.NECTAR_SOURCE.get(), drained.getAmount()),
                    IFluidHandler.FluidAction.EXECUTE);
                isActive = true;
            }
        }

        if (be.nectarTank.getFluidAmount() > 0) {
            var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.below(), Direction.UP);
            if (cap != null) {
                FluidStack toTransfer = be.nectarTank.drain(100, IFluidHandler.FluidAction.SIMULATE);
                int filled = cap.fill(toTransfer, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) be.nectarTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            }
        }

        if (wasActive != isActive) {
            level.setBlock(pos, state.setValue(CrystallizerBlock.ACTIVE, isActive), 3);
        }
    }

    private boolean canProcess() {
        return royalJellyTank.getFluidAmount() > 0 && nectarTank.getFluidAmount() < nectarTank.getCapacity();
    }

    private int calculateSpeedMultiplier() {
        int multiplier = 0;
        for (ItemStack crystal : crystals) {
            if (crystal.is(BeemancerItems.HONEY_CRYSTAL.get())) multiplier += 1;
            else if (crystal.is(BeemancerItems.ENRICHED_HONEY_CRYSTAL.get())) multiplier += 2;
            else if (crystal.is(BeemancerItems.RADIANT_HONEY_CRYSTAL.get())) multiplier += 4;
        }
        return multiplier;
    }

    public boolean insertCrystal(ItemStack crystal) {
        for (int i = 0; i < crystals.size(); i++) {
            if (crystals.get(i).isEmpty()) {
                crystals.set(i, crystal);
                setChanged();
                return true;
            }
        }
        return false;
    }

    public FluidTank getRoyalJellyTank() { return royalJellyTank; }
    public FluidTank getHoneyTank() { return honeyTank; }
    public FluidTank getNectarTank() { return nectarTank; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.crystallizer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new CrystallizerMenu(containerId, playerInv, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag crystalsTag = new CompoundTag();
        ContainerHelper.saveAllItems(crystalsTag, crystals, registries);
        tag.put("Crystals", crystalsTag);
        tag.put("RoyalJelly", royalJellyTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Honey", honeyTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Nectar", nectarTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag.getCompound("Crystals"), crystals, registries);
        royalJellyTank.readFromNBT(registries, tag.getCompound("RoyalJelly"));
        honeyTank.readFromNBT(registries, tag.getCompound("Honey"));
        nectarTank.readFromNBT(registries, tag.getCompound("Nectar"));
    }
}
