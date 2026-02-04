/**
 * ============================================================
 * [InfuserHeartBlockEntity.java]
 * Description: Controleur du multibloc Infuser - Infusion d'items avec miel
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | MultiblockController    | Interface controleur | Formation/destruction          |
 * | MultiblockPatterns      | Definition pattern   | INFUSER_MULTIBLOCK             |
 * | MultiblockValidator     | Validation           | tryFormMultiblock()            |
 * | MultiblockEvents        | Enregistrement       | Detection destruction          |
 * | BeemancerBlockEntities  | Type registration    | Constructor                    |
 * | BeemancerRecipeTypes    | Recettes infusing    | Processing                     |
 * | BeemancerFluids         | Validation fluides   | Tank filtering                 |
 * | ParticleHelper          | Effets visuels       | Particules processing          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - InfuserHeartBlock.java (creation BlockEntity, ticker)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.alchemy.InfuserHeartBlock;
import com.chapeau.beemancer.common.menu.alchemy.InfuserMenu;
import com.chapeau.beemancer.core.multiblock.BlockMatcher;
import com.chapeau.beemancer.core.multiblock.MultiblockController;
import com.chapeau.beemancer.core.multiblock.MultiblockEvents;
import com.chapeau.beemancer.core.multiblock.MultiblockPattern;
import com.chapeau.beemancer.core.multiblock.MultiblockPatterns;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import com.chapeau.beemancer.core.multiblock.MultiblockValidator;
import com.chapeau.beemancer.core.recipe.BeemancerRecipeTypes;
import com.chapeau.beemancer.core.recipe.ProcessingRecipeInput;
import com.chapeau.beemancer.core.recipe.type.InfusingRecipe;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import com.chapeau.beemancer.core.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * BlockEntity du Coeur de l'Infuser multibloc.
 * Equivalent a l'InfuserBlockEntity TIER3 (16000mB, 0.5x process time).
 * Ne process que lorsque le multibloc est forme.
 */
public class InfuserHeartBlockEntity extends BlockEntity implements MultiblockController, MenuProvider {

    private static final int TANK_CAPACITY = 16000;
    private static final float PROCESS_TIME_MULTIPLIER = 0.5f;
    private static final int DEFAULT_PROCESS_TIME = 200;

    private boolean formed = false;
    private boolean isProcessingDrain = false;

    private final ItemStackHandler inputSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            currentRecipe = null;
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

    public InfuserHeartBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.INFUSER_HEART.get(), pos, state);
    }

    // ==================== MultiblockController ====================

    @Override
    public boolean isFormed() { return formed; }

    @Override
    public MultiblockPattern getPattern() {
        return MultiblockPatterns.INFUSER_MULTIBLOCK;
    }

    @Override
    public BlockPos getControllerPos() { return worldPosition; }

    @Override
    public void onMultiblockFormed() {
        formed = true;
        if (level != null && !level.isClientSide()) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(InfuserHeartBlock.MULTIBLOCK)) {
                level.setBlock(worldPosition, state.setValue(InfuserHeartBlock.MULTIBLOCK, MultiblockProperty.INFUSER), 3);
            }
            setFormedOnStructureBlocks(true);
            MultiblockEvents.registerActiveController(level, worldPosition);
            setChanged();
        }
    }

    @Override
    public void onMultiblockBroken() {
        formed = false;
        if (level != null && !level.isClientSide()) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(InfuserHeartBlock.MULTIBLOCK)) {
                level.setBlock(worldPosition, state.setValue(InfuserHeartBlock.MULTIBLOCK, MultiblockProperty.NONE), 3);
            }
            setFormedOnStructureBlocks(false);
            MultiblockEvents.unregisterController(worldPosition);
            setChanged();
        }
    }

    /**
     * Met à jour la propriété MULTIBLOCK sur tous les blocs de la structure.
     */
    private void setFormedOnStructureBlocks(boolean formed) {
        if (level == null) return;
        for (MultiblockPattern.PatternElement element : getPattern().getElements()) {
            if (BlockMatcher.isAirMatcher(element.matcher())) continue;
            BlockPos blockPos = worldPosition.offset(element.offset());
            BlockState state = level.getBlockState(blockPos);
            for (var prop : state.getProperties()) {
                if (prop.getName().equals("multiblock") && prop instanceof net.minecraft.world.level.block.state.properties.EnumProperty<?> enumProp) {
                    @SuppressWarnings("unchecked")
                    net.minecraft.world.level.block.state.properties.EnumProperty<MultiblockProperty> mbProp =
                        (net.minecraft.world.level.block.state.properties.EnumProperty<MultiblockProperty>) enumProp;
                    MultiblockProperty value = formed ? MultiblockProperty.INFUSER : MultiblockProperty.NONE;
                    if (mbProp.getPossibleValues().contains(value) && state.getValue(mbProp) != value) {
                        level.setBlock(blockPos, state.setValue(mbProp, value), 3);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Tente de former le multibloc Infuser.
     * @return true si la formation a reussi
     */
    public boolean tryFormMultiblock() {
        if (level == null || level.isClientSide()) return false;

        int rotation = MultiblockValidator.validateWithRotations(getPattern(), level, worldPosition);
        if (rotation >= 0) {
            onMultiblockFormed();
            return true;
        }

        Beemancer.LOGGER.debug("Infuser validation failed at {}", worldPosition);
        return false;
    }

    // ==================== Processing ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, InfuserHeartBlockEntity be) {
        if (!be.formed) return;

        boolean wasWorking = state.getValue(InfuserHeartBlock.WORKING);
        boolean isWorking = false;

        Optional<RecipeHolder<InfusingRecipe>> recipe = be.findRecipe(level);
        if (recipe.isPresent()) {
            be.currentRecipe = recipe.get();
            be.currentProcessTime = Math.max(1, (int)(recipe.get().value().processingTime() * PROCESS_TIME_MULTIPLIER));

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
            level.setBlock(pos, state.setValue(InfuserHeartBlock.WORKING, isWorking), 3);
        }

        if (isWorking && level instanceof ServerLevel serverLevel && level.getGameTime() % 2 == 0) {
            DustParticleOptions honeyParticle = new DustParticleOptions(new Vector3f(1.0f, 0.75f, 0.1f), 0.6f);
            Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            ParticleHelper.orbitingRing(serverLevel, honeyParticle, center, 0.35, 8, 0.08);
        }

        be.setChanged();
    }

    private Optional<RecipeHolder<InfusingRecipe>> findRecipe(Level level) {
        if (inputSlot.getStackInSlot(0).isEmpty() || honeyTank.isEmpty()) {
            return Optional.empty();
        }
        ProcessingRecipeInput input = ProcessingRecipeInput.of(
            inputSlot.getStackInSlot(0),
            honeyTank.getFluid()
        );
        return level.getRecipeManager().getRecipeFor(
            BeemancerRecipeTypes.INFUSING.get(),
            input,
            level
        );
    }

    private boolean canProcess(InfusingRecipe recipe) {
        ItemStack output = outputSlot.getStackInSlot(0);
        if (output.isEmpty()) return true;
        ItemStack expectedOutput = recipe.result();
        return ItemStack.isSameItemSameComponents(output, expectedOutput)
            && output.getCount() < output.getMaxStackSize();
    }

    private void processItem(InfusingRecipe recipe) {
        inputSlot.extractItem(0, 1, false);
        isProcessingDrain = true;
        honeyTank.drain(recipe.fluidIngredient().amount(), IFluidHandler.FluidAction.EXECUTE);
        isProcessingDrain = false;

        ItemStack output = outputSlot.getStackInSlot(0);
        if (output.isEmpty()) {
            outputSlot.setStackInSlot(0, recipe.result().copy());
        } else {
            output.grow(1);
        }
    }

    // ==================== Accessors ====================

    public FluidTank getHoneyTank() { return honeyTank; }
    public ItemStackHandler getInputSlot() { return inputSlot; }
    public ItemStackHandler getOutputSlot() { return outputSlot; }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.infuser");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new InfuserMenu(containerId, playerInv, this, dataAccess);
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
        tag.put("Input", inputSlot.serializeNBT(registries));
        tag.put("Output", outputSlot.serializeNBT(registries));
        tag.put("HoneyTank", honeyTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("Formed");
        inputSlot.deserializeNBT(registries, tag.getCompound("Input"));
        outputSlot.deserializeNBT(registries, tag.getCompound("Output"));
        honeyTank.readFromNBT(registries, tag.getCompound("HoneyTank"));
        progress = tag.getInt("Progress");
    }

    // ==================== Sync ====================

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Formed", formed);
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
