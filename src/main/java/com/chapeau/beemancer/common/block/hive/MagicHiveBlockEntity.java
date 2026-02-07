/**
 * ============================================================
 * [MagicHiveBlockEntity.java]
 * Description: BlockEntity ruche magique - orchestrateur principal
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance               | Raison                | Utilisation                    |
 * |--------------------------|----------------------|--------------------------------|
 * | HiveBeeSlot              | Données slot         | État de chaque abeille         |
 * | HiveFlowerPool           | Pool fleurs          | Gestion des fleurs partagées   |
 * | HiveBeeLifecycleManager  | Cycle de vie abeilles| Release, entry, breeding, tick |
 * | MagicBeeEntity           | Entité abeille       | Spawn/capture                  |
 * | MagicBeeItem             | Item abeille         | Conversion entity<->item       |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MagicHiveBlock.java: Création et interaction
 * - MagicHiveMenu.java: Interface GUI
 * - MagicBeeEntity.java: Notifications
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.hive;

import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.common.item.debug.DebugWandItem;
import com.chapeau.beemancer.common.item.bee.MagicBeeItem;
import com.chapeau.beemancer.common.menu.MagicHiveMenu;
import com.chapeau.beemancer.content.gene.environment.EnvironmentGene;
import com.chapeau.beemancer.content.gene.flower.FlowerGene;
import com.chapeau.beemancer.core.bee.BiomeTemperatureManager;
import com.chapeau.beemancer.core.bee.BeeSpeciesManager;
import com.chapeau.beemancer.core.behavior.BeeBehaviorConfig;
import com.chapeau.beemancer.core.behavior.BeeBehaviorManager;
import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MagicHiveBlockEntity extends BlockEntity implements MenuProvider, net.minecraft.world.Container {

    public static final int BEE_SLOTS = 5;
    public static final int OUTPUT_SLOTS = 7;
    public static final int TOTAL_SLOTS = BEE_SLOTS + OUTPUT_SLOTS;

    // === Manager ===
    private final HiveBeeLifecycleManager lifecycleManager = new HiveBeeLifecycleManager(this);

    // Inventaire
    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    // Slots d'abeilles (refactorisé)
    private final HiveBeeSlot[] beeSlots = new HiveBeeSlot[BEE_SLOTS];

    // Pool de fleurs (refactorisé)
    private final HiveFlowerPool flowerPool = new HiveFlowerPool();

    // Breeding
    private boolean antibreedingMode = false;
    private int breedingCooldown = 0;

    // UUID sync verification timer (transient, not saved)
    private int outsideVerifyTimer = 0;

    // Conditions de ruche (pour GUI)
    private boolean hasFlowers = false;
    private boolean hasMushrooms = false;
    private boolean isDaytime = true;
    private int temperature = 0;

    // Legacy compatibility
    public enum BeeState { EMPTY, INSIDE, OUTSIDE }

    // ContainerData for GUI sync
    // 0: antibreedingMode, 1: hasFlowers, 2: hasMushrooms, 3: isDaytime, 4: temperature (-2 to 2, offset +2 for positive)
    // 5-9: canForage pour chaque slot d'abeille
    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> antibreedingMode ? 1 : 0;
                case 1 -> hasFlowers ? 1 : 0;
                case 2 -> hasMushrooms ? 1 : 0;
                case 3 -> isDaytime ? 1 : 0;
                case 4 -> temperature + 2;
                case 5, 6, 7, 8, 9 -> canBeeForage(index - 5) ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> antibreedingMode = value != 0;
                case 1 -> hasFlowers = value != 0;
                case 2 -> hasMushrooms = value != 0;
                case 3 -> isDaytime = value != 0;
                case 4 -> temperature = value - 2;
            }
        }

        @Override
        public int getCount() { return 10; }
    };

    public MagicHiveBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.MAGIC_HIVE.get(), pos, state);
        for (int i = 0; i < BEE_SLOTS; i++) {
            beeSlots[i] = new HiveBeeSlot();
        }
        DebugWandItem.addDisplay(this, this::buildDebugText, new Vec3(0, 1.3, 0));
    }

    // === Manager Accessors (package-private) ===

    NonNullList<ItemStack> getItems() { return items; }
    HiveBeeSlot[] getBeeSlots() { return beeSlots; }
    int getBreedingCooldown() { return breedingCooldown; }
    void setBreedingCooldown(int value) { this.breedingCooldown = value; }

    // ==================== Container Implementation ====================

    @Override
    public int getContainerSize() { return TOTAL_SLOTS; }

    @Override
    public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }

    @Override
    public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < BEE_SLOTS && beeSlots[slot].isOutside()) {
            handleBeeRemoval(slot);
        }
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < BEE_SLOTS && (stack.isEmpty() || !stack.is(BeemancerItems.MAGIC_BEE.get()))) {
            if (beeSlots[slot].isOutside()) {
                MagicBeeEntity bee = findBeeEntity(slot);
                if (bee != null) {
                    bee.markAsEnteredHive();
                    bee.discard();
                }
            }
            returnAssignedFlower(slot);
            beeSlots[slot].clear();
        }

        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }

        if (slot < BEE_SLOTS) {
            if (!stack.isEmpty() && stack.is(BeemancerItems.MAGIC_BEE.get())) {
                initializeBeeSlot(slot, stack);
            }
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
    }

    @Override
    public void clearContent() {
        items.clear();
        for (HiveBeeSlot slot : beeSlots) slot.clear();
        flowerPool.clear();
    }

    // ==================== Bee Slot Initialization ====================

    private void initializeBeeSlot(int slot, ItemStack beeItem) {
        MagicBeeItem.setAssignedHive(beeItem, worldPosition, slot);

        BeeGeneData geneData = MagicBeeItem.getGeneData(beeItem);
        String speciesId = HiveBeeLifecycleManager.getSpeciesId(geneData);
        BeeBehaviorConfig config = BeeBehaviorManager.getConfig(speciesId);

        HiveBeeSlot beeSlot = beeSlots[slot];
        beeSlot.setState(HiveBeeSlot.State.INSIDE);
        beeSlot.setBeeUUID(null);
        beeSlot.setCooldown(config.getRandomRestCooldown(level != null ? level.getRandom() : RandomSource.create()));
        beeSlot.setMaxHealth((float) config.getHealth());
        beeSlot.setCurrentHealth(MagicBeeItem.getStoredHealth(beeItem, beeSlot.getMaxHealth()));
        beeSlot.setNeedsHealing(beeSlot.getCurrentHealth() < beeSlot.getMaxHealth());

        triggerFlowerScan();
    }

    private void handleBeeRemoval(int slot) {
        MagicBeeEntity bee = findBeeEntity(slot);
        if (bee != null) {
            syncEntityToItem(slot, bee);
            bee.markAsEnteredHive();
            bee.discard();
        }
        returnAssignedFlower(slot);
        beeSlots[slot].clear();
    }

    // ==================== Flower Management ====================

    void triggerFlowerScan() {
        if (level == null) return;

        Set<TagKey<Block>> flowerTags = new HashSet<>();
        int maxRadius = 0;

        for (int i = 0; i < BEE_SLOTS; i++) {
            if (beeSlots[i].isEmpty()) continue;
            ItemStack beeItem = items.get(i);
            if (beeItem.isEmpty()) continue;

            BeeGeneData geneData = MagicBeeItem.getGeneData(beeItem);
            Gene flowerGene = geneData.getGene(GeneCategory.FLOWER);
            if (flowerGene instanceof FlowerGene fg && fg.getFlowerTag() != null) {
                flowerTags.add(fg.getFlowerTag());
            }

            BeeBehaviorConfig config = BeeBehaviorManager.getConfig(HiveBeeLifecycleManager.getSpeciesId(geneData));
            maxRadius = Math.max(maxRadius, config.getAreaOfEffect());
        }

        List<BlockPos> assigned = new ArrayList<>();
        for (HiveBeeSlot slot : beeSlots) {
            if (slot.hasAssignedFlower()) assigned.add(slot.getAssignedFlower());
        }

        flowerPool.scanFlowers(level, worldPosition, flowerTags, maxRadius, assigned);
    }

    @Nullable
    public BlockPos getAndAssignFlower(int slot) {
        if (slot < 0 || slot >= BEE_SLOTS) return null;

        ItemStack beeItem = items.get(slot);
        TagKey<Block> flowerTag = null;
        if (!beeItem.isEmpty()) {
            BeeGeneData geneData = MagicBeeItem.getGeneData(beeItem);
            Gene flowerGene = geneData.getGene(GeneCategory.FLOWER);
            if (flowerGene instanceof FlowerGene fg) {
                flowerTag = fg.getFlowerTag();
            }
        }

        RandomSource random = level != null ? level.getRandom() : RandomSource.create();
        BlockPos flower = flowerPool.assignRandomFlower(level, flowerTag, random);

        if (flower != null) {
            beeSlots[slot].setAssignedFlower(flower);
        }
        return flower;
    }

    public void returnFlower(int slot, BlockPos flower) {
        if (slot >= 0 && slot < BEE_SLOTS) {
            beeSlots[slot].clearAssignedFlower();
        }
        flowerPool.returnFlower(flower);
    }

    void returnAssignedFlower(int slot) {
        if (slot >= 0 && slot < BEE_SLOTS && beeSlots[slot].hasAssignedFlower()) {
            flowerPool.returnFlower(beeSlots[slot].getAssignedFlower());
            beeSlots[slot].clearAssignedFlower();
        }
    }

    public boolean hasFlowersForSlot(int slot) {
        return flowerPool.hasFlowers();
    }

    // ==================== Bee State Accessors (Legacy) ====================

    public BeeState getBeeState(int slot) {
        if (slot < 0 || slot >= BEE_SLOTS) return BeeState.EMPTY;
        return switch (beeSlots[slot].getState()) {
            case EMPTY -> BeeState.EMPTY;
            case INSIDE -> BeeState.INSIDE;
            case OUTSIDE -> BeeState.OUTSIDE;
        };
    }

    public boolean isAntibreedingMode() { return antibreedingMode; }
    public boolean hasFlowers() { return hasFlowers; }
    public boolean hasMushrooms() { return hasMushrooms; }
    public boolean isDaytime() { return isDaytime; }
    public int getTemperature() { return temperature; }

    /**
     * Vérifie si une abeille dans le slot peut aller butiner.
     * Conditions: fleurs disponibles, jour (sauf nocturne), température OK, pas en mode antibreeding
     */
    public boolean canBeeForage(int slot) {
        if (slot < 0 || slot >= BEE_SLOTS) return false;
        if (items.get(slot).isEmpty()) return false;
        if (!hasFlowers) return false;
        if (antibreedingMode) return false;

        BeeGeneData geneData = MagicBeeItem.getGeneData(items.get(slot));
        Gene envGene = geneData.getGene(GeneCategory.ENVIRONMENT);
        String speciesId = HiveBeeLifecycleManager.getSpeciesId(geneData);

        BeeSpeciesManager.BeeSpeciesData speciesData = BeeSpeciesManager.getSpecies(speciesId);
        int speciesLevel = (speciesData != null) ? speciesData.toleranceLevel : 1;

        boolean canWorkAtNight = false;
        int geneTolerance = 0;
        if (envGene instanceof EnvironmentGene env) {
            canWorkAtNight = env.canWorkAtNight();
            geneTolerance = env.getTemperatureTolerance();
        }

        if (!isDaytime && !canWorkAtNight) return false;

        int totalTolerance = geneTolerance + (speciesLevel - 1);
        int preferredTemp = (speciesData != null) ? speciesData.environment : 0;
        int tempDiff = Math.abs(temperature - preferredTemp);
        if (tempDiff > totalTolerance) return false;

        return true;
    }

    public int[] getBeeCooldowns() {
        int[] cooldowns = new int[BEE_SLOTS];
        for (int i = 0; i < BEE_SLOTS; i++) cooldowns[i] = beeSlots[i].getCooldown();
        return cooldowns;
    }

    /**
     * Retourne les états de tous les slots d'abeilles (pour debug HUD).
     */
    public HiveBeeSlot.State[] getBeeStates() {
        HiveBeeSlot.State[] states = new HiveBeeSlot.State[BEE_SLOTS];
        for (int i = 0; i < BEE_SLOTS; i++) states[i] = beeSlots[i].getState();
        return states;
    }

    public int getFlowerScanCooldown() { return flowerPool.getScanCooldown(); }

    public List<BlockPos>[] getBeeFlowers() {
        @SuppressWarnings("unchecked")
        List<BlockPos>[] result = new List[BEE_SLOTS];
        for (int i = 0; i < BEE_SLOTS; i++) {
            result[i] = new ArrayList<>(flowerPool.getAvailableFlowers());
        }
        return result;
    }

    // ==================== Bee Lifecycle (délègue) ====================

    public void releaseBee(int slot) { lifecycleManager.releaseBee(slot); }

    public boolean canBeeEnter(MagicBeeEntity bee) { return lifecycleManager.canBeeEnter(bee); }

    public void addBee(MagicBeeEntity bee) { lifecycleManager.addBee(bee); }

    public void onBeeKilled(UUID beeUUID) { lifecycleManager.onBeeKilled(beeUUID); }

    public boolean handleBeePing(MagicBeeEntity bee) { return lifecycleManager.handleBeePing(bee); }

    public void insertIntoOutputSlots(ItemStack stack) { lifecycleManager.insertIntoOutputSlots(stack); }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, MagicHiveBlockEntity hive) {
        hive.antibreedingMode = level.getBlockState(pos.above()).is(BeemancerBlocks.BREEDING_CRYSTAL.get());

        hive.updateHiveConditions(level, pos);

        if (hive.flowerPool.tickScanCooldown()) {
            hive.triggerFlowerScan();
        }

        for (int i = 0; i < BEE_SLOTS; i++) {
            hive.lifecycleManager.tickBeeSlot(i);
        }

        hive.lifecycleManager.checkReturningBees(level, pos);

        hive.outsideVerifyTimer++;
        if (hive.outsideVerifyTimer >= 40) {
            hive.outsideVerifyTimer = 0;
            hive.lifecycleManager.verifyOutsideBees();
        }

        // Breeding géré uniquement à l'entrée des abeilles (voir addBee)
    }

    private void updateHiveConditions(Level level, BlockPos pos) {
        isDaytime = level.isDay();
        temperature = BiomeTemperatureManager.getTemperature(level.getBiome(pos));
        hasFlowers = flowerPool.hasFlowers();
        hasMushrooms = flowerPool.hasMushrooms();
    }

    // ==================== Helpers (restent sur parent) ====================

    @Nullable
    private MagicBeeEntity findBeeEntity(int slot) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        if (slot < 0 || slot >= BEE_SLOTS || !beeSlots[slot].isOutside()) return null;
        UUID uuid = beeSlots[slot].getBeeUUID();
        if (uuid == null) return null;
        Entity entity = serverLevel.getEntity(uuid);
        return entity instanceof MagicBeeEntity bee ? bee : null;
    }

    private void syncEntityToItem(int slot, @Nullable MagicBeeEntity bee) {
        if (slot < 0 || slot >= BEE_SLOTS || bee == null) return;
        ItemStack beeItem = items.get(slot);
        if (beeItem.isEmpty() || !beeItem.is(BeemancerItems.MAGIC_BEE.get())) return;

        MagicBeeItem.saveGeneData(beeItem, bee.getGeneData());
        MagicBeeItem.setStoredHealth(beeItem, bee.getHealth());
        beeSlots[slot].setCurrentHealth(bee.getHealth());
        beeSlots[slot].setMaxHealth(bee.getMaxHealth());
        beeSlots[slot].setNeedsHealing(bee.getHealth() < bee.getMaxHealth());
        setChanged();
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);

        ListTag slotsTag = new ListTag();
        for (int i = 0; i < BEE_SLOTS; i++) {
            slotsTag.add(beeSlots[i].save());
        }
        tag.put("BeeSlots", slotsTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);

        if (tag.contains("BeeSlots")) {
            ListTag slotsTag = tag.getList("BeeSlots", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(slotsTag.size(), BEE_SLOTS); i++) {
                beeSlots[i].load(slotsTag.getCompound(i));
            }
        } else if (tag.contains("BeeStates")) {
            ListTag statesTag = tag.getList("BeeStates", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(statesTag.size(), BEE_SLOTS); i++) {
                CompoundTag slotTag = statesTag.getCompound(i);
                beeSlots[i].setState(HiveBeeSlot.State.values()[slotTag.getInt("State")]);
                if (slotTag.hasUUID("UUID")) beeSlots[i].setBeeUUID(slotTag.getUUID("UUID"));
                beeSlots[i].setCooldown(slotTag.getInt("Cooldown"));
                beeSlots[i].setNeedsHealing(slotTag.getBoolean("NeedsHealing"));
                beeSlots[i].setCurrentHealth(slotTag.getFloat("CurrentHealth"));
                beeSlots[i].setMaxHealth(slotTag.getFloat("MaxHealth"));
            }
        }
    }

    // ==================== Menu ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.beemancer.magic_hive");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MagicHiveMenu(containerId, playerInventory, this, containerData);
    }

    // ==================== Debug Display ====================

    /**
     * Construit le texte debug pour DebugWandItem.addDisplay().
     * Reproduit la logique de l'ancien HiveDebugRenderer.
     */
    private String buildDebugText() {
        StringBuilder sb = new StringBuilder("[Hive Cooldowns]");
        boolean hasAnyBee = false;
        for (int i = 0; i < BEE_SLOTS; i++) {
            HiveBeeSlot.State state = beeSlots[i].getState();
            if (state == HiveBeeSlot.State.EMPTY) continue;
            hasAnyBee = true;
            int cooldown = beeSlots[i].getCooldown();
            String cooldownStr = cooldown > 0
                ? String.format("%.1fs", cooldown / 20.0f)
                : "Ready";
            sb.append("\nSlot ").append(i + 1).append(": ")
              .append(state.name()).append(" [").append(cooldownStr).append("]");
        }
        if (!hasAnyBee) {
            sb.append("\n(empty)");
        }
        return sb.toString();
    }

    // ==================== Cleanup ====================

    public void dropContents() {
        if (level != null && !level.isClientSide()) {
            for (int i = 0; i < BEE_SLOTS; i++) {
                if (beeSlots[i].isOutside()) {
                    MagicBeeEntity bee = findBeeEntity(i);
                    if (bee != null) {
                        syncEntityToItem(i, bee);
                        bee.markAsEnteredHive();
                        bee.discard();
                    }
                    returnAssignedFlower(i);
                    beeSlots[i].clear();
                }
            }
            Containers.dropContents(level, worldPosition, this);
        }
    }
}
