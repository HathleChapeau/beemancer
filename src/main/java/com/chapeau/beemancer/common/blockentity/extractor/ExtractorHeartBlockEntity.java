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
package com.chapeau.beemancer.common.blockentity.extractor;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.extractor.ExtractorHeartBlock;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import com.chapeau.beemancer.common.blockentity.altar.HoneyPedestalBlockEntity;
import com.chapeau.beemancer.common.blockentity.altar.HoneyReservoirBlockEntity;
import com.chapeau.beemancer.common.item.bee.MagicBeeItem;
import com.chapeau.beemancer.common.item.essence.EssenceItem;
import com.chapeau.beemancer.core.bee.BeeSpeciesManager;
import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.multiblock.MultiblockController;
import com.chapeau.beemancer.core.multiblock.MultiblockEvents;
import com.chapeau.beemancer.core.multiblock.MultiblockPattern;
import com.chapeau.beemancer.core.multiblock.MultiblockPatterns;
import com.chapeau.beemancer.core.multiblock.MultiblockValidator;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * BlockEntity du cœur de l'extracteur d'essence.
 * Consomme du miel pour extraire des essences des abeilles.
 */
public class ExtractorHeartBlockEntity extends BlockEntity implements MultiblockController {

    private static final int HONEY_CONSUMPTION_PER_TICK = 10; // mB par seconde
    private static final double ESSENCE_DROP_CHANCE = 0.05; // 5% par seconde
    private static final int TICK_INTERVAL = 1200; // Toutes les minutes

    // Poids des essences par niveau
    private static final int WEIGHT_LESSER = 8;
    private static final int WEIGHT_NORMAL = 4;
    private static final int WEIGHT_GREATER = 2;
    private static final int WEIGHT_PERFECT = 1;

    private boolean extractorFormed = false;
    private int tickCounter = 0;

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
        super(BeemancerBlockEntities.EXTRACTOR_HEART.get(), pos, state);
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
        if (!beeItem.is(BeemancerItems.MAGIC_BEE.get())) return;

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
                ItemStack diurnal = new ItemStack(BeemancerItems.DIURNAL_ESSENCE.get());
                for (int i = 0; i < WEIGHT_NORMAL; i++) list.add(diurnal);
            }
            case "night" -> {
                // Nocturnal = niveau "essence" (normal), poids 4
                ItemStack nocturnal = new ItemStack(BeemancerItems.NOCTURNAL_ESSENCE.get());
                for (int i = 0; i < WEIGHT_NORMAL; i++) list.add(nocturnal);
            }
            case "both" -> {
                // Insomnia = niveau "perfect", poids 1
                ItemStack insomnia = new ItemStack(BeemancerItems.INSOMNIA_ESSENCE.get());
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
                case 1 -> new ItemStack(BeemancerItems.LESSER_DROP_ESSENCE.get());
                case 2 -> new ItemStack(BeemancerItems.DROP_ESSENCE.get());
                case 3 -> new ItemStack(BeemancerItems.GREATER_DROP_ESSENCE.get());
                case 4 -> new ItemStack(BeemancerItems.PERFECT_DROP_ESSENCE.get());
                default -> ItemStack.EMPTY;
            };
            case SPEED -> switch (level) {
                case 1 -> new ItemStack(BeemancerItems.LESSER_SPEED_ESSENCE.get());
                case 2 -> new ItemStack(BeemancerItems.SPEED_ESSENCE.get());
                case 3 -> new ItemStack(BeemancerItems.GREATER_SPEED_ESSENCE.get());
                case 4 -> new ItemStack(BeemancerItems.PERFECT_SPEED_ESSENCE.get());
                default -> ItemStack.EMPTY;
            };
            case FORAGING -> switch (level) {
                case 1 -> new ItemStack(BeemancerItems.LESSER_FORAGING_ESSENCE.get());
                case 2 -> new ItemStack(BeemancerItems.FORAGING_ESSENCE.get());
                case 3 -> new ItemStack(BeemancerItems.GREATER_FORAGING_ESSENCE.get());
                case 4 -> new ItemStack(BeemancerItems.PERFECT_FORAGING_ESSENCE.get());
                default -> ItemStack.EMPTY;
            };
            case TOLERANCE -> switch (level) {
                case 1 -> new ItemStack(BeemancerItems.LESSER_TOLERANCE_ESSENCE.get());
                case 2 -> new ItemStack(BeemancerItems.TOLERANCE_ESSENCE.get());
                case 3 -> new ItemStack(BeemancerItems.GREATER_TOLERANCE_ESSENCE.get());
                case 4 -> new ItemStack(BeemancerItems.PERFECT_TOLERANCE_ESSENCE.get());
                default -> ItemStack.EMPTY;
            };
            default -> ItemStack.EMPTY;
        };
    }

    // ==================== Pedestal Access ====================

    @Nullable
    private HoneyPedestalBlockEntity getPedestal(int index) {
        if (level == null || index < 0 || index >= PEDESTAL_OFFSETS.length) return null;
        BlockPos pedestalPos = worldPosition.offset(PEDESTAL_OFFSETS[index]);
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

    // ==================== Reservoir/Honey Access ====================

    private int getTotalHoney() {
        if (level == null) return 0;
        int total = 0;
        for (BlockPos offset : RESERVOIR_OFFSETS) {
            BlockPos reservoirPos = worldPosition.offset(offset);
            BlockEntity be = level.getBlockEntity(reservoirPos);
            if (be instanceof HoneyReservoirBlockEntity reservoir) {
                FluidStack fluid = reservoir.getFluid();
                if (fluid.getFluid() == BeemancerFluids.HONEY_SOURCE.get()) {
                    total += fluid.getAmount();
                }
            }
        }
        return total;
    }

    private void consumeHoney(int amount) {
        if (level == null) return;
        int remaining = amount;
        for (BlockPos offset : RESERVOIR_OFFSETS) {
            if (remaining <= 0) break;
            BlockPos reservoirPos = worldPosition.offset(offset);
            BlockEntity be = level.getBlockEntity(reservoirPos);
            if (be instanceof HoneyReservoirBlockEntity reservoir) {
                FluidStack fluid = reservoir.getFluid();
                if (fluid.getFluid() == BeemancerFluids.HONEY_SOURCE.get()) {
                    FluidStack drained = reservoir.drain(remaining, IFluidHandler.FluidAction.EXECUTE);
                    remaining -= drained.getAmount();
                }
            }
        }
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
    public void onMultiblockFormed() {
        extractorFormed = true;
        if (level != null && !level.isClientSide()) {
            level.setBlock(worldPosition, getBlockState().setValue(ExtractorHeartBlock.MULTIBLOCK, MultiblockProperty.EXTRACTOR), 3);
            MultiblockEvents.registerActiveController(level, worldPosition);
            setChanged();
            syncToClient();
        }
    }

    @Override
    public void onMultiblockBroken() {
        extractorFormed = false;
        if (level != null && !level.isClientSide()) {
            if (level.getBlockState(worldPosition).hasProperty(ExtractorHeartBlock.MULTIBLOCK)) {
                level.setBlock(worldPosition, getBlockState().setValue(ExtractorHeartBlock.MULTIBLOCK, MultiblockProperty.NONE), 3);
            }
            MultiblockEvents.unregisterController(worldPosition);
            setChanged();
            syncToClient();
        }
    }

    // ==================== Public API ====================

    public boolean tryFormExtractor() {
        if (level == null || level.isClientSide()) return false;

        var result = MultiblockValidator.validateDetailed(getPattern(), level, worldPosition);

        if (result.valid()) {
            onMultiblockFormed();
            return true;
        }

        // Log détaillé pour debug
        BlockPos failPos = result.failedAt();
        if (failPos != null) {
            BlockState failedState = level.getBlockState(failPos);
            Beemancer.LOGGER.warn("Extractor validation failed at {} (offset from controller: {}) - Found: {} - {}",
                failPos, failPos.subtract(worldPosition), failedState, result.reason());
        } else {
            Beemancer.LOGGER.warn("Extractor validation failed - {}", result.reason());
        }
        return false;
    }

    // ==================== Lifecycle ====================

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (extractorFormed) {
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        extractorFormed = tag.getBoolean("ExtractorFormed");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
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
