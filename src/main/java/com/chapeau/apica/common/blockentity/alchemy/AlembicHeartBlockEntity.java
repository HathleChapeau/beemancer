/**
 * ============================================================
 * [AlembicHeartBlockEntity.java]
 * Description: Controleur du multibloc Alembic - Distillation de fluides
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | MultiblockController          | Interface controleur | Formation/destruction          |
 * | MultiblockCapabilityProvider  | Delegation caps      | Capabilities sur reservoirs    |
 * | MultiblockIOConfig            | Config IO declarative| IO_CONFIG statique             |
 * | MultiblockPatterns            | Definition pattern   | ALEMBIC_MULTIBLOCK             |
 * | MultiblockValidator           | Validation           | tryFormMultiblock()            |
 * | MultiblockEvents              | Enregistrement       | Detection destruction          |
 * | SplitFluidHandler             | Handlers directionnels| inputOnly/outputOnly          |
 * | ApicaBlockEntities        | Type registration    | Constructor                    |
 * | ApicaRecipeTypes          | Recettes distilling  | Processing                     |
 * | ApicaFluids               | Validation fluides   | Tank filtering                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - AlembicHeartBlock.java (creation BlockEntity, ticker)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.alchemy;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.alchemy.AlembicHeartBlock;
import com.chapeau.apica.common.menu.alchemy.AlembicMenu;
import com.chapeau.apica.core.multiblock.BlockIORule;
import com.chapeau.apica.core.multiblock.IOMode;
import com.chapeau.apica.core.multiblock.MultiblockCapabilityProvider;
import com.chapeau.apica.core.multiblock.MultiblockController;
import com.chapeau.apica.core.multiblock.MultiblockEvents;
import com.chapeau.apica.core.multiblock.MultiblockFormationHelper;
import com.chapeau.apica.core.multiblock.MultiblockIOConfig;
import com.chapeau.apica.core.multiblock.MultiblockPattern;
import com.chapeau.apica.core.multiblock.MultiblockPatterns;
import com.chapeau.apica.core.multiblock.MultiblockProperty;
import com.chapeau.apica.core.multiblock.MultiblockValidator;
import com.chapeau.apica.core.util.SplitFluidHandler;
import com.chapeau.apica.core.recipe.ApicaRecipeTypes;
import com.chapeau.apica.core.recipe.ProcessingRecipeInput;
import com.chapeau.apica.core.recipe.type.DistillingRecipe;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.registry.ApicaFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
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
public class AlembicHeartBlockEntity extends BlockEntity implements MultiblockController, MultiblockCapabilityProvider, MenuProvider {

    private static final int TANK_CAPACITY = 4000;
    private static final int DEFAULT_PROCESS_TIME = 80;

    // Positions des reservoirs (relatif au coeur)
    private static final BlockPos[] RESERVOIR_OFFSETS = {
        new BlockPos(0, -1, 0),   // Bas (nectar output)
        new BlockPos(-1, 0, 0),   // Gauche (honey input)
        new BlockPos(1, 0, 0)     // Droit (royal jelly input)
    };

    // Configuration IO declarative : quelles faces exposent quoi
    private static final MultiblockIOConfig IO_CONFIG = MultiblockIOConfig.builder()
        // Heart (0,0,0) : directional per-face
        .fluid(0, 0, 0, BlockIORule.builder()
            .down(IOMode.OUTPUT)
            .up(IOMode.INPUT)
            .sides(IOMode.INPUT)
            .build())
        // Reservoir bas (0,-1,0) : nectar output sur les cotes
        .fluid(0, -1, 0, BlockIORule.sides(IOMode.OUTPUT))
        // Reservoir gauche (-1,0,0) : honey input sur les cotes
        .fluid(-1, 0, 0, BlockIORule.sides(IOMode.INPUT))
        // Reservoir droit (1,0,0) : royal jelly input sur les cotes
        .fluid(1, 0, 0, BlockIORule.sides(IOMode.INPUT))
        .build();

    private boolean formed = false;
    private int multiblockRotation = 0;
    private boolean isProcessingDrain = false;

    private final FluidTank honeyTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == ApicaFluids.HONEY_SOURCE.get();
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
            return stack.getFluid() == ApicaFluids.ROYAL_JELLY_SOURCE.get();
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
        super(ApicaBlockEntities.ALEMBIC_HEART.get(), pos, state);
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
    public int getRotation() {
        return multiblockRotation;
    }

    @Override
    public void onMultiblockFormed() {
        formed = true;
        if (level != null && !level.isClientSide()) {
            // 1. Link reservoirs AU CONTROLLER d'abord (avant que les blockstates ne declenchent updateShape)
            MultiblockFormationHelper.linkReservoirs(level, worldPosition, RESERVOIR_OFFSETS, multiblockRotation, true);

            // 2. Puis changer les blockstates (declenche updateShape sur les pipes voisines)
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(AlembicHeartBlock.MULTIBLOCK)) {
                level.setBlock(worldPosition, state.setValue(AlembicHeartBlock.MULTIBLOCK, MultiblockProperty.ALEMBIC), 3);
            }
            // Mapping des proprietes MULTIBLOCK pour l'Alembic:
            // - Reservoirs lateraux (Y=0, X!=0): ALEMBIC_0 (avec frames)
            // - Reservoir bottom (Y=-1): ALEMBIC (simple colonne)
            // - Glass et RoyalGold: ALEMBIC
            MultiblockFormationHelper.setFormedOnStructureBlocks(level, worldPosition, getPattern(),
                offset -> {
                    if (offset.getY() == 0 && offset.getX() != 0) {
                        return MultiblockProperty.ALEMBIC_0; // Reservoirs lateraux avec frames
                    }
                    return MultiblockProperty.ALEMBIC; // Tous les autres (bottom, glass, royal gold)
                },
                multiblockRotation);

            // 3. Definir le FACING sur TOUS les blocs de structure selon la rotation du multibloc
            Direction facing = rotationToFacing(multiblockRotation);
            MultiblockFormationHelper.setFacingOnStructureBlocks(level, worldPosition, getPattern(), multiblockRotation, facing);

            // 4. Invalider les capabilities de TOUS les blocs du multibloc
            MultiblockFormationHelper.invalidateAllCapabilities(level, worldPosition, getPattern(), multiblockRotation);

            MultiblockEvents.registerActiveController(level, worldPosition);
            setChanged();
        }
    }

    /**
     * Convertit la rotation du multibloc (0-3) en Direction FACING pour les reservoirs.
     * Le modele alembic_0 a des frames sur E/W, donc:
     * - rotation 0 (non rotaté) → WEST (y=0, frames E/W)
     * - rotation 1 (90° CW) → NORTH (y=90, frames N/S)
     * - rotation 2 (180°) → EAST (y=180, frames E/W)
     * - rotation 3 (270°) → SOUTH (y=270, frames N/S)
     */
    private static Direction rotationToFacing(int rotation) {
        return switch (rotation) {
            case 1 -> Direction.NORTH;
            case 2 -> Direction.EAST;
            case 3 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }

    @Override
    public void onMultiblockBroken() {
        formed = false;
        if (level != null && !level.isClientSide()) {
            // 1. Changer les blockstates d'abord
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(AlembicHeartBlock.MULTIBLOCK)) {
                level.setBlock(worldPosition, state.setValue(AlembicHeartBlock.MULTIBLOCK, MultiblockProperty.NONE), 3);
            }
            MultiblockFormationHelper.clearFormedOnStructureBlocks(level, worldPosition, getPattern(), multiblockRotation);

            // 2. Unlink reservoirs (controllerPos = null)
            MultiblockFormationHelper.linkReservoirs(level, worldPosition, RESERVOIR_OFFSETS, multiblockRotation, false);

            // 3. Invalider les capabilities de TOUS les blocs du multibloc
            MultiblockFormationHelper.invalidateAllCapabilities(level, worldPosition, getPattern(), multiblockRotation);

            multiblockRotation = 0;
            MultiblockEvents.unregisterController(level, worldPosition);
            setChanged();
        }
    }

    /**
     * Tente de former le multibloc Alembic.
     * @return true si la formation a reussi
     */
    public boolean tryFormMultiblock() {
        if (level == null || level.isClientSide()) return false;

        int rotation = MultiblockValidator.validateWithRotations(getPattern(), level, worldPosition);
        if (rotation >= 0) {
            multiblockRotation = rotation;
            onMultiblockFormed();
            return true;
        }

        Apica.LOGGER.debug("Alembic validation failed at {}", worldPosition);
        return false;
    }

    // ==================== MultiblockCapabilityProvider ====================

    @Override
    @Nullable
    public IFluidHandler getFluidHandlerForBlock(BlockPos worldPos, @Nullable Direction face) {
        if (!formed) return null;
        IOMode mode = IO_CONFIG.getFluidMode(worldPosition, worldPos, face, multiblockRotation);
        if (mode == null || mode == IOMode.NONE) return null;
        // Inverse-rotate the world offset to get back to pattern coordinates for tank selection
        Vec3i worldOffset = worldPos.subtract(worldPosition);
        Vec3i patternOffset = MultiblockPattern.rotateY(worldOffset, (4 - multiblockRotation) & 3);
        FluidTank tank = getAlembicTankForOffset(patternOffset, face);
        if (tank == null) return null;
        return switch (mode) {
            case INPUT -> SplitFluidHandler.inputOnly(tank);
            case OUTPUT -> SplitFluidHandler.outputOnly(tank);
            case BOTH -> tank;
            default -> null;
        };
    }

    /**
     * Mappe une position relative et une face au tank correspondant.
     * Le heart a un mapping per-face, les reservoirs ont un mapping par position.
     */
    private FluidTank getAlembicTankForOffset(Vec3i offset, @Nullable Direction face) {
        if (offset.equals(Vec3i.ZERO)) {
            if (face == Direction.DOWN) return nectarTank;
            if (face == Direction.UP) return royalJellyTank;
            return honeyTank;
        }
        if (offset.getY() == -1) return nectarTank;
        if (offset.getX() == -1) return honeyTank;
        if (offset.getX() == 1) return royalJellyTank;
        return null;
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
            ApicaRecipeTypes.DISTILLING.get(),
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
        return Component.translatable("container.apica.alembic");
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
        if (level != null) {
            MultiblockEvents.unregisterController(level, worldPosition);
        } else {
            MultiblockEvents.unregisterController(worldPosition);
        }
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
        tag.putInt("MultiblockRotation", multiblockRotation);
        tag.put("HoneyTank", honeyTank.writeToNBT(registries, new CompoundTag()));
        tag.put("RoyalJellyTank", royalJellyTank.writeToNBT(registries, new CompoundTag()));
        tag.put("NectarTank", nectarTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("Formed");
        multiblockRotation = tag.getInt("MultiblockRotation");
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
