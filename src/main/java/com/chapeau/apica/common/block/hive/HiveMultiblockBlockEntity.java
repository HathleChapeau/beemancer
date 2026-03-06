/**
 * ============================================================
 * [HiveMultiblockBlockEntity.java]
 * Description: BlockEntity pour multibloc ruche 3x3x3
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation               |
 * |--------------------------|----------------------|-----------------------    |
 * | HiveBeeLifecycleManager  | Cycle de vie abeilles| Delegation bee lifecycle  |
 * | IHiveInternals           | Interface lifecycle  | Back-reference manager    |
 * | HiveConfig               | Parametres ruche     | MULTIBLOCK config         |
 * | MultiblockValidator      | Validation           | Verification pattern      |
 * | MultiblockPatterns       | Pattern              | Definition structure      |
 * | MultiblockController     | Interface            | Controleur multibloc      |
 * ------------------------------------------------------------
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.hive;

import com.chapeau.apica.common.entity.bee.MagicBeeEntity;
import com.chapeau.apica.common.item.bee.MagicBeeItem;
import com.chapeau.apica.common.menu.MagicHiveMenu;
import com.chapeau.apica.content.gene.flower.FlowerGene;
import com.chapeau.apica.core.behavior.BeeBehaviorConfig;
import com.chapeau.apica.core.behavior.BeeBehaviorManager;
import com.chapeau.apica.core.gene.BeeGeneData;
import com.chapeau.apica.core.gene.Gene;
import com.chapeau.apica.core.gene.GeneCategory;
import com.chapeau.apica.core.multiblock.MultiblockController;
import com.chapeau.apica.core.multiblock.MultiblockEvents;
import com.chapeau.apica.core.multiblock.MultiblockPattern;
import com.chapeau.apica.core.multiblock.MultiblockPatterns;
import com.chapeau.apica.core.multiblock.MultiblockProperty;
import com.chapeau.apica.core.multiblock.MultiblockValidator;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.registry.ApicaBlocks;
import com.chapeau.apica.core.registry.ApicaItems;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class HiveMultiblockBlockEntity extends BlockEntity implements MenuProvider, net.minecraft.world.Container, MultiblockController, IHiveBeeHost, IHiveInternals {

    public static final int BEE_SLOTS = 5;
    public static final int OUTPUT_SLOTS = 7;
    public static final int TOTAL_SLOTS = BEE_SLOTS + OUTPUT_SLOTS;

    // === Manager ===
    private final HiveBeeLifecycleManager lifecycleManager = new HiveBeeLifecycleManager(this, HiveConfig.MULTIBLOCK, BEE_SLOTS, TOTAL_SLOTS);

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

    // UUID sync verification timer (transient, not saved)
    private int outsideVerifyTimer = 0;

    // Proximity check (transient, not saved)
    private boolean crowded = false;
    private int crowdedCheckTimer = 0;

    // Indices alignés avec MagicHiveBlockEntity:
    // 0: breedingMode, 1-9: non utilisés (retournent 0), 10: crowded
    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> breedingMode ? 1 : 0;
                case 10 -> crowded ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> breedingMode = value != 0;
                case 10 -> crowded = value != 0;
            }
        }

        @Override
        public int getCount() { return 11; }
    };

    public HiveMultiblockBlockEntity(BlockPos pos, BlockState state) {
        super(ApicaBlockEntities.HIVE_MULTIBLOCK.get(), pos, state);
        for (int i = 0; i < BEE_SLOTS; i++) {
            beeSlots[i] = new HiveBeeSlot();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (isController && formed && level != null && !level.isClientSide()) {
            MultiblockEvents.registerActiveController(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (isController) {
            MultiblockEvents.unregisterController(worldPosition);
        }
    }

    // ==================== IHiveInternals ====================

    @Override public NonNullList<ItemStack> getItems() { return items; }
    @Override public HiveBeeSlot[] getBeeSlots() { return beeSlots; }
    @Override public HiveFlowerPool getFlowerPool() { return flowerPool; }

    @Override
    public int getOutputSlotLimit() { return 64; }

    @Override
    public boolean shouldReleaseBee(int slot) {
        if (breedingMode) return false;
        if (crowded) return false;
        return true;
    }

    @Override
    public boolean shouldBreedOnEntry() {
        return true;
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
        // Formation logic is handled in formMultiblock()
    }

    @Override
    public void onMultiblockBroken() {
        onBroken();
    }

    @Nullable
    public HiveMultiblockBlockEntity findOrBecomeController() {
        if (level == null) return null;

        if (controllerPos != null) {
            BlockEntity be = level.getBlockEntity(controllerPos);
            if (be instanceof HiveMultiblockBlockEntity controller && controller.isFormed()) {
                return controller;
            }
            controllerPos = null;
        }

        if (isController && formed) {
            return this;
        }

        return tryFormMultiblock();
    }

    @Nullable
    private HiveMultiblockBlockEntity tryFormMultiblock() {
        if (level == null) return null;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos potentialController = worldPosition.offset(dx, dy, dz);

                    if (MultiblockValidator.validate(MultiblockPatterns.HIVE_MULTIBLOCK, level, potentialController)) {
                        BlockEntity be = level.getBlockEntity(potentialController);
                        if (be instanceof HiveMultiblockBlockEntity controller) {
                            controller.formMultiblock();

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

        // Set controller blockstate: MULTIBLOCK=HIVE, CONTROLLER=true
        BlockState controllerState = level.getBlockState(worldPosition);
        if (controllerState.hasProperty(HiveMultiblockBlock.MULTIBLOCK)) {
            level.setBlock(worldPosition, controllerState
                    .setValue(HiveMultiblockBlock.MULTIBLOCK, MultiblockProperty.HIVE)
                    .setValue(HiveMultiblockBlock.CONTROLLER, true), 3);
        }

        // Set all structure members: MULTIBLOCK=HIVE, CONTROLLER=false
        MultiblockPattern pattern = getPattern();
        for (Vec3i offset : pattern.getStructurePositions()) {
            BlockPos structurePos = worldPosition.offset(offset);
            if (structurePos.equals(worldPosition)) continue;

            BlockState blockState = level.getBlockState(structurePos);
            if (blockState.hasProperty(HiveMultiblockBlock.MULTIBLOCK)) {
                level.setBlock(structurePos, blockState
                        .setValue(HiveMultiblockBlock.MULTIBLOCK, MultiblockProperty.HIVE)
                        .setValue(HiveMultiblockBlock.CONTROLLER, false), 3);
                BlockEntity be = level.getBlockEntity(structurePos);
                if (be instanceof HiveMultiblockBlockEntity hive) {
                    hive.controllerPos = worldPosition;
                    hive.isController = false;
                    hive.formed = true;
                    hive.setChanged();
                }
            }
        }

        MultiblockEvents.registerActiveController(level, worldPosition);
        setChanged();

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.players().stream()
                .filter(p -> p.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 256)
                .forEach(p -> p.sendSystemMessage(Component.translatable("message.apica.hive_multiblock.formed")));
        }
    }

    public void onBroken() {
        if (level == null || level.isClientSide()) return;

        if (isController && formed) {
            dropContents();

            // Reset all member blocks
            MultiblockPattern pattern = getPattern();
            for (Vec3i offset : pattern.getStructurePositions()) {
                BlockPos structurePos = worldPosition.offset(offset);
                if (structurePos.equals(worldPosition)) continue;

                BlockState blockState = level.getBlockState(structurePos);
                if (blockState.hasProperty(HiveMultiblockBlock.MULTIBLOCK)) {
                    level.setBlock(structurePos, blockState
                            .setValue(HiveMultiblockBlock.MULTIBLOCK, MultiblockProperty.NONE)
                            .setValue(HiveMultiblockBlock.CONTROLLER, false), 3);
                    BlockEntity be = level.getBlockEntity(structurePos);
                    if (be instanceof HiveMultiblockBlockEntity hive) {
                        hive.controllerPos = null;
                        hive.isController = false;
                        hive.formed = false;
                        hive.setChanged();
                    }
                }
            }

            // Reset the controller block itself
            BlockState controllerState = level.getBlockState(worldPosition);
            if (controllerState.hasProperty(HiveMultiblockBlock.MULTIBLOCK)) {
                level.setBlock(worldPosition, controllerState
                        .setValue(HiveMultiblockBlock.MULTIBLOCK, MultiblockProperty.NONE)
                        .setValue(HiveMultiblockBlock.CONTROLLER, false), 3);
            }

            MultiblockEvents.unregisterController(worldPosition);
        } else if (controllerPos != null) {
            BlockEntity be = level.getBlockEntity(controllerPos);
            if (be instanceof HiveMultiblockBlockEntity controller) {
                controller.onBroken();
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
        if (slot < BEE_SLOTS && (stack.isEmpty() || !stack.is(ApicaItems.MAGIC_BEE.get()))) {
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

        if (slot < BEE_SLOTS && !stack.isEmpty() && stack.is(ApicaItems.MAGIC_BEE.get())) {
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

    @Override
    public void triggerFlowerScan() {
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

    @Override
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

    @Override
    public void returnFlower(int slot, BlockPos flower) {
        if (slot >= 0 && slot < BEE_SLOTS) {
            beeSlots[slot].clearAssignedFlower();
        }
        flowerPool.returnFlower(flower);
    }

    @Override
    public void returnAssignedFlower(int slot) {
        if (slot >= 0 && slot < BEE_SLOTS && beeSlots[slot].hasAssignedFlower()) {
            flowerPool.returnFlower(beeSlots[slot].getAssignedFlower());
            beeSlots[slot].clearAssignedFlower();
        }
    }

    @Override
    public boolean hasFlowersForSlot(int slot) {
        return flowerPool.hasFlowers();
    }

    // ==================== Bee Lifecycle (delegue au manager) ====================

    public void releaseBee(int slot) { lifecycleManager.releaseBee(slot); }

    public boolean canBeeEnter(MagicBeeEntity bee) { return lifecycleManager.canBeeEnter(bee); }

    public void addBee(MagicBeeEntity bee) { lifecycleManager.addBee(bee); }

    @Override
    public void onBeeKilled(UUID beeUUID) { lifecycleManager.onBeeKilled(beeUUID); }

    @Override
    public boolean handleBeePing(MagicBeeEntity bee) { return lifecycleManager.handleBeePing(bee); }

    public void insertIntoOutputSlots(ItemStack stack) { lifecycleManager.insertIntoOutputSlots(stack); }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, HiveMultiblockBlockEntity hive) {
        if (!hive.isController || !hive.formed) return;

        hive.breedingMode = level.getBlockState(pos.above(2)).is(ApicaBlocks.BREEDING_CRYSTAL.get());

        hive.crowdedCheckTimer++;
        if (hive.crowdedCheckTimer >= 100) {
            hive.crowdedCheckTimer = 0;
            hive.crowded = hive.hasNearbyHive(4);
        }

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
    }

    public boolean isCrowded() { return crowded; }

    // ==================== Helpers ====================

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
        if (beeItem.isEmpty() || !beeItem.is(ApicaItems.MAGIC_BEE.get())) return;

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
        return Component.translatable("block.apica.hive_multiblock");
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
