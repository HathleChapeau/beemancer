/**
 * ============================================================
 * [HiveMultiblockBlockEntity.java]
 * Description: BlockEntity pour multibloc ruche 3x3x3
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation           |
 * |-------------------------|----------------------|-----------------------|
 * | MagicHiveBlockEntity    | Logique ruche        | Copie comportement    |
 * | MultiblockValidator     | Validation           | Vérification pattern  |
 * | MultiblockPatterns      | Pattern              | Définition structure  |
 * | MultiblockController    | Interface            | Contrôleur multibloc  |
 * ------------------------------------------------------------
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.hive;

import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.common.item.bee.BeeLarvaItem;
import com.chapeau.beemancer.common.item.bee.MagicBeeItem;
import com.chapeau.beemancer.common.menu.MagicHiveMenu;
import com.chapeau.beemancer.content.gene.flower.FlowerGene;
import com.chapeau.beemancer.core.behavior.BeeBehaviorConfig;
import com.chapeau.beemancer.core.behavior.BeeBehaviorManager;
import com.chapeau.beemancer.core.breeding.BreedingManager;
import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.multiblock.MultiblockController;
import com.chapeau.beemancer.core.multiblock.MultiblockPattern;
import com.chapeau.beemancer.core.multiblock.MultiblockPatterns;
import com.chapeau.beemancer.core.multiblock.MultiblockValidator;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerEntities;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.*;

public class HiveMultiblockBlockEntity extends BlockEntity implements MenuProvider, net.minecraft.world.Container, MultiblockController {

    public static final int BEE_SLOTS = 5;
    public static final int OUTPUT_SLOTS = 7;
    public static final int TOTAL_SLOTS = BEE_SLOTS + OUTPUT_SLOTS;

    // Controller position (null = not part of a formed multiblock, or this IS the controller)
    @Nullable
    private BlockPos controllerPos = null;
    private boolean isController = false;
    private boolean formed = false;

    // Inventory (only used by controller)
    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private final HiveBeeSlot[] beeSlots = new HiveBeeSlot[BEE_SLOTS];
    private final HiveFlowerPool flowerPool = new HiveFlowerPool();

    // Breeding
    private boolean breedingMode = false;
    private int breedingCooldown = 0;

    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? (breedingMode ? 1 : 0) : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) breedingMode = value != 0;
        }

        @Override
        public int getCount() { return 1; }
    };

    public HiveMultiblockBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.HIVE_MULTIBLOCK.get(), pos, state);
        for (int i = 0; i < BEE_SLOTS; i++) {
            beeSlots[i] = new HiveBeeSlot();
        }
    }

    // ==================== Multiblock Logic ====================

    @Override
    public boolean isFormed() {
        return formed;
    }

    @Override
    public MultiblockPattern getPattern() {
        return MultiblockPatterns.HIVE_MULTIBLOCK;
    }

    @Override
    public BlockPos getControllerPos() {
        return worldPosition;
    }

    @Override
    public void onMultiblockFormed() {
        // Called by external systems when multiblock is formed
        // Formation logic is handled in formMultiblock()
    }

    @Override
    public void onMultiblockBroken() {
        if (isController && formed) {
            dropContents();
        }
        formed = false;
        isController = false;
        controllerPos = null;
        setChanged();
    }

    /**
     * Find the controller for this block's multiblock, or try to form one.
     * Returns the controller if found/formed, null otherwise.
     */
    @Nullable
    public HiveMultiblockBlockEntity findOrBecomeController() {
        if (level == null) return null;

        // If we have a controller, return it
        if (controllerPos != null) {
            BlockEntity be = level.getBlockEntity(controllerPos);
            if (be instanceof HiveMultiblockBlockEntity controller && controller.isFormed()) {
                return controller;
            }
            // Controller no longer valid
            controllerPos = null;
        }

        // If this is already a controller, return this
        if (isController && formed) {
            return this;
        }

        // Try to validate and form the multiblock
        return tryFormMultiblock();
    }

    @Nullable
    private HiveMultiblockBlockEntity tryFormMultiblock() {
        if (level == null) return null;

        // Find the potential controller position (center of bottom layer)
        // We need to search around this block to find where the controller should be
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -2; dy <= 0; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos potentialController = worldPosition.offset(dx, dy, dz);

                    // Check if pattern is valid with this as controller
                    if (MultiblockValidator.validate(MultiblockPatterns.HIVE_MULTIBLOCK, level, potentialController)) {
                        BlockEntity be = level.getBlockEntity(potentialController);
                        if (be instanceof HiveMultiblockBlockEntity controller) {
                            controller.formMultiblock();

                            // Set this block's controller reference
                            if (controller != this) {
                                this.controllerPos = potentialController;
                            }

                            return controller;
                        }
                    }
                }
            }
        }

        return null;
    }

    private void formMultiblock() {
        if (level == null) return;

        isController = true;
        formed = true;
        controllerPos = null;

        // Notify only hive blocks in the structure (not slabs)
        MultiblockPattern pattern = getPattern();
        for (Vec3i offset : pattern.getStructurePositions()) {
            // Skip slabs (Y+3) - they don't have BlockEntities
            if (offset.getY() >= 3) continue;

            BlockPos structurePos = worldPosition.offset(offset);
            if (!structurePos.equals(worldPosition)) {
                BlockEntity be = level.getBlockEntity(structurePos);
                if (be instanceof HiveMultiblockBlockEntity hive) {
                    hive.controllerPos = worldPosition;
                    hive.isController = false;
                    hive.formed = true;
                    hive.setChanged();
                }
            }
        }

        com.chapeau.beemancer.core.multiblock.MultiblockEvents.registerActiveController(level, worldPosition);
        setChanged();

        // Send formation message to nearby players
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.players().stream()
                .filter(p -> p.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 256)
                .forEach(p -> p.sendSystemMessage(Component.translatable("message.beemancer.hive_multiblock.formed")));
        }
    }

    public void onBroken() {
        if (level == null || level.isClientSide()) return;

        if (isController && formed) {
            // Drop all contents
            dropContents();

            // Notify only hive blocks in the structure (not slabs)
            MultiblockPattern pattern = getPattern();
            for (Vec3i offset : pattern.getStructurePositions()) {
                // Skip slabs (Y+3) - they don't have BlockEntities
                if (offset.getY() >= 3) continue;

                BlockPos structurePos = worldPosition.offset(offset);
                if (!structurePos.equals(worldPosition)) {
                    BlockEntity be = level.getBlockEntity(structurePos);
                    if (be instanceof HiveMultiblockBlockEntity hive) {
                        hive.controllerPos = null;
                        hive.isController = false;
                        hive.formed = false;
                        hive.setChanged();
                    }
                }
            }

            com.chapeau.beemancer.core.multiblock.MultiblockEvents.unregisterController(worldPosition);
        } else if (controllerPos != null) {
            // Notify the controller that structure is broken
            BlockEntity be = level.getBlockEntity(controllerPos);
            if (be instanceof HiveMultiblockBlockEntity controller) {
                controller.onMultiblockBroken();
            }
        }

        formed = false;
        isController = false;
        controllerPos = null;
    }

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

        if (slot < BEE_SLOTS && !stack.isEmpty() && stack.is(BeemancerItems.MAGIC_BEE.get())) {
            initializeBeeSlot(slot, stack);
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
        String speciesId = getSpeciesId(geneData);
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

    private void triggerFlowerScan() {
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

            BeeBehaviorConfig config = BeeBehaviorManager.getConfig(getSpeciesId(geneData));
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

    private void returnAssignedFlower(int slot) {
        if (slot >= 0 && slot < BEE_SLOTS && beeSlots[slot].hasAssignedFlower()) {
            flowerPool.returnFlower(beeSlots[slot].getAssignedFlower());
            beeSlots[slot].clearAssignedFlower();
        }
    }

    public boolean hasFlowersForSlot(int slot) {
        return flowerPool.hasFlowers();
    }

    // ==================== Bee Release/Entry ====================

    public void releaseBee(int slot) {
        if (slot < 0 || slot >= BEE_SLOTS) return;
        if (!beeSlots[slot].isInside()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        ItemStack beeItem = items.get(slot);
        if (beeItem.isEmpty() || !beeItem.is(BeemancerItems.MAGIC_BEE.get())) return;
        if (!hasFlowersForSlot(slot)) return;

        MagicBeeEntity bee = BeemancerEntities.MAGIC_BEE.get().create(level);
        if (bee == null) return;

        // Spawn above the multiblock structure (Y+4 to be above slabs)
        BlockPos spawnPos = worldPosition.above(4);
        bee.moveTo(spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5, 0, 0);

        BeeGeneData geneData = MagicBeeItem.getGeneData(beeItem);
        bee.getGeneData().copyFrom(geneData);
        for (Gene gene : geneData.getAllGenes()) bee.setGene(gene);

        bee.setStoredHealth(beeSlots[slot].getCurrentHealth());
        bee.setAssignedHive(worldPosition, slot);
        bee.setPollinated(false);
        bee.setEnraged(false);
        bee.setReturning(false);

        serverLevel.addFreshEntity(bee);

        beeSlots[slot].setState(HiveBeeSlot.State.OUTSIDE);
        beeSlots[slot].setBeeUUID(bee.getUUID());
        setChanged();
    }

    public boolean canBeeEnter(MagicBeeEntity bee) {
        if (!bee.hasAssignedHive() || !worldPosition.equals(bee.getAssignedHivePos())) return false;
        int slot = bee.getAssignedSlot();
        return slot >= 0 && slot < BEE_SLOTS && beeSlots[slot].isOutside();
    }

    public void addBee(MagicBeeEntity bee) {
        int slot = bee.getAssignedSlot();
        if (slot < 0 || slot >= BEE_SLOTS) return;

        bee.markAsEnteredHive();

        if (bee.isPollinated()) {
            depositPollinationLoot(bee);
        }

        returnAssignedFlower(slot);

        ItemStack beeItem = MagicBeeItem.captureFromEntity(bee);
        items.set(slot, beeItem);

        HiveBeeSlot beeSlot = beeSlots[slot];
        beeSlot.setState(HiveBeeSlot.State.INSIDE);
        beeSlot.setBeeUUID(null);
        beeSlot.setCurrentHealth(bee.getHealth());
        beeSlot.setMaxHealth(bee.getMaxHealth());
        beeSlot.setNeedsHealing(bee.getHealth() < bee.getMaxHealth());
        beeSlot.setCooldown(bee.getBehaviorConfig().getRandomRestCooldown(level.getRandom()));

        bee.setPollinated(false);
        bee.setEnraged(false);
        bee.setReturning(false);

        triggerFlowerScan();
        setChanged();
    }

    public void onBeeKilled(UUID beeUUID) {
        for (int i = 0; i < BEE_SLOTS; i++) {
            if (beeUUID.equals(beeSlots[i].getBeeUUID()) && beeSlots[i].isOutside()) {
                returnAssignedFlower(i);
                items.set(i, ItemStack.EMPTY);
                beeSlots[i].clear();
                setChanged();
                return;
            }
        }
    }

    // ==================== Loot & Output ====================

    private void depositPollinationLoot(MagicBeeEntity bee) {
        if (level == null) return;
        BeeBehaviorConfig config = bee.getBehaviorConfig();
        for (ItemStack stack : config.rollPollinationLoot(level.getRandom())) {
            insertIntoOutputSlots(stack);
        }
    }

    public void insertIntoOutputSlots(ItemStack stack) {
        if (stack.isEmpty()) return;

        for (int i = BEE_SLOTS; i < TOTAL_SLOTS && !stack.isEmpty(); i++) {
            ItemStack existing = items.get(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                int toAdd = Math.min(existing.getMaxStackSize() - existing.getCount(), stack.getCount());
                if (toAdd > 0) {
                    existing.grow(toAdd);
                    stack.shrink(toAdd);
                }
            }
        }

        for (int i = BEE_SLOTS; i < TOTAL_SLOTS && !stack.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack.copy());
                stack.setCount(0);
            }
        }
        setChanged();
    }

    // ==================== Breeding ====================

    private void attemptBreeding(RandomSource random) {
        List<Integer> insideBees = new ArrayList<>();
        for (int i = 0; i < BEE_SLOTS; i++) {
            if (beeSlots[i].isInside() && !items.get(i).isEmpty()) {
                insideBees.add(i);
            }
        }
        if (insideBees.size() < 2) return;

        int outputSlot = -1;
        for (int i = BEE_SLOTS; i < TOTAL_SLOTS; i++) {
            if (items.get(i).isEmpty()) { outputSlot = i; break; }
        }
        if (outputSlot < 0) return;

        int idx1 = random.nextInt(insideBees.size());
        int idx2;
        do { idx2 = random.nextInt(insideBees.size()); } while (idx2 == idx1);

        int slot1 = insideBees.get(idx1), slot2 = insideBees.get(idx2);
        BeeGeneData parent1 = MagicBeeItem.getGeneData(items.get(slot1));
        BeeGeneData parent2 = MagicBeeItem.getGeneData(items.get(slot2));

        Gene species1 = parent1.getGene(GeneCategory.SPECIES);
        Gene species2 = parent2.getGene(GeneCategory.SPECIES);
        if (species1 == null || species2 == null) return;

        String offspringSpecies = BreedingManager.resolveOffspringSpecies(species1.getId(), species2.getId(), random);

        int costSlot = random.nextBoolean() ? slot1 : slot2;
        BeeGeneData costData = MagicBeeItem.getGeneData(items.get(costSlot));
        int lifetimeCost = (int) (costData.getMaxLifetime() * BreedingManager.LIFETIME_COST_RATIO);
        costData.setRemainingLifetime(costData.getRemainingLifetime() - lifetimeCost);
        MagicBeeItem.saveGeneData(items.get(costSlot), costData);

        if ("nothing".equals(offspringSpecies)) return;

        BeeGeneData offspringData = BreedingManager.createOffspringGeneData(parent1, parent2, offspringSpecies, random);
        items.set(outputSlot, BeeLarvaItem.createWithGenes(offspringData));
        setChanged();
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, HiveMultiblockBlockEntity hive) {
        // Only the controller ticks
        if (!hive.isController || !hive.formed) return;

        // Check for breeding crystal above the structure (Y+4)
        hive.breedingMode = level.getBlockState(pos.above(4)).is(BeemancerBlocks.BREEDING_CRYSTAL.get());

        if (hive.flowerPool.tickScanCooldown()) {
            hive.triggerFlowerScan();
        }

        for (int i = 0; i < BEE_SLOTS; i++) {
            hive.tickBeeSlot(i);
        }

        hive.checkReturningBees(level, pos);
        hive.tickBreeding(level.getRandom());
    }

    private void tickBeeSlot(int slot) {
        HiveBeeSlot beeSlot = beeSlots[slot];
        if (!beeSlot.isInside() || items.get(slot).isEmpty()) return;

        BeeGeneData geneData = MagicBeeItem.getGeneData(items.get(slot));
        BeeBehaviorConfig config = BeeBehaviorManager.getConfig(getSpeciesId(geneData));

        if (beeSlot.needsHealing()) {
            beeSlot.heal(config.getRegenerationRate() / 20.0f);
            if (!beeSlot.needsHealing()) {
                MagicBeeItem.setStoredHealth(items.get(slot), beeSlot.getCurrentHealth());
            }
        }

        if (!beeSlot.needsHealing()) {
            beeSlot.decrementCooldown();
        }

        if (beeSlot.isCooldownComplete() && !beeSlot.needsHealing() && !breedingMode) {
            releaseBee(slot);
        }
    }

    private void checkReturningBees(Level level, BlockPos pos) {
        // Check in a larger area since bees spawn above the structure
        AABB searchBox = new AABB(pos).inflate(4, 5, 4);
        List<MagicBeeEntity> nearbyBees = level.getEntitiesOfClass(MagicBeeEntity.class, searchBox,
                bee -> bee.hasAssignedHive() && pos.equals(bee.getAssignedHivePos()));

        for (MagicBeeEntity bee : nearbyBees) {
            if (canBeeEnter(bee) && bee.isReturning() && bee.position().distanceTo(pos.getCenter()) < 3.0) {
                addBee(bee);
                bee.discard();
            }
        }
    }

    private void tickBreeding(RandomSource random) {
        if (breedingMode && breedingCooldown <= 0) {
            if (random.nextDouble() < BreedingManager.BREEDING_CHANCE_PER_SECOND) {
                attemptBreeding(random);
            }
            breedingCooldown = 20;
        }
        if (breedingCooldown > 0) breedingCooldown--;
    }

    // ==================== Helpers ====================

    private String getSpeciesId(BeeGeneData geneData) {
        Gene species = geneData.getGene(GeneCategory.SPECIES);
        return species != null ? species.getId() : "meadow";
    }

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

        tag.putBoolean("IsController", isController);
        tag.putBoolean("Formed", formed);

        if (controllerPos != null) {
            tag.put("ControllerPos", NbtUtils.writeBlockPos(controllerPos));
        }

        if (isController) {
            ContainerHelper.saveAllItems(tag, items, registries);

            ListTag slotsTag = new ListTag();
            for (int i = 0; i < BEE_SLOTS; i++) {
                slotsTag.add(beeSlots[i].save());
            }
            tag.put("BeeSlots", slotsTag);

            tag.putInt("BreedingCooldown", breedingCooldown);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        isController = tag.getBoolean("IsController");
        formed = tag.getBoolean("Formed");

        if (tag.contains("ControllerPos")) {
            NbtUtils.readBlockPos(tag.getCompound("ControllerPos"), "").ifPresent(pos -> controllerPos = pos);
        } else {
            controllerPos = null;
        }

        if (isController) {
            ContainerHelper.loadAllItems(tag, items, registries);

            if (tag.contains("BeeSlots")) {
                ListTag slotsTag = tag.getList("BeeSlots", Tag.TAG_COMPOUND);
                for (int i = 0; i < Math.min(slotsTag.size(), BEE_SLOTS); i++) {
                    beeSlots[i].load(slotsTag.getCompound(i));
                }
            }

            breedingCooldown = tag.getInt("BreedingCooldown");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("IsController", isController);
        tag.putBoolean("Formed", formed);
        if (controllerPos != null) {
            tag.put("ControllerPos", NbtUtils.writeBlockPos(controllerPos));
        }
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

    // ==================== Menu ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.beemancer.hive_multiblock");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (!isController || !formed) return null;
        return new MagicHiveMenu(containerId, playerInventory, this, containerData);
    }

    // ==================== Cleanup ====================

    public void dropContents() {
        if (level != null && !level.isClientSide() && isController) {
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
