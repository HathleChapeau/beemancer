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
 * | CodexPlayerData         | Knowledge joueur     | Deblocage nom d'espece         |
 * | CodexSyncPacket         | Sync codex           | Envoi data au client           |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - InjectorBlock.java (creation, ticker, drop)
 * - InjectorMenu.java (menu joueur)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.injector;

import com.chapeau.apica.common.codex.CodexPlayerData;
import com.chapeau.apica.common.item.bee.MagicBeeItem;
import com.chapeau.apica.common.item.essence.EssenceItem;
import com.chapeau.apica.common.item.essence.SpeciesEssenceItem;
import com.chapeau.apica.common.menu.InjectorMenu;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.chapeau.apica.core.config.EssenceValue;
import com.chapeau.apica.core.config.InjectionConfigManager;
import com.chapeau.apica.core.network.packets.CodexSyncPacket;
import com.chapeau.apica.core.registry.ApicaAttachments;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.registry.ApicaItems;
import com.chapeau.apica.core.registry.ApicaParticles;
import com.chapeau.apica.core.util.BeeInjectionHelper;
import com.chapeau.apica.core.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class InjectorBlockEntity extends BlockEntity implements MenuProvider {

    public static final int BEE_SLOT = 0;
    public static final int ESSENCE_SLOT = 1;
    private static final int DATA_COUNT = 10;
    private static final int SPECIES_ESSENCE_PROCESS_TICKS = 1200; // 60 seconds

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
                    || stack.getItem() instanceof SpeciesEssenceItem;
            return false;
        }
    };

    /** UUID du dernier joueur ayant ouvert le menu (pour learnSpecies apres processing). */
    @Nullable
    private java.util.UUID lastPlayerUUID = null;

    private int processTimer = 0;

    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            ItemStack bee = itemHandler.getStackInSlot(BEE_SLOT);
            BeeSpeciesManager.BeeSpeciesData species = getSpeciesData(bee);
            int ppl = InjectionConfigManager.getPointsPerLevel();
            return switch (index) {
                case 0 -> processTimer;
                case 1 -> getMaxProcessTicks();
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
        if (essenceStack.isEmpty()) {
            be.resetTimer();
            return;
        }

        boolean isSpeciesEssence = essenceStack.getItem() instanceof SpeciesEssenceItem;
        if (!isSpeciesEssence && !(essenceStack.getItem() instanceof EssenceItem)) {
            be.resetTimer();
            return;
        }

        be.processTimer++;
        if (be.processTimer < be.getMaxProcessTicks()) return;

        // Processing complet: consommer l'essence
        if (isSpeciesEssence) {
            be.processSpeciesEssence(beeStack, essenceStack);
        } else {
            be.processEssence(beeStack, (EssenceItem) essenceStack.getItem());
        }

        // Decouvrir le nom de l'espece pour le joueur qui a utilise l'injecteur
        be.learnSpeciesForPlayer(beeStack, level);

        // Explosion spherique de runes quand l'abeille devient attuned
        if (BeeInjectionHelper.isSatiated(beeStack) && level instanceof ServerLevel serverLevel) {
            ParticleHelper.spawnRingBurst(serverLevel, ApicaParticles.RUNE.get(), Vec3.atCenterOf(pos), 30, 0.15);
        }

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

    /**
     * Traite une essence d'espece : sature toujours l'abeille.
     * Si les deux especes sont parents d'un enfant harmonized ET que les 5 stats
     * de l'abeille (base+bonus) correspondent aux stats par defaut de l'essence,
     * l'abeille obtient l'etat harmonized.
     */
    private void processSpeciesEssence(ItemStack beeStack, ItemStack essenceStack) {
        BeeInjectionHelper.saturateInstantly(beeStack);

        String beeSpeciesId = MagicBeeItem.getSpeciesId(beeStack);
        String essenceSpeciesId = SpeciesEssenceItem.getSpeciesId(essenceStack);
        if (beeSpeciesId == null || essenceSpeciesId == null) return;

        BeeSpeciesManager.BeeSpeciesData beeData = BeeSpeciesManager.getSpecies(beeSpeciesId);
        BeeSpeciesManager.BeeSpeciesData essenceData = BeeSpeciesManager.getSpecies(essenceSpeciesId);
        if (beeData == null || essenceData == null) return;

        // Chercher si un enfant harmonized a ces deux especes comme parents
        boolean hasHarmonizedChild = false;
        for (BeeSpeciesManager.BeeSpeciesData child : BeeSpeciesManager.getAllSpecies()) {
            if (child.harmonized && child.parents != null
                    && child.parents.contains(beeSpeciesId)
                    && child.parents.contains(essenceSpeciesId)) {
                hasHarmonizedChild = true;
                break;
            }
        }
        if (!hasHarmonizedChild) return;

        // Verifier que les 5 stats de l'abeille (base+bonus) == stats par defaut de l'essence
        int beeDrop = beeData.dropLevel + BeeInjectionHelper.getBonusLevel(beeStack, EssenceItem.EssenceType.DROP);
        int beeSpeed = beeData.flyingSpeedLevel + BeeInjectionHelper.getBonusLevel(beeStack, EssenceItem.EssenceType.SPEED);
        int beeForaging = beeData.foragingDurationLevel + BeeInjectionHelper.getBonusLevel(beeStack, EssenceItem.EssenceType.FORAGING);
        int beeTolerance = beeData.toleranceLevel + BeeInjectionHelper.getBonusLevel(beeStack, EssenceItem.EssenceType.TOLERANCE);
        int beeActivity = BeeInjectionHelper.getActivityLevel(beeData.dayNight)
                + BeeInjectionHelper.getBonusLevel(beeStack, EssenceItem.EssenceType.DIURNAL);

        int essenceActivity = BeeInjectionHelper.getActivityLevel(essenceData.dayNight);

        if (beeDrop == essenceData.dropLevel
                && beeSpeed == essenceData.flyingSpeedLevel
                && beeForaging == essenceData.foragingDurationLevel
                && beeTolerance == essenceData.toleranceLevel
                && beeActivity == essenceActivity) {
            BeeInjectionHelper.setHarmonized(beeStack, true);
        }
    }

    /**
     * Apprend le nom de l'espece de l'abeille au joueur qui a lance le processing.
     * Cherche le ServerPlayer via le UUID stocke.
     */
    private void learnSpeciesForPlayer(ItemStack beeStack, Level level) {
        if (lastPlayerUUID == null || !(level instanceof ServerLevel serverLevel)) return;
        String speciesId = MagicBeeItem.getSpeciesId(beeStack);
        if (speciesId == null) return;

        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(lastPlayerUUID);
        if (player == null) return;

        CodexPlayerData codex = player.getData(ApicaAttachments.CODEX_DATA);
        if (codex.isSpeciesKnown(speciesId)) return;

        codex.learnSpecies(speciesId);
        player.setData(ApicaAttachments.CODEX_DATA, codex);
        PacketDistributor.sendToPlayer(player, new CodexSyncPacket(codex));
    }

    private void resetTimer() {
        if (processTimer > 0) {
            processTimer = 0;
            setChanged();
        }
    }

    // ========== HELPERS ==========

    private int getMaxProcessTicks() {
        ItemStack essenceStack = itemHandler.getStackInSlot(ESSENCE_SLOT);
        if (essenceStack.getItem() instanceof SpeciesEssenceItem) {
            return SPECIES_ESSENCE_PROCESS_TICKS;
        }
        return InjectionConfigManager.getProcessTimeTicks();
    }

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
        if (lastPlayerUUID != null) {
            tag.putUUID("LastPlayer", lastPlayerUUID);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        processTimer = tag.getInt("ProcessTimer");
        if (tag.hasUUID("LastPlayer")) {
            lastPlayerUUID = tag.getUUID("LastPlayer");
        }
    }

    // ========== MENU ==========

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.apica.injector");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        this.lastPlayerUUID = player.getUUID();
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
