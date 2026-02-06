/**
 * ============================================================
 * [CentrifugeHeartBlockEntity.java]
 * Description: Controleur du multibloc Centrifuge - Centrifugation automatique
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | MultiblockController          | Interface controleur | Formation/destruction          |
 * | MultiblockCapabilityProvider  | Delegation caps      | Capabilities sur reservoirs    |
 * | MultiblockPatterns            | Definition pattern   | CENTRIFUGE_MULTIBLOCK          |
 * | MultiblockValidator           | Validation           | tryFormMultiblock()            |
 * | MultiblockEvents              | Enregistrement       | Detection destruction          |
 * | SplitFluidHandler             | Split fill/drain     | Capability fluid               |
 * | SplitItemHandler              | Split insert/extract | Capability item                |
 * | BeemancerBlockEntities        | Type registration    | Constructor                    |
 * | BeemancerRecipeTypes          | Recettes centrifuge  | Processing                     |
 * | BeemancerFluids               | Validation fluides   | Tank filtering                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CentrifugeHeartBlock.java (creation BlockEntity, ticker)
 * - Beemancer.java (capability registration)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.alchemy.CentrifugeHeartBlock;
import com.chapeau.beemancer.common.blockentity.altar.HoneyReservoirBlockEntity;
import com.chapeau.beemancer.common.menu.alchemy.PoweredCentrifugeMenu;
import com.chapeau.beemancer.core.multiblock.BlockMatcher;
import com.chapeau.beemancer.core.multiblock.BlockIORule;
import com.chapeau.beemancer.core.multiblock.IOMode;
import com.chapeau.beemancer.core.multiblock.MultiblockCapabilityProvider;
import com.chapeau.beemancer.core.multiblock.MultiblockController;
import com.chapeau.beemancer.core.multiblock.MultiblockEvents;
import com.chapeau.beemancer.core.multiblock.MultiblockIOConfig;
import com.chapeau.beemancer.core.multiblock.MultiblockPattern;
import com.chapeau.beemancer.core.multiblock.MultiblockPatterns;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import com.chapeau.beemancer.core.multiblock.MultiblockValidator;
import com.chapeau.beemancer.core.util.SplitFluidHandler;
import com.chapeau.beemancer.core.util.SplitItemHandler;
import com.chapeau.beemancer.core.recipe.BeemancerRecipeTypes;
import com.chapeau.beemancer.core.recipe.ProcessingOutput;
import com.chapeau.beemancer.core.recipe.ProcessingRecipeInput;
import com.chapeau.beemancer.core.recipe.type.CentrifugeRecipe;
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
import net.minecraft.util.RandomSource;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * BlockEntity du Coeur de la Centrifuge multibloc.
 * Equivalent au PoweredCentrifugeBlockEntity TIER3 (25mB/tick, 32000mB, 0.3x process time).
 * Ne process que lorsque le multibloc est forme.
 */
public class CentrifugeHeartBlockEntity extends BlockEntity implements MultiblockController, MultiblockCapabilityProvider, MenuProvider {

    private static final int HONEY_CONSUMPTION = 25;
    private static final int TANK_CAPACITY = 32000;
    private static final float PROCESS_TIME_MULTIPLIER = 0.3f;
    private static final int DEFAULT_PROCESS_TIME = 100;

    // Animation cubes centraux
    private static final float MAX_ROTATION_SPEED = 14.0f;
    private static final float ACCELERATION = 0.15f;

    // Positions des reservoirs (relatif au coeur)
    // Top (Y+1): cardinaux pour le fuel tank (entree miel + items)
    private static final BlockPos[] FUEL_RESERVOIR_OFFSETS = {
        new BlockPos(0, 1, -1),    // Nord
        new BlockPos(-1, 1, 0),    // Ouest
        new BlockPos(1, 1, 0),     // Est
        new BlockPos(0, 1, 1)      // Sud
    };
    // Bottom (Y-1): cardinaux pour le output tank (sortie produits)
    private static final BlockPos[] OUTPUT_RESERVOIR_OFFSETS = {
        new BlockPos(0, -1, -1),   // Nord
        new BlockPos(-1, -1, 0),   // Ouest
        new BlockPos(1, -1, 0),    // Est
        new BlockPos(0, -1, 1)     // Sud
    };

    // Configuration IO declarative : quelles faces exposent quoi
    private static final MultiblockIOConfig IO_CONFIG = MultiblockIOConfig.builder()
        // Top reservoirs (Y+1): fuel/items INPUT sur les cotes uniquement
        .position(0, 1, -1, BlockIORule.sides(IOMode.INPUT), BlockIORule.sides(IOMode.INPUT))
        .position(-1, 1, 0, BlockIORule.sides(IOMode.INPUT), BlockIORule.sides(IOMode.INPUT))
        .position(1, 1, 0, BlockIORule.sides(IOMode.INPUT), BlockIORule.sides(IOMode.INPUT))
        .position(0, 1, 1, BlockIORule.sides(IOMode.INPUT), BlockIORule.sides(IOMode.INPUT))
        // Bottom reservoirs (Y-1): product OUTPUT sur les cotes uniquement
        .position(0, -1, -1, BlockIORule.sides(IOMode.OUTPUT), BlockIORule.sides(IOMode.OUTPUT))
        .position(-1, -1, 0, BlockIORule.sides(IOMode.OUTPUT), BlockIORule.sides(IOMode.OUTPUT))
        .position(1, -1, 0, BlockIORule.sides(IOMode.OUTPUT), BlockIORule.sides(IOMode.OUTPUT))
        .position(0, -1, 1, BlockIORule.sides(IOMode.OUTPUT), BlockIORule.sides(IOMode.OUTPUT))
        .build();

    private boolean formed = false;
    private ItemStack previousInputType = ItemStack.EMPTY;

    // Animation (client-side principalement)
    private float rotation = 0.0f;
    private float velocity = 0.0f;
    private float prevRotation = 0.0f;

    private final ItemStackHandler inputSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            currentRecipe = null;
            checkInputChanged();
        }
    };

    private final ItemStackHandler outputSlots = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    private final FluidTank fuelTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == BeemancerFluids.HONEY_SOURCE.get();
        }
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private final FluidTank outputTank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private final SplitFluidHandler splitFluidHandler;
    private final SplitItemHandler splitItemHandler;

    private int progress = 0;
    private int currentProcessTime = DEFAULT_PROCESS_TIME;
    @Nullable
    private RecipeHolder<CentrifugeRecipe> currentRecipe = null;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> currentProcessTime;
                case 2 -> fuelTank.getFluidAmount();
                case 3 -> outputTank.getFluidAmount();
                case 4 -> fuelTank.getCapacity();
                case 5 -> outputTank.getCapacity();
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            if (index == 1) currentProcessTime = value;
        }
        @Override
        public int getCount() { return 6; }
    };

    public CentrifugeHeartBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.CENTRIFUGE_HEART.get(), pos, state);
        this.splitFluidHandler = new SplitFluidHandler(fuelTank, outputTank);
        this.splitItemHandler = new SplitItemHandler(inputSlot, outputSlots);
    }

    // ==================== MultiblockController ====================

    @Override
    public boolean isFormed() { return formed; }

    @Override
    public MultiblockPattern getPattern() {
        return MultiblockPatterns.CENTRIFUGE_MULTIBLOCK;
    }

    @Override
    public BlockPos getControllerPos() { return worldPosition; }

    @Override
    public void onMultiblockFormed() {
        formed = true;
        if (level != null && !level.isClientSide()) {
            // 1. Link réservoirs AU CONTROLLER d'abord (avant que les blockstates ne déclenchent updateShape)
            linkReservoirControllers(true);

            // 2. Puis changer les blockstates (déclenche updateShape sur les pipes voisines)
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(CentrifugeHeartBlock.MULTIBLOCK)) {
                level.setBlock(worldPosition, state.setValue(CentrifugeHeartBlock.MULTIBLOCK, MultiblockProperty.CENTRIFUGE), 3);
            }
            setFormedOnStructureBlocks(true);

            // 3. Invalider les capabilities de TOUS les blocs du multibloc
            invalidateAllCapabilities();

            MultiblockEvents.registerActiveController(level, worldPosition);
            setChanged();
        }
    }

    @Override
    public void onMultiblockBroken() {
        formed = false;
        if (level != null && !level.isClientSide()) {
            // 1. Changer les blockstates d'abord
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(CentrifugeHeartBlock.MULTIBLOCK)) {
                level.setBlock(worldPosition, state.setValue(CentrifugeHeartBlock.MULTIBLOCK, MultiblockProperty.NONE), 3);
            }
            setFormedOnStructureBlocks(false);

            // 2. Unlink réservoirs (controllerPos = null)
            linkReservoirControllers(false);

            // 3. Invalider les capabilities de TOUS les blocs du multibloc
            invalidateAllCapabilities();

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
                    MultiblockProperty value = formed ? MultiblockProperty.CENTRIFUGE : MultiblockProperty.NONE;
                    if (mbProp.getPossibleValues().contains(value) && state.getValue(mbProp) != value) {
                        level.setBlock(blockPos, state.setValue(mbProp, value), 3);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Tente de former le multibloc Centrifuge.
     * @return true si la formation a reussi
     */
    public boolean tryFormMultiblock() {
        if (level == null || level.isClientSide()) return false;

        int rotation = MultiblockValidator.validateWithRotations(getPattern(), level, worldPosition);
        if (rotation >= 0) {
            onMultiblockFormed();
            return true;
        }

        Beemancer.LOGGER.debug("Centrifuge validation failed at {}", worldPosition);
        return false;
    }

    // ==================== MultiblockCapabilityProvider ====================

    @Override
    @Nullable
    public IFluidHandler getFluidHandlerForBlock(BlockPos worldPos, @Nullable Direction face) {
        if (!formed) return null;
        IOMode mode = IO_CONFIG.getFluidMode(worldPosition, worldPos, face);
        if (mode == null || mode == IOMode.NONE) return null;
        return switch (mode) {
            case INPUT -> SplitFluidHandler.inputOnly(fuelTank);
            case OUTPUT -> SplitFluidHandler.outputOnly(outputTank);
            case BOTH -> splitFluidHandler;
            default -> null;
        };
    }

    @Override
    @Nullable
    public IItemHandler getItemHandlerForBlock(BlockPos worldPos, @Nullable Direction face) {
        if (!formed) return null;
        IOMode mode = IO_CONFIG.getItemMode(worldPosition, worldPos, face);
        if (mode == null || mode == IOMode.NONE) return null;
        return switch (mode) {
            case INPUT -> SplitItemHandler.inputOnly(inputSlot);
            case OUTPUT -> SplitItemHandler.outputOnly(outputSlots);
            case BOTH -> splitItemHandler;
            default -> null;
        };
    }

    /**
     * Lie ou délie les réservoirs au contrôleur pour la délégation de capabilities.
     * Ne déclenche PAS invalidateCapabilities ici (fait séparément dans invalidateAllCapabilities).
     */
    private void linkReservoirControllers(boolean link) {
        if (level == null) return;
        BlockPos[][] allOffsets = { FUEL_RESERVOIR_OFFSETS, OUTPUT_RESERVOIR_OFFSETS };
        for (BlockPos[] offsets : allOffsets) {
            for (BlockPos offset : offsets) {
                BlockPos reservoirPos = worldPosition.offset(offset);
                if (level.getBlockEntity(reservoirPos) instanceof HoneyReservoirBlockEntity reservoir) {
                    reservoir.setControllerPosQuiet(link ? worldPosition : null);
                }
            }
        }
    }

    /**
     * Invalide les capabilities de TOUS les blocs du multibloc (coeur + réservoirs + structure).
     * Force NeoForge à re-query les lambdas de capabilities pour chaque bloc.
     */
    private void invalidateAllCapabilities() {
        if (level == null) return;
        // Invalider le coeur
        level.invalidateCapabilities(worldPosition);
        // Invalider tous les blocs structurels (réservoirs, honeyed stone)
        for (MultiblockPattern.PatternElement element : getPattern().getElements()) {
            BlockPos blockPos = worldPosition.offset(element.offset());
            level.invalidateCapabilities(blockPos);
        }
    }

    // ==================== Animation ====================

    /**
     * Tick client pour l'animation des cubes centraux.
     * La velocity accélère quand WORKING=true, décélère sinon.
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, CentrifugeHeartBlockEntity be) {
        be.prevRotation = be.rotation;

        boolean working = state.hasProperty(CentrifugeHeartBlock.WORKING)
            && state.getValue(CentrifugeHeartBlock.WORKING);

        if (working) {
            be.velocity += ACCELERATION;
        } else {
            be.velocity -= ACCELERATION;
        }

        be.velocity = Math.max(0.0f, Math.min(be.velocity, MAX_ROTATION_SPEED));

        be.rotation += be.velocity;
        if (be.rotation >= 360.0f) {
            be.rotation -= 360.0f;
            be.prevRotation -= 360.0f;
        }
    }

    /**
     * Retourne l'angle de rotation interpolé pour le rendu smooth.
     */
    public float getClientRotation(float partialTick) {
        float interpRotation = prevRotation + (rotation - prevRotation) * partialTick;
        if (interpRotation < 0) {
            interpRotation += 360.0f;
        }
        return interpRotation;
    }

    // ==================== Processing ====================

    private void checkInputChanged() {
        ItemStack current = inputSlot.getStackInSlot(0);
        if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, previousInputType)) {
            progress = 0;
        }
        previousInputType = current.isEmpty() ? ItemStack.EMPTY : current.copyWithCount(1);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CentrifugeHeartBlockEntity be) {
        if (!be.formed) return;

        boolean wasWorking = state.getValue(CentrifugeHeartBlock.WORKING);
        boolean isWorking = false;

        if (be.currentRecipe == null) {
            be.findValidRecipe(level);
        }

        if (be.currentRecipe != null) {
            if (be.fuelTank.getFluidAmount() >= HONEY_CONSUMPTION && be.hasOutputSpace()) {
                be.fuelTank.drain(HONEY_CONSUMPTION, IFluidHandler.FluidAction.EXECUTE);
                be.progress++;
                isWorking = true;

                if (be.progress >= be.currentProcessTime) {
                    be.processItem(level.getRandom());
                    be.progress = 0;
                    be.currentRecipe = null;
                }
            }
        } else {
            be.progress = 0;
        }

        if (wasWorking != isWorking) {
            level.setBlock(pos, state.setValue(CentrifugeHeartBlock.WORKING, isWorking), 3);
        }

        // Mise à jour des niveaux visuels des reservoirs (toutes les 10 ticks)
        if (level.getGameTime() % 10 == 0) {
            be.updateReservoirLevels();
        }

        be.setChanged();
    }

    /**
     * Met à jour les niveaux visuels des reservoirs du multibloc.
     * - Reservoirs du haut (Y+1): affichent le niveau du fuelTank (entree)
     * - Reservoirs du bas (Y-1): affichent le niveau du outputTank (sortie)
     * Chaque reservoir affiche 1/4 de la capacité totale du tank.
     */
    private void updateReservoirLevels() {
        if (level == null) return;

        // Calcul des quantités par reservoir (4 reservoirs par tank)
        int fuelPerReservoir = fuelTank.getFluidAmount() / 4;
        int outputPerReservoir = outputTank.getFluidAmount() / 4;

        // Mise à jour des reservoirs fuel (bas)
        for (BlockPos offset : FUEL_RESERVOIR_OFFSETS) {
            BlockPos reservoirPos = worldPosition.offset(offset);
            if (level.getBlockEntity(reservoirPos) instanceof HoneyReservoirBlockEntity reservoir) {
                FluidTank tank = reservoir.getFluidTank();
                FluidStack currentFluid = fuelTank.getFluid();

                // Synchronise le niveau visuel
                tank.setFluid(currentFluid.isEmpty()
                    ? FluidStack.EMPTY
                    : currentFluid.copyWithAmount(Math.min(fuelPerReservoir, HoneyReservoirBlockEntity.CAPACITY)));
            }
        }

        // Mise à jour des reservoirs output (haut)
        for (BlockPos offset : OUTPUT_RESERVOIR_OFFSETS) {
            BlockPos reservoirPos = worldPosition.offset(offset);
            if (level.getBlockEntity(reservoirPos) instanceof HoneyReservoirBlockEntity reservoir) {
                FluidTank tank = reservoir.getFluidTank();
                FluidStack currentFluid = outputTank.getFluid();

                // Synchronise le niveau visuel
                tank.setFluid(currentFluid.isEmpty()
                    ? FluidStack.EMPTY
                    : currentFluid.copyWithAmount(Math.min(outputPerReservoir, HoneyReservoirBlockEntity.CAPACITY)));
            }
        }
    }

    private void findValidRecipe(Level level) {
        ItemStack stack = inputSlot.getStackInSlot(0);
        if (stack.isEmpty()) {
            currentRecipe = null;
            return;
        }

        ProcessingRecipeInput input = ProcessingRecipeInput.ofItem(stack);
        Optional<RecipeHolder<CentrifugeRecipe>> recipe = level.getRecipeManager().getRecipeFor(
            BeemancerRecipeTypes.CENTRIFUGING.get(),
            input,
            level
        );

        if (recipe.isPresent()) {
            currentRecipe = recipe.get();
            currentProcessTime = Math.max(1, (int)(recipe.get().value().processingTime() * PROCESS_TIME_MULTIPLIER));
        } else {
            currentRecipe = null;
        }
    }

    private void processItem(RandomSource random) {
        if (currentRecipe == null) return;

        CentrifugeRecipe recipe = currentRecipe.value();
        inputSlot.extractItem(0, 1, false);

        FluidStack fluidOutput = recipe.getFluidOutput();
        if (!fluidOutput.isEmpty()) {
            outputTank.fill(fluidOutput, IFluidHandler.FluidAction.EXECUTE);
        }

        for (ProcessingOutput output : recipe.results()) {
            output.roll(random).ifPresent(this::addToOutput);
        }
    }

    private void addToOutput(ItemStack stack) {
        for (int i = 0; i < outputSlots.getSlots(); i++) {
            ItemStack existing = outputSlots.getStackInSlot(i);
            if (existing.isEmpty()) {
                outputSlots.setStackInSlot(i, stack.copy());
                return;
            } else if (ItemStack.isSameItemSameComponents(existing, stack) &&
                       existing.getCount() < existing.getMaxStackSize()) {
                int toAdd = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(toAdd);
                return;
            }
        }
    }

    private boolean hasOutputSpace() {
        if (currentRecipe != null) {
            FluidStack fluidOutput = currentRecipe.value().getFluidOutput();
            if (!fluidOutput.isEmpty()) {
                int spaceInTank = outputTank.getCapacity() - outputTank.getFluidAmount();
                if (spaceInTank < fluidOutput.getAmount()) {
                    return false;
                }
            }
        }

        for (int i = 0; i < outputSlots.getSlots(); i++) {
            ItemStack existing = outputSlots.getStackInSlot(i);
            if (existing.isEmpty() || existing.getCount() < existing.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    // ==================== Accessors ====================

    public FluidTank getFuelTank() { return fuelTank; }
    public FluidTank getOutputTank() { return outputTank; }
    public ItemStackHandler getInputSlot() { return inputSlot; }
    public ItemStackHandler getOutputSlots() { return outputSlots; }
    public SplitFluidHandler getSplitFluidHandler() { return splitFluidHandler; }
    public SplitItemHandler getSplitItemHandler() { return splitItemHandler; }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.powered_centrifuge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new PoweredCentrifugeMenu(containerId, playerInv, this, dataAccess);
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
        tag.put("Output", outputSlots.serializeNBT(registries));
        tag.put("FuelTank", fuelTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("Formed");
        inputSlot.deserializeNBT(registries, tag.getCompound("Input"));
        outputSlots.deserializeNBT(registries, tag.getCompound("Output"));
        fuelTank.readFromNBT(registries, tag.getCompound("FuelTank"));
        outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
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
