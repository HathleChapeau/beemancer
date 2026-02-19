/**
 * ============================================================
 * [ApicaFurnaceBlockEntity.java]
 * Description: BlockEntity pour les fours Apica (honey, royal, nectar)
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Consomme un fluide specifique comme carburant (31 mB par item)
 * - Utilise les recettes vanilla smelting (RecipeType.SMELTING)
 * - Honey: 1 slot in/out, 100 ticks (2x)
 * - Royal: 2 slots in/out, 67 ticks (3x), parallele
 * - Nectar: 2 slots in/out, 33 ticks (6x), parallele
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ApicaFurnaceBlock       | Bloc parent          | Etat WORKING                   |
 * | ApicaFurnaceMenu        | Menu container       | createMenu                     |
 * | ApicaFluids             | Fluides du mod       | Validation du fuel             |
 * | ApicaBlockEntities      | Registre BE          | Type factory                   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaFurnaceBlock (newBlockEntity, ticker)
 * - ApicaFurnaceMenu (server constructor)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.alchemy;

import com.chapeau.apica.common.block.alchemy.ApicaFurnaceBlock;
import com.chapeau.apica.common.menu.alchemy.ApicaFurnaceMenu;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.registry.ApicaFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Optional;

public class ApicaFurnaceBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TANK_CAPACITY = 8000;
    public static final int FUEL_PER_ITEM = 31;

    private static final int HONEY_PROCESS_TIME = 100;
    private static final int ROYAL_PROCESS_TIME = 67;
    private static final int NECTAR_PROCESS_TIME = 33;

    private final int processTime;
    private final int inputSlotCount;
    private final int outputSlotCount;
    private final String translationKey;

    private final ItemStackHandler inputSlots;
    private final ItemStackHandler outputSlots;
    private final FluidTank fuelTank;

    private final int[] progress;
    private final int[] maxProgress;

    protected final ContainerData dataAccess;

    public ApicaFurnaceBlockEntity(BlockPos pos, BlockState state) {
        this(ApicaBlockEntities.HONEY_FURNACE.get(), pos, state,
            HONEY_PROCESS_TIME, 1, 1, ApicaFluids.HONEY_SOURCE.get(),
            "container.apica.honey_furnace");
    }

    public ApicaFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                    int processTime, int inputCount, int outputCount,
                                    Fluid acceptedFluid, String translationKey) {
        super(type, pos, state);
        this.processTime = processTime;
        this.inputSlotCount = inputCount;
        this.outputSlotCount = outputCount;
        this.translationKey = translationKey;

        this.inputSlots = new ItemStackHandler(inputCount) {
            @Override
            protected void onContentsChanged(int slot) { setChanged(); }
        };

        this.outputSlots = new ItemStackHandler(outputCount) {
            @Override
            protected void onContentsChanged(int slot) { setChanged(); }
        };

        this.fuelTank = new FluidTank(TANK_CAPACITY) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return stack.getFluid() == acceptedFluid;
            }
            @Override
            protected void onContentsChanged() { setChanged(); }
        };

        this.progress = new int[inputCount];
        this.maxProgress = new int[inputCount];
        for (int i = 0; i < inputCount; i++) {
            this.maxProgress[i] = processTime;
        }

        this.dataAccess = new ContainerData() {
            @Override
            public int get(int index) {
                if (index == 0) return progress[0];
                if (index == 1) return maxProgress[0];
                if (index == 2 && inputSlotCount > 1) return progress[1];
                if (index == 3 && inputSlotCount > 1) return maxProgress[1];
                if (index == 4) return fuelTank.getFluidAmount();
                if (index == 5) return fuelTank.getCapacity();
                return 0;
            }
            @Override
            public void set(int index, int value) {
                if (index == 0) progress[0] = value;
                if (index == 1) maxProgress[0] = value;
                if (index == 2 && inputSlotCount > 1) progress[1] = value;
                if (index == 3 && inputSlotCount > 1) maxProgress[1] = value;
            }
            @Override
            public int getCount() { return 6; }
        };
    }

    public static ApicaFurnaceBlockEntity createRoyal(BlockPos pos, BlockState state) {
        return new ApicaFurnaceBlockEntity(
            ApicaBlockEntities.ROYAL_FURNACE.get(), pos, state,
            ROYAL_PROCESS_TIME, 2, 2, ApicaFluids.ROYAL_JELLY_SOURCE.get(),
            "container.apica.royal_furnace");
    }

    public static ApicaFurnaceBlockEntity createNectar(BlockPos pos, BlockState state) {
        return new ApicaFurnaceBlockEntity(
            ApicaBlockEntities.NECTAR_FURNACE.get(), pos, state,
            NECTAR_PROCESS_TIME, 2, 2, ApicaFluids.NECTAR_SOURCE.get(),
            "container.apica.nectar_furnace");
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ApicaFurnaceBlockEntity be) {
        boolean wasWorking = state.getValue(ApicaFurnaceBlock.WORKING);
        boolean isWorking = false;

        for (int slot = 0; slot < be.inputSlotCount; slot++) {
            if (be.processSlot(level, slot)) {
                isWorking = true;
            }
        }

        if (wasWorking != isWorking) {
            level.setBlock(pos, state.setValue(ApicaFurnaceBlock.WORKING, isWorking), 3);
        }

        be.setChanged();
    }

    private boolean processSlot(Level level, int slot) {
        ItemStack input = inputSlots.getStackInSlot(slot);
        if (input.isEmpty()) {
            progress[slot] = 0;
            return false;
        }

        Optional<RecipeHolder<SmeltingRecipe>> recipeOpt = level.getRecipeManager().getRecipeFor(
            RecipeType.SMELTING,
            new SingleRecipeInput(input),
            level
        );

        if (recipeOpt.isEmpty()) {
            progress[slot] = 0;
            return false;
        }

        ItemStack result = recipeOpt.get().value().getResultItem(level.registryAccess());
        if (!canInsertOutput(slot, result)) {
            return false;
        }

        if (progress[slot] == 0) {
            if (fuelTank.getFluidAmount() < FUEL_PER_ITEM) {
                return false;
            }
            fuelTank.drain(FUEL_PER_ITEM, IFluidHandler.FluidAction.EXECUTE);
            maxProgress[slot] = processTime;
        }

        progress[slot]++;

        if (progress[slot] >= maxProgress[slot]) {
            inputSlots.extractItem(slot, 1, false);
            insertOutput(slot, result.copy());
            progress[slot] = 0;
        }

        return true;
    }

    private boolean canInsertOutput(int slot, ItemStack result) {
        ItemStack existing = outputSlots.getStackInSlot(slot);
        if (existing.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(existing, result)) return false;
        return existing.getCount() + result.getCount() <= existing.getMaxStackSize();
    }

    private void insertOutput(int slot, ItemStack result) {
        ItemStack existing = outputSlots.getStackInSlot(slot);
        if (existing.isEmpty()) {
            outputSlots.setStackInSlot(slot, result);
        } else {
            existing.grow(result.getCount());
        }
    }

    public FluidTank getFuelTank() { return fuelTank; }
    public ItemStackHandler getInputSlots() { return inputSlots; }
    public ItemStackHandler getOutputSlots() { return outputSlots; }
    public int getInputSlotCount() { return inputSlotCount; }
    public int getOutputSlotCount() { return outputSlotCount; }
    public boolean isDualSlot() { return inputSlotCount > 1; }

    @Override
    public Component getDisplayName() {
        return Component.translatable(translationKey);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new ApicaFurnaceMenu(containerId, playerInv, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Input", inputSlots.serializeNBT(registries));
        tag.put("Output", outputSlots.serializeNBT(registries));
        tag.put("FuelTank", fuelTank.writeToNBT(registries, new CompoundTag()));
        for (int i = 0; i < inputSlotCount; i++) {
            tag.putInt("Progress" + i, progress[i]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inputSlots.deserializeNBT(registries, tag.getCompound("Input"));
        outputSlots.deserializeNBT(registries, tag.getCompound("Output"));
        fuelTank.readFromNBT(registries, tag.getCompound("FuelTank"));
        for (int i = 0; i < inputSlotCount; i++) {
            progress[i] = tag.getInt("Progress" + i);
        }
    }
}
