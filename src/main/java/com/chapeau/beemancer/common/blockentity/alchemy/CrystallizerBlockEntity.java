/**
 * ============================================================
 * [CrystallizerBlockEntity.java]
 * Description: Transforme les fluides en cristaux solides
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Honey (500mB) -> HONEY_CRYSTAL (ou selon recette JSON)
 * - Royal Jelly (500mB) -> ROYAL_HONEY_CRYSTAL
 * - Process time: configurable via recette
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.CrystallizerBlock;
import com.chapeau.beemancer.common.menu.alchemy.CrystallizerMenu;
import com.chapeau.beemancer.core.recipe.BeemancerRecipeTypes;
import com.chapeau.beemancer.core.recipe.ProcessingRecipeInput;
import com.chapeau.beemancer.core.recipe.type.CrystallizingRecipe;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
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
import net.minecraft.world.level.Level;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Optional;

public class CrystallizerBlockEntity extends BlockEntity implements MenuProvider {
    private static final int DEFAULT_PROCESS_TIME = 100;

    // Flag pour eviter l'invalidation de la recette quand la machine drain elle-meme
    private boolean isProcessingDrain = false;

    private final FluidTank inputTank = new FluidTank(4000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            // Invalider seulement si ce n'est pas un drain interne
            if (!isProcessingDrain) {
                currentRecipe = null;
            }
        }
    };

    private final ItemStackHandler outputSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false; // Extraction seulement, pas d'insertion externe
        }
    };

    private int progress = 0;
    private int currentProcessTime = DEFAULT_PROCESS_TIME;
    @Nullable
    private RecipeHolder<CrystallizingRecipe> currentRecipe = null;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> currentProcessTime;
                case 2 -> inputTank.getFluidAmount();
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            if (index == 1) currentProcessTime = value;
        }
        @Override
        public int getCount() { return 3; }
    };

    public CrystallizerBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.CRYSTALLIZER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrystallizerBlockEntity be) {
        boolean wasActive = state.getValue(CrystallizerBlock.ACTIVE);
        boolean isActive = false;

        Optional<RecipeHolder<CrystallizingRecipe>> recipe = be.findRecipe(level);
        if (recipe.isPresent()) {
            be.currentRecipe = recipe.get();
            be.currentProcessTime = recipe.get().value().processingTime();

            if (be.canProcess(recipe.get().value())) {
                be.progress++;
                isActive = true;

                if (be.progress >= be.currentProcessTime) {
                    be.processFluid(recipe.get().value());
                    be.progress = 0;
                    be.currentRecipe = null;
                }
            } else {
                be.progress = 0;
            }
        } else {
            be.progress = 0;
            be.currentRecipe = null;
        }

        if (wasActive != isActive) {
            level.setBlock(pos, state.setValue(CrystallizerBlock.ACTIVE, isActive), 3);
        }

        // Sync au client pour le renderer (progress + output)
        if (isActive || wasActive) {
            level.sendBlockUpdated(pos, state, state, 3);
        }

        be.setChanged();
    }

    private Optional<RecipeHolder<CrystallizingRecipe>> findRecipe(Level level) {
        // Eviter les lookups inutiles si le tank est vide
        if (inputTank.isEmpty()) {
            return Optional.empty();
        }
        ProcessingRecipeInput input = createRecipeInput();
        return level.getRecipeManager().getRecipeFor(
            BeemancerRecipeTypes.CRYSTALLIZING.get(),
            input,
            level
        );
    }

    private ProcessingRecipeInput createRecipeInput() {
        return ProcessingRecipeInput.ofFluid(inputTank.getFluid());
    }

    private boolean canProcess(CrystallizingRecipe recipe) {
        // Ne pas produire si un crystal est deja present dans l'output
        return outputSlot.getStackInSlot(0).isEmpty();
    }

    private void processFluid(CrystallizingRecipe recipe) {
        // Consume fluid (drain interne - ne pas invalider la recette)
        isProcessingDrain = true;
        inputTank.drain(recipe.fluidIngredient().amount(), IFluidHandler.FluidAction.EXECUTE);
        isProcessingDrain = false;

        // Add crystal to output
        ItemStack existing = outputSlot.getStackInSlot(0);
        if (existing.isEmpty()) {
            outputSlot.setStackInSlot(0, recipe.result().copy());
        } else {
            existing.grow(1);
        }
    }

    public FluidTank getInputTank() { return inputTank; }
    public ItemStackHandler getOutputSlot() { return outputSlot; }
    @Nullable
    public RecipeHolder<CrystallizingRecipe> getCurrentRecipe() { return currentRecipe; }
    public int getProgress() { return progress; }
    public int getCurrentProcessTime() { return currentProcessTime; }
    public boolean hasOutputCrystal() { return !outputSlot.getStackInSlot(0).isEmpty(); }

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
        tag.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Output", outputSlot.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("ProcessTime", currentProcessTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
        outputSlot.deserializeNBT(registries, tag.getCompound("Output"));
        progress = tag.getInt("Progress");
        if (tag.contains("ProcessTime")) {
            currentProcessTime = tag.getInt("ProcessTime");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Progress", progress);
        tag.putInt("ProcessTime", currentProcessTime);
        tag.put("Output", outputSlot.serializeNBT(registries));
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }
}
