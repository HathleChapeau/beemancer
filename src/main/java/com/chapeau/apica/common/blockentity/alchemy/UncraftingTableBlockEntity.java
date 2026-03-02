/**
 * ============================================================
 * [UncraftingTableBlockEntity.java]
 * Description: BlockEntity pour l'Uncrafting Table — décompose un item crafté en ses ingrédients
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Place un item crafté dans le slot input
 * - Reverse-lookup la recette de crafting
 * - Consomme ~500 mB de nectar graduellement sur 2 minutes
 * - Produit les ingrédients dans une grille 3x3 de sortie
 * - Pause si le tank est vide (0 mB), reprend quand rempli
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | UncraftingTableBlock    | Bloc parent          | Etat WORKING                   |
 * | UncraftingTableMenu     | Menu container       | createMenu                     |
 * | ApicaFluids             | Fluides du mod       | Validation nectar              |
 * | ApicaBlockEntities      | Registre BE          | Type factory                   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - UncraftingTableBlock (newBlockEntity, ticker)
 * - UncraftingTableMenu (server constructor)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.alchemy;

import com.chapeau.apica.common.block.alchemy.UncraftingTableBlock;
import com.chapeau.apica.common.menu.alchemy.UncraftingTableMenu;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.registry.ApicaFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class UncraftingTableBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TANK_CAPACITY = 4000;
    public static final int MAX_PROGRESS = 200;
    public static final int NECTAR_COST = 500;

    private final ItemStackHandler inputSlot;
    private final ItemStackHandler outputSlots;
    private final FluidTank nectarTank;

    private int progress;
    private NonNullList<ItemStack> cachedIngredients;

    protected final ContainerData dataAccess;

    public UncraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ApicaBlockEntities.UNCRAFTING_TABLE.get(), pos, state);

        this.inputSlot = new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                syncToClient();
            }
        };

        this.outputSlots = new ItemStackHandler(9) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                syncToClient();
            }
        };

        this.nectarTank = new FluidTank(TANK_CAPACITY) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return stack.getFluid().isSame(ApicaFluids.NECTAR_SOURCE.get());
            }
            @Override
            protected void onContentsChanged() { setChanged(); }
        };

        this.dataAccess = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> progress;
                    case 1 -> MAX_PROGRESS;
                    case 2 -> nectarTank.getFluidAmount();
                    case 3 -> nectarTank.getCapacity();
                    default -> 0;
                };
            }
            @Override
            public void set(int index, int value) {
                if (index == 0) progress = value;
            }
            @Override
            public int getCount() { return 4; }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, UncraftingTableBlockEntity be) {
        boolean wasWorking = state.getValue(UncraftingTableBlock.WORKING);
        boolean isWorking = be.tick(level);

        if (wasWorking != isWorking) {
            level.setBlock(pos, state.setValue(UncraftingTableBlock.WORKING, isWorking), 3);
        }
        be.setChanged();
    }

    private boolean tick(Level level) {
        ItemStack input = inputSlot.getStackInSlot(0);

        if (input.isEmpty()) {
            resetProgress();
            return false;
        }

        if (cachedIngredients == null) {
            cachedIngredients = findIngredients(level, input);
            if (cachedIngredients == null) {
                return false;
            }
        }

        if (!outputsEmpty()) {
            return false;
        }

        if (nectarTank.getFluidAmount() <= 0) {
            return false;
        }

        int expectedDrained = (int) ((long) (progress + 1) * NECTAR_COST / MAX_PROGRESS);
        int currentDrained = (int) ((long) progress * NECTAR_COST / MAX_PROGRESS);
        int toDrain = expectedDrained - currentDrained;
        if (toDrain > 0) {
            nectarTank.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
        }
        progress++;

        if (progress >= MAX_PROGRESS) {
            completeCraft();
            return false;
        }

        return true;
    }

    private void completeCraft() {
        if (cachedIngredients == null) return;

        for (int i = 0; i < cachedIngredients.size() && i < 9; i++) {
            outputSlots.setStackInSlot(i, cachedIngredients.get(i).copy());
        }

        inputSlot.extractItem(0, 1, false);
        resetProgress();
    }

    private void resetProgress() {
        progress = 0;
        cachedIngredients = null;
    }

    private boolean outputsEmpty() {
        for (int i = 0; i < 9; i++) {
            if (!outputSlots.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private NonNullList<ItemStack> findIngredients(Level level, ItemStack target) {
        for (RecipeHolder<CraftingRecipe> holder : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            CraftingRecipe recipe = holder.value();
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (ItemStack.isSameItem(result, target)) {
                NonNullList<Ingredient> ingredients = recipe.getIngredients();
                NonNullList<ItemStack> resolved = NonNullList.withSize(9, ItemStack.EMPTY);
                for (int i = 0; i < ingredients.size() && i < 9; i++) {
                    Ingredient ing = ingredients.get(i);
                    ItemStack[] items = ing.getItems();
                    if (items.length > 0) {
                        resolved.set(i, items[0].copy());
                    }
                }
                return resolved;
            }
        }
        return null;
    }

    public FluidTank getNectarTank() { return nectarTank; }
    public ItemStackHandler getInputSlot() { return inputSlot; }
    public ItemStackHandler getOutputSlots() { return outputSlots; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.apica.uncrafting_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new UncraftingTableMenu(containerId, playerInv, this, dataAccess);
    }

    // ==================== Sync ====================

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Input", inputSlot.serializeNBT(registries));
        tag.put("Output", outputSlots.serializeNBT(registries));
        tag.put("NectarTank", nectarTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inputSlot.deserializeNBT(registries, tag.getCompound("Input"));
        outputSlots.deserializeNBT(registries, tag.getCompound("Output"));
        nectarTank.readFromNBT(registries, tag.getCompound("NectarTank"));
        progress = tag.getInt("Progress");
    }
}
