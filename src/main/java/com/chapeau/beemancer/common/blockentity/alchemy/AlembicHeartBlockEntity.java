/**
 * ============================================================
 * [AlembicHeartBlockEntity.java]
 * Description: Controleur du multibloc Alembic - Distillation de fluides
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | MultiblockController    | Interface controleur | Formation/destruction          |
 * | MultiblockPatterns      | Definition pattern   | ALEMBIC_MULTIBLOCK             |
 * | MultiblockValidator     | Validation           | tryFormMultiblock()            |
 * | MultiblockEvents        | Enregistrement       | Detection destruction          |
 * | BeemancerBlockEntities  | Type registration    | Constructor                    |
 * | BeemancerRecipeTypes    | Recettes distilling  | Processing                     |
 * | BeemancerFluids         | Validation fluides   | Tank filtering                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - AlembicHeartBlock.java (creation BlockEntity, ticker)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.alchemy.AlembicHeartBlock;
import com.chapeau.beemancer.common.menu.alchemy.AlembicMenu;
import com.chapeau.beemancer.core.multiblock.MultiblockController;
import com.chapeau.beemancer.core.multiblock.MultiblockEvents;
import com.chapeau.beemancer.core.multiblock.MultiblockPattern;
import com.chapeau.beemancer.core.multiblock.MultiblockPatterns;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import com.chapeau.beemancer.core.multiblock.MultiblockValidator;
import com.chapeau.beemancer.core.recipe.BeemancerRecipeTypes;
import com.chapeau.beemancer.core.recipe.ProcessingRecipeInput;
import com.chapeau.beemancer.core.recipe.type.DistillingRecipe;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * BlockEntity du Coeur de l'Alembic multibloc.
 * Gere la distillation: Honey + Royal Jelly -> Nectar.
 * Ne process que lorsque le multibloc est forme.
 */
public class AlembicHeartBlockEntity extends BlockEntity implements MultiblockController, MenuProvider {

    private static final int TANK_CAPACITY = 4000;
    private static final int DEFAULT_PROCESS_TIME = 80;

    private boolean formed = false;
    private boolean isProcessingDrain = false;

    private final FluidTank honeyTank = new FluidTank(TANK_CAPACITY) {
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

    private final FluidTank royalJellyTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == BeemancerFluids.ROYAL_JELLY_SOURCE.get();
        }
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (!isProcessingDrain) {
                currentRecipe = null;
            }
        }
    };

    private final FluidTank nectarTank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private int progress = 0;
    private int currentProcessTime = DEFAULT_PROCESS_TIME;
    @Nullable
    private RecipeHolder<DistillingRecipe> currentRecipe = null;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> currentProcessTime;
                case 2 -> honeyTank.getFluidAmount();
                case 3 -> royalJellyTank.getFluidAmount();
                case 4 -> nectarTank.getFluidAmount();
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            if (index == 1) currentProcessTime = value;
        }
        @Override
        public int getCount() { return 5; }
    };

    public AlembicHeartBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.ALEMBIC_HEART.get(), pos, state);
    }

    // ==================== MultiblockController ====================

    @Override
    public boolean isFormed() {
        return formed;
    }

    @Override
    public MultiblockPattern getPattern() {
        return MultiblockPatterns.ALEMBIC_MULTIBLOCK;
    }

    @Override
    public BlockPos getControllerPos() {
        return worldPosition;
    }

    @Override
    public void onMultiblockFormed() {
        formed = true;
        if (level != null && !level.isClientSide()) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(AlembicHeartBlock.MULTIBLOCK)) {
                level.setBlock(worldPosition, state.setValue(AlembicHeartBlock.MULTIBLOCK, MultiblockProperty.ALEMBIC), 3);
            }
            MultiblockEvents.registerActiveController(level, worldPosition);
            setChanged();
        }
    }

    @Override
    public void onMultiblockBroken() {
        formed = false;
        if (level != null && !level.isClientSide()) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(AlembicHeartBlock.MULTIBLOCK)) {
                level.setBlock(worldPosition, state.setValue(AlembicHeartBlock.MULTIBLOCK, MultiblockProperty.NONE), 3);
            }
            MultiblockEvents.unregisterController(worldPosition);
            setChanged();
        }
    }

    /**
     * Tente de former le multibloc Alembic.
     * @return true si la formation a reussi
     */
    public boolean tryFormMultiblock() {
        if (level == null || level.isClientSide()) return false;

        var result = MultiblockValidator.validateDetailed(getPattern(), level, worldPosition);

        if (result.valid()) {
            onMultiblockFormed();
            return true;
        }

        Beemancer.LOGGER.debug("Alembic validation failed at {} - {}", result.failedAt(), result.reason());
        return false;
    }

    // ==================== Processing ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlembicHeartBlockEntity be) {
        if (!be.formed) return;

        boolean wasDistilling = state.getValue(AlembicHeartBlock.DISTILLING);
        boolean isDistilling = false;

        Optional<RecipeHolder<DistillingRecipe>> recipe = be.findRecipe(level);
        if (recipe.isPresent()) {
            be.currentRecipe = recipe.get();
            be.currentProcessTime = recipe.get().value().processingTime();

            if (be.canProcess(recipe.get().value())) {
                be.progress++;
                isDistilling = true;

                if (be.progress >= be.currentProcessTime) {
                    be.processFluids(recipe.get().value());
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

        if (be.nectarTank.getFluidAmount() > 0) {
            var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.below(), Direction.UP);
            if (cap != null && !be.nectarTank.isEmpty()) {
                int toTransferAmount = Math.min(100, be.nectarTank.getFluidAmount());
                FluidStack toTransfer = new FluidStack(be.nectarTank.getFluid().getFluid(), toTransferAmount);
                int canFill = cap.fill(toTransfer, IFluidHandler.FluidAction.SIMULATE);
                if (canFill > 0) {
                    FluidStack actualTransfer = new FluidStack(be.nectarTank.getFluid().getFluid(), canFill);
                    int filled = cap.fill(actualTransfer, IFluidHandler.FluidAction.EXECUTE);
                    if (filled > 0) be.nectarTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }

        if (wasDistilling != isDistilling) {
            level.setBlock(pos, state.setValue(AlembicHeartBlock.DISTILLING, isDistilling), 3);
        }

        be.setChanged();
    }

    private Optional<RecipeHolder<DistillingRecipe>> findRecipe(Level level) {
        if (honeyTank.isEmpty() && royalJellyTank.isEmpty()) {
            return Optional.empty();
        }
        ProcessingRecipeInput input = ProcessingRecipeInput.of(
            List.of(),
            List.of(honeyTank.getFluid(), royalJellyTank.getFluid())
        );
        return level.getRecipeManager().getRecipeFor(
            BeemancerRecipeTypes.DISTILLING.get(),
            input,
            level
        );
    }

    private boolean canProcess(DistillingRecipe recipe) {
        FluidStack output = recipe.getFluidOutput();
        return nectarTank.getFluidAmount() + output.getAmount() <= nectarTank.getCapacity();
    }

    private void processFluids(DistillingRecipe recipe) {
        isProcessingDrain = true;
        if (recipe.fluidIngredients().size() >= 1) {
            honeyTank.drain(recipe.fluidIngredients().get(0).amount(), IFluidHandler.FluidAction.EXECUTE);
        }
        if (recipe.fluidIngredients().size() >= 2) {
            royalJellyTank.drain(recipe.fluidIngredients().get(1).amount(), IFluidHandler.FluidAction.EXECUTE);
        }
        isProcessingDrain = false;
        nectarTank.fill(recipe.getFluidOutput(), IFluidHandler.FluidAction.EXECUTE);
    }

    // ==================== Accessors ====================

    public FluidTank getHoneyTank() { return honeyTank; }
    public FluidTank getRoyalJellyTank() { return royalJellyTank; }
    public FluidTank getNectarTank() { return nectarTank; }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.alembic");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new AlembicMenu(containerId, playerInv, this, dataAccess);
    }

    // ==================== Lifecycle ====================

    @Override
    public void setRemoved() {
        super.setRemoved();
        MultiblockEvents.unregisterController(worldPosition);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (formed && level != null && !level.isClientSide()) {
            MultiblockEvents.registerActiveController(level, worldPosition);
        }
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Formed", formed);
        tag.put("HoneyTank", honeyTank.writeToNBT(registries, new CompoundTag()));
        tag.put("RoyalJellyTank", royalJellyTank.writeToNBT(registries, new CompoundTag()));
        tag.put("NectarTank", nectarTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("Formed");
        honeyTank.readFromNBT(registries, tag.getCompound("HoneyTank"));
        royalJellyTank.readFromNBT(registries, tag.getCompound("RoyalJellyTank"));
        nectarTank.readFromNBT(registries, tag.getCompound("NectarTank"));
        progress = tag.getInt("Progress");
    }

    // ==================== Sync ====================

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Formed", formed);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
