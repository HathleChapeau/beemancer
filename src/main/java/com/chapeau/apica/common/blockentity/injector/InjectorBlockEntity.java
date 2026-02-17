/**
 * ============================================================
 * [InjectorBlockEntity.java]
 * Description: BlockEntity de l'injecteur d'essence avec processing et sync GUI
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | BeeInjectionHelper      | Manipulation data    | Lecture/ecriture stats abeille  |
 * | InjectionConfigManager  | Configuration        | Valeurs essences, timing       |
 * | BeeSpeciesManager       | Stats espece         | Niveaux de base                |
 * | EssenceItem             | Type/niveau essence  | Identification de l'essence    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - InjectorBlock.java (creation, ticker, drop)
 * - InjectorMenu.java (menu joueur)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.injector;

import com.chapeau.apica.common.item.bee.MagicBeeItem;
import com.chapeau.apica.common.item.essence.EssenceItem;
import com.chapeau.apica.common.item.essence.SpeciesEssenceItem;
import com.chapeau.apica.common.menu.InjectorMenu;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.chapeau.apica.core.config.EssenceValue;
import com.chapeau.apica.core.config.InjectionConfigManager;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.registry.ApicaItems;
import com.chapeau.apica.core.util.BeeInjectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class InjectorBlockEntity extends BlockEntity implements MenuProvider {

    public static final int BEE_SLOT = 0;
    public static final int ESSENCE_SLOT = 1;
    private static final int DATA_COUNT = 10;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            processTimer = 0;
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public int getSlotLimit(int slot) { return slot == BEE_SLOT ? 1 : 64; }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == BEE_SLOT) return stack.is(ApicaItems.MAGIC_BEE.get());
            if (slot == ESSENCE_SLOT) return stack.getItem() instanceof EssenceItem
                    && !(stack.getItem() instanceof SpeciesEssenceItem);
            return false;
        }
    };

    private int processTimer = 0;

    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            ItemStack bee = itemHandler.getStackInSlot(BEE_SLOT);
            BeeSpeciesManager.BeeSpeciesData species = getSpeciesData(bee);
            int ppl = InjectionConfigManager.getPointsPerLevel();
            return switch (index) {
                case 0 -> processTimer;
                case 1 -> InjectionConfigManager.getProcessTimeTicks();
                case 2 -> BeeInjectionHelper.getHunger(bee);
                case 3 -> InjectionConfigManager.getMaxHunger();
                case 4 -> species != null ? BeeInjectionHelper.getTotalGaugePoints(bee, EssenceItem.EssenceType.DROP, species.dropLevel) : 0;
                case 5 -> species != null ? BeeInjectionHelper.getTotalGaugePoints(bee, EssenceItem.EssenceType.SPEED, species.flyingSpeedLevel) : 0;
                case 6 -> species != null ? BeeInjectionHelper.getTotalGaugePoints(bee, EssenceItem.EssenceType.FORAGING, species.foragingDurationLevel) : 0;
                case 7 -> species != null ? BeeInjectionHelper.getTotalGaugePoints(bee, EssenceItem.EssenceType.TOLERANCE, species.toleranceLevel) : 0;
                case 8 -> species != null ? BeeInjectionHelper.getTotalGaugePoints(bee, EssenceItem.EssenceType.DIURNAL, BeeInjectionHelper.getActivityLevel(species.dayNight)) : 0;
                case 9 -> BeeInjectionHelper.isSatiated(bee) ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) processTimer = value;
        }

        @Override
        public int getCount() { return DATA_COUNT; }
    };

    public InjectorBlockEntity(BlockPos pos, BlockState state) {
        super(ApicaBlockEntities.INJECTOR.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }

    // ========== TICK ==========

    public static void serverTick(Level level, BlockPos pos, BlockState state, InjectorBlockEntity be) {
        ItemStack beeStack = be.itemHandler.getStackInSlot(BEE_SLOT);
        if (beeStack.isEmpty() || BeeInjectionHelper.isSatiated(beeStack)) {
            be.resetTimer();
            return;
        }

        ItemStack essenceStack = be.itemHandler.getStackInSlot(ESSENCE_SLOT);
        if (essenceStack.isEmpty() || !(essenceStack.getItem() instanceof EssenceItem essenceItem)) {
            be.resetTimer();
            return;
        }

        be.processTimer++;
        if (be.processTimer < InjectionConfigManager.getProcessTimeTicks()) return;

        // Processing complet: consommer l'essence
        be.processEssence(beeStack, essenceItem);
        be.processTimer = 0;
        be.itemHandler.extractItem(ESSENCE_SLOT, 1, false);
        be.setChanged();
    }

    private void processEssence(ItemStack beeStack, EssenceItem essenceItem) {
        EssenceItem.EssenceType type = essenceItem.getEssenceType();
        EssenceItem.EssenceLevel essenceLevel = essenceItem.getEssenceLevel();
        EssenceValue value = InjectionConfigManager.getEssenceValue(type, essenceLevel);

        BeeSpeciesManager.BeeSpeciesData species = getSpeciesData(beeStack);
        if (species == null) return;

        // Determiner le base level et le cap
        if (BeeInjectionHelper.isActivityType(type)) {
            int baseActivity = BeeInjectionHelper.getActivityLevel(species.dayNight);
            int essenceCap = BeeInjectionHelper.getActivityEssenceCap(type);
            BeeInjectionHelper.addStatPoints(beeStack, type, value.statPoints(), baseActivity, essenceCap);
        } else {
            int baseLevel = getBaseLevel(species, type);
            BeeInjectionHelper.addStatPoints(beeStack, type, value.statPoints(), baseLevel, essenceLevel.getValue());
        }

        // La faim monte TOUJOURS (meme si pas de gain de stat)
        BeeInjectionHelper.addHunger(beeStack, value.hungerCost());
    }

    private void resetTimer() {
        if (processTimer > 0) {
            processTimer = 0;
            setChanged();
        }
    }

    // ========== HELPERS ==========

    @Nullable
    private static BeeSpeciesManager.BeeSpeciesData getSpeciesData(ItemStack beeStack) {
        if (beeStack.isEmpty()) return null;
        String speciesId = MagicBeeItem.getSpeciesId(beeStack);
        return speciesId != null ? BeeSpeciesManager.getSpecies(speciesId) : null;
    }

    private static int getBaseLevel(BeeSpeciesManager.BeeSpeciesData species, EssenceItem.EssenceType type) {
        return switch (type) {
            case DROP -> species.dropLevel;
            case SPEED -> species.flyingSpeedLevel;
            case FORAGING -> species.foragingDurationLevel;
            case TOLERANCE -> species.toleranceLevel;
            default -> 1;
        };
    }

    private static boolean isActivityType(EssenceItem.EssenceType type) {
        return type == EssenceItem.EssenceType.DIURNAL
            || type == EssenceItem.EssenceType.NOCTURNAL
            || type == EssenceItem.EssenceType.INSOMNIA;
    }

    // ========== NBT ==========

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.putInt("ProcessTimer", processTimer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        processTimer = tag.getInt("ProcessTimer");
    }

    // ========== MENU ==========

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.apica.injector");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new InjectorMenu(containerId, playerInventory, this, containerData);
    }

    // ========== SYNC ==========

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void dropContents() {
        if (level != null && !level.isClientSide()) {
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                        itemHandler.getStackInSlot(i));
            }
        }
    }
}
