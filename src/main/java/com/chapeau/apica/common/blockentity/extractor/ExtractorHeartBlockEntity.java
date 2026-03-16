/**
 * ============================================================
 * [ExtractorHeartBlockEntity.java]
 * Description: BlockEntity du cœur de l'extracteur d'essence
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation           |
 * |-------------------------|----------------------|-----------------------|
 * | MultiblockController    | Interface contrôleur | Implémentation        |
 * | MultiblockPatterns      | Définition pattern   | ESSENCE_EXTRACTOR     |
 * | HoneyPedestalBlockEntity| Accès pedestals      | Items abeille/essence |
 * | HoneyReservoirBlockEntity| Accès réservoirs    | Consommation miel     |
 * | BeeSpeciesManager       | Stats abeille        | Calcul essences       |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ExtractorHeartBlock.java (création BlockEntity)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.extractor;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.extractor.ExtractorHeartBlock;
import com.chapeau.apica.core.multiblock.MultiblockProperty;
import com.chapeau.apica.common.blockentity.altar.HoneyPedestalBlockEntity;
import com.chapeau.apica.common.blockentity.altar.HoneyReservoirBlockEntity;
import com.chapeau.apica.common.item.bee.MagicBeeItem;
import com.chapeau.apica.common.item.essence.EssenceItem;
import com.chapeau.apica.common.item.essence.SpeciesEssenceItem;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.chapeau.apica.core.gene.BeeGeneData;
import com.chapeau.apica.core.gene.Gene;
import com.chapeau.apica.core.gene.GeneCategory;
import com.chapeau.apica.core.multiblock.BlockIORule;
import com.chapeau.apica.core.multiblock.IOMode;
import com.chapeau.apica.core.multiblock.MultiblockCapabilityProvider;
import com.chapeau.apica.core.multiblock.MultiblockController;
import com.chapeau.apica.core.multiblock.MultiblockEvents;
import com.chapeau.apica.core.multiblock.MultiblockFormationHelper;
import com.chapeau.apica.core.multiblock.MultiblockIOConfig;
import com.chapeau.apica.core.multiblock.MultiblockPattern;
import com.chapeau.apica.core.multiblock.MultiblockPatterns;
import com.chapeau.apica.core.multiblock.MultiblockValidator;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.registry.ApicaFluids;
import com.chapeau.apica.core.registry.ApicaItems;
import com.chapeau.apica.core.util.SplitFluidHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * BlockEntity du cœur de l'extracteur d'essence.
 * Consomme du miel pour extraire des essences des abeilles.
 */
public class ExtractorHeartBlockEntity extends BlockEntity implements MultiblockController, MultiblockCapabilityProvider {

    private static final int HONEY_CONSUMPTION_PER_TICK = 10; // mB par seconde
    private static final double ESSENCE_DROP_CHANCE = 0.05; // 5% par seconde
    private static final int TICK_INTERVAL = 1200; // Toutes les minutes
    private static final int TANK_CAPACITY = 16000; // 4 réservoirs x 4000mB

    // Poids des essences par niveau
    private static final int WEIGHT_LESSER = 8;
    private static final int WEIGHT_NORMAL = 4;
    private static final int WEIGHT_GREATER = 2;
    private static final int WEIGHT_PERFECT = 1;

    /**
     * Configuration IO: les réservoirs acceptent INPUT sur leurs faces externes (N/S/E/W).
     * L'Extractor consomme du honey depuis son tank centralisé.
     */
    private static final MultiblockIOConfig IO_CONFIG = MultiblockIOConfig.builder()
        // Réservoirs (Y+1): INPUT honey sur toutes les faces horizontales
        .fluid(0, 1, -1, BlockIORule.sides(IOMode.INPUT))  // Nord
        .fluid(0, 1, 1, BlockIORule.sides(IOMode.INPUT))   // Sud
        .fluid(1, 1, 0, BlockIORule.sides(IOMode.INPUT))   // Est
        .fluid(-1, 1, 0, BlockIORule.sides(IOMode.INPUT))  // Ouest
        .build();

    private boolean extractorFormed = false;
    private int multiblockRotation = 0;
    private int tickCounter = 0;

    /**
     * Tank centralisé pour le honey. Les réservoirs sont des PROXIES.
     * ⚠️ Le stockage est ICI, pas dans les réservoirs. ⚠️
     */
    private final FluidTank honeyTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == ApicaFluids.HONEY_SOURCE.get();
        }
        @Override
        protected void onContentsChanged() {
            setChanged();
            updateReservoirVisuals();
        }
    };

    // Positions relatives des pedestals (Y-2)
    private static final BlockPos[] PEDESTAL_OFFSETS = {
        new BlockPos(0, -2, 0),   // Centre (abeille)
        new BlockPos(0, -2, -1),  // Nord
        new BlockPos(0, -2, 1),   // Sud
        new BlockPos(1, -2, 0),   // Est
        new BlockPos(-1, -2, 0)   // Ouest
    };

    // Positions relatives des réservoirs (Y+1, bords N/S/E/W)
    private static final BlockPos[] RESERVOIR_OFFSETS = {
        new BlockPos(0, 1, -1),   // Nord
        new BlockPos(0, 1, 1),    // Sud
        new BlockPos(1, 1, 0),    // Est
        new BlockPos(-1, 1, 0)    // Ouest
    };

    public ExtractorHeartBlockEntity(BlockPos pos, BlockState state) {
        super(ApicaBlockEntities.EXTRACTOR_HEART.get(), pos, state);
    }

    // ==================== Tick Logic ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, ExtractorHeartBlockEntity be) {
        if (!be.extractorFormed) return;

        be.tickCounter++;
        if (be.tickCounter < TICK_INTERVAL) return;
        be.tickCounter = 0;

        be.tryExtractEssence();
    }

    private void tryExtractEssence() {
        if (level == null || level.isClientSide()) return;

        // Vérifier qu'il y a une abeille au centre
        HoneyPedestalBlockEntity centerPedestal = getPedestal(0);
        if (centerPedestal == null || centerPedestal.isEmpty()) return;

        ItemStack beeItem = centerPedestal.getStoredItem();
        if (!beeItem.is(ApicaItems.MAGIC_BEE.get())) return;

        // Vérifier qu'il y a du miel
        int totalHoney = getTotalHoney();
        if (totalHoney < HONEY_CONSUMPTION_PER_TICK) return;

        // Consommer du miel
        consumeHoney(HONEY_CONSUMPTION_PER_TICK);

        // 5% de chance de drop une essence
        if (level.getRandom().nextDouble() >= ESSENCE_DROP_CHANCE) return;

        // Vérifier qu'au moins un pedestal de sortie est vide
        List<HoneyPedestalBlockEntity> emptyPedestals = getEmptyOutputPedestals();
        if (emptyPedestals.isEmpty()) return;

        // Créer et placer l'essence
        ItemStack essence = createRandomEssence(beeItem);
        if (!essence.isEmpty()) {
            HoneyPedestalBlockEntity target = emptyPedestals.get(level.getRandom().nextInt(emptyPedestals.size()));
            target.placeItem(essence);
        }
    }

    private ItemStack createRandomEssence(ItemStack beeItem) {
        BeeGeneData geneData = MagicBeeItem.getGeneData(beeItem);
        Gene speciesGene = geneData.getGene(GeneCategory.SPECIES);
        if (speciesGene == null) return ItemStack.EMPTY;

        BeeSpeciesManager.BeeSpeciesData speciesData = BeeSpeciesManager.getSpecies(speciesGene.getId());
        if (speciesData == null) return ItemStack.EMPTY;

        // Construire la liste pondérée d'essences
        List<ItemStack> weightedEssences = new ArrayList<>();

        // Drop essences (niveau dropLevel)
        addWeightedEssences(weightedEssences, EssenceItem.EssenceType.DROP, speciesData.dropLevel);

        // Speed essences (niveau flyingSpeedLevel)
        addWeightedEssences(weightedEssences, EssenceItem.EssenceType.SPEED, speciesData.flyingSpeedLevel);

        // Foraging essences (niveau foragingDurationLevel)
        addWeightedEssences(weightedEssences, EssenceItem.EssenceType.FORAGING, speciesData.foragingDurationLevel);

        // Tolerance essences (niveau toleranceLevel)
        addWeightedEssences(weightedEssences, EssenceItem.EssenceType.TOLERANCE, speciesData.toleranceLevel);

        // Activity essences (diurnal/nocturnal/insomnia)
        addActivityEssences(weightedEssences, speciesData.dayNight);

        // Species essence (faible poids, meme que PERFECT)
        ItemStack speciesEssence = SpeciesEssenceItem.createForSpecies(speciesGene.getId());
        weightedEssences.add(speciesEssence);

        if (weightedEssences.isEmpty()) return ItemStack.EMPTY;

        // Choisir aléatoirement
        return weightedEssences.get(level.getRandom().nextInt(weightedEssences.size())).copy();
    }

    private void addWeightedEssences(List<ItemStack> list, EssenceItem.EssenceType type, int level) {
        // Ajouter uniquement l'essence du niveau exact de l'abeille
        ItemStack essence = getEssenceItem(type, level);
        if (!essence.isEmpty()) {
            int weight = getWeightForLevel(level);
            for (int i = 0; i < weight; i++) {
                list.add(essence);
            }
        }
    }

    private void addActivityEssences(List<ItemStack> list, String dayNight) {
        switch (dayNight) {
            case "day" -> {
                // Diurnal = niveau "essence" (normal), poids 4
                ItemStack diurnal = new ItemStack(ApicaItems.DIURNAL_ESSENCE.get());
                for (int i = 0; i < WEIGHT_NORMAL; i++) list.add(diurnal);
            }
            case "night" -> {
                // Nocturnal = niveau "essence" (normal), poids 4
                ItemStack nocturnal = new ItemStack(ApicaItems.NOCTURNAL_ESSENCE.get());
                for (int i = 0; i < WEIGHT_NORMAL; i++) list.add(nocturnal);
            }
            case "both" -> {
                // Insomnia = niveau "perfect", poids 1
                ItemStack insomnia = new ItemStack(ApicaItems.INSOMNIA_ESSENCE.get());
                list.add(insomnia);
            }
        }
    }

    private int getWeightForLevel(int level) {
        return switch (level) {
            case 1 -> WEIGHT_LESSER;
            case 2 -> WEIGHT_NORMAL;
            case 3 -> WEIGHT_GREATER;
            case 4 -> WEIGHT_PERFECT;
            default -> WEIGHT_NORMAL;
        };
    }

    private ItemStack getEssenceItem(EssenceItem.EssenceType type, int level) {
        return switch (type) {
            case DROP -> switch (level) {
                case 1 -> new ItemStack(ApicaItems.LESSER_DROP_ESSENCE.get());
                case 2 -> new ItemStack(ApicaItems.DROP_ESSENCE.get());
                case 3 -> new ItemStack(ApicaItems.GREATER_DROP_ESSENCE.get());
                case 4 -> new ItemStack(ApicaItems.PERFECT_DROP_ESSENCE.get());
                default -> ItemStack.EMPTY;
            };
            case SPEED -> switch (level) {
                case 1 -> new ItemStack(ApicaItems.LESSER_SPEED_ESSENCE.get());
                case 2 -> new ItemStack(ApicaItems.SPEED_ESSENCE.get());
                case 3 -> new ItemStack(ApicaItems.GREATER_SPEED_ESSENCE.get());
                case 4 -> new ItemStack(ApicaItems.PERFECT_SPEED_ESSENCE.get());
                default -> ItemStack.EMPTY;
            };
            case FORAGING -> switch (level) {
                case 1 -> new ItemStack(ApicaItems.LESSER_FORAGING_ESSENCE.get());
                case 2 -> new ItemStack(ApicaItems.FORAGING_ESSENCE.get());
                case 3 -> new ItemStack(ApicaItems.GREATER_FORAGING_ESSENCE.get());
                case 4 -> new ItemStack(ApicaItems.PERFECT_FORAGING_ESSENCE.get());
                default -> ItemStack.EMPTY;
            };
            case TOLERANCE -> switch (level) {
                case 1 -> new ItemStack(ApicaItems.LESSER_TOLERANCE_ESSENCE.get());
                case 2 -> new ItemStack(ApicaItems.TOLERANCE_ESSENCE.get());
                case 3 -> new ItemStack(ApicaItems.GREATER_TOLERANCE_ESSENCE.get());
                case 4 -> new ItemStack(ApicaItems.PERFECT_TOLERANCE_ESSENCE.get());
                default -> ItemStack.EMPTY;
            };
            default -> ItemStack.EMPTY;
        };
    }

    // ==================== Pedestal Access ====================

    @Nullable
    private HoneyPedestalBlockEntity getPedestal(int index) {
        if (level == null || index < 0 || index >= PEDESTAL_OFFSETS.length) return null;
        Vec3i rotatedOffset = MultiblockPattern.rotateY(PEDESTAL_OFFSETS[index], multiblockRotation);
        BlockPos pedestalPos = worldPosition.offset(rotatedOffset);
        BlockEntity be = level.getBlockEntity(pedestalPos);
        return be instanceof HoneyPedestalBlockEntity pedestal ? pedestal : null;
    }

    private List<HoneyPedestalBlockEntity> getEmptyOutputPedestals() {
        List<HoneyPedestalBlockEntity> empty = new ArrayList<>();
        // Pedestals 1-4 sont les sorties (0 est le centre avec l'abeille)
        for (int i = 1; i < PEDESTAL_OFFSETS.length; i++) {
            HoneyPedestalBlockEntity pedestal = getPedestal(i);
            if (pedestal != null && pedestal.isEmpty()) {
                empty.add(pedestal);
            }
        }
        return empty;
    }

    // ==================== Centralized Honey Tank ====================

    /**
     * Retourne le montant total de honey dans le tank centralisé.
     * ⚠️ Le stockage est dans le contrôleur, PAS dans les réservoirs. ⚠️
     */
    private int getTotalHoney() {
        return honeyTank.getFluidAmount();
    }

    /**
     * Consomme du honey depuis le tank centralisé.
     * ⚠️ Le stockage est dans le contrôleur, PAS dans les réservoirs. ⚠️
     */
    private void consumeHoney(int amount) {
        honeyTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
    }

    /**
     * Met à jour le cache visuel des réservoirs pour le rendu.
     * Appelé quand le tank change.
     */
    private void updateReservoirVisuals() {
        if (level == null || !extractorFormed) return;

        int honeyPerReservoir = honeyTank.getFluidAmount() / 4;
        float fillRatio = (float) honeyPerReservoir / HoneyReservoirBlockEntity.VISUAL_CAPACITY;

        for (BlockPos offset : RESERVOIR_OFFSETS) {
            Vec3i rotatedOffset = MultiblockPattern.rotateY(offset, multiblockRotation);
            BlockPos reservoirPos = worldPosition.offset(rotatedOffset);
            if (level.getBlockEntity(reservoirPos) instanceof HoneyReservoirBlockEntity reservoir) {
                FluidStack visualFluid = honeyTank.getFluid().isEmpty()
                    ? FluidStack.EMPTY
                    : honeyTank.getFluid().copyWithAmount(Math.min(honeyPerReservoir, HoneyReservoirBlockEntity.VISUAL_CAPACITY));
                reservoir.setVisualFluid(visualFluid, Math.min(1f, fillRatio));
            }
        }
    }

    // ==================== MultiblockCapabilityProvider ====================

    @Override
    @Nullable
    public IFluidHandler getFluidHandlerForBlock(BlockPos worldPos, @Nullable Direction face) {
        if (!extractorFormed) return null;
        IOMode mode = IO_CONFIG.getFluidMode(worldPosition, worldPos, face, multiblockRotation);
        if (mode == null || mode == IOMode.NONE) return null;
        return switch (mode) {
            case INPUT -> SplitFluidHandler.inputOnly(honeyTank);
            case OUTPUT -> SplitFluidHandler.outputOnly(honeyTank);
            case BOTH -> honeyTank;
            default -> null;
        };
    }

    public FluidTank getHoneyTank() {
        return honeyTank;
    }

    // ==================== MultiblockController ====================

    @Override
    public MultiblockPattern getPattern() {
        return MultiblockPatterns.ESSENCE_EXTRACTOR;
    }

    @Override
    public boolean isFormed() {
        return extractorFormed;
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
        extractorFormed = true;
        if (level != null && !level.isClientSide()) {
            // 1. Link réservoirs au contrôleur
            MultiblockFormationHelper.linkReservoirs(level, worldPosition, RESERVOIR_OFFSETS, multiblockRotation, true);
            // 2. Blockstate contrôleur
            level.setBlock(worldPosition, getBlockState().setValue(ExtractorHeartBlock.MULTIBLOCK, MultiblockProperty.EXTRACTOR), 3);
            // 3. Blockstates structure (framework)
            MultiblockFormationHelper.setFormedOnStructureBlocks(level, worldPosition, getPattern(), MultiblockProperty.EXTRACTOR, multiblockRotation);
            // 4. Pedestal rotations (spécifique extractor)
            setPedestalRotations(true, multiblockRotation);
            // 5. Invalider capabilities
            MultiblockFormationHelper.invalidateAllCapabilities(level, worldPosition, getPattern(), multiblockRotation);
            // 6. Register events
            MultiblockEvents.registerActiveController(level, worldPosition);
            setChanged();
            syncToClient();
        }
    }

    @Override
    public void onMultiblockBroken() {
        int savedRotation = multiblockRotation;
        extractorFormed = false;
        multiblockRotation = 0;
        if (level != null && !level.isClientSide()) {
            // 1. Blockstate contrôleur
            if (level.getBlockState(worldPosition).hasProperty(ExtractorHeartBlock.MULTIBLOCK)) {
                level.setBlock(worldPosition, getBlockState().setValue(ExtractorHeartBlock.MULTIBLOCK, MultiblockProperty.NONE), 3);
            }
            // 2. Blockstates structure (framework)
            MultiblockFormationHelper.clearFormedOnStructureBlocks(level, worldPosition, getPattern(), savedRotation);
            // 3. Pedestal rotations (spécifique extractor)
            setPedestalRotations(false, savedRotation);
            // 4. Unlink réservoirs
            MultiblockFormationHelper.linkReservoirs(level, worldPosition, RESERVOIR_OFFSETS, savedRotation, false);
            // 5. Invalider capabilities
            MultiblockFormationHelper.invalidateAllCapabilities(level, worldPosition, getPattern(), savedRotation);
            // 6. Unregister events (dimension-aware)
            MultiblockEvents.unregisterController(level, worldPosition);
            setChanged();
            syncToClient();
        }
    }

    /**
     * Met à jour FORMED_ROTATION sur les pedestals de la structure.
     * La propriété MULTIBLOCK est gérée par MultiblockFormationHelper.
     */
    private void setPedestalRotations(boolean formed, int rotation) {
        if (level == null) return;
        for (MultiblockPattern.PatternElement element : getPattern().getElements()) {
            Vec3i rotatedOffset = MultiblockPattern.rotateY(element.offset(), rotation);
            BlockPos blockPos = worldPosition.offset(rotatedOffset);
            BlockState state = level.getBlockState(blockPos);

            if (state.getBlock() instanceof com.chapeau.apica.common.block.altar.HoneyPedestalBlock
                && state.hasProperty(com.chapeau.apica.common.block.altar.HoneyPedestalBlock.FORMED_ROTATION)) {
                int pedestalRot = computePedestalRotation(element.offset(), formed);
                if (state.getValue(com.chapeau.apica.common.block.altar.HoneyPedestalBlock.FORMED_ROTATION) != pedestalRot) {
                    level.setBlock(blockPos, state.setValue(com.chapeau.apica.common.block.altar.HoneyPedestalBlock.FORMED_ROTATION, pedestalRot), 3);
                }
            }
        }
    }

    /**
     * Calcule la rotation du pedestal selon sa position dans le multibloc.
     * Utilise l'offset NON rotaté (position dans le pattern original).
     * - 0: pedestal normal (non formé)
     * - 1: pedestal extractor centre (0, -2, 0)
     * - 2-5: pedestals extractor côtés (N, E, S, W)
     */
    private int computePedestalRotation(Vec3i offset, boolean formed) {
        if (!formed) return 0;

        // Centre
        if (offset.getX() == 0 && offset.getY() == -2 && offset.getZ() == 0) {
            return 1;
        }

        // Côtés (Y=-2 uniquement)
        if (offset.getY() == -2) {
            if (offset.getZ() == -1) return 2; // Nord
            if (offset.getX() == 1) return 3;  // Est
            if (offset.getZ() == 1) return 4;  // Sud
            if (offset.getX() == -1) return 5; // Ouest
        }

        return 0;
    }

    // ==================== Public API ====================

    public boolean tryFormExtractor() {
        if (level == null || level.isClientSide()) return false;

        int rotation = MultiblockValidator.validateWithRotations(getPattern(), level, worldPosition);
        if (rotation >= 0) {
            this.multiblockRotation = rotation;
            onMultiblockFormed();
            return true;
        }

        Apica.LOGGER.debug("Extractor validation failed at {}", worldPosition);
        return false;
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
        if (extractorFormed && level != null && !level.isClientSide()) {
            MultiblockEvents.registerActiveController(level, worldPosition);
        }
    }

    // ==================== Sync ====================

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("ExtractorFormed", extractorFormed);
        tag.putInt("MultiblockRotation", multiblockRotation);
        tag.put("HoneyTank", honeyTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        extractorFormed = tag.getBoolean("ExtractorFormed");
        multiblockRotation = tag.getInt("MultiblockRotation");
        if (tag.contains("HoneyTank")) {
            honeyTank.readFromNBT(registries, tag.getCompound("HoneyTank"));
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
}
