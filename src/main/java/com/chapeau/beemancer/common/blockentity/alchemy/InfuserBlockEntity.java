/**
 * ============================================================
 * [InfuserBlockEntity.java]
 * Description: Infuse du bois avec du miel pour créer du bois emmiélé
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Input: Any wood (tag #logs) + 250mB Honey
 * - Output: HONEYED_WOOD (ou selon recette JSON)
 * - Process time: configurable via recette
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.InfuserBlock;
import com.chapeau.beemancer.common.menu.alchemy.InfuserMenu;
import com.chapeau.beemancer.core.recipe.BeemancerRecipeTypes;
import com.chapeau.beemancer.core.recipe.ProcessingRecipeInput;
import com.chapeau.beemancer.core.recipe.type.InfusingRecipe;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import net.minecraft.world.level.block.entity.BlockEntityType;

import javax.annotation.Nullable;
import com.chapeau.beemancer.core.registry.BeemancerParticles;
import com.chapeau.beemancer.core.util.ParticleHelper;
import java.util.Optional;

public class InfuserBlockEntity extends BlockEntity implements MenuProvider {
    // --- TIER CONFIG ---
    public static final int TIER1_TANK_CAPACITY = 4000;
    public static final float TIER1_PROCESS_MULTIPLIER = 1.0f;

    public static final int TIER2_TANK_CAPACITY = 8000;
    public static final float TIER2_PROCESS_MULTIPLIER = 0.7f;

    public static final int TIER3_TANK_CAPACITY = 16000;
    public static final float TIER3_PROCESS_MULTIPLIER = 0.5f;

    private static final int DEFAULT_PROCESS_TIME = 200;

    private final int tankCapacity;
    private final float processTimeMultiplier;

    private final ItemStackHandler inputSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            currentRecipe = null; // Invalidate cached recipe
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    private final ItemStackHandler outputSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    // Flag pour eviter l'invalidation de la recette quand la machine drain elle-meme
    private boolean isProcessingDrain = false;

    private final FluidTank honeyTank;

    private int progress = 0;
    private int currentProcessTime = DEFAULT_PROCESS_TIME;
    @Nullable
    private RecipeHolder<InfusingRecipe> currentRecipe = null;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> currentProcessTime;
                case 2 -> honeyTank.getFluidAmount();
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

    public InfuserBlockEntity(BlockPos pos, BlockState state) {
        this(BeemancerBlockEntities.INFUSER.get(), pos, state,
            TIER1_TANK_CAPACITY, TIER1_PROCESS_MULTIPLIER);
    }

    public InfuserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                              int tankCapacity, float processTimeMultiplier) {
        super(type, pos, state);
        this.tankCapacity = tankCapacity;
        this.processTimeMultiplier = processTimeMultiplier;

        this.honeyTank = new FluidTank(tankCapacity) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return stack.getFluid() == BeemancerFluids.HONEY_SOURCE.get();
            }
            @Override
            protected void onContentsChanged() {
                setChanged();
                if (!isProcessingDrain) {
                    currentRecipe = null;
                }
            }
        };
    }

    // Factory methods for tiered versions
    public static InfuserBlockEntity createTier2(BlockPos pos, BlockState state) {
        return new InfuserBlockEntity(BeemancerBlockEntities.INFUSER_TIER2.get(), pos, state,
            TIER2_TANK_CAPACITY, TIER2_PROCESS_MULTIPLIER);
    }

    public static InfuserBlockEntity createTier3(BlockPos pos, BlockState state) {
        return new InfuserBlockEntity(BeemancerBlockEntities.INFUSER_TIER3.get(), pos, state,
            TIER3_TANK_CAPACITY, TIER3_PROCESS_MULTIPLIER);
    }

    public int getTankCapacity() { return tankCapacity; }
    public float getProcessTimeMultiplier() { return processTimeMultiplier; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, InfuserBlockEntity be) {
        boolean wasWorking = state.getValue(InfuserBlock.WORKING);
        boolean isWorking = false;

        Optional<RecipeHolder<InfusingRecipe>> recipe = be.findRecipe(level);
        if (recipe.isPresent()) {
            be.currentRecipe = recipe.get();
            be.currentProcessTime = Math.max(1, (int)(recipe.get().value().processingTime() * be.processTimeMultiplier));

            if (be.canProcess(recipe.get().value())) {
                be.progress++;
                isWorking = true;

                if (be.progress >= be.currentProcessTime) {
                    be.processItem(recipe.get().value());
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

        if (wasWorking != isWorking) {
            level.setBlock(pos, state.setValue(InfuserBlock.WORKING, isWorking), 3);
        }

        // Particules pixel jaune miel orbitant autour du bloc pendant le processing
        if (isWorking && level instanceof ServerLevel serverLevel && level.getGameTime() % 2 == 0) {
            Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            ParticleHelper.orbitingRing(serverLevel, BeemancerParticles.HONEY_PIXEL.get(), center, 0.35, 8, 0.08);
        }

        be.setChanged();
    }

    private Optional<RecipeHolder<InfusingRecipe>> findRecipe(Level level) {
        // Eviter les lookups inutiles si les inputs sont vides
        if (inputSlot.getStackInSlot(0).isEmpty() || honeyTank.isEmpty()) {
            return Optional.empty();
        }
        ProcessingRecipeInput input = createRecipeInput();
        return level.getRecipeManager().getRecipeFor(
            BeemancerRecipeTypes.INFUSING.get(),
            input,
            level
        );
    }

    private ProcessingRecipeInput createRecipeInput() {
        return ProcessingRecipeInput.of(
            inputSlot.getStackInSlot(0),
            honeyTank.getFluid()
        );
    }

    private boolean canProcess(InfusingRecipe recipe) {
        // Check if we have space in output
        ItemStack output = outputSlot.getStackInSlot(0);
        if (output.isEmpty()) {
            return true;
        }

        ItemStack expectedOutput = recipe.result();
        return ItemStack.isSameItemSameComponents(output, expectedOutput)
            && output.getCount() < output.getMaxStackSize();
    }

    private void processItem(InfusingRecipe recipe) {
        // Consume input
        inputSlot.extractItem(0, 1, false);

        // Drain interne - ne pas invalider la recette
        isProcessingDrain = true;
        honeyTank.drain(recipe.fluidIngredient().amount(), IFluidHandler.FluidAction.EXECUTE);
        isProcessingDrain = false;

        // Add result to output
        ItemStack output = outputSlot.getStackInSlot(0);
        if (output.isEmpty()) {
            outputSlot.setStackInSlot(0, recipe.result().copy());
        } else {
            output.grow(1);
        }
    }

    public FluidTank getHoneyTank() { return honeyTank; }
    public ItemStackHandler getInputSlot() { return inputSlot; }
    public ItemStackHandler getOutputSlot() { return outputSlot; }
    @Nullable
    public RecipeHolder<InfusingRecipe> getCurrentRecipe() { return currentRecipe; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.infuser");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new InfuserMenu(containerId, playerInv, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Input", inputSlot.serializeNBT(registries));
        tag.put("Output", outputSlot.serializeNBT(registries));
        tag.put("HoneyTank", honeyTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inputSlot.deserializeNBT(registries, tag.getCompound("Input"));
        outputSlot.deserializeNBT(registries, tag.getCompound("Output"));
        honeyTank.readFromNBT(registries, tag.getCompound("HoneyTank"));
        progress = tag.getInt("Progress");
    }

    // --- Client sync pour le renderer (affichage item + particules) ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Input", inputSlot.serializeNBT(registries));
        tag.put("Output", outputSlot.serializeNBT(registries));
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
